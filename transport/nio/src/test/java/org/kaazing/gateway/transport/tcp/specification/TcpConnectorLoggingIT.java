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
package org.kaazing.gateway.transport.tcp.specification;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.session.IoSession;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.model.Statement;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.test.util.MemoryAppender;
import org.kaazing.test.util.MethodExecutionTrace;
import org.kaazing.test.util.ResolutionTestUtils;

/**
 * RFC-793
 */
@RunWith(Parameterized.class)
public class TcpConnectorLoggingIT {

    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/tcp/rfc793");

    private TcpConnectorRule connector = new TcpConnectorRule();

    private static String networkInterface = ResolutionTestUtils.getLoopbackInterface();

    private List<String> expectedPatterns;

    public TestRule trace = new MethodExecutionTrace();
    public TestRule timeoutRule = new DisableOnDebug(Timeout.builder().withTimeout(10, TimeUnit.SECONDS)
            .withLookingForStuckThread(true).build());

    private TestRule checkLogMessageRule = (base, description) -> new Statement() {
        @Override
        public void evaluate() throws Throwable {
            base.evaluate();
            MemoryAppender.assertMessagesLogged(expectedPatterns, null, null, true);
        }
    };

    @Rule
    public TestRule chain = RuleChain.outerRule(trace).around(checkLogMessageRule).around(connector).around(k3po).around(timeoutRule);


    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {"tcp://127.0.0.1:8080"}, {"tcp://[@" + networkInterface + "]:8080"}
        });
    }

    @Parameter
    public String uri;

    private void connectTo8080(IoHandlerAdapter<IoSessionEx> handler) throws InterruptedException {
        final String connectURIString = uri;
        ConnectFuture x = connector.connect(connectURIString, handler, null);
        x.await(1, SECONDS);
        Assert.assertTrue("Failed to connect, exception " + x.getException(), x.isConnected());
    }

    private void writeStringMessageToSession(String message, IoSession session) {
        ByteBuffer data = ByteBuffer.allocate(message.length());
        data.put(message.getBytes());

        data.flip();

        IoBufferAllocatorEx<?> allocator = ((IoSessionEx) session).getBufferAllocator();

        session.write(allocator.wrap(data.duplicate(), IoBufferEx.FLAG_SHARED)).addListener(new IoFutureListener<IoFuture>() {
            @Override
            public void operationComplete(IoFuture future) {
                throw new NullPointerException();
            }
        });
    }

    @Test
    @Specification({
            "echo.data/server"
    })
    public void exceptionMonitorShouldLogMessage() throws Exception {
        k3po.start();
        k3po.awaitBarrier("BOUND");
        connectTo8080(new IoHandlerAdapter<IoSessionEx>(){
            private int counter = 1;
            private DataMatcher dataMatch = new DataMatcher("server data " + counter);

            @Override
            protected void doSessionOpened(IoSessionEx session) throws Exception {
                writeStringMessageToSession("client data " + counter, session);
                InetSocketAddress socketAddress = (InetSocketAddress)session.getLocalAddress();
                expectedPatterns = Arrays.asList(String.format("\\[tcp#%d 127.0.0.1:%d\\] java.lang.NullPointerException", session.getId(), socketAddress.getPort()));
            }

            @Override
            protected void doMessageReceived(IoSessionEx session, Object message) throws Exception {
                String decoded = new String(((IoBuffer) message).array());

                if (dataMatch.addFragment(decoded) && counter < 2) {
                    counter++;
                    writeStringMessageToSession("client data " + counter, session);
                    dataMatch = new DataMatcher("server data " + counter);
                }
            }
        });
        k3po.finish();

    }

}
