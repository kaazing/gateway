/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package org.apache.mina.transport.socket.nio;


import static com.kaazing.junit.matchers.JUnitMatchers.instanceOf;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

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

public class NioSocketConnectorExIT {

    private NioSocketConnectorEx connector;

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery() {
        {
            setThreadingPolicy(new Synchroniser());
        }
    };


    @Before
    public void before() {
        connector = new NioSocketConnectorEx(1);
    }

    @After
    public void after() throws Exception {
        connector.dispose();
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

        ServerSocket server = new ServerSocket();
        server.bind(new InetSocketAddress("127.0.0.1", 2124));

        connector.setHandler(handler);
        ConnectFuture future = connector.connect(new InetSocketAddress("127.0.0.1", 2124));
        IoSession session = future.await().getSession();

        Socket accepted = server.accept();
        accepted.close();

        session.getCloseFuture().await();

        server.close();
    }
}
