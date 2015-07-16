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
import static org.junit.rules.RuleChain.outerRule;
import static org.kaazing.gateway.resource.address.ResourceAddressFactory.newResourceAddressFactory;

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
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.nio.internal.NioSocketAcceptor;
import org.kaazing.gateway.transport.nio.internal.NioSocketConnector;
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

    private final TestRule timeout = new DisableOnDebug(new Timeout(5, SECONDS));

    private NioSocketConnector connector;
    private SchedulerProvider schedulerProvider;
    
    @Rule
    public final TestRule chain = outerRule(k3po).around(timeout);

    @Before
    public void before() throws Exception {
        NioSocketAcceptor acceptor = new NioSocketAcceptor(new Properties());
        
        connector = new NioSocketConnector(new Properties());
        connector.setResourceAddressFactory(newResourceAddressFactory());
        connector.setTcpAcceptor(acceptor);
    }

    private void connectTo8080(IoHandlerAdapter handler) throws InterruptedException {
        ResourceAddressFactory addressFactory = ResourceAddressFactory.newResourceAddressFactory();

        Map<String, Object> acceptOptions = new HashMap<>();
        
        final String connectURIString = "tcp://127.0.0.1:8080";
        final ResourceAddress bindAddress =
                addressFactory.newResourceAddress(
                        URI.create(connectURIString),
                        acceptOptions);
        
        CountDownLatch latch = new CountDownLatch(1);
        ConnectFuture x = connector.connect(bindAddress, handler, null);
        x.addListener(new IoFutureListener(){

            @Override
            public void operationComplete(IoFuture arg0) {
                latch.countDown();
            }
            
        });
        boolean didConnect = latch.await(1, SECONDS);
        Assert.assertTrue("Fail to connect", didConnect);
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
        connectTo8080(new IoHandlerAdapter());
        k3po.finish();
    }

    @Test
    @Specification({
        "server.sent.data/tcp.server"
        })
    public void serverSentData() throws Exception {
        connectTo8080(new IoHandlerAdapter());
        
        k3po.finish();
    }

    @Test
    @Specification({
        "client.sent.data/tcp.server"
        })
    public void clientSentData() throws Exception {
        connectTo8080(new IoHandlerAdapter(){
            @Override
            protected void doSessionOpened(IoSession session) throws Exception {
                ByteBuffer data = ByteBuffer.allocate(20);
                String str = "client data";
                data.put(str.getBytes());
                
                data.flip();

                IoBufferAllocatorEx<?> allocator = ((IoSessionEx) session).getBufferAllocator();
                
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
        connectTo8080(new IoHandlerAdapter(){
            private int counter = 1;
            
            @Override
            protected void doSessionOpened(IoSession session) throws Exception {
                ByteBuffer data = ByteBuffer.allocate(20);
                String str = "client data " + counter;
                data.put(str.getBytes());
                
                data.flip();

                IoBufferAllocatorEx<?> allocator = ((IoSessionEx) session).getBufferAllocator();
                
                session.write(allocator.wrap(data.duplicate(), IoBufferEx.FLAG_SHARED));
            }
            
            @Override
            protected void doMessageReceived(IoSession session, Object message) throws Exception {
                String decoded = new String(((IoBuffer) message).array());

                if (decoded.equals("server data " + counter) && counter < 2) {
                    ByteBuffer data = ByteBuffer.allocate(20);
                    counter++;
                    String str = "client data " + counter;
                    
                    data.put(str.getBytes());
                
                    data.flip();

                    IoBufferAllocatorEx<?> allocator = ((IoSessionEx) session).getBufferAllocator();
                
                    session.write(allocator.wrap(data.duplicate(), IoBufferEx.FLAG_SHARED));
                } else {
                    session.close();
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
        connectTo8080(new IoHandlerAdapter());
        
        k3po.finish();
    }

    @Test
    @Specification({
        "client.close/tcp.server"
        })
    public void clientClose() throws Exception {
        connectTo8080(new IoHandlerAdapter(){
            @Override
            protected void doSessionOpened(IoSession session) throws Exception {
                session.close();
            }
        });
        k3po.finish();
    }

    @Test
    @Specification({
        "concurrent.connections/tcp.server"
        })
    public void concurrentConnections() throws Exception {
        IoHandlerAdapter adapter = new IoHandlerAdapter(){
            @Override
            protected void doSessionOpened(IoSession session) throws Exception {
                ByteBuffer data = ByteBuffer.allocate(20);
                String str = "Hello";
                data.put(str.getBytes());
                
                data.flip();

                IoBufferAllocatorEx<?> allocator = ((IoSessionEx) session).getBufferAllocator();
                
                session.write(allocator.wrap(data.duplicate(), IoBufferEx.FLAG_SHARED));
            }
            
            @Override
            protected void doMessageReceived(IoSession session, Object message) throws Exception {
                String decoded = new String(((IoBuffer) message).array());

                if (decoded.equals("Hello")) {
                    ByteBuffer data = ByteBuffer.allocate(7);
                    data.put("Goodbye".getBytes());
                    data.flip();

                    IoBufferAllocatorEx<?> allocator = ((IoSessionEx) session).getBufferAllocator();
                    
                    session.write(allocator.wrap(data.duplicate(), IoBufferEx.FLAG_SHARED));
                } else {
                    session.close();
                }
                
            }
        };
        connectTo8080(adapter);
        connectTo8080(adapter);
        connectTo8080(adapter);
        
        k3po.finish();

    }

}
