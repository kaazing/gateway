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

import static org.junit.Assert.assertTrue;
import static org.kaazing.test.util.ITUtil.createRuleChain;

import java.nio.ByteBuffer;
import java.util.Random;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandler;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.ws.bridge.filter.WsBuffer;
import org.kaazing.gateway.transport.wsn.WsnProtocol;
import org.kaazing.gateway.transport.wsn.WsnSession;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.session.IoSessionEx;

public class BaseFramingIT {
    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/ws/framing");

    private final WsnConnectorRule connectorRule = new WsnConnectorRule();

    @Rule
    public TestRule chain = createRuleChain(connectorRule, k3po);

    @Test
    @Ignore("Exception: message is empty. Forgot to call flip()?")
    @Specification({
        "echo.binary.payload.length.0/handshake.response.and.frame"
        })
    public void shouldEchoBinaryFrameWithPayloadLength0() throws Exception {
        IoHandler handler = newIoHandlerForBaseFraming(WsBuffer.Kind.BINARY, 0);
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({
        "echo.binary.payload.length.125/handshake.response.and.frame"
        })
    public void shouldEchoBinaryFrameWithPayloadLength125() throws Exception {
        IoHandler handler = newIoHandlerForBaseFraming(WsBuffer.Kind.BINARY, 125);
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({
        "echo.binary.payload.length.126/handshake.response.and.frame"
        })
    public void shouldEchoBinaryFrameWithPayloadLength126() throws Exception {
        IoHandler handler = newIoHandlerForBaseFraming(WsBuffer.Kind.BINARY, 126);
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({
        "echo.binary.payload.length.127/handshake.response.and.frame"
        })
    public void shouldEchoBinaryFrameWithPayloadLength127() throws Exception {
        IoHandler handler = newIoHandlerForBaseFraming(WsBuffer.Kind.BINARY, 127);
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({
        "echo.binary.payload.length.128/handshake.response.and.frame"
        })
    public void shouldEchoBinaryFrameWithPayloadLength128() throws Exception {
        IoHandler handler = newIoHandlerForBaseFraming(WsBuffer.Kind.BINARY, 128);
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({
        "echo.binary.payload.length.65535/handshake.response.and.frame"
        })
    public void shouldEchoBinaryFrameWithPayloadLength65535() throws Exception {
        IoHandler handler = newIoHandlerForBaseFraming(WsBuffer.Kind.BINARY, 65535);
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({
        "echo.binary.payload.length.65536/handshake.response.and.frame"
        })
    public void shouldEchoBinaryFrameWithPayloadLength65536() throws Exception {
        IoHandler handler = newIoHandlerForBaseFraming(WsBuffer.Kind.BINARY, 65536);
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Ignore("IllegalArgumentException: emessage is empty forgot to call flip")
    @Specification({
        "echo.text.payload.length.0/handshake.response.and.frame"
        })
    public void shouldEchoTextFrameWithPayloadLength0() throws Exception {
        IoHandler handler = newIoHandlerForBaseFraming(WsBuffer.Kind.TEXT, 0);
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({
        "echo.text.payload.length.125/handshake.response.and.frame"
        })
    public void shouldEchoTextFrameWithPayloadLength125() throws Exception {
        IoHandler handler = newIoHandlerForBaseFraming(WsBuffer.Kind.TEXT, 125);
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({
        "echo.text.payload.length.126/handshake.response.and.frame"
        })
    public void shouldEchoTextFrameWithPayloadLength126() throws Exception {
        IoHandler handler = newIoHandlerForBaseFraming(WsBuffer.Kind.TEXT, 126);
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({
        "echo.text.payload.length.127/handshake.response.and.frame"
        })
    public void shouldEchoTextFrameWithPayloadLength127() throws Exception {
        IoHandler handler = newIoHandlerForBaseFraming(WsBuffer.Kind.TEXT, 127);
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({
        "echo.text.payload.length.128/handshake.response.and.frame"
        })
    public void shouldEchoTextFrameWithPayloadLength128() throws Exception {
        IoHandler handler = newIoHandlerForBaseFraming(WsBuffer.Kind.TEXT, 128);
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({
        "echo.text.payload.length.65535/handshake.response.and.frame"
        })
    public void shouldEchoTextFrameWithPayloadLength65535() throws Exception {
        IoHandler handler = newIoHandlerForBaseFraming(WsBuffer.Kind.TEXT, 65535);
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({
        "echo.text.payload.length.65536/handshake.response.and.frame"
        })
    public void shouldEchoTextFrameWithPayloadLength65536() throws Exception {
        IoHandler handler = newIoHandlerForBaseFraming(WsBuffer.Kind.TEXT, 65536);
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    private static IoHandler newIoHandlerForBaseFraming(final WsBuffer.Kind kind, final int len) {
        return new IoHandlerAdapter<IoSessionEx>() {
            protected void doSessionOpened(IoSessionEx session) throws Exception {
                WsnSession wsnConnectSession = (WsnSession) session;
                byte[] bytes = null;

                String filterName = WsnProtocol.NAME + "#text";

                if (wsnConnectSession != null) {
                    IoFilterChain parentFilterChain = wsnConnectSession.getParent().getFilterChain();
                    if (parentFilterChain.contains(filterName)) {
                        parentFilterChain.remove(filterName);
                    }
                }

                if (kind == WsBuffer.Kind.BINARY) {
                    Random random = new Random();
                    bytes = new byte[len];

                    if (len > 0) {
                        random.nextBytes(bytes);
                    }
                }
                else {
                    String str = len > 0 ? new RandomString(len).nextString() : "";
                    bytes = str.getBytes();
                }

                IoBufferAllocatorEx<? extends WsBuffer> allocator = wsnConnectSession.getBufferAllocator();
                WsBuffer wsBuffer = allocator.wrap(ByteBuffer.wrap(bytes), IoBufferEx.FLAG_SHARED);
                wsBuffer.setKind(kind);
                wsnConnectSession.write(wsBuffer);
            }
        };
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
