package org.kaazing.gateway.transport.wsn.specification.ws.connector;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandler;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.gateway.transport.ws.bridge.filter.WsBuffer;
import org.kaazing.gateway.transport.wsn.WsnProtocol;
import org.kaazing.gateway.transport.wsn.WsnSession;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.test.util.MethodExecutionTrace;

public class FragmentationIT {
    private final WsnConnectorRule connector = new WsnConnectorRule();
    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/ws/fragmentation");
    private final TestRule timeoutRule = new DisableOnDebug(Timeout.builder().withTimeout(10, SECONDS)
                .withLookingForStuckThread(true).build());
    private final TestRule trace = new MethodExecutionTrace();

    @Rule
    public TestRule chain = RuleChain.outerRule(trace).around(timeoutRule).around(connector).around(k3po);

    private static String TEXT_FILTER_NAME = WsnProtocol.NAME + "#text";

    private Mockery context;

    @Before
    public void initialize() {
        context = new Mockery();
        context.setThreadingPolicy(new Synchroniser());
    }

    @Test
    @Ignore("Issue# 306: IllegalArgumentException: message is empty. Forgot to call flip")
    @Specification({
        "server.echo.binary.payload.length.0.fragmented.with.injected.ping.pong/handshake.response.and.frames" })
    public void shouldEchoServerSendBinaryFrameWithEmptyPayloadFragmentedAndInjectedPingPong() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final List<ByteBuffer> bufferList = new ArrayList<ByteBuffer>();
        final CountDownLatch latch = new CountDownLatch(1);

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

                            // ### Issue# 316: Temporary hack till the issue related to Connector writing out TEXT frame
                            //                  instead of BINARY is resolved.
                            IoFilterChain parentFilterChain = wsnConnectSession.getParent().getFilterChain();
                            if (parentFilterChain.contains(TEXT_FILTER_NAME)) {
                                parentFilterChain.remove(TEXT_FILTER_NAME);
                            }

                            IoBufferAllocatorEx<? extends WsBuffer> allocator = wsnConnectSession.getBufferAllocator();
                            WsBuffer wsBuffer = allocator.wrap(buffer, IoBufferEx.FLAG_SHARED);
                            wsBuffer.setKind(WsBuffer.Kind.BINARY);
                            wsnConnectSession.write(wsBuffer);
                        }

                        return null;
                    }
                });
                // Once the issue is fixed, we can relax this expectation.
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
                will(new CustomAction("Capture exception") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        Throwable throwable = (Throwable) invocation.getParameter(1);
                        throwable.printStackTrace();
                        return null;
                    }
                });

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

        final ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());
        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
        context.assertIsSatisfied();
    }

    @Test
    @Specification({
        "server.echo.binary.payload.length.125.fragmented/handshake.response.and.frames" })
    public void shouldEchoServerSendBinaryFrameWithPayloadFragmented() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final List<ByteBuffer> bufferList = new ArrayList<ByteBuffer>();
        final CountDownLatch latch = new CountDownLatch(1);

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

                            // ### Issue# 316: Temporary hack till the issue related to Connector writing out TEXT frame
                            //                  instead of BINARY is resolved.
                            IoFilterChain parentFilterChain = wsnConnectSession.getParent().getFilterChain();
                            if (parentFilterChain.contains(TEXT_FILTER_NAME)) {
                                parentFilterChain.remove(TEXT_FILTER_NAME);
                            }

                            IoBufferAllocatorEx<? extends WsBuffer> allocator = wsnConnectSession.getBufferAllocator();
                            WsBuffer wsBuffer = allocator.wrap(buffer, IoBufferEx.FLAG_SHARED);
                            wsBuffer.setKind(WsBuffer.Kind.BINARY);
                            wsnConnectSession.write(wsBuffer);
                        }

                        return null;
                    }
                });
                allowing(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
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

        final ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());
        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
        context.assertIsSatisfied();
    }

    @Test
    @Specification({
        "server.echo.binary.payload.length.125.fragmented.with.injected.ping.pong/handshake.response.and.frames" })
    public void shouldEchoServerSendBinaryFrameWithPayloadFragmentedAndInjectedPingPong() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final List<ByteBuffer> bufferList = new ArrayList<ByteBuffer>();
        final CountDownLatch latch = new CountDownLatch(1);

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

                            // ### Issue# 316: Temporary hack till the issue related to Connector writing out TEXT frame
                            //                  instead of BINARY is resolved.
                            IoFilterChain parentFilterChain = wsnConnectSession.getParent().getFilterChain();
                            if (parentFilterChain.contains(TEXT_FILTER_NAME)) {
                                parentFilterChain.remove(TEXT_FILTER_NAME);
                            }

                            IoBufferAllocatorEx<? extends WsBuffer> allocator = wsnConnectSession.getBufferAllocator();
                            WsBuffer wsBuffer = allocator.wrap(buffer, IoBufferEx.FLAG_SHARED);
                            wsBuffer.setKind(WsBuffer.Kind.BINARY);
                            wsnConnectSession.write(wsBuffer);
                        }

                        return null;
                    }
                });
                allowing(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
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

        final ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());
        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
        context.assertIsSatisfied();
    }

    @Test
    @Specification({
        "server.echo.binary.payload.length.125.fragmented.with.some.empty.fragments/handshake.response.and.frames" })
    public void shouldEchoServerSendBinaryFrameWithPayloadFragmentedWithSomeEmptyFragments() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final List<ByteBuffer> bufferList = new ArrayList<ByteBuffer>();
        final CountDownLatch latch = new CountDownLatch(1);

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

                            // ### Issue# 316: Temporary hack till the issue related to Connector writing out TEXT frame
                            //                  instead of BINARY is resolved.
                            IoFilterChain parentFilterChain = wsnConnectSession.getParent().getFilterChain();
                            if (parentFilterChain.contains(TEXT_FILTER_NAME)) {
                                parentFilterChain.remove(TEXT_FILTER_NAME);
                            }

                            IoBufferAllocatorEx<? extends WsBuffer> allocator = wsnConnectSession.getBufferAllocator();
                            WsBuffer wsBuffer = allocator.wrap(buffer, IoBufferEx.FLAG_SHARED);
                            wsBuffer.setKind(WsBuffer.Kind.BINARY);
                            wsnConnectSession.write(wsBuffer);
                        }

                        return null;
                    }
                });
                allowing(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
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

        final ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());
        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
        context.assertIsSatisfied();
    }

    @Test
    @Specification({
        "server.echo.binary.payload.length.125.not.fragmented/handshake.response.and.frame" })
    public void shouldEchoServerSendBinaryFrameWithPayloadNotFragmented() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final List<ByteBuffer> bufferList = new ArrayList<ByteBuffer>();
        final CountDownLatch latch = new CountDownLatch(1);

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

                            // ### Issue# 316: Temporary hack till the issue related to Connector writing out TEXT frame
                            //                  instead of BINARY is resolved.
                            IoFilterChain parentFilterChain = wsnConnectSession.getParent().getFilterChain();
                            if (parentFilterChain.contains(TEXT_FILTER_NAME)) {
                                parentFilterChain.remove(TEXT_FILTER_NAME);
                            }

                            IoBufferAllocatorEx<? extends WsBuffer> allocator = wsnConnectSession.getBufferAllocator();
                            WsBuffer wsBuffer = allocator.wrap(buffer, IoBufferEx.FLAG_SHARED);
                            wsBuffer.setKind(WsBuffer.Kind.BINARY);
                            wsnConnectSession.write(wsBuffer);
                        }

                        return null;
                    }
                });
                allowing(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
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

        final ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());
        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
        context.assertIsSatisfied();
    }

    @Test
    @Ignore("Issue# 306: IllegalArgumentException: message is empty. Forgot to call flip")
    @Specification({
        "server.echo.text.payload.length.0.fragmented/handshake.response.and.frames"
        })
    public void shouldEchoServerSendTextFrameWithEmptyPayloadFragmented() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final List<ByteBuffer> bufferList = new ArrayList<ByteBuffer>();
        final CountDownLatch latch = new CountDownLatch(1);

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
                allowing(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
                will(new CustomAction("Capture exception") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        Throwable throwable = (Throwable) invocation.getParameter(1);
                        throwable.printStackTrace();
                        return null;
                    }
                });
                oneOf(handler).messageSent(with(any(IoSessionEx.class)), with(any(Object.class)));
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

        final ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());
        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
        context.assertIsSatisfied();
    }

    @Test
    @Ignore("Issue# 306: IllegalArgumentException: message is empty. Forgot to call flip")
    @Specification({
        "server.echo.text.payload.length.0.fragmented.with.injected.ping.pong/handshake.response.and.frames"
        })
    public void shouldEchoServerSendTextFrameWithEmptyPayloadFragmentedAndInjectedPingPong() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final List<ByteBuffer> bufferList = new ArrayList<ByteBuffer>();
        final CountDownLatch latch = new CountDownLatch(1);

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
                // Once the issue is fixed, we can relax this expectation.
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
                will(new CustomAction("Capture exception") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        Throwable throwable = (Throwable) invocation.getParameter(1);
                        throwable.printStackTrace();
                        return null;
                    }
                });
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

        final ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());
        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
        context.assertIsSatisfied();
    }

    @Test
    @Specification({
        "server.echo.text.payload.length.125.fragmented/handshake.response.and.frames" })
    public void shouldEchoServerSendTextFrameWithPayloadFragmented() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final List<ByteBuffer> bufferList = new ArrayList<ByteBuffer>();
        final CountDownLatch latch = new CountDownLatch(1);

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
                allowing(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
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

        final ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());
        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
        context.assertIsSatisfied();
    }

    @Test
    @Specification({
        "server.echo.text.payload.length.125.fragmented.but.not.utf8.aligned/handshake.response.and.frames" })
    public void shouldEchoServerSendTextFrameWithPayloadFragmentedEvenWhenNotUTF8Aligned() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final List<ByteBuffer> bufferList = new ArrayList<ByteBuffer>();
        final CountDownLatch latch = new CountDownLatch(1);

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
                allowing(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
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

        final ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());
        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
        context.assertIsSatisfied();
    }

    @Test
    @Specification({
        "server.echo.text.payload.length.125.fragmented.with.injected.ping.pong/handshake.response.and.frames" })
    public void shouldEchoServerSendTextFrameWithPayloadFragmentedAndInjectedPingPong() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final List<ByteBuffer> bufferList = new ArrayList<ByteBuffer>();
        final CountDownLatch latch = new CountDownLatch(1);

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
                allowing(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
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

        final ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());
        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
        context.assertIsSatisfied();
    }

    @Test
    @Specification({
        "server.echo.text.payload.length.125.fragmented.with.some.empty.fragments/handshake.response.and.frames" })
    public void shouldEchoServerSendTextFrameWithPayloadFragmentedWithSomeEmptyFragments() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final List<ByteBuffer> bufferList = new ArrayList<ByteBuffer>();
        final CountDownLatch latch = new CountDownLatch(1);

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
                allowing(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
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

        final ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());
        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
        context.assertIsSatisfied();
    }

    @Test
    @Specification({
        "server.echo.text.payload.length.125.not.fragmented/handshake.response.and.frame" })
    public void shouldEchoServerSendTextFrameWithPayloadNotFragmented() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final List<ByteBuffer> bufferList = new ArrayList<ByteBuffer>();
        final CountDownLatch latch = new CountDownLatch(1);

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
                allowing(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
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

        final ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());
        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
        context.assertIsSatisfied();
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
                allowing(handler).messageReceived(with(any(IoSessionEx.class)), with(any(Object.class)));
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
        "server.send.close.payload.length.2.fragmented/handshake.response.and.frames" })
    public void shouldFailWebSocketConnectionWhenServerSendCloseFrameWithPayloadFragmented() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                allowing(handler).messageReceived(with(any(IoSessionEx.class)), with(any(Object.class)));
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
        "server.send.continuation.payload.length.125.fragmented/handshake.response.and.frames" })
    public void shouldFailWebSocketConnectionWhenServerSendContinuationFrameWithPayloadFragmented() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                allowing(handler).messageReceived(with(any(IoSessionEx.class)), with(any(Object.class)));
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
        "server.send.continuation.payload.length.125.not.fragmented/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendContinuationFrameWithPayloadNotFragmented() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                allowing(handler).messageReceived(with(any(IoSessionEx.class)), with(any(Object.class)));
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
        "server.send.ping.payload.length.0.fragmented/handshake.response.and.frames" })
    public void shouldFailWebSocketConnectionWhenServerSendPingFrameWithPayloadFragmented() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                allowing(handler).messageReceived(with(any(IoSessionEx.class)), with(any(Object.class)));
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
        "server.send.pong.payload.length.0.fragmented/handshake.response.and.frames" })
    public void shouldFailWebSocketConnectionWhenServerSendPongFrameWithPayloadFragmented() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                allowing(handler).messageReceived(with(any(IoSessionEx.class)), with(any(Object.class)));
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
