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

package org.kaazing.gateway.server.context.resolve;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Appender;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.kaazing.gateway.security.CrossSiteConstraintContext;
import org.kaazing.gateway.server.Gateway;
import org.kaazing.gateway.server.config.parse.GatewayConfigParser;
import org.kaazing.gateway.server.config.sep2014.GatewayConfigDocument;
import org.kaazing.gateway.server.context.GatewayContext;
import org.kaazing.gateway.server.test.MethodExecutionTrace;
import org.kaazing.gateway.service.ServiceContext;

/**
 * Unit tests for resolving gateway-config.xml.
 */
public class GatewayContextResolverTest {
    @Rule
    public MethodRule testExecutionTrace = new MethodExecutionTrace();

    private static GatewayConfigParser parser;
    private static GatewayContextResolver resolver;

    private File configFile;
    private File keyStoreFile;
    private File keyStorePasswordFile;
    private File trustStoreFile;

    private static boolean DEBUG = false;

    @BeforeClass
    public static void init() {
        if (DEBUG) {
            PropertyConfigurator.configure("src/test/resources/log4j-trace.properties");
        }

        parser = new GatewayConfigParser();

        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            URL keystoreDB = classLoader.getResource("keystore.db");
            File keyStoreFile = new File(classLoader.getResource("keystore.db").toURI());

            resolver = new GatewayContextResolver(new File(keyStoreFile.getParent()), null, null);
        } catch (Exception ex) {
            Assert.fail("Failed to load keystore.db, unable to init test due to exception: " + ex);
        }
    }

    @Before
    public void setAllowedServices()
            throws Exception {

        Set<String> serviceList = new HashSet<>();
        serviceList.add("echo");
        serviceList.add("balancer");

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        keyStoreFile = new File(classLoader.getResource("keystore.db").toURI());
        keyStorePasswordFile = new File(classLoader.getResource("keystore.pw").toURI());
        trustStoreFile = new File(classLoader.getResource("truststore-JCEKS.db").toURI());
    }

    @After
    public void deleteConfigFile() {
        if (configFile != null) {
            configFile.delete();
        }
    }

    private File createTempFileFromResource(String resourceName) throws IOException {
        File file = File.createTempFile("gateway-config", "xml");
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream is = classLoader.getResource(resourceName).openStream();
        FileOutputStream fos = new FileOutputStream(file);
        int datum;
        while ((datum = is.read()) != -1) {
            fos.write(datum);
        }
        fos.flush();
        fos.close();
        return file;
    }

    private File createTempFileFromResource(String resourceName, String... values) throws IOException {
        File file = File.createTempFile("gateway-config", "xml");
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream is = classLoader.getResource(resourceName).openStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream(is.available());
        int datum;
        while ((datum = is.read()) != -1) {
            baos.write(datum);
        }
        is.close();

        final String replacedContent = MessageFormat.format(baos.toString("UTF-8"), values);
        ByteArrayInputStream bais = new ByteArrayInputStream(replacedContent.getBytes("UTF-8"));

        FileOutputStream fos = new FileOutputStream(file);
        while ((datum = bais.read()) != -1) {
            fos.write(datum);
        }
        fos.flush();
        fos.close();

        return file;
    }

    @Test
    public void testLowerCaseOfResolvedServices() throws Exception {
        File configFile = null;
        try {
            configFile = createTempFileFromResource("org/kaazing/gateway/server/context/parse/data/gateway-config-mixedcase.xml");
            org.kaazing.gateway.server.config.sep2014.GatewayConfigDocument doc = parser.parse(configFile);
            GatewayContext ctx = resolver.resolve(doc);

            Collection<? extends ServiceContext> services = ctx.getServices();
            for (ServiceContext service : services) {
                // validate that the accepts have lower-case host names
                Collection<URI> acceptURIs = service.getAccepts();
                for (URI acceptURI : acceptURIs) {
                    Assert.assertTrue(acceptURI.getHost().equals(acceptURI.getHost().toLowerCase()));
                }

                // validate that the cross-site-constraints have lower-case host names
                Map<URI, ? extends Map<String, ? extends CrossSiteConstraintContext>> crossSiteConstraints =
                        service.getCrossSiteConstraints();
                for (URI key : crossSiteConstraints.keySet()) {
                    Map<String, ? extends CrossSiteConstraintContext> crossSiteConstraintsByURI = crossSiteConstraints.get(key);
                    for (CrossSiteConstraintContext crossSiteConstraint : crossSiteConstraintsByURI.values()) {
                        String allowOrigin = crossSiteConstraint.getAllowOrigin();
                        if (!"*".equals(allowOrigin)) {
                            URI originURI = URI.create(allowOrigin);
                            Assert.assertTrue(originURI.getHost().equals(originURI.getHost().toLowerCase()));
                        }
                    }
                }
            }
        } finally {
            if (configFile != null) {
                configFile.delete();
            }
        }
    }

    @Test // KG-2250/KG-2251
    public void testWrongKeystoreType() throws Exception {
        File configFile = null;
        boolean sawExpectedEx = false;

        String expectedLogMessage = "Exception .* caught loading file .*keystore.db.* you may need to specify " +
                "\"<type>JCEKS</type>\" in the gateway configuration file";
        String expected = "Invalid keystore format";
        LogMessageInspector inspector = LogMessageInspector.create(Gateway.class);

        try {
            configFile =
                    createTempFileFromResource("org/kaazing/gateway/server/config/parse/data/gateway-config-wrong-keystore-type" +
                            ".xml");
            GatewayConfigDocument doc = parser.parse(configFile);
            Assert.assertNotNull(doc);
            resolver.resolve(doc);

        } catch (IOException e) {
            if (e.getMessage().contains(expected)) {
                sawExpectedEx = true;
            }
            final List<LoggingEvent> grepResult = inspector.grep(org.apache.log4j.Level.ERROR, expectedLogMessage);
            Assert.assertEquals("Expected to find a matching error message", 1, grepResult.size());

        } finally {
            if (configFile != null) {
                configFile.delete();
            }
        }

        Assert.assertTrue(String.format("Did not see expected IOException with message '%s'", expected), sawExpectedEx);
    }

    @Test // KG-2250/KG-2251
    public void testWrongTruststoreType() throws Exception {
        File configFile = null;
        boolean sawExpectedEx = false;

        String expectedLogMessage = "Exception .* caught loading file .*truststore-JCEKS.db.* you may need to specify " +
                "\"<type>JCEKS</type>\" in the gateway configuration file";
        String expected = "Invalid keystore format";
        LogMessageInspector inspector = LogMessageInspector.create(Gateway.class);

        try {
            configFile =
                    createTempFileFromResource("org/kaazing/gateway/server/config/parse/data/gateway-config-wrong-truststore" +
                            "-type.xml");
            GatewayConfigDocument doc = parser.parse(configFile);
            Assert.assertNotNull(doc);
            resolver.resolve(doc);

        } catch (IOException e) {
            if (e.getMessage().contains(expected)) {
                sawExpectedEx = true;
            }
            final List<LoggingEvent> grepResult = inspector.grep(org.apache.log4j.Level.ERROR, expectedLogMessage);
            Assert.assertEquals("Expected to find a matching error message", 1, grepResult.size());

        } finally {
            if (configFile != null) {
                configFile.delete();
            }
        }

        Assert.assertTrue(String
                .format("Did not see expected IllegalArgumentException with message '%s'", expected), sawExpectedEx);
    }

    @Test // KG-5510
    public void shouldAcceptKeyStoreFileAbsolutePath()
            throws Exception {

        File configFile =
                createTempFileFromResource("org/kaazing/gateway/server/context/parse/data/gateway-config-abs-path-security" +
                                "-files.xml",
                        keyStoreFile.getAbsolutePath(),
                        "keystore.pw",
                        "truststore-JCEKS.db");
        GatewayConfigDocument doc = parser.parse(configFile);
        Assert.assertNotNull(doc);
        resolver.resolve(doc);
    }

    @Test // KG-5510
    public void shouldAcceptKeyStorePasswordFileAbsolutePath()
            throws Exception {

        File configFile =
                createTempFileFromResource("org/kaazing/gateway/server/context/parse/data/gateway-config-abs-path-security" +
                                "-files.xml",
                        "keystore.db",
                        keyStorePasswordFile.getAbsolutePath(),
                        "truststore-JCEKS.db");
        GatewayConfigDocument doc = parser.parse(configFile);
        Assert.assertNotNull(doc);
        resolver.resolve(doc);
    }

    @Test // KG-5510
    public void shouldAcceptTrustStoreFileAbsolutePath()
            throws Exception {

        File configFile =
                createTempFileFromResource("org/kaazing/gateway/server/context/parse/data/gateway-config-abs-path-security" +
                                "-files.xml",
                        "keystore.db",
                        "keystore.pw",
                        trustStoreFile.getAbsolutePath());
        GatewayConfigDocument doc = parser.parse(configFile);
        Assert.assertNotNull(doc);
        resolver.resolve(doc);
    }

    // See http://jira.kaazing.wan/browse/KG-7237
    @Test(expected = RuntimeException.class)
    public void shouldNotResolveDuplicateRealms()
            throws Exception {
        File configFile =
                createTempFileFromResource("org/kaazing/gateway/server/context/parse/data/gateway-config-duplicate-realms.xml");
        GatewayConfigDocument doc = parser.parse(configFile);
        Assert.assertNotNull(doc);
        resolver.resolve(doc);
    }

    // The following replaces code we used to have GatewayContextResolverTest (rev 25379) that created a log4j Hierarchy
    // and called LogManager.setRepositorySelector in an effort to force log4j to return instances of our BufferedLogger
    // class so we can mess with message logging. This technique is not reliable because if any code that executes before
    // us gets hold of a logger with a particular name (e.g. by caling org.slf4j.LoggerFactory.getLogger(Gateway.class))
    // then that logger instance (which would not be our BufferedLogger) gets cached in org.slf4j.impl.Log4jLoggerFactory
    // and will always be used from then on, which defeats our strategy!
    public static class LogMessageInspector {
        private org.apache.log4j.Logger logger;
        private BufferedAppender appender;

        private LogMessageInspector(String loggerName) {
            logger = org.apache.log4j.Logger.getLogger(loggerName);
            removeBufferedAppenders(logger);
            appender = new BufferedAppender();
            logger.addAppender(appender);
        }

        public static LogMessageInspector create(String loggerName) {
            return new LogMessageInspector(loggerName);
        }

        public static LogMessageInspector create(Class<?> claz) {
            return create(claz.getCanonicalName());
        }

        public List<LoggingEvent> grep(org.apache.log4j.Level level, String messageRegex) {
            return appender.grep(level, messageRegex);
        }

        private void removeBufferedAppenders(org.apache.log4j.Logger logger) {
            Enumeration<?> appenders = logger.getAllAppenders();
            while (appenders.hasMoreElements()) {
                Appender appender = (Appender) appenders.nextElement();
                if (appender instanceof BufferedAppender) {
                    logger.removeAppender(appender);
                }
            }
        }

    }

    static class BufferedAppender extends org.apache.log4j.AppenderSkeleton {
        private List<LoggingEvent> events = new ArrayList<>();

        private BufferedAppender() {
            super();
        }

        @Override
        protected void append(LoggingEvent event) {
            events.add(event);
        }

        public List<LoggingEvent> grep(org.apache.log4j.Level level, String message) {
            List<LoggingEvent> result = new ArrayList<>();
            for (LoggingEvent event : events) {
                if (event.getLevel() == level && event.getMessage() != null && message != null
                        && event.getMessage().toString().matches(message)) {
                    result.add(event);
                }
            }
            return result;
        }

        @Override
        public void close() {
            events = null;
        }

        @Override
        public boolean requiresLayout() {
            return false;
        }
    } // end BufferedAppender

}
