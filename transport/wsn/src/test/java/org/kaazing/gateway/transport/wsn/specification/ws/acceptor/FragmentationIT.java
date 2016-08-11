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
package org.kaazing.gateway.transport.wsn.specification.ws.acceptor;

import static org.kaazing.gateway.resource.address.ws.WsResourceAddress.LIGHTWEIGHT;
import static org.kaazing.test.util.ITUtil.createRuleChain;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.ws.bridge.filter.WsBuffer;
import org.kaazing.gateway.transport.wsn.WsnAcceptorRule;
import org.kaazing.gateway.transport.wsn.WsnSession;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.session.IoSessionEx;

/**
 * RFC-6455, section 5.4 "Fragmentation"
 */
public class FragmentationIT {

    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/ws/fragmentation");

    private final WsnAcceptorRule acceptor = new WsnAcceptorRule();

    @Rule
    public TestRule chain = createRuleChain(acceptor, k3po);


    @Test
    @Specification({
        "client.send.continuation.payload.length.125.not.fragmented/handshake.request.and.frame"
        })
    public void shouldFailWebSocketConnectionWhenClientSendContinuationFrameWithPayloadNotFragmented() throws Exception {
        IoHandlerAdapter<IoSessionEx> acceptHandler = textFragmentIoHandlerAdapter(WsBuffer.Kind.CONTINUATION);
        acceptor.bind("ws://localhost:8080/echo", acceptHandler);
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.continuation.payload.length.125.fragmented/handshake.request.and.frames"
        })
    public void shouldFailWebSocketConnectionWhenClientSendContinuationFrameWithPayloadFragmented() throws Exception {
        IoHandlerAdapter<IoSessionEx> acceptHandler = textFragmentIoHandlerAdapter(WsBuffer.Kind.CONTINUATION);
        acceptor.bind("ws://localhost:8080/echo", acceptHandler);
        k3po.finish();
    }

    @Test
    @Specification({
        "client.echo.text.payload.length.125.not.fragmented/handshake.request.and.frame"
        })
    public void shouldEchoClientSendTextFrameWithPayloadNotFragmented() throws Exception {
        IoHandlerAdapter<IoSessionEx> acceptHandler = textFragmentIoHandlerAdapter(WsBuffer.Kind.TEXT);
        acceptor.bind("ws://localhost:8080/echo", acceptHandler);
        k3po.finish();
    }

    @Test
    @Specification({
        "client.echo.text.payload.length.0.fragmented/handshake.request.and.frames"
        })
    public void shouldEchoClientSendTextFrameWithEmptyPayloadFragmented() throws Exception {
        IoHandlerAdapter<IoSessionEx> acceptHandler = textFragmentIoHandlerAdapter(WsBuffer.Kind.TEXT);
        acceptor.bind("ws://localhost:8080/echo", acceptHandler);
        k3po.finish();
    }

    @Test
    @Specification({
        "client.echo.text.payload.length.0.fragmented.with.injected.ping.pong/handshake.request.and.frames"
        })
    public void shouldEchoClientSendTextFrameWithEmptyPayloadFragmentedAndInjectedPingPong() throws Exception {
        IoHandlerAdapter<IoSessionEx> acceptHandler = textFragmentIoHandlerAdapter(WsBuffer.Kind.TEXT);
        acceptor.bind("ws://localhost:8080/echo", acceptHandler);
        k3po.finish();
    }

    @Test
    @Specification({
        "client.echo.text.payload.length.125.fragmented/handshake.request.and.frames"
        })
    public void shouldEchoClientSendTextFrameWithPayloadFragmented() throws Exception {
        IoHandlerAdapter<IoSessionEx> acceptHandler = textFragmentIoHandlerAdapter(WsBuffer.Kind.TEXT);
        acceptor.bind("ws://localhost:8080/echo", acceptHandler);
        k3po.finish();
    }

    @Test
    @Specification({
        "client.echo.text.payload.length.125.fragmented.with.some.empty.fragments/handshake.request.and.frames"
        })
    public void shouldEchoClientSendTextFrameWithPayloadFragmentedWithSomeEmptyFragments() throws Exception {
        IoHandlerAdapter<IoSessionEx> acceptHandler = textFragmentIoHandlerAdapter(WsBuffer.Kind.TEXT);
        acceptor.bind("ws://localhost:8080/echo", acceptHandler);
        k3po.finish();
    }

    @Test
    @Ignore("TODO: investigate")
    @Specification({
        "client.echo.text.payload.length.125.fragmented.but.not.utf8.aligned/handshake.request.and.frames"
        })
    public void shouldEchoClientSendTextFrameWithPayloadFragmentedEvenWhenNotUTF8Aligned() throws Exception {
        IoHandlerAdapter<IoSessionEx> acceptHandler = textFragmentIoHandlerAdapter(WsBuffer.Kind.TEXT);
        acceptor.bind("ws://localhost:8080/echo", acceptHandler);
        k3po.finish();
    }

    @Test
    @Specification({
        "client.echo.text.payload.length.125.fragmented.with.injected.ping.pong/handshake.request.and.frames"
        })
    public void shouldEchoClientSendTextFrameWithPayloadFragmentedAndInjectedPingPong() throws Exception {
        IoHandlerAdapter<IoSessionEx> acceptHandler = textFragmentIoHandlerAdapter(WsBuffer.Kind.TEXT);
        acceptor.bind("ws://localhost:8080/echo", acceptHandler);
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.text.payload.length.125.fragmented.but.not.continued/handshake.request.and.frames"
        })
    public void shouldFailWebSocketConnectionWhenClientSendTextFrameWithPayloadFragmentedButNotContinued() throws Exception {
        IoHandlerAdapter<IoSessionEx> acceptHandler = textFragmentIoHandlerAdapter(WsBuffer.Kind.TEXT);
        acceptor.bind("ws://localhost:8080/echo", acceptHandler);
        k3po.finish();
    }

    @Test
    @Specification({
        "client.echo.binary.payload.length.125.not.fragmented/handshake.request.and.frame"
        })
    public void shouldEchoClientSendBinaryFrameWithPayloadNotFragmented() throws Exception {
        IoHandlerAdapter<IoSessionEx> acceptHandler = textFragmentIoHandlerAdapter(WsBuffer.Kind.BINARY);
        acceptor.bind("ws://localhost:8080/echo", acceptHandler);
        k3po.finish();
    }

    @Test
    @Specification({
        "client.echo.binary.payload.length.0.fragmented/handshake.request.and.frames"
        })
    public void shouldEchoClientSendBinaryFrameWithEmptyPayloadFragmented() throws Exception {
        IoHandlerAdapter<IoSessionEx> acceptHandler = textFragmentIoHandlerAdapter(WsBuffer.Kind.BINARY);
        acceptor.bind("ws://localhost:8080/echo", acceptHandler);
        k3po.finish();
    }

    @Test
    @Specification({
        "client.echo.binary.payload.length.0.fragmented.with.injected.ping.pong/handshake.request.and.frames"
        })
    public void shouldEchoClientSendBinaryFrameWithEmptyPayloadFragmentedAndInjectedPingPong() throws Exception {
        IoHandlerAdapter<IoSessionEx> acceptHandler = textFragmentIoHandlerAdapter(WsBuffer.Kind.BINARY);
        acceptor.bind("ws://localhost:8080/echo", acceptHandler);
        k3po.finish();
    }

    @Test
    @Specification({
        "client.echo.binary.payload.length.125.fragmented/handshake.request.and.frames"
        })
    public void shouldEchoClientSendBinaryFrameWithPayloadFragmented() throws Exception {
        IoHandlerAdapter<IoSessionEx> acceptHandler = textFragmentIoHandlerAdapter(WsBuffer.Kind.BINARY);
        acceptor.bind("ws://localhost:8080/echo", acceptHandler);
        k3po.finish();
    }

    @Test
    @Specification({
        "client.echo.binary.payload.length.125.fragmented.with.some.empty.fragments/handshake.request.and.frames"
        })
    public void shouldEchoClientSendBinaryFrameWithPayloadFragmentedWithSomeEmptyFragments() throws Exception {
        IoHandlerAdapter<IoSessionEx> acceptHandler = textFragmentIoHandlerAdapter(WsBuffer.Kind.BINARY);
        acceptor.bind("ws://localhost:8080/echo", acceptHandler);
        k3po.finish();
    }

    @Test
    @Specification({
        "client.echo.binary.payload.length.125.fragmented.with.injected.ping.pong/handshake.request.and.frames"
        })
    public void shouldEchoClientSendBinaryFrameWithPayloadFragmentedAndInjectedPingPong() throws Exception {
        IoHandlerAdapter<IoSessionEx> acceptHandler = textFragmentIoHandlerAdapter(WsBuffer.Kind.BINARY);
        acceptor.bind("ws://localhost:8080/echo", acceptHandler);
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.binary.payload.length.125.fragmented.but.not.continued/handshake.request.and.frames"
        })
    public void shouldFailWebSocketConnectionWhenClientSendBinaryFrameWithPayloadFragmentedButNotContinued() throws Exception {
        acceptor.bind("ws://localhost:8080/echo", new IoHandlerAdapter<IoSessionEx>());
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.close.payload.length.2.fragmented/handshake.request.and.frames"
        })
    public void shouldFailWebSocketConnectionWhenClientSendCloseFrameWithPayloadFragmented() throws Exception {
        acceptor.bind("ws://localhost:8080/echo", new IoHandlerAdapter<IoSessionEx>());
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.ping.payload.length.0.fragmented/handshake.request.and.frames"
        })
    public void shouldFailWebSocketConnectionWhenClientSendPingFrameWithPayloadFragmented() throws Exception {
        acceptor.bind("ws://localhost:8080/echo", new IoHandlerAdapter<IoSessionEx>());
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.pong.payload.length.0.fragmented/handshake.request.and.frames"
        })
    public void shouldFailWebSocketConnectionWhenClientSendPongFrameWithPayloadFragmented() throws Exception {
        acceptor.bind("ws://localhost:8080/echo", new IoHandlerAdapter<IoSessionEx>());
        k3po.finish();
    }

    /** special IoHandlerAdapter for consolidating message fragments
     * @return
     */
    private IoHandlerAdapter<IoSessionEx> textFragmentIoHandlerAdapter(WsBuffer.Kind kind) {
        IoHandlerAdapter<IoSessionEx> acceptHandler = new IoHandlerAdapter<IoSessionEx>() {
            List<ByteBuffer> bufferList = new ArrayList<>();

            @Override
            protected void doMessageReceived(IoSessionEx session, Object message) throws Exception {
                WsnSession wsnSession = (WsnSession) session;

                if (wsnSession != null) {
                    IoFilterChain filterChain = wsnSession.getFilterChain();
                    IoBufferAllocatorEx<? extends WsBuffer> allocator = wsnSession.getBufferAllocator();

                    final boolean hasPostUpgradeChildWsnSession = wsnSession.getHandler() == this;
                    final ResourceAddress wsnSessionLocalAddress = wsnSession.getLocalAddress();
                    final boolean isLightweightWsnSession = wsnSessionLocalAddress.getOption(LIGHTWEIGHT);
                    boolean sendMessagesDirect = isLightweightWsnSession
                                                 && hasPostUpgradeChildWsnSession; // post-upgrade
                    if ( sendMessagesDirect ) {
                        filterChain.fireMessageReceived(message);
                        return;
                    }

                    WsBuffer wsMessage = (WsBuffer) message;


                    bufferList.add(wsMessage.buf());

                    if (wsMessage.isFin()) {
                        ByteBuffer buffer = ByteBuffer.allocate(4096);

                        for (ByteBuffer bb : bufferList) {
                            buffer.put(bb);
                        }
                        buffer.flip();
                        WsBuffer wsBuffer = allocator.wrap(buffer, IoBufferEx.FLAG_SHARED);
                        wsBuffer.setKind(kind);
                        session.write(wsBuffer);
                    }
                }
            }

        };
        return acceptHandler;
    }
}
