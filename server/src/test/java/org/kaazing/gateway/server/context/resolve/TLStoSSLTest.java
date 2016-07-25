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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kaazing.gateway.server.test.Gateway;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.gateway.service.ServiceFactory;
import org.kaazing.gateway.service.echo.EchoService;
import org.kaazing.gateway.server.config.june2016.GatewayConfigDocument;
import org.kaazing.gateway.server.config.parse.GatewayConfigParser;

public class TLStoSSLTest {

    private static GatewayConfigParser parser;
    private static GatewayContextResolver resolver;

    private File configFile;
    private File keyStoreFile;
    private File keyStorePasswordFile;
    private File trustStoreFile;

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

    @Before
    public void setAllowedServices() throws Exception {
        Set<String> serviceList = new HashSet<>();
        serviceList.add("directory");
        serviceList.add("proxy");

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        keyStoreFile = new File(classLoader.getResource("mykeystore.db").toURI());
        keyStorePasswordFile = new File(classLoader.getResource("mykeystore.pw").toURI());
        trustStoreFile = new File(classLoader.getResource("truststore.db").toURI());
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

        final String replacedContent = MessageFormat.format(baos.toString("UTF-8"), (Object[])values);
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
    public void parseAndResolveTLSinAccept() throws Exception {
        configFile = createTempFileFromResource(
                "org/kaazing/gateway/server/config/parse/data/gateway-config-tls-in-accept.xml");
        GatewayConfigDocument doc = parser.parse(configFile);
        Assert.assertNotNull(doc);
        resolver.resolve(doc);
    }
    
    @Test
    public void parseAndResolveTLSAcceptOptions() throws Exception {
        configFile = createTempFileFromResource(
                "org/kaazing/gateway/server/config/parse/data/gateway-config-tls-accept-options.xml");
        GatewayConfigDocument doc = parser.parse(configFile);
        Assert.assertNotNull(doc);
        resolver.resolve(doc);
    }

    @Test
    public void parseAndResolveTLSinConnect() throws Exception {
        configFile = createTempFileFromResource(
                "org/kaazing/gateway/server/config/parse/data/gateway-config-tls-in-connect.xml");
        GatewayConfigDocument doc = parser.parse(configFile);
        Assert.assertNotNull(doc);
        resolver.resolve(doc);
    }
    
    @Test
    public void parseAndResolveTLSConnectOptions() throws Exception {
        configFile = createTempFileFromResource(
                "org/kaazing/gateway/server/config/parse/data/gateway-config-tls-accept-options.xml");
        GatewayConfigDocument doc = parser.parse(configFile);
        Assert.assertNotNull(doc);
        resolver.resolve(doc);
    }
}
