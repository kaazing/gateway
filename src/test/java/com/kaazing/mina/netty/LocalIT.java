/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import static org.jboss.netty.channel.Channels.pipeline;
import static org.jboss.netty.channel.Channels.pipelineFactory;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.SocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.mina.core.buffer.IoBuffer;
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

/**
 * Integration test for mina.netty layer
 */
public class LocalIT {
    SocketAddress bindTo = new LocalAddress(8123);
    SocketAddress bindTo2 = new LocalAddress(8124);
    ChannelIoAcceptor<?, ?, ?> acceptor = null;
    ChannelIoConnector<?, ?, ?> connector = null;
    
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
                IoBuffer buf = (IoBuffer)message;
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
        final IoSession session = connectFuture.getSession();
        
        await(session.write(IoBuffer.wrap(new byte[] { 0x00, 0x01, 0x02 })), "session.write");
        
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
