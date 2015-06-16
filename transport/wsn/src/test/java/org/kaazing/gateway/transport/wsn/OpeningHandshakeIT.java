/**
 * Copyright (c) 2007-2014, Kaazing Corporation. All rights reserved.
 */

package org.kaazing.gateway.transport.wsn;

import org.apache.log4j.PropertyConfigurator;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandler;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.mina.core.session.IoSessionEx;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.rules.RuleChain.outerRule;


public class OpeningHandshakeIT {

    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/ws/opening");

    private final TestRule timeout = new DisableOnDebug(new Timeout(5, SECONDS));

    private final WsnConnectorRule connectorRule = new WsnConnectorRule();

    @Rule
    public final TestRule chain = outerRule(connectorRule).around(k3po).around(timeout);


    @BeforeClass
    public static void initLogging() throws Exception {
        PropertyConfigurator.configure("src/test/resources/log4j-trace.properties");
    }

    @Test
    @Specification({"response.header.sec.websocket.accept.missing/handshake.response"})
    public void missingSecWebSocketAcceptHeader() throws Exception {
        IoHandler handler = new IoHandlerAdapter<IoSessionEx>() {};
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/path?query", null, handler);
        assertFalse(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({"connection.established/handshake.response"})
    public void shouldEstablishConnection() throws Exception {
        IoHandler handler = new IoHandlerAdapter<IoSessionEx>() {};
        ConnectFuture connectFuture = connectorRule.connect("ws://localhost:8080/path?query", null, handler);
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

}
