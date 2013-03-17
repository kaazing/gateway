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
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
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
