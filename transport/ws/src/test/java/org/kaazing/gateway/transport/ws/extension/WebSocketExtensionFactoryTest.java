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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.kaazing.gateway.transport.ws.extension.WebSocketExtensionFactorySpi.ExtensionOrderCategory.EMULATION;
import static org.kaazing.gateway.transport.ws.extension.WebSocketExtensionFactorySpi.ExtensionOrderCategory.NETWORK;

import java.io.IOException;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.kaazing.gateway.transport.ws.extension.WebSocketExtensionFactoryTest.MockWebSocketExtensionSpi.MockNegotiate;

public class WebSocketExtensionFactoryTest {

    private WebSocketExtensionFactory wsExtFactory;
    private WsResourceAddress address;
    private MockNegotiate mockNegotiate;
    private WebSocketExtension webSocketExtensionSpi;
    private WebSocketExtension webSocketExtensionSpi2;

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
        mockNegotiate = context.mock(MockNegotiate.class);
        MockWebSocketExtensionSpi.setNegotiateBehavoir(mockNegotiate);
        webSocketExtensionSpi = context.mock(WebSocketExtension.class);
        webSocketExtensionSpi2 = context.mock(WebSocketExtension.class, "webSocketExtensionSpi2");
    }

    @Test
    public void shouldNegotiateOneExtension() throws IOException {
        context.checking(new Expectations() {
            {
                oneOf(mockNegotiate).negotiate(with(new ExtensionHeaderTokenMatcher("mock")), with(address));
                will(returnValue(webSocketExtensionSpi));
            }
        });

        List<String> clientRequestedExtensions = Arrays.asList(new String[]{"mock"});
        ActiveExtensions extension = wsExtFactory.negotiateWebSocketExtensions(address, clientRequestedExtensions);
        assertNotNull(extension);
        assertSame(extension.asList().get(0), webSocketExtensionSpi);
    }

    @Test
    public void shouldNotNegotiateNonExistentExtension() throws IOException {
        context.checking(new Expectations() {
            {
            }
        });

        List<String> clientRequestedExtensions = Arrays.asList(new String[]{"nonexistant"});
        ActiveExtensions extension = wsExtFactory.negotiateWebSocketExtensions(address, clientRequestedExtensions);
        assertNotNull(extension);
        Assert.assertTrue(extension.asList().isEmpty());
    }

    @Test
    public void shouldNegotiateMultipleExtensions() throws ProtocolException {
        List<String> clientRequestedExtensions = new ArrayList<>();
        clientRequestedExtensions.add("mock2");
        clientRequestedExtensions.add("mock1");
        context.checking(new Expectations() {
            {
                oneOf(mockNegotiate).negotiate(with(new ExtensionHeaderTokenMatcher("mock1")), with(address));
                will(returnValue(webSocketExtensionSpi));
                oneOf(mockNegotiate).negotiate(with(new ExtensionHeaderTokenMatcher("mock2")), with(address));
                will(returnValue(webSocketExtensionSpi2));
            }
        });
        ActiveExtensions ActiveWebSocketExtensions =
                wsExtFactory.negotiateWebSocketExtensions(address, clientRequestedExtensions);
        assertEquals(2, ActiveWebSocketExtensions.asList().size());
        assertSame(webSocketExtensionSpi2, ActiveWebSocketExtensions.asList().get(0));
        assertSame(webSocketExtensionSpi, ActiveWebSocketExtensions.asList().get(1));

    }

    @Test
    public void shouldNegotiateOnlyExistingxtensions() throws ProtocolException {
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
        ActiveExtensions ActiveWebSocketExtensions =
                wsExtFactory.negotiateWebSocketExtensions(address, clientRequestedExtensions);
        assertSame(webSocketExtensionSpi, ActiveWebSocketExtensions.asList().get(0));
        assertEquals(1, ActiveWebSocketExtensions.asList().size());
    }

    @Test
    public void shouldNegotiateExtensionsWithParameter() throws ProtocolException {
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
        ActiveExtensions ActiveWebSocketExtensions =
                wsExtFactory.negotiateWebSocketExtensions(address, clientRequestedExtensions);
        assertSame(webSocketExtensionSpi, ActiveWebSocketExtensions.asList().get(0));
        assertEquals(1, ActiveWebSocketExtensions.asList().size());
    }

    @Test
    public void negotiateExtensionsShouldKeepRequestedOrderWithinSameOrderCategory() throws ProtocolException {
        List<String> clientRequestedExtensions = new ArrayList<>();
        clientRequestedExtensions.add("ping-pong");
        clientRequestedExtensions.add("per-message-deflate");
        clientRequestedExtensions.add("network");
        clientRequestedExtensions.add("foo");
        ActiveExtensions activeExtensions = wsExtFactory.negotiateWebSocketExtensions(address, clientRequestedExtensions);
        List<WebSocketExtension> actual = activeExtensions.asList();
        assertEquals("foo", actual.get(0).getExtensionHeader().getExtensionToken());
        assertEquals("ping-pong", actual.get(1).getExtensionHeader().getExtensionToken());
        assertEquals("per-message-deflate", actual.get(2).getExtensionHeader().getExtensionToken());
        assertEquals("network", actual.get(3).getExtensionHeader().getExtensionToken());
        // reverse order
        clientRequestedExtensions.clear();
        clientRequestedExtensions.add("ping-pong");
        clientRequestedExtensions.add("network");
        clientRequestedExtensions.add("per-message-deflate");
        clientRequestedExtensions.add("foo");
        activeExtensions = wsExtFactory.negotiateWebSocketExtensions(address, clientRequestedExtensions);
        actual = activeExtensions.asList();
        assertEquals("foo", actual.get(0).getExtensionHeader().getExtensionToken());
        assertEquals("ping-pong", actual.get(1).getExtensionHeader().getExtensionToken());
        assertEquals("network", actual.get(2).getExtensionHeader().getExtensionToken());
        assertEquals("per-message-deflate", actual.get(3).getExtensionHeader().getExtensionToken());
    }

    @Test
    public void negotiateShouldOrderByOrderCategory() throws ProtocolException {
        List<String> clientRequestedExtensions = new ArrayList<>();
        clientRequestedExtensions.add("foo");
        clientRequestedExtensions.add("per-message-deflate");
        clientRequestedExtensions.add("ping-pong");
        ActiveExtensions activeWebSocketExtensions =
                wsExtFactory.negotiateWebSocketExtensions(address, clientRequestedExtensions);
        assertEquals(3, activeWebSocketExtensions.asList().size());
        assertEquals("foo", activeWebSocketExtensions.asList().get(0).getExtensionHeader().getExtensionToken());
        assertEquals("ping-pong", activeWebSocketExtensions.asList().get(1).getExtensionHeader().getExtensionToken());
        assertEquals("per-message-deflate", activeWebSocketExtensions.asList().get(2).getExtensionHeader().getExtensionToken());

        clientRequestedExtensions.clear();
        clientRequestedExtensions.add("foo");
        clientRequestedExtensions.add("ping-pong");
        clientRequestedExtensions.add("per-message-deflate");
        activeWebSocketExtensions = wsExtFactory.negotiateWebSocketExtensions(address, clientRequestedExtensions);
        assertEquals(3, activeWebSocketExtensions.asList().size());
        assertEquals("foo", activeWebSocketExtensions.asList().get(0).getExtensionHeader().getExtensionToken());
        assertEquals("ping-pong", activeWebSocketExtensions.asList().get(1).getExtensionHeader().getExtensionToken());
        assertEquals("per-message-deflate", activeWebSocketExtensions.asList().get(2).getExtensionHeader().getExtensionToken());

        clientRequestedExtensions.clear();
        clientRequestedExtensions.add("ping-pong");
        clientRequestedExtensions.add("foo");
        clientRequestedExtensions.add("per-message-deflate");
        activeWebSocketExtensions = wsExtFactory.negotiateWebSocketExtensions(address, clientRequestedExtensions);
        assertEquals(3, activeWebSocketExtensions.asList().size());
        assertEquals("foo", activeWebSocketExtensions.asList().get(0).getExtensionHeader().getExtensionToken());
        assertEquals("ping-pong", activeWebSocketExtensions.asList().get(1).getExtensionHeader().getExtensionToken());
        assertEquals("per-message-deflate", activeWebSocketExtensions.asList().get(2).getExtensionHeader().getExtensionToken());

        clientRequestedExtensions.clear();
        clientRequestedExtensions.add("ping-pong");
        clientRequestedExtensions.add("per-message-deflate");
        clientRequestedExtensions.add("foo");
        activeWebSocketExtensions = wsExtFactory.negotiateWebSocketExtensions(address, clientRequestedExtensions);
        assertEquals(3, activeWebSocketExtensions.asList().size());
        assertEquals("foo", activeWebSocketExtensions.asList().get(0).getExtensionHeader().getExtensionToken());
        assertEquals("ping-pong", activeWebSocketExtensions.asList().get(1).getExtensionHeader().getExtensionToken());
        assertEquals("per-message-deflate", activeWebSocketExtensions.asList().get(2).getExtensionHeader().getExtensionToken());

        clientRequestedExtensions.clear();
        clientRequestedExtensions.add("per-message-deflate");
        clientRequestedExtensions.add("ping-pong");
        clientRequestedExtensions.add("foo");
        activeWebSocketExtensions = wsExtFactory.negotiateWebSocketExtensions(address, clientRequestedExtensions);
        assertEquals(3, activeWebSocketExtensions.asList().size());
        assertEquals("foo", activeWebSocketExtensions.asList().get(0).getExtensionHeader().getExtensionToken());
        assertEquals("ping-pong", activeWebSocketExtensions.asList().get(1).getExtensionHeader().getExtensionToken());
        assertEquals("per-message-deflate", activeWebSocketExtensions.asList().get(2).getExtensionHeader().getExtensionToken());

        clientRequestedExtensions.clear();
        clientRequestedExtensions.add("per-message-deflate");
        clientRequestedExtensions.add("foo");
        clientRequestedExtensions.add("ping-pong");
        activeWebSocketExtensions = wsExtFactory.negotiateWebSocketExtensions(address, clientRequestedExtensions);
        assertEquals(3, activeWebSocketExtensions.asList().size());
        assertEquals("foo", activeWebSocketExtensions.asList().get(0).getExtensionHeader().getExtensionToken());
        assertEquals("ping-pong", activeWebSocketExtensions.asList().get(1).getExtensionHeader().getExtensionToken());
        assertEquals("per-message-deflate", activeWebSocketExtensions.asList().get(2).getExtensionHeader().getExtensionToken());
    }

    @Test
    public void shouldNotNegotiateWebSocketExtensionWhenFactoryReturnsNull() throws ProtocolException {
        List<String> clientRequestedExtensions = new ArrayList<>();
        clientRequestedExtensions.add("mock");
        context.checking(new Expectations() {
            {
                oneOf(mockNegotiate).negotiate(with(new ExtensionHeaderTokenMatcher("mock")), with(address));
                will(returnValue(null));
            }
        });
        ActiveExtensions ActiveWebSocketExtensions =
                wsExtFactory.negotiateWebSocketExtensions(address, clientRequestedExtensions);
        Assert.assertEquals(0, ActiveWebSocketExtensions.asList().size());
    }

    @Test(expected = ProtocolException.class)
    public void shouldThrowProtocolExceptionDuringNegotiateIfThrownByFactorySPI() throws ProtocolException {
        List<String> clientRequestedExtensions = new ArrayList<>();
        clientRequestedExtensions.add("mock");
        context.checking(new Expectations() {
            {
                oneOf(mockNegotiate).negotiate(with(new ExtensionHeaderTokenMatcher("mock")), with(address));
                will(throwException(new ProtocolException("Protocol Exception")));
            }
        });
        wsExtFactory.negotiateWebSocketExtensions(address, clientRequestedExtensions);
    }

    public static class MockWebSocketExtensionSpi extends WebSocketExtensionFactorySpi {

        static MockNegotiate mockBehavior;

        @Override
        public String getExtensionName() {
            return "mock";
        }

        @Override
        public WebSocketExtension negotiate(ExtensionHeader requestedExtension, WsResourceAddress address)
                throws ProtocolException {
            return (mockBehavior == null) ? null : mockBehavior.negotiate(requestedExtension, address);
        }

        static void setNegotiateBehavoir(MockNegotiate behavoir) {
            mockBehavior = behavoir;
        }

        public interface MockNegotiate {
            WebSocketExtension negotiate(ExtensionHeader requestedExtension, WsResourceAddress address) throws ProtocolException;
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
        public WebSocketExtension negotiate(ExtensionHeader requestedExtension, WsResourceAddress address)
                throws ProtocolException {
            return new WebSocketExtension() {
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
        public WebSocketExtension negotiate(ExtensionHeader requestedExtension, WsResourceAddress address)
                throws ProtocolException {
            return new WebSocketExtension() {
                @Override
                public ExtensionHeader getExtensionHeader() {
                    return new ExtensionHeaderBuilder("per-message-deflate");
                }
            };
        }

        @Override
        public ExtensionOrderCategory getOrderCategory() {
            return NETWORK;
        }

        @Override
        public String getExtensionName() {
            return "per-message-deflate";
        }

    }

    public static class NetworkWebSocketExtensionFactory extends WebSocketExtensionFactorySpi {
        @Override
        public WebSocketExtension negotiate(ExtensionHeader requestedExtension, WsResourceAddress address)
                throws ProtocolException {
            return new WebSocketExtension() {
                @Override
                public ExtensionHeader getExtensionHeader() {
                    return new ExtensionHeaderBuilder("network");
                }
            };
        }

        @Override
        public ExtensionOrderCategory getOrderCategory() {
            return NETWORK;
        }

        @Override
        public String getExtensionName() {
            return "network";
        }

    }

    public static class PingPongWebSocketExtensionFactory extends WebSocketExtensionFactorySpi {

        @Override
        public WebSocketExtension negotiate(ExtensionHeader requestedExtension, WsResourceAddress address)
                throws ProtocolException {
            return new WebSocketExtension() {
                @Override
                public ExtensionHeader getExtensionHeader() {
                    return new ExtensionHeaderBuilder("ping-pong");
                }
            };
        }

        @Override
        public ExtensionOrderCategory getOrderCategory() {
            return EMULATION;
        }

        @Override
        public String getExtensionName() {
            return "ping-pong";
        }
    }

}
