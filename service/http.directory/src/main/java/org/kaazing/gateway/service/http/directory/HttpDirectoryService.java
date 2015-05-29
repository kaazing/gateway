/**
 * Copyright (c) 2007-2014 Kaazing Corporation. All rights reserved.
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.kaazing.gateway.service.http.directory;

import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;

import org.apache.mina.core.session.IoSession;
import org.kaazing.gateway.service.Service;
import org.kaazing.gateway.service.ServiceContext;
import org.kaazing.gateway.service.ServiceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gateway service of type "directory".
 */
public class HttpDirectoryService implements Service {
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

        Collection<URI> accepts = serviceContext.getAccepts();
        Collection<URI> failedAccepts = new HashSet<>();
        for (URI accept : accepts) {
            String path = accept.getPath();
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
            for (URI accept : accepts) {
                logger.warn(
                        "Ignoring user-defined file contents for {}clientaccesspolicy.xml, please modify configuration instead",
                        accept);
            }
        }

        handler.setServiceContext(serviceContext);
        handler.setBaseDir(directoryFile);
        handler.setWelcomeFile(welcomeFile);
        handler.setErrorPagesDir(errorPagesDir);

        String indexes = properties.get("options");
        if ((indexes != null) && "indexes".equalsIgnoreCase(indexes)) {
            handler.setIndexes(true);
        }

        // Register the Gateway's connection capabilities with the handler so that session counts are tracked
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
        File locationFile = null;
        if (location != null) {
            URI locationURI = URI.create(location);
            locationFile = new File(locationURI.getPath());
            if (locationURI.getScheme() == null) {
                if (location.charAt(0) == '/') {
                    location = location.substring(1);
                    locationFile = new File(rootDir, location);
                }
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
