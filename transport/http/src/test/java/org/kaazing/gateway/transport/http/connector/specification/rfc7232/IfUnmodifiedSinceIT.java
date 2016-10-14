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

public class IfUnmodifiedSinceIT {

    private final HttpConnectorRule connector = new HttpConnectorRule();

    private JUnitRuleMockery context = new JUnitRuleMockery() {
        {
            setThreadingPolicy(new Synchroniser());
        }
    };

    private final TestRule trace = new MethodExecutionTrace();
    private TestRule contextRule = ITUtil.toTestRule(context);
    private final K3poRule k3po =
            new K3poRule().setScriptRoot("org/kaazing/specification/http/rfc7232/preconditions/if.unmodified.since");
    private final TestRule timeoutRule = new DisableOnDebug(new Timeout(5, SECONDS));

    @Rule
    public TestRule chain = RuleChain.outerRule(trace).around(connector).around(contextRule).around(k3po).around(timeoutRule);

    @Test
    @Specification({"condition.failed.delete.status.412/response"})
    public void shouldResultInPreconditionFailedResponseWithDeleteAndConditionFailed() throws Exception {
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
            connectSession.addWriteHeader(HttpHeaders.HEADER_IF_UNMODIFIED_SINCE, "Thu, 1 Jan 2015 01:23:45 GMT");
            connectSession.addWriteHeader(HttpHeaders.HEADER_CONTENT_LENGTH, String.valueOf(7));
            ByteBuffer bytes = ByteBuffer.wrap("content".getBytes());
            connectSession.write(connectSession.getBufferAllocator().wrap(bytes));
        }
    }

    @Test
    @Specification({"condition.failed.post.status.412/response"})
    public void shouldResultInPreconditionFailedResponseWithPostAndConditionFailed() throws Exception {
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

        connector.connect("http://localhost:8000/index.html/", handler, new ConnectSessionInitializer());
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
        connector.connect("http://localhost:8000/index.html/", handler, new ConnectSessionInitializerPost());
        assertTrue(closed2.await(2, SECONDS));
        k3po.finish();
    }

    private static class ConnectSessionInitializerPost implements IoSessionInitializer<ConnectFuture> {
        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            HttpConnectSession connectSession = (HttpConnectSession) session;
            connectSession.setMethod(HttpMethod.POST);
            connectSession.addWriteHeader(HttpHeaders.HEADER_HOST, "localhost:8000");
            connectSession.addWriteHeader(HttpHeaders.HEADER_IF_UNMODIFIED_SINCE, "Thu, 1 Jan 2015 01:23:45 GMT");
            connectSession.addWriteHeader(HttpHeaders.HEADER_CONTENT_LENGTH, String.valueOf(7));
            ByteBuffer bytes = ByteBuffer.wrap("content".getBytes());
            connectSession.write(connectSession.getBufferAllocator().wrap(bytes));
        }
    }

    @Test
    @Specification({"condition.failed.put.status.412/response"})
    public void shouldResultInPreconditionFailedResponseWithPutAndConditionFailed() throws Exception {
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

        connector.connect("http://localhost:8000/index.html/", handler, new ConnectSessionInitializer());
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
        connector.connect("http://localhost:8000/index.html/", handler, new ConnectSessionInitializerPut());
        assertTrue(closed2.await(2, SECONDS));
        k3po.finish();
    }

    private static class ConnectSessionInitializerPut implements IoSessionInitializer<ConnectFuture> {
        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            HttpConnectSession connectSession = (HttpConnectSession) session;
            connectSession.setMethod(HttpMethod.PUT);
            connectSession.addWriteHeader(HttpHeaders.HEADER_HOST, "localhost:8000");
            connectSession.addWriteHeader(HttpHeaders.HEADER_IF_UNMODIFIED_SINCE, "Thu, 1 Jan 2015 01:23:45 GMT");
            connectSession.addWriteHeader(HttpHeaders.HEADER_CONTENT_LENGTH, String.valueOf(7));
            ByteBuffer bytes = ByteBuffer.wrap("content".getBytes());
            connectSession.write(connectSession.getBufferAllocator().wrap(bytes));
        }
    }

    @Test
    @Specification({"condition.passed.delete.status.200/response"})
    public void shouldResultInSuccessfulResponseWithDeleteAndConditionPassed() throws Exception {
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

        connector.connect("http://localhost:8000/index.html/", handler, new ConnectSessionInitializer());
        assertTrue(closed.await(2, SECONDS));
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
        connector.connect("http://localhost:8000/index.html/", handler, new ConnectSessionInitializerDelete());
        assertTrue(closed2.await(2, SECONDS));
        k3po.finish();
    }

    @Test
    @Specification({"condition.passed.post.status.200/response"})
    public void shouldResultInSuccessfulResponseWithPostAndConditionPassed() throws Exception {
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

        connector.connect("http://localhost:8000/index.html/", handler, new ConnectSessionInitializer());
        assertTrue(closed.await(2, SECONDS));
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
        connector.connect("http://localhost:8000/index.html/", handler, new ConnectSessionInitializerPost());
        assertTrue(closed2.await(2, SECONDS));
        k3po.finish();
    }

    @Test
    @Specification({"condition.passed.put.status.200/response"})
    public void shouldResultInSuccessfulResponseWithPutAndConditionPassed() throws Exception {
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

        connector.connect("http://localhost:8000/index.html/", handler, new ConnectSessionInitializer());
        assertTrue(closed.await(2, SECONDS));
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
        connector.connect("http://localhost:8000/index.html/", handler, new ConnectSessionInitializerPut());
        assertTrue(closed2.await(2, SECONDS));
        k3po.finish();
    }

    @Test
    @Specification({"ignored.with.get/response"})
    public void shouldIgnoreIfUnmodifiedSinceHeaderWithGet() throws Exception {
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

        connector.connect("http://localhost:8000/index.html/", handler, new ConnectSessionInitializer());
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
        connector.connect("http://localhost:8000/index.html/", handler, new ConnectSessionInitializerGet());
        assertTrue(closed2.await(2, SECONDS));
        k3po.finish();
    }

    private static class ConnectSessionInitializerGet implements IoSessionInitializer<ConnectFuture> {
        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            HttpConnectSession connectSession = (HttpConnectSession) session;
            connectSession.setMethod(HttpMethod.GET);
            connectSession.addWriteHeader(HttpHeaders.HEADER_HOST, "localhost:8000");
            connectSession.addWriteHeader(HttpHeaders.HEADER_IF_UNMODIFIED_SINCE, "Thu, 1 Jan 2015 01:23:45 GMT");
        }
    }

    private static class ConnectSessionInitializerHead implements IoSessionInitializer<ConnectFuture> {
        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            HttpConnectSession connectSession = (HttpConnectSession) session;
            connectSession.setMethod(HttpMethod.HEAD);
            connectSession.addWriteHeader(HttpHeaders.HEADER_HOST, "localhost:8000");
            connectSession.addWriteHeader(HttpHeaders.HEADER_IF_UNMODIFIED_SINCE, "Thu, 1 Jan 2015 01:23:45 GMT");
        }
    }

    @Test
    @Specification({"ignored.with.head/response"})
    public void shouldIgnoreIfUnmodifiedSinceHeaderWithHead() throws Exception {
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

        connector.connect("http://localhost:8000/index.html/", handler, new ConnectSessionInitializer());
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
        connector.connect("http://localhost:8000/index.html/", handler, new ConnectSessionInitializerHead());
        assertTrue(closed2.await(2, SECONDS));
        k3po.finish();
    }

    @Test
    @Specification({"ignored.with.delete.and.invalid.http.date/response"})
    public void shouldIgnoreIfUnmodifiedSinceHeaderWithInvalidDateInDelete() throws Exception {
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

        connector.connect("http://localhost:8000/index.html/", handler, new ConnectSessionInitializer());
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
        connector.connect("http://localhost:8000/index.html/", handler, new ConnectSessionInitializerDelete());
        assertTrue(closed2.await(2, SECONDS));
        k3po.finish();
    }

    @Test
    @Specification({"ignored.with.post.and.invalid.http.date/response"})
    public void shouldIgnoreIfUnmodifiedSinceHeaderWithInvalidDateInPost() throws Exception {
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

        connector.connect("http://localhost:8000/index.html/", handler, new ConnectSessionInitializer());
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
        connector.connect("http://localhost:8000/index.html/", handler, new ConnectSessionInitializerDelete());
        assertTrue(closed2.await(2, SECONDS));
        k3po.finish();
    }

    @Test
    @Specification({"ignored.with.put.and.invalid.http.date/response"})
    public void shouldIgnoreIfUnmodifiedSinceHeaderWithInvalidDateInPut() throws Exception {
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

        connector.connect("http://localhost:8000/index.html/", handler, new ConnectSessionInitializer());
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
        connector.connect("http://localhost:8000/index.html/", handler, new ConnectSessionInitializerPut());
        assertTrue(closed2.await(2, SECONDS));
        k3po.finish();
    }

    @Test
    @Specification({"ignored.with.delete.and.if.match/response"})
    public void shouldIgnoreIfUnmodifiedSinceHeaderAsDeleteAlsoContainsIfMatchHeader() throws Exception {
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

        connector.connect("http://localhost:8000/index.html/", handler, new ConnectSessionInitializer());
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
        connector.connect("http://localhost:8000/index.html/", handler, new ConnectSessionInitializerDeleteMatch());
        assertTrue(closed2.await(2, SECONDS));
        k3po.finish();
    }

    private static class ConnectSessionInitializerDeleteMatch implements IoSessionInitializer<ConnectFuture> {
        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            HttpConnectSession connectSession = (HttpConnectSession) session;
            connectSession.setMethod(HttpMethod.DELETE);
            connectSession.addWriteHeader(HttpHeaders.HEADER_HOST, "localhost:8000");
            connectSession.addWriteHeader(HttpHeaders.HEADER_IF_UNMODIFIED_SINCE, "Thu, 1 Jan 2015 01:23:45 GMT");
            connectSession.addWriteHeader(HttpHeaders.HEADER_IF_MATCH, "r2d2xxxx");
            connectSession.addWriteHeader(HttpHeaders.HEADER_CONTENT_LENGTH, String.valueOf(7));
            ByteBuffer bytes = ByteBuffer.wrap("content".getBytes());
            connectSession.write(connectSession.getBufferAllocator().wrap(bytes));
        }
    }

    @Test
    @Specification({"ignored.with.post.and.if.match/response"})
    public void shouldIgnoreIfUnmodifiedSinceHeaderAsPostAlsoContainsIfMatchHeader() throws Exception {
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

        connector.connect("http://localhost:8000/index.html/", handler, new ConnectSessionInitializer());
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
        connector.connect("http://localhost:8000/index.html/", handler, new ConnectSessionInitializerPostMatch());
        assertTrue(closed2.await(2, SECONDS));
        k3po.finish();
    }

    private static class ConnectSessionInitializerPostMatch implements IoSessionInitializer<ConnectFuture> {
        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            HttpConnectSession connectSession = (HttpConnectSession) session;
            connectSession.setMethod(HttpMethod.POST);
            connectSession.addWriteHeader(HttpHeaders.HEADER_HOST, "localhost:8000");
            connectSession.addWriteHeader(HttpHeaders.HEADER_IF_UNMODIFIED_SINCE, "Thu, 1 Jan 2015 01:23:45 GMT");
            connectSession.addWriteHeader(HttpHeaders.HEADER_IF_MATCH, "r2d2xxxx");
            connectSession.addWriteHeader(HttpHeaders.HEADER_CONTENT_LENGTH, String.valueOf(7));
            ByteBuffer bytes = ByteBuffer.wrap("content".getBytes());
            connectSession.write(connectSession.getBufferAllocator().wrap(bytes));
        }
    }

    @Test
    @Specification({"ignored.with.put.and.if.match/response"})
    public void shouldIgnoreIfUnmodifiedSinceHeaderAsPutAlsoContainsIfMatchHeader() throws Exception {
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

        connector.connect("http://localhost:8000/index.html/", handler, new ConnectSessionInitializer());
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
        connector.connect("http://localhost:8000/index.html/", handler, new ConnectSessionInitializerPutMatch());
        assertTrue(closed2.await(2, SECONDS));
        k3po.finish();
    }

    private static class ConnectSessionInitializerPutMatch implements IoSessionInitializer<ConnectFuture> {
        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            HttpConnectSession connectSession = (HttpConnectSession) session;
            connectSession.setMethod(HttpMethod.PUT);
            connectSession.addWriteHeader(HttpHeaders.HEADER_HOST, "localhost:8000");
            connectSession.addWriteHeader(HttpHeaders.HEADER_IF_UNMODIFIED_SINCE, "Thu, 1 Jan 2015 01:23:45 GMT");
            connectSession.addWriteHeader(HttpHeaders.HEADER_IF_MATCH, "r2d2xxxx");
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
