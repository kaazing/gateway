/*
 * Copyright 2014, Kaazing Corporation. All rights reserved.
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

package org.kaazing.gateway.transport.ws.extension;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.kaazing.gateway.transport.ws.extension.WebSocketExtensionFactorySpi.ExtensionOrderCategory.EMULATION;
import static org.kaazing.gateway.transport.ws.extension.WebSocketExtensionFactorySpi.ExtensionOrderCategory.NETWORK;

import java.io.IOException;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.kaazing.gateway.resource.address.ws.WsResourceAddress;
import org.kaazing.gateway.transport.http.HttpAcceptSession;
import org.kaazing.gateway.transport.ws.extension.WebSocketExtensionFactoryTest.MockWebSocketExtensionSpi.MockNegotiate;

public class WebSocketExtensionFactoryTest {

    private WebSocketExtensionFactory wsExtFactory;
    private WsResourceAddress address;
    private ExtensionHeader extensionHeader;
    private MockNegotiate mockNegotiate;
    private WebSocketExtensionSpi webSocketExtensionSpi;
    private HttpAcceptSession session;

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    @Before
    public void init() {
        wsExtFactory = WebSocketExtensionFactory.newInstance();
        address = context.mock(WsResourceAddress.class);
        extensionHeader = context.mock(ExtensionHeader.class);
        mockNegotiate = context.mock(MockNegotiate.class);
        MockWebSocketExtensionSpi.setNegotiateBehavoir(mockNegotiate);
        webSocketExtensionSpi = context.mock(WebSocketExtensionSpi.class);
        session = context.mock(HttpAcceptSession.class);
    }

    @Test
    public void testGetExtensionNames() {
        Collection<String> extensionNames = wsExtFactory.getExtensionNames();
        assertTrue(extensionNames.contains("mock"));
    }

    @Test
    public void testNegotiateExtFound() throws IOException {
        context.checking(new Expectations() {
            {
                oneOf(extensionHeader).getExtensionToken();
                will(returnValue("mock"));
                oneOf(mockNegotiate).negotiate(extensionHeader, address);
                will(returnValue(webSocketExtensionSpi));
            }
        });

        WebSocketExtensionSpi extension = wsExtFactory.negotiate(extensionHeader, address);
        assertNotNull(extension);
        assertSame(extension, webSocketExtensionSpi);
    }

    @Test
    public void testNegotiateExtNameNotFound() throws IOException {
        context.checking(new Expectations() {
            {
                oneOf(extensionHeader).getExtensionToken();
                will(returnValue("NonExistant"));
            }
        });

        WebSocketExtensionSpi extension = wsExtFactory.negotiate(extensionHeader, address);
        assertNull(extension);
    }

    @Test
    public void testNegotiateWebSocketExtensions() throws ProtocolException {
        List<String> clientRequestedExtensions = new ArrayList<>();
        clientRequestedExtensions.add("mock");
        context.checking(new Expectations() {
            {
                oneOf(mockNegotiate).negotiate(with(new ExtensionHeaderTokenMatcher("mock")), with(address));
                will(returnValue(webSocketExtensionSpi));
            }
        });
        ActiveWebSocketExtensions ActiveWebSocketExtensions =
                wsExtFactory.negotiateWebSocketExtensions(address, session, "Sec-Websocket-Extensions",
                        clientRequestedExtensions);
        assertSame(webSocketExtensionSpi, ActiveWebSocketExtensions.asList().get(0));
        assertEquals(1, ActiveWebSocketExtensions.asList().size());

    }

    @Test
    public void testNegotiateWebSocketExtensionsWithSomeMissing() throws ProtocolException {
        List<String> clientRequestedExtensions = new ArrayList<>();
        clientRequestedExtensions.add("not-there");
        clientRequestedExtensions.add("mock");
        clientRequestedExtensions.add("not-there2");
        context.checking(new Expectations() {
            {
                oneOf(mockNegotiate).negotiate(with(new ExtensionHeaderTokenMatcher("mock")), with(address));
                will(returnValue(webSocketExtensionSpi));
            }
        });
        ActiveWebSocketExtensions ActiveWebSocketExtensions =
                wsExtFactory.negotiateWebSocketExtensions(address, session, "Sec-Websocket-Extensions",
                        clientRequestedExtensions);
        assertSame(webSocketExtensionSpi, ActiveWebSocketExtensions.asList().get(0));
        assertEquals(1, ActiveWebSocketExtensions.asList().size());
    }

    @Test
    public void testNegotiateWebSocketExtensionsWithParameter() throws ProtocolException {
        List<String> clientRequestedExtensions = new ArrayList<>();
        clientRequestedExtensions.add("not-there");
        clientRequestedExtensions.add("mock; foo=2");
        clientRequestedExtensions.add("not-there2; bar=2");
        context.checking(new Expectations() {
            {
                oneOf(mockNegotiate).negotiate(with(new ExtensionHeaderTokenMatcher("mock").withParameter("foo=2")),
                        with(address));
                will(returnValue(webSocketExtensionSpi));
            }
        });
        ActiveWebSocketExtensions ActiveWebSocketExtensions =
                wsExtFactory.negotiateWebSocketExtensions(address, session, "Sec-Websocket-Extensions",
                        clientRequestedExtensions);
        assertSame(webSocketExtensionSpi, ActiveWebSocketExtensions.asList().get(0));
        assertEquals(1, ActiveWebSocketExtensions.asList().size());
    }

    @Test
    public void testOrderingMechanism() {
        WebSocketExtensionFactorySpi fooWebSocketExtensionFactory = new FooWebSocketExtensionFactory();
        WebSocketExtensionFactorySpi perMessageDeflateWebSocketExtensionFactory =
                new PerMessageDeflateWebSocketExtensionFactory();
        WebSocketExtensionFactorySpi pingPongWebSocketExtensionFactory = new PingPongWebSocketExtensionFactory();

        LinkedList<WebSocketExtensionFactorySpi> orderedExtensions = new LinkedList<>();
        WebSocketExtensionFactory.addExtensionInCorrectOrder(pingPongWebSocketExtensionFactory, orderedExtensions);
        WebSocketExtensionFactory.addExtensionInCorrectOrder(fooWebSocketExtensionFactory, orderedExtensions);
        WebSocketExtensionFactory.addExtensionInCorrectOrder(perMessageDeflateWebSocketExtensionFactory, orderedExtensions);
        assertSame(fooWebSocketExtensionFactory, orderedExtensions.get(0));
        assertSame(pingPongWebSocketExtensionFactory, orderedExtensions.get(1));
        assertSame(perMessageDeflateWebSocketExtensionFactory, orderedExtensions.get(2));

        orderedExtensions.clear();
        WebSocketExtensionFactory.addExtensionInCorrectOrder(pingPongWebSocketExtensionFactory, orderedExtensions);
        WebSocketExtensionFactory.addExtensionInCorrectOrder(perMessageDeflateWebSocketExtensionFactory, orderedExtensions);
        WebSocketExtensionFactory.addExtensionInCorrectOrder(fooWebSocketExtensionFactory, orderedExtensions);
        assertSame(fooWebSocketExtensionFactory, orderedExtensions.get(0));
        assertSame(pingPongWebSocketExtensionFactory, orderedExtensions.get(1));
        assertSame(perMessageDeflateWebSocketExtensionFactory, orderedExtensions.get(2));

        orderedExtensions.clear();
        WebSocketExtensionFactory.addExtensionInCorrectOrder(fooWebSocketExtensionFactory, orderedExtensions);
        WebSocketExtensionFactory.addExtensionInCorrectOrder(perMessageDeflateWebSocketExtensionFactory, orderedExtensions);
        WebSocketExtensionFactory.addExtensionInCorrectOrder(pingPongWebSocketExtensionFactory, orderedExtensions);
        assertSame(fooWebSocketExtensionFactory, orderedExtensions.get(0));
        assertSame(pingPongWebSocketExtensionFactory, orderedExtensions.get(1));
        assertSame(perMessageDeflateWebSocketExtensionFactory, orderedExtensions.get(2));

        orderedExtensions.clear();
        WebSocketExtensionFactory.addExtensionInCorrectOrder(fooWebSocketExtensionFactory, orderedExtensions);
        WebSocketExtensionFactory.addExtensionInCorrectOrder(pingPongWebSocketExtensionFactory, orderedExtensions);
        WebSocketExtensionFactory.addExtensionInCorrectOrder(perMessageDeflateWebSocketExtensionFactory, orderedExtensions);
        assertSame(fooWebSocketExtensionFactory, orderedExtensions.get(0));
        assertSame(pingPongWebSocketExtensionFactory, orderedExtensions.get(1));
        assertSame(perMessageDeflateWebSocketExtensionFactory, orderedExtensions.get(2));

        orderedExtensions.clear();
        WebSocketExtensionFactory.addExtensionInCorrectOrder(perMessageDeflateWebSocketExtensionFactory, orderedExtensions);
        WebSocketExtensionFactory.addExtensionInCorrectOrder(fooWebSocketExtensionFactory, orderedExtensions);
        WebSocketExtensionFactory.addExtensionInCorrectOrder(pingPongWebSocketExtensionFactory, orderedExtensions);
        assertSame(fooWebSocketExtensionFactory, orderedExtensions.get(0));
        assertSame(pingPongWebSocketExtensionFactory, orderedExtensions.get(1));
        assertSame(perMessageDeflateWebSocketExtensionFactory, orderedExtensions.get(2));

        orderedExtensions.clear();
        WebSocketExtensionFactory.addExtensionInCorrectOrder(perMessageDeflateWebSocketExtensionFactory, orderedExtensions);
        WebSocketExtensionFactory.addExtensionInCorrectOrder(pingPongWebSocketExtensionFactory, orderedExtensions);
        WebSocketExtensionFactory.addExtensionInCorrectOrder(fooWebSocketExtensionFactory, orderedExtensions);
        assertSame(fooWebSocketExtensionFactory, orderedExtensions.get(0));
        assertSame(pingPongWebSocketExtensionFactory, orderedExtensions.get(1));
        assertSame(perMessageDeflateWebSocketExtensionFactory, orderedExtensions.get(2));
    }

    @Test
    public void testNegotiateMultipleWebSocketExtensionsThatMaintainsOrder() throws ProtocolException {
        List<String> clientRequestedExtensions = new ArrayList<>();
        clientRequestedExtensions.add("foo");
        clientRequestedExtensions.add("per-message-deflate");
        clientRequestedExtensions.add("ping-pong");
        ActiveWebSocketExtensions activeWebSocketExtensions =
                wsExtFactory.negotiateWebSocketExtensions(address, session, "Sec-Websocket-Extensions",
                        clientRequestedExtensions);
        assertEquals(3, activeWebSocketExtensions.asList().size());
        assertEquals("foo", activeWebSocketExtensions.asList().get(0).getExtensionHeader().getExtensionToken());
        assertEquals("ping-pong", activeWebSocketExtensions.asList().get(1).getExtensionHeader().getExtensionToken());
        assertEquals("per-message-deflate", activeWebSocketExtensions.asList().get(2).getExtensionHeader().getExtensionToken());

        clientRequestedExtensions.clear();
        clientRequestedExtensions.add("foo");
        clientRequestedExtensions.add("ping-pong");
        clientRequestedExtensions.add("per-message-deflate");
        activeWebSocketExtensions =
                wsExtFactory.negotiateWebSocketExtensions(address, session, "Sec-Websocket-Extensions",
                        clientRequestedExtensions);
        assertEquals(3, activeWebSocketExtensions.asList().size());
        assertEquals("foo", activeWebSocketExtensions.asList().get(0).getExtensionHeader().getExtensionToken());
        assertEquals("ping-pong", activeWebSocketExtensions.asList().get(1).getExtensionHeader().getExtensionToken());
        assertEquals("per-message-deflate", activeWebSocketExtensions.asList().get(2).getExtensionHeader().getExtensionToken());

        clientRequestedExtensions.clear();
        clientRequestedExtensions.add("ping-pong");
        clientRequestedExtensions.add("foo");
        clientRequestedExtensions.add("per-message-deflate");
        activeWebSocketExtensions =
                wsExtFactory.negotiateWebSocketExtensions(address, session, "Sec-Websocket-Extensions",
                        clientRequestedExtensions);
        assertEquals(3, activeWebSocketExtensions.asList().size());
        assertEquals("foo", activeWebSocketExtensions.asList().get(0).getExtensionHeader().getExtensionToken());
        assertEquals("ping-pong", activeWebSocketExtensions.asList().get(1).getExtensionHeader().getExtensionToken());
        assertEquals("per-message-deflate", activeWebSocketExtensions.asList().get(2).getExtensionHeader().getExtensionToken());

        clientRequestedExtensions.clear();
        clientRequestedExtensions.add("ping-pong");
        clientRequestedExtensions.add("per-message-deflate");
        clientRequestedExtensions.add("foo");
        activeWebSocketExtensions =
                wsExtFactory.negotiateWebSocketExtensions(address, session, "Sec-Websocket-Extensions",
                        clientRequestedExtensions);
        assertEquals(3, activeWebSocketExtensions.asList().size());
        assertEquals("foo", activeWebSocketExtensions.asList().get(0).getExtensionHeader().getExtensionToken());
        assertEquals("ping-pong", activeWebSocketExtensions.asList().get(1).getExtensionHeader().getExtensionToken());
        assertEquals("per-message-deflate", activeWebSocketExtensions.asList().get(2).getExtensionHeader().getExtensionToken());

        clientRequestedExtensions.clear();
        clientRequestedExtensions.add("per-message-deflate");
        clientRequestedExtensions.add("ping-pong");
        clientRequestedExtensions.add("foo");
        activeWebSocketExtensions =
                wsExtFactory.negotiateWebSocketExtensions(address, session, "Sec-Websocket-Extensions",
                        clientRequestedExtensions);
        assertEquals(3, activeWebSocketExtensions.asList().size());
        assertEquals("foo", activeWebSocketExtensions.asList().get(0).getExtensionHeader().getExtensionToken());
        assertEquals("ping-pong", activeWebSocketExtensions.asList().get(1).getExtensionHeader().getExtensionToken());
        assertEquals("per-message-deflate", activeWebSocketExtensions.asList().get(2).getExtensionHeader().getExtensionToken());

        clientRequestedExtensions.clear();
        clientRequestedExtensions.add("per-message-deflate");
        clientRequestedExtensions.add("foo");
        clientRequestedExtensions.add("ping-pong");
        activeWebSocketExtensions =
                wsExtFactory.negotiateWebSocketExtensions(address, session, "Sec-Websocket-Extensions",
                        clientRequestedExtensions);
        assertEquals(3, activeWebSocketExtensions.asList().size());
        assertEquals("foo", activeWebSocketExtensions.asList().get(0).getExtensionHeader().getExtensionToken());
        assertEquals("ping-pong", activeWebSocketExtensions.asList().get(1).getExtensionHeader().getExtensionToken());
        assertEquals("per-message-deflate", activeWebSocketExtensions.asList().get(2).getExtensionHeader().getExtensionToken());
    }

    @Test
    public void testNegotiateWebSocketExtensionsNotThere() throws ProtocolException {
        List<String> clientRequestedExtensions = new ArrayList<>();
        clientRequestedExtensions.add("notthere");
        ActiveWebSocketExtensions ActiveWebSocketExtensions =
                wsExtFactory.negotiateWebSocketExtensions(address, session, "Sec-Websocket-Extensions",
                        clientRequestedExtensions);
        assertTrue(ActiveWebSocketExtensions.asList().isEmpty());
    }

    @Test
    public void testNegotiatedWebSocketExtensionRejectedParameter() throws ProtocolException {
        List<String> clientRequestedExtensions = new ArrayList<>();
        clientRequestedExtensions.add("mock");
        context.checking(new Expectations() {
            {
                oneOf(mockNegotiate).negotiate(with(new ExtensionHeaderTokenMatcher("mock")), with(address));
                will(returnValue(null));
            }
        });
        ActiveWebSocketExtensions ActiveWebSocketExtensions =
                wsExtFactory.negotiateWebSocketExtensions(address, session, "Sec-Websocket-Extensions",
                        clientRequestedExtensions);
        Assert.assertEquals(0, ActiveWebSocketExtensions.asList().size());
    }

    @Test(expected = ProtocolException.class)
    public void testProtocolExceptionInNegotiate() throws ProtocolException {
        List<String> clientRequestedExtensions = new ArrayList<>();
        clientRequestedExtensions.add("mock");
        context.checking(new Expectations() {
            {
                oneOf(mockNegotiate).negotiate(with(new ExtensionHeaderTokenMatcher("mock")), with(address));
                will(throwException(new ProtocolException("Protocol Exception")));
            }
        });
        wsExtFactory.negotiateWebSocketExtensions(address, session, "Sec-Websocket-Extensions", clientRequestedExtensions);
    }

    public static class MockWebSocketExtensionSpi extends WebSocketExtensionFactorySpi {

        static MockNegotiate mockBehavior;

        @Override
        public String getExtensionName() {
            return "mock";
        }

        @Override
        public WebSocketExtensionSpi negotiate(ExtensionHeader requestedExtension, WsResourceAddress address)
                throws ProtocolException {
            return (mockBehavior == null) ? null : mockBehavior.negotiate(requestedExtension, address);
        }

        static void setNegotiateBehavoir(MockNegotiate behavoir) {
            mockBehavior = behavoir;
        }

        public interface MockNegotiate {
            WebSocketExtensionSpi negotiate(ExtensionHeader requestedExtension, WsResourceAddress address)
                    throws ProtocolException;
        }
    }

    /**
     * Matches extensionToken name and that all parameters are expected
     *
     */
    public class ExtensionHeaderTokenMatcher extends BaseMatcher<ExtensionHeader> {

        private String extensionToken;
        private List<String> expectedParameters = new ArrayList<>();

        /**
         * No expected Parameters
         * @param extensionToken
         */
        public ExtensionHeaderTokenMatcher(String extensionToken) {
            this.extensionToken = extensionToken;
        }

        public ExtensionHeaderTokenMatcher(String extensionToken, List<String> expectedParameters) {
            this.extensionToken = extensionToken;
            this.expectedParameters = expectedParameters;
        }

        /**
         * Builder method for convenience
         * @param param
         * @return
         */
        public ExtensionHeaderTokenMatcher withParameter(String param) {
            expectedParameters.add(param);
            return this;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("Matches the extension token Of extension header");

        }

        @Override
        public boolean matches(Object item) {
            boolean result = true;
            if (item instanceof ExtensionHeader && extensionToken.equals(((ExtensionHeader) item).getExtensionToken())) {
                for (ExtensionParameter actual : ((ExtensionHeader) item).getParameters()) {
                    if (!expectedParameters.remove(actual.toString())) {
                        result = false;
                        break;
                    }
                }
                if (expectedParameters.size() != 0) {
                    result = false;
                }
            } else {
                result = false;
            }
            return result;
        }
    }

    public static class FooWebSocketExtensionFactory extends WebSocketExtensionFactorySpi {
        @Override
        public WebSocketExtensionSpi negotiate(ExtensionHeader requestedExtension, WsResourceAddress address)
                throws ProtocolException {
            return new WebSocketExtensionSpi() {
                @Override
                public ExtensionHeader getExtensionHeader() {
                    return new ExtensionHeaderBuilder("foo");
                }
            };
        }

        @Override
        public String getExtensionName() {
            return "foo";
        }
    }

    public static class PerMessageDeflateWebSocketExtensionFactory extends WebSocketExtensionFactorySpi {
        @Override
        public WebSocketExtensionSpi negotiate(ExtensionHeader requestedExtension, WsResourceAddress address)
                throws ProtocolException {
            return new WebSocketExtensionSpi() {
                @Override
                public ExtensionHeader getExtensionHeader() {
                    return new ExtensionHeaderBuilder("per-message-deflate");
                }
            };
        }

        @Override
        public String getExtensionName() {
            return "per-message-deflate";
        }

        @Override
        public ExtensionOrderCategory orderCategory() {
            return NETWORK;
        }
    }

    public static class PingPongWebSocketExtensionFactory extends WebSocketExtensionFactorySpi {

        @Override
        public WebSocketExtensionSpi negotiate(ExtensionHeader requestedExtension, WsResourceAddress address)
                throws ProtocolException {
            return new WebSocketExtensionSpi() {
                @Override
                public ExtensionHeader getExtensionHeader() {
                    return new ExtensionHeaderBuilder("ping-pong");
                }
            };
        }

        @Override
        public String getExtensionName() {
            return "ping-pong";
        }

        @Override
        public ExtensionOrderCategory orderCategory() {
            return EMULATION;
        }
    }

}
