/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import static java.util.concurrent.Executors.newCachedThreadPool;
import static org.jboss.netty.channel.Channels.pipeline;
import static org.jboss.netty.channel.Channels.pipelineFactory;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.DefaultSocketSessionConfig;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChildChannelStateEvent;
import org.jboss.netty.channel.ServerChannelFactory;
import org.jboss.netty.channel.local.DefaultLocalClientChannelFactory;
import org.jboss.netty.channel.local.DefaultLocalServerChannelFactory;
import org.jboss.netty.channel.local.LocalAddress;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;
import org.jboss.netty.channel.socket.ServerSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioWorker;
import org.jboss.netty.channel.socket.nio.NioWorkerPool;
import org.jboss.netty.channel.socket.nio.WorkerPool;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.logging.InternalLogLevel;
import org.junit.After;
import org.junit.Test;

import com.kaazing.mina.netty.socket.SocketChannelIoAcceptor;
import com.kaazing.mina.netty.socket.SocketChannelIoConnector;

public class IT {
    SocketAddress bindTo;
    ChannelIoAcceptor<?, ?, ?> acceptor = null;
    ChannelIoConnector<?, ?, ?> connector = null;
    
    @After
    public void tearDown() throws Exception {
        acceptor.unbind(bindTo);
        
        connector.dispose();
        acceptor.dispose();
    }
    
	@Test
	public void testNettyLocal() throws Exception {
	    bindTo = new LocalAddress(8123);
        
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
	
    @Test
    public void testThreadAlignment() throws Exception {
        bindTo = new InetSocketAddress(8123);
        
        // Mimic what NioSocketAcceptor does (in initAcceptor)
        WorkerPool<NioWorker> workerPool = new NioWorkerPool(Executors.newCachedThreadPool(), 3, false);
        ServerSocketChannelFactory serverChannelFactory = new NioServerSocketChannelFactory(
                Executors.newCachedThreadPool(), // boss executor
                workerPool);
        acceptor = new SocketChannelIoAcceptor(new DefaultSocketSessionConfig(), serverChannelFactory, 
                new IoAcceptorChannelHandlerFactory() {

                    @Override
                    public IoAcceptorChannelHandler createHandler(ChannelIoAcceptor<?, ?, ?> acceptor) {
                        return new IoAcceptorChannelHandler(acceptor) {

                            @Override
                            public void childChannelOpen(ChannelHandlerContext ctx, ChildChannelStateEvent e) throws Exception {
                                super.childChannelOpen(ctx, e);
                            }
                            
                        };
                    }
            
        });

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

        final CountDownLatch echoedMessageReceived = new CountDownLatch(1);
        
        ClientSocketChannelFactory clientChannelFactory = new NioClientSocketChannelFactory(
                newCachedThreadPool(), 
                1, // boss thread count
                workerPool);
        connector = new SocketChannelIoConnector(new DefaultSocketSessionConfig(), clientChannelFactory);
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
        
//        Callable<WriteFuture> writeTask = new Callable<WriteFuture>() {
//
//            @Override
//            public WriteFuture call() throws Exception {
//                return session.write(IoBuffer.wrap(new byte[] { 0x00, 0x01, 0x02 }));
//            }
//            
//        };
//        
//        WriteFuture written = new ScheduledThreadPoolExecutor(1).submit(writeTask).get();
        
        WriteFuture written = session.write(IoBuffer.wrap(new byte[] { 0x00, 0x01, 0x02 }));
        
        await(written, "session.write called in another thread");
        
        await(echoedMessageReceived, "echoedMessageReceived");
        await(session.close(true), "session close(true) future");
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
