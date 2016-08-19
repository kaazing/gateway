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

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandler;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
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

public class BaseFramingIT {
    private final WsnConnectorRule connector = new WsnConnectorRule();
    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/ws/framing");
    private final TestRule timeoutRule = new DisableOnDebug(Timeout.builder().withTimeout(10, SECONDS)
                .withLookingForStuckThread(true).build());
    private final TestRule trace = new MethodExecutionTrace();

    private static Charset UTF_8 = Charset.forName("UTF-8");

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
        "echo.binary.payload.length.0/handshake.response.and.frame"
        })
    public void shouldEchoBinaryFrameWithPayloadLength0() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).messageReceived(with(any(IoSessionEx.class)), with(hasRemaining(0)));
                will(countDown(latch));
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        WsnSession wsnConnectSession = (WsnSession) connectFuture.getSession();

        byte[] bytes = new byte[0];
        IoBufferAllocatorEx<? extends WsBuffer> allocator = wsnConnectSession.getBufferAllocator();
        WsBuffer wsBuffer = allocator.wrap(ByteBuffer.wrap(bytes), IoBufferEx.FLAG_SHARED);
        wsBuffer.setKind(WsBuffer.Kind.BINARY);
        wsnConnectSession.write(wsBuffer);

        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
    }

    @Test
    @Specification({
        "echo.binary.payload.length.125/handshake.response.and.frame"
        })
    public void shouldEchoBinaryFrameWithPayloadLength125() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);
        byte[] bytes = new byte[125];
        Random random = new Random();
        random.nextBytes(bytes);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).messageReceived(with(any(IoSessionEx.class)), with(ioBufferMatching(bytes)));
                will(countDown(latch));
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        WsnSession wsnConnectSession = (WsnSession) connectFuture.getSession();

        IoBufferAllocatorEx<? extends WsBuffer> allocator = wsnConnectSession.getBufferAllocator();
        WsBuffer wsBuffer = allocator.wrap(ByteBuffer.wrap(bytes), IoBufferEx.FLAG_SHARED);
        wsBuffer.setKind(WsBuffer.Kind.BINARY);
        wsnConnectSession.write(wsBuffer);

        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
    }

    @Test
    @Specification({
        "echo.binary.payload.length.126/handshake.response.and.frame"
        })
    public void shouldEchoBinaryFrameWithPayloadLength126() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        Random random = new Random();
        byte[] bytes = new byte[126];
        random.nextBytes(bytes);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).messageReceived(with(any(IoSessionEx.class)), with(ioBufferMatching(bytes)));
                will(countDown(latch));
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        WsnSession wsnConnectSession = (WsnSession) connectFuture.getSession();

        IoBufferAllocatorEx<? extends WsBuffer> allocator = wsnConnectSession.getBufferAllocator();
        WsBuffer wsBuffer = allocator.wrap(ByteBuffer.wrap(bytes), IoBufferEx.FLAG_SHARED);
        wsBuffer.setKind(WsBuffer.Kind.BINARY);
        wsnConnectSession.write(wsBuffer);

        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
    }

    @Test
    @Specification({
        "echo.binary.payload.length.127/handshake.response.and.frame"
        })
    public void shouldEchoBinaryFrameWithPayloadLength127() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        Random random = new Random();
        byte[] bytes = new byte[127];
        random.nextBytes(bytes);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).messageReceived(with(any(IoSessionEx.class)), with(ioBufferMatching(bytes)));
                will(countDown(latch));
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        WsnSession wsnConnectSession = (WsnSession) connectFuture.getSession();

        IoBufferAllocatorEx<? extends WsBuffer> allocator = wsnConnectSession.getBufferAllocator();
        WsBuffer wsBuffer = allocator.wrap(ByteBuffer.wrap(bytes), IoBufferEx.FLAG_SHARED);
        wsBuffer.setKind(WsBuffer.Kind.BINARY);
        wsnConnectSession.write(wsBuffer);

        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
    }

    @Test
    @Specification({
        "echo.binary.payload.length.128/handshake.response.and.frame"
        })
    public void shouldEchoBinaryFrameWithPayloadLength128() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        Random random = new Random();
        byte[] bytes = new byte[128];
        random.nextBytes(bytes);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).messageReceived(with(any(IoSessionEx.class)), with(ioBufferMatching(bytes)));
                will(countDown(latch));

            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        WsnSession wsnConnectSession = (WsnSession) connectFuture.getSession();

        IoBufferAllocatorEx<? extends WsBuffer> allocator = wsnConnectSession.getBufferAllocator();
        WsBuffer wsBuffer = allocator.wrap(ByteBuffer.wrap(bytes), IoBufferEx.FLAG_SHARED);
        wsBuffer.setKind(WsBuffer.Kind.BINARY);
        wsnConnectSession.write(wsBuffer);

        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
    }

    @Test
    @Specification({
        "echo.binary.payload.length.65535/handshake.response.and.frame"
        })
    public void shouldEchoBinaryFrameWithPayloadLength65535() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        Random random = new Random();
        byte[] bytes = new byte[65535];
        random.nextBytes(bytes);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).messageReceived(with(any(IoSessionEx.class)), with(ioBufferMatching(bytes)));
                will(countDown(latch));
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        WsnSession wsnConnectSession = (WsnSession) connectFuture.getSession();

        IoBufferAllocatorEx<? extends WsBuffer> allocator = wsnConnectSession.getBufferAllocator();
        WsBuffer wsBuffer = allocator.wrap(ByteBuffer.wrap(bytes), IoBufferEx.FLAG_SHARED);
        wsBuffer.setKind(WsBuffer.Kind.BINARY);
        wsnConnectSession.write(wsBuffer);

        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
    }

    @Test
    @Specification({
        "echo.binary.payload.length.65536/handshake.response.and.frame"
        })
    public void shouldEchoBinaryFrameWithPayloadLength65536() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        Random random = new Random();
        byte[] bytes = new byte[65536];
        random.nextBytes(bytes);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).messageReceived(with(any(IoSessionEx.class)), with(ioBufferMatching(bytes)));
                will(countDown(latch));
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        WsnSession wsnConnectSession = (WsnSession) connectFuture.getSession();
        IoBufferAllocatorEx<? extends WsBuffer> allocator = wsnConnectSession.getBufferAllocator();
        WsBuffer wsBuffer = allocator.wrap(ByteBuffer.wrap(bytes), IoBufferEx.FLAG_SHARED);
        wsBuffer.setKind(WsBuffer.Kind.BINARY);
        wsnConnectSession.write(wsBuffer);

        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
    }

    @Test
    @Specification({
        "echo.text.payload.length.0/handshake.response.and.frame"
        })
    public void shouldEchoTextFrameWithPayloadLength0() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).messageReceived(with(any(IoSessionEx.class)), with(hasRemaining(0)));
                will(countDown(latch));
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        WsnSession wsnConnectSession = (WsnSession) connectFuture.getSession();
        String str = "";
        byte[] bytes = str.getBytes(UTF_8);

        IoBufferAllocatorEx<? extends WsBuffer> allocator = wsnConnectSession.getBufferAllocator();
        WsBuffer wsBuffer = allocator.wrap(ByteBuffer.wrap(bytes), IoBufferEx.FLAG_SHARED);
        wsBuffer.setKind(WsBuffer.Kind.TEXT);
        wsnConnectSession.write(wsBuffer);

        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
    }

    @Test
    @Specification({
        "echo.text.payload.length.125/handshake.response.and.frame"
        })
    public void shouldEchoTextFrameWithPayloadLength125() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        String str = new RandomString(125).nextString();
        byte[] bytes = str.getBytes(UTF_8);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).messageReceived(with(any(IoSessionEx.class)), with(ioBufferMatching(bytes)));
                will(countDown(latch));
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        WsnSession wsnConnectSession = (WsnSession) connectFuture.getSession();

        IoBufferAllocatorEx<? extends WsBuffer> allocator = wsnConnectSession.getBufferAllocator();
        WsBuffer wsBuffer = allocator.wrap(ByteBuffer.wrap(bytes), IoBufferEx.FLAG_SHARED);
        wsBuffer.setKind(WsBuffer.Kind.TEXT);
        wsnConnectSession.write(wsBuffer);

        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
    }

    @Test
    @Specification({
        "echo.text.payload.length.126/handshake.response.and.frame"
        })
    public void shouldEchoTextFrameWithPayloadLength126() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        String str = new RandomString(126).nextString();
        byte[] bytes = str.getBytes(UTF_8);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).messageReceived(with(any(IoSessionEx.class)), with(ioBufferMatching(bytes)));
                will(countDown(latch));
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        WsnSession wsnConnectSession = (WsnSession) connectFuture.getSession();

        IoBufferAllocatorEx<? extends WsBuffer> allocator = wsnConnectSession.getBufferAllocator();
        WsBuffer wsBuffer = allocator.wrap(ByteBuffer.wrap(bytes), IoBufferEx.FLAG_SHARED);
        wsBuffer.setKind(WsBuffer.Kind.TEXT);
        wsnConnectSession.write(wsBuffer);

        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
    }

    @Test
    @Specification({
        "echo.text.payload.length.127/handshake.response.and.frame"
        })
    public void shouldEchoTextFrameWithPayloadLength127() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        String str = new RandomString(127).nextString();
        byte[] bytes = str.getBytes(UTF_8);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).messageReceived(with(any(IoSessionEx.class)), with(ioBufferMatching(bytes)));
                will(countDown(latch));
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        WsnSession wsnConnectSession = (WsnSession) connectFuture.getSession();

        IoBufferAllocatorEx<? extends WsBuffer> allocator = wsnConnectSession.getBufferAllocator();
        WsBuffer wsBuffer = allocator.wrap(ByteBuffer.wrap(bytes), IoBufferEx.FLAG_SHARED);
        wsBuffer.setKind(WsBuffer.Kind.TEXT);
        wsnConnectSession.write(wsBuffer);

        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
    }

    @Test
    @Specification({
        "echo.text.payload.length.128/handshake.response.and.frame"
        })
    public void shouldEchoTextFrameWithPayloadLength128() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        String str = new RandomString(128).nextString();
        byte[] bytes = str.getBytes(UTF_8);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).messageReceived(with(any(IoSessionEx.class)), with(ioBufferMatching(bytes)));
                will(countDown(latch));
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        WsnSession wsnConnectSession = (WsnSession) connectFuture.getSession();

        IoBufferAllocatorEx<? extends WsBuffer> allocator = wsnConnectSession.getBufferAllocator();
        WsBuffer wsBuffer = allocator.wrap(ByteBuffer.wrap(bytes), IoBufferEx.FLAG_SHARED);
        wsBuffer.setKind(WsBuffer.Kind.TEXT);
        wsnConnectSession.write(wsBuffer);

        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
    }

    @Test
    @Specification({
        "echo.text.payload.length.65535/handshake.response.and.frame"
        })
    public void shouldEchoTextFrameWithPayloadLength65535() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        String str = new RandomString(65535).nextString();
        byte[] bytes = str.getBytes(UTF_8);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).messageReceived(with(any(IoSessionEx.class)), with(ioBufferMatching(bytes)));
                will(countDown(latch));
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        WsnSession wsnConnectSession = (WsnSession) connectFuture.getSession();

        IoBufferAllocatorEx<? extends WsBuffer> allocator = wsnConnectSession.getBufferAllocator();
        WsBuffer wsBuffer = allocator.wrap(ByteBuffer.wrap(bytes), IoBufferEx.FLAG_SHARED);
        wsBuffer.setKind(WsBuffer.Kind.TEXT);
        wsnConnectSession.write(wsBuffer);

        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
    }

    @Test
    @Specification({
        "echo.text.payload.length.65536/handshake.response.and.frame"
        })
    public void shouldEchoTextFrameWithPayloadLength65536() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch latch = new CountDownLatch(1);

        String str = new RandomString(65536).nextString();
        byte[] bytes = str.getBytes(UTF_8);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).messageReceived(with(any(IoSessionEx.class)), with(ioBufferMatching(bytes)));
                will(countDown(latch));
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        WsnSession wsnConnectSession = (WsnSession) connectFuture.getSession();

        IoBufferAllocatorEx<? extends WsBuffer> allocator =
               wsnConnectSession.getBufferAllocator();
        WsBuffer wsBuffer = allocator.wrap(ByteBuffer.wrap(bytes), IoBufferEx.FLAG_SHARED);
        wsBuffer.setKind(WsBuffer.Kind.TEXT);
        wsnConnectSession.write(wsBuffer);

        k3po.finish();
        assertTrue(latch.await(10, SECONDS));
    }

    private static class RandomString {

        private static final char[] SYMBOLS;

        static {
            StringBuilder tmp = new StringBuilder();
            for (char ch = 32; ch <= 126; ++ch) {
                tmp.append(ch);
            }
            SYMBOLS = tmp.toString().toCharArray();
        }

        private final Random random = new Random();

        private final char[] buf;

        public RandomString(int length) {
            if (length < 1) {
                throw new IllegalArgumentException("length < 1: " + length);
            }
            buf = new char[length];
        }

        public String nextString() {
            for (int idx = 0; idx < buf.length; ++idx) {
                buf[idx] = SYMBOLS[random.nextInt(SYMBOLS.length)];
            }

            return new String(buf);
        }
    }
}
