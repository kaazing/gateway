/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package org.apache.mina.transport.socket.nio;


import static com.kaazing.junit.matchers.JUnitMatchers.instanceOf;
import static org.apache.mina.core.buffer.IoBuffer.wrap;
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
                oneOf(handler).messageReceived(with(instanceOf(IoSessionEx.class)), with(equal(wrap("text".getBytes(UTF_8)))));
                oneOf(handler).sessionClosed(with(instanceOf(IoSessionEx.class)));
            }
        });

        acceptor.setHandler(handler);
        acceptor.bind(new InetSocketAddress("127.0.0.1", 2121));

        DatagramSocket socket = new DatagramSocket();
        socket.connect(new InetSocketAddress("127.0.0.1", 2121));
        socket.send(new DatagramPacket("text".getBytes(UTF_8), 4));
        socket.close();

        acceptor.unbind();
    }
}
