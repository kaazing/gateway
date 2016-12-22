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
package org.kaazing.gateway.transport.wseb.logging;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertTrue;
import static org.kaazing.test.util.ITUtil.timeoutRule;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandler;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.kaazing.gateway.transport.test.Expectations;
import org.kaazing.gateway.transport.wseb.WsebSession;
import org.kaazing.gateway.transport.wseb.test.WsebConnectorRule;
import org.kaazing.gateway.util.InternalSystemProperty;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.test.util.ITUtil;
import org.kaazing.test.util.MemoryAppender;
import org.kaazing.test.util.MethodExecutionTrace;

public class WsebConnectorLoggingIT {
    private final WsebConnectorRule connector;

    {
        Properties configuration = new Properties();
        configuration.setProperty(InternalSystemProperty.WS_CLOSE_TIMEOUT.getPropertyName(), "2s");
        connector = new WsebConnectorRule(configuration);
    }

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery() {
        {
            setThreadingPolicy(new Synchroniser());
        }
    };

    private final K3poRule k3po = new K3poRule()
            .setScriptRoot("org/kaazing/specification/wse");

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

    private TestRule contextRule = ITUtil.toTestRule(context);

    @Rule
    // Special ordering: connector around k3po allows connector to detect k3po closing any still open connections
    // to make sure we get the log messages for the abrupt close. Context rule allows jmock context checking to
    // be done last to ensure all events have occurred (especially session closed).
    public final TestRule chain = RuleChain.outerRule(new MethodExecutionTrace()).around(checkLogMessageRule)
            .around(contextRule).around(connector).around(k3po).around(timeoutRule(5, SECONDS));

    @Test
    @Specification({
        "control/server.send.invalid.ping/response" })
    public void shouldLogProtocolException() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch closed = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(countDown(closed));
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/path?query", null, handler);
        connectFuture.awaitUninterruptibly();

        // Must wait for closed otherwise context.assertIsSatisfied my fire before it
        assertTrue("Closed event was not fired", closed.await(4, SECONDS));
        assertTrue("Closed future was not fulfilled", connectFuture.getSession().getCloseFuture().isClosed());
        k3po.finish();


        expectedPatterns = Arrays.asList(
            "tcp#.*OPENED",
            "tcp#.*WRITE",
            "tcp#.*RECEIVED",
            "tcp#.*CLOSED",
            "http#.*OPENED",
            "http#.*WRITE",
            "http#.*RECEIVED",
            "http#.*CLOSED",
            "http#.*EXCEPTION.*ProtocolDecoderException",
            "wseb#.*OPENED",
            "wseb#.*WRITE",
            "wseb#.*EXCEPTION.*IOException",
            "wseb#.*CLOSED"
        );

        forbiddenPatterns = null;
    }

    @Test
    @Specification({
        "data/echo.binary.payload.length.127/response" })
    public void shouldLogOpenWriteReceivedAndCloseHandshakeTimedOut() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch closed = new CountDownLatch(1);

        Random random = new Random();
        final byte[] bytes = new byte[127];
        random.nextBytes(bytes);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).messageReceived(with(any(IoSessionEx.class)), with(ioBufferMatching(bytes)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
                will(countDown(closed));
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/path?query", null, handler);

        IoSessionEx connectSession = (IoSessionEx) connectFuture.getSession();

        IoBufferAllocatorEx<?> allocator = connectSession.getBufferAllocator();
        IoBufferEx buffer = allocator.wrap(ByteBuffer.wrap(bytes));
        connectSession.write(buffer);

        connectSession.close(false);
        assertTrue("connectSession did not close", closed.await(10, SECONDS));

        k3po.finish();

        expectedPatterns = Arrays.asList(
             "tcp#.* [^/]*:\\d*] OPENED",
             "tcp#.* [^/]*:\\d*] WRITE",
             "tcp#.* [^/]*:\\d*] RECEIVED",
             "tcp#.* [^/]*:\\d*] CLOSED",
             "http#[^wseb#]*wseb#[^ ]* [^/]*:\\d*] OPENED",
             "http#[^wseb#]*wseb#[^ ]* [^/]*:\\d*] WRITE",
             "http#[^wseb#]*wseb#[^ ]* [^/]*:\\d*] RECEIVED",
             "http#[^wseb#]*wseb#[^ ]* [^/]*:\\d*] CLOSED",
             "http#.* [^/]*:\\d*] OPENED",
             "http#.* [^/]*:\\d*] WRITE",
             "http#.* [^/]*:\\d*] RECEIVED",
             "http#.* [^/]*:\\d*] CLOSED",
             "wseb#.* [^/]*:\\d*] OPENED",
             "wseb#.* [^/]*:\\d*] WRITE",
             "wseb#.* [^/]*:\\d*] RECEIVED",
             "wseb#.* [^/]*:\\d*] CLOSED"
        );

        forbiddenPatterns = null;
    }

    @Test
    @Specification("closing/client.send.close/response")
    public void shouldLogOpenAndCleanClientClose() throws Exception {
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

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/path?query", null, handler);
        WsebSession connectSession = (WsebSession) connectFuture.getSession();
        connectSession.close(false);
        assertTrue("connectSession did not close", closed.await(10, SECONDS));
        k3po.finish();

        expectedPatterns = Arrays.asList(
            "tcp#.* [^/]*:\\d*] OPENED",
            "tcp#.* [^/]*:\\d*] WRITE",
            "tcp#.* [^/]*:\\d*] RECEIVED",
            "tcp#.* [^/]*:\\d*] CLOSED",
            "http#[^wseb#]*wseb#[^ ]* [^/]*:\\d*] OPENED",
            "http#[^wseb#]*wseb#[^ ]* [^/]*:\\d*] WRITE",
            "http#[^wseb#]*wseb#[^ ]* [^/]*:\\d*] RECEIVED",
            "http#[^wseb#]*wseb#[^ ]* [^/]*:\\d*] CLOSED",
            "http#.* [^/]*:\\d*] OPENED",
            "http#.* [^/]*:\\d*] WRITE",
            "http#.* [^/]*:\\d*] RECEIVED",
            "http#.* [^/]*:\\d*] CLOSED",
            "wseb#.* [^/]*:\\d*] OPENED",
            "wseb#.* [^/]*:\\d*] CLOSED"
        );

        forbiddenPatterns = Collections.singletonList("#.*EXCEPTION");
    }

    @Test
    @Specification("closing/server.send.close/response")
    public void shouldLogOpenAndCleanServerClose() throws Exception {
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

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/path?query", null, handler);
        WsebSession connectSession = (WsebSession) connectFuture.getSession();
        connectSession.close(false);
        assertTrue("connectSession did not close", closed.await(10, SECONDS));
        k3po.finish();

        expectedPatterns = Arrays.asList(
            "tcp#.* [^/]*:\\d*] OPENED",
            "tcp#.* [^/]*:\\d*] WRITE",
            "tcp#.* [^/]*:\\d*] RECEIVED",
            "tcp#.* [^/]*:\\d*] CLOSED",
            "http#[^wseb#]*wseb#[^ ]* [^/]*:\\d*] OPENED",
            "http#[^wseb#]*wseb#[^ ]* [^/]*:\\d*] WRITE",
            "http#[^wseb#]*wseb#[^ ]* [^/]*:\\d*] RECEIVED",
            "http#[^wseb#]*wseb#[^ ]* [^/]*:\\d*] CLOSED",
            "http#.* [^/]*:\\d*] OPENED",
            "http#.* [^/]*:\\d*] WRITE",
            "http#.* [^/]*:\\d*] RECEIVED",
            "http#.* [^/]*:\\d*] CLOSED",
            "wseb#.* [^/]*:\\d*] OPENED",
            "wseb#.* [^/]*:\\d*] CLOSED"
        );

        forbiddenPatterns = Collections.singletonList("#.*EXCEPTION");
    }

}