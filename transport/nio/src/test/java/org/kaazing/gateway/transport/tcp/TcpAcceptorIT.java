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

import org.apache.mina.core.session.IoSession;
import org.junit.After;
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
import org.kaazing.gateway.util.scheduler.SchedulerProvider;
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

    

    private final TestRule timeout = new DisableOnDebug(new Timeout(5, SECONDS));

    private NioSocketAcceptor acceptor;
    private SchedulerProvider schedulerProvider;
    
    @Rule
    public final TestRule chain = outerRule(k3po).around(timeout);

    @Before
    public void before() throws Exception {
        acceptor = new NioSocketAcceptor(new Properties());
        acceptor.setSchedulerProvider(schedulerProvider = new SchedulerProvider());
        acceptor.setResourceAddressFactory(newResourceAddressFactory());
    }

    private void bindTo8080(IoHandlerAdapter handler) {
        ResourceAddressFactory addressFactory = ResourceAddressFactory.newResourceAddressFactory();

        Map<String, Object> acceptOptions = new HashMap<>();
        
        final String connectURIString = "tcp://127.0.0.1:8080";
        final ResourceAddress bindAddress =
                addressFactory.newResourceAddress(
                        URI.create(connectURIString),
                        acceptOptions);
        
        acceptor.bind(bindAddress, handler, null);
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
        bindTo8080(new IoHandlerAdapter());
        k3po.finish();
    }

    @Test
    @Specification({
        "server.sent.data/tcp.client"
        })
    public void serverSentData() throws Exception {
        bindTo8080(new IoHandlerAdapter(){
            @Override
            protected void doSessionOpened(IoSession session) throws Exception {
             // KG-8210: push pending write requests onto the write request queue before disposing, should not cause hang in dispose
                ByteBuffer data = ByteBuffer.allocate(20);
                String str = "server data";
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
        "client.sent.data/tcp.client"
        })
    public void clientSentData() throws Exception {
        bindTo8080(new IoHandlerAdapter());
        k3po.finish();
    }

    @Test
    @Specification({
        "bidirectional.data/tcp.client"
        })
    public void bidirectionalData() throws Exception {
        bindTo8080(new IoHandlerAdapter(){
            private int counter = 1;
            @Override
            protected void doMessageReceived(IoSession session, Object message) throws Exception {
             // KG-8210: push pending write requests onto the write request queue before disposing, should not cause hang in dispose
                ByteBuffer data = ByteBuffer.allocate(20);
                String str = "server data " + counter;
                counter++;
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
        "server.close/tcp.client"
        })
    public void serverClose() throws Exception {
        bindTo8080(new IoHandlerAdapter(){
            @Override
            protected void doSessionOpened(IoSession session) throws Exception {
                session.close();
            }
        });
        
        k3po.finish();
    }

    @Test
    @Specification({
        "client.close/tcp.client"
        })
    public void clientClose() throws Exception {
        bindTo8080(new IoHandlerAdapter());
        k3po.finish();
    }

    @Test
    @Specification({
        "concurrent.connections/tcp.client"
        })
    public void concurrentConnections() throws Exception {
        bindTo8080(new IoHandlerAdapter(){
            @Override
            protected void doMessageReceived(IoSession session, Object message) throws Exception {                
                session.write(message);
            }
        });
        k3po.finish();

    }

}
