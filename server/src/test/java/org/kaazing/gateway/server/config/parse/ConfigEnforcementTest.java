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
package org.kaazing.gateway.server.config.parse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.log4j.PropertyConfigurator;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit tests for 4.0 configuration enforcement (KG-9163)
 */
public class ConfigEnforcementTest {
    private static GatewayConfigParser parser;

    private static final boolean DEBUG = false;

    @BeforeClass
    public static void init() {
        if (DEBUG) {
            PropertyConfigurator.configure("src/test/resources/log4j-trace.properties");
        }

        parser = new GatewayConfigParser();
    }

    @Test(expected = GatewayConfigParserException.class)
    public void testNoBalancedService() throws Exception {
        File configFile = null;
        try {
            configFile =
                    createTempFileFromResource("org/kaazing/gateway/server/config/parse/data/gateway-config-test-no-balanced" +
                            "-service.xml");
            parser.parse(configFile);
        } finally {
            if (configFile != null) {
                configFile.delete();
            }
        }
    }

    @Test
    public void testValidBalancedService() throws Exception {
        File configFile = null;
        try {
            configFile =
                    createTempFileFromResource("org/kaazing/gateway/server/config/parse/data/gateway-config-test-valid-balanced" +
                            "-service.xml");
            parser.parse(configFile);
        } finally {
            if (configFile != null) {
                configFile.delete();
            }
        }
    }

    @Test
    public void testSeptSchemaValidBalancedSvc() throws Exception {
        File configFile = null;
        try {
            configFile =
                    createTempFileFromResource("org/kaazing/gateway/server/config/parse/data/gateway-config-test-sep-schema" +
                            "-valid-balanced-svc.xml");
            parser.parse(configFile);
        } finally {
            if (configFile != null) {
                configFile.delete();
            }
        }
    }

    @Test(expected = GatewayConfigParserException.class)
    public void testOrphanedBalancer() throws Exception {
        File configFile = null;
        try {
            configFile =
                    createTempFileFromResource("org/kaazing/gateway/server/config/parse/data/gateway-config-test-orphaned" +
                            "-balancer.xml");
            parser.parse(configFile);
        } finally {
            if (configFile != null) {
                configFile.delete();
            }
        }
    }

    @Test
    public void testWSandWSS() throws Exception {
        File configFile = null;
        try {
            configFile =
                    createTempFileFromResource("org/kaazing/gateway/server/config/parse/data/gateway-config-test-ws-and-wss.xml");
            parser.parse(configFile);
        } finally {
            if (configFile != null) {
                configFile.delete();
            }
        }
    }

    @Test(expected = GatewayConfigParserException.class)
    public void testWSandOrphanedWSS() throws Exception {
        File configFile = null;
        try {
            configFile =
                    createTempFileFromResource("org/kaazing/gateway/server/config/parse/data/gateway-config-test-ws-and" +
                            "-orphaned-wss.xml");
            parser.parse(configFile);
        } finally {
            if (configFile != null) {
                configFile.delete();
            }
        }
    }

    @Test(expected = GatewayConfigParserException.class)
    public void testWSandWSSMismatch() throws Exception {
        File configFile = null;
        try {
            configFile =
                    createTempFileFromResource("org/kaazing/gateway/server/config/parse/data/gateway-config-test-ws-and-wss" +
                            "-mismatch.xml");
            parser.parse(configFile);
        } finally {
            if (configFile != null) {
                configFile.delete();
            }
        }
    }

    @Test(expected = GatewayConfigParserException.class)
    public void testAcceptAndBalanceMatch() throws Exception {
        File configFile = null;
        try {
            // acceptURI and balanceURI match, which would create an infinite redirect loop so an error should be thrown
            configFile =
                    createTempFileFromResource("org/kaazing/gateway/server/config/parse/data/gateway-config-accept-and-balance" +
                            "-match.xml");
            parser.parse(configFile);
        } finally {
            if (configFile != null) {
                configFile.delete();
            }
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
}
