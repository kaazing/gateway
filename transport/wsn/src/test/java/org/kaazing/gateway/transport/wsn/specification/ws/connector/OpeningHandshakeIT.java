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

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandler;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.kaazing.gateway.transport.test.Expectations;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.test.util.ITUtil;
import org.kaazing.test.util.MethodExecutionTrace;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.kaazing.test.util.ITUtil.timeoutRule;

public class OpeningHandshakeIT {
    private final WsnConnectorRule connector = new WsnConnectorRule();
    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/ws/opening");
    private final TestRule trace = new MethodExecutionTrace();
    private final TestRule timeoutRule = timeoutRule(10, SECONDS);

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
        "response.header.sec.websocket.accept.missing/handshake.response"
        })
    public void missingSecWebSocketAcceptHeader() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);

        context.checking(new Expectations() {
            {
                never(handler).sessionCreated(with(any(IoSessionEx.class)));
                never(handler).sessionOpened(with(any(IoSessionEx.class)));
                never(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
                never(handler).sessionClosed(with(any(IoSessionEx.class)));
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/path?query", null, handler);
        connectFuture.awaitUninterruptibly();
        assertFalse(connectFuture.isConnected());

        k3po.finish();
        context.assertIsSatisfied();

    }

    @Test
    @Specification({
        "connection.established/handshake.response"
        })
    public void shouldEstablishConnection() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                atMost(1).of(handler).sessionOpened(with(any(IoSessionEx.class)));
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/path?query", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification("response.header.upgrade.case.insensitive.websocket/handshake.response")
    public void shouldEstablishConnectionWithRandomCaseUpgradeRequestHeader() throws Exception {

        final IoHandler handler = context.mock(IoHandler.class);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                atMost(1).of(handler).sessionOpened(with(any(IoSessionEx.class)));
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/path?query", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification("response.header.connection.case.insensitive.upgrade/handshake.response")
    public void shouldEstablishConnectionWithRandomCaseConnectionRequestHeader() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                atMost(1).of(handler).sessionOpened(with(any(IoSessionEx.class)));
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/path?query", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());


        k3po.finish();
    }

    @Test
    @Specification("response.header.connection.case.insensitive.upgrade/handshake.response")
    public void shouldEstablishConnectionWithRandomCaseUpgradeResponseHeader() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                atMost(1).of(handler).sessionOpened(with(any(IoSessionEx.class)));
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/path?query", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());


        k3po.finish();
    }

    @Test
    @Specification("response.header.connection.case.insensitive.upgrade/handshake.response")
    public void shouldEstablishConnectionWithRandomCaseConnectionResponseHeader() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                atMost(1).of(handler).sessionOpened(with(any(IoSessionEx.class)));
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/path?query", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());


        k3po.finish();
    }

    @Test
    @Specification({
        "request.header.origin/handshake.response"
        })
    public void shouldEstablishConnectionWithRequestHeaderOrigin() throws Exception {

        final IoHandler handler = context.mock(IoHandler.class);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                atMost(1).of(handler).sessionOpened(with(any(IoSessionEx.class)));
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/path?query", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        Thread.sleep(100);

        k3po.finish();
    }

    @Test
    @Ignore("K3po issue# 53: Simplify matching syntax for comma-separated list HTTP headers; currently has comma separated which is not support in K3po yet")
    @Specification({
        "request.header.sec.websocket.protocol/handshake.response"
        })
    public void shouldEstablishConnectionWithRequestHeaderSecWebSocketProtocol() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                atMost(1).of(handler).sessionOpened(with(any(IoSessionEx.class)));
            }
        });

        String [] protocols = {"primary", " secondary"}; // Need the space to honor the script.
        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/path?query", protocols, null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({
        "response.header.connection.not.upgrade/handshake.response" })
    public void shouldFailConnectionWhenResponseHeaderConnectionNotUpgrade() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);

        context.checking(new Expectations() {
            {
                never(handler).sessionCreated(with(any(IoSessionEx.class)));
                never(handler).sessionOpened(with(any(IoSessionEx.class)));
                never(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
                never(handler).sessionClosed(with(any(IoSessionEx.class)));
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/path?query", null, handler);
        connectFuture.awaitUninterruptibly();
        assertFalse(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({"response.header.connection.missing/handshake.response"})
    public void shouldFailConnectionWhenResponseHeaderConnectionMissing() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);

        context.checking(new Expectations() {
            {
                never(handler).sessionCreated(with(any(IoSessionEx.class)));
                never(handler).sessionOpened(with(any(IoSessionEx.class)));
                never(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
                never(handler).sessionClosed(with(any(IoSessionEx.class)));
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/path?query", null, handler);
        connectFuture.awaitUninterruptibly();
        assertFalse(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({
        "response.header.upgrade.not.websocket/handshake.response" })
    public void shouldFailConnectionWhenResponseHeaderUpgradeNotWebSocket() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);

        context.checking(new Expectations() {
            {
                never(handler).sessionCreated(with(any(IoSessionEx.class)));
                never(handler).sessionOpened(with(any(IoSessionEx.class)));
                never(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
                never(handler).sessionClosed(with(any(IoSessionEx.class)));
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/path?query", null, handler);
        connectFuture.awaitUninterruptibly();
        assertFalse(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification("response.header.upgrade.missing/handshake.response")
    public void shouldFailConnectionWhenResponseHeaderUpgradeMissing() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);

        context.checking(new Expectations() {
            {
                never(handler).sessionCreated(with(any(IoSessionEx.class)));
                never(handler).sessionOpened(with(any(IoSessionEx.class)));
                never(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
                never(handler).sessionClosed(with(any(IoSessionEx.class)));
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/path?query", null, handler);
        connectFuture.awaitUninterruptibly();
        assertFalse(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Ignore("Did not fail and should -- I think the script is hashing Sec-WebSocket-Accept. Need clarification.")
    @Specification({
        "response.header.sec.websocket.accept.not.hashed/handshake.response" })
    public void shouldFailConnectionWhenResponseHeaderSecWebSocketAcceptNotHashed() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);

        context.checking(new Expectations() {
            {
                never(handler).sessionCreated(with(any(IoSessionEx.class)));
                never(handler).sessionOpened(with(any(IoSessionEx.class)));
                never(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
                never(handler).sessionClosed(with(any(IoSessionEx.class)));
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/path?query", null, handler);
        connectFuture.awaitUninterruptibly();
        assertFalse(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({
        "response.header.sec.websocket.accept.missing/handshake.response" })
    public void shouldFailConnectionWhenResponseHeaderSecWebSocketAcceptMissing() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);

        context.checking(new Expectations() {
            {
                never(handler).sessionCreated(with(any(IoSessionEx.class)));
                never(handler).sessionOpened(with(any(IoSessionEx.class)));
                never(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
                never(handler).sessionClosed(with(any(IoSessionEx.class)));
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/path?query", null, handler);
        connectFuture.awaitUninterruptibly();
        assertFalse(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Ignore("Issue# 315: connectFuture.isConnected() must return false as the negotiated protocol does not match any"
            + "of the supported protocols")
    @Specification({
        "response.header.sec.websocket.protocol.not.negotiated/handshake.response" })
    public void shouldFailConnectionWhenResponseHeaderSecWebSocketProtocolNotNegotiated() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);

        context.checking(new Expectations() {
            {
                never(handler).sessionCreated(with(any(IoSessionEx.class)));
                never(handler).sessionOpened(with(any(IoSessionEx.class)));
                never(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
                never(handler).sessionClosed(with(any(IoSessionEx.class)));
            }
        });

        String [] protocols = {"primary", " secondary"}; // Need the space to honor the script.
        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/path?query", protocols, null, handler);
        connectFuture.awaitUninterruptibly();
        assertFalse(connectFuture.isConnected());

        k3po.finish();
    }

    @Test
    @Specification({
        "multiple.connections.established/handshake.responses"
        })
    public void shouldEstablishMultipleConnections() throws Exception {
        final IoHandler handler1 = context.mock(IoHandler.class, "handler1");
        final IoHandler handler2 = context.mock(IoHandler.class, "handler2");

        context.checking(new Expectations() {
            {
                oneOf(handler1).sessionCreated(with(any(IoSessionEx.class)));
                atMost(1).of(handler1).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler2).sessionCreated(with(any(IoSessionEx.class)));
                atMost(1).of(handler2).sessionOpened(with(any(IoSessionEx.class)));
            }
        });

        ConnectFuture connectFuture1 = connector.connect("ws://localhost:8080/path?query", null, handler1);
        connectFuture1.awaitUninterruptibly();
        assertTrue(connectFuture1.isConnected());

        ConnectFuture connectFuture2 = connector.connect("ws://localhost:8080/path?query", null, handler2);
        connectFuture2.awaitUninterruptibly();
        assertTrue(connectFuture2.isConnected());

        k3po.finish();
    }
}
