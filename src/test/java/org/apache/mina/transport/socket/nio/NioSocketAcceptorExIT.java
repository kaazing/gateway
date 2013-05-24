/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package org.apache.mina.transport.socket.nio;


import static com.kaazing.junit.matchers.JUnitMatchers.instanceOf;
import static com.kaazing.mina.core.session.IoSessionEx.BUFFER_ALLOCATOR;
import static com.kaazing.mina.netty.PortUtil.nextPort;
import static java.nio.ByteBuffer.wrap;
import static org.jboss.netty.util.CharsetUtil.UTF_8;
import static org.jmock.lib.script.ScriptedAction.perform;
import static org.junit.Assert.assertEquals;

import java.net.InetSocketAddress;
import java.net.Socket;

import org.apache.mina.core.service.IoHandler;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.kaazing.mina.core.session.IoSessionEx;

public class NioSocketAcceptorExIT {

    private NioSocketAcceptorEx acceptor;

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery() {
        {
            setThreadingPolicy(new Synchroniser());
        }
    };

    @Before
    public void before() {
        acceptor = new NioSocketAcceptorEx(1);
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
                will(perform("$0.close(false); return;"));
                oneOf(handler).sessionClosed(with(instanceOf(IoSessionEx.class)));
            }
        });

        InetSocketAddress bindAddress = new InetSocketAddress("127.0.0.1", nextPort(2100, 100));

        acceptor.setHandler(handler);
        acceptor.bind(bindAddress);

        Socket socket = new Socket();
        socket.connect(bindAddress);
        socket.getOutputStream().write("text".getBytes(UTF_8));

        int eos = socket.getInputStream().read();
        assertEquals(-1, eos);

        acceptor.unbind();
    }
}
