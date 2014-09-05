/**
 * Copyright (c) 2007-2014, Kaazing Corporation. All rights reserved.
 */

package org.apache.mina.transport.socket.nio;


import static com.kaazing.junit.matchers.JUnitMatchers.instanceOf;
import static com.kaazing.mina.netty.PortUtil.nextPort;

import java.net.DatagramSocket;
import java.net.InetSocketAddress;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IoSession;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.kaazing.mina.core.session.IoSessionEx;

public class NioDatagramConnectorExIT {

    private NioDatagramConnectorEx connector;

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery() {
        {
            setThreadingPolicy(new Synchroniser());
        }
    };


    @Before
    public void before() {
        connector = new NioDatagramConnectorEx(1);
    }

    @After
    public void after() throws Exception {
        context.assertIsSatisfied();
    }

    @Test
    public void shouldConnect() throws Exception {

        final IoHandler handler = context.mock(IoHandler.class);

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(instanceOf(IoSessionEx.class)));
                oneOf(handler).sessionOpened(with(instanceOf(IoSessionEx.class)));
                oneOf(handler).sessionClosed(with(instanceOf(IoSessionEx.class)));
            }
        });

        InetSocketAddress bindAddress = new InetSocketAddress("127.0.0.1", nextPort(2100, 100));

        DatagramSocket socket = new DatagramSocket(bindAddress);

        connector.setHandler(handler);
        ConnectFuture future = connector.connect(bindAddress);
        IoSession session = future.await().getSession();
        session.close(false).await();

        socket.close();
    }
}
