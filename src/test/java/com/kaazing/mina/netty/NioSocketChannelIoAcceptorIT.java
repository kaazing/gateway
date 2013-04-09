/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import static com.kaazing.mina.netty.PortUtil.nextPort;
import static java.lang.Thread.currentThread;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.future.DefaultWriteFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.DefaultWriteRequest;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.filter.logging.LoggingFilter;
import org.jboss.netty.channel.socket.nio.BossPool;
import org.jboss.netty.channel.socket.nio.NioServerBoss;
import org.jboss.netty.channel.socket.nio.NioServerBossPool;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioWorker;
import org.jboss.netty.channel.socket.nio.NioWorkerPool;
import org.jboss.netty.channel.socket.nio.WorkerPool;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.kaazing.mina.netty.socket.DefaultSocketChannelIoSessionConfig;
import com.kaazing.mina.netty.socket.SocketChannelIoSessionConfig;
import com.kaazing.mina.netty.socket.nio.NioSocketChannelIoAcceptor;

/**
 * NioSocketChannelIoAcceptor integration test.
 */
public class NioSocketChannelIoAcceptorIT {

    private IoAcceptor acceptor;
    private Socket socket;

    @Before
    public void initResources() throws Exception {
        WorkerPool<NioWorker> workerPool = new NioWorkerPool(newCachedThreadPool(), 3);
        BossPool<NioServerBoss> bossPool = new NioServerBossPool(newCachedThreadPool(), 1);
        NioServerSocketChannelFactory serverChannelFactory = new NioServerSocketChannelFactory(bossPool, workerPool);
        SocketChannelIoSessionConfig sessionConfig = new DefaultSocketChannelIoSessionConfig();
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

    @Test //(timeout = 1000)
    public void shouldEchoBytes() throws Exception {
        final AtomicInteger exceptionsCaught = new AtomicInteger();
        acceptor.setHandler(new IoHandlerAdapter() {
            @Override
            public void messageReceived(IoSession session, Object message)
                    throws Exception {
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
        byte[] sendPayload = new byte[] { 0x00, 0x01, 0x02 };
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

    private void assertNoWorkerThreads(String when) {
        Thread[] threads = new Thread[Thread.activeCount()];
        Thread.enumerate(threads);
        int workersFound = 0;
        int bossesFound = 0;
        System.out.println("List of active threads " + when + ":");
        for (Thread thread: threads) {
            System.out.println(thread.getName());
            if (thread.getName().matches(".*I/O worker.*")) {
                workersFound++;
            }
            if (thread.getName().matches(".*boss")) {
                workersFound++;
            }
        }
        assertTrue(String.format("No worker or boss threads should be running %s, found %d workers, %d bosses",
                when, workersFound, bossesFound), workersFound == 0 && bossesFound == 0);
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
            public void sessionIdle(IoSession session, IdleStatus status)
                    throws Exception {
                Object affinity = session.getAttribute("affinity");
                assertTrue("thread aligned", affinity == currentThread());
            }

            @Override
            public void messageReceived(IoSession session, Object message)
                    throws Exception {
                Object affinity = session.getAttribute("affinity");
                assertTrue("thread aligned", affinity == currentThread());
            }

            @Override
            public void messageSent(IoSession session, Object message)
                    throws Exception {
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

        byte[] sendPayload = new byte[] { 0x00, 0x01, 0x02 };
        socket.connect(bindAddress);
        OutputStream output = socket.getOutputStream();
        output.write(sendPayload);

        assertEquals("no handler exceptions", 0, exceptionsCaught.get());
    }

}
