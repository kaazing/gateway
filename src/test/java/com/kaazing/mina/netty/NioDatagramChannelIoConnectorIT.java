/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.BufferOverflowException;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.logging.LoggingFilter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.kaazing.mina.netty.socket.DefaultDatagramChannelIoSessionConfig;
import com.kaazing.mina.netty.socket.nio.NioDatagramChannelIoConnector;

/**
 * Integration test for mina.netty layer. Similar to IT, but for datagram transport.
 */
public class NioDatagramChannelIoConnectorIT {

    private ExecutorService executor;
    private ServerSocket acceptor;
    private Socket accepted;
    private IoConnector connector;

    @Before
    public void initAcceptor() throws Exception {
        executor = Executors.newFixedThreadPool(1);
        acceptor = new ServerSocket();
        connector = new NioDatagramChannelIoConnector(new DefaultDatagramChannelIoSessionConfig());
        connector.getFilterChain().addLast("logger", new LoggingFilter());
    }

    @After
    public void disposeAcceptor() throws Exception {
        if (acceptor != null) {
            acceptor.close();
        }
        if (accepted != null) {
            accepted.close();
        }
        if (executor != null) {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
        if (connector != null) {
            connector.dispose();
        }
    }

    @Test (timeout = 1000)
    public void shouldEchoBytes() throws Exception {
        byte[] sendPayload = new byte[] { 0x00, 0x01, 0x02 };
        final byte[] receivePayload = new byte[sendPayload.length];

        final SocketAddress bindAddress = new InetSocketAddress("localhost", 8123);

        Callable<Void> echoTask = new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                byte[] payload = new byte[32];
                acceptor.bind(bindAddress);
                accepted = acceptor.accept();
                InputStream input = accepted.getInputStream();
                OutputStream output = accepted.getOutputStream();
                int bytesRead = input.read(payload);
                output.write(payload, 0, bytesRead);
                output.close();
                input.close();
                return null;
            }
        };
        executor.submit(echoTask);

        final AtomicInteger exceptionsCaught = new AtomicInteger();
        connector.setHandler(new IoHandlerAdapter() {
            @Override
            public void messageReceived(IoSession session, Object message)
                    throws Exception {
                IoBuffer buf = (IoBuffer) message;
                buf.get(receivePayload);

                if (buf.hasRemaining()) {
                    throw new BufferOverflowException();
                }

                session.close(false);
            }

            @Override
            public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
                exceptionsCaught.incrementAndGet();
            }
        });

        ConnectFuture connect = connector.connect(bindAddress);
        connect.await();
        IoSession session = connect.getSession();
        session.write(IoBuffer.wrap(sendPayload));
        session.getCloseFuture().await();

        assertTrue("payload echoed", Arrays.equals(sendPayload, receivePayload));
        assertEquals("no handler exceptions", 0, exceptionsCaught.get());
    }

}
