/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import static com.kaazing.mina.netty.PortUtil.nextPort;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.logging.LoggingFilter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.kaazing.mina.netty.socket.DatagramChannelIoSessionConfig;
import com.kaazing.mina.netty.socket.DefaultDatagramChannelIoSessionConfig;
import com.kaazing.mina.netty.socket.nio.NioDatagramChannelIoAcceptor;

/**
 * Integration test for mina.netty layer. Similar to IT, but for datagram transport.
 */
public class NioDatagramChannelIoAcceptorIT {

    private IoAcceptor acceptor;
    private DatagramSocket socket;

    @Before
    public void initResources() throws Exception {
        DatagramChannelIoSessionConfig sessionConfig = new DefaultDatagramChannelIoSessionConfig();
        sessionConfig.setReuseAddress(true);
        acceptor = new NioDatagramChannelIoAcceptor(sessionConfig);
        acceptor.getFilterChain().addLast("logger", new LoggingFilter());
        socket = new DatagramSocket();
        socket.setReuseAddress(true);
    }

    @After
    public void disposeResources() throws Exception {
        if (socket != null) {
            socket.close();
        }
        if (acceptor != null) {
            acceptor.dispose();
        }
    }

    @Test (timeout = 1000)
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

        byte[] sendPayload = new byte[] { 0x00, 0x01, 0x02 };
        DatagramPacket sendPacket = new DatagramPacket(sendPayload, sendPayload.length);
        socket.connect(bindAddress);
        socket.send(sendPacket);
        byte[] receivePayload = new byte[sendPayload.length];
        DatagramPacket receivePacket = new DatagramPacket(receivePayload, receivePayload.length);
        socket.receive(receivePacket);

        assertTrue("payload echoed", Arrays.equals(sendPayload, receivePayload));
        assertEquals("no handler exceptions", 0, exceptionsCaught.get());
    }

}
