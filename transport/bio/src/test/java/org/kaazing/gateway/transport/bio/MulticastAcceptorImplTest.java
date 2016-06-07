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
package org.kaazing.gateway.transport.bio;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoHandler;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Test;

public class MulticastAcceptorImplTest {

    private static final InetAddress BIND_ADDRESS;
    private static final int GROUP_PORT = 10101;
    private static final String GROUP_ADDRESS = "224.0.0.1";

    static {
        try {
            // Lookup the first non-loopback InetAddress for bind
            // Note: loopback fails multicast test on Windows
            InetAddress bindAddress = null;
            for (NetworkInterface netiface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!"lo".equals(netiface.getName())) {
                    List<InetAddress> bindAddresses = Collections.list(netiface.getInetAddresses());
                    if (!bindAddresses.isEmpty()) {
                        bindAddress = bindAddresses.get(0);
                        break;
                    }
                }
            }
            BIND_ADDRESS = bindAddress;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void avoidNoRunnableMethodsError() {
    }

//    @Test
    public void testBindGroupAddressWithPort() throws Exception {
        Mockery context = new Mockery();
        final IoHandler handler = context.mock(IoHandler.class);
        final byte[] message = new byte[] { 'H', 'E', 'L', 'L', 'O' };

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(aNonNull(MulticastSession.class)));
                oneOf(handler).sessionOpened(with(aNonNull(MulticastSession.class)));
                oneOf(handler).messageReceived(with(aNonNull(MulticastSession.class)), with(equal(IoBuffer.wrap(message))));
                oneOf(handler).sessionClosed(with(aNonNull(MulticastSession.class)));
            }
        });

        MulticastAcceptorImpl acceptor = new MulticastAcceptorImpl();
        acceptor.setHandler(handler);

        InetAddress groupAddress = InetAddress.getByName(GROUP_ADDRESS);
        InetSocketAddress bindAddress = new InetSocketAddress(GROUP_PORT);
        NetworkInterface device = NetworkInterface.getByInetAddress(bindAddress.getAddress());
        MulticastAddress localAddress = new MulticastAddress(groupAddress, device, GROUP_PORT);
        acceptor.bind(localAddress);

        InetSocketAddress remoteAddress = new InetSocketAddress(groupAddress, GROUP_PORT);
        MulticastSocket socket = new MulticastSocket();
        DatagramPacket packet = new DatagramPacket(message, message.length, remoteAddress);
        socket.send(packet);

        Thread.sleep(1000);

        acceptor.dispose();

        context.assertIsSatisfied();
    }

//    @Test
    public void testBindGroupAddressWithHostAndPort() throws Exception {
        Mockery context = new Mockery();
        final IoHandler handler = context.mock(IoHandler.class);
        final byte[] message = new byte[] { 'H', 'E', 'L', 'L', 'O' };

        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(aNonNull(MulticastSession.class)));
                oneOf(handler).sessionOpened(with(aNonNull(MulticastSession.class)));
                oneOf(handler).messageReceived(with(aNonNull(MulticastSession.class)), with(equal(IoBuffer.wrap(message))));
                oneOf(handler).sessionClosed(with(aNonNull(MulticastSession.class)));
            }
        });

        MulticastAcceptorImpl acceptor = new MulticastAcceptorImpl();
        acceptor.setHandler(handler);

        InetAddress groupAddress = InetAddress.getByName(GROUP_ADDRESS);
        InetSocketAddress bindAddress = new InetSocketAddress(BIND_ADDRESS, GROUP_PORT);
        NetworkInterface device = NetworkInterface.getByInetAddress(bindAddress.getAddress());
        MulticastAddress localAddress = new MulticastAddress(groupAddress, device, GROUP_PORT);
        acceptor.bind(localAddress);

        InetSocketAddress remoteAddress = new InetSocketAddress(groupAddress, GROUP_PORT);
        MulticastSocket socket = new MulticastSocket();
        DatagramPacket packet = new DatagramPacket(message, message.length, remoteAddress);
        socket.send(packet);

        Thread.sleep(1000);

        acceptor.dispose();

        context.assertIsSatisfied();
    }
}
