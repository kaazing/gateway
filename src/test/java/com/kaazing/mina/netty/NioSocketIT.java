/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import static com.kaazing.mina.netty.PortUtil.nextPort;
import static java.lang.String.format;
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
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;
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
import com.kaazing.mina.core.future.UnbindFuture;
import com.kaazing.mina.core.session.IoSessionConfigEx;
import com.kaazing.mina.netty.socket.nio.DefaultNioSocketChannelIoSessionConfig;
import com.kaazing.mina.netty.socket.nio.NioSocketChannelIoAcceptor;
import com.kaazing.mina.netty.socket.nio.NioSocketChannelIoConnector;

/**
 * Integration test for mina.netty layer
 */
public class NioSocketIT {
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
    public void testThreadAlignment() throws Exception {
        bindTo = new InetSocketAddress("localhost", nextPort(8100, 100));
        final AtomicInteger acceptExceptionsCaught = new AtomicInteger(0);

        // Mimic what NioSocketAcceptor does (in initAcceptor)
        WorkerPool<NioWorker> workerPool = new NioWorkerPool(
                Executors.newCachedThreadPool(), // worker executor
                3); // number of workers
        NioServerSocketChannelFactory serverChannelFactory = new NioServerSocketChannelFactory(
                Executors.newCachedThreadPool(), // boss executor
                workerPool);
        acceptor = new NioSocketChannelIoAcceptor(new DefaultNioSocketChannelIoSessionConfig(),
                                                  serverChannelFactory);

        DefaultIoFilterChainBuilder builder = new DefaultIoFilterChainBuilder();
        //builder.addLast("logger", new LoggingFilter());
        acceptor.setPipelineFactory(pipelineFactory(pipeline(new LoggingHandler(InternalLogLevel.INFO))));
        acceptor.setFilterChainBuilder(builder);
        acceptor.setHandler(new IoHandlerAdapter() {
            @Override
            public void messageReceived(IoSession session, Object message)
                    throws Exception {
                IoBuffer buf = (IoBuffer) message;
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
        connector = new NioSocketChannelIoConnector(new DefaultNioSocketChannelIoSessionConfig(),
                clientChannelFactory);
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
    //@Ignore
    public void testBindAsync() throws Exception {
        bindTo = new InetSocketAddress("localhost", nextPort(8100, 100));
        bindTo2 = new InetSocketAddress("localhost", nextPort(8100, 100));
        final AtomicInteger acceptExceptionsCaught = new AtomicInteger(0);

        // Mimic what NioSocketAcceptor does (in initAcceptor)
        WorkerPool<NioWorker> workerPool = new NioWorkerPool(
                Executors.newCachedThreadPool(), // worker executor
                3); // number of workers
        NioServerSocketChannelFactory serverChannelFactory = new NioServerSocketChannelFactory(
                Executors.newCachedThreadPool(), // boss executor
                workerPool);
        acceptor = new NioSocketChannelIoAcceptor(new DefaultNioSocketChannelIoSessionConfig(),
                                                  serverChannelFactory);

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
                IoBuffer buf = (IoBuffer) message;
                // Synchronous acceptor.bind call fails from an IO worker thread. But we should be able to do
                // an asynchronous bind (see KG-7179)
                try {
                    boundInIoThread[0] = acceptor.bindAsync(bindTo2);
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
        connector = new NioSocketChannelIoConnector(new DefaultNioSocketChannelIoSessionConfig(), clientChannelFactory);
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
        //await(session.close(true), "session close(true) future");

        assertEquals("Exceptions caught by connect handler", 0, connectExceptionsCaught.get());
        assertEquals("Exceptions caught by accept handler", 0, acceptExceptionsCaught.get());
        assertNull("acceptor.bind in acceptor IO thread threw exception " + bindException[0], bindException[0]);
        boundInIoThread[0].await();
        assertTrue("Bind in IO thread failed with exception " + boundInIoThread[0].getException(),
                   boundInIoThread[0].isBound());

        acceptor.unbind(bindTo);
    }

    @Test
    public void testUnbindAsync() throws Exception {
        bindTo = new InetSocketAddress("localhost", nextPort(8100, 100));
        bindTo2 = new InetSocketAddress("localhost", nextPort(8100, 100));
        final AtomicInteger acceptExceptionsCaught = new AtomicInteger(0);

        // Mimic what NioSocketAcceptor does (in initAcceptor)
        WorkerPool<NioWorker> workerPool = new NioWorkerPool(
                Executors.newCachedThreadPool(), // worker executor
                3); // number of workers
        NioServerSocketChannelFactory serverChannelFactory = new NioServerSocketChannelFactory(
                Executors.newCachedThreadPool(), // boss executor
                workerPool);
        acceptor = new NioSocketChannelIoAcceptor(new DefaultNioSocketChannelIoSessionConfig(),
                                                  serverChannelFactory);

        DefaultIoFilterChainBuilder builder = new DefaultIoFilterChainBuilder();
        //builder.addLast("logger", new LoggingFilter());
        acceptor.setPipelineFactory(pipelineFactory(pipeline(new LoggingHandler(InternalLogLevel.INFO))));
        acceptor.setFilterChainBuilder(builder);
        final Throwable[] unbindException = new Throwable[]{null};
        final UnbindFuture[] unboundInIoThread = new UnbindFuture[]{null};
        acceptor.setHandler(new IoHandlerAdapter() {
            @Override
            public void messageReceived(IoSession session, Object message)
                    throws Exception {
                IoBuffer buf = (IoBuffer) message;
                // Test asynchronous acceptor.unbind call from an IO worker thread.
                try {
                    unboundInIoThread[0] = acceptor.unbindAsync(bindTo2);
                }
                catch (Throwable t) {
                    unbindException[0] = t;
                }
                session.write(buf.duplicate());
            }

            @Override
            public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
                acceptExceptionsCaught.incrementAndGet();
            }
        });

        acceptor.bind(bindTo);
        acceptor.bind(bindTo2);

        final CountDownLatch echoedMessageReceived = new CountDownLatch(1);
        final AtomicInteger connectExceptionsCaught = new AtomicInteger(0);

        NioClientSocketChannelFactory clientChannelFactory = new NioClientSocketChannelFactory(
                newCachedThreadPool(),
                1, // boss thread count
                workerPool);
        connector = new NioSocketChannelIoConnector(new DefaultNioSocketChannelIoSessionConfig(), clientChannelFactory);
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
        //await(session.close(true), "session close(true) future");

        assertEquals("Exceptions caught by connect handler", 0, connectExceptionsCaught.get());
        assertEquals("Exceptions caught by accept handler", 0, acceptExceptionsCaught.get());
        assertNull("acceptor.bind in acceptor IO thread threw exception " + unbindException[0], unbindException[0]);
        unboundInIoThread[0].await();
        assertTrue("Unbind in IO thread failed with exception " + unboundInIoThread[0].getException(),
                   unboundInIoThread[0].isUnbound());

        acceptor.unbindAsync(bindTo).await();
    }

    @Test
    public void testBothIdleTimeout() throws Exception {
        testIdleTimeout(IdleStatus.BOTH_IDLE);
    }

    @Test
    public void testReadIdleTimeout() throws Exception {
        testIdleTimeout(IdleStatus.READER_IDLE);
    }

    @Test
    public void testWriteIdleTimeout() throws Exception {
        testIdleTimeout(IdleStatus.WRITER_IDLE);
    }

    private void testIdleTimeout(final IdleStatus statusUnderTest) throws Exception {

        bindTo = new InetSocketAddress("localhost", nextPort(8100, 100));

        final AtomicInteger acceptExceptionsCaught = new AtomicInteger(0);

        // Mimic what NioSocketAcceptor does (in initAcceptor)
        WorkerPool<NioWorker> workerPool = new NioWorkerPool(
                Executors.newCachedThreadPool(), // worker executor
                3); // number of workers
        NioServerSocketChannelFactory serverChannelFactory = new NioServerSocketChannelFactory(
                Executors.newCachedThreadPool(), // boss executor
                workerPool);
        acceptor = new NioSocketChannelIoAcceptor(new DefaultNioSocketChannelIoSessionConfig(), serverChannelFactory);

        DefaultIoFilterChainBuilder builder = new DefaultIoFilterChainBuilder();
        final CountDownLatch idleFired = new CountDownLatch(2);
        builder.addLast("idleTimeoutTestFilter", new IoFilterAdapter() {
            private long idleTimeoutSetAt;

            public void sessionIdle(NextFilter nextFilter, IoSession session,
                    IdleStatus status) throws Exception {
                assert status.equals(statusUnderTest);
                long idleFiredAfter = System.currentTimeMillis() - idleTimeoutSetAt;
                System.out.println(
                    format("idleTimeoutTestFilter: sessionIdle(%s) was called %d ms after calling setIdleTimeInMillis",
                    status, idleFiredAfter));
                idleFired.countDown();
                if (idleFired.getCount() > 0) {
                    System.out.println("idleTimeoutTestFilter.sessionIdle: calling setIdleTimeInMillis(200)");
                    ((IoSessionConfigEx) session.getConfig()).setIdleTimeInMillis(status, 200);
                    idleTimeoutSetAt = System.currentTimeMillis();
                }
                nextFilter.sessionIdle(session, status);
            }

            public void messageReceived(NextFilter nextFilter, IoSession session,
                    Object message) throws Exception {
                nextFilter.messageReceived(session, message);
                System.out.println("idleTimeoutTestFilter.messageReceived: calling setIdleTimeInMillis(50)");
                ((IoSessionConfigEx) session.getConfig()).setIdleTimeInMillis(statusUnderTest, 50);
                idleTimeoutSetAt = System.currentTimeMillis();
            }

        });
        acceptor.setPipelineFactory(pipelineFactory(pipeline(new LoggingHandler(InternalLogLevel.INFO))));
        acceptor.setFilterChainBuilder(builder);
        acceptor.setHandler(new IoHandlerAdapter() {
            @Override
            public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
                System.out.println("Acceptor handler: exceptionCaught:" + cause);
                acceptExceptionsCaught.incrementAndGet();
            }
        });

        acceptor.bind(bindTo);

        final AtomicInteger connectExceptionsCaught = new AtomicInteger(0);

        NioClientSocketChannelFactory clientChannelFactory = new NioClientSocketChannelFactory(
                newCachedThreadPool(),
                1, // boss thread count
                workerPool);
        connector = new NioSocketChannelIoConnector(new DefaultNioSocketChannelIoSessionConfig(),
                clientChannelFactory);
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
        await(idleFired, "sessionIdle fired twice in idleTimeoutTestFilter");

        await(session.close(true), "session close(true) future");

        assertEquals("Exceptions caught by connector handler", 0, connectExceptionsCaught.get());
        assertEquals("Exceptions caught by acceptor handler", 0, acceptExceptionsCaught.get());
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
            fail(String.format("\"%s\" latch not did not complete in %d seconds", description, waitSeconds));
        }
    }

}
