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
package org.kaazing.mina.netty;

import static java.nio.ByteBuffer.wrap;
import static org.jboss.netty.channel.Channels.pipeline;
import static org.jboss.netty.channel.Channels.pipelineFactory;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.SocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;
import org.apache.mina.filter.logging.LoggingFilter;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ServerChannelFactory;
import org.jboss.netty.channel.local.DefaultLocalClientChannelFactory;
import org.jboss.netty.channel.local.DefaultLocalServerChannelFactory;
import org.jboss.netty.channel.local.LocalAddress;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.logging.InternalLogLevel;
import org.junit.After;
import org.junit.Test;

import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.session.IoSessionEx;

/**
 * Integration test for mina.netty layer
 */
public class LocalIT {
    SocketAddress bindTo = new LocalAddress(8123);
    SocketAddress bindTo2 = new LocalAddress(8124);
    ChannelIoAcceptor<?, ?, ?> acceptor;
    ChannelIoConnector<?, ?, ?> connector;

    @After
    public void tearDown() throws Exception {
        if (connector != null) {
            connector.dispose();
        }
        if (acceptor != null) {
            acceptor.unbind(bindTo);
            acceptor.unbind(bindTo2);
            acceptor.dispose();
        }
    }

    @Test
    public void testNettyLocal() throws Exception {
        ServerChannelFactory serverChannelFactory = new DefaultLocalServerChannelFactory();

        acceptor = new DefaultChannelIoAcceptor(serverChannelFactory);
        DefaultIoFilterChainBuilder builder = new DefaultIoFilterChainBuilder();
        builder.addLast("logger", new LoggingFilter());
        acceptor.setPipelineFactory(pipelineFactory(pipeline(new LoggingHandler(InternalLogLevel.INFO))));
        acceptor.setFilterChainBuilder(builder);
        acceptor.setHandler(new IoHandlerAdapter() {
            @Override
            public void messageReceived(IoSession session, Object message)
                    throws Exception {
                IoBufferEx buf = (IoBufferEx) message;
                session.write(buf.duplicate());
            }
        });

        acceptor.bind(bindTo);

        ChannelFactory clientChannelFactory = new DefaultLocalClientChannelFactory();

        final CountDownLatch echoedMessageReceived = new CountDownLatch(1);
        connector = new DefaultChannelIoConnector(clientChannelFactory);
        connector.setPipelineFactory(pipelineFactory(pipeline(new LoggingHandler(InternalLogLevel.INFO))));
        connector.setFilterChainBuilder(builder);
        connector.setHandler(new IoHandlerAdapter() {
            @Override
            public void messageReceived(IoSession session, Object message)
                    throws Exception {
                echoedMessageReceived.countDown();
            }
        });

        final AtomicBoolean sessionInitialized = new AtomicBoolean();
        ConnectFuture connectFuture = connector.connect(bindTo, new IoSessionInitializer<ConnectFuture>() {

            @Override
            public void initializeSession(IoSession session, ConnectFuture future) {
                sessionInitialized.set(true);
            }
        });

        await(connectFuture, "connect");
        assertTrue(sessionInitialized.get());
        final IoSessionEx session = (IoSessionEx) connectFuture.getSession();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        await(session.write(allocator.wrap(wrap(new byte[] { 0x00, 0x01, 0x02 }))), "session.write");

        await(echoedMessageReceived, "echoedMessageReceived");
        await(session.close(true), "session close(true) future");
    }

    static void await(IoFuture future, String description) throws InterruptedException {
        int waitSeconds = 10;
        if (!(future.await(waitSeconds, TimeUnit.SECONDS))) {
            fail(String.format("%s future not did not complete in %d seconds", description, waitSeconds));
        }
    }

    static void await(CountDownLatch latch, String description) throws InterruptedException {
        int waitSeconds = 10;
        if (!(latch.await(waitSeconds, TimeUnit.SECONDS))) {
            fail(String.format("%s latch not did not complete in %d seconds", description, waitSeconds));
        }
    }

}
