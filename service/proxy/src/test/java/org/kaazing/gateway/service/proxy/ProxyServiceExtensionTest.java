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
package org.kaazing.gateway.service.proxy;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.joining;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.test.util.ITUtil;

public class ProxyServiceExtensionTest {

    private static ClassLoader classLoader;

    @BeforeClass
    public static void before() throws Exception {
        classLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(new TestClassLoader(TestExtension.class.getName()));
    }

    @AfterClass
    public static void after() {
        Thread.currentThread().setContextClassLoader(classLoader);
    }

    private GatewayRule gateway = new GatewayRule() {
        {
            // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                        .service()
                            .accept("tcp://localhost:8888")
                            .connect("tcp://localhost:8051")
                            .type("proxy")
                        .done()
                    .done();
            // @formatter:on
            init(configuration);
        }
    };

    @Rule
    public RuleChain chain = ITUtil.createRuleChain(gateway, 20, SECONDS);

    @Test
    public void shouldInvokeExtension() throws Exception {
        CountDownLatch backendReady = new CountDownLatch(1);

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try (ServerSocket backendServer = new ServerSocket()) {
                    backendServer.bind(new InetSocketAddress("localhost", 8051));
                    backendReady.countDown();
                    Socket s = backendServer.accept();
                    s.close();
                } catch (Exception ex) {
                    fail("Unexpected exception in backend server: " + ex);
                }
            }
        }, "backendServer-thread");
        t.start();

        boolean backendBound = backendReady.await(1, TimeUnit.SECONDS);
        assertTrue("Backend Server failed to bind, pre-conditions not established", backendBound);

        // connect to the service to ensure the extension is executed
        try (Socket clientSocket = new Socket("localhost", 8888)) {
            boolean success = TestExtension.latch.await(5, TimeUnit.SECONDS);
            assertTrue("Failed to execute all phases of proxy service extension", success);
        } catch (Exception ex) {
            fail("Unexpected exception in client connecting to server: " + ex);
        }

        Thread.currentThread().setContextClassLoader(new ProxyServiceExtensionIT.TestClassLoader());

    }

    /**
     * A classloader whose getResources("META-INF/services/org.kaazing.gateway.service.proxy.ProxyServiceExtensionSpi")
     * method will return a URL whose contents will be the list of class names supplied in the constructor.
     * This avoids the need for test meta-info resources files to be available on the test class path.
     */
    static class TestClassLoader extends ClassLoader {
        private URL url;

        TestClassLoader(String... factorySpiClassNames) throws MalformedURLException {
            url = new URL(null, "data:metainf", new TestURLStreamHandler(factorySpiClassNames));
        }

        @Override
        public Enumeration<URL> getResources(String name) throws IOException {
            if (name.equals("META-INF/services/" + ProxyServiceExtensionSpi.class.getName())) {
                return Collections.enumeration(Collections.singletonList(url));
            }
            return super.getResources(name);
        }

    }

    private static class TestURLStreamHandler extends URLStreamHandler {
        private final byte[] contents;

        TestURLStreamHandler(String[] factorySpiClassNames) {
            String metaInfContent = Arrays.stream(factorySpiClassNames).collect(joining("\n"));
            contents = metaInfContent.getBytes(UTF_8);
        }

        @Override
        protected URLConnection openConnection(URL u) throws IOException {
            return new URLConnection(u) {

                @Override
                public void connect() throws IOException {
                }

                @Override
                public InputStream getInputStream() {
                    return new ByteArrayInputStream(contents);
                }

            };
        }

    }

}
