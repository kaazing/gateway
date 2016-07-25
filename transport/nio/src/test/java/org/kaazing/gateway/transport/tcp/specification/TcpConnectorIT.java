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
import static org.kaazing.test.util.ITUtil.createRuleChain;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.session.IoSession;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.test.util.ResolutionTestUtils;

/**
 * RFC-793
 */
@RunWith(Parameterized.class)
public class TcpConnectorIT {

    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/tcp/rfc793");

    private TcpConnectorRule connector = new TcpConnectorRule();

    private static String networkInterface = ResolutionTestUtils.getLoopbackInterface();

    @Rule
    public TestRule chain = createRuleChain(connector, k3po);

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

        session.write(allocator.wrap(data.duplicate(), IoBufferEx.FLAG_SHARED));
    }

    @Ignore("https://github.com/kaazing/tickets/issues/538")
    @Test
    @Specification({
        "establish.connection/server"
        })
    public void shouldEstablishConnection() throws Exception {
        k3po.start();
        k3po.awaitBarrier("BOUND");
        connectTo8080(new IoHandlerAdapter<>());
        k3po.finish();
    }

    @Test
    @Specification({
        "server.sent.data/server"
        })
    public void shouldReceiveServerSentData() throws Exception {
        k3po.start();
        k3po.awaitBarrier("BOUND");
        connectTo8080(new IoHandlerAdapter<>());

        k3po.finish();
    }

    @Test
    @Specification({
        "client.sent.data/server"
        })
    public void shouldReceiveClientSentData() throws Exception {
        k3po.start();
        k3po.awaitBarrier("BOUND");
        connectTo8080(new IoHandlerAdapter<IoSessionEx>(){
            @Override
            protected void doSessionOpened(IoSessionEx session) throws Exception {
                ByteBuffer data = ByteBuffer.allocate(20);
                String str = "client data";
                data.put(str.getBytes());

                data.flip();

                IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();

                session.write(allocator.wrap(data.duplicate(), IoBufferEx.FLAG_SHARED));
            }
        });

        k3po.finish();
    }

    @Test
    @Specification({
        "echo.data/server"
        })
    public void shouldEchoData() throws Exception {
        k3po.start();
        k3po.awaitBarrier("BOUND");
        connectTo8080(new IoHandlerAdapter<IoSessionEx>(){
            private int counter = 1;
            private DataMatcher dataMatch = new DataMatcher("server data " + counter);

            @Override
            protected void doSessionOpened(IoSessionEx session) throws Exception {
                writeStringMessageToSession("client data " + counter, session);
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

    @Ignore("https://github.com/kaazing/tickets/issues/538")
    @Test
    @Specification({
        "server.close/server"
        })
    public void shouldHandleServerClose() throws Exception {
        k3po.start();
        k3po.awaitBarrier("BOUND");
        CountDownLatch closed = new CountDownLatch(1);
        connectTo8080(new IoHandlerAdapter<IoSessionEx>() {
            @Override
            protected void doSessionClosed(IoSessionEx session) throws Exception {
                closed.countDown();
            }
        });
        
        k3po.notifyBarrier("CLOSEABLE");
        closed.await(5,  SECONDS);

        k3po.finish();
    }

    @Test
    @Specification({
        "client.close/server"
        })
    public void shouldIssueClientClose() throws Exception {
        k3po.start();
        k3po.awaitBarrier("BOUND");
        connectTo8080(new IoHandlerAdapter<IoSessionEx>(){
            @Override
            protected void doSessionOpened(IoSessionEx session) throws Exception {
                session.close(true);
            }
        });
        k3po.finish();
    }

    @Test
    @Specification({
        "concurrent.connections/server"
        })
    public void shouldEstablishConcurrentConnections() throws Exception {
        IoHandlerAdapter<IoSessionEx> adapter = new IoHandlerAdapter<IoSessionEx>(){
            @Override
            protected void doSessionOpened(IoSessionEx session) throws Exception {
                session.setAttribute("dataMatch", new DataMatcher("Hello"));
                writeStringMessageToSession("Hello", session);
            }

            @Override
            protected void doMessageReceived(IoSessionEx session, Object message) throws Exception {
                String decoded = new String(((IoBuffer) message).array());
                DataMatcher dataMatch = (DataMatcher) session.getAttribute("dataMatch");

                if (dataMatch.addFragment(decoded)) {
                    if (dataMatch.target.equals("Hello")) {
                        dataMatch = new DataMatcher("Goodbye");
                        writeStringMessageToSession("Goodbye", session);

                    } else {
                        session.close(true);
                    }
                    session.setAttribute("dataMatch", dataMatch);
                }

            }
        };
        k3po.start();
        k3po.awaitBarrier("BOUND");
        connectTo8080(adapter);
        connectTo8080(adapter);
        connectTo8080(adapter);

        k3po.finish();
    }

}
