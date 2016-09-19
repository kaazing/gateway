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
package org.kaazing.gateway.transport.nio.internal;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IoSession;
import org.jmock.Mockery;
import org.jmock.Sequence;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.test.Expectations;
import org.kaazing.test.util.MethodExecutionTrace;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.kaazing.gateway.resource.address.ResourceAddressFactory.newResourceAddressFactory;

public class NioDatagramAcceptorTest {

    @Rule
    public TestRule testExecutionTrace = new MethodExecutionTrace();

    @Rule
    public TestRule timeout = new DisableOnDebug(new Timeout(10, TimeUnit.SECONDS));

    @Test
    public void echo() throws Exception {
        Mockery context = new Mockery();
        context.setThreadingPolicy(new Synchroniser());
        IoHandler handler = context.mock(IoHandler.class);

        ResourceAddressFactory addressFactory = ResourceAddressFactory.newResourceAddressFactory();
        ResourceAddress bindAddress = addressFactory.newResourceAddress("udp://localhost:8080", new HashMap<>());

        Properties configuration = new Properties();
        NioSocketAcceptor tcpAcceptor = new NioSocketAcceptor(configuration);
        NioDatagramAcceptor acceptor = new NioDatagramAcceptor(configuration);
        acceptor.setResourceAddressFactory(newResourceAddressFactory());
        acceptor.setTcpAcceptor(tcpAcceptor);

        acceptor.bind(bindAddress, handler, null);
        String str = "Hello World";
        byte[] bytes = str.getBytes(UTF_8);

        Sequence order = context.sequence("order");
        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSession.class))); inSequence(order);
                will(saveParameter("session", 0));
                oneOf(handler).sessionOpened(with(variable("session", IoSession.class))); inSequence(order);
                oneOf(handler).messageReceived(with(variable("session", IoSession.class)), with(ioBufferMatching(bytes))); inSequence(order);
                will(new CustomAction("Send data.") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        IoSession session = (IoSession) invocation.getParameter(0);
                        IoBuffer buffer = (IoBuffer) invocation.getParameter(1);
                        session.write(buffer);
                        return null;
                    }
                });
                oneOf(handler).sessionClosed(with(variable("session", IoSession.class))); inSequence(order);
                allowing(handler).messageSent(with(variable("session", IoSession.class)), with(any(Object.class)));
            }
        });

        DatagramSocket udpClient = new DatagramSocket();
        udpClient.connect(new InetSocketAddress("localhost", 8080));

        byte[] buf = str.getBytes(UTF_8);
        DatagramPacket dp = new DatagramPacket(buf, buf.length);
        udpClient.send(dp);
        buf = new byte[20];
        dp = new DatagramPacket(buf, 0, buf.length);
        udpClient.receive(dp);
        String got = new String(dp.getData(), dp.getOffset(), dp.getLength(), UTF_8);
        assertEquals(str, got);

        udpClient.close();

        acceptor.dispose();

        context.assertIsSatisfied();
    }

}
