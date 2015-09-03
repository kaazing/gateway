/**
 * Copyright (c) 2007-2014, Kaazing Corporation. All rights reserved.
 */

package org.kaazing.gateway.transport.wsn.specification.ws.connector;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.kaazing.test.util.ITUtil.createRuleChain;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandler;
import org.junit.Ignore;
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
    @Specification({
        "response.header.sec.websocket.accept.missing/handshake.response"
        })
    public void missingSecWebSocketAcceptHeader() throws Exception {
        IoHandler handler = new IoHandlerAdapter<IoSessionEx>();
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/path?query", null, handler);
        connectFuture.awaitUninterruptibly();
        assertFalse(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({
        "connection.established/handshake.response"
        })
    public void shouldEstablishConnection() throws Exception {
        IoHandler handler = new IoHandlerAdapter<IoSessionEx>();
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/path?query", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({
        "request.headers.random.case/handshake.response"
        })
    public void shouldEstablishConnectionWithRandomCaseRequestHeaders() throws Exception {
        IoHandler handler = new IoHandlerAdapter<IoSessionEx>();
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/path?query", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({
        "response.headers.random.case/handshake.response"
        })
    public void shouldEstablishConnectionWithRandomCaseResponseHeaders() throws Exception {
        IoHandler handler = new IoHandlerAdapter<IoSessionEx>();
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/path?query", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({
        "request.header.origin/handshake.response"
        })
    public void shouldEstablishConnectionWithRequestHeaderOrigin() throws Exception {
        IoHandler handler = new IoHandlerAdapter<IoSessionEx>();
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/path?query", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({
        "request.header.sec.websocket.protocol/handshake.response"
        })
    public void shouldEstablishConnectionWithRequestHeaderSecWebSocketProtocol() throws Exception {
        IoHandler handler = new IoHandlerAdapter<IoSessionEx>();
        String [] protocols = {"primary", " secondary"}; // Need the space to honor the script.
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/path?query", protocols, null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Ignore("Missing Sec-WebSocket-Extensions header in the handshake request")
    @Specification({
        "request.header.sec.websocket.extensions/handshake.response"
        })
    public void shouldEstablishConnectionWithRequestHeaderSecWebSocketExtensions() throws Exception {
        IoHandler handler = new IoHandlerAdapter<IoSessionEx>();
        String[] extensions = {"primary", " secondary"}; // Need the space to honor the script.
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/path?query", null, extensions, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Ignore("Missing Sec-WebSocket-Extensions header in the handshake request")
    @Specification({
        "response.header.sec.websocket.extensions.partial.agreement/handshake.response"
        })
    public void shouldEstablishConnectionWithSomeExtensionsNegotiated() throws Exception {
        IoHandler handler = new IoHandlerAdapter<IoSessionEx>();
        String[] extensions = {"valid", " invalid"}; // Need the space to honor the script.
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/path?query", null, extensions, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Ignore("Missing Sec-WebSocket-Extensions header in the handshake request")
    @Specification({
        "response.header.sec.websocket.extensions.reordered/handshake.response"
        })
    public void shouldEstablishConnectionWhenOrderOfExtensionsNegotiatedChanged() throws Exception {
        IoHandler handler = new IoHandlerAdapter<IoSessionEx>();
        String[] extensions = {"primary", " secondary"}; // Need the space to honor the script.
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/path?query", null, extensions, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Ignore("Connection header set to invalid value in the handshake response. connectFuture.isConnected() must return false")
    @Specification({
        "response.header.connection.not.upgrade/handshake.response" })
    public void shouldFailConnectionWhenResponseHeaderConnectionNotUpgrade() throws Exception {
        IoHandler handler = new IoHandlerAdapter<IoSessionEx>();
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/path?query", null, handler);
        connectFuture.awaitUninterruptibly();
        assertFalse(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Ignore("Missing Connection header in handshake response. connectFuture.isConnected() must return false")
    @Specification({
        "response.header.connection.missing/handshake.response" })
    public void shouldFailConnectionWhenResponseHeaderConnectionMissing() throws Exception {
        IoHandler handler = new IoHandlerAdapter<IoSessionEx>();
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/path?query", null, handler);
        connectFuture.awaitUninterruptibly();
        assertFalse(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Ignore("Upgrade header set to invalid value in the handshake response. connectFuture.isConnected() must return false")
    @Specification({
        "response.header.upgrade.not.websocket/handshake.response" })
    public void shouldFailConnectionWhenResponseHeaderUpgradeNotWebSocket() throws Exception {
        IoHandler handler = new IoHandlerAdapter<IoSessionEx>();
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/path?query", null, handler);
        connectFuture.awaitUninterruptibly();
        assertFalse(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Ignore("Missing Upgrade header in handshake response. connectFuture.isConnected() must return false")
    @Specification({
        "response.header.upgrade.missing/handshake.response" })
    public void shouldFailConnectionWhenResponseHeaderUpgradeMissing() throws Exception {
        IoHandler handler = new IoHandlerAdapter<IoSessionEx>();
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/path?query", null, handler);
        connectFuture.awaitUninterruptibly();
        assertFalse(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Ignore("Did not fail and should")
    @Specification({
        "response.header.sec.websocket.accept.not.hashed/handshake.response" })
    public void shouldFailConnectionWhenResponseHeaderSecWebSocketAcceptNotHashed() throws Exception {
        IoHandler handler = new IoHandlerAdapter<IoSessionEx>();
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/path?query", null, handler);
        connectFuture.awaitUninterruptibly();
        assertFalse(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({
        "response.header.sec.websocket.accept.missing/handshake.response" })
    public void shouldFailConnectionWhenResponseHeaderSecWebSocketAcceptMissing() throws Exception {
        IoHandler handler = new IoHandlerAdapter<IoSessionEx>();
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/path?query", null, handler);
        connectFuture.awaitUninterruptibly();
        assertFalse(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Ignore("Missing Sec-WebSocket-Extensions header in the handshake request. Also, connectFuture.isConnected()"
            + "must return false as the negotiated extension does not match the supported extensions.")
    @Specification({
        "response.header.sec.websocket.extensions.not.negotiated/handshake.response" })
    public void shouldFailConnectionWhenResponseHeaderSecWebSocketExtensionsNotNegotiated() throws Exception {
        IoHandler handler = new IoHandlerAdapter<IoSessionEx>();
        String[] extensions = {"primary", " secondary"};  // Need space to honor the script.
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/path?query", extensions, null, handler);
        connectFuture.awaitUninterruptibly();
        assertFalse(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Ignore("connectFuture.isConnected() must return false as the negotiated protocol does not match any"
            + "of the supported protocols")
    @Specification({
        "response.header.sec.websocket.protocol.not.negotiated/handshake.response" })
    public void shouldFailConnectionWhenResponseHeaderSecWebSocketProtocolNotNegotiated() throws Exception {
        IoHandler handler = new IoHandlerAdapter<IoSessionEx>();
        String [] protocols = {"primary", " secondary"}; // Need the space to honor the script.
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/path?query", protocols, null, handler);
        connectFuture.awaitUninterruptibly();
        assertFalse(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({
        "multiple.connections.established/handshake.responses"
        })
    public void shouldEstablishMultipleConnections() throws Exception {
        IoHandler handler1 = new IoHandlerAdapter<IoSessionEx>();
        ConnectFuture connectFuture1 = connectorRule.connect("ws://localhost:8080/path?query", null, handler1);
        connectFuture1.awaitUninterruptibly();
        assertTrue(connectFuture1.isConnected());

        IoHandler handler2 = new IoHandlerAdapter<IoSessionEx>();
        ConnectFuture connectFuture2 = connectorRule.connect("ws://localhost:8080/path?query", null, handler2);
        connectFuture2.awaitUninterruptibly();
        assertTrue(connectFuture2.isConnected());

        k3po.finish();
    }
}
