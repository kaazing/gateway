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

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

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

        File file = File.createTempFile("gateway-config", ".xml");
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

    // see http://jira.kaazing.wan/browse/KG-6390
    @Test
    public void shouldParse201409Namespace()
        throws Exception {

        File configFile = null;
        try {
            configFile = createTempFileFromResource("org/kaazing/gateway/server/config/parse/data/gateway-config-201409.xml");
            parser.parse(configFile);

        } finally {
            if (configFile != null) {
                configFile.delete();
            }
        }
    }
}
