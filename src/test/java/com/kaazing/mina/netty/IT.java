/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import static java.util.concurrent.Executors.newCachedThreadPool;
import static org.jboss.netty.channel.Channels.pipeline;
import static org.jboss.netty.channel.Channels.pipelineFactory;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;
import org.apache.mina.filter.logging.LoggingFilter;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChildChannelStateEvent;
import org.jboss.netty.channel.ServerChannelFactory;
import org.jboss.netty.channel.local.DefaultLocalClientChannelFactory;
import org.jboss.netty.channel.local.DefaultLocalServerChannelFactory;
import org.jboss.netty.channel.local.LocalAddress;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioWorker;
import org.jboss.netty.channel.socket.nio.NioWorkerPool;
import org.jboss.netty.channel.socket.nio.WorkerPool;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.logging.InternalLogLevel;
import org.junit.After;
import org.junit.Test;

import com.kaazing.mina.core.future.BindFuture;
import com.kaazing.mina.core.session.IoSessionConfigEx;
import com.kaazing.mina.netty.nio.NioSocketChannelIoAcceptor;
import com.kaazing.mina.netty.nio.NioSocketChannelIoConnector;
import com.kaazing.mina.netty.socket.DefaultSocketChannelIoSessionConfig;

/**
 * Integration test for mina.netty layer
 */
public class IT {
    SocketAddress bindTo = new LocalAddress(8123);
    SocketAddress bindTo2 = new LocalAddress(8124);
    ChannelIoAcceptor<?, ?, ?> acceptor = null;
    ChannelIoConnector<?, ?, ?> connector = null;
    
    @After
    public void tearDown() throws Exception {
        acceptor.unbind(bindTo);
        acceptor.unbind(bindTo2);        
        connector.dispose();
        acceptor.dispose();
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
	
    @Test
    public void testThreadAlignment() throws Exception {
        bindTo = new InetSocketAddress(8123);
        
        final AtomicInteger acceptExceptionsCaught = new AtomicInteger(0);
        
        // Mimic what NioSocketAcceptor does (in initAcceptor)
        WorkerPool<NioWorker> workerPool = new NioWorkerPool(
                Executors.newCachedThreadPool(), // worker executor 
                3); // number of workers
        NioServerSocketChannelFactory serverChannelFactory = new NioServerSocketChannelFactory(
                Executors.newCachedThreadPool(), // boss executor
                workerPool);
        acceptor = new NioSocketChannelIoAcceptor(new DefaultSocketChannelIoSessionConfig(),
                   serverChannelFactory, 
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
        
        NioClientSocketChannelFactory clientChannelFactory = new NioClientSocketChannelFactory(
                newCachedThreadPool(), 
                1, // boss thread count
                workerPool);
        connector = new NioSocketChannelIoConnector(new DefaultSocketChannelIoSessionConfig(), clientChannelFactory);
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
    
    @Test
    public void testBindAsync() throws Exception {
        bindTo = new InetSocketAddress(8123);
        
        final AtomicInteger acceptExceptionsCaught = new AtomicInteger(0);
        
        // Mimic what NioSocketAcceptor does (in initAcceptor)
        WorkerPool<NioWorker> workerPool = new NioWorkerPool(
                Executors.newCachedThreadPool(), // worker executor 
                3); // number of workers
        NioServerSocketChannelFactory serverChannelFactory = new NioServerSocketChannelFactory(
                Executors.newCachedThreadPool(), // boss executor
                workerPool);
        acceptor = new NioSocketChannelIoAcceptor(new DefaultSocketChannelIoSessionConfig(),
                   serverChannelFactory, 
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
        //builder.addLast("logger", new LoggingFilter());
        acceptor.setPipelineFactory(pipelineFactory(pipeline(new LoggingHandler(InternalLogLevel.INFO))));
        acceptor.setFilterChainBuilder(builder);
        final Throwable[] bindException = new Throwable[]{null};
        final BindFuture[] boundInIoThread = new BindFuture[]{null};
        acceptor.setHandler(new IoHandlerAdapter() {
            @Override
            public void messageReceived(IoSession session, Object message)
                    throws Exception {
                IoBuffer buf = (IoBuffer)message;
                // Synchronous acceptor.bind call fails from an IO worker thread. But we should be able to do 
                // an asynchronous bind (see KG-7179)
                try {
                    boundInIoThread[0] = acceptor.bindAsync(new InetSocketAddress(8124));
                }
                catch (Throwable t) {
                    bindException[0] = t;
                }
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
        
        NioClientSocketChannelFactory clientChannelFactory = new NioClientSocketChannelFactory(
                newCachedThreadPool(), 
                1, // boss thread count
                workerPool);
        connector = new NioSocketChannelIoConnector(new DefaultSocketChannelIoSessionConfig(), clientChannelFactory);
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
        assertEquals("Exceptions caught by accept handler", 0, acceptExceptionsCaught.get());
        assertNull("acceptor.bind in acceptor IO thread threw exception " + bindException[0], bindException[0]);
        boundInIoThread[0].await();
        assertTrue("Bind in IO thread failed with exception " + boundInIoThread[0].getException(), 
                   boundInIoThread[0].isBound());
    }
    
    @Test
    public void testIdleTimeout() throws Exception {
        bindTo = new InetSocketAddress(8123);
        
        final AtomicInteger acceptExceptionsCaught = new AtomicInteger(0);
        
        // Mimic what NioSocketAcceptor does (in initAcceptor)
        WorkerPool<NioWorker> workerPool = new NioWorkerPool(
                Executors.newCachedThreadPool(), // worker executor 
                3); // number of workers
        NioServerSocketChannelFactory serverChannelFactory = new NioServerSocketChannelFactory(
                Executors.newCachedThreadPool(), // boss executor
                workerPool);
        acceptor = new NioSocketChannelIoAcceptor(new DefaultSocketChannelIoSessionConfig(), serverChannelFactory);

        DefaultIoFilterChainBuilder builder = new DefaultIoFilterChainBuilder();
        final CountDownLatch idleFired = new CountDownLatch(2);
        builder.addLast("idleTimeoutTestFilter", new IoFilterAdapter() {
            private long idleTimeoutSetAt;
            public void sessionIdle(NextFilter nextFilter, IoSession session,
                    IdleStatus status) throws Exception {
                long idleFiredAfter = System.currentTimeMillis() - idleTimeoutSetAt;
                System.out.println(
                    String.format("idleTimeoutTestFilter: sessionIdle was called %d millis after calling setIdleTimeInMillis",
                    idleFiredAfter));
                idleFired.countDown();
                if (idleFired.getCount() > 0) {
                    System.out.println("idleTimeoutTestFilter.sessionIdle: calling setIdleTimeInMillis(200)");
                    ((IoSessionConfigEx)session.getConfig()).setIdleTimeInMillis(IdleStatus.READER_IDLE, 200);
                    idleTimeoutSetAt = System.currentTimeMillis();
                }
                nextFilter.sessionIdle(session, status);
            }
 
            public void messageReceived(NextFilter nextFilter, IoSession session,
                    Object message) throws Exception {
                nextFilter.messageReceived(session, message);
                System.out.println("idleTimeoutTestFilter.messageReceived: calling setIdleTimeInMillis");
                ((IoSessionConfigEx)session.getConfig()).setIdleTimeInMillis(IdleStatus.READER_IDLE, 50);
                idleTimeoutSetAt = System.currentTimeMillis();
            }

        });
        acceptor.setPipelineFactory(pipelineFactory(pipeline(new LoggingHandler(InternalLogLevel.INFO))));
        acceptor.setFilterChainBuilder(builder);
        acceptor.setHandler(new IoHandlerAdapter() {
            @Override
            public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
                acceptExceptionsCaught.incrementAndGet();
            }
        });
        
        acceptor.bind(bindTo);

        final AtomicInteger connectExceptionsCaught = new AtomicInteger(0);
        
        NioClientSocketChannelFactory clientChannelFactory = new NioClientSocketChannelFactory(
                newCachedThreadPool(), 
                1, // boss thread count
                workerPool);
        connector = new NioSocketChannelIoConnector(new DefaultSocketChannelIoSessionConfig(), clientChannelFactory);
        connector.setPipelineFactory(pipelineFactory(pipeline(new LoggingHandler(InternalLogLevel.INFO))));
        connector.setFilterChainBuilder(builder);
        connector.setHandler(new IoHandlerAdapter() {
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

        // this is the main point of this test
        await(idleFired, "sessionIdle fired on idleTimeoutTestFilter");
        
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
