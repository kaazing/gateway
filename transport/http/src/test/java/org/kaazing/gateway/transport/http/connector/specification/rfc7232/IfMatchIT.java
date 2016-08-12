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
package org.kaazing.gateway.transport.http.connector.specification.rfc7232;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertTrue;

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

public class IfMatchIT {

    private final HttpConnectorRule connector = new HttpConnectorRule();

    private JUnitRuleMockery context = new JUnitRuleMockery() {
        {
            setThreadingPolicy(new Synchroniser());
        }
    };

    private final TestRule trace = new MethodExecutionTrace();
    private TestRule contextRule = ITUtil.toTestRule(context);
    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/http/rfc7232/preconditions/if.match");
    private final TestRule timeoutRule = new DisableOnDebug(new Timeout(5, SECONDS));

    @Rule
    public TestRule chain = RuleChain.outerRule(trace).around(connector).around(contextRule).around(k3po).around(timeoutRule);

    @Test
    @Specification({"multiple.etags.delete.status.200/response"})
    public void shouldSucceedWithDeleteAndMatchingETagInTheList() throws Exception {
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
        connector.connect("http://localhost:8000/index.html", handler, new ConnectSessionInitializer());
        assertTrue(closed.await(2, SECONDS));
        final CountDownLatch closed2 = new CountDownLatch(1);
        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        closed2.countDown();
                        return null;
                    }
                });
            }
        });
        connector.connect("http://localhost:8000/index.html", handler, new ConnectSessionInitializerDelete());
        assertTrue(closed2.await(2, SECONDS));

        k3po.finish();
    }

    private static class ConnectSessionInitializerDelete implements IoSessionInitializer<ConnectFuture> {
        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            HttpConnectSession connectSession = (HttpConnectSession) session;
            connectSession.setMethod(HttpMethod.DELETE);
            connectSession.addWriteHeader(HttpHeaders.HEADER_HOST, "localhost:8000");
            connectSession.addWriteHeader(HttpHeaders.HEADER_IF_MATCH, "\"r2d2xxxx\"");
            connectSession.addWriteHeader(HttpHeaders.HEADER_IF_MATCH, "\"tag1\"");
            connectSession.addWriteHeader(HttpHeaders.HEADER_IF_MATCH, "\"tag2\"");
            connectSession.addWriteHeader(HttpHeaders.HEADER_CONTENT_LENGTH, String.valueOf(7));
            ByteBuffer bytes = ByteBuffer.wrap("content".getBytes());
            connectSession.write(connectSession.getBufferAllocator().wrap(bytes));

        }
    }

    @Test
    @Specification({"multiple.etags.get.status.200/response"})
    public void shouldIgnoreIfMatchHeaderWithGetAndMatchingETagInTheList() throws Exception {
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

        connector.connect("http://localhost:8000/index.html", handler, new ConnectSessionInitializer());
        assertTrue(closed.await(2, SECONDS));
        final CountDownLatch closed2 = new CountDownLatch(1);
        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        closed2.countDown();
                        return null;
                    }
                });
            }
        });
        connector.connect("http://localhost:8000/index.html", handler, new ConnectSessionInitializerGet());
        assertTrue(closed2.await(2, SECONDS));

        k3po.finish();
    }

    private static class ConnectSessionInitializerGet implements IoSessionInitializer<ConnectFuture> {
        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            HttpConnectSession connectSession = (HttpConnectSession) session;
            connectSession.setMethod(HttpMethod.GET);
            connectSession.addWriteHeader(HttpHeaders.HEADER_HOST, "localhost:8000");
            connectSession.addWriteHeader(HttpHeaders.HEADER_IF_MATCH, "\"r2d2xxxx\"");
            connectSession.addWriteHeader(HttpHeaders.HEADER_IF_MATCH, "\"tag1\"");
            connectSession.addWriteHeader(HttpHeaders.HEADER_IF_MATCH, "\"tag2\"");
        }
    }

    @Test
    @Specification({"multiple.etags.head.status.200/response"})
    public void shouldIgnoreIfMatchHeaderWithHeadAndMatchingETagInTheList() throws Exception {
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

        connector.connect("http://localhost:8000/index.html", handler, new ConnectSessionInitializer());
        assertTrue(closed.await(2, SECONDS));
        final CountDownLatch closed2 = new CountDownLatch(1);
        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        closed2.countDown();
                        return null;
                    }
                });
            }
        });
        connector.connect("http://localhost:8000/index.html", handler, new ConnectSessionInitializerHead());

        assertTrue(closed2.await(2, SECONDS));

        k3po.finish();
    }

    private static class ConnectSessionInitializerHead implements IoSessionInitializer<ConnectFuture> {
        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            HttpConnectSession connectSession = (HttpConnectSession) session;
            connectSession.setMethod(HttpMethod.HEAD);
            connectSession.addWriteHeader(HttpHeaders.HEADER_HOST, "localhost:8000");
            connectSession.addWriteHeader(HttpHeaders.HEADER_IF_MATCH, "\"r2d2xxxx\"");
            connectSession.addWriteHeader(HttpHeaders.HEADER_IF_MATCH, "\"tag1\"");
            connectSession.addWriteHeader(HttpHeaders.HEADER_IF_MATCH, "\"tag2\"");
        }
    }

    @Test
    @Specification({"multiple.etags.post.status.200/response"})
    public void shouldSucceedWithPostAndMatchingETagInTheList() throws Exception {
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

        connector.connect("http://localhost:8000/index.html", handler, new ConnectSessionInitializer());
        assertTrue(closed.await(2, SECONDS));
        final CountDownLatch closed2 = new CountDownLatch(1);
        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        closed2.countDown();
                        return null;
                    }
                });
            }
        });
        connector.connect("http://localhost:8000/index.html", handler, new ConnectSessionInitializerPost());
        assertTrue(closed2.await(2, SECONDS));

        k3po.finish();
    }

    private static class ConnectSessionInitializerPost implements IoSessionInitializer<ConnectFuture> {
        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            HttpConnectSession connectSession = (HttpConnectSession) session;
            connectSession.setMethod(HttpMethod.POST);
            connectSession.addWriteHeader(HttpHeaders.HEADER_HOST, "localhost:8000");
            connectSession.addWriteHeader(HttpHeaders.HEADER_IF_MATCH, "\"r2d2xxxx\"");
            connectSession.addWriteHeader(HttpHeaders.HEADER_IF_MATCH, "\"tag1\"");
            connectSession.addWriteHeader(HttpHeaders.HEADER_IF_MATCH, "\"tag2\"");
            connectSession.addWriteHeader(HttpHeaders.HEADER_CONTENT_LENGTH, String.valueOf(7));
            ByteBuffer bytes = ByteBuffer.wrap("content".getBytes());
            connectSession.write(connectSession.getBufferAllocator().wrap(bytes));
        }
    }

    @Test
    @Specification({"multiple.etags.put.status.200/response"})
    public void shouldSucceedWithPutAndMatchingETagInTheList() throws Exception {
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
        connector.connect("http://localhost:8000/index.html", handler, new ConnectSessionInitializer());
        assertTrue(closed.await(2, SECONDS));
        final CountDownLatch closed2 = new CountDownLatch(1);
        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        closed2.countDown();
                        return null;
                    }
                });
            }
        });
        connector.connect("http://localhost:8000/index.html", handler, new ConnectSessionInitializerPut());
        assertTrue(closed2.await(2, SECONDS));

        k3po.finish();
    }

    private static class ConnectSessionInitializerPut implements IoSessionInitializer<ConnectFuture> {
        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            HttpConnectSession connectSession = (HttpConnectSession) session;
            connectSession.setMethod(HttpMethod.PUT);
            connectSession.addWriteHeader(HttpHeaders.HEADER_HOST, "localhost:8000");
            connectSession.addWriteHeader(HttpHeaders.HEADER_IF_MATCH, "\"r2d2xxxx\"");
            connectSession.addWriteHeader(HttpHeaders.HEADER_IF_MATCH, "\"tag1\"");
            connectSession.addWriteHeader(HttpHeaders.HEADER_IF_MATCH, "\"tag2\"");
            connectSession.addWriteHeader(HttpHeaders.HEADER_CONTENT_LENGTH, String.valueOf(7));
            ByteBuffer bytes = ByteBuffer.wrap("content".getBytes());
            connectSession.write(connectSession.getBufferAllocator().wrap(bytes));
        }
    }

    @Test
    @Specification({"strong.etag.delete.status.200/response"})
    public void shouldSucceedWithDeleteAndMatchingStrongETag() throws Exception {
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
        connector.connect("http://localhost:8000/index.html", handler, new ConnectSessionInitializer());
        assertTrue(closed.await(2, SECONDS));
        final CountDownLatch closed2 = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        closed2.countDown();
                        return null;
                    }
                });
            }
        });
        connector.connect("http://localhost:8000/index.html", handler, new ConnectSessionInitializerDeleteStrong());
        assertTrue(closed2.await(2, SECONDS));

        k3po.finish();
    }

    private static class ConnectSessionInitializerDeleteStrong implements IoSessionInitializer<ConnectFuture> {
        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            HttpConnectSession connectSession = (HttpConnectSession) session;
            connectSession.setMethod(HttpMethod.DELETE);
            connectSession.addWriteHeader(HttpHeaders.HEADER_HOST, "localhost:8000");
            connectSession.addWriteHeader(HttpHeaders.HEADER_IF_MATCH, "r2d2xxxx");
            connectSession.addWriteHeader(HttpHeaders.HEADER_CONTENT_LENGTH, String.valueOf(7));
            ByteBuffer bytes = ByteBuffer.wrap("content".getBytes());
            connectSession.write(connectSession.getBufferAllocator().wrap(bytes));
        }
    }

    @Test
    @Specification({"strong.etag.get.status.200/response"})
    public void shouldSucceedWithGetAndMatchingStrongETag() throws Exception {
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

        connector.connect("http://localhost:8000/index.html", handler, new ConnectSessionInitializer());
        assertTrue(closed.await(2, SECONDS));
        final CountDownLatch closed2 = new CountDownLatch(1);
        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        closed2.countDown();
                        return null;
                    }
                });
            }
        });
        connector.connect("http://localhost:8000/index.html", handler, new ConnectSessionInitializerGetSuccess());
        assertTrue(closed2.await(2, SECONDS));

        k3po.finish();
    }

    private static class ConnectSessionInitializerGetSuccess implements IoSessionInitializer<ConnectFuture> {
        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            HttpConnectSession connectSession = (HttpConnectSession) session;
            connectSession.setMethod(HttpMethod.GET);
            connectSession.addWriteHeader(HttpHeaders.HEADER_HOST, "localhost:8000");
            connectSession.addWriteHeader(HttpHeaders.HEADER_IF_MATCH, "r2d2xxxx");
        }
    }

    @Test
    @Specification({"strong.etag.head.status.200/response"})
    public void shouldSucceedWithHeadAndMatchingStrongETag() throws Exception {
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
        connector.connect("http://localhost:8000/index.html", handler, new ConnectSessionInitializer());
        assertTrue(closed.await(2, SECONDS));

        final CountDownLatch closed2 = new CountDownLatch(1);
        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        closed2.countDown();
                        return null;
                    }
                });
            }
        });
        connector.connect("http://localhost:8000/index.html", handler, new ConnectSessionInitializerHeadSuccess());
        assertTrue(closed2.await(2, SECONDS));

        k3po.finish();
    }

    private static class ConnectSessionInitializerHeadSuccess implements IoSessionInitializer<ConnectFuture> {
        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            HttpConnectSession connectSession = (HttpConnectSession) session;
            connectSession.setMethod(HttpMethod.HEAD);
            connectSession.addWriteHeader(HttpHeaders.HEADER_HOST, "localhost:8000");
            connectSession.addWriteHeader(HttpHeaders.HEADER_IF_MATCH, "r2d2xxxx");
        }
    }

    @Test
    @Specification({"strong.etag.post.status.200/response"})
    public void shouldSucceedWithPostAndMatchingStrongETag() throws Exception {
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

        connector.connect("http://localhost:8000/index.html", handler, new ConnectSessionInitializer());
        assertTrue(closed.await(2, SECONDS));
        final CountDownLatch closed2 = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        closed2.countDown();
                        return null;
                    }
                });
            }
        });
        connector.connect("http://localhost:8000/index.html", handler, new ConnectSessionInitializerPostSuccess());
        assertTrue(closed2.await(2, SECONDS));

        k3po.finish();
    }

    private static class ConnectSessionInitializerPostSuccess implements IoSessionInitializer<ConnectFuture> {
        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            HttpConnectSession connectSession = (HttpConnectSession) session;
            connectSession.setMethod(HttpMethod.POST);
            connectSession.addWriteHeader(HttpHeaders.HEADER_HOST, "localhost:8000");
            connectSession.addWriteHeader(HttpHeaders.HEADER_IF_MATCH, "r2d2xxxx");
            connectSession.addWriteHeader(HttpHeaders.HEADER_CONTENT_LENGTH, String.valueOf(7));
            ByteBuffer bytes = ByteBuffer.wrap("content".getBytes());
            connectSession.write(connectSession.getBufferAllocator().wrap(bytes));
        }
    }

    @Test
    @Specification({"strong.etag.put.status.200/response"})
    public void shouldSucceedWithPutAndMatchingStrongETag() throws Exception {
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

        connector.connect("http://localhost:8000/index.html", handler, new ConnectSessionInitializer());
        assertTrue(closed.await(2, SECONDS));
        final CountDownLatch closed2 = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        closed2.countDown();
                        return null;
                    }
                });
            }
        });
        connector.connect("http://localhost:8000/index.html", handler, new ConnectSessionInitializerPutSuccess());
        assertTrue(closed2.await(2, SECONDS));

        k3po.finish();
    }

    private static class ConnectSessionInitializerPutSuccess implements IoSessionInitializer<ConnectFuture> {
        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            HttpConnectSession connectSession = (HttpConnectSession) session;
            connectSession.setMethod(HttpMethod.PUT);
            connectSession.addWriteHeader(HttpHeaders.HEADER_IF_MATCH, "r2d2xxxx");
            connectSession.addWriteHeader(HttpHeaders.HEADER_CONTENT_LENGTH, String.valueOf(7));
            ByteBuffer bytes = ByteBuffer.wrap("content".getBytes());
            connectSession.write(connectSession.getBufferAllocator().wrap(bytes));
        }
    }

    @Test
    @Specification({"strong.unmatched.etag.delete.status.412/response"})
    public void shouldCausePreconditionFailedWithDeleteAndUnmatchedStrongETag() throws Exception {
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

        connector.connect("http://localhost:8000/index.html", handler, new ConnectSessionInitializerDeleteStrongUnmatched());
        assertTrue(closed.await(2, SECONDS));

        k3po.finish();
    }

    private static class ConnectSessionInitializerDeleteStrongUnmatched implements IoSessionInitializer<ConnectFuture> {
        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            HttpConnectSession connectSession = (HttpConnectSession) session;
            connectSession.setMethod(HttpMethod.DELETE);
            connectSession.addWriteHeader(HttpHeaders.HEADER_IF_MATCH, "unexpectedEtag");
            connectSession.addWriteHeader(HttpHeaders.HEADER_CONTENT_LENGTH, String.valueOf(7));
            ByteBuffer bytes = ByteBuffer.wrap("content".getBytes());
            connectSession.write(connectSession.getBufferAllocator().wrap(bytes));
        }
    }

    @Test
    @Specification({"strong.unmatched.etag.get.status.200/response"})
    public void shouldIgnoreIfMatchHeaderWithGetAndUnmatchedStrongETag() throws Exception {
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

        connector.connect("http://localhost:8000/index.html", handler, new ConnectSessionInitializerGetStrongUnmatched());
        assertTrue(closed.await(2, SECONDS));

        k3po.finish();
    }

    private static class ConnectSessionInitializerGetStrongUnmatched implements IoSessionInitializer<ConnectFuture> {
        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            HttpConnectSession connectSession = (HttpConnectSession) session;
            connectSession.setMethod(HttpMethod.GET);
            connectSession.addWriteHeader(HttpHeaders.HEADER_IF_MATCH, "unexpectedEtag");
        }
    }

    @Test
    @Specification({"strong.unmatched.etag.head.status.200/response"})
    public void shouldIgnoreIfMatchHeaderWithHeadAndUnmatchedStrongETag() throws Exception {
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

        connector.connect("http://localhost:8000/index.html", handler, new ConnectSessionInitializerHeadStrongUnmatched());
        assertTrue(closed.await(2, SECONDS));

        k3po.finish();
    }

    private static class ConnectSessionInitializerHeadStrongUnmatched implements IoSessionInitializer<ConnectFuture> {
        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            HttpConnectSession connectSession = (HttpConnectSession) session;
            connectSession.setMethod(HttpMethod.HEAD);
            connectSession.addWriteHeader(HttpHeaders.HEADER_IF_MATCH, "unexpectedEtag");
        }
    }

    @Test
    @Specification({"strong.unmatched.etag.post.status.412/response"})
    public void shouldCausePreconditionFailedWithPostAndUnmatchedStrongETag() throws Exception {
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

        connector.connect("http://localhost:8000/index.html", handler, new ConnectSessionInitializerPostStrongUnmatched());
        assertTrue(closed.await(2, SECONDS));

        k3po.finish();
    }

    private static class ConnectSessionInitializerPostStrongUnmatched implements IoSessionInitializer<ConnectFuture> {
        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            HttpConnectSession connectSession = (HttpConnectSession) session;
            connectSession.setMethod(HttpMethod.POST);
            connectSession.addWriteHeader(HttpHeaders.HEADER_IF_MATCH, "unexpectedEtag");
            connectSession.addWriteHeader(HttpHeaders.HEADER_CONTENT_LENGTH, String.valueOf(7));
            ByteBuffer bytes = ByteBuffer.wrap("content".getBytes());
            connectSession.write(connectSession.getBufferAllocator().wrap(bytes));
        }
    }

    @Test
    @Specification({"strong.unmatched.etag.put.status.412/response"})
    public void shouldCausePreconditionFailedWithPutAndUnmatchedStrongETag() throws Exception {
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

        connector.connect("http://localhost:8000/index.html", handler, new ConnectSessionInitializerPutStrongUnmatched());
        assertTrue(closed.await(2, SECONDS));

        k3po.finish();
    }

    private static class ConnectSessionInitializerPutStrongUnmatched implements IoSessionInitializer<ConnectFuture> {
        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            HttpConnectSession connectSession = (HttpConnectSession) session;
            connectSession.setMethod(HttpMethod.PUT);
            connectSession.addWriteHeader(HttpHeaders.HEADER_IF_MATCH, "unexpectedEtag");
            connectSession.addWriteHeader(HttpHeaders.HEADER_CONTENT_LENGTH, String.valueOf(7));
            ByteBuffer bytes = ByteBuffer.wrap("content".getBytes());
            connectSession.write(connectSession.getBufferAllocator().wrap(bytes));
        }
    }

    @Test
    @Specification({"weak.etag.delete.status.412/response"})
    public void shouldCausePreconditionFailedWithDeleteAndWeakETag() throws Exception {
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

        connector.connect("http://localhost:8000/index.html", handler, new ConnectSessionInitializerDeleteWeak());
        assertTrue(closed.await(2, SECONDS));

        k3po.finish();
    }

    private static class ConnectSessionInitializerDeleteWeak implements IoSessionInitializer<ConnectFuture> {
        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            HttpConnectSession connectSession = (HttpConnectSession) session;
            connectSession.setMethod(HttpMethod.DELETE);
            connectSession.addWriteHeader(HttpHeaders.HEADER_IF_MATCH, "W/\"weak-etag\"");
            connectSession.addWriteHeader(HttpHeaders.HEADER_CONTENT_LENGTH, String.valueOf(7));
            ByteBuffer bytes = ByteBuffer.wrap("content".getBytes());
            connectSession.write(connectSession.getBufferAllocator().wrap(bytes));
        }
    }

    @Test
    @Specification({"weak.etag.get.status.200/response"})
    public void shouldIgnoreIfMatchHeaderWithGetAndWeakETag() throws Exception {
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

        connector.connect("http://localhost:8000/index.html", handler, new ConnectSessionInitializerGetWeak());
        assertTrue(closed.await(2, SECONDS));

        k3po.finish();
    }

    private static class ConnectSessionInitializerGetWeak implements IoSessionInitializer<ConnectFuture> {
        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            HttpConnectSession connectSession = (HttpConnectSession) session;
            connectSession.setMethod(HttpMethod.GET);
            connectSession.addWriteHeader(HttpHeaders.HEADER_IF_MATCH, "W/\"weak-etag\"");
        }
    }

    @Test
    @Specification({"weak.etag.head.status.200/response"})
    public void shouldIgnoreIfMatchHeaderWithHeadAndWeakETag() throws Exception {
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

        connector.connect("http://localhost:8000/index.html", handler, new ConnectSessionInitializerHeadWeak());
        assertTrue(closed.await(2, SECONDS));

        k3po.finish();
    }

    private static class ConnectSessionInitializerHeadWeak implements IoSessionInitializer<ConnectFuture> {
        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            HttpConnectSession connectSession = (HttpConnectSession) session;
            connectSession.setMethod(HttpMethod.HEAD);
            connectSession.addWriteHeader(HttpHeaders.HEADER_IF_MATCH, "W/\"weak-etag\"");
        }
    }

    @Test
    @Specification({"weak.etag.post.status.412/response"})
    public void shouldCausePreconditionFailedWithPostAndWeakETag() throws Exception {
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

        connector.connect("http://localhost:8000/index.html", handler, new ConnectSessionInitializerPostWeak());
        assertTrue(closed.await(2, SECONDS));

        k3po.finish();
    }

    private static class ConnectSessionInitializerPostWeak implements IoSessionInitializer<ConnectFuture> {
        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            HttpConnectSession connectSession = (HttpConnectSession) session;
            connectSession.setMethod(HttpMethod.POST);
            connectSession.addWriteHeader(HttpHeaders.HEADER_IF_MATCH, "W/\"weak-etag\"");
            connectSession.addWriteHeader(HttpHeaders.HEADER_CONTENT_LENGTH, String.valueOf(7));
            ByteBuffer bytes = ByteBuffer.wrap("content".getBytes());
            connectSession.write(connectSession.getBufferAllocator().wrap(bytes));
        }
    }

    @Test
    @Specification({"weak.etag.put.status.412/response"})
    public void shouldCausePreconditionFailedWithPutAndWeakETag() throws Exception {
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

        connector.connect("http://localhost:8000/index.html", handler, new ConnectSessionInitializerPutWeak());
        assertTrue(closed.await(2, SECONDS));

        k3po.finish();
    }

    private static class ConnectSessionInitializerPutWeak implements IoSessionInitializer<ConnectFuture> {
        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            HttpConnectSession connectSession = (HttpConnectSession) session;
            connectSession.setMethod(HttpMethod.PUT);
            connectSession.addWriteHeader(HttpHeaders.HEADER_IF_MATCH, "W/\"weak-etag\"");
            connectSession.addWriteHeader(HttpHeaders.HEADER_CONTENT_LENGTH, String.valueOf(7));
            ByteBuffer bytes = ByteBuffer.wrap("content".getBytes());
            connectSession.write(connectSession.getBufferAllocator().wrap(bytes));
        }
    }

    @Test
    @Specification({"wildcard.etag.delete.status.200/response"})
    public void shouldSucceedWithDeleteAndWildcardForValidResource() throws Exception {
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

        connector.connect("http://localhost:8000/index.html/", handler, new ConnectSessionInitializerDeleteWildcard());
        assertTrue(closed.await(2, SECONDS));

        k3po.finish();
    }

    private static class ConnectSessionInitializerDeleteWildcard implements IoSessionInitializer<ConnectFuture> {
        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            HttpConnectSession connectSession = (HttpConnectSession) session;
            connectSession.setMethod(HttpMethod.DELETE);
            connectSession.addWriteHeader(HttpHeaders.HEADER_IF_MATCH, "\"*\"");
            connectSession.addWriteHeader(HttpHeaders.HEADER_CONTENT_LENGTH, String.valueOf(7));
            ByteBuffer bytes = ByteBuffer.wrap("content".getBytes());
            connectSession.write(connectSession.getBufferAllocator().wrap(bytes));
        }
    }

    @Test
    @Specification({"wildcard.etag.get.status.200/response"})
    public void shouldSucceedWithGetAndWildcardForValidResource() throws Exception {
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

        connector.connect("http://localhost:8000/index.html/", handler, new ConnectSessionInitializerGetWildcard());
        assertTrue(closed.await(2, SECONDS));

        k3po.finish();
    }

    private static class ConnectSessionInitializerGetWildcard implements IoSessionInitializer<ConnectFuture> {
        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            HttpConnectSession connectSession = (HttpConnectSession) session;
            connectSession.setMethod(HttpMethod.GET);
            connectSession.addWriteHeader(HttpHeaders.HEADER_IF_MATCH, "\"*\"");
        }
    }

    @Test
    @Specification({"wildcard.etag.head.status.200/response"})
    public void shouldSucceedWithHeadAndWildcardForValidResource() throws Exception {
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

        connector.connect("http://localhost:8000/index.html/", handler, new ConnectSessionInitializerHeadWildcard());
        assertTrue(closed.await(2, SECONDS));

        k3po.finish();
    }

    private static class ConnectSessionInitializerHeadWildcard implements IoSessionInitializer<ConnectFuture> {
        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            HttpConnectSession connectSession = (HttpConnectSession) session;
            connectSession.setMethod(HttpMethod.HEAD);
            connectSession.addWriteHeader(HttpHeaders.HEADER_IF_MATCH, "\"*\"");
        }
    }

    @Test
    @Specification({"wildcard.etag.post.status.200/response"})
    public void shouldSucceedWithPostAndWildcardForValidResource() throws Exception {
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

        connector.connect("http://localhost:8000/index.html/", handler, new ConnectSessionInitializerPostWildcard());
        assertTrue(closed.await(2, SECONDS));

        k3po.finish();
    }

    private static class ConnectSessionInitializerPostWildcard implements IoSessionInitializer<ConnectFuture> {
        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            HttpConnectSession connectSession = (HttpConnectSession) session;
            connectSession.setMethod(HttpMethod.POST);
            connectSession.addWriteHeader(HttpHeaders.HEADER_IF_MATCH, "\"*\"");
            connectSession.addWriteHeader(HttpHeaders.HEADER_CONTENT_LENGTH, String.valueOf(7));
            ByteBuffer bytes = ByteBuffer.wrap("content".getBytes());
            connectSession.write(connectSession.getBufferAllocator().wrap(bytes));
        }
    }

    @Test
    @Specification({"wildcard.etag.put.status.200/response"})
    public void shouldSucceedWithPutAndWildcardForValidResource() throws Exception {
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

        connector.connect("http://localhost:8000/index.html/", handler, new ConnectSessionInitializerPutWildcard());
        assertTrue(closed.await(2, SECONDS));

        k3po.finish();
    }

    private static class ConnectSessionInitializerPutWildcard implements IoSessionInitializer<ConnectFuture> {
        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            HttpConnectSession connectSession = (HttpConnectSession) session;
            connectSession.setMethod(HttpMethod.PUT);
            connectSession.addWriteHeader(HttpHeaders.HEADER_IF_MATCH, "\"*\"");
            connectSession.addWriteHeader(HttpHeaders.HEADER_CONTENT_LENGTH, String.valueOf(7));
            ByteBuffer bytes = ByteBuffer.wrap("content".getBytes());
            connectSession.write(connectSession.getBufferAllocator().wrap(bytes));
        }
    }

    private static class ConnectSessionInitializer implements IoSessionInitializer<ConnectFuture> {
        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            HttpConnectSession connectSession = (HttpConnectSession) session;
            connectSession.setMethod(HttpMethod.GET);
            connectSession.addWriteHeader(HttpHeaders.HEADER_HOST, "localhost:8000");
        }
    }

}
