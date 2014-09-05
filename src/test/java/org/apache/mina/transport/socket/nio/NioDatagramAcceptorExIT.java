/**
 * Copyright (c) 2007-2014, Kaazing Corporation. All rights reserved.
 */

package org.apache.mina.transport.socket.nio;


import static com.kaazing.junit.matchers.JUnitMatchers.instanceOf;
import static com.kaazing.mina.core.buffer.SimpleBufferAllocator.BUFFER_ALLOCATOR;
import static com.kaazing.mina.netty.PortUtil.nextPort;
import static java.nio.ByteBuffer.wrap;
import static org.jboss.netty.util.CharsetUtil.UTF_8;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

import org.apache.mina.core.service.IoHandler;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.kaazing.mina.core.session.IoSessionEx;

public class NioDatagramAcceptorExIT {

    private NioDatagramAcceptorEx acceptor;

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery() {
        {
            setThreadingPolicy(new Synchroniser());
        }
    };

    @Before
    public void before() {
        acceptor = new NioDatagramAcceptorEx();
    }

    @After
    public void after() throws Exception {
        acceptor.dispose();
    }

    @Test
    public void shouldConnect() throws Exception {

        final IoHandler handler = context.mock(IoHandler.class);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(instanceOf(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(instanceOf(IoSessionEx.class)));
                oneOf(handler).messageReceived(with(instanceOf(IoSessionEx.class)),
                                           with(equal(BUFFER_ALLOCATOR.wrap(wrap("text".getBytes(UTF_8))))));
                oneOf(handler).sessionClosed(with(instanceOf(IoSessionEx.class)));
            }
        });

        InetSocketAddress bindAddress = new InetSocketAddress("127.0.0.1", nextPort(2100, 100));

        acceptor.setHandler(handler);
        acceptor.bind(bindAddress);

        DatagramSocket socket = new DatagramSocket();
        socket.connect(bindAddress);
        socket.send(new DatagramPacket("text".getBytes(UTF_8), 4));
        socket.close();

        acceptor.unbind();
    }
}
