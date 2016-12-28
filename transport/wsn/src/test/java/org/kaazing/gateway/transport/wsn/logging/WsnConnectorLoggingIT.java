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
package org.kaazing.gateway.transport.wsn.logging;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertTrue;
import static org.kaazing.test.util.ITUtil.timeoutRule;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandler;
import org.jmock.Expectations;
import org.jmock.api.Invocation;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.jmock.lib.action.CustomAction;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.kaazing.gateway.transport.ws.bridge.filter.WsBuffer;
import org.kaazing.gateway.transport.wsn.WsnProtocol;
import org.kaazing.gateway.transport.wsn.WsnSession;
import org.kaazing.gateway.transport.wsn.specification.ws.connector.WsnConnectorRule;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.test.util.ITUtil;
import org.kaazing.test.util.MemoryAppender;
import org.kaazing.test.util.MethodExecutionTrace;

// This is a subset of BaseFramingIT (connector version) used to verify wsn transport level logging
public class WsnConnectorLoggingIT {
    private static final String TEXT_FILTER_NAME = WsnProtocol.NAME + "#text";
    private final WsnConnectorRule connector = new WsnConnectorRule();
    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/ws");
    private List<String> expectedPatterns;
    private List<String> forbiddenPatterns;

    private TestRule checkLogMessageRule = new TestRule() {
        @Override
        public Statement apply(final Statement base, Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    base.evaluate();
                    MemoryAppender.assertMessagesLogged(expectedPatterns,
                            forbiddenPatterns, ".*\\[.*#.*].*", true);
                }
            };
        }
    };

    private JUnitRuleMockery context = new JUnitRuleMockery() {
        {
            setThreadingPolicy(new Synchroniser());
        }
    };

    private TestRule contextRule = ITUtil.toTestRule(context);

    @Rule
    // Special ordering: connector around k3po allows connector to detect k3po closing any still open connections
    // to make sure we get the log messages for the abrupt close. Context rule allows jmock context checking to
    // be done last to ensure all events have occurred (especially session closed).
    public final TestRule chain = RuleChain.outerRule(new MethodExecutionTrace()).around(checkLogMessageRule)
            .around(contextRule).around(connector).around(k3po).around(timeoutRule(5, SECONDS));

    @Test
    @Specification({
        "extensibility/server.send.text.frame.with.rsv.1/handshake.response.and.frame"
        })
    public void shouldLogProtocolException() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();
        assertTrue(connectFuture.isConnected());

        k3po.finish();

        expectedPatterns = Arrays.asList(
                "tcp#.*OPENED",
                "tcp#.*WRITE",
                "tcp#.*RECEIVED",
                "tcp#.*CLOSED",
                "http#.*OPENED",
                "http#.*CLOSED",
                "wsn#.*OPENED",
                "tcp#.*EXCEPTION.*Protocol.*Exception",
                "wsn#.*EXCEPTION.*IOException.*caused by.*Protocol.*Exception",
                "wsn#.*CLOSED"
        );

        forbiddenPatterns = null;
    }

    @Test
    @Specification({
        "framing/echo.binary.payload.length.125/handshake.response.and.frame"
        })
    public void shouldLogOpenWriteReceivedAndAbruptClose() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch received = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).messageReceived(with(any(IoSessionEx.class)), with(any(Object.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        received.countDown();
                        return null;
                    }
                });
                allowing(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
                allowing(handler).sessionClosed(with(any(IoSessionEx.class)));
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.awaitUninterruptibly();

        WsnSession wsnConnectSession = (WsnSession) connectFuture.getSession();
        // ### Issue# 316: Temporary hack till the issue related to Connector writing out TEXT frame
        //                  instead of BINARY is resolved.
        if (wsnConnectSession != null) {
            IoFilterChain parentFilterChain = wsnConnectSession.getParent().getFilterChain();
            if (parentFilterChain.contains(TEXT_FILTER_NAME)) {
                parentFilterChain.remove(TEXT_FILTER_NAME);
            }
        }

        Random random = new Random();
        byte[] bytes = new byte[125];
        random.nextBytes(bytes);

        IoBufferAllocatorEx<? extends WsBuffer> allocator = wsnConnectSession.getBufferAllocator();
        WsBuffer wsBuffer = allocator.wrap(ByteBuffer.wrap(bytes), IoBufferEx.FLAG_SHARED);
        wsBuffer.setKind(WsBuffer.Kind.BINARY);
        wsnConnectSession.write(wsBuffer);
        assertTrue(received.await(10, SECONDS));

        k3po.finish();

        expectedPatterns = Arrays.asList(
            "tcp#.*OPENED",
            "tcp#.*WRITE",
            "tcp#.*RECEIVED",
            "tcp#.*CLOSED",
            "http#.*OPENED",
            "http#.*CLOSED",
            "wsn#.*OPENED",
            "wsn#.*WRITE",
            "wsn#.*RECEIVED",
            "wsn#.*EXCEPTION", // because the script does not complete the WebSocket close handshake
            "wsn#.*CLOSED"
        );

        forbiddenPatterns = null;
    }

    @Test
    @Specification({
        "closing/client.send.close.frame.with.code.1000/handshake.response.and.frame" })
    public void shouldLogOpenAndCleanClose() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch close = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(new CustomAction("Latch countdown") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        close.countDown();
                        return null;
                    }
                });
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/echo", null, handler);
        connectFuture.await(10, SECONDS);
        connectFuture.getSession().close(false);

        k3po.finish();
        assertTrue(close.await(10, SECONDS));

        expectedPatterns = Arrays.asList(
            "tcp#.* [^/]*:\\d*] OPENED",
            "tcp#.* [^/]*:\\d*] WRITE",
            "tcp#.* [^/]*:\\d*] RECEIVED",
            "tcp#.* [^/]*:\\d*] CLOSED",
            "http#.* [^/]*:\\d*] OPENED",
            "http#.* [^/]*:\\d*] CLOSED",
            "wsn#.* [^/]*:\\d*] OPENED",
            "wsn#.* [^/]*:\\d*] CLOSED"
        );

        forbiddenPatterns = Collections.singletonList("#.*EXCEPTION");
    }

}