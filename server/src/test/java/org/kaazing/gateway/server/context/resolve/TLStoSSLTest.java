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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kaazing.gateway.server.config.june2016.GatewayConfigDocument;
import org.kaazing.gateway.server.config.parse.GatewayConfigParser;

public class TLStoSSLTest {

    private static final String BASE_PATH = "org/kaazing/gateway/server/config/parse/data/";
    private static final String ACCEPT_XML = "gateway-config-tls-in-accept.xml";    
    private static final String ACCEPT_OPTIONS_XML = "gateway-config-tls-accept-options.xml";
    private static final String CONNECT_XML = "gateway-config-tls-in-connect.xml";
    private static final String CONNECT_OPTIONS_XML = "gateway-config-tls-connect-options.xml";

    private static GatewayConfigParser parser;
    private static GatewayContextResolver resolver;

    private File configFile;

    @BeforeClass
    public static void init() {
        parser = new GatewayConfigParser();

        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            File keyStoreFile = new File(classLoader.getResource("mykeystore.db").toURI());

            resolver = new GatewayContextResolver(new File(keyStoreFile.getParent()), null, null);
        } catch (Exception ex) {
            Assert.fail("Failed to load keystore.db, unable to init test due to exception: " + ex);
        }  
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

    @Test
    public void parseAndResolveTLSinAccept() throws Exception {
        configFile = createTempFileFromResource(
                String.format("%s%s", BASE_PATH, ACCEPT_XML));
        GatewayConfigDocument doc = parser.parse(configFile);
        Assert.assertNotNull(doc);
        resolver.resolve(doc);
    }
 
    @Test
    public void parseAndResolveTLSAcceptOptions() throws Exception {
        configFile = createTempFileFromResource(
                String.format("%s%s", BASE_PATH, ACCEPT_OPTIONS_XML));
        GatewayConfigDocument doc = parser.parse(configFile);
        Assert.assertNotNull(doc);
        resolver.resolve(doc);
    }

    @Test
    public void parseAndResolveTLSinConnect() throws Exception {
        configFile = createTempFileFromResource(
                String.format("%s%s", BASE_PATH, CONNECT_XML));
        GatewayConfigDocument doc = parser.parse(configFile);
        Assert.assertNotNull(doc);
        resolver.resolve(doc);
    }
 
    @Test
    public void parseAndResolveTLSConnectOptions() throws Exception {
        configFile = createTempFileFromResource(
                String.format("%s%s", BASE_PATH, CONNECT_OPTIONS_XML));
        GatewayConfigDocument doc = parser.parse(configFile);
        Assert.assertNotNull(doc);
        resolver.resolve(doc);
    }
}
