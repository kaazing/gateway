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
package org.kaazing.gateway.transport.http.connector.specification.rfc7230;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;
import org.jmock.Expectations;
import org.jmock.api.Invocation;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.jmock.lib.action.CustomAction;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.gateway.transport.http.HttpConnectSession;
import org.kaazing.gateway.transport.http.HttpConnectorRule;
import org.kaazing.gateway.transport.http.HttpHeaders;
import org.kaazing.gateway.transport.http.HttpMethod;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.test.util.ITUtil;
import org.kaazing.test.util.MethodExecutionTrace;

public class MessageFormatIT {

    private final HttpConnectorRule connector = new HttpConnectorRule();

    private JUnitRuleMockery context = new JUnitRuleMockery() {
        {
            setThreadingPolicy(new Synchroniser());
        }
    };

    private final TestRule trace = new MethodExecutionTrace();
    private TestRule contextRule = ITUtil.toTestRule(context);
    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/http/rfc7230/message.format");
    private final TestRule timeoutRule = new DisableOnDebug(new Timeout(5, SECONDS));

    @Rule
    public TestRule chain = RuleChain.outerRule(trace).around(connector).around(contextRule).around(k3po).around(timeoutRule);

    @Test
    @Specification({"inbound.should.accept.headers/response"})
    public void inboundShouldAcceptHeaders() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch closed = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        closed.countDown();
                        return null;
                    }
                });
            }
        });

        connector.connect("http://localhost:8080/", handler, new ConnectSessionInitializerGetHeader());
        closed.await(2, SECONDS);

        k3po.finish();
    }

    @Test
    @Specification({"outbound.should.accept.no.headers/response"})
    public void outboundShouldAcceptNoHeaders() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch closed = new CountDownLatch(1);

        connector.getConnectOptions().put("http.userAgentHeaderEnabled", Boolean.FALSE);
        connector.getConnectOptions().put("http.hostHeaderEnabled", Boolean.FALSE);


        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        closed.countDown();
                        return null;
                    }
                });
            }
        });

        connector.connect("http://localhost:8080/", handler, new ConnectSessionInitializerGetUriChange());
        assertTrue(closed.await(2, SECONDS));

        k3po.finish();
    }

    @Test
    @Specification({"outbound.should.accept.headers/response"})
    public void outboundShouldAcceptHeaders() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch closed = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        closed.countDown();
                        return null;
                    }
                });
            }
        });

        connector.connect("http://localhost:8080/", handler, new ConnectSessionInitializerGet());
        assertTrue(closed.await(2, SECONDS));

        k3po.finish();
    }

    @Test
    @Specification({"request.must.start.with.request.line/response"})
    public void requestMustStartWithRequestLine() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch closed = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        closed.countDown();
                        return null;
                    }
                });
            }
        });

        connector.connect("http://localhost:8080/", handler, new ConnectSessionInitializerGet());
        assertTrue(closed.await(2, SECONDS));

        k3po.finish();
    }

    @Test
    @Specification({"server.should.send.414.to.request.with.too.long.a.request/response"})
    public void serverShouldSend414ToRequestWithTooLongARequest() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch closed = new CountDownLatch(1);

        connector.getConnectOptions().put("http.userAgentHeaderEnabled", Boolean.FALSE);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        closed.countDown();
                        return null;
                    }
                });
            }
        });

        connector.connect("http://localhost:8080/", handler, new ConnectSessionInitializerGetLongRequest());
        assertTrue(closed.await(2, SECONDS));

        k3po.finish();
    }

    @Test
    @Specification({"server.should.send.status.line.in.start.line/response"})
    public void serverShouldSendStatusLineInStartLine() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch closed = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        closed.countDown();
                        return null;
                    }
                });
            }
        });

        connector.connect("http://localhost:8080/", handler, new ConnectSessionInitializerGet());
        assertTrue(closed.await(2, SECONDS));

        k3po.finish();
    }

    @Test
    @Specification({"server.should.reject.obs.in.header.value/response"})
    public void serverShouldRejectOBSInHeaderValue() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch closed = new CountDownLatch(1);

        connector.getConnectOptions().put("http.userAgentHeaderEnabled", Boolean.FALSE);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        closed.countDown();
                        return null;
                    }
                });
            }
        });

        connector.connect("http://localhost:8080/", handler, new ConnectSessionInitializerOBSInHeader());
        assertTrue(closed.await(2, SECONDS));

        k3po.finish();
    }

    @Test
    @Specification({"proxy.or.gateway.must.reject.obs.in.header.value/response"})
    public void proxyOrGatewayMustRejectOBSInHeaderValue() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch closed = new CountDownLatch(1);

        connector.getConnectOptions().put("http.userAgentHeaderEnabled", Boolean.FALSE);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        closed.countDown();
                        return null;
                    }
                });
            }
        });

        connector.connect("http://localhost:8080/", handler, new ConnectSessionInitializerOBSInHeader());
        assertTrue(closed.await(2, SECONDS));

        k3po.finish();
    }

    @Test
    @Specification({"inbound.on.receiving.field.with.length.larger.than.wanting.to.process.must.reply.with.4xx/response"})
    public void inboundOnReceivingFieldWithLengthLargerThanWantingToProcessMustReplyWith4xx() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch closed = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        closed.countDown();
                        return null;
                    }
                });
            }
        });

        connector.connect("http://localhost:8080/", handler, new ConnectSessionInitializerGetLongHeader());
        assertTrue(closed.await(2, SECONDS));

        k3po.finish();
    }

    @Test
    @Specification({"server.should.send.501.to.unknown.transfer.encoding/response"})
    public void serverShouldSend501ToUnknownTransferEncoding() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch closed = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        closed.countDown();
                        return null;
                    }
                });
            }
        });

        connector.connect("http://localhost:8080/", handler, new ConnectSessionInitializerTransferEncoding());
        assertTrue(closed.await(2, SECONDS));

        k3po.finish();
    }

    @Ignore("Assertion Error: Unexpected Invocation")
    @Test
    @Specification({"outbound.should.process.response.with.content.length/response"})
    public void outboundShouldProcessResponseWithContentLength() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch closed = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        closed.countDown();
                        return null;
                    }
                });
            }
        });

        connector.connect("http://localhost:8080/", handler, new ConnectSessionInitializerGet());
        closed.await(2, SECONDS);

        k3po.finish();
    }

    @Test
    @Specification({"inbound.should.process.request.with.content.length/response"})
    public void inboundShouldProcessRequestWithContentLength() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch closed = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        closed.countDown();
                        return null;
                    }
                });
            }
        });

        connector.connect("http://localhost:8080/", handler, new ConnectSessionInitializerPostNoContent());
        assertTrue(closed.await(2, SECONDS));

        k3po.finish();
    }

    @Test
    @Specification({"client.should.send.content.length.header.in.post.even.if.no.content/response"})
    public void clientShouldSendContentLengthHeaderInPostEvenIfNoContent() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch closed = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        closed.countDown();
                        return null;
                    }
                });
            }
        });

        connector.connect("http://localhost:8080/", handler, new ConnectSessionInitializerPostNoContent());
        assertTrue(closed.await(2, SECONDS));

        k3po.finish();
    }

    @Test
    @Specification({"head.response.must.not.have.content/response"})
    public void headResponseMustNotHaveContent() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch closed = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        closed.countDown();
                        return null;
                    }
                });
            }
        });

        connector.connect("http://localhost:8080/", handler, new ConnectSessionInitializerHead());
        assertTrue(closed.await(2, SECONDS));

        k3po.finish();
    }

    @Test
    @Specification({"head.response.must.not.have.content.though.may.have.content.length/response"})
    public void headResponseMustNotHaveContentThoughMayHaveContentLength() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch closed = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        closed.countDown();
                        return null;
                    }
                });
            }
        });

        connector.connect("http://localhost:8080/", handler, new ConnectSessionInitializerHead());
        assertTrue(closed.await(2, SECONDS));

        k3po.finish();
    }

    @Test
    @Specification({"server.must.reject.request.with.multiple.different.content.length/response"})
    public void serverMustRejectRequestWithMultipleDifferentContentLength() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch closed = new CountDownLatch(1);

        connector.getConnectOptions().put("http.userAgentHeaderEnabled", Boolean.FALSE);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        closed.countDown();
                        return null;
                    }
                });
            }
        });

        connector.connect("http://localhost:8080/", handler, new ConnectSessionInitializerPostErrored());
        assertTrue(closed.await(2, SECONDS));

        k3po.finish();
    }

    @Test
    @Specification({"non.http.request.to.http.server.should.be.responded.to.with.400/response"})
    public void nonHttpRequestToHttpServerShouldBeRespondedToWith400() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch closed = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        closed.countDown();
                        return null;
                    }
                });
            }
        });

        connector.connect("http://localhost:8080/", handler, new ConnectSessionInitializerGet());
        assertTrue(closed.await(2, SECONDS));

        k3po.finish();
    }

    private static class ConnectSessionInitializerHead implements IoSessionInitializer<ConnectFuture> {
        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            HttpConnectSession connectSession = (HttpConnectSession) session;
            connectSession.setMethod(HttpMethod.HEAD);
        }
    }

    private static class ConnectSessionInitializerGet implements IoSessionInitializer<ConnectFuture> {
        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            HttpConnectSession connectSession = (HttpConnectSession) session;
            connectSession.setMethod(HttpMethod.GET);
        }
    }

    private static class ConnectSessionInitializerGetLongHeader implements IoSessionInitializer<ConnectFuture> {
        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            HttpConnectSession connectSession = (HttpConnectSession) session;
            connectSession.setMethod(HttpMethod.GET);
            connectSession.addWriteHeader("header", "long_string");
            connectSession.close(false);
        }
    }

    private static class ConnectSessionInitializerGetLongRequest implements IoSessionInitializer<ConnectFuture> {
        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            HttpConnectSession connectSession = (HttpConnectSession) session;
            connectSession.setRequestURI(URI.create("http://localhost:8080/"));
            connectSession.setMethod(HttpMethod.GET);
            connectSession.write(((IoSessionEx) session).getBufferAllocator().wrap(ByteBuffer.wrap(new byte[7911])));
        }
    }

    private static class ConnectSessionInitializerGetUriChange implements IoSessionInitializer<ConnectFuture> {
        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            HttpConnectSession connectSession = (HttpConnectSession) session;
            connectSession.setRequestURI(URI.create("http://localhost:8080/"));
            connectSession.setMethod(HttpMethod.GET);
        }
    }

    private static class ConnectSessionInitializerGetHeader implements IoSessionInitializer<ConnectFuture> {
        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            HttpConnectSession connectSession = (HttpConnectSession) session;
            connectSession.setMethod(HttpMethod.GET);
            connectSession.addWriteHeader("some", "header");
            connectSession.close(false);
        }
    }

    private static class ConnectSessionInitializerOBSInHeader implements IoSessionInitializer<ConnectFuture> {
        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            HttpConnectSession connectSession = (HttpConnectSession) session;
            connectSession.setMethod(HttpMethod.GET);
            connectSession.addWriteHeader(HttpHeaders.HEADER_HOST, "localhost:8080");
            connectSession.addWriteHeader("BadHeader", String.valueOf("value\r\nPart of value\tstill part of value"));
        }
    }

    private static class ConnectSessionInitializerTransferEncoding implements IoSessionInitializer<ConnectFuture> {
        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            HttpConnectSession connectSession = (HttpConnectSession) session;
            connectSession.setMethod(HttpMethod.GET);
            connectSession.addWriteHeader(HttpHeaders.HEADER_TRANSFER_ENCODING, "Unknown");
        }
    }

    private static class ConnectSessionInitializerPostErrored implements IoSessionInitializer<ConnectFuture> {
        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            HttpConnectSession connectSession = (HttpConnectSession) session;
            connectSession.setMethod(HttpMethod.POST);
            connectSession.addWriteHeader(HttpHeaders.HEADER_HOST, "localhost:8080");
            connectSession.addWriteHeader(HttpHeaders.HEADER_CONTENT_LENGTH, String.valueOf(0));
            connectSession.addWriteHeader(HttpHeaders.HEADER_CONTENT_LENGTH, String.valueOf(0));
        }
    }

    private static class ConnectSessionInitializerPostNoContent implements IoSessionInitializer<ConnectFuture> {
        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            HttpConnectSession connectSession = (HttpConnectSession) session;
            connectSession.setMethod(HttpMethod.POST);
            connectSession.addWriteHeader(HttpHeaders.HEADER_CONTENT_LENGTH, String.valueOf(0));
        }
    }

}
