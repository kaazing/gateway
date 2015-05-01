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

package org.kaazing.gateway.server.config.parse;

import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.kaazing.gateway.server.config.sep2014.GatewayConfigDocument;
import org.kaazing.gateway.util.http.DefaultUtilityHttpClient;

/**
 * Unit tests for parsing gateway-config.xml.
 */
public class GatewayConfigParserTest {
    private static GatewayConfigParser parser;

    @BeforeClass
    public static void init() {
        parser = new GatewayConfigParser();
    }

    @Before
    public void setProperties() {
        System.setProperty("service.domain", "localhost");
    }

    @After
    public void unsetProperties() {
        System.setProperty("service.domain", "");
    }

    private File createTempFileFromResource(String resourceName)
            throws IOException {

        File file = File.createTempFile("gateway-config", "xml");
        ClassLoader classLoader = Thread.currentThread()
                .getContextClassLoader();
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

    @Test
    // KG-3472
    public void testBroadcastPropertiesWithAccepts() throws Exception {
        File configFile = null;
        try {
            configFile = createTempFileFromResource("org/kaazing/gateway/server/config/parse/data/gateway-config-kg3472.xml");
            parser.parse(configFile);

        } finally {
            if (configFile != null) {
                configFile.delete();
            }
        }
    }

    @Test
    // KG-4462
    public void testSslCiphersOptions() throws Exception {
        File configFile = null;
        try {
            configFile = createTempFileFromResource("org/kaazing/gateway/server/config/parse/data/gateway-config-kg4462.xml");
            parser.parse(configFile);

        } finally {
            if (configFile != null) {
                configFile.delete();
            }
        }
    }

    @Test
    public void testWillParseServiceDefaults() throws Exception {
        File configFile = null;
        try {
            configFile = createTempFileFromResource(
                    "org/kaazing/gateway/server/config/parse/data/gateway-config-with-service-defaults.xml");
            parser.parse(configFile);

        } finally {
            if (configFile != null) {
                configFile.delete();
            }
        }
    }

    @Test
    // see http://jira.kaazing.wan/browse/KG-6189
    public void shouldParseExcaliburSslCiphersConnectOptions() throws Exception {

        File configFile = null;
        try {
            configFile = createTempFileFromResource("org/kaazing/gateway/server/config/parse/data/gateway-config-kg6189.xml");
            parser.parse(configFile);

        } finally {
            if (configFile != null) {
                configFile.delete();
            }
        }
    }

    @Test
    public void testTcpMaxOutboundRate() throws Exception {
        File configFile = null;
        try {
            configFile = createTempFileFromResource("org/kaazing/gateway/server/config/parse/data/gateway-config-data-rate.xml");
            parser.parse(configFile);
        } finally {
            if (configFile != null) {
                configFile.delete();
            }
        }
    }

    @Test
    public void shouldParseInSequenceAcceptOptions() throws Exception {
        File configFile = null;
        try {
            configFile =
                    createTempFileFromResource("org/kaazing/gateway/server/config/parse/data/gateway-config-in-sequence-accept" +
                            "-options.xml");
            parser.parse(configFile);

        } finally {
            if (configFile != null) {
                configFile.delete();
            }
        }
    }

    @Test
    public void shouldParseOutOfSequenceAcceptOptions() throws Exception {
        File configFile = null;
        try {
            configFile =
                    createTempFileFromResource("org/kaazing/gateway/server/config/parse/data/gateway-config-out-of-sequence" +
                            "-accept-options.xml");
            parser.parse(configFile);

        } finally {
            if (configFile != null) {
                configFile.delete();
            }
        }
    }

    @Test
    public void shouldParseInSequenceConnectOptions() throws Exception {
        File configFile = null;
        try {
            configFile =
                    createTempFileFromResource("org/kaazing/gateway/server/config/parse/data/gateway-config-in-sequence-connect" +
                            "-options.xml");
            parser.parse(configFile);

        } finally {
            if (configFile != null) {
                configFile.delete();
            }
        }
    }

    @Test
    public void shouldParseOutOfSequenceConnectOptions() throws Exception {
        File configFile = null;
        try {
            configFile =
                    createTempFileFromResource("org/kaazing/gateway/server/config/parse/data/gateway-config-out-of-sequence" +
                            "-connect-options.xml");
            parser.parse(configFile);

        } finally {
            if (configFile != null) {
                configFile.delete();
            }
        }
    }

    @Test
    public void testValidWsInactivityTimeout() throws Exception {
        File configFile = null;
        try {
            configFile =
                    createTempFileFromResource("org/kaazing/gateway/server/config/parse/data/gateway-config-valid-ws-inactivity" +
                            "-timeout.xml");
            parser.parse(configFile);

        } finally {
            if (configFile != null) {
                configFile.delete();
            }
        }
    }

    @Test(expected = GatewayConfigParserException.class)
    @Ignore("XSD no longer validates accept-options types")
    public void testInvalidWsInactivityTimeout() throws Exception {
        File configFile = null;
        try {
            configFile =
                    createTempFileFromResource("org/kaazing/gateway/server/config/parse/data/gateway-config-invalid-ws" +
                            "-inactivity-timeout.xml");
            parser.parse(configFile);

        } finally {
            if (configFile != null) {
                configFile.delete();
            }
        }
    }

    @Test(expected = GatewayConfigParserException.class)
    @Ignore("XSD no longer validates connect-options types")
    public void testInvalidWsInactivityTimeoutAsConnectOption()
            throws Exception {
        File configFile = null;
        try {
            configFile =
                    createTempFileFromResource("org/kaazing/gateway/server/config/parse/data/gateway-config-invalid-ws" +
                            "-inactivity-timeout-connect.xml");
            parser.parse(configFile);

        } finally {
            if (configFile != null) {
                configFile.delete();
            }
        }
    }

    @Test
    public void testParamenterWithinComments() throws Exception {
        File configFile = null;
        try {
            configFile =
                    createTempFileFromResource("org/kaazing/gateway/server/config/parse/data/gateway-config-variable-comments" +
                            ".xml");
            parser.parse(configFile);

        } finally {
            if (configFile != null) {
                configFile.delete();
            }
        }
    }

    @Test(expected = RuntimeException.class)
    public void testUnknownService() throws Exception {
        File configFile = null;
        try {
            configFile =
                    createTempFileFromResource("org/kaazing/gateway/server/config/parse/data/gateway-config-unknownservice.xml");
            parser.parse(configFile);
        } finally {
            if (configFile != null) {
                configFile.delete();
            }
        }
    }

    @Test(expected = GatewayConfigParserException.class)
    public void testMultipleClusterElementsThrowsError() throws Exception {
        File configFile = null;
        try {
            configFile =
                    createTempFileFromResource("org/kaazing/gateway/server/config/parse/data/gateway-config-multiple-cluster" +
                            ".xml");
            parser.parse(configFile);
        } finally {
            if (configFile != null) {
                configFile.delete();
            }
        }
    }

    @Test
    public void testCanConvertAwsHostToAwsHostnameIfAvailable() {
        boolean onAWS = false;
        DefaultUtilityHttpClient httpClient = new DefaultUtilityHttpClient();
        try {
            httpClient
                    .performGetRequest("http://169.254.169.254/2014-02-25/meta-data/");
            onAWS = true;
        } catch (Exception e) {
            onAWS = false;
        }

        File configFile = null;
        GatewayConfigDocument doc = null;
        try {
            configFile = createTempFileFromResource("org/kaazing/gateway/server/config/parse/data/gateway-config-aws-host.xml");
            doc = parser.parse(configFile);
        } catch (Exception e) {
            if (!onAWS) {
                Assert.assertTrue(e instanceof GatewayConfigParserException);
            } else {
                fail("Caught unexpected exception running on AWS " + e);
            }
        } finally {
            if (configFile != null) {
                configFile.delete();
            }
        }
        if (onAWS) {
            String accept = doc.getGatewayConfig().getServiceArray(0)
                    .getAcceptArray(0);
            System.out.println("AWS Accept was \"" + accept + "\"");
            Assert.assertFalse("accept is null", accept == null);
            Assert.assertFalse("accept is empty", "".endsWith(accept));
            Assert.assertFalse("Found pattern in accept",
                    accept.contains("${cloud.host}"));
            Pattern pattern = Pattern.compile("\\s");
            Matcher matcher = pattern.matcher(accept);
            boolean found = matcher.find();
            Assert.assertFalse("Found whitespace in accept", found);
        }

    }

    @Test
    public void testCanConvertHostname() {
        File configFile = null;
        GatewayConfigDocument doc = null;
        try {
            configFile = createTempFileFromResource("org/kaazing/gateway/server/config/parse/data/gateway-config-hostname.xml");
            doc = parser.parse(configFile);
        } catch (Exception e) {
                fail("Caught unexpected exception parsing: " + e);
        } finally {
            if (configFile != null) {
                configFile.delete();
            }
        }
        String accept = doc.getGatewayConfig().getServiceArray(0)
                .getAcceptArray(0);
        Assert.assertFalse("accept is null", accept == null);
        Assert.assertFalse("accept is empty", "".endsWith(accept));
        Assert.assertFalse("Found pattern in accept",
                accept.contains("${hostname}"));
        Pattern pattern = Pattern.compile("\\s");
        Matcher matcher = pattern.matcher(accept);
        boolean found = matcher.find();
        Assert.assertFalse("Found whitespace in accept", found);
    }

    @Test
    public void testCanConvertAwsInstanceIdIfAvailable() {
        boolean onAWS = false;
        DefaultUtilityHttpClient httpClient = new DefaultUtilityHttpClient();
        try {
            httpClient
                    .performGetRequest("http://169.254.169.254/2014-02-25/meta-data/");
            onAWS = true;
        } catch (Exception e) {
            onAWS = false;
        }

        File configFile = null;
        GatewayConfigDocument doc = null;
        try {
            configFile = createTempFileFromResource("org/kaazing/gateway/server/config/parse/data/gateway-config-aws-host.xml");
            doc = parser.parse(configFile);
        } catch (Exception e) {
            if (!onAWS) {
                Assert.assertTrue(e instanceof GatewayConfigParserException);
            } else {
                fail("Caught unexpected exception running on AWS " + e);
            }
        } finally {
            if (configFile != null) {
                configFile.delete();
            }
        }
        if (onAWS) {
            String accept = doc.getGatewayConfig().getServiceArray(0)
                    .getAcceptArray(0);
            System.out.println("AWS Accept was \"" + accept + "\"");
            Assert.assertFalse("accept is null", accept == null);
            Assert.assertFalse("accept is empty", "".endsWith(accept));
            Assert.assertFalse("Found pattern in accept",
                    accept.contains("${cloud.instanceId}"));
            Pattern pattern = Pattern.compile("\\s");
            Matcher matcher = pattern.matcher(accept);
            boolean found = matcher.find();
            Assert.assertFalse("Found whitespace in accept", found);
        }

    }

    @Test
    public void testWontConvertAwsHostToAwsHostnameIfUserOverridesProperties()
            throws Exception {
        File configFile = null;
        GatewayConfigDocument doc = null;
        try {
            configFile =
                    createTempFileFromResource("org/kaazing/gateway/server/config/parse/data/gateway-config-aws-host-with-useroverride.xml");
            doc = parser.parse(configFile);

        } finally {
            if (configFile != null) {
                configFile.delete();
            }
        }
        String accept = doc.getGatewayConfig().getServiceArray(0)
                .getAcceptArray(0);
        assumeTrue("accept equals localhost", "localhost".equals(accept));
    }

    @Test
    public void testWontComplainOfPropertiesInComments() throws Exception {
        File configFile = null;
        GatewayConfigDocument doc = null;
        try {
            configFile =
                    createTempFileFromResource("org/kaazing/gateway/server/config/parse/data/gateway-config-aws-host-commented-out.xml");
            doc = parser.parse(configFile);

        } finally {
            if (configFile != null) {
                configFile.delete();
            }
        }
        String accept = doc.getGatewayConfig().getServiceArray(0)
                .getAcceptArray(0);
        assumeTrue("accept equals localhost", "localhost".equals(accept));
    }

}
