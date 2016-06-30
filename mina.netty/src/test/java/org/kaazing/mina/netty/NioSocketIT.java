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

import static org.kaazing.mina.netty.PortUtil.nextPort;
import static java.lang.String.format;
import static java.nio.ByteBuffer.wrap;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static org.jboss.netty.channel.Channels.pipeline;
import static org.jboss.netty.channel.Channels.pipelineFactory;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
import org.junit.Rule;
import org.junit.Test;

import org.junit.rules.TestRule;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.future.BindFuture;
import org.kaazing.mina.core.future.UnbindFuture;
import org.kaazing.mina.core.session.IoSessionConfigEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.mina.netty.socket.nio.DefaultNioSocketChannelIoSessionConfig;
import org.kaazing.mina.netty.socket.nio.NioSocketChannelIoAcceptor;
import org.kaazing.mina.netty.socket.nio.NioSocketChannelIoConnector;
import org.kaazing.test.util.ITUtil;
import org.kaazing.test.util.MethodExecutionTrace;

/**
 * Integration test for mina.netty layer
 */
public class NioSocketIT {

    private final TestRule trace = new MethodExecutionTrace();

    @Rule
    public final TestRule chain = ITUtil.createRuleChain(trace, 15, TimeUnit.SECONDS);

    SocketAddress bindTo = new LocalAddress(8123);
    SocketAddress bindTo2 = new LocalAddress(8124);
    ChannelIoAcceptor<?, ?, ?> acceptor;
    ChannelIoConnector<?, ?, ?> connector;

    // max expected milliseconds between the call to AbstractIoSession#increaseIdleCount in
    // DefaultIoFilterChain.fireSessionIdle and its call to our test filter's sessionIdle method
    static final long IDLE_TOLERANCE_MILLIS = 30;


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
                IoBufferEx buf = (IoBufferEx) message;
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
        final IoSessionEx session = (IoSessionEx) connectFuture.getSession();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        WriteFuture written = session.write(allocator.wrap(wrap(new byte[] { 0x00, 0x01, 0x02 })));

        await(written, "session.write");

        await(echoedMessageReceived, "echoedMessageReceived");
        await(session.close(true), "session close(true) future");

        assertEquals("Exceptions caught by connect handler", 0, connectExceptionsCaught.get());
        assertEquals("Exceptions caught by except handler", 0, acceptExceptionsCaught.get());
    }

    @Test
    public void testBindTwiceClientCloseFirst() throws Exception {
        testBindAndBindAgain(true);
    }

    @Test
    public void testBindTwiceServerCloseFirst() throws Exception {
        testBindAndBindAgain(false);
    }


    private void testBindAndBindAgain(boolean clientCloseFirst) throws Exception {
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
        acceptor.setPipelineFactory(pipelineFactory(pipeline(new LoggingHandler(InternalLogLevel.INFO))));
        acceptor.setFilterChainBuilder(builder);

        acceptor.setHandler(new IoHandlerAdapter() {
            @Override
            public void messageReceived(IoSession session, Object message)
                    throws Exception {
                IoBufferEx buf = (IoBufferEx) message;
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
        final IoSessionEx session = (IoSessionEx) connectFuture.getSession();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();

        WriteFuture written = session.write(allocator.wrap(wrap(new byte[] { 0x00, 0x01, 0x02 })));

        await(written, "session.write");

        await(echoedMessageReceived, "echoedMessageReceived");

        assertEquals("Exceptions caught by connect handler", 0, connectExceptionsCaught.get());
        assertEquals("Exceptions caught by accept handler", 0, acceptExceptionsCaught.get());

        if (clientCloseFirst) {
            await(session.close(true), "session close(true) future");
        }

        acceptor.unbind(bindTo);

        if (!clientCloseFirst) {
            await(session.close(true), "session close(true) future");
        }

        acceptor.bind(bindTo);
        acceptor.unbind(bindTo);
    }

    @Test
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

        acceptor.setPipelineFactory(pipelineFactory(pipeline(new LoggingHandler(InternalLogLevel.INFO))));
        acceptor.setFilterChainBuilder(builder);
        final Throwable[] bindException = new Throwable[]{null};
        final BindFuture[] boundInIoThread = new BindFuture[]{null};
        acceptor.setHandler(new IoHandlerAdapter() {
            @Override
            public void messageReceived(IoSession session, Object message)
                    throws Exception {
                IoBufferEx buf = (IoBufferEx) message;
                // Synchronous acceptor.bind call fails from an IO worker thread. But we should be able to do
                // an asynchronous bind (see KG-7179)
                try {
                    boundInIoThread[0] = acceptor.bindAsync(bindTo2);
                } catch (Throwable t) {
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
        final IoSessionEx session = (IoSessionEx) connectFuture.getSession();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();

        WriteFuture written = session.write(allocator.wrap(wrap(new byte[] { 0x00, 0x01, 0x02 })));

        await(written, "session.write");

        await(echoedMessageReceived, "echoedMessageReceived");

        assertEquals("Exceptions caught by connect handler", 0, connectExceptionsCaught.get());
        assertEquals("Exceptions caught by accept handler", 0, acceptExceptionsCaught.get());
        assertNull("acceptor.bind in acceptor IO thread threw exception " + bindException[0], bindException[0]);
        boundInIoThread[0].await();
        assertTrue("Bind in IO thread failed with exception " + boundInIoThread[0].getException(), boundInIoThread[0]
                .isBound());

        await(session.close(true), "session close(true) future");
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
        // builder.addLast("logger", new LoggingFilter());
        acceptor.setPipelineFactory(pipelineFactory(pipeline(new LoggingHandler(InternalLogLevel.INFO))));
        acceptor.setFilterChainBuilder(builder);
        final Throwable[] unbindException = new Throwable[]{null};
        final UnbindFuture[] unboundInIoThread = new UnbindFuture[]{null};
        acceptor.setHandler(new IoHandlerAdapter() {
            @Override
            public void messageReceived(IoSession session, Object message)
                    throws Exception {
                IoBufferEx buf = (IoBufferEx) message;
                // Test asynchronous acceptor.unbind call from an IO worker thread.
                try {
                    unboundInIoThread[0] = acceptor.unbindAsync(bindTo2);
                } catch (Throwable t) {
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
        final IoSessionEx session = (IoSessionEx) connectFuture.getSession();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();

        WriteFuture written = session.write(allocator.wrap(wrap(new byte[] { 0x00, 0x01, 0x02 })));

        await(written, "session.write");

        await(echoedMessageReceived, "echoedMessageReceived");

        assertEquals("Exceptions caught by connect handler", 0, connectExceptionsCaught.get());
        assertEquals("Exceptions caught by accept handler", 0, acceptExceptionsCaught.get());
        assertNull("acceptor.bind in acceptor IO thread threw exception " + unbindException[0], unbindException[0]);
        unboundInIoThread[0].await();
        assertTrue("Unbind in IO thread failed with exception " + unboundInIoThread[0].getException(), unboundInIoThread[0]
                .isUnbound());

        await(session.close(true), "session close(true) future");

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
        final List<String> fails = new ArrayList<>();
        builder.addLast("idleTimeoutTestFilter", new IoFilterAdapter() {
            private long idleTimeoutSetAt;

            @Override
            public void sessionIdle(NextFilter nextFilter, IoSession session,
                    IdleStatus status) throws Exception {
                assert status.equals(statusUnderTest);
                long idleFiredAfter = System.currentTimeMillis() - idleTimeoutSetAt;
                long configured = session.getConfig().getIdleTimeInMillis(statusUnderTest);
                System.out.println(
                    format("idleTimeoutTestFilter: sessionIdle(%s) was called %d ms after calling setIdleTimeInMillis(%d)",
                    status, idleFiredAfter, configured));
                if (idleFiredAfter + IDLE_TOLERANCE_MILLIS < configured) {
                    fails.add(
                       format("idleTimeoutTestFilter TEST FAILED: sessionIdle(%s) was called %d ms after calling "
                               + "setIdleTimeInMillis(%d), less than configured idle time %d millis",
                               status, idleFiredAfter, configured, configured));
                }
                idleFired.countDown();
                if (idleFired.getCount() > 0) {
                    System.out.println("idleTimeoutTestFilter.sessionIdle: calling setIdleTimeInMillis(200)");
                    idleTimeoutSetAt = System.currentTimeMillis();
                    ((IoSessionConfigEx) session.getConfig()).setIdleTimeInMillis(status, 200);
                }
                nextFilter.sessionIdle(session, status);
            }

            @Override
            public void messageReceived(NextFilter nextFilter, IoSession session,
                    Object message) throws Exception {
                System.out.println("idleTimeoutTestFilter.messageReceived: calling setIdleTimeInMillis(50)");
                idleTimeoutSetAt = System.currentTimeMillis();
                ((IoSessionConfigEx) session.getConfig()).setIdleTimeInMillis(statusUnderTest, 50);
                nextFilter.messageReceived(session, message);
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
        final IoSessionEx session = (IoSessionEx) connectFuture.getSession();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();

        WriteFuture written = session.write(allocator.wrap(wrap(new byte[] { 0x00, 0x01, 0x02 })));

        await(written, "session.write");

        // this is the main point of this test
        await(idleFired, "sessionIdle fired twice in idleTimeoutTestFilter");

        await(session.close(true), "session close(true) future");

        assertEquals("Exceptions caught by connector handler", 0, connectExceptionsCaught.get());
        assertEquals("Exceptions caught by acceptor handler", 0, acceptExceptionsCaught.get());
        if (!fails.isEmpty()) {
            fail("Failing test due to the following failure messages from the filter: " + fails);
        }
    }

    @Test
    public void bothIdleShouldNotFireWhenNotIdle() throws Exception {
        sessionIdleShouldNotFireWhenNotIdle(IdleStatus.BOTH_IDLE);
    }

    @Test
    public void readIdleShouldNotFireWhenNotIdle() throws Exception {
        sessionIdleShouldNotFireWhenNotIdle(IdleStatus.READER_IDLE);
    }

    private void sessionIdleShouldNotFireWhenNotIdle(final IdleStatus statusUnderTest) throws Exception {
        final long IDLE_TIME = 80;

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
        final CountDownLatch setIdleTimeCalled = new CountDownLatch(1);
        final List<String> fails = new ArrayList<>();
        builder.addLast("idleTimeoutTestFilter", new IoFilterAdapter() {
            private long idleTimeoutSetAt;
            private boolean idleTimeoutSet;

            @Override
            public void sessionIdle(NextFilter nextFilter, IoSession session,
                    IdleStatus status) throws Exception {
                assert status.equals(statusUnderTest);
                long idleFiredAfter = System.currentTimeMillis() - idleTimeoutSetAt;
                long configured = session.getConfig().getIdleTimeInMillis(statusUnderTest);
                String message =
                    format("TEST FAILED: sessionIdle(%s) was called (%d ms after calling setIdleTimeInMillis(%d))",
                    status, idleFiredAfter, configured);
                System.out.println(message);
                fails.add(message);
                nextFilter.sessionIdle(session, status);
            }

            @Override
            public void messageReceived(NextFilter nextFilter, IoSession session,
                    Object message) throws Exception {
                if (!idleTimeoutSet) {
                    idleTimeoutSet = true;
                    System.out.println(String.format("idleTimeoutTestFilter.messageReceived: calling setIdleTimeInMillis(%d)",
                            IDLE_TIME));
                    idleTimeoutSetAt = System.currentTimeMillis();
                    ((IoSessionConfigEx) session.getConfig()).setIdleTimeInMillis(statusUnderTest, IDLE_TIME);
                    setIdleTimeCalled.countDown();
                }
                nextFilter.messageReceived(session, message);
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
        final IoSessionEx session = (IoSessionEx) connectFuture.getSession();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();

        WriteFuture written = session.write(allocator.wrap(wrap(new byte[] { 0x00, 0x01, 0x02 })));

        await(written, "session.write");

        await(setIdleTimeCalled, "setIdleTime called in idleTimeoutTestFilter.messageReceived");

        // If we keep sending messages at interval less than configured idle time then no
        // session read or both idle events should be fired
        for (int i = 0; i < 15; i++) {
            Thread.sleep(IDLE_TIME / 5);
            session.write(allocator.wrap(wrap(new byte[] { 0x03, 0x04, 0x05 })));
        }

        await(session.close(true), "session close(true) future");

        assertEquals("Exceptions caught by connector handler", 0, connectExceptionsCaught.get());
        assertEquals("Exceptions caught by acceptor handler", 0, acceptExceptionsCaught.get());
        if (!fails.isEmpty()) {
            fail("Failing test due to the following failure messages from the filter: " + fails);
        }
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
