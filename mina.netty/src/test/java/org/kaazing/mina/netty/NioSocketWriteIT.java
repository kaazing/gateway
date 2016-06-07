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
/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.kaazing.mina.netty;

import static org.kaazing.mina.core.buffer.IoBufferEx.FLAG_NONE;
import static org.kaazing.mina.core.buffer.SimpleBufferAllocator.BUFFER_ALLOCATOR;
import static java.lang.Integer.MAX_VALUE;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.SocketSessionConfigEx;
import org.jboss.netty.channel.socket.ServerSocketChannelFactory;
import org.jboss.netty.channel.socket.SocketChannelConfig;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioWorker;
import org.jboss.netty.channel.socket.nio.NioWorkerPool;
import org.jboss.netty.channel.socket.nio.WorkerPool;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.buffer.SimpleBufferAllocator.SimpleBuffer;
import org.kaazing.mina.netty.socket.SocketChannelIoSessionConfig;
import org.kaazing.mina.netty.socket.nio.DefaultNioSocketChannelIoSessionConfig;
import org.kaazing.mina.netty.socket.nio.NioSocketChannelIoAcceptor;

public class NioSocketWriteIT {

    // private static final int MAXRCVBUF = 1024 * 1000;
    // private static final int MAXSENDBUF = 1024 * 1000;
    // private static final int[] DATA_LENGTHS = {1, 10, 25, 50, 100, 200, 512, 1024, 2048, 4096, 8092};

    private static final int MAXRCVBUF = 1024 * 1000;
    private static final int MAXSENDBUF = 1024 * 1000;
    private static final int[] DATA_LENGTHS = {25, 50, 100, 200};

    private static final String BENCHMARK_FILENAME = "benchmark.csv";
    private static byte[] data;
    private EchoRepeatHandler sh;
    private Socket socket;
    // private ChannelIoAcceptor<DefaultNioSocketChannelIoSessionConfig, ServerSocketChannelFactory, InetSocketAddress>
    // acceptor;
    private ChannelIoAcceptor<SocketChannelIoSessionConfig<? extends SocketChannelConfig>, ServerSocketChannelFactory,
                                        InetSocketAddress> acceptor;
    private PrintWriter writer;
    private boolean isJunitTest;

    public static void main(String[] args) throws Throwable {
        NioSocketWriteIT obj = new NioSocketWriteIT();
        obj.writer = new PrintWriter(BENCHMARK_FILENAME, "UTF-8");
        obj.benchmark();
    }

    public void benchmark() throws Throwable {
        writer = new PrintWriter(BENCHMARK_FILENAME, "UTF-8");
        try {
            writer.write("TCP_NO_DELAY,MSG_SIZE,SO_SNDSIZE,SO_RCVSIZE,WRITES_BEFORE_INCOMPLETE\n");
            benchmark0(false);
            benchmark0(true);
        } finally {
            writer.close();
        }
    }

    private void benchmark0(boolean noDelay) throws Throwable {
        for (int dataLength : DATA_LENGTHS) {
            int sendBufferSize = 0;
            do {
                int rcvBufferSize = 0;
                do {
                    try {
                        initTest();
                        setUpAndConnect(noDelay, dataLength, sendBufferSize, rcvBufferSize);
                        int num = writeAndReadRepeats();
                        writer.write(
                                String.format("%s,%d,%d,%d,%d\n", noDelay, dataLength, sendBufferSize, rcvBufferSize, num - 1));
                    }
                    finally {
                        destroy();
                    }
                    rcvBufferSize = rcvBufferSize == 0 ? 1024 : rcvBufferSize * 2;
                } while (rcvBufferSize <= MAXRCVBUF);
                sendBufferSize = sendBufferSize == 0 ? 1024 : sendBufferSize * 2;
            } while (sendBufferSize <= MAXSENDBUF);

        }
    }

    @Before
    public void junitTestinit() {
        isJunitTest = true;
        initTest();
    }

    public void initTest() {
        WorkerPool<NioWorker> workerPool = new NioWorkerPool(newCachedThreadPool(), 3);
        NioServerSocketChannelFactory serverChannelFactory = new NioServerSocketChannelFactory(newCachedThreadPool(),
                workerPool);
        acceptor = new NioSocketChannelIoAcceptor(new DefaultNioSocketChannelIoSessionConfig(), serverChannelFactory);
        socket = new Socket();
    }

    @After
    public void destroy() throws Throwable {
        if (socket != null) {
            socket.close();
        }
        if (acceptor != null) {
            acceptor.dispose();
        }
    }

    // This test sends 100 byte messages and sends enough messages to fill the server send buffer. Testes with
    // TCP_NO_DELAY = true with default OS settings for send buffer size and receive buffer size.
    @Test
    public void testCompleteWrites() throws Throwable {
        setUpAndConnect(true, 100, 0, 0);
        writeAndReadRepeats();
        assertEquals("Expected no incomplete writes", 0, sh.incompleteWrites);
    }

    private void setUpAndConnect(boolean noDelay, int dataLen, int sendBuf, int rcvBuf) throws Throwable {

        System.out.println(String.format(
                "Starting test with parameters TCP_NODELAY=%s, dataLen=%d, sendBufSize=%d, rcvBufSize=%d", noDelay,
                dataLen, sendBuf, rcvBuf));

        data = new byte[dataLen];

        SocketChannelIoSessionConfig<? extends SocketChannelConfig> config = acceptor.getSessionConfig();

        // Set up Server Options
        if (rcvBuf != 0) {
            config.setReceiveBufferSize(rcvBuf);
        }
        if (sendBuf != 0) {
            config.setSendBufferSize(sendBuf);
        }
        config.setTcpNoDelay(noDelay);



        sh = new EchoRepeatHandler(data, !isJunitTest);

        DefaultIoFilterChainBuilder builder = new DefaultIoFilterChainBuilder();
        acceptor.setFilterChainBuilder(builder);
        acceptor.setHandler(sh);


        final int port = 8021;

        acceptor.bind(new InetSocketAddress(port));

        // Set up Client socket options. Really only Receive Buffer should matter for this test.
        if (rcvBuf != 0) {
            socket.setReceiveBufferSize(rcvBuf);
        }
        if (sendBuf != 0) {
            socket.setSendBufferSize(sendBuf);
        }

        socket.setTcpNoDelay(noDelay);

        SocketAddress bindAddress = new InetSocketAddress(port);

        socket.connect(bindAddress);
    }


    private int writeAndReadRepeats() throws Throwable {
        OutputStream output = socket.getOutputStream();
        InputStream input = socket.getInputStream();

        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i % 256);
        }
        output.write(data);

        // Wait till the server writes everything out. So we know if and when we get an incomplete write
        assertTrue("Failed to complete all writes", sh.allWritesDone.await(120, SECONDS));

        byte[] receivePayload = new byte[data.length];
        int messagesReceived = 0;
        while (messagesReceived < sh.numSent) {
            int numRead = 0;
            do {
                int n = input.read(receivePayload, numRead, data.length - numRead);
                assert n != -1 : "EOF on Input Stream";
                numRead += n;

            } while (numRead < data.length);
            assertTrue("payload echoed", Arrays.equals(data, receivePayload));
            messagesReceived++;
        }
        return sh.numSent;
    }

    private static class EchoRepeatHandler extends IoHandlerAdapter {
        final AtomicReference<Throwable> exception = new AtomicReference<>();
        volatile int counter;
        volatile int incompleteWrites;
        volatile int numSent;

        CountDownLatch allWritesDone = new CountDownLatch(1);
        CountDownLatch connected = new CountDownLatch(1);

        private int nb_writes;
        private final byte[] data;
        private final boolean writeTillIncomplete;
        private SimpleBuffer cumulation;

        EchoRepeatHandler(byte[] data, boolean writeTillIncomplete) {
            this.writeTillIncomplete = writeTillIncomplete;
            this.data = data;
        }

        @Override
        public void sessionCreated(IoSession session) throws Exception {
            if (writeTillIncomplete) {
                nb_writes = MAX_VALUE;
            } else {
                nb_writes = ((SocketSessionConfigEx) session.getConfig()).getSendBufferSize() / data.length;
            }
            connected.countDown();
        }

        @Override
        public void messageReceived(IoSession session, Object message)
                throws Exception {
            IoBufferEx m = (IoBufferEx) message;
            int readableBytes = m.limit() - m.position();

            if (readableBytes < data.length) {
                if (cumulation == null) {
                    cumulation = BUFFER_ALLOCATOR.wrap(BUFFER_ALLOCATOR.allocate(data.length, FLAG_NONE), FLAG_NONE);
                }
                cumulation.put(m);
                readableBytes = cumulation.position();
                if (readableBytes < data.length) {
                    return;
                }
                cumulation.flip();
                m = cumulation;
                cumulation = null;
            }

            IoBufferEx sendMessage = m.duplicate();

            byte[] actual = new byte[readableBytes];
            m.get(actual);
            int lastIdx = counter;
            for (int i = 0; i < actual.length; i++) {
                assertEquals(data[i + lastIdx], actual[i]);
            }

            for (numSent = 0; numSent < nb_writes; numSent++) {
                WriteFuture writeFuture = session.write(sendMessage);
                if (!writeFuture.isWritten()) {
                    incompleteWrites++;
                    numSent++;
                    System.out.println(String.format("Write number %d incomplete", numSent));
                    if (writeFuture.getException() != null) {
                        writeFuture.getException().printStackTrace();
                    }
                    // We are done when we get an incomplete write.
                    break;
                }
            }
            System.out.println(String.format("%d Writes done", numSent));
            allWritesDone.countDown();

            counter += actual.length;
        }

        @Override
        public void exceptionCaught(IoSession session, Throwable cause)
                throws Exception {
            if (exception.compareAndSet(null, cause)) {
                session.close();
            }
        }
    }
}
