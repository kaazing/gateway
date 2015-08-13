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

package org.kaazing.gateway.server.util.version;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Attributes;

import org.slf4j.Logger;

/**
 * Class used for detecting if duplicate jar files are loaded by the gateway.
 */
public class DuplicateJarFinder {

    private static final String JAR_FILE_WITH_VERSION_LOGGING_MESSAGE = "The jar file {} with the version {} was loaded.";
    private static final String CONFLICTING_JARS_LOGGING_MESSAGE =
            "Failed to start because of conflicting versions of artifact on the classpath: {}.jar.";
    private static final String MANIFEST_VERSION = "Implementation-Version";
    private static final String MANIFEST_JAR_NAME = "Jar-Name";
    private static final String MANIFEST_PRODUCT = "Kaazing-Product";

    private ClassPathParser classPathParser;
    private Logger gatewayLogger;
    private Set<String> duplicateJars = new HashSet<String>();
    private Set<String> loadedJars = new HashSet<String>();

    public DuplicateJarFinder(Logger gatewayLogger) {
        this.gatewayLogger = gatewayLogger;
        this.classPathParser = new ClassPathParser();
    }

    /**
     * Parses the class path system attribute and the manifest files
     * and if there are duplicate jar a DuplicateJarsException is thrown.
     * @throws IOException
     * @throws DuplicateJarsException
     */
    public void findDuplicateJars() throws IOException, DuplicateJarsException {
        String[] classPathEntries = classPathParser.getClassPathEntries();
        for (String classPathEntry : classPathEntries) {
            parseManifestFileFromClassPathEntry(classPathEntry);
        }
        logErrorForDuplicateJars();
        throwExceptionIfDuplicateJarsAreFound();
    }

    private void parseManifestFileFromClassPathEntry(String classPathEntry) throws IOException {
        Attributes manifestAttributes = classPathParser.getManifestAttributesFromClassPathEntry(classPathEntry);
        String version = manifestAttributes.getValue(MANIFEST_VERSION);
        String jarName = manifestAttributes.getValue(MANIFEST_JAR_NAME);
        if (isKaazingProduct(jarName)) {
            logJarVersion(version, jarName);
            checkDuplicateJar(jarName);
        }
    }

    private void logJarVersion(String version, String jarName) {
        gatewayLogger.debug(JAR_FILE_WITH_VERSION_LOGGING_MESSAGE, jarName, version);
    }

    private void checkDuplicateJar(String jarName) {
        if (!loadedJars.add(jarName)) {
            duplicateJars.add(jarName);
        }
    }

    private boolean isKaazingProduct(String product) {
        return product != null && (product.contains("org.kaazing") || product.contains("com.kaazing"));
    }

    private void logErrorForDuplicateJars() {
        for (String jarName : duplicateJars) {
            gatewayLogger.error(CONFLICTING_JARS_LOGGING_MESSAGE, jarName);
        }
    }

    private void throwExceptionIfDuplicateJarsAreFound() throws DuplicateJarsException {
        if (!duplicateJars.isEmpty()) {
            throw new DuplicateJarsException();
        }
    }

    /**
     * This method is only for testing purposes
     * @param classPathParser - the mock for the classPathParser
     */
    void setClassPathParser(ClassPathParser classPathParser) {
        this.classPathParser = classPathParser;
    }

}
