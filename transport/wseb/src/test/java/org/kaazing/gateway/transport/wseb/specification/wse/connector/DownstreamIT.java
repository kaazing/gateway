/**

    @Test
    @Specification({
        "connection.established/handshake.request",
        "connection.established/handshake.response" })
    public void shouldEstablishConnection() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "request.header.origin/handshake.request",
        "request.header.origin/handshake.response" })
    public void shouldEstablishConnectionWithRequestHeaderOrigin()
            throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "request.header.x.websocket.protocol/handshake.request",
        "request.header.x.websocket.protocol/handshake.response" })
    public void shouldEstablishConnectionWithRequestHeaderXWebSocketProtocol()
            throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "request.header.x.websocket.extensions/handshake.request",
        "request.header.x.websocket.extensions/handshake.response" })
    public void shouldEstablishConnectionWithRequestHeaderXWebSocketExtensions()
            throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "request.with.body/handshake.request",
        "request.with.body/handshake.response" })
    // Server only test. Spec compliant clients ALWAYS use a POST request with an empty body.
    public void serverShouldTolerateNonEmptyRequestBody()
            throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "response.body.has.upstream.with.different.port/handshake.request",
        "response.body.has.upstream.with.different.port/handshake.response" })
    public void shouldEstablishConnectionWhenResponseBodyHasUpstreamWithDifferentPort()
            throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "response.body.has.downstream.with.different.port/handshake.request",
        "response.body.has.downstream.with.different.port/handshake.response" })
    public void shouldEstablishConnectionWhenResponseBodyHasDownstreamWithDifferentPort()
            throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "request.method.get/handshake.request",
        "request.method.get/handshake.response" })
    // Server only test. Spec compliant clients ALWAYS use POST.
    public void serverShouldTolerateRequestMethodGet() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "request.method.not.post.or.get/handshake.request",
        "request.method.not.post.or.get/handshake.response" })
    public void shouldFailHandshakeWhenRequestMethodNotPostOrGet() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "request.header.x.sequence.number.missing/handshake.request",
        "request.header.x.sequence.number.missing/handshake.response" })
    public void shouldFailHandshakeWhenRequestHeaderXSequenceNoIsMissing() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "request.header.x.sequence.number.negative/handshake.request",
        "request.header.x.sequence.number.negative/handshake.response" })
    public void shouldFailHandshakeWhenRequestHeaderXSequenceNoIsNegative() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "request.header.x.sequence.number.non.integer/handshake.request",
        "request.header.x.sequence.number.non.integer/handshake.response" })
    public void shouldFailHandshakeWhenRequestHeaderXSequenceNoIsNotInteger() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "request.header.x.sequence.number.out.of.range/handshake.request",
        "request.header.x.sequence.number.out.of.range/handshake.response" })
    public void shouldFailHandshakeWhenRequestHeaderXSequenceNoIsOutOfRange() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "request.header.x.websocket.version.missing/handshake.request",
        "request.header.x.websocket.version.missing/handshake.response" })
    public void shouldFailHandshakeWhenRequestHeaderXWebSocketVersionMissing()
            throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "request.header.x.websocket.version.not.wseb-1.0/handshake.request",
        "request.header.x.websocket.version.not.wseb-1.0/handshake.response" })
    public void shouldFailHandshakeWhenRequestHeaderXWebSocketVersionNotWseb10()
            throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "request.header.x.accept.commands.not.ping/handshake.request",
        "request.header.x.accept.commands.not.ping/handshake.response" })
    public void shouldFailHandshakeWhenHeaderXAcceptCommandsNotPing()
            throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "response.status.code.not.201/handshake.request",
        "response.status.code.not.201/handshake.response" })
    public void shouldFailConnectionWhenResponseStatusCodeNot201()
            throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "response.header.content.type.missing/handshake.request",
        "response.header.content.type.missing/handshake.response" })
    public void shouldFailConnectionWhenResponseHeaderContentTypeIsMissing()
            throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "response.header.content.type.not.text.plain.charset.utf-8/handshake.request",
        "response.header.content.type.not.text.plain.charset.utf-8/handshake.response" })
    public void shouldFailConnectionWhenResponseHeaderContentTypeNotTextPlainCharsetUTF8()
            throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "response.header.x.websocket.protocol.not.negotiated/handshake.request",
        "response.header.x.websocket.protocol.not.negotiated/handshake.response" })
    public void shouldFailConnectionWhenXWebSocketProtocolNotNegotiated()
            throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "response.header.x.websocket.extensions.not.negotiated/handshake.request",
        "response.header.x.websocket.extensions.not.negotiated/handshake.response" })
    public void shouldFailConnectionWhenXWebSocketExtensionsNotNegotiated()
            throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "response.body.with.no.downstream/handshake.request",
        "response.body.with.no.downstream/handshake.response" })
    public void shouldFailConnectionWhenResponseBodyHasNoDownstream()
            throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "response.body.has.upstream.with.scheme.not.http.or.https/handshake.request",
        "response.body.has.upstream.with.scheme.not.http.or.https/handshake.response" })
    public void shouldFailConnectionWhenResponseBodyHasUpstreamWithSchemeNotHttpOrHttps()
            throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "response.body.has.upstream.with.different.host/handshake.request",
        "response.body.has.upstream.with.different.host/handshake.response" })
    public void shouldFailConnectionWhenResponseBodyHasUpstreamWithDifferentHost()
            throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "response.body.has.upstream.with.different.path.prefix/handshake.request",
        "response.body.has.upstream.with.different.path.prefix/handshake.response" })
    public void shouldFailConnectionWhenResponseBodyHasUpstreamWithDifferentPathPrefix()
            throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "response.body.has.downstream.with.scheme.not.http.or.https/handshake.request",
        "response.body.has.downstream.with.scheme.not.http.or.https/handshake.response" })
    public void shouldFailConnectionWhenResponseBodyHasDownstreamWithSchemeNotHttpOrHttps()
            throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "response.body.has.downstream.with.different.host/handshake.request",
        "response.body.has.downstream.with.different.host/handshake.response" })
    public void shouldFailConnectionWhenResponseBodyHasDownstreamWithDifferentHost()
            throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "response.body.has.downstream.with.different.path.prefix/handshake.request",
        "response.body.has.downstream.with.different.path.prefix/handshake.response" })
    public void shouldFailConnectionWhenResponseBodyHasDownstreamWithDifferentPathPrefix()
            throws Exception {
        k3po.finish();
    } * Copyright 2007-2015, Kaazing Corporation. All rights reserved.
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
import static org.kaazing.test.util.ITUtil.timeoutRule;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;

import org.apache.mina.core.service.IoHandler;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.kaazing.gateway.transport.test.Expectations;
import org.kaazing.gateway.transport.wseb.test.WsebConnectorRule;
import org.kaazing.gateway.util.InternalSystemProperty;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.test.util.ITUtil;
import org.kaazing.test.util.MemoryAppender;
import org.kaazing.test.util.MethodExecutionTrace;

public class DownstreamIT {
    private static final Set<String> EMPTY_STRING_SET = Collections.emptySet();

    private K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/wse/downstream");

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
    // contextRule after k3po so we don't choke on exceptionCaught happening when k3po closes connections
    public TestRule chain = RuleChain.outerRule(trace).around(connector).around(k3po).around(contextRule)
            .around(timeoutRule);

    @Test
    @Specification("binary/response.header.content.type.has.unexpected.value/downstream.response")
    public void shouldCloseConnectionWhenBinaryDownstreamResponseContentTypeHasUnexpectedValue()
            throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(IOException.class)));
                allowing(handler).sessionClosed(with(any(IoSessionEx.class)));
            }
        });
        connector.connect("ws://localhost:8080/path?query", null, handler);
        k3po.finish();
        MemoryAppender.assertMessagesLogged(Arrays.asList(".*nexpected.*type.*"), EMPTY_STRING_SET, null, false);
    }

    @Test
    @Specification("binary/response.status.code.not.200/downstream.response")
    public void shouldCloseConnectionWhenBinaryDownstreamResponseStatusCodeNot200() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(IOException.class)));
                allowing(handler).sessionClosed(with(any(IoSessionEx.class)));
            }
        });
        connector.connect("ws://localhost:8080/path?query", null, handler);
        k3po.finish();
        MemoryAppender.assertMessagesLogged(Arrays.asList(".*nexpected.*status.*"), EMPTY_STRING_SET, null, false);
    }

    @Test
    @Specification("binary/server.send.frame.after.reconnect/downstream.response")
    public void shouldCloseConnectionWhenBinaryDownstreamResponseContainsFrameAfterReconnectFrame()
            throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(IOException.class)));
                allowing(handler).sessionClosed(with(any(IoSessionEx.class)));
            }
        });
        connector.connect("ws://localhost:8080/path?query", null, handler);
        k3po.finish();
        MemoryAppender.assertMessagesLogged(Arrays.asList(".*received.*after reconnect.*"), EMPTY_STRING_SET, null, false);
    }

    // Only relevant for browser clients
    @Specification("binary/request.header.origin/downstream.response")
    void shouldConnectWithDownstreamRequestOriginHeaderSet()
            throws Exception {
        k3po.finish();
    }

    // Server only test. Spec compliant clients ALWAYS use GET.
    @Specification("binary/request.method.post/downstream.response")
    void serverShouldTolerateDownstreamRequestMethodPost()
            throws Exception {
        k3po.finish();
    }

    // Server only test. Spec compliant clients ALWAYS use GET.
    @Specification("binary/request.method.post.with.body/downstream.response")
    void serverShouldTolerateDownstreamRequestMethodPostWithBody()
            throws Exception {
        k3po.finish();
    }

    // Server only test. Spec compliant clients ALWAYS use GET.
    @Specification("binary/request.method.not.get.or.post/downstream.response")
    void shouldRespondWithBadRequestWhenDownstreamRequestMethodNotGetOrPost()
            throws Exception {
        k3po.finish();
    }

    // Server only test. Spec compliant clients do not do this.
    @Specification("binary/request.out.of.order/downstream.response")
    void shouldCloseConnectionWhenBinaryDownstreamRequestIsOutOfOrder() throws Exception {
        k3po.finish();
    }

    // Server only test. Spec compliant clients do not do this.
    @Specification("binary/subsequent.request.out.of.order/response")
    void shouldCloseConnectionWhenSubsequentBinaryDownstreamRequestIsOutOfOrder() throws Exception {
        k3po.finish();
    }

    // Not relevant, WsebConnector always uses binary encoding
    @Specification("binary.as.escaped.text/response.header.content.type.has.unexpected.value/downstream.response")
    void shouldCloseConnectionWhenEscapedTextDownstreamResponseContentTypeHasUnexpectedValue()
            throws Exception {
        k3po.finish();
    }

    // Not relevant, WsebConnector always uses binary encoding
    @Specification("binary.as.mixed.text/response.header.content.type.has.unexpected.value/downstream.response")
    void shouldCloseConnectionWhenMixedTextDownstreamResponseContentTypeHasUnexpectedValue()
            throws Exception {
        k3po.finish();
    }

    // Not relevant, WsebConnector always uses binary encoding
    @Specification("binary.as.text/response.header.content.type.has.unexpected.value/downstream.response")
    void shouldCloseConnectionWhenTextDownstreamResponseContentTypeHasUnexpectedValue()
            throws Exception {
        k3po.finish();
    }

}
