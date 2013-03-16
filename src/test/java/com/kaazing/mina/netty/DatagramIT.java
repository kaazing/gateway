/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import static org.jboss.netty.channel.Channels.pipeline;
import static org.jboss.netty.channel.Channels.pipelineFactory;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;
import org.jboss.netty.channel.local.LocalAddress;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.logging.InternalLogLevel;
import org.junit.After;
import org.junit.Test;

import com.kaazing.mina.netty.nio.NioDatagramChannelIoAcceptor;
import com.kaazing.mina.netty.nio.NioDatagramChannelIoConnector;
import com.kaazing.mina.netty.socket.DefaultDatagramChannelIoSessionConfig;

/**
 * Integration test for mina.netty layer. Similar to IT, but for datagram transport.
 */
public class DatagramIT {
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
    public void testDatagram() throws Exception {
        bindTo = new InetSocketAddress(8123);
        final AtomicInteger acceptExceptionsCaught = new AtomicInteger(0);
        
        NioDatagramChannelFactory serverChannelFactory = new NioDatagramChannelFactory();
        acceptor = new NioDatagramChannelIoAcceptor(new DefaultDatagramChannelIoSessionConfig(),
                   serverChannelFactory);

        DefaultIoFilterChainBuilder builder = new DefaultIoFilterChainBuilder();
        //builder.addLast("logger", new LoggingFilter());
        acceptor.setPipelineFactory(pipelineFactory(pipeline(new LoggingHandler(InternalLogLevel.INFO))));
        acceptor.setFilterChainBuilder(builder);
        acceptor.setHandler(new IoHandlerAdapter() {
            @Override
            public void messageReceived(IoSession session, Object message)
                    throws Exception {
                IoBuffer buf = (IoBuffer)message;
                session.write(buf.duplicate());
            }            
            @Override
            public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
                acceptExceptionsCaught.incrementAndGet();
            }
        });
        
        acceptor.bind(bindTo);

        final CountDownLatch echoedMessageReceived = new CountDownLatch(1);
        final AtomicInteger connectExceptionsCaught = new AtomicInteger(0);
        
        NioDatagramChannelFactory clientChannelFactory = new NioDatagramChannelFactory();
        connector = new NioDatagramChannelIoConnector(new DefaultDatagramChannelIoSessionConfig(), clientChannelFactory);
        connector.setPipelineFactory(pipelineFactory(pipeline(new LoggingHandler(InternalLogLevel.INFO))));
        connector.setFilterChainBuilder(builder);
        connector.setHandler(new IoHandlerAdapter() {
            @Override
            public void messageReceived(IoSession session, Object message) throws Exception {
                echoedMessageReceived.countDown();
            }
            @Override
            public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
                connectExceptionsCaught.incrementAndGet();
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
        
        WriteFuture written = session.write(IoBuffer.wrap(new byte[] { 0x00, 0x01, 0x02 }));
        
        await(written, "session.write");
        
        await(echoedMessageReceived, "echoedMessageReceived");
        await(session.close(true), "session close(true) future");
        
        assertEquals("Exceptions caught by connect handler", 0, connectExceptionsCaught.get());
        assertEquals("Exceptions caught by except handler", 0, acceptExceptionsCaught.get());
    }
    
	private void await(IoFuture future, String description) throws InterruptedException {
	    int waitSeconds = 10;
	    if (!(future.await(waitSeconds, TimeUnit.SECONDS))) {
	        fail(String.format("%s future not did not complete in %d seconds", description, waitSeconds));
	    }	    
	}
    
    private void await(CountDownLatch latch, String description) throws InterruptedException {
        int waitSeconds = 10;
        if (!(latch.await(waitSeconds, TimeUnit.SECONDS))) {
            fail(String.format("%s latch not did not complete in %d seconds", description, waitSeconds));
        }       
    }

}
