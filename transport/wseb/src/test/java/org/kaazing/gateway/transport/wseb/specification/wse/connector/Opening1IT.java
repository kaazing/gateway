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
package org.kaazing.gateway.transport.wseb.specification.wse.connector;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertTrue;
import static org.kaazing.test.util.ITUtil.timeoutRule;

import java.io.IOException;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IoSession;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.resource.address.ws.WsResourceAddress;
import org.kaazing.gateway.transport.test.Expectations;
import org.kaazing.gateway.transport.ws.extension.ExtensionHeader;
import org.kaazing.gateway.transport.ws.extension.ExtensionHeaderBuilder;
import org.kaazing.gateway.transport.ws.extension.ExtensionHelper;
import org.kaazing.gateway.transport.ws.extension.WebSocketExtension;
import org.kaazing.gateway.transport.ws.extension.WebSocketExtensionFactorySpi;
import org.kaazing.gateway.transport.wseb.test.WsebConnectorRule;
import org.kaazing.gateway.util.InternalSystemProperty;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.netx.URLConnectionHelper;
import org.kaazing.test.util.ITUtil;
import org.kaazing.test.util.MemoryAppender;
import org.kaazing.test.util.MethodExecutionTrace;

public class Opening1IT {
    private static ClassLoader classLoader;

    private static final Set<String> EMPTY_STRING_SET = Collections.emptySet();

    private K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/wse/opening");

    private final WsebConnectorRule connector;

    {
        Properties configuration = new Properties();
        configuration.setProperty(InternalSystemProperty.WS_CLOSE_TIMEOUT.getPropertyName(), "1s");
        connector = new WsebConnectorRule(configuration);
    }

    private JUnitRuleMockery context = new JUnitRuleMockery() {
        {
            setThreadingPolicy(new Synchroniser());
        }
    };

    private TestRule contextRule = ITUtil.toTestRule(context);
    private final TestRule trace = new MethodExecutionTrace();
    private final TestRule timeoutRule = timeoutRule(5, SECONDS);

    @Rule
    public TestRule chain = RuleChain.outerRule(trace).around(contextRule).around(connector)
            .around(k3po).around(timeoutRule);

    @BeforeClass
    public static void before() throws Exception {
        classLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(new TestClassLoader(PrimaryExtensionFactory.class.getName(), SecondaryExtensionFactory.class.getName()));
    }

    @AfterClass
    public static void after() {
        Thread.currentThread().setContextClassLoader(classLoader);
    }

    @Test
    @Specification("connection.established/handshake.response")
    public void shouldEstablishConnection() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                // No close handshake so IOException may occur depending on timing of k3po closing connections
                allowing(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(IOException.class)));
                allowing(handler).sessionClosed(with(any(IoSessionEx.class)));
            }
        });
        connector.connect("ws://localhost:8080/path?query", null, handler).getSession();
        k3po.finish();
    }

    // Server only test
    @Specification("request.header.origin/handshake.request")
    void shouldEstablishConnectionWithRequestHeaderOrigin()
            throws Exception {
        k3po.finish();
    }

    @Test
    @Specification("request.header.x.websocket.extensions/handshake.response")
    public void shouldEstablishConnectionWithRequestHeaderXWebSocketExtensions() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                // No close handshake so IOException may occur depending on timing of k3po closing connections
                allowing(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(IOException.class)));
                allowing(handler).sessionClosed(with(any(IoSessionEx.class)));
            }
        });
        Map<String, Object> connectOptions = new HashMap<>();
        final ResourceAddress connectAddress =
                ResourceAddressFactory.newResourceAddressFactory().newResourceAddress(
                        "ws://localhost:8080/path?query",
                        connectOptions);

        connector.connect(connectAddress, handler).getSession();
        k3po.finish();
    }

    @Test
    @Specification("response.header.x.websocket.extensions.not.requested/handshake.response")
    public void shouldFailConnectionWhenXWebSocketExtensionsNotRequested() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Exception.class)));
                allowing(handler).sessionClosed(with(any(IoSessionEx.class)));
            }
        });
        Map<String, Object> connectOptions = new HashMap<>();
        final ResourceAddress connectAddress =
                ResourceAddressFactory.newResourceAddressFactory().newResourceAddress(
                        "ws://localhost:8080/path?query",
                        connectOptions);

        IoSession session = connector.connect(connectAddress, handler).getSession();

        k3po.finish();
        assertTrue(session.getCloseFuture().await(4, SECONDS));
        MemoryAppender.assertMessagesLogged(Collections.singletonList("WebSocket extension.*not requested"), EMPTY_STRING_SET, null, false);
    }

    public static class PrimaryExtensionFactory extends WebSocketExtensionFactorySpi {

        @Override
        public String getExtensionName() {
            return "primary";
        }

        @Override
        public WebSocketExtension negotiate(ExtensionHeader header, ExtensionHelper extensionHelper, WsResourceAddress address) throws ProtocolException {
            return new WebSocketExtension(extensionHelper) {

                @Override
                public ExtensionHeader getExtensionHeader() {
                    return header;
                }

            };
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

    public static class SecondaryExtensionFactory extends WebSocketExtensionFactorySpi {

        @Override
        public String getExtensionName() {
            return "secondary";
        }

        @Override
        public WebSocketExtension negotiate(ExtensionHeader header, ExtensionHelper extensionHelper, WsResourceAddress address) throws ProtocolException {
            return new WebSocketExtension(extensionHelper) {

                @Override
                public ExtensionHeader getExtensionHeader() {
                    return header;
                }

            };
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
    private static class TestClassLoader extends ClassLoader {
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
