/**
 * Copyright (c) 2007-2014, Kaazing Corporation. All rights reserved.
 */

package org.kaazing.gateway.transport.wsn.specification.ws.connector;

import static org.junit.Assert.assertFalse;
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


public class OpeningHandshakeIT {

    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/ws/opening");

    private final WsnConnectorRule connectorRule = new WsnConnectorRule();

    @Rule
    public TestRule chain = createRuleChain(connectorRule, k3po);

    @Test
    @Specification({"response.header.sec.websocket.accept.missing/handshake.response"})
    public void missingSecWebSocketAcceptHeader() throws Exception {
        IoHandler handler = new IoHandlerAdapter<IoSessionEx>() {};
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/path?query", null, handler);
        connectFuture.awaitUninterruptibly();
        assertFalse(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({"connection.established/handshake.response"})
    public void shouldEstablishConnection() throws Exception {
        IoHandler handler = new IoHandlerAdapter<IoSessionEx>() {};
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/path?query", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

}
