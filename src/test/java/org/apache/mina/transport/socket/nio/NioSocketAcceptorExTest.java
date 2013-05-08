/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package org.apache.mina.transport.socket.nio;


import static com.kaazing.junit.matchers.JUnitMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.jmock.lib.script.ScriptedAction.perform;

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

public class NioSocketAcceptorExTest {

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
                will(perform("$0.close(false); return;"));
                oneOf(handler).sessionClosed(with(instanceOf(IoSessionEx.class)));
            }
        });

        acceptor.setHandler(handler);
        acceptor.bind(new InetSocketAddress("127.0.0.1", 2122));

        Socket socket = new Socket();
        socket.connect(new InetSocketAddress("127.0.0.1", 2122));

        int eos = socket.getInputStream().read();
        assertEquals(-1, eos);

        acceptor.unbind();
    }
}
