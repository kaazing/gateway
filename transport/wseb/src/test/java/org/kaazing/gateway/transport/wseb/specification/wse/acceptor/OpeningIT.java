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
package org.kaazing.gateway.transport.wseb.specification.wse.acceptor;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.kaazing.gateway.resource.address.ws.WsResourceAddress.SUPPORTED_PROTOCOLS;
import static org.kaazing.test.util.ITUtil.timeoutRule;

import java.net.ProtocolException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.mina.core.service.IoHandler;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.resource.address.ws.WsResourceAddress;
import org.kaazing.gateway.transport.test.Expectations;
import org.kaazing.gateway.transport.ws.extension.ExtensionHeader;
import org.kaazing.gateway.transport.ws.extension.ExtensionHelper;
import org.kaazing.gateway.transport.ws.extension.WebSocketExtension;
import org.kaazing.gateway.transport.ws.extension.WebSocketExtensionFactorySpi;
import org.kaazing.gateway.transport.wseb.test.WsebAcceptorRule;
import org.kaazing.gateway.util.InternalSystemProperty;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.test.util.ITUtil;
import org.kaazing.test.util.MethodExecutionTrace;

public class OpeningIT {

    private K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/wse/opening");

    private WsebAcceptorRule acceptor;

    {
        Properties configuration = new Properties();
        configuration.setProperty(InternalSystemProperty.WS_CLOSE_TIMEOUT.getPropertyName(), "2s");
        acceptor = new WsebAcceptorRule(configuration);
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
    public TestRule chain = RuleChain.outerRule(trace).around(acceptor).around(contextRule).
             around(k3po).around(timeoutRule);

    ResourceAddressFactory resourceAddressFactory = ResourceAddressFactory.newResourceAddressFactory();

    @Test
    @Specification("connection.established/handshake.request")
    public void shouldEstablishConnection() throws Exception {
        acceptor.bind("wse://localhost:8080/path", mockHandler());
        k3po.finish();
    }

    @Test
    @Specification("request.header.origin/handshake.request")
    public void shouldEstablishConnectionWithRequestHeaderOrigin() throws Exception {
        acceptor.bind("wse://localhost:8080/path", mockHandler());
        k3po.finish();
    }

    @Test
    @Specification("request.header.x.websocket.protocol/handshake.request")
    public void shouldEstablishConnectionWithRequestHeaderXWebSocketProtocol() throws Exception {
        Map<String, Object> options = new HashMap<>();
        options.put(SUPPORTED_PROTOCOLS.name(), new String[]{"primary"});
        ResourceAddress address = resourceAddressFactory.newResourceAddress("wse://localhost:8080/path",
                options);
        acceptor.bind(address, mockHandler());
        k3po.finish();
    }

    @Test
    @Specification("request.header.x.websocket.extensions/handshake.request")
    public void shouldEstablishConnectionWithRequestHeaderXWebSocketExtensions() throws Exception {
        acceptor.bind("wse://localhost:8080/path", mockHandler());
        k3po.finish();
    }

    @Test
    @Specification("request.with.body/handshake.request")
    public void serverShouldTolerateNonEmptyRequestBody() throws Exception {
        acceptor.bind("wse://localhost:8080/path", mockHandler());
        k3po.finish();
    }

    // Client test only
    @Specification("response.body.has.upstream.with.different.port/handshake.request")
    void shouldEstablishConnectionWhenResponseBodyHasUpstreamWithDifferentPort()
            throws Exception {
        k3po.finish();
    }

    // Client test only
    @Specification("response.body.has.downstream.with.different.port/handshake.request")
    void shouldEstablishConnectionWhenResponseBodyHasDownstreamWithDifferentPort()
            throws Exception {
        k3po.finish();
    }

    @Test
    @Specification("request.method.get/handshake.request")
    public void serverShouldTolerateRequestMethodGet() throws Exception {
        acceptor.bind("wse://localhost:8080/path", mockHandler());
        k3po.finish();
    }

    @Test
    @Specification("request.method.not.post.or.get/handshake.request")
    public void shouldFailHandshakeWhenRequestMethodNotPostOrGet() throws Exception {
        IoHandler handler = context.mock(IoHandler.class);
        acceptor.bind("wse://localhost:8080/path", handler);
        k3po.finish();
    }

    @Test
    @Specification("request.header.x.sequence.number.missing/handshake.request")
    public void shouldFailHandshakeWhenRequestHeaderXSequenceNoIsMissing() throws Exception {
        IoHandler handler = context.mock(IoHandler.class);
        acceptor.bind("wse://localhost:8080/path", handler);
        k3po.finish();
    }

    @Test
    @Specification("request.header.x.sequence.number.negative/handshake.request")
    public void shouldFailHandshakeWhenRequestHeaderXSequenceNoIsNegative() throws Exception {
        IoHandler handler = context.mock(IoHandler.class);
        acceptor.bind("wse://localhost:8080/path", handler);
        k3po.finish();
    }

    @Test
    @Specification("request.header.x.sequence.number.non.integer/handshake.request")
    public void shouldFailHandshakeWhenRequestHeaderXSequenceNoIsNotInteger() throws Exception {
        IoHandler handler = context.mock(IoHandler.class);
        acceptor.bind("wse://localhost:8080/path", handler);
        k3po.finish();
    }

    @Test
    @Specification("request.header.x.sequence.number.out.of.range/handshake.request")
    public void shouldFailHandshakeWhenRequestHeaderXSequenceNoIsOutOfRange() throws Exception {
        IoHandler handler = context.mock(IoHandler.class);
        acceptor.bind("wse://localhost:8080/path", handler);
        k3po.finish();
    }

    @Test
    @Specification("request.header.x.websocket.version.missing/handshake.request")
    public void shouldFailHandshakeWhenRequestHeaderXWebSocketVersionMissing() throws Exception {
        IoHandler handler = context.mock(IoHandler.class);
        acceptor.bind("wse://localhost:8080/path", handler);
        k3po.finish();
    }

    @Test
    @Specification("request.header.x.websocket.version.not.wseb-1.0/handshake.request")
    public void shouldFailHandshakeWhenRequestHeaderXWebSocketVersionNotWseb10() throws Exception {
        IoHandler handler = context.mock(IoHandler.class);
        acceptor.bind("wse://localhost:8080/path", handler);
        k3po.finish();
    }

    @Test
    @Specification("request.header.x.accept.commands.not.ping/handshake.request")
    public void shouldFailHandshakeWhenHeaderXAcceptCommandsNotPing() throws Exception {
        IoHandler handler = context.mock(IoHandler.class);
        acceptor.bind("wse://localhost:8080/path", handler);
        k3po.finish();
    }

    @Test
    @Specification({ "connection.established.no.websocket.extensions/handshake.request" })
    public void shouldEstablishConnectionWithNoXWebSocketExtensions() throws Exception {
        acceptor.bind("wse://localhost:8080/path", mockHandler());
        k3po.finish();
    }

    // Client test only
    @Specification("response.status.code.not.201/handshake.request")
    void shouldFailConnectionWhenResponseStatusCodeNot201()
            throws Exception {
        k3po.finish();
    }

    // Client test only
    @Specification("response.header.content.type.missing/handshake.request")
    void shouldFailConnectionWhenResponseHeaderContentTypeIsMissing()
            throws Exception {
        k3po.finish();
    }

    // Client test only
    @Specification("response.header.content.type.not.text.plain.charset.utf-8/handshake.request")
    void shouldFailConnectionWhenResponseHeaderContentTypeNotTextPlainCharsetUTF8()
            throws Exception {
        k3po.finish();
    }

    // Client test only
    @Specification("response.header.x.websocket.protocol.not.negotiated/handshake.request")
    void shouldFailConnectionWhenXWebSocketProtocolNotNegotiated()
            throws Exception {
        k3po.finish();
    }

    // Client test only
    @Specification("response.header.x.websocket.extensions.not.negotiated/handshake.request")
    void shouldFailConnectionWhenXWebSocketExtensionsNotNegotiated()
            throws Exception {
        k3po.finish();
    }

    // Client test only
    @Specification("response.body.with.no.downstream/handshake.request")
    void shouldFailConnectionWhenResponseBodyHasNoDownstream()
            throws Exception {
        k3po.finish();
    }

    // Client test only
    @Specification("response.body.has.upstream.with.scheme.not.http.or.https/handshake.request")
    void shouldFailConnectionWhenResponseBodyHasUpstreamWithSchemeNotHttpOrHttps()
            throws Exception {
        k3po.finish();
    }

    // Client test only
    @Specification("response.body.has.upstream.with.different.host/handshake.request")
    void shouldFailConnectionWhenResponseBodyHasUpstreamWithDifferentHost()
            throws Exception {
        k3po.finish();
    }

    // Client test only
    @Specification("response.body.has.upstream.with.different.path.prefix/handshake.request")
    void shouldFailConnectionWhenResponseBodyHasUpstreamWithDifferentPathPrefix()
            throws Exception {
        k3po.finish();
    }

    // Client test only
    @Specification("response.body.has.downstream.with.scheme.not.http.or.https/handshake.request")
    void shouldFailConnectionWhenResponseBodyHasDownstreamWithSchemeNotHttpOrHttps()
            throws Exception {
        k3po.finish();
    }

    // Client test only
    @Specification("response.body.has.downstream.with.different.host/handshake.request")
    void shouldFailConnectionWhenResponseBodyHasDownstreamWithDifferentHost()
            throws Exception {
        k3po.finish();
    }

    // Client test only
    @Specification("response.body.has.downstream.with.different.path.prefix/handshake.request")
    void shouldFailConnectionWhenResponseBodyHasDownstreamWithDifferentPathPrefix()
            throws Exception {
        k3po.finish();
    }

    public static class ExtensionFactory extends WebSocketExtensionFactorySpi {

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
    }

    private IoHandler mockHandler() throws Exception {
        IoHandler handler = context.mock(IoHandler.class);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
            }
        });
        return handler;
    }
}
