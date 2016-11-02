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

import static org.kaazing.junit.matchers.JUnitMatchers.instanceOf;
import static org.kaazing.mina.netty.PortUtil.nextPort;
import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static org.apache.mina.core.session.IdleStatus.BOTH_IDLE;
import static org.apache.mina.core.session.IdleStatus.READER_IDLE;
import static org.apache.mina.core.session.IdleStatus.WRITER_IDLE;
import static org.jmock.lib.script.ScriptedAction.perform;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;
import org.apache.mina.filter.logging.LoggingFilter;
import org.jboss.netty.channel.socket.nio.BossPool;
import org.jboss.netty.channel.socket.nio.NioServerBoss;
import org.jboss.netty.channel.socket.nio.NioServerBossPool;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioWorker;
import org.jboss.netty.channel.socket.nio.NioWorkerPool;
import org.jboss.netty.channel.socket.nio.WorkerPool;
import org.jmock.Expectations;
import org.jmock.api.Invocation;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.jmock.lib.action.CustomAction;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.mina.netty.socket.nio.DefaultNioSocketChannelIoSessionConfig;
import org.kaazing.mina.netty.socket.nio.NioSocketChannelIoAcceptor;

/**
 * NioSocketChannelIoAcceptor integration test.
 */
public class NioSocketChannelIoAcceptorIT {

    private ChannelIoAcceptor<?, ?, ?> acceptor;
    private Socket socket;

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery() {
        {
            setThreadingPolicy(new Synchroniser());
        }
    };

    @Before
    public void initResources() throws Exception {
        WorkerPool<NioWorker> workerPool = new NioWorkerPool(newCachedThreadPool(), 3);
        BossPool<NioServerBoss> bossPool = new NioServerBossPool(newCachedThreadPool(), 1);
        NioServerSocketChannelFactory serverChannelFactory = new NioServerSocketChannelFactory(bossPool, workerPool);
        DefaultNioSocketChannelIoSessionConfig sessionConfig = new DefaultNioSocketChannelIoSessionConfig();
        sessionConfig.setReuseAddress(true);
        acceptor = new NioSocketChannelIoAcceptor(sessionConfig, serverChannelFactory);
        acceptor.getFilterChain().addLast("logger", new LoggingFilter());
        socket = new Socket();
    }

    @After
    public void disposeResources() throws Exception {
        if (socket != null) {
            socket.close();
            socket = null;
        }
        if (acceptor != null) {
            acceptor.dispose();
            acceptor = null;
        }
    }

    @Test
    public void shouldRebind() throws Exception {
        SocketAddress bindAddress = new InetSocketAddress("localhost", nextPort(8100, 100));
        acceptor.setHandler(new IoHandlerAdapter());
        acceptor.bind(bindAddress);
        acceptor.unbind(bindAddress);
        acceptor.bind(bindAddress);
    }

    @Test
    // (timeout = 1000)
    public void shouldCountScheduledAndWrittenBytes() throws Exception {
        final List<Throwable> exceptionsCaught = Collections.synchronizedList(new ArrayList<>());
        acceptor.setHandler(new IoHandlerAdapter() {
            private final AtomicInteger written = new AtomicInteger(0);

            @Override
            public void messageReceived(final IoSession session, Object message) throws Exception {
                IoBuffer buf = (IoBuffer) message;
                final int expectedWrittenBytes = written.addAndGet(buf.remaining());
                session.write(buf.duplicate()).addListener(new IoFutureListener<WriteFuture>() {
                    @Override
                    public void operationComplete(WriteFuture future) {
                        try {
                            assertEquals("getScheduledWriteBytes", 0, session.getScheduledWriteBytes());
                        }
                        catch (Throwable t) {
                            exceptionsCaught.add(t);
                        }
                        try {
                            assertEquals("getWrittenBytes", expectedWrittenBytes, session.getWrittenBytes());
                        }
                        catch (Throwable t) {
                            exceptionsCaught.add(t);
                        }
                    }
                });
            }

            @Override
            public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
                exceptionsCaught.add(cause);
            }
        });

        SocketAddress bindAddress = new InetSocketAddress("localhost", nextPort(8100, 100));
        acceptor.bind(bindAddress);

        socket.connect(bindAddress);
        OutputStream output = socket.getOutputStream();
        InputStream input = socket.getInputStream();
        byte[] sendPayload = new byte[]{0x00, 0x01, 0x02};
        output.write(sendPayload);
        byte[] receivePayload = new byte[sendPayload.length];
        input.read(receivePayload);
        assertTrue("payload echoed", Arrays.equals(sendPayload, receivePayload));

        sendPayload = new byte[]{0x03, 0x04, 0x05, 0x7};
        output.write(sendPayload);
        receivePayload = new byte[sendPayload.length];
        input.read(receivePayload);
        assertTrue("payload echoed", Arrays.equals(sendPayload, receivePayload));

        assertTrue(format("Got handler exceptions: %s", exceptionsCaught), 0 == exceptionsCaught.size());
    }

    @Test
    // (timeout = 1000)
    public void shouldEchoBytes() throws Exception {
        final AtomicInteger exceptionsCaught = new AtomicInteger();
        acceptor.setHandler(new IoHandlerAdapter() {
            @Override
            public void messageReceived(IoSession session, Object message) throws Exception {
                IoBuffer buf = (IoBuffer) message;
                session.write(buf.duplicate());
            }

            @Override
            public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
                exceptionsCaught.incrementAndGet();
            }
        });

        SocketAddress bindAddress = new InetSocketAddress("localhost", nextPort(8100, 100));
        acceptor.bind(bindAddress);

        socket.connect(bindAddress);
        OutputStream output = socket.getOutputStream();
        InputStream input = socket.getInputStream();
        byte[] sendPayload = new byte[]{0x00, 0x01, 0x02};
        output.write(sendPayload);
        byte[] receivePayload = new byte[sendPayload.length];
        input.read(receivePayload);

        assertTrue("payload echoed", Arrays.equals(sendPayload, receivePayload));
        assertEquals("no handler exceptions", 0, exceptionsCaught.get());
    }

    @Test
    public void disposeShouldStopAll_IO_Threads() throws Exception {
        shouldEchoBytes();
        disposeResources();
        Thread.sleep(1000); // experience shows Timer.cancel() does not immediately stop the timer thread
        assertNoWorkerThreads("after disposeResources");
    }

    @Test
    public void channelClosedShouldNotFireFilterClose() throws Exception {
        final AtomicInteger exceptionsCaught = new AtomicInteger();
        final AtomicInteger filterCloseCalls = new AtomicInteger();
        final AtomicInteger sessionClosedCalls = new AtomicInteger();
        final CountDownLatch done = new CountDownLatch(1);
        acceptor.setHandler(new IoHandlerAdapter() {
            @Override
            public void sessionOpened(IoSession session) throws Exception {
                // Add filter with filterClose method which writes to the session
                session.getFilterChain().addFirst("writeInFilterClose", new IoFilterAdapter() {

                    @Override
                    public void filterClose(NextFilter nextFilter, IoSession session) throws Exception {
                        filterCloseCalls.incrementAndGet();
                        super.filterClose(nextFilter, session);
                        done.countDown();
                    }

                });
            }

            @Override
            public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
                exceptionsCaught.incrementAndGet();
            }

            @Override
            public void sessionClosed(IoSession session) throws Exception {
                sessionClosedCalls.incrementAndGet();
                super.sessionClosed(session);
                done.countDown();
            }
        });

        SocketAddress bindAddress = new InetSocketAddress("localhost", nextPort(8100, 100));
        acceptor.bind(bindAddress);
        socket.connect(bindAddress);
        socket.close();
        done.await();
        System.out.println(String.format("Call totals: %d exceptionCaught, %d filterClose, %d sessionClosed",
                exceptionsCaught.get(), filterCloseCalls.get(), sessionClosedCalls.get()));
        assertEquals("exceptionsCaught", 0, exceptionsCaught.get());
        assertEquals("filterCloseCalls", 0, filterCloseCalls.get());
        assertEquals("sessionClosedCalls", 1, sessionClosedCalls.get());
    }

    @Test
    public void channelOpenedShouldInitializeSessionBeforeFiringSessionCreated() throws Exception {
        final CountDownLatch done = new CountDownLatch(1);
        final List<String> actions = Collections.synchronizedList(new ArrayList<>());
        acceptor.setIoSessionInitializer(new IoSessionInitializer<IoFuture>() {
            @Override
            public void initializeSession(IoSession session, IoFuture future) {
                actions.add("initializer");
                // Add filter with sessionCreated method. This should get fired.
                session.getFilterChain().addFirst("sessionCreatedTest", new IoFilterAdapter() {

                    @Override
                    public void sessionCreated(NextFilter nextFilter, IoSession session) throws Exception {
                        actions.add("filter.sessionCreated");
                        super.sessionCreated(nextFilter, session);
                    }

                });
            }
        });
        acceptor.setHandler(new IoHandlerAdapter() {
            @Override
            public void sessionCreated(IoSession session) throws Exception {
                actions.add("handler.sessionCreated");
                done.countDown();
            }
        });
        SocketAddress bindAddress = new InetSocketAddress("localhost", nextPort(8100, 100));
        acceptor.bind(bindAddress);
        socket.connect(bindAddress);
        socket.close();
        done.await();
        System.out.println("Actions, in order, were: " + actions);
        assertEquals("initializer", actions.get(0));
        assertEquals("filter.sessionCreated", actions.get(1));
    }

    private void assertNoWorkerThreads(String when) {
        Thread[] threads = new Thread[Thread.activeCount()];
        Thread.enumerate(threads);
        int workersFound = 0;
        int bossesFound = 0;
        List<String> badThreads = new LinkedList<>();
        for (Thread thread : threads) {
            System.out.println(thread.getName());
            if (thread.getName().matches(".*I/O worker.*")) {
                badThreads.add(thread.getName());
                workersFound++;
            }
            if (thread.getName().matches(".*boss")) {
                badThreads.add(thread.getName());
                bossesFound++;
            }
        }
        assertTrue(String.format("No worker or boss threads should be running %s, found %d workers, %d bosses: %s",
                when, workersFound, bossesFound, badThreads), workersFound == 0 && bossesFound == 0);
    }

    @Test(timeout = 5000)
    public void shouldNotIdleTimeoutWhenBothNotIdle() throws Exception {
        shouldNotIdleTimeoutWhenNotIdle(BOTH_IDLE);
    }

    @Test(timeout = 5000)
    public void shouldNotIdleTimeoutWhenReaderNotIdle() throws Exception {
        shouldNotIdleTimeoutWhenNotIdle(READER_IDLE);
    }

    @Test(timeout = 5000)
    public void shouldNotIdleTimeoutWhenWriterNotIdle() throws Exception {
        shouldNotIdleTimeoutWhenNotIdle(WRITER_IDLE);
    }

    private void shouldNotIdleTimeoutWhenNotIdle(final IdleStatus statusUnderTest) throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);
        // context.setThreadingPolicy(new Synchroniser());
        final boolean[] sessonIdleCalled = new boolean[]{false};

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(instanceOf(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(instanceOf(IoSessionEx.class)));
                will(perform("$0.getConfig().setIdleTimeInMillis(status, 1000L); return;").where("status",
                        statusUnderTest));

                oneOf(handler).messageReceived(with(instanceOf(IoSessionEx.class)), with(any(Object.class)));
                // will(perform("$0.write($0.getBufferAllocator().wrap(ByteBuffer.wrap(new byte[]{0x41, 0x42}))); return;"));
                will(perform("$0.write($1); return;"));

                oneOf(handler).messageReceived(with(instanceOf(IoSessionEx.class)), with(any(Object.class)));
                will(perform("$0.close(false); return;"));

                oneOf(handler).sessionClosed(with(instanceOf(IoSessionEx.class)));

                allowing(handler).sessionIdle(with(instanceOf(IoSessionEx.class)), with(statusUnderTest));
                will(new CustomAction("fail the test") {

                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        sessonIdleCalled[0] = true;
                        return null;
                    }

                });
            }
        });

        InetSocketAddress bindAddress = new InetSocketAddress("127.0.0.1", nextPort(2100, 100));

        acceptor.setHandler(handler);
        acceptor.bind(bindAddress);

        Socket socket = new Socket();
        socket.connect(bindAddress);

        // Cause IO half way through idle timeout. Should prevent sessionIdle from firing within the next idle time
        // period.
        Thread.sleep(500);
        socket.getOutputStream().write(new byte[]{0x41});

        int read = socket.getInputStream().read();
        assertEquals(0x41, read);

        // Sleep past first idle time period
        Thread.sleep(600);

        if (sessonIdleCalled[0]) {
            fail("sessionIdle should not be called, for idle status " + statusUnderTest);
        }

        // This message should cause close
        socket.getOutputStream().write(new byte[]{0x42});

        int eos = socket.getInputStream().read();
        assertEquals(-1, eos);

        acceptor.unbind();
    }

    @Test(timeout = 5000)
    public void shouldRepeatedlyIdleTimeoutWhenBothIdle() throws Exception {
        shouldRepeatedlyIdleTimeoutWhenIdle(BOTH_IDLE);
    }

    @Test(timeout = 5000)
    public void shouldRepeatedlyIdleTimeoutWhenReaderIdle() throws Exception {
        shouldRepeatedlyIdleTimeoutWhenIdle(READER_IDLE);
    }

    @Test(timeout = 5000)
    public void shouldRepeatedlyIdleTimeoutWhenWriterIdle() throws Exception {
        shouldRepeatedlyIdleTimeoutWhenIdle(WRITER_IDLE);
    }

    private void shouldRepeatedlyIdleTimeoutWhenIdle(final IdleStatus statusUnderTest) throws Exception {
        final IoHandler handler = context.mock(IoHandler.class);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(instanceOf(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(instanceOf(IoSessionEx.class)));
                will(perform("$0.getConfig().setIdleTimeInMillis(status, 50L); return;").where("status",
                        statusUnderTest));
                atLeast(1).of(handler).sessionIdle(with(instanceOf(IoSessionEx.class)), with(statusUnderTest));
                will(perform("$0.close(false); return;"));
                oneOf(handler).sessionClosed(with(instanceOf(IoSessionEx.class)));
            }
        });

        InetSocketAddress bindAddress = new InetSocketAddress("127.0.0.1", nextPort(2100, 100));

        acceptor.setHandler(handler);
        acceptor.bind(bindAddress);

        Socket socket = new Socket();
        socket.connect(bindAddress);

        int eos = socket.getInputStream().read();
        assertEquals(-1, eos);

        acceptor.unbind();
    }

    @Test
    public void shouldReceiveBytesThreadAligned() throws Exception {

        final AtomicInteger exceptionsCaught = new AtomicInteger();
        acceptor.setHandler(new IoHandlerAdapter() {

            @Override
            public void sessionCreated(IoSession session) throws Exception {
                session.setAttribute("affinity", currentThread());

                Object affinity = session.getAttribute("affinity");
                assertTrue("thread aligned", affinity == currentThread());
            }

            @Override
            public void sessionOpened(IoSession session) throws Exception {
                Object affinity = session.getAttribute("affinity");
                assertTrue("thread aligned", affinity == currentThread());
            }

            @Override
            public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
                Object affinity = session.getAttribute("affinity");
                assertTrue("thread aligned", affinity == currentThread());
            }

            @Override
            public void messageReceived(IoSession session, Object message) throws Exception {
                Object affinity = session.getAttribute("affinity");
                assertTrue("thread aligned", affinity == currentThread());
            }

            @Override
            public void messageSent(IoSession session, Object message) throws Exception {
                Object affinity = session.getAttribute("affinity");
                assertTrue("thread aligned", affinity == currentThread());
            }

            @Override
            public void sessionClosed(IoSession session) throws Exception {
                Object affinity = session.getAttribute("affinity");
                assertTrue("thread aligned", affinity == currentThread());
            }

            @Override
            public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
                exceptionsCaught.incrementAndGet();

                Object affinity = session.getAttribute("affinity");
                assertTrue("thread aligned", affinity == currentThread());
            }

        });

        SocketAddress bindAddress = new InetSocketAddress("localhost", nextPort(8100, 100));
        acceptor.bind(bindAddress);

        byte[] sendPayload = new byte[]{0x00, 0x01, 0x02};
        socket.connect(bindAddress);
        OutputStream output = socket.getOutputStream();
        output.write(sendPayload);

        assertEquals("no handler exceptions", 0, exceptionsCaught.get());
    }

}
