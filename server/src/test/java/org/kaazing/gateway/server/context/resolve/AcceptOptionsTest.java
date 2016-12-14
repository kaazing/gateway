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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.kaazing.gateway.server.config.parse.GatewayConfigParser;
import org.kaazing.gateway.server.config.parse.GatewayConfigParserException;
import org.kaazing.gateway.server.config.june2016.GatewayConfigDocument;
import org.kaazing.gateway.server.config.june2016.ServiceAcceptOptionsType;
import org.kaazing.gateway.service.AcceptOptionsContext;
import org.kaazing.gateway.service.TransportOptionNames;

/**
 * Unit tests for resolving gateway-config.xml.
 */
public class AcceptOptionsTest {
    public static final long UNLIMITED_MAX_OUTPUT_RATE = 0xFFFFFFFFL;

    private static GatewayConfigParser parser;
    private static GatewayContextResolver resolver;

    @BeforeClass
    public static void init() {
        parser = new GatewayConfigParser();

        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            File keyStoreFile = new File(classLoader.getResource("keystore.db").toURI());

            resolver = new GatewayContextResolver(new File(keyStoreFile.getParent()), null, null);
        } catch (Exception ex) {
            Assert.fail("Failed to load keystore.db, unable to init test due to exception: " + ex);
        }
    }

    @Before
    public void setAllowedServices() {
        Set<String> serviceList = new HashSet<>();
        serviceList.add("echo");
        serviceList.add("directory");
    }

    @Test
    public void testSslCiphersOption() throws Exception {
        expectSuccess("ssl.ciphers", "  FOO,BAR ", "ssl.ciphers", new String[]{"FOO", "BAR"});
        expectParseFailure("ssl.ciphers", "FOO, BAR");
    }

    @Test
    public void testTlsCiphersOption() throws Exception {
        expectSuccess("tls.ciphers", "  FOO,BAR ", "ssl.ciphers", new String[]{"FOO", "BAR"});
        expectParseFailure("tls.ciphers", "FOO, BAR");
    }

    @Test
    public void testHttpKeepAliveTimeoutOption() throws Exception {
        // expect default if 0 is specified
        expectSuccess("http.keepalive.timeout", "0 minutes", "http[http/1.1].keepAliveTimeout", 30);

        expectSuccess("http.keepalive.timeout", "10 seconds", "http[http/1.1].keepAliveTimeout", 10);
        expectSuccess("http.keepalive.timeout", "10 minutes", "http[http/1.1].keepAliveTimeout", 600);
        expectSuccess("http.keepalive.timeout", "0.5 minutes", "http[http/1.1].keepAliveTimeout", 30);
        //expectSuccess("http.keepalive.timeout", "", "http[http/1.1].keepAliveTimeout", 30);

        expectParseFailure("http.keepalive.timeout", "-1 seconds");
        expectParseFailure("http.keepalive.timeout", "abc");
        expectParseFailure("http.keepalive.timeout", null);
    }

    @Test
    public void testHttpTransportOption() throws Exception {
        expectSuccess("http.transport", "tcp://127.0.0.1:80", "http[http/1.1].transport", "tcp://127.0.0.1:80");
        expectSuccess("http.transport", "tcp://127.0.0.1:80", "http.transport", null);
    }

    @Test
    public void testTlsTransportOption() throws Exception {
        expectSuccess("tls.transport", "tcp://127.0.0.1:80", "ssl.transport", "tcp://127.0.0.1:80");
        expectSuccess("tls.transport", "tcp://127.0.0.1:80", "tls.transport", null);
    }

    @Test
    @Ignore("XSD no longer validates accept-options types")
    public void testWsMaximumMessageSizeOption() throws Exception {
        expectSuccess("ws.maximum.message.size", "10k", "ws.maxMessageSize", 10240);
        expectSuccess("ws.maximum.message.size", "10m", "ws.maxMessageSize", 10485760);
        expectSuccess("ws.maximum.message.size", "0", "ws.maxMessageSize", 0);

        expectParseFailure("ws.maximum.message.size", "-1");
        expectParseFailure("ws.maximum.message.size", "10kb");
        expectParseFailure("ws.maximum.message.size", "");
        expectParseFailure("ws.maximum.message.size", null);
    }

    @Test
    @Ignore("XSD no longer validates accept-options types")
    public void testTcpMaximumOutboundRateOption() throws Exception {
        expectSuccess("tcp.maximum.outbound.rate", "10KiB/s", TransportOptionNames.TCP_MAXIMUM_OUTBOUND_RATE, 10240L);
        expectSuccess("tcp.maximum.outbound.rate", "10MiB/s", TransportOptionNames.TCP_MAXIMUM_OUTBOUND_RATE, 10485760L);
        expectSuccess("tcp.maximum.outbound.rate", "10kB/s", TransportOptionNames.TCP_MAXIMUM_OUTBOUND_RATE, 10000L);
        expectSuccess("tcp.maximum.outbound.rate", "10MB/s", TransportOptionNames.TCP_MAXIMUM_OUTBOUND_RATE, 10000000L);

        // would overflow INT, but we are long
        expectSuccess("tcp.maximum.outbound.rate", "214748364MiB/s", TransportOptionNames.TCP_MAXIMUM_OUTBOUND_RATE,
                UNLIMITED_MAX_OUTPUT_RATE);

        expectRuntimeFailure("tcp.maximum.outbound.rate", (Long.MAX_VALUE / 10L) + "MiB/s");
        expectRuntimeFailure("tcp.maximum.outbound.rate", "0");
        expectParseFailure("tcp.maximum.outbound.rate", "-1");
        expectParseFailure("tcp.maximum.outbound.rate", "10kb");
        expectParseFailure("tcp.maximum.outbound.rate", "");
        expectParseFailure("tcp.maximum.outbound.rate", null);
    }

    @Test
    public void testWsInactivityTimeout() throws Exception {
        expectSuccess("ws.inactivity.timeout", "60", "ws.inactivityTimeout", 60000L, "http[http/1.1].keepAliveTimeout", 60);
        expectSuccess("ws.inactivity.timeout", "60s", "ws.inactivityTimeout", 60000L, "http[http/1.1].keepAliveTimeout", 60);
        //https://github.com/kaazing/gateway/issues/595
        expectSuccess("ws.inactivity.timeout", "60000ms", "ws.inactivityTimeout", 60000L, "http[http/1.1].keepAliveTimeout", 60);
    }


    @Test
    public void testTcpBindOption() throws Exception {
        expectSuccess("tcp.bind", "4000", "tcp.bind", "0.0.0.0:4000");
        expectSuccess("tcp.bind", "foo:4000", "tcp.bind", "foo:4000");

        expectRuntimeFailure("tcp.bind", "foo");
        expectRuntimeFailure("tcp.bind", "");
        expectRuntimeFailure("tcp.bind", null);
    }

    @Test
    public void testSslBindOption() throws Exception {
        expectSuccess("ssl.bind", "4000", "ssl.tcp.bind", "0.0.0.0:4000");
        expectSuccess("ssl.bind", "foo:4000", "ssl.tcp.bind", "foo:4000");

        expectRuntimeFailure("ssl.bind", "foo");
        expectRuntimeFailure("ssl.bind", "");
        expectRuntimeFailure("ssl.bind", null);
    }

    @Test
    public void testHttpsBindOption() throws Exception {
        expectSuccess("https.bind", "4000", "http.ssl.tcp.bind", "0.0.0.0:4000");
        expectSuccess("https.bind", "foo:4000", "http.ssl.tcp.bind", "foo:4000");

        expectRuntimeFailure("https.bind", "foo");
        expectRuntimeFailure("https.bind", "");
        expectRuntimeFailure("https.bind", null);
    }

    @Test
    public void testHttpBindOption() throws Exception {
        expectSuccess("http.bind", "4000", "http.tcp.bind", "0.0.0.0:4000");
        expectSuccess("http.bind", "foo:4000", "http.tcp.bind", "foo:4000");

        expectRuntimeFailure("http.bind", "foo");
        expectRuntimeFailure("http.bind", "");
        expectRuntimeFailure("http.bind", null);
    }

    @Test
    public void testWsBindOption() throws Exception {
        expectSuccess("ws.bind", "4000", "ws.http.tcp.bind", "0.0.0.0:4000");
        expectSuccess("ws.bind", "foo:4000", "ws.http.tcp.bind", "foo:4000");

        expectRuntimeFailure("ws.bind", "foo");
        expectRuntimeFailure("ws.bind", "");
        expectRuntimeFailure("ws.bind", null);
    }

    @Test
    public void testWssBindOption() throws Exception {
        expectSuccess("wss.bind", "4000", "ws.http.ssl.tcp.bind", "0.0.0.0:4000");
        expectSuccess("wss.bind", "foo:4000", "ws.http.ssl.tcp.bind", "foo:4000");

        expectRuntimeFailure("wss.bind", "foo");
        expectRuntimeFailure("wss.bind", "");
        expectRuntimeFailure("wss.bind", null);
    }

    @Test
    @Ignore("XSD no longer validates accept-options types")
    public void testSslVerifyClientOption() throws Exception {
        expectSuccess("ssl.verify-client", "none",
                TransportOptionNames.SSL_NEED_CLIENT_AUTH, Boolean.FALSE,
                TransportOptionNames.SSL_WANT_CLIENT_AUTH, Boolean.FALSE);
        expectSuccess("ssl.verify-client", "optional",
                TransportOptionNames.SSL_NEED_CLIENT_AUTH, Boolean.FALSE,
                TransportOptionNames.SSL_WANT_CLIENT_AUTH, Boolean.TRUE);
        expectSuccess("ssl.verify-client", "required",
                TransportOptionNames.SSL_NEED_CLIENT_AUTH, Boolean.TRUE,
                TransportOptionNames.SSL_WANT_CLIENT_AUTH, Boolean.FALSE);

        expectParseFailure("ssl.verify-client", "badvalue");
        expectParseFailure("ssl.verify-client", null);
    }

    @Test
    @Ignore("XSD no longer validates accept-options types")
    public void testSslEncryptionOption() throws Exception {
        expectSuccess("ssl.encryption", "enabled", "ssl.encryptionEnabled", Boolean.TRUE);
        expectSuccess("ssl.encryption", "disabled", "ssl.encryptionEnabled", Boolean.FALSE);

        expectParseFailure("ssl.encryption", "badvalue");
    }

    @Test(expected = GatewayConfigParserException.class)
    public void testNegativeHttpKeepaliveTimeout() throws Exception {
        File configFile = null;
        try {
            configFile =
                    createTempFileFromResource("org/kaazing/gateway/server/config/parse/data/gateway-config-bad-http-keepalive" +
                            "-timeout.xml");
            GatewayConfigDocument doc = parser.parse(configFile);
            Assert.assertNotNull(doc);
            resolver.resolve(doc, null);
        } finally {
            if (configFile != null) {
                configFile.delete();
            }
        }
    }

    enum TestResult {
        PARSE_FAILURE,
        VALIDATE_FAILURE,
        RUNTIME_EXCEPTION,
        SUCCESS
    }

    void expectSuccess(String optionName,
                       String optionValue,
                       String optionsMapKey,
                       Object expectedValue,
                       Object... extras) throws Exception {
        runTestCase(optionName, optionValue, TestResult.SUCCESS, optionsMapKey, expectedValue, extras);
    }

    void expectParseFailure(String optionName,
                            String optionValue) throws Exception {
        runTestCase(optionName, optionValue, TestResult.PARSE_FAILURE, null, null);
    }

    void expectValidateFailure(String optionName,
                               String optionValue) throws Exception {
        runTestCase(optionName, optionValue, TestResult.VALIDATE_FAILURE, null, null);
    }

    void expectRuntimeFailure(String optionName,
                              String optionValue) throws Exception {
        runTestCase(optionName, optionValue, TestResult.RUNTIME_EXCEPTION, null, null);
    }

    void runTestCase(String optionName,
                     String optionValue,
                     TestResult expectedResult,
                     String expectedKey,
                     Object expectedValue,
                     Object... extras) throws Exception {

        File configFile;
        configFile =
                createTempFileFromResource("org/kaazing/gateway/server/config/parse/data/gateway-config-accept-options-template" +
                                ".xml",
                        optionName, optionValue);

        GatewayConfigDocument doc;
        try {
            doc = parser.parse(configFile);

        } catch (Exception e) {
            if (expectedResult == TestResult.PARSE_FAILURE) {
//                System.out.println("Caught expected parse exception " + e);
                return;
            } else {
                throw e;
            }
        }
        Assert.assertNotNull(doc);
        ServiceAcceptOptionsType serviceAcceptOptionsType = doc.getGatewayConfig().getServiceArray(0).getAcceptOptions();
        AcceptOptionsContext acceptOptionsContext;
        try {
            acceptOptionsContext = new DefaultAcceptOptionsContext(serviceAcceptOptionsType, null);

        } catch (Exception e) {
            if (e instanceof RuntimeException &&
                    expectedResult == TestResult.RUNTIME_EXCEPTION) {
                return;
            }

            if (expectedResult == TestResult.VALIDATE_FAILURE) {
                return;

            } else {
                throw e;
            }
        }
        Map<String, Object> optionsMap = acceptOptionsContext.asOptionsMap();
        if (expectedValue instanceof String[]) {
            assertArrayEquals((String[]) expectedValue, (String[]) optionsMap.get(expectedKey));
        } else {
            assertEquals(expectedValue, optionsMap.get(expectedKey));
        }
        if (extras != null) {
            if (extras.length % 2 != 0) {
                throw new IllegalStateException("Test case error: must provide key and value for options map validation.");
            } else {
                for (int i = 0; i < extras.length; i += 2) {
                    String k = (String) extras[i];
                    Object v = extras[i + 1];
                    assertEquals(v, optionsMap.get(k));
                }
            }
        }
        if (expectedResult != TestResult.SUCCESS) {
            throw new IllegalStateException(
                    "Unexpected successful valid accept option " + optionName + " with value " + optionValue);
        }

    }

    private File createTempFileFromResource(String resourceName, Object... values) throws IOException {
        File file = File.createTempFile("gateway-config", "xml");
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream is = classLoader.getResource(resourceName).openStream();
        ByteArrayOutputStream bos = new ByteArrayOutputStream(is.available());
        int datum;
        while ((datum = is.read()) != -1) {
            bos.write(datum);
        }
        final String replacedContent = MessageFormat.format(bos.toString("UTF-8"), values);
        //System.out.println(replacedContent);
        ByteArrayInputStream bis = new ByteArrayInputStream(replacedContent.getBytes("UTF-8"));
        FileOutputStream fos = new FileOutputStream(file);
        while ((datum = bis.read()) != -1) {
            fos.write(datum);
        }
        fos.flush();
        fos.close();
        return file;
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
