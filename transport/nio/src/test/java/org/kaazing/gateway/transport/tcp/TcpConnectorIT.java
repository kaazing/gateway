/*
 * Copyright 2014, Kaazing Corporation. All rights reserved.
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

package org.kaazing.gateway.transport.tcp;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.kaazing.gateway.resource.address.ResourceAddressFactory.newResourceAddressFactory;
import static org.kaazing.test.util.ITUtil.createRuleChain;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.session.IoSession;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.nio.internal.NioSocketAcceptor;
import org.kaazing.gateway.transport.nio.internal.NioSocketConnector;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.session.IoSessionEx;

/**
 * RFC-793
 */
public class TcpConnectorIT {

    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/tcp/rfc793");

    private NioSocketConnector connector;

    @Rule
    public TestRule chain = createRuleChain(k3po, 10, SECONDS);

    @Before
    public void before() throws Exception {
        NioSocketAcceptor acceptor = new NioSocketAcceptor(new Properties());

        connector = new NioSocketConnector(new Properties());
        connector.setResourceAddressFactory(newResourceAddressFactory());
        connector.setTcpAcceptor(acceptor);
    }

    private void connectTo8080(IoHandlerAdapter<IoSessionEx> handler) throws InterruptedException {
        ResourceAddressFactory addressFactory = ResourceAddressFactory.newResourceAddressFactory();

        Map<String, Object> acceptOptions = new HashMap<>();

        final String connectURIString = "tcp://127.0.0.1:8080";
        final ResourceAddress bindAddress =
                addressFactory.newResourceAddress(
                        URI.create(connectURIString),
                        acceptOptions);

        CountDownLatch latch = new CountDownLatch(1);
        ConnectFuture x = connector.connect(bindAddress, handler, null);
        x.addListener(new IoFutureListener<IoFuture>(){

            @Override
            public void operationComplete(IoFuture arg0) {
                latch.countDown();
            }

        });
        boolean didConnect = latch.await(1, SECONDS);
        Assert.assertTrue("Fail to connect", didConnect);
    }

    private void writeStringMessageToSession(String message, IoSession session) {
        ByteBuffer data = ByteBuffer.allocate(message.length());
        data.put(message.getBytes());

        data.flip();

        IoBufferAllocatorEx<?> allocator = ((IoSessionEx) session).getBufferAllocator();

        session.write(allocator.wrap(data.duplicate(), IoBufferEx.FLAG_SHARED));
    }

    @After
    public void after() throws Exception {
        // Make sure we always stop all I/O worker threads
        if (connector != null) {
            //schedulerProvider.shutdownNow();
            connector.dispose();
        }
    }

    @Test
    @Specification({
        "establish.connection/tcp.server"
        })
    public void establishConnection() throws Exception {
        connectTo8080(new IoHandlerAdapter<IoSessionEx>());
        k3po.finish();
    }

    @Test
    @Specification({
        "server.sent.data/tcp.server"
        })
    public void serverSentData() throws Exception {
        connectTo8080(new IoHandlerAdapter<IoSessionEx>());

        k3po.finish();
    }

    @Test
    @Specification({
        "client.sent.data/tcp.server"
        })
    public void clientSentData() throws Exception {
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
        "bidirectional.data/tcp.server"
        })
    public void bidirectionalData() throws Exception {
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

    @Test
    @Specification({
        "server.close/tcp.server"
        })
    public void serverClose() throws Exception {
        connectTo8080(new IoHandlerAdapter<IoSessionEx>());

        k3po.finish();
    }

    @Test
    @Specification({
        "client.close/tcp.server"
        })
    public void clientClose() throws Exception {
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
        "concurrent.connections/tcp.server"
        })
    public void concurrentConnections() throws Exception {
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
        connectTo8080(adapter);
        connectTo8080(adapter);
        connectTo8080(adapter);

        k3po.finish();

    }

}
