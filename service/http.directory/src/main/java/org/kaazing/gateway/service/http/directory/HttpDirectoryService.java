/**
 * Copyright 2007-2016, Kaazing Corporation. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kaazing.gateway.service.http.directory;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.mina.core.session.IoSession;
import org.kaazing.gateway.resource.address.uri.URIUtils;
import org.kaazing.gateway.service.Service;
import org.kaazing.gateway.service.ServiceContext;
import org.kaazing.gateway.service.ServiceProperties;
import org.kaazing.gateway.service.http.directory.cachecontrol.ConflictResolverUtils;
import org.kaazing.gateway.service.http.directory.cachecontrol.PatternCacheControl;
import org.kaazing.gateway.service.http.directory.cachecontrol.PatternMatcherUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gateway service of type "directory".
 */
public class HttpDirectoryService implements Service {

    private static final Comparator<PatternCacheControl> PATTERN_CACHE_CONTROL_COMPARATOR =
            new Comparator<PatternCacheControl>() {
                @Override
                public int compare(PatternCacheControl first, PatternCacheControl second) {
                    return (Integer.valueOf(first.getMatchingPatternCount())).compareTo(second
                            .getMatchingPatternCount());
                }
            };

    private final Logger logger = LoggerFactory.getLogger("service.directory");

    private HttpDirectoryServiceHandler handler;
    private ServiceContext serviceContext;

    public HttpDirectoryService() {
    }

    @Override
    public String getType() {
        return "directory";
    }

    @Override
    public void init(ServiceContext serviceContext) throws Exception {
        this.serviceContext = serviceContext;
        handler = new HttpDirectoryServiceHandler();
        File webDir = serviceContext.getWebDirectory();
        ServiceProperties properties = serviceContext.getProperties();

        Collection<String> accepts = serviceContext.getAccepts();
        Collection<String> failedAccepts = new HashSet<>();
        for (String accept : accepts) {
            String path = URIUtils.getPath(accept);
            if (path == null || path.isEmpty()) {
                failedAccepts.add(accept);
            }
        }
        if (!failedAccepts.isEmpty()) {
            throw new IllegalArgumentException(
                    "The following directory service accept elements are missing the URL path. You may need to add a trailing slash): "
                            + failedAccepts);
        }

        String directory = properties.get("directory");
        if (directory == null) {
            throw new IllegalArgumentException("Missing required property: directory");
        }
        File directoryFile = toFile(webDir, directory);

        String welcomeFile = properties.get("welcome-file");
        if (welcomeFile != null) {
            String pathSeparator = File.pathSeparator;
            if (welcomeFile.contains(pathSeparator)) {
                throw new IllegalArgumentException("Unexpected character \"" + pathSeparator + "\" in welcome file: "
                        + welcomeFile);
            }
        }

        File errorPagesDir = toFile(webDir.getParentFile(), properties.get("error-pages-directory"));

        // Notify that custom clientaccesspolicy.xml is ignored
        File clientAccessPolicyXml = new File(directoryFile, "/clientaccesspolicy.xml");
        if (clientAccessPolicyXml.exists()) {
            for (String accept : accepts) {
                logger.warn(
                        "Ignoring user-defined file contents for {}clientaccesspolicy.xml, please modify configuration instead",
                        accept);
            }
        }

        handler.setServiceContext(serviceContext);
        handler.setBaseDir(directoryFile);
        handler.setWelcomeFile(welcomeFile);
        handler.setErrorPagesDir(errorPagesDir);
        handler.setPatterns(buildPatternsList(properties));

        String indexes = properties.get("options");
        if ((indexes != null) && "indexes".equalsIgnoreCase(indexes)) {
            handler.setIndexes(true);
        }

        // Register the Gateway's connection capabilities with the handler so that session counts are tracked
    }

    /**
     * Creates the list of PatternCacheControl objects
     * @param properties - list of ServiceProperties from the configuration file
     * @return a list of PatternCacheControl objects
     */
    private List<PatternCacheControl> buildPatternsList(ServiceProperties properties) {
        Map<String, PatternCacheControl> patterns = new LinkedHashMap<>();
        List<ServiceProperties> locationsList = properties.getNested("location");
        if (locationsList != null && locationsList.size() != 0) {
            for (ServiceProperties location : locationsList) {
                String directiveList = location.get("cache-control");
                String[] patternList = location.get("patterns").split("\\s+");
                for (String pattern : patternList) {
                    patterns.put(pattern, new PatternCacheControl(pattern, directiveList));
                }
            }
            resolvePatternSpecificity(patterns);
            return sortByMatchingPatternCount(patterns);
        }
        return new ArrayList<>(patterns.values());
    }

    /**
     * Matches the patterns from the map and determines each pattern's specificity
     * @param patterns - the map with the patterns to be matched
     */
    private void resolvePatternSpecificity(Map<String, PatternCacheControl> patterns) {
        List<String> patternList = new ArrayList<>();
        patternList.addAll(patterns.keySet());

        int patternCount = patternList.size();

        for (int i = 0; i < patternCount - 1; i++) {
            String specificPattern = patternList.get(i);
            for (int j = i + 1; j < patternCount; j++) {
                String generalPattern = patternList.get(j);
                checkPatternMatching(patterns, specificPattern, generalPattern);
                checkPatternMatching(patterns, generalPattern, specificPattern);
            }
        }
    }

    /**
     * Checks if the first pattern can be included in the second one and resolves directive conflicts if needed
     * @param patterns
     * @param specificPattern
     * @param generalPattern
     */
    private void checkPatternMatching(Map<String, PatternCacheControl> patterns, String specificPattern, String generalPattern) {
        if (PatternMatcherUtils.caseInsensitiveMatch(specificPattern, generalPattern)) {
            PatternCacheControl specificPatternDirective = patterns.get(specificPattern);
            PatternCacheControl generalPatternDirective = patterns.get(generalPattern);
            generalPatternDirective.incrementMatchingPatternCount();
            ConflictResolverUtils.resolveConflicts(specificPatternDirective, generalPatternDirective);
        }
    }

    /**
     * Sorts the patterns map by the number of matching patterns and returns a list of sorted PatternCacheControl elements.
     * The sorted list is used at request, so that a file's URL can be matched to the most specific pattern. 
     * @param unsortedMap
     * @return a list of sorted PatternCacheControl elements
     */
    private List<PatternCacheControl> sortByMatchingPatternCount(Map<String, PatternCacheControl> unsortedMap) {
        List<PatternCacheControl> list = new ArrayList<>(unsortedMap.values());
        Collections.sort(list, PATTERN_CACHE_CONTROL_COMPARATOR);
        return list;
    }

    /**
     * Converts a location in the gateway configuration file into a file relative to a specified root directory.
     * 
     * @param rootDir
     *            the root directory
     * @param location
     *            the location (either a file:// URI or a location relative the root directory
     * @return the file corresponding to the location
     */
    private File toFile(File rootDir, String location) {
        File locationFile = rootDir;
        if (location != null) {
            URI locationURI = URI.create(location);
            locationFile = new File(locationURI.getPath());
            if (locationURI.getScheme() == null) {
                locationFile = new File(rootDir, location);
            } else if (!"file".equals(locationURI.getScheme())) {
                throw new IllegalArgumentException("Unexpected resources directory: " + location);
            }
        }
        return locationFile;
    }

    @Override
    public void start() throws Exception {
        serviceContext.bind(serviceContext.getAccepts(), handler);
    }

    @Override
    public void stop() throws Exception {
        quiesce();

        if (serviceContext != null) {
            for (IoSession session : serviceContext.getActiveSessions()) {
                session.close(true);
            }
        }
        if (handler != null) {
            handler.emptyUrlCacheControlMap();
        }
    }

    @Override
    public void quiesce() throws Exception {
        if (serviceContext != null) {
            serviceContext.unbind(serviceContext.getAccepts(), handler);
        }
    }

    @Override
    public void destroy() throws Exception {
    }
}
