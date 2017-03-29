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

package org.kaazing.gateway.server.context.resolve;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.security.auth.login.AppConfigurationEntry;

import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.ConfigurationObserver;
import org.kaazing.gateway.server.config.june2016.GatewayConfigDocument;
import org.kaazing.gateway.server.config.parse.GatewayConfigParser;
import org.kaazing.gateway.server.context.GatewayContext;
import org.kaazing.test.util.MethodExecutionTrace;
/**
 * Unit tests for resolving gateway-config.xml.
 */
public class CustomOptionParserTest {
    @Rule
    public TestRule testExecutionTrace = new MethodExecutionTrace();

    private static GatewayConfigParser parser;
    private static GatewayContextResolver resolver;

    private File configFile;

    @BeforeClass
    public static void init() {
        parser = new GatewayConfigParser();

        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            File keyStoreFile =
                    new File(classLoader.getResource("keystore.db").toURI());

            resolver = new GatewayContextResolver(new File(keyStoreFile.getParent()), null, null);
        }
        catch (Exception ex) {
            Assert.fail("Failed to load keystore.db, unable to init test due to exception: " + ex);
        }
    }

    @After
    public void deleteConfigFile() {
        if ( configFile != null ) {
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


    @SuppressWarnings("unchecked")
    @Test
    public void shouldInsertNestedOptions() throws Exception {
        configFile =
                createTempFileFromResource("org/kaazing/gateway/server/config/parse/data/gateway-config-nested-options.xml");
        GatewayConfigDocument doc = parser.parse(configFile);

        ConfigurationObserver confObserver = ConfigurationObserver.newInstance();
        resolver.setObserver(confObserver);
        GatewayContext ctx = resolver.resolve(doc);

        AppConfigurationEntry[] entries = ctx.getRealm("demo").getConfiguration().getAppConfigurationEntry("demo");
        boolean foundCustomLoginModule = false;
        for (AppConfigurationEntry e : entries) {
            if (e.getLoginModuleName().equals("org.kaazing.gateway.server.context.resolve.CustomParserLoginModule")) {
                foundCustomLoginModule = true;
                Map<String, ?> options = e.getOptions();
                String customOption = (String) options.get("customOption");
                assertEquals("customValue", customOption);
            }
        }
        assertTrue(foundCustomLoginModule);
    }

}
