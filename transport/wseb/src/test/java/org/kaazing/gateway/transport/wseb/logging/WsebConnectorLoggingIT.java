/**
 * Copyright 2007-2015, Kaazing Corporation. All rights reserved.
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
import static org.kaazing.test.util.ITUtil.timeoutRule;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandler;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.kaazing.gateway.transport.test.Expectations;
import org.kaazing.gateway.transport.wseb.WsebSession;
import org.kaazing.gateway.transport.wseb.test.WsebConnectorRule;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.test.util.ITUtil;
import org.kaazing.test.util.MemoryAppender;
import org.kaazing.test.util.MethodExecutionTrace;

public class WsebConnectorLoggingIT {
    private final WsebConnectorRule connector = new WsebConnectorRule();

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
        "data/binary/echo.payload.length.127/response" })
    public void shouldLogOpenWriteReceivedAndAbruptClose() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        final CountDownLatch received = new CountDownLatch(1);

        Random random = new Random();
        final byte[] bytes = new byte[127];
        random.nextBytes(bytes);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).messageReceived(with(any(IoSessionEx.class)), with(ioBufferMatching(bytes)));
                will(countDown(received));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/path?query", null, handler);
        connectFuture.awaitUninterruptibly();

        IoSessionEx connectSession = (IoSessionEx) connectFuture.getSession();

        IoBufferAllocatorEx<?> allocator = connectSession.getBufferAllocator();
        IoBufferEx buffer = allocator.wrap(ByteBuffer.wrap(bytes));
        connectSession.write(buffer);

        // This is a workaround for the fact that WsebConnector does not do close properly
        received.await(10, SECONDS);

        connectSession.close(false).await();

        k3po.finish();

        expectedPatterns = new ArrayList<String>(Arrays.asList(new String[] {
            "tcp#.* [^/]*:\\d*] OPENED",
            "tcp#.* [^/]*:\\d*] WRITE",
            "tcp#.* [^/]*:\\d*] RECEIVED",
            "tcp#.* [^/]*:\\d*] CLOSED",
            "http#.* [^/]*:\\d*] OPENED",
            "http#.* [^/]*:\\d*] WRITE",
            "http#.* [^/]*:\\d*] RECEIVED",
            "http#.* [^/]*:\\d*] CLOSED",
            "http#.* [^/]*:\\d*] EXCEPTION.*IOException",
            "wseb#.* [^/]*:\\d*] OPENED",
            "wseb#.* [^/]*:\\d*] WRITE",
            "wseb#.* [^/]*:\\d*] RECEIVED",
            "wseb#.* [^/]*:\\d*] CLOSED"
        }));

        forbiddenPatterns = null;
    }

    @Test
    @Specification({
        "closing/client.send.close/response"
        })
    @Ignore("gateway#345: WsebConnector does not write a close command on the writer when session is closed")
    public void shouldLogOpenAndCleanClientClose() throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
                oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
            }
        });

        ConnectFuture connectFuture = connector.connect("ws://localhost:8080/path?query", null, handler);

        WsebSession connectSession = (WsebSession) connectFuture.getSession();

        connectSession.close(false).await();
        k3po.finish();

        expectedPatterns = new ArrayList<String>(Arrays.asList(new String[] {
            "tcp#.* [^/]*:\\d*] OPENED",
            "tcp#.* [^/]*:\\d*] WRITE",
            "tcp#.* [^/]*:\\d*] RECEIVED",
            "tcp#.* [^/]*:\\d*] CLOSED",
            "http#.* [^/]*:\\d*] OPENED",
            "http#.* [^/]*:\\d*] WRITE",
            "http#.* [^/]*:\\d*] RECEIVED",
            "http#.* [^/]*:\\d*] CLOSED",
            "wseb#.* [^/]*:\\d*] OPENED",
            "wseb#.* [^/]*:\\d*] CLOSED"
        }));

        forbiddenPatterns = Arrays.asList("#.*EXCEPTION");
    }

}