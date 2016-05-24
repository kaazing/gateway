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
package org.kaazing.gateway.server.util.version;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;

import org.slf4j.Logger;

/**
 * Class used for detecting if duplicate jar files are loaded by the gateway.
 */
public class DuplicateJarFinder {

    private static final String JAR_FILE_WITH_VERSION_LOGGING_MESSAGE = "The jar file {} with the version {} was loaded.";
    private static final String CONFLICTING_JARS_LOGGING_MESSAGE =
            "Failed to start because of conflicting versions of artifact on the classpath: {}.jar., with versions {}";
    private static final String MANIFEST_VERSION = "Implementation-Version";
    private static final String MANIFEST_JAR_NAME = "Artifact-Name";

    private ManifestReader classPathParser;
    private Logger gatewayLogger;

    public DuplicateJarFinder(Logger gatewayLogger) {
        this.gatewayLogger = gatewayLogger;
        this.classPathParser = new ManifestReader();
    }

    /**
     * Parses the class path system attribute and the manifest files
     * and if there are duplicate jar a DuplicateJarsException is thrown.
     * @throws IOException
     * @throws DuplicateJarsException
     */
    public void findDuplicateJars() throws IOException, DuplicateJarsException {
        Map<String, List<String>> artifactsToVersion = new HashMap<>();
        Enumeration<URL> manifestURLs = classPathParser.getManifestURLs();
        while (manifestURLs.hasMoreElements()) {
            parseManifestFileFromClassPathEntry(manifestURLs.nextElement(), artifactsToVersion);
        }
        checkForDuplicateJars(artifactsToVersion);
    }

    private void parseManifestFileFromClassPathEntry(URL url, Map<String, List<String>> artifactsToVersion) throws IOException {
        Attributes manifestAttributes = classPathParser.getManifestAttributesFromURL(url);
        String version = manifestAttributes.getValue(MANIFEST_VERSION);
        String artifactName = manifestAttributes.getValue(MANIFEST_JAR_NAME);
        if (isKaazingProduct(artifactName)) {
            artifactName = artifactName.replace("com.kaazing:", "").replace("com.kaazing:", "");
            List<String> versionList = artifactsToVersion.get(artifactName);
            if (versionList == null) {
                versionList = new ArrayList<>();
                artifactsToVersion.put(artifactName, versionList);
            }
            versionList.add(version);
            // log only kaazing jars loaded
            gatewayLogger.debug(JAR_FILE_WITH_VERSION_LOGGING_MESSAGE, artifactName, version);
        }
    }

    private boolean isKaazingProduct(String product) {
        return product != null && (product.contains("org.kaazing") || product.contains("com.kaazing"));
    }

    private void checkForDuplicateJars(Map<String, List<String>> artifactsToVersion) throws DuplicateJarsException {
        for (String artifact : artifactsToVersion.keySet()) {
            List<String> versions = artifactsToVersion.get(artifact);
            if (versions.size() > 1) {
                gatewayLogger.error(CONFLICTING_JARS_LOGGING_MESSAGE, artifact, versions);
                throw new DuplicateJarsException();
            }
        }
    }

    /**
     * This method is only for testing purposes
     * @param classPathParser - the mock for the classPathParser
     */
    void setClassPathParser(ManifestReader classPathParser) {
        this.classPathParser = classPathParser;
    }

}
