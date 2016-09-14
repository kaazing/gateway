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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IoSession;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.test.Expectations;
import org.kaazing.gateway.transport.wseb.test.WsebConnectorRule;
import org.kaazing.gateway.util.InternalSystemProperty;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.test.util.ITUtil;
import org.kaazing.test.util.MemoryAppender;
import org.kaazing.test.util.MethodExecutionTrace;

public class OpeningIT {
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
    @Specification("request.header.x.websocket.protocol/handshake.response")
    public void shouldEstablishConnectionWithRequestHeaderXWebSocketProtocol() throws Exception {
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
        connectOptions.put("supportedProtocols", new String[]{"primary", "secondary"});
        final ResourceAddress connectAddress =
                ResourceAddressFactory.newResourceAddressFactory().newResourceAddress(
                        "ws://localhost:8080/path?query",
                        connectOptions);

        connector.connect(connectAddress, handler).getSession();
        k3po.finish();
    }

    // Server only test. Spec compliant clients ALWAYS use a POST request with an empty body.
    void serverShouldTolerateNonEmptyRequestBody()
            throws Exception {
        k3po.finish();
    }

    @Test
    @Specification("response.body.has.upstream.with.different.port/handshake.response")
    public void shouldEstablishConnectionWhenResponseBodyHasUpstreamWithDifferentPort() throws Exception {
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

    @Test
    @Specification("response.body.has.downstream.with.different.port/handshake.response")
    public void shouldEstablishConnectionWhenResponseBodyHasDownstreamWithDifferentPort() throws Exception {
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

    @Specification("request.method.get/handshake.response")
    // Server only test. Spec compliant clients ALWAYS use POST.
    void serverShouldTolerateRequestMethodGet() throws Exception {
        k3po.finish();
    }

    // Server only test.
    @Specification("request.method.not.post.or.get/handshake.response")
    void shouldFailHandshakeWhenRequestMethodNotPostOrGet() throws Exception {
        k3po.finish();
    }

    // Server only test.
    @Specification("request.header.x.sequence.number.missing/handshake.response")
    void shouldFailHandshakeWhenRequestHeaderXSequenceNoIsMissing() throws Exception {
        k3po.finish();
    }

    // Server only test.
    @Specification("request.header.x.sequence.number.negative/handshake.response")
    void shouldFailHandshakeWhenRequestHeaderXSequenceNoIsNegative() throws Exception {
        k3po.finish();
    }

    // Server only test.
    @Specification("request.header.x.sequence.number.non.integer/handshake.response")
    void shouldFailHandshakeWhenRequestHeaderXSequenceNoIsNotInteger() throws Exception {
        k3po.finish();
    }

    // Server only test.
    @Specification("request.header.x.sequence.number.out.of.range/handshake.response")
    void shouldFailHandshakeWhenRequestHeaderXSequenceNoIsOutOfRange() throws Exception {
        k3po.finish();
    }

    // Server only test.
    @Specification("request.header.x.websocket.version.missing/handshake.response")
    void shouldFailHandshakeWhenRequestHeaderXWebSocketVersionMissing()
            throws Exception {
        k3po.finish();
    }

    // Server only test.
    @Specification("request.header.x.websocket.version.not.wseb-1.0/handshake.response")
    void shouldFailHandshakeWhenRequestHeaderXWebSocketVersionNotWseb10()
            throws Exception {
        k3po.finish();
    }

    // Server only test.
    @Specification("request.header.x.accept.commands.not.ping/handshake.response")
    void shouldFailHandshakeWhenHeaderXAcceptCommandsNotPing()
            throws Exception {
        k3po.finish();
    }

    @Test
    @Specification("response.status.code.not.201/handshake.response")
    public void shouldFailConnectionWhenResponseStatusCodeNot201() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Exception.class)));
                allowing(handler).sessionClosed(with(any(IoSessionEx.class)));
            }
        });
        IoSession session = connector.connect("ws://localhost:8080/path?query", null, handler).getSession();
        k3po.finish();
        assertTrue(session.getCloseFuture().await(4, SECONDS));
        MemoryAppender.assertMessagesLogged(Collections.singletonList("response.*status.*"), EMPTY_STRING_SET, null, false);
    }

    @Test
    @Specification("response.header.content.type.missing/handshake.response")
    public void shouldFailConnectionWhenResponseHeaderContentTypeIsMissing() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Exception.class)));
                allowing(handler).sessionClosed(with(any(IoSessionEx.class)));
            }
        });
        IoSession session = connector.connect("ws://localhost:8080/path?query", null, handler).getSession();
        k3po.finish();
        assertTrue(session.getCloseFuture().await(4, SECONDS));
        MemoryAppender.assertMessagesLogged(Collections.singletonList("response.*content.*type"), EMPTY_STRING_SET, null, false);
    }

    @Test
    @Specification("response.header.content.type.not.text.plain.charset.utf-8/handshake.response")
    public void shouldFailConnectionWhenResponseHeaderContentTypeNotTextPlainCharsetUTF8() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Exception.class)));
                allowing(handler).sessionClosed(with(any(IoSessionEx.class)));
            }
        });
        IoSession session = connector.connect("ws://localhost:8080/path?query", null, handler).getSession();
        k3po.finish();
        assertTrue(session.getCloseFuture().await(4, SECONDS));
        MemoryAppender.assertMessagesLogged(Collections.singletonList("response.*content.*type"), EMPTY_STRING_SET, null, false);
    }

    @Test
    @Specification("response.header.x.websocket.protocol.not.negotiated/handshake.response")
    public void shouldFailConnectionWhenXWebSocketProtocolNotNegotiated() throws Exception {
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
        connectOptions.put("nextProtocol", "primary");
        connectOptions.put("supportedProtocols", new String[]{"secondary"});
        final ResourceAddress connectAddress =
                ResourceAddressFactory.newResourceAddressFactory().newResourceAddress(
                        "ws://localhost:8080/path?query",
                        connectOptions);

        IoSession session = connector.connect(connectAddress, handler).getSession();

        k3po.finish();
        assertTrue(session.getCloseFuture().await(4, SECONDS));
        MemoryAppender.assertMessagesLogged(Collections.singletonList("WebSocket.*protocol"), EMPTY_STRING_SET, null, false);
    }

    @Test
    @Specification("response.body.with.no.downstream/handshake.response")
    public void shouldFailConnectionWhenResponseBodyHasNoDownstream() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Exception.class)));
                allowing(handler).sessionClosed(with(any(IoSessionEx.class)));
            }
        });
        IoSession session = connector.connect("ws://localhost:8080/path?query", null, handler).getSession();
        k3po.finish();
        assertTrue(session.getCloseFuture().await(4, SECONDS));
        MemoryAppender.assertMessagesLogged(Collections.singletonList("invalid response"), EMPTY_STRING_SET, null, false);
    }

    @Test
    @Specification({
        "response.body.create.https.has.upstream.with.scheme.not.https/handshake.request",
        "response.body.create.https.has.upstream.with.scheme.not.https/handshake.response" })
    @Ignore("k3po does not yet support https")
    public void shouldFailConnectionWhenCreateHttpsResponseBodyHasUpstreamWithSchemeNotHttps()
            throws Exception {

        final IoHandler handler = context.mock(IoHandler.class);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Exception.class)));
                allowing(handler).sessionClosed(with(any(IoSessionEx.class)));
            }
        });
        IoSession session = connector.connect("ws://localhost:8080/path?query", null, handler).getSession();
        k3po.finish();
        assertTrue(session.getCloseFuture().await(4, SECONDS));
        MemoryAppender.assertMessagesLogged(Collections.singletonList("upstream"), EMPTY_STRING_SET, null, false);
    }

    @Test
    @Specification("response.body.has.upstream.with.scheme.not.http.or.https/handshake.response")
    public void shouldFailConnectionWhenResponseBodyHasUpstreamWithSchemeNotHttpOrHttps() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Exception.class)));
                allowing(handler).sessionClosed(with(any(IoSessionEx.class)));
            }
        });
        IoSession session = connector.connect("ws://localhost:8080/path?query", null, handler).getSession();
        k3po.finish();
        assertTrue(session.getCloseFuture().await(4, SECONDS));
        MemoryAppender.assertMessagesLogged(Collections.singletonList("upstream"), EMPTY_STRING_SET, null, false);
    }

    @Test
    @Specification({
        "response.body.create.https.has.downstream.with.scheme.not.https/handshake.request",
        "response.body.create.https.has.downstream.with.scheme.not.https/handshake.response" })
    @Ignore("k3po does not yet support https")
    public void shouldFailConnectionWhenCreateHttpsResponseBodyHasDownstreamWithSchemeNotHttps()
            throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Exception.class)));
                allowing(handler).sessionClosed(with(any(IoSessionEx.class)));
            }
        });
        IoSession session = connector.connect("ws://localhost:8080/path?query", null, handler).getSession();
        k3po.finish();
        assertTrue(session.getCloseFuture().await(4, SECONDS));
        MemoryAppender.assertMessagesLogged(Collections.singletonList("upstream"), EMPTY_STRING_SET, null, false);
    }

    @Test
    @Specification("response.body.has.upstream.with.different.host/handshake.response")
    public void shouldFailConnectionWhenResponseBodyHasUpstreamWithDifferentHost() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Exception.class)));
                allowing(handler).sessionClosed(with(any(IoSessionEx.class)));
            }
        });
        IoSession session = connector.connect("ws://localhost:8080/path?query", null, handler).getSession();
        k3po.finish();
        assertTrue(session.getCloseFuture().await(4, SECONDS));
        MemoryAppender.assertMessagesLogged(Collections.singletonList("upstream"), EMPTY_STRING_SET, null, false);
    }

    @Test
    @Specification("response.body.has.upstream.with.different.path.prefix/handshake.response")
    public void shouldFailConnectionWhenResponseBodyHasUpstreamWithDifferentPathPrefix() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Exception.class)));
                allowing(handler).sessionClosed(with(any(IoSessionEx.class)));
            }
        });
        IoSession session = connector.connect("ws://localhost:8080/path?query", null, handler).getSession();
        k3po.finish();
        assertTrue(session.getCloseFuture().await(4, SECONDS));
        MemoryAppender.assertMessagesLogged(Collections.singletonList("upstream"), EMPTY_STRING_SET, null, false);
    }

    @Test
    @Specification("response.body.has.downstream.with.scheme.not.http.or.https/handshake.response")
    public void shouldFailConnectionWhenResponseBodyHasDownstreamWithSchemeNotHttpOrHttps()
            throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Exception.class)));
                allowing(handler).sessionClosed(with(any(IoSessionEx.class)));
            }
        });
        IoSession session = connector.connect("ws://localhost:8080/path?query", null, handler).getSession();
        k3po.finish();
        assertTrue(session.getCloseFuture().await(4, SECONDS));
        MemoryAppender.assertMessagesLogged(Collections.singletonList("downstream"), EMPTY_STRING_SET, null, false);
    }

    @Test
    @Specification("response.body.has.downstream.with.different.host/handshake.response")
    public void shouldFailConnectionWhenResponseBodyHasDownstreamWithDifferentHost()
            throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Exception.class)));
                allowing(handler).sessionClosed(with(any(IoSessionEx.class)));
            }
        });
        IoSession session = connector.connect("ws://localhost:8080/path?query", null, handler).getSession();
        k3po.finish();
        assertTrue(session.getCloseFuture().await(4, SECONDS));
        MemoryAppender.assertMessagesLogged(Collections.singletonList("downstream"), EMPTY_STRING_SET, null, false);
    }

    @Test
    @Specification("response.body.has.downstream.with.different.path.prefix/handshake.response")
    public void shouldFailConnectionWhenResponseBodyHasDownstreamWithDifferentPathPrefix()
            throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Exception.class)));
                allowing(handler).sessionClosed(with(any(IoSessionEx.class)));
            }
        });
        IoSession session = connector.connect("ws://localhost:8080/path?query", null, handler).getSession();
        k3po.finish();
        assertTrue(session.getCloseFuture().await(4, SECONDS));
        MemoryAppender.assertMessagesLogged(Collections.singletonList("downstream"), EMPTY_STRING_SET, null, false);
    }

    @Test
    @Specification("connection.established.no.websocket.extensions/handshake.response")
    public void shouldEstablishConnectionWithNoXWebSocketExtensions() throws Exception {
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
        ResourceAddress connectAddress =
                ResourceAddressFactory.newResourceAddressFactory().newResourceAddress(
                        "wse://localhost:8080/path?query",
                        connectOptions);

        connector.connect(connectAddress, handler).getSession();
        k3po.finish();
    }

}
