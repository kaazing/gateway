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

import java.net.URI;
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

public class ArchitectureIT {

    private final HttpConnectorRule connector = new HttpConnectorRule();

    private JUnitRuleMockery context = new JUnitRuleMockery() {
        {
            setThreadingPolicy(new Synchroniser());
        }
    };

    private final TestRule trace = new MethodExecutionTrace();
    private TestRule contextRule = ITUtil.toTestRule(context);
    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/http/rfc7230/architecture");
    private final TestRule timeoutRule = new DisableOnDebug(new Timeout(5, SECONDS));

    @Rule
    public TestRule chain = RuleChain.outerRule(trace).around(connector).around(contextRule).around(k3po).around(timeoutRule);

    @Test
    @Specification({"outbound.must.send.version/response"})
    public void outboundMustSendVersion() throws Exception {
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
        assertTrue(closed.await(2, SECONDS));

        k3po.finish();
    }

    @Test
    @Specification({"inbound.must.send.version/response"})
    public void inboundMustSendVersion() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch closed = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        closed.countDown();
                        return null;
                    }
                });
            }
        });

        connector.connect("http://localhost:8080/", handler, new ConnectSessionInitializerGet2());
        assertTrue(closed.await(2, SECONDS));

        k3po.finish();
    }

    @Test
    @Specification({"response.must.be.400.on.invalid.version/response"})
    public void inboundMustSend400OnInvalidVersion() throws Exception {
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
        assertTrue(closed.await(2, SECONDS));

        k3po.finish();
    }

    @Test
    @Specification({"inbound.must.reply.with.version.one.dot.one.when.received.higher.minor.version/response"})
    public void inboundMustReplyWithVersionOneDotOneWhenReceivedHigherMinorVersion() throws Exception {
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
        assertTrue(closed.await(2, SECONDS));

        k3po.finish();
    }

    @Test
    @Specification({"origin.server.should.send.505.on.major.version.not.equal.to.one/response"})
    public void originServerShouldSend505OnMajorVersionNotEqualToOne() throws Exception {
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
        assertTrue(closed.await(2, SECONDS));

        k3po.finish();
    }

    @Test
    @Specification({"client.must.send.host.identifier/response"})
    public void clientMustSendHostIdentifier() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch closed = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        closed.countDown();
                        return null;
                    }
                });
            }
        });

        connector.connect("http://localhost:8080/", handler, new ConnectSessionInitializerGetHost());
        assertTrue(closed.await(2, SECONDS));

        k3po.finish();
    }

    @Test
    @Specification({"inbound.must.reject.requests.missing.host.identifier/response"})
    public void inboundMustRejectRequestsMissingHostIdentifier() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch closed = new CountDownLatch(1);

        connector.getConnectOptions().put("http.hostHeaderEnabled", Boolean.FALSE);
        connector.getConnectOptions().put("http.userAgentHeaderEnabled", Boolean.FALSE);

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
        assertTrue(closed.await(2, SECONDS));

        k3po.finish();
    }

    @Test
    @Specification({"inbound.must.reject.requests.with.user.info.on.uri/response"})
    public void inboundMustRejectRequestWithUserInfoOnURI() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch closed = new CountDownLatch(1);

        connector.getConnectOptions().put("http.hostHeaderEnabled", Boolean.FALSE);
        connector.getConnectOptions().put("http.userAgentHeaderEnabled", Boolean.FALSE);

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

        connector.connect("http://localhost:8080/", handler, new ConnectSessionInitializerErroredURI());
        assertTrue(closed.await(2, SECONDS));

        k3po.finish();
    }

    private static class ConnectSessionInitializerErroredURI implements IoSessionInitializer<ConnectFuture> {
        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            HttpConnectSession connectSession = (HttpConnectSession) session;
            connectSession.setRequestURI(URI.create("http://username:password@localhost:8000/"));
            connectSession.setMethod(HttpMethod.GET);
            connectSession.addWriteHeader(HttpHeaders.HEADER_HOST, "localhost:8000");
        }
    }

    @Test
    @Specification({"inbound.should.allow.requests.with.percent.chars.in.uri/response"})
    public void inboundShouldAllowRequestsWithPercentCharsInURI() throws Exception {
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

        connector.connect("http://localhost:8080/Some%20Path", handler, new ConnectSessionInitializerGet());
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

    private static class ConnectSessionInitializerGet2 implements IoSessionInitializer<ConnectFuture> {
        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            HttpConnectSession connectSession = (HttpConnectSession) session;
            connectSession.setMethod(HttpMethod.GET);
            connectSession.close(false);
        }
    }

    private static class ConnectSessionInitializerGetHost implements IoSessionInitializer<ConnectFuture> {
        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            HttpConnectSession connectSession = (HttpConnectSession) session;
            connectSession.setMethod(HttpMethod.GET);
            connectSession.close(false);
        }
    }

}
