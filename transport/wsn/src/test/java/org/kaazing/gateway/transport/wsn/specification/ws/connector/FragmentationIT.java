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
package org.kaazing.gateway.transport.wsn.specification.ws.connector;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertTrue;
import static org.kaazing.test.util.ITUtil.timeoutRule;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandler;
import org.jmock.api.Invocation;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.jmock.lib.action.CustomAction;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.kaazing.gateway.transport.test.Expectations;
import org.kaazing.gateway.transport.ws.bridge.filter.WsBuffer;
import org.kaazing.gateway.transport.wsn.WsnSession;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.test.util.ITUtil;
import org.kaazing.test.util.MethodExecutionTrace;

public class FragmentationIT {
    private final WsnConnectorRule connector = new WsnConnectorRule();
    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/ws/fragmentation");
    private final TestRule trace = new MethodExecutionTrace();
    private final TestRule timeoutRule = timeoutRule(10, SECONDS);

    private JUnitRuleMockery context = new JUnitRuleMockery() {
        {
            setThreadingPolicy(new Synchroniser());
        }
    };

    private TestRule contextRule = ITUtil.toTestRule(context);

    @Rule
    public TestRule chain = RuleChain.outerRule(trace).around(connector).around(k3po).around(timeoutRule)
            .around(contextRule);

    @Test
    @Specification({
        "server.echo.binary.payload.length.0.fragmented.with.injected.ping.pong/handshake.response.and.frames" })
    public void shouldEchoServerSendBinaryFrameWithEmptyPayloadFragmentedAndInjectedPingPong() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final List<ByteBuffer> bufferList = new ArrayList<>();

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                allowing(handler).messageReceived(with(any(IoSessionEx.class)), with(any(Object.class)));
                will(new CustomAction("Capture messageReceived() parameters") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        Object message = invocation.getParameter(1);
                        IoBufferEx ioBuffer = (IoBufferEx) message;
                        bufferList.add(ioBuffer.buf());

                        if (bufferList.size() == 5) {
                            WsnSession wsnConnectSession = (WsnSession) invocation.getParameter(0);
                            ByteBuffer buffer = ByteBuffer.allocate(4096);

                            for (ByteBuffer bb : bufferList) {
                                buffer.put(bb);
                            }
                            buffer.flip();

                            IoBufferAllocatorEx<? extends WsBuffer> allocator = wsnConnectSession.getBufferAllocator();
                            WsBuffer wsBuffer = allocator.wrap(buffer, IoBufferEx.FLAG_SHARED);
                            wsBuffer.setKind(WsBuffer.Kind.BINARY);
                            wsnConnectSession.write(wsBuffer);
                        }

                        return null;
                    }
                });
            }
        });

        final ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());
        k3po.finish();
    }

    @Test
    @Specification({
        "server.echo.binary.payload.length.125.fragmented/handshake.response.and.frames" })
    public void shouldEchoServerSendBinaryFrameWithPayloadFragmented() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final List<ByteBuffer> bufferList = new ArrayList<>();

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                allowing(handler).messageReceived(with(any(IoSessionEx.class)), with(any(Object.class)));
                will(new CustomAction("Capture messageReceived() parameters") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        Object message = invocation.getParameter(1);
                        IoBufferEx ioBuffer = (IoBufferEx) message;
                        bufferList.add(ioBuffer.buf());

                        if (bufferList.size() == 5) {
                            WsnSession wsnConnectSession = (WsnSession) invocation.getParameter(0);
                            ByteBuffer buffer = ByteBuffer.allocate(4096);

                            for (ByteBuffer bb : bufferList) {
                                buffer.put(bb);
                            }
                            buffer.flip();

                            IoBufferAllocatorEx<? extends WsBuffer> allocator = wsnConnectSession.getBufferAllocator();
                            WsBuffer wsBuffer = allocator.wrap(buffer, IoBufferEx.FLAG_SHARED);
                            wsBuffer.setKind(WsBuffer.Kind.BINARY);
                            wsnConnectSession.write(wsBuffer);
                        }

                        return null;
                    }
                });
            }
        });

        final ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());
        k3po.finish();
    }

    @Test
    @Specification({
        "server.echo.binary.payload.length.125.fragmented.with.injected.ping.pong/handshake.response.and.frames" })
    public void shouldEchoServerSendBinaryFrameWithPayloadFragmentedAndInjectedPingPong() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final List<ByteBuffer> bufferList = new ArrayList<>();

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                allowing(handler).messageReceived(with(any(IoSessionEx.class)), with(any(Object.class)));
                will(new CustomAction("Capture messageReceived() parameters") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        Object message = invocation.getParameter(1);
                        IoBufferEx ioBuffer = (IoBufferEx) message;
                        bufferList.add(ioBuffer.buf());

                        if (bufferList.size() == 5) {
                            WsnSession wsnConnectSession = (WsnSession) invocation.getParameter(0);
                            ByteBuffer buffer = ByteBuffer.allocate(4096);

                            for (ByteBuffer bb : bufferList) {
                                buffer.put(bb);
                            }
                            buffer.flip();

                            IoBufferAllocatorEx<? extends WsBuffer> allocator = wsnConnectSession.getBufferAllocator();
                            WsBuffer wsBuffer = allocator.wrap(buffer, IoBufferEx.FLAG_SHARED);
                            wsBuffer.setKind(WsBuffer.Kind.BINARY);
                            wsnConnectSession.write(wsBuffer);
                        }

                        return null;
                    }
                });
            }
        });

        final ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());
        k3po.finish();
    }

    @Test
    @Specification({
        "server.echo.binary.payload.length.125.fragmented.with.some.empty.fragments/handshake.response.and.frames" })
    public void shouldEchoServerSendBinaryFrameWithPayloadFragmentedWithSomeEmptyFragments() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final List<ByteBuffer> bufferList = new ArrayList<>();

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                allowing(handler).messageReceived(with(any(IoSessionEx.class)), with(any(Object.class)));
                will(new CustomAction("Capture messageReceived() parameters") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        Object message = invocation.getParameter(1);
                        IoBufferEx ioBuffer = (IoBufferEx) message;
                        bufferList.add(ioBuffer.buf());

                        if (bufferList.size() == 7) {
                            WsnSession wsnConnectSession = (WsnSession) invocation.getParameter(0);
                            ByteBuffer buffer = ByteBuffer.allocate(4096);

                            for (ByteBuffer bb : bufferList) {
                                buffer.put(bb);
                            }
                            buffer.flip();

                            IoBufferAllocatorEx<? extends WsBuffer> allocator = wsnConnectSession.getBufferAllocator();
                            WsBuffer wsBuffer = allocator.wrap(buffer, IoBufferEx.FLAG_SHARED);
                            wsBuffer.setKind(WsBuffer.Kind.BINARY);
                            wsnConnectSession.write(wsBuffer);
                        }

                        return null;
                    }
                });
            }
        });

        final ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());
        k3po.finish();
    }

    @Test
    @Specification({
        "server.echo.binary.payload.length.125.not.fragmented/handshake.response.and.frame" })
    public void shouldEchoServerSendBinaryFrameWithPayloadNotFragmented() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final List<ByteBuffer> bufferList = new ArrayList<>();

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                allowing(handler).messageReceived(with(any(IoSessionEx.class)), with(any(Object.class)));
                will(new CustomAction("Capture messageReceived() parameters") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        Object message = invocation.getParameter(1);
                        IoBufferEx ioBuffer = (IoBufferEx) message;
                        bufferList.add(ioBuffer.buf());

                        if (bufferList.size() == 1) {
                            WsnSession wsnConnectSession = (WsnSession) invocation.getParameter(0);
                            ByteBuffer buffer = ByteBuffer.allocate(4096);

                            for (ByteBuffer bb : bufferList) {
                                buffer.put(bb);
                            }
                            buffer.flip();

                            IoBufferAllocatorEx<? extends WsBuffer> allocator = wsnConnectSession.getBufferAllocator();
                            WsBuffer wsBuffer = allocator.wrap(buffer, IoBufferEx.FLAG_SHARED);
                            wsBuffer.setKind(WsBuffer.Kind.BINARY);
                            wsnConnectSession.write(wsBuffer);
                        }

                        return null;
                    }
                });
            }
        });

        final ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());
        k3po.finish();
    }

    @Test
    @Specification({
        "server.echo.text.payload.length.0.fragmented/handshake.response.and.frames"
        })
    public void shouldEchoServerSendTextFrameWithEmptyPayloadFragmented() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final List<ByteBuffer> bufferList = new ArrayList<>();

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                allowing(handler).messageReceived(with(any(IoSessionEx.class)), with(any(Object.class)));
                will(new CustomAction("Capture messageReceived() parameters") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        Object message = invocation.getParameter(1);
                        IoBufferEx ioBuffer = (IoBufferEx) message;
                        bufferList.add(ioBuffer.buf());

                        if (bufferList.size() == 5) {
                            WsnSession wsnConnectSession = (WsnSession) invocation.getParameter(0);
                            ByteBuffer buffer = ByteBuffer.allocate(4096);

                            for (ByteBuffer bb : bufferList) {
                                buffer.put(bb);
                            }
                            buffer.flip();

                            IoBufferAllocatorEx<? extends WsBuffer> allocator = wsnConnectSession.getBufferAllocator();
                            WsBuffer wsBuffer = allocator.wrap(buffer, IoBufferEx.FLAG_SHARED);
                            wsBuffer.setKind(WsBuffer.Kind.TEXT);
                            wsnConnectSession.write(wsBuffer);
                        }

                        return null;
                    }
                });
            }
        });

        final ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());
        k3po.finish();
    }

    @Test
    @Specification({
        "server.echo.text.payload.length.0.fragmented.with.injected.ping.pong/handshake.response.and.frames"
        })
    public void shouldEchoServerSendTextFrameWithEmptyPayloadFragmentedAndInjectedPingPong() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final List<ByteBuffer> bufferList = new ArrayList<>();

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                allowing(handler).messageReceived(with(any(IoSessionEx.class)), with(any(Object.class)));
                will(new CustomAction("Capture messageReceived() parameters") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        Object message = invocation.getParameter(1);
                        IoBufferEx ioBuffer = (IoBufferEx) message;
                        bufferList.add(ioBuffer.buf());

                        if (bufferList.size() == 5) {
                            WsnSession wsnConnectSession = (WsnSession) invocation.getParameter(0);
                            ByteBuffer buffer = ByteBuffer.allocate(4096);

                            for (ByteBuffer bb : bufferList) {
                                buffer.put(bb);
                            }
                            buffer.flip();

                            IoBufferAllocatorEx<? extends WsBuffer> allocator = wsnConnectSession.getBufferAllocator();
                            WsBuffer wsBuffer = allocator.wrap(buffer, IoBufferEx.FLAG_SHARED);
                            wsBuffer.setKind(WsBuffer.Kind.TEXT);
                            wsnConnectSession.write(wsBuffer);
                        }

                        return null;
                    }
                });
            }
        });

        final ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());
        k3po.finish();
    }

    @Test
    @Specification({
        "server.echo.text.payload.length.125.fragmented/handshake.response.and.frames" })
    public void shouldEchoServerSendTextFrameWithPayloadFragmented() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final List<ByteBuffer> bufferList = new ArrayList<>();

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                allowing(handler).messageReceived(with(any(IoSessionEx.class)), with(any(Object.class)));
                will(new CustomAction("Capture messageReceived() parameters") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        Object message = invocation.getParameter(1);
                        IoBufferEx ioBuffer = (IoBufferEx) message;
                        bufferList.add(ioBuffer.buf());

                        if (bufferList.size() == 5) {
                            WsnSession wsnConnectSession = (WsnSession) invocation.getParameter(0);
                            ByteBuffer buffer = ByteBuffer.allocate(4096);

                            for (ByteBuffer bb : bufferList) {
                                buffer.put(bb);
                            }
                            buffer.flip();

                            IoBufferAllocatorEx<? extends WsBuffer> allocator = wsnConnectSession.getBufferAllocator();
                            WsBuffer wsBuffer = allocator.wrap(buffer, IoBufferEx.FLAG_SHARED);
                            wsBuffer.setKind(WsBuffer.Kind.TEXT);
                            wsnConnectSession.write(wsBuffer);
                        }

                        return null;
                    }
                });
            }
        });

        final ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());
        k3po.finish();
    }

    @Test
    @Specification({
        "server.echo.text.payload.length.125.fragmented.but.not.utf8.aligned/handshake.response.and.frames" })
    public void shouldEchoServerSendTextFrameWithPayloadFragmentedEvenWhenNotUTF8Aligned() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final List<ByteBuffer> bufferList = new ArrayList<>();

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                allowing(handler).messageReceived(with(any(IoSessionEx.class)), with(any(Object.class)));
                will(new CustomAction("Capture messageReceived() parameters") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        Object message = invocation.getParameter(1);
                        IoBufferEx ioBuffer = (IoBufferEx) message;
                        bufferList.add(ioBuffer.buf());

                        if (bufferList.size() == 2) {
                            WsnSession wsnConnectSession = (WsnSession) invocation.getParameter(0);
                            ByteBuffer buffer = ByteBuffer.allocate(4096);

                            for (ByteBuffer bb : bufferList) {
                                buffer.put(bb);
                            }
                            buffer.flip();

                            IoBufferAllocatorEx<? extends WsBuffer> allocator = wsnConnectSession.getBufferAllocator();
                            WsBuffer wsBuffer = allocator.wrap(buffer, IoBufferEx.FLAG_SHARED);
                            wsBuffer.setKind(WsBuffer.Kind.TEXT);
                            wsnConnectSession.write(wsBuffer);
                        }

                        return null;
                    }
                });
            }
        });

        final ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());
        k3po.finish();
    }

    @Test
    @Specification({
        "server.echo.text.payload.length.125.fragmented.with.injected.ping.pong/handshake.response.and.frames" })
    public void shouldEchoServerSendTextFrameWithPayloadFragmentedAndInjectedPingPong() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final List<ByteBuffer> bufferList = new ArrayList<>();

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                allowing(handler).messageReceived(with(any(IoSessionEx.class)), with(any(Object.class)));
                will(new CustomAction("Capture messageReceived() parameters") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        Object message = invocation.getParameter(1);
                        IoBufferEx ioBuffer = (IoBufferEx) message;
                        bufferList.add(ioBuffer.buf());

                        if (bufferList.size() == 5) {
                            WsnSession wsnConnectSession = (WsnSession) invocation.getParameter(0);
                            ByteBuffer buffer = ByteBuffer.allocate(4096);

                            for (ByteBuffer bb : bufferList) {
                                buffer.put(bb);
                            }
                            buffer.flip();

                            IoBufferAllocatorEx<? extends WsBuffer> allocator = wsnConnectSession.getBufferAllocator();
                            WsBuffer wsBuffer = allocator.wrap(buffer, IoBufferEx.FLAG_SHARED);
                            wsBuffer.setKind(WsBuffer.Kind.TEXT);
                            wsnConnectSession.write(wsBuffer);
                        }

                        return null;
                    }
                });
            }
        });

        final ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());
        k3po.finish();
    }

    @Test
    @Specification({
        "server.echo.text.payload.length.125.fragmented.with.some.empty.fragments/handshake.response.and.frames" })
    public void shouldEchoServerSendTextFrameWithPayloadFragmentedWithSomeEmptyFragments() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final List<ByteBuffer> bufferList = new ArrayList<>();

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                allowing(handler).messageReceived(with(any(IoSessionEx.class)), with(any(Object.class)));
                will(new CustomAction("Capture messageReceived() parameters") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        Object message = invocation.getParameter(1);
                        IoBufferEx ioBuffer = (IoBufferEx) message;
                        bufferList.add(ioBuffer.buf());

                        if (bufferList.size() == 7) {
                            WsnSession wsnConnectSession = (WsnSession) invocation.getParameter(0);
                            ByteBuffer buffer = ByteBuffer.allocate(4096);

                            for (ByteBuffer bb : bufferList) {
                                buffer.put(bb);
                            }
                            buffer.flip();

                            IoBufferAllocatorEx<? extends WsBuffer> allocator = wsnConnectSession.getBufferAllocator();
                            WsBuffer wsBuffer = allocator.wrap(buffer, IoBufferEx.FLAG_SHARED);
                            wsBuffer.setKind(WsBuffer.Kind.TEXT);
                            wsnConnectSession.write(wsBuffer);
                        }

                        return null;
                    }
                });
            }
        });

        final ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());
        k3po.finish();
    }

    @Test
    @Specification({
        "server.echo.text.payload.length.125.not.fragmented/handshake.response.and.frame" })
    public void shouldEchoServerSendTextFrameWithPayloadNotFragmented() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final List<ByteBuffer> bufferList = new ArrayList<>();

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).messageReceived(with(any(IoSessionEx.class)), with(any(Object.class)));
                will(new CustomAction("Capture messageReceived() parameters") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        Object message = invocation.getParameter(1);
                        IoBufferEx ioBuffer = (IoBufferEx) message;
                        bufferList.add(ioBuffer.buf());

                        if (bufferList.size() == 1) {
                            WsnSession wsnConnectSession = (WsnSession) invocation.getParameter(0);
                            ByteBuffer buffer = ByteBuffer.allocate(4096);

                            for (ByteBuffer bb : bufferList) {
                                buffer.put(bb);
                            }
                            buffer.flip();

                            IoBufferAllocatorEx<? extends WsBuffer> allocator = wsnConnectSession.getBufferAllocator();
                            WsBuffer wsBuffer = allocator.wrap(buffer, IoBufferEx.FLAG_SHARED);
                            wsBuffer.setKind(WsBuffer.Kind.TEXT);
                            wsnConnectSession.write(wsBuffer);
                        }

                        return null;
                    }
                });
            }
        });

        final ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());
        k3po.finish();
    }

    @Test
    @Specification({
        "server.send.binary.payload.length.125.fragmented.but.not.continued/handshake.response.and.frames" })
    public void shouldFailWebSocketConnectionWhenServerSendBinaryFrameWithPayloadFragmentedButNotContinued() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).messageReceived(with(any(IoSessionEx.class)), with(any(Object.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Exception.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(countDown(latch));
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
    }

    @Test
    @Specification({
        "server.send.close.payload.length.2.fragmented/handshake.response.and.frames" })
    public void shouldFailWebSocketConnectionWhenServerSendCloseFrameWithPayloadFragmented() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Exception.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(countDown(latch));
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
    }

    @Test
    @Specification({
        "server.send.continuation.payload.length.125.fragmented/handshake.response.and.frames" })
    public void shouldFailWebSocketConnectionWhenServerSendContinuationFrameWithPayloadFragmented() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Exception.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(countDown(latch));
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
    }

    @Test
    @Specification({
        "server.send.continuation.payload.length.125.not.fragmented/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendContinuationFrameWithPayloadNotFragmented() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Exception.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(countDown(latch));
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
    }

    @Test
    @Specification({
        "server.send.ping.payload.length.0.fragmented/handshake.response.and.frames" })
    public void shouldFailWebSocketConnectionWhenServerSendPingFrameWithPayloadFragmented() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Exception.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(countDown(latch));
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
    }

    @Test
    @Specification({
        "server.send.pong.payload.length.0.fragmented/handshake.response.and.frames" })
    public void shouldFailWebSocketConnectionWhenServerSendPongFrameWithPayloadFragmented() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Exception.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(countDown(latch));
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
    }
}
