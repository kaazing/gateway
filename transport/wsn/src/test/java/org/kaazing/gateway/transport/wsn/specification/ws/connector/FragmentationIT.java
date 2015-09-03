package org.kaazing.gateway.transport.wsn.specification.ws.connector;

import static org.junit.Assert.assertTrue;
import static org.kaazing.gateway.resource.address.ws.WsResourceAddress.LIGHTWEIGHT;
import static org.kaazing.test.util.ITUtil.createRuleChain;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandler;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.ws.bridge.filter.WsBuffer;
import org.kaazing.gateway.transport.wsn.WsnProtocol;
import org.kaazing.gateway.transport.wsn.WsnSession;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.session.IoSessionEx;

public class FragmentationIT {
    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/ws/fragmentation");

    private final WsnConnectorRule connectorRule = new WsnConnectorRule();

    @Rule
    public TestRule chain = createRuleChain(connectorRule, k3po);


    @Test
    @Ignore("IllegalArgumentException: message is empty. Forgot to call flip")
    @Specification({
        "server.echo.binary.payload.length.0.fragmented.with.injected.ping.pong/handshake.response.and.frames" })
    public void shouldEchoServerSendBinaryFrameWithEmptyPayloadFragmentedAndInjectedPingPong() throws Exception {
        IoHandler handler = newFragmentationIoHandlerAdapter(WsBuffer.Kind.BINARY, 5);
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({
        "server.echo.binary.payload.length.125.fragmented/handshake.response.and.frames" })
    public void shouldEchoServerSendBinaryFrameWithPayloadFragmented() throws Exception {
        IoHandler handler = newFragmentationIoHandlerAdapter(WsBuffer.Kind.BINARY, 5);
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({
        "server.echo.binary.payload.length.125.fragmented.with.injected.ping.pong/handshake.response.and.frames" })
    public void shouldEchoServerSendBinaryFrameWithPayloadFragmentedAndInjectedPingPong() throws Exception {
        IoHandler handler = newFragmentationIoHandlerAdapter(WsBuffer.Kind.BINARY, 5);
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({
        "server.echo.binary.payload.length.125.fragmented.with.some.empty.fragments/handshake.response.and.frames" })
    public void shouldEchoServerSendBinaryFrameWithPayloadFragmentedWithSomeEmptyFragments() throws Exception {
        IoHandler handler = newFragmentationIoHandlerAdapter(WsBuffer.Kind.BINARY, 7);
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({
        "server.echo.binary.payload.length.125.not.fragmented/handshake.response.and.frame" })
    public void shouldEchoServerSendBinaryFrameWithPayloadNotFragmented() throws Exception {
        IoHandler handler = newFragmentationIoHandlerAdapter(WsBuffer.Kind.BINARY, 1);
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Ignore("IllegalArgumentException: message is empty. Forgot to call flip")
    @Specification({
        "server.echo.text.payload.length.0.fragmented/handshake.response.and.frames" })
    public void shouldEchoServerSendTextFrameWithEmptyPayloadFragmented() throws Exception {
        IoHandler handler = newFragmentationIoHandlerAdapter(WsBuffer.Kind.TEXT, 5);
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Ignore("IllegalArgumentException: message is empty. Forgot to call flip")
    @Specification({
        "server.echo.text.payload.length.0.fragmented.with.injected.ping.pong/handshake.response.and.frames" })
    public void shouldEchoServerSendTextFrameWithEmptyPayloadFragmentedAndInjectedPingPong() throws Exception {
        IoHandler handler = newFragmentationIoHandlerAdapter(WsBuffer.Kind.TEXT, 5);
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({
        "server.echo.text.payload.length.125.fragmented/handshake.response.and.frames" })
    public void shouldEchoServerSendTextFrameWithPayloadFragmented() throws Exception {
        IoHandler handler = newFragmentationIoHandlerAdapter(WsBuffer.Kind.TEXT, 5);
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({
        "server.echo.text.payload.length.125.fragmented.but.not.utf8.aligned/handshake.response.and.frames" })
    public void shouldEchoServerSendTextFrameWithPayloadFragmentedEvenWhenNotUTF8Aligned() throws Exception {
        IoHandler handler = newFragmentationIoHandlerAdapter(WsBuffer.Kind.TEXT, 2);
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({
        "server.echo.text.payload.length.125.fragmented.with.injected.ping.pong/handshake.response.and.frames" })
    public void shouldEchoServerSendTextFrameWithPayloadFragmentedAndInjectedPingPong() throws Exception {
        IoHandler handler = newFragmentationIoHandlerAdapter(WsBuffer.Kind.TEXT, 5);
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({
        "server.echo.text.payload.length.125.fragmented.with.some.empty.fragments/handshake.response.and.frames" })
    public void shouldEchoServerSendTextFrameWithPayloadFragmentedWithSomeEmptyFragments() throws Exception {
        IoHandler handler = newFragmentationIoHandlerAdapter(WsBuffer.Kind.TEXT, 7);
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({
        "server.echo.text.payload.length.125.not.fragmented/handshake.response.and.frame" })
    public void shouldEchoServerSendTextFrameWithPayloadNotFragmented() throws Exception {
        IoHandler handler = newFragmentationIoHandlerAdapter(WsBuffer.Kind.TEXT, 1);
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({
        "server.send.binary.payload.length.125.fragmented.but.not.continued/handshake.response.and.frames" })
    public void shouldFailWebSocketConnectionWhenServerSendBinaryFrameWithPayloadFragmentedButNotContinued() throws Exception {
        IoHandler handler = new IoHandlerAdapter<IoSessionEx>();
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({
        "server.send.close.payload.length.2.fragmented/handshake.response.and.frames" })
    public void shouldFailWebSocketConnectionWhenServerSendCloseFrameWithPayloadFragmented() throws Exception {
        IoHandler handler = new IoHandlerAdapter<IoSessionEx>();
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({
        "server.send.continuation.payload.length.125.fragmented/handshake.response.and.frames" })
    public void shouldFailWebSocketConnectionWhenServerSendContinuationFrameWithPayloadFragmented() throws Exception {
        IoHandler handler = new IoHandlerAdapter<IoSessionEx>();
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({
        "server.send.continuation.payload.length.125.not.fragmented/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendContinuationFrameWithPayloadNotFragmented() throws Exception {
        IoHandler handler = new IoHandlerAdapter<IoSessionEx>();
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({
        "server.send.ping.payload.length.0.fragmented/handshake.response.and.frames" })
    public void shouldFailWebSocketConnectionWhenServerSendPingFrameWithPayloadFragmented() throws Exception {
        IoHandler handler = new IoHandlerAdapter<IoSessionEx>();
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({
        "server.send.pong.payload.length.0.fragmented/handshake.response.and.frames" })
    public void shouldFailWebSocketConnectionWhenServerSendPongFrameWithPayloadFragmented() throws Exception {
        IoHandler handler = new IoHandlerAdapter<IoSessionEx>();
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    private IoHandlerAdapter<IoSessionEx> newFragmentationIoHandlerAdapter(WsBuffer.Kind kind, int fragmentCount) {
        IoHandlerAdapter<IoSessionEx> connectHandler = new IoHandlerAdapter<IoSessionEx>() {
            List<ByteBuffer> bufferList = new ArrayList<ByteBuffer>();

            @Override
            protected void doMessageReceived(IoSessionEx session, Object message) throws Exception {
                WsnSession wsnConnectSession = (WsnSession) session;
                String filterName = WsnProtocol.NAME + "#text";

                if (wsnConnectSession != null) {
                    IoFilterChain parentFilterChain = wsnConnectSession.getParent().getFilterChain();
                    if (parentFilterChain.contains(filterName)) {
                        parentFilterChain.remove(filterName);
                    }

                    IoFilterChain filterChain = wsnConnectSession.getFilterChain();
                    IoBufferAllocatorEx<? extends WsBuffer> allocator = wsnConnectSession.getBufferAllocator();

                    final boolean hasPostUpgradeChildWsnSession = wsnConnectSession.getHandler() == this;
                    final ResourceAddress wsnSessionLocalAddress = wsnConnectSession.getLocalAddress();
                    final boolean isLightweightWsnSession = wsnSessionLocalAddress.getOption(LIGHTWEIGHT);
                    boolean sendMessagesDirect = isLightweightWsnSession
                                                 && hasPostUpgradeChildWsnSession; // post-upgrade

                    if (sendMessagesDirect) {
                        filterChain.fireMessageReceived(message);
                        return;
                    }

                    IoBufferEx ioBuffer = (IoBufferEx) message;
                    bufferList.add(ioBuffer.buf());

                    if (bufferList.size() == fragmentCount) {
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
        return connectHandler;
    }
}
