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

import static org.kaazing.test.util.ITUtil.createRuleChain;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
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
public class TcpAcceptorIT {

    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/tcp/rfc793");
    
    private TcpAcceptorRule acceptor = new TcpAcceptorRule();

    private static String networkInterface = ResolutionTestUtils.getLoopbackInterface();

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {     
                {"tcp://127.0.0.1:8080"}, {"tcp://[@" + networkInterface + "]:8080"}
           });
    }

    @Parameter
    public String uri;

    @Rule
    public TestRule chain = createRuleChain(acceptor, k3po);

    private void bindTo8080(IoHandlerAdapter<IoSessionEx> handler) throws InterruptedException {
        acceptor.bind(uri, handler);
        k3po.start();
        k3po.notifyBarrier("BOUND");
    }

    private void writeStringMessageToSession(String message, IoSession session) {
        ByteBuffer data = ByteBuffer.allocate(message.length());
        data.put(message.getBytes());

        data.flip();

        IoBufferAllocatorEx<?> allocator = ((IoSessionEx) session).getBufferAllocator();

        session.write(allocator.wrap(data.duplicate(), IoBufferEx.FLAG_SHARED));
    }

    @Ignore("https://github.com/kaazing/gateway/issues/357")
    @Test
    @Specification({
        "establish.connection/client"
        })
    public void establishConnection() throws Exception {
        bindTo8080(new IoHandlerAdapter<>());
        k3po.finish();
    }

    @Test
    @Specification({
        "server.sent.data/client"
        })
    public void serverSentData() throws Exception {
        bindTo8080(new IoHandlerAdapter<IoSessionEx>(){
            @Override
            protected void doSessionOpened(IoSessionEx session) throws Exception {
                writeStringMessageToSession("server data", session);
            }
        });

        k3po.finish();
    }

    @Test
    @Specification({
        "client.sent.data/client"
        })
    public void clientSentData() throws Exception {
        bindTo8080(new IoHandlerAdapter<>());
        k3po.finish();
    }

    @Test
    @Specification({
        "echo.data/client"
        })
    public void bidirectionalData() throws Exception {
        bindTo8080(new IoHandlerAdapter<IoSessionEx>(){
            private int counter = 1;
            private DataMatcher dataMatch = new DataMatcher("client data " + counter);
            @Override
            protected void doMessageReceived(IoSessionEx session, Object message) throws Exception {
                String decoded = new String(((IoBuffer) message).array());

                if (dataMatch.addFragment(decoded)) {
                    writeStringMessageToSession("server data " + counter, session);
                    counter++;
                    dataMatch = new DataMatcher("client data " + counter);
                }
            }
        });


        k3po.finish();
    }

    @Test
    @Specification({
        "server.close/client"
        })
    public void serverClose() throws Exception {
        bindTo8080(new IoHandlerAdapter<IoSessionEx>(){
            @Override
            protected void doSessionOpened(IoSessionEx session) throws Exception {
                session.close(true);
            }
        });

        k3po.finish();
    }

    @Test
    @Specification({
        "client.close/client"
        })
    public void clientClose() throws Exception {
        bindTo8080(new IoHandlerAdapter<IoSessionEx>() {
            @Override
            protected void doSessionOpened(IoSessionEx session) throws Exception {
                k3po.notifyBarrier("CLOSEABLE");
            }
        });
        k3po.finish();
    }

    @Test
    @Specification({
        "concurrent.connections/client"
        })
    public void concurrentConnections() throws Exception {
        bindTo8080(new IoHandlerAdapter<IoSessionEx>(){

            @Override
            protected void doSessionOpened(IoSessionEx session) throws Exception {
                session.setAttribute("dataMatch", new DataMatcher("Hello"));
            }

            @Override
            protected void doMessageReceived(IoSessionEx session, Object message) throws Exception {
                String decoded = new String(((IoBuffer) message).array());
                DataMatcher dataMatch = (DataMatcher) session.getAttribute("dataMatch");

                if (dataMatch.addFragment(decoded)) {
                    if (dataMatch.target.equals("Hello")) {
                        dataMatch = new DataMatcher("Goodbye");
                        writeStringMessageToSession("Hello", session);

                    } else {
                        dataMatch = new DataMatcher("");
                        writeStringMessageToSession("Goodbye", session);
                    }
                    session.setAttribute("dataMatch", dataMatch);
                }
            }
        });
        k3po.finish();

    }

}
