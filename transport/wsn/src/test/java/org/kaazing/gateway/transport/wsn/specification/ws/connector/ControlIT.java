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

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandler;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.mina.core.session.IoSessionEx;

public class ControlIT {
    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/ws/control");

    private final WsnConnectorRule connectorRule = new WsnConnectorRule();

    @Rule
    public TestRule chain = createRuleChain(connectorRule, k3po);

    @Test
    @Specification({
        "server.send.close.payload.length.0/handshake.response.and.frame" })
    public void shouldEchoServerCloseFrameWithEmptyPayload() throws Exception {
        IoHandler handler = new IoHandlerAdapter<IoSessionEx>();
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({
        "server.send.close.payload.length.125/handshake.response.and.frame" })
    public void shouldEchoServerCloseFrameWithPayload() throws Exception {
        IoHandler handler = new IoHandlerAdapter<IoSessionEx>();
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({
        "server.send.close.payload.length.126/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendCloseFrameWithPayloadTooLong() throws Exception {
        IoHandler handler = new IoHandlerAdapter<IoSessionEx>();
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({
        "server.send.ping.payload.length.0/handshake.response.and.frame" })
    public void shouldPongServerPingFrameWithEmptyPayload() throws Exception {
        IoHandler handler = new IoHandlerAdapter<IoSessionEx>();
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({
        "server.send.ping.payload.length.125/handshake.response.and.frame" })
    public void shouldPongServerPingFrameWithPayload() throws Exception {
        IoHandler handler = new IoHandlerAdapter<IoSessionEx>();
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({
        "server.send.ping.payload.length.126/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendPingFrameWithPayloadTooLong() throws Exception {
        IoHandler handler = new IoHandlerAdapter<IoSessionEx>();
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({
        "server.send.pong.payload.length.0/handshake.response.and.frame" })
    public void shouldReceiveServerPongFrameWithEmptyPayload() throws Exception {
        IoHandler handler = new IoHandlerAdapter<IoSessionEx>();
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({
        "server.send.pong.payload.length.125/handshake.response.and.frame" })
    public void shouldReceiveServerPongFrameWithPayload() throws Exception {
        IoHandler handler = new IoHandlerAdapter<IoSessionEx>();
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({
        "server.send.pong.payload.length.126/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendPongFrameWithPayloadTooLong() throws Exception {
        IoHandler handler = new IoHandlerAdapter<IoSessionEx>();
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({
        "server.send.opcode.0x0b/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendOpcode11Frame() throws Exception {
        IoHandler handler = new IoHandlerAdapter<IoSessionEx>();
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({
        "server.send.opcode.0x0c/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendOpcode12Frame() throws Exception {
        IoHandler handler = new IoHandlerAdapter<IoSessionEx>();
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({
        "server.send.opcode.0x0d/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendOpcode13Frame() throws Exception {
        IoHandler handler = new IoHandlerAdapter<IoSessionEx>();
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({
        "server.send.opcode.0x0e/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendOpcode14Frame() throws Exception {
        IoHandler handler = new IoHandlerAdapter<IoSessionEx>();
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({
        "server.send.opcode.0x0f/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendOpcode15Frame() throws Exception {
        IoHandler handler = new IoHandlerAdapter<IoSessionEx>();
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

}
