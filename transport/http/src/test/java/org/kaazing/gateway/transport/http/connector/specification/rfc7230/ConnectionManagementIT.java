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
package org.kaazing.gateway.transport.http.connector.specification.rfc7230;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;
import org.jmock.Expectations;
import org.jmock.api.Invocation;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.jmock.lib.action.CustomAction;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.gateway.transport.http.HttpConnectSession;
import org.kaazing.gateway.transport.http.HttpConnectorRule;
import org.kaazing.gateway.transport.http.HttpHeaders;
import org.kaazing.gateway.transport.http.HttpMethod;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.test.util.ITUtil;
import org.kaazing.test.util.MethodExecutionTrace;

public class ConnectionManagementIT {

    private final HttpConnectorRule connector = new HttpConnectorRule();

    private JUnitRuleMockery context = new JUnitRuleMockery() {
        {
            setThreadingPolicy(new Synchroniser());
        }
    };

    private final TestRule trace = new MethodExecutionTrace();
    private TestRule contextRule = ITUtil.toTestRule(context);
    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/http/rfc7230/connection.management");
    private final TestRule timeoutRule = new DisableOnDebug(new Timeout(5, SECONDS));

    @Rule
    public TestRule chain = RuleChain.outerRule(trace).around(connector).around(contextRule).around(k3po).around(timeoutRule);

    @Ignore("Unwanted User-Agent header")
    @Test
    @Specification({"client.must.close.connection.after.request.with.connection.close/response"})
    public void clientMustCloseConnectionAfterRequestWithConnectionClose() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch closed = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        closed.countDown();
                        return null;
                    }
                });
            }
        });

        ConnectFuture connectFuture = connector.connect("http://localhost:8080/", handler, new ConnectSessionInitializerGet());
        connectFuture.getSession();
        assertTrue(closed.await(2, SECONDS));

        k3po.finish();
    }

    @Ignore("Unwanted User-Agent header")
    @Test
    @Specification({"server.must.close.connection.after.response.with.connection.close/response"})
    public void serverMustCloseConnectionAfterResponseWithConnectionClose() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch closed = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        closed.countDown();
                        return null;
                    }
                });
            }
        });

        ConnectFuture connectFuture = connector.connect("http://localhost:8080/", handler, new ConnectSessionInitializerGet());
        connectFuture.getSession();
        assertTrue(closed.await(2, SECONDS));

        k3po.finish();
    }

    @Ignore("assertion error - 2 connections work but 3 don't")
    @Test
    @Specification({"connections.should.persist.by.default/backend"})
    public void connectionsShouldPersistByDefault() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch closed = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        closed.countDown();
                        return null;
                    }
                });
            }
        });

        connector.connect("http://localhost:8080/", handler, new ConnectSessionInitializerGet());

        connector.connect("http://localhost:8080/", handler, new ConnectSessionInitializerGet());

        connector.connect("http://localhost:8080/", handler, new ConnectSessionInitializerGet());

        assertTrue(closed.await(4, SECONDS));

        k3po.finish();
    }

    @Ignore("Assertion error")
    @Test
    @Specification({"server.should.accept.http.pipelining/response"})
    public void serverShouldAcceptHttpPipelining() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch closed = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        closed.countDown();
                        return null;
                    }
                });
            }
        });

        ConnectFuture connectFuture =
                connector.connect("http://localhost:8080/request1", handler, new ConnectSessionInitializerGet());
        connectFuture.getSession();

        connectFuture = connector.connect("http://localhost:8080/request2", handler, new ConnectSessionInitializerGet());
        connectFuture.getSession();

        connectFuture = connector.connect("http://localhost:8080/request3", handler, new ConnectSessionInitializerGet());
        connectFuture.getSession();

        assertTrue(closed.await(4, SECONDS));

        k3po.finish();
    }

    @Ignore("Assertion Error")
    @Test
    @Specification({"client.with.pipelining.must.not.retry.pipelining.immediately.after.failure/response"})
    public void clientWithPipeliningMustNotRetryPipeliningImmediatelyAfterFailure() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch closed = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        closed.countDown();
                        return null;
                    }
                });
            }
        });

        connector.connect("http://localhost:8080/request1", handler, new ConnectSessionInitializerGet());
        connector.connect("http://localhost:8080/request2", handler, new ConnectSessionInitializerGet());
        connector.connect("http://localhost:8080/request2", handler, new ConnectSessionInitializerGet());
        assertTrue(closed.await(2, SECONDS));

        k3po.finish();
    }

    @Test
    @Specification({"server.must.close.its.half.of.connection.after.sending.response.if.it.receives.a.close/response"})
    public void serverMustCloseItsHalfOfConnectionAfterSendingResponseIfItReceivesAClose() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch closed = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        closed.countDown();
                        return null;
                    }
                });
            }
        });

        ConnectFuture connectFuture =
                connector.connect("http://localhost:8080/path", handler, new ConnectSessionInitializerGetConnection());
        connectFuture.getSession();
        assertTrue(closed.await(2, SECONDS));

        k3po.finish();
    }

    @Test
    @Specification({"client.must.not.reuse.tcp.connection.when.receives.connection.close/response"})
    public void clientMustNotReuseTcpConnectionWhenReceivesConnectionClose() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch closed = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        closed.countDown();
                        return null;
                    }
                });
            }
        });

        connector.connect("http://localhost:8080/path1", handler, new ConnectSessionInitializerGet());
        connector.connect("http://localhost:8080/path2", handler, new ConnectSessionInitializerGet());
        assertTrue(closed.await(2, SECONDS));

        k3po.finish();
    }

    @Ignore()
    @Test
    @Specification({"server.getting.upgrade.request.must.respond.with.upgrade.header/response"})
    public void serverGettingUpgradeRequestMustRespondWithUpgradeHeader() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch closed = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        closed.countDown();
                        return null;
                    }
                });
            }
        });

        ConnectFuture connectFuture = connector.connect("http://localhost:8080/", handler, new ConnectSessionInitializerGetUpgrade());
        connectFuture.getSession();
        assertTrue(closed.await(2, SECONDS));

        k3po.finish();
    }

    @Test
    @Specification({"server.that.sends.upgrade.required.must.include.upgrade.header/response"})
    public void serverThatSendsUpgradeRequiredMustIncludeUpgradeHeader() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch closed = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        closed.countDown();
                        return null;
                    }
                });
            }
        });

        ConnectFuture connectFuture = connector.connect("http://localhost:8080/", handler, new ConnectSessionInitializerGet());
        connectFuture.getSession();
        assertTrue(closed.await(2, SECONDS));

        k3po.finish();
    }

    @Ignore("Not upgrading")
    @Test
    @Specification({"server.that.is.upgrading.must.send.a.101.response/response"})
    public void serverThatIsUpgradingMustSendA100Response() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch closed = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        closed.countDown();
                        return null;
                    }
                });
            }
        });

        ConnectFuture connectFuture = connector.connect("http://localhost:8080/", handler, new ConnectSessionInitializerGet());
        connectFuture.getSession();
        assertTrue(closed.await(2, SECONDS));

        k3po.finish();
    }

    private static class ConnectSessionInitializerGet implements IoSessionInitializer<ConnectFuture> {
        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            HttpConnectSession connectSession = (HttpConnectSession) session;
            connectSession.setMethod(HttpMethod.GET);
        }
    }

    private static class ConnectSessionInitializerGetConnection implements IoSessionInitializer<ConnectFuture> {
        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            HttpConnectSession connectSession = (HttpConnectSession) session;
            connectSession.setMethod(HttpMethod.GET);
            connectSession.addWriteHeader(HttpHeaders.HEADER_CONNECTION, String.valueOf("close"));
        }
    }
    
    private static class ConnectSessionInitializerGetUpgrade implements IoSessionInitializer<ConnectFuture> {
        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            HttpConnectSession connectSession = (HttpConnectSession) session;
            connectSession.setMethod(HttpMethod.GET);
            connectSession.addWriteHeader(HttpHeaders.HEADER_UPGRADE, "WebSocket");
            connectSession.addWriteHeader(HttpHeaders.HEADER_CONNECTION, String.valueOf("upgrade"));
        }
    }

}
