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

import static org.kaazing.mina.netty.PortUtil.nextPort;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.logging.LoggingFilter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.mina.netty.socket.nio.DefaultNioSocketChannelIoSessionConfig;
import org.kaazing.mina.netty.socket.nio.NioSocketChannelIoConnector;

/**
 * Integration test for mina.netty layer.
 */
public class NioSocketChannelIoConnectorIT {

    private ExecutorService executor;
    private ServerSocket server;
    private Socket accepted;
    private IoConnector connector;

    @Before
    public void init() throws Exception {
        executor = Executors.newFixedThreadPool(1);
        server = new ServerSocket();
        server.setReuseAddress(true);
        connector = new NioSocketChannelIoConnector(new DefaultNioSocketChannelIoSessionConfig());
        connector.getFilterChain().addLast("logger", new LoggingFilter());
    }

    @After
    public void dispose() throws Exception {
        if (server != null) {
            server.close();
        }
        if (accepted != null) {
            accepted.close();
        }
        if (executor != null) {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
        if (connector != null) {
            connector.dispose();
        }
    }

    @Test //(timeout = 1000)
    public void shouldEchoBytes() throws Exception {
        byte[] sendPayload = new byte[] { 0x00, 0x01, 0x02 };
        final byte[] receivePayload = new byte[sendPayload.length];

        final SocketAddress bindAddress = new InetSocketAddress("localhost", nextPort(8100, 100));

        Callable<Void> echoTask = new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                byte[] payload = new byte[32];
                accepted = server.accept();
                InputStream input = accepted.getInputStream();
                OutputStream output = accepted.getOutputStream();
                int bytesRead = input.read(payload);
                output.write(payload, 0, bytesRead);
                output.close();
                input.close();
                return null;
            }
        };
        server.bind(bindAddress);
        executor.submit(echoTask);

        final AtomicInteger exceptionsCaught = new AtomicInteger();
        connector.setHandler(new IoHandlerAdapter() {
            @Override
            public void messageReceived(IoSession session, Object message)
                    throws Exception {
                IoBufferEx buf = (IoBufferEx) message;
                buf.get(receivePayload);

                if (buf.hasRemaining()) {
                    throw new BufferOverflowException();
                }

                session.close(false);
            }

            @Override
            public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
                exceptionsCaught.incrementAndGet();
            }
        });

        ConnectFuture connect = connector.connect(bindAddress);
        connect.await();
        IoSessionEx session = (IoSessionEx) connect.getSession();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        session.write(allocator.wrap(ByteBuffer.wrap(sendPayload)));
        session.getCloseFuture().await();

        assertTrue("payload echoed", Arrays.equals(sendPayload, receivePayload));
        assertEquals("no handler exceptions", 0, exceptionsCaught.get());
    }

}
