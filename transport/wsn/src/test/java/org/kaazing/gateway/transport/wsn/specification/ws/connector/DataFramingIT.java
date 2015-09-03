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

public class DataFramingIT {
    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/ws/data");

    private final WsnConnectorRule connectorRule = new WsnConnectorRule();

    @Rule
    public TestRule chain = createRuleChain(connectorRule, k3po);

    @Test
    @Specification({
        "server.send.opcode.0x03/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendOpcode3Frame() throws Exception {
        IoHandler handler = new IoHandlerAdapter<IoSessionEx>();
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({
        "server.send.opcode.0x04/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendOpcode4Frame() throws Exception {
        IoHandler handler = new IoHandlerAdapter<IoSessionEx>();
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({
        "server.send.opcode.0x05/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendOpcode5Frame() throws Exception {
        IoHandler handler = new IoHandlerAdapter<IoSessionEx>();
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({
        "server.send.opcode.0x06/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendOpcode6Frame() throws Exception {
        IoHandler handler = new IoHandlerAdapter<IoSessionEx>();
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({
        "server.send.opcode.0x07/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendOpcode7Frame() throws Exception {
        IoHandler handler = new IoHandlerAdapter<IoSessionEx>();
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

}
