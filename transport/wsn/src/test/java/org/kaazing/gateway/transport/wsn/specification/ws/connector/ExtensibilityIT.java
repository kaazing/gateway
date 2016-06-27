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
package org.kaazing.gateway.transport.wsn.specification.ws.connector;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandler;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.test.util.MethodExecutionTrace;

public class ExtensibilityIT {
    private final WsnConnectorRule connector = new WsnConnectorRule();
    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/ws/extensibility");
    private final TestRule timeoutRule = new DisableOnDebug(Timeout.builder().withTimeout(10, SECONDS)
                .withLookingForStuckThread(true).build());
    private final TestRule trace = new MethodExecutionTrace();

    @Rule
    public TestRule chain = RuleChain.outerRule(trace).around(timeoutRule).around(connector).around(k3po);

    private Mockery context;

    @Before
    public void initialize() {
        context = new Mockery();
        context.setThreadingPolicy(new Synchroniser());
    }

    @Test
    @Specification({
            "server.send.text.frame.with.rsv.1/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendTextFrameWithRsv1() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        latch.countDown();
                        return null;
                    }
                });
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
        context.assertIsSatisfied();
    }

    @Test
    @Specification({
            "server.send.binary.frame.with.rsv.1/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendBinaryFrameWithRsv1() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        latch.countDown();
                        return null;
                    }
                });

            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
        context.assertIsSatisfied();
    }

    @Test
    @Specification({
            "server.send.close.frame.with.rsv.1/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendCloseFrameWithRsv1() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        latch.countDown();
                        return null;
                    }
                });
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
        context.assertIsSatisfied();
    }

    @Test
    @Specification({
            "server.send.ping.frame.with.rsv.1/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendPingFrameWithRsv1() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        latch.countDown();
                        return null;
                    }
                });
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
        context.assertIsSatisfied();
    }

    @Test
    @Specification({
            "server.send.pong.frame.with.rsv.1/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendPongFrameWithRsv1() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        latch.countDown();
                        return null;
                    }
                });
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
        context.assertIsSatisfied();
    }

    @Test
    @Specification({
            "server.send.text.frame.with.rsv.2/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendTextFrameWithRsv2() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        latch.countDown();
                        return null;
                    }
                });
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
        context.assertIsSatisfied();
    }

    @Test
    @Specification({
            "server.send.binary.frame.with.rsv.2/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendBinaryFrameWithRsv2() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        latch.countDown();
                        return null;
                    }
                });
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
        context.assertIsSatisfied();
    }

    @Test
    @Specification({
            "server.send.close.frame.with.rsv.2/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendCloseFrameWithRsv2() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        latch.countDown();
                        return null;
                    }
                });
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
        context.assertIsSatisfied();
    }

    @Test
    @Specification({
            "server.send.ping.frame.with.rsv.2/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendPingFrameWithRsv2() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        latch.countDown();
                        return null;
                    }
                });
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
        context.assertIsSatisfied();
    }

    @Test
    @Specification({
            "server.send.pong.frame.with.rsv.2/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendPongFrameWithRsv2() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        latch.countDown();
                        return null;
                    }
                });
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
        context.assertIsSatisfied();
    }

    @Test
    @Specification({
            "server.send.text.frame.with.rsv.3/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendTextFrameWithRsv3() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        latch.countDown();
                        return null;
                    }
                });
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
        context.assertIsSatisfied();
    }

    @Test
    @Specification({
            "server.send.binary.frame.with.rsv.3/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendBinaryFrameWithRsv3() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        latch.countDown();
                        return null;
                    }
                });
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
        context.assertIsSatisfied();
    }

    @Test
    @Specification({
            "server.send.close.frame.with.rsv.3/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendCloseFrameWithRsv3() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        latch.countDown();
                        return null;
                    }
                });
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
        context.assertIsSatisfied();
    }

    @Test
    @Specification({
            "server.send.ping.frame.with.rsv.3/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendPingFrameWithRsv3() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        latch.countDown();
                        return null;
                    }
                });
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
        context.assertIsSatisfied();
    }

    @Test
    @Specification({
            "server.send.pong.frame.with.rsv.3/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendPongFrameWithRsv3() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        latch.countDown();
                        return null;
                    }
                });
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
        context.assertIsSatisfied();
    }

    @Test
    @Specification({
            "server.send.text.frame.with.rsv.4/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendTextFrameWithRsv4() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        latch.countDown();
                        return null;
                    }
                });
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
        context.assertIsSatisfied();
    }

    @Test
    @Specification({
            "server.send.binary.frame.with.rsv.4/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendBinaryFrameWithRsv4() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        latch.countDown();
                        return null;
                    }
                });
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
        context.assertIsSatisfied();
    }

    @Test
    @Specification({
            "server.send.close.frame.with.rsv.4/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendCloseFrameWithRsv4() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        latch.countDown();
                        return null;
                    }
                });
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
        context.assertIsSatisfied();
    }

    @Test
    @Specification({
            "server.send.ping.frame.with.rsv.4/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendPingFrameWithRsv4() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        latch.countDown();
                        return null;
                    }
                });
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
        context.assertIsSatisfied();
    }

    @Test
    @Specification({
            "server.send.pong.frame.with.rsv.4/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendPongFrameWithRsv4() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        latch.countDown();
                        return null;
                    }
                });
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
        context.assertIsSatisfied();
    }

    @Test
    @Specification({
            "server.send.text.frame.with.rsv.5/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendTextFrameWithRsv5() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        latch.countDown();
                        return null;
                    }
                });
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
        context.assertIsSatisfied();
    }

    @Test
    @Specification({
            "server.send.binary.frame.with.rsv.5/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendBinaryFrameWithRsv5() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        latch.countDown();
                        return null;
                    }
                });
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
        context.assertIsSatisfied();
    }

    @Test
    @Specification({
            "server.send.close.frame.with.rsv.5/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendCloseFrameWithRsv5() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        latch.countDown();
                        return null;
                    }
                });
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
        context.assertIsSatisfied();
    }

    @Test
    @Specification({
            "server.send.ping.frame.with.rsv.5/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendPingFrameWithRsv5() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        latch.countDown();
                        return null;
                    }
                });
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
        context.assertIsSatisfied();
    }

    @Test
    @Specification({
            "server.send.pong.frame.with.rsv.5/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendPongFrameWithRsv5() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        latch.countDown();
                        return null;
                    }
                });
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
        context.assertIsSatisfied();
    }

    @Test
    @Specification({
            "server.send.text.frame.with.rsv.6/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendTextFrameWithRsv6() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        latch.countDown();
                        return null;
                    }
                });
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
        context.assertIsSatisfied();
    }

    @Test
    @Specification({
            "server.send.binary.frame.with.rsv.6/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendBinaryFrameWithRsv6() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        latch.countDown();
                        return null;
                    }
                });
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
        context.assertIsSatisfied();
    }

    @Test
    @Specification({
            "server.send.close.frame.with.rsv.6/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendCloseFrameWithRsv6() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        latch.countDown();
                        return null;
                    }
                });
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
        context.assertIsSatisfied();
    }

    @Test
    @Specification({
            "server.send.ping.frame.with.rsv.6/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendPingFrameWithRsv6() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        latch.countDown();
                        return null;
                    }
                });
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
        context.assertIsSatisfied();
    }

    @Test
    @Specification({
            "server.send.pong.frame.with.rsv.6/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendPongFrameWithRsv6() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        latch.countDown();
                        return null;
                    }
                });
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
        context.assertIsSatisfied();
    }

    @Test
    @Specification({
            "server.send.text.frame.with.rsv.7/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendTextFrameWithRsv7() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        latch.countDown();
                        return null;
                    }
                });
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
        context.assertIsSatisfied();
    }

    @Test
    @Specification({
            "server.send.binary.frame.with.rsv.7/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendBinaryFrameWithRsv7() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        latch.countDown();
                        return null;
                    }
                });
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
        context.assertIsSatisfied();
    }

    @Test
    @Specification({
            "server.send.close.frame.with.rsv.7/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendCloseFrameWithRsv7() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        latch.countDown();
                        return null;
                    }
                });
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
        context.assertIsSatisfied();
    }

    @Test
    @Specification({
            "server.send.ping.frame.with.rsv.7/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendPingFrameWithRsv7() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        latch.countDown();
                        return null;
                    }
                });
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
        context.assertIsSatisfied();
    }

    @Test
    @Specification({
            "server.send.pong.frame.with.rsv.7/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendPongFrameWithRsv7() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        latch.countDown();
                        return null;
                    }
                });
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
        context.assertIsSatisfied();
    }
}
