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
package org.kaazing.mina.netty;

import static java.util.concurrent.Executors.newCachedThreadPool;
import static org.kaazing.mina.netty.PortUtil.nextPort;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.logging.LoggingFilter;
import org.jboss.netty.channel.socket.nio.NioServerDatagramChannelFactory;
import org.jboss.netty.channel.socket.nio.NioWorkerPool;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.mina.netty.socket.DatagramChannelIoSessionConfig;
import org.kaazing.mina.netty.socket.DefaultDatagramChannelIoSessionConfig;
import org.kaazing.mina.netty.socket.nio.NioDatagramChannelIoAcceptor;

/**
 * Integration test for mina.netty layer. Similar to IT, but for datagram transport.
 */
public class NioDatagramChannelIoAcceptorIT {

    @Rule
    public TestRule timeout = new DisableOnDebug(new Timeout(10, TimeUnit.SECONDS));

    private IoAcceptor acceptor;
    private DatagramSocket socket;

    @Before
    public void initResources() throws Exception {
        DatagramChannelIoSessionConfig sessionConfig = new DefaultDatagramChannelIoSessionConfig();
        sessionConfig.setReuseAddress(true);
        NioWorkerPool workerPool = new NioWorkerPool(newCachedThreadPool(), 4);
        NioServerDatagramChannelFactory channelFactory = new NioServerDatagramChannelFactory(newCachedThreadPool(), 1, workerPool);
        acceptor = new NioDatagramChannelIoAcceptor(sessionConfig, channelFactory);
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

    @Test
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
