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
package org.kaazing.gateway.transport.wsn.specification.ws.connector;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertTrue;
import static org.kaazing.test.util.ITUtil.timeoutRule;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandler;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.kaazing.gateway.resource.address.ws.WsResourceAddress;
import org.kaazing.gateway.transport.test.Expectations;
import org.kaazing.gateway.transport.ws.extension.ExtensionHeader;
import org.kaazing.gateway.transport.ws.extension.ExtensionHeaderBuilder;
import org.kaazing.gateway.transport.ws.extension.ExtensionHelper;
import org.kaazing.gateway.transport.ws.extension.WebSocketExtension;
import org.kaazing.gateway.transport.ws.extension.WebSocketExtensionFactorySpi;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.netx.URLConnectionHelper;
import org.kaazing.test.util.ITUtil;
import org.kaazing.test.util.MethodExecutionTrace;

import java.io.IOException;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class OpeningHandshake2IT {
    private static ClassLoader classLoader;

    private final WsnConnectorRule connector = new WsnConnectorRule();
    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/ws/opening");
    private final TestRule trace = new MethodExecutionTrace();
    private final TestRule timeoutRule = timeoutRule(10, SECONDS);

    private JUnitRuleMockery context = new JUnitRuleMockery() {
        {
            setThreadingPolicy(new Synchroniser());
        }
    };

    private TestRule contextRule = ITUtil.toTestRule(context);

    @Rule
    public TestRule chain = RuleChain.outerRule(trace).around(connector).around(k3po).around(timeoutRule)
            .around(contextRule);

    @BeforeClass
    public static void before() throws Exception {
        classLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader testCL = new TestClassLoader(ValidExtensionFactory.class.getName(), InvalidExtensionFactory.class.getName());
        Thread.currentThread().setContextClassLoader(testCL);
    }

    @AfterClass
    public static void after() {
        Thread.currentThread().setContextClassLoader(classLoader);
    }

    @Test
    @Specification("response.header.sec.websocket.extensions.partial.agreement/handshake.response")
    public void shouldEstablishConnectionWithSomeExtensionsNegotiated() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                atMost(1).of(handler).sessionOpened(with(any(IoSessionEx.class)));
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/path?query", null, null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }


    public static class InvalidExtensionFactory extends WebSocketExtensionFactorySpi {

        @Override
        public String getExtensionName() {
            return "invalid";
        }

        @Override
        public WebSocketExtension negotiate(ExtensionHeader header, ExtensionHelper extensionHelper, WsResourceAddress address) throws ProtocolException {
            return null;
        }

        @Override
        public WebSocketExtension offer(ExtensionHelper extensionHelper, WsResourceAddress address) {
            return new WebSocketExtension(extensionHelper) {

                @Override
                public ExtensionHeader getExtensionHeader() {
                    return new ExtensionHeaderBuilder(getExtensionName());
                }

            };
        }
    }

    public static class ValidExtensionFactory extends WebSocketExtensionFactorySpi {

        @Override
        public String getExtensionName() {
            return "valid";
        }

        @Override
        public WebSocketExtension negotiate(ExtensionHeader header, ExtensionHelper extensionHelper, WsResourceAddress address) throws ProtocolException {
            return null;
        }

        @Override
        public WebSocketExtension offer(ExtensionHelper extensionHelper, WsResourceAddress address) {
            return new WebSocketExtension(extensionHelper) {

                @Override
                public ExtensionHeader getExtensionHeader() {
                    return new ExtensionHeaderBuilder(getExtensionName());
                }

            };
        }
    }

    /**
     * A classloader whose getResources("META-INF/services/org.kaazing.gateway.service.proxy.ProxyServiceExtensionSpi")
     * method will return a URL whose contents will be the list of class names supplied in the constructor.
     * This avoids the need for test meta-info resources files to be available on the test class path.
     */
    public static class TestClassLoader extends ClassLoader {
        private final List<URL> urls;

        TestClassLoader(String... factorySpiClassNames) throws IOException {
            URLConnectionHelper helper = URLConnectionHelper.newInstance();
            List<URL> tempUrls = new ArrayList<>();
            for(String className : factorySpiClassNames){
                URI uri = URI.create("data:," + className);
                URL url = helper.toURL(uri);
                tempUrls.add(url);
            }
            urls = Collections.unmodifiableList(tempUrls);
        }

        @Override
        public Enumeration<URL> getResources(String name) throws IOException {
            if (name.equals("META-INF/services/" + WebSocketExtensionFactorySpi.class.getName())) {
                return Collections.enumeration(urls);
            }
            return super.getResources(name);
        }

    }
}
