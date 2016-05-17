/**
 * Copyright 2007-2016, Kaazing Corporation. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.mina.transport.socket.nio;


import static org.kaazing.junit.matchers.JUnitMatchers.instanceOf;
import static org.kaazing.mina.netty.PortUtil.nextPort;

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

import org.kaazing.mina.core.session.IoSessionEx;

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

                // The session close future is fired before calling handler.sessionClosed so
                // this next expectation may not be completed before the test ends
                atMost(1).of(handler).sessionClosed(with(instanceOf(IoSessionEx.class)));
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
