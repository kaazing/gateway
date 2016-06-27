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

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.nio.internal.NioSocketAcceptor;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.session.IoSessionEx;

/**
 * RFC-793
 */
public class TcpAcceptorIT {

    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/tcp/rfc793");

    private NioSocketAcceptor acceptor;
    private SchedulerProvider schedulerProvider;

    @Rule
    public TestRule chain = createRuleChain(k3po, 5, SECONDS);

    @Before
    public void before() throws Exception {
        acceptor = new NioSocketAcceptor(new Properties());
        acceptor.setSchedulerProvider(schedulerProvider = new SchedulerProvider());
        acceptor.setResourceAddressFactory(newResourceAddressFactory());
    }

    private void bindTo8080(IoHandlerAdapter<IoSessionEx> handler) {
        ResourceAddressFactory addressFactory = ResourceAddressFactory.newResourceAddressFactory();

        Map<String, Object> acceptOptions = new HashMap<>();

        final String connectURIString = "tcp://127.0.0.1:8080";
        final ResourceAddress bindAddress =
                addressFactory.newResourceAddress(
                        URI.create(connectURIString),
                        acceptOptions);

        acceptor.bind(bindAddress, handler, null);
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
        if (acceptor != null) {
            schedulerProvider.shutdownNow();
            acceptor.dispose();
        }
    }

    @Test
    @Specification({
        "establish.connection/tcp.client"
        })
    public void establishConnection() throws Exception {
        bindTo8080(new IoHandlerAdapter<IoSessionEx>());
        k3po.finish();
    }

    @Test
    @Specification({
        "server.sent.data/tcp.client"
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
        "client.sent.data/tcp.client"
        })
    public void clientSentData() throws Exception {
        bindTo8080(new IoHandlerAdapter<IoSessionEx>());
        k3po.finish();
    }

    @Test
    @Specification({
        "bidirectional.data/tcp.client"
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
        "server.close/tcp.client"
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
        "client.close/tcp.client"
        })
    public void clientClose() throws Exception {
        bindTo8080(new IoHandlerAdapter<IoSessionEx>());
        k3po.finish();
    }

    @Test
    @Specification({
        "concurrent.connections/tcp.client"
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
