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
package org.kaazing.gateway.transport.wseb.specification.wse.acceptor;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertTrue;
import static org.kaazing.test.util.ITUtil.timeoutRule;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IoSession;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.test.Expectations;
import org.kaazing.gateway.transport.wseb.test.WsebAcceptorRule;
import org.kaazing.gateway.util.InternalSystemProperty;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.test.util.ITUtil;
import org.kaazing.test.util.MemoryAppender;
import org.kaazing.test.util.MethodExecutionTrace;

public class ClosingIT {

    private K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/wse/closing");

    private WsebAcceptorRule acceptor;

    {
        Properties configuration = new Properties();
        configuration.setProperty(InternalSystemProperty.WS_CLOSE_TIMEOUT.getPropertyName(), "2s");
        acceptor = new WsebAcceptorRule(configuration);
    }

    private JUnitRuleMockery context = new JUnitRuleMockery() {
        {
            setThreadingPolicy(new Synchroniser());
        }
    };

    private TestRule contextRule = ITUtil.toTestRule(context);
    private final TestRule trace = new MethodExecutionTrace();
    private final TestRule timeoutRule = timeoutRule(5, SECONDS);

    @Rule
    public TestRule chain = RuleChain.outerRule(trace).around(acceptor).around(contextRule).
             around(k3po).around(timeoutRule);

    @Test
    @Specification("client.send.close/request")
    public void shouldPerformClientInitiatedClose() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch closed = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(countDown(closed));
            }
        });

        acceptor.bind("wse://localhost:8080/path", handler);
        k3po.finish();
        assertTrue(closed.await(4, SECONDS));
    }

    // Client test only
    @Specification("client.send.close.no.reply.from.server/request")
    void clientShouldCloseIfServerDoesNotEchoCloseFrame() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification("client.abruptly.closes.downstream/request")
    public void clientAbruptlyClosesDownstream() throws Exception {
        final AtomicLong timeToClose = new AtomicLong(0);
        CountDownLatch closed = new CountDownLatch(1);
        acceptor.bind("wse://localhost:8080/path", new IoHandlerAdapter<IoSession>() {
            @Override
            protected void doSessionOpened(IoSession session) throws Exception {
                final long start = currentTimeMillis();
                session.getCloseFuture().addListener(new IoFutureListener<IoFuture>() {

                    @Override
                    public void operationComplete(IoFuture future) {
                        timeToClose.set(currentTimeMillis() - start);
                        closed.countDown();
                    }
                });
            }

        });
        k3po.finish();
        assertTrue("wsebSession was not closed after 4 seconds", closed.await(4, SECONDS));
        // Timing is not exact but should be close
        assertTrue(format("Closed should be immediate, but took %d millisecs, longer than ws close timeout of 2000 millisecs",
                timeToClose.get()),
                timeToClose.get() < 2000);
    }

    @Test
    @Specification("client.abruptly.closes.upstream/request")
    public void clientAbruptlyClosesUpstream() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch closed = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(IOException.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(countDown(closed));
            }
        });

        acceptor.bind("wse://localhost:8080/path", handler);
        k3po.finish();
        assertTrue("wsebSession was not closed after 4 seconds", closed.await(4, SECONDS));

        // Check no exceptions occurred (like "expected current thread... to match..." as in issue #427)
        // except for IOException which is expected because of client abrupt close
        final Set<String> EMPTY_STRING_SET = Collections.emptySet();
        MemoryAppender.assertMessagesLogged(EMPTY_STRING_SET, Collections.singletonList("[^O]Exception"), null, false);
    }

    // Client only test
    @Specification("server.abruptly.closes.downstream/request")
    void serverAbruptlyClosesDownstream() throws Exception {
        k3po.finish();
    }

    // Client only test
    @Specification("server.abruptly.closes.upstream/response")
    void serverAbruptlyClosesUpstream() throws Exception {
        k3po.finish();
    }


    @Test
    @Specification("server.send.close/request")
    public void shouldPerformServerInitiatedClose() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch closed = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                will(closeSession(0));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(countDown(closed));
            }
        });

        acceptor.bind("wse://localhost:8080/path", handler);
        k3po.finish();
        assertTrue(closed.await(4, SECONDS));
    }

    @Test
    @Specification("server.send.close.no.reply.from.client/request")
    public void serverShouldCloseIfClientDoesNotEchoCloseFrame() throws Exception {
        final AtomicLong timeToClose = new AtomicLong(0);
        CountDownLatch closed = new CountDownLatch(1);
        acceptor.bind("wse://localhost:8080/path", new IoHandlerAdapter<IoSession>() {
            @Override
            protected void doSessionOpened(IoSession session) throws Exception {
                final long start = currentTimeMillis();
                session.close(false).addListener(new IoFutureListener<IoFuture>() {

                    @Override
                    public void operationComplete(IoFuture future) {
                        timeToClose.set(currentTimeMillis() - start);
                        closed.countDown();
                    }
                });
            }

        });
        k3po.start();
        assertTrue("wsebSession was not closed after 4 seconds", closed.await(4, SECONDS));
        // Timing is not exact but should be close
        assertTrue(format("Time taken for ws close handshake %d should be close to ws close timeout of 2000 millisecs",
                timeToClose.get()),
                timeToClose.get() > 1500 && timeToClose.get() < 4000);
        k3po.finish();
    }

    // Client test only
    @Specification("server.send.data.after.close/request")
    void shouldIgnoreDataFromServerAfterCloseFrame() throws Exception {
        k3po.finish();
    }

    // Client test only
    @Specification("server.send.data.after.reconnect/request")
    void shouldIgnoreDataFromServerAfterReconnectFrame() throws Exception {
        k3po.finish();
    }
}
