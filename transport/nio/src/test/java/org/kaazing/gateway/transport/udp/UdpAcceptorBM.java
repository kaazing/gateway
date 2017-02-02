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
package org.kaazing.gateway.transport.udp;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.TransportFactory;
import org.kaazing.gateway.transport.nio.internal.NioDatagramAcceptor;
import org.kaazing.gateway.transport.nio.internal.NioDatagramConnector;
import org.kaazing.gateway.transport.nio.internal.NioSocketAcceptor;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.kaazing.gateway.util.InternalSystemProperty.UDP_IDLE_TIMEOUT;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 8, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 8, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@State(Scope.Benchmark)
public class UdpAcceptorBM {

    private static final int PORT = 8080;
    private static final String URI = "udp://127.0.0.1:8080";

    private static ResourceAddressFactory addressFactory;
    private static SchedulerProvider schedulerProvider;
    private static NioDatagramAcceptor udpAcceptor;
    private static NioDatagramConnector udpConnector;

    private static NioSocketAcceptor tcpAcceptor;

    private static AtomicInteger clientSent = new AtomicInteger(0);
    private static AtomicInteger clientReceived = new AtomicInteger(0);

    @Setup
    public void init() throws Exception {
        Map<String, Object> configuration = new HashMap<>();
        configuration.put(UDP_IDLE_TIMEOUT.getPropertyName(), "2");

        addressFactory = ResourceAddressFactory.newResourceAddressFactory();
        TransportFactory transportFactory = TransportFactory.newTransportFactory(configuration);
        BridgeServiceFactory serviceFactory = new BridgeServiceFactory(transportFactory);

        schedulerProvider = new SchedulerProvider();

        tcpAcceptor = (NioSocketAcceptor)transportFactory.getTransport("tcp").getAcceptor();
        tcpAcceptor.setResourceAddressFactory(addressFactory);
        tcpAcceptor.setBridgeServiceFactory(serviceFactory);
        tcpAcceptor.setSchedulerProvider(schedulerProvider);

        udpAcceptor = (NioDatagramAcceptor) transportFactory.getTransport("udp").getAcceptor();
        udpAcceptor.setTcpAcceptor(tcpAcceptor);
        udpAcceptor.setResourceAddressFactory(addressFactory);
        udpAcceptor.setBridgeServiceFactory(serviceFactory);
        udpAcceptor.setSchedulerProvider(schedulerProvider);

        udpConnector = (NioDatagramConnector) transportFactory.getTransport("udp").getConnector();
        udpConnector.setResourceAddressFactory(addressFactory);
        udpConnector.setBridgeServiceFactory(serviceFactory);
        udpConnector.setTcpAcceptor(tcpAcceptor);

        ResourceAddress address = addressFactory.newResourceAddress(URI);
        System.out.println("UdpAcceptor starting ...");
        EchoHandler echoHandler = new EchoHandler();
        udpAcceptor.bind(address, echoHandler, null);
    }

    @State(Scope.Thread)
    public static class SocketState {
        DatagramSocket udpClient;

        public SocketState() {
            try {
                udpClient = new DatagramSocket();
                udpClient.setSoTimeout(1000);
                System.out.println("Connecting DatagramSocket client ..." + Thread.currentThread());
                udpClient.connect(new InetSocketAddress("localhost", PORT));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @State(Scope.Thread)
    public static class ConnectorState {
        IoSession session;
        ConnectorHandler handler;

        public ConnectorState() {
            try {
                ResourceAddress address = addressFactory.newResourceAddress(URI);
                handler = new ConnectorHandler();
                ConnectFuture future = udpConnector.connect(address, handler, null);
                System.out.println("Connecting Udp connector client ..." + Thread.currentThread());
                future.await(1, TimeUnit.SECONDS);
                session = future.getSession();
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }

    @TearDown
    public void destroy() throws Exception {
        System.out.println("UdpAcceptor stopping ...");
        tcpAcceptor.dispose();
        udpAcceptor.dispose();
        udpConnector.dispose();
        schedulerProvider.shutdownNow();

        System.out.println();
        System.out.println("Total client sent = " + clientSent + " client received = " + clientReceived);
    }

    // Aynchronous client using gateway's udp connector
    // send, send, send ...
    // receive, receive, receive ...
    // It is possible that many messages are not received by server
    @Benchmark
    public void testAsyncConnector(ConnectorState state) throws Exception {
        String message = "Hello World";
        IoSessionEx session = (IoSessionEx) state.session;
        ByteBuffer data = ByteBuffer.wrap(message.getBytes());
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        WriteFuture writeFuture = session.write(allocator.wrap(data));
        writeFuture.await(1, TimeUnit.SECONDS);
        clientSent.incrementAndGet();
    }

    // Synchronous client using gateway's udp connector
    // send - receive - send - receive ....
    @Benchmark
    public void testSyncConnector(ConnectorState state) throws Exception {
        String message = "Hello World";
        IoSessionEx session = (IoSessionEx) state.session;
        ByteBuffer data = ByteBuffer.wrap(message.getBytes());
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        CountDownLatch latch = new CountDownLatch(1);
        state.handler.setLatch(latch);
        session.write(allocator.wrap(data));
        latch.await(1, TimeUnit.SECONDS);

        clientSent.incrementAndGet();
    }

    // Synchronous client using DatagramSocket client
    @Benchmark
    public void testDatagramSocket(SocketState state) throws Exception {
        DatagramSocket udpClient = state.udpClient;
        String str = "Hello World";
        byte[] sendBuf = str.getBytes(UTF_8);
        DatagramPacket sendDp = new DatagramPacket(sendBuf, sendBuf.length);
        udpClient.send(sendDp);
        clientSent.incrementAndGet();

        try {
            byte[] recvBuf = new byte[20];
            DatagramPacket recvDp = new DatagramPacket(recvBuf, 0, recvBuf.length);
            udpClient.receive(recvDp);
            String got = new String(recvDp.getData(), recvDp.getOffset(), recvDp.getLength(), UTF_8);
            assertEquals(str, got);
            clientReceived.incrementAndGet();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static final class EchoHandler extends IoHandlerAdapter<IoSessionEx> {
        AtomicLong value = new AtomicLong(0);

        @Override
        protected void doMessageReceived(IoSessionEx session, Object message) {
            session.write(message);
            value.incrementAndGet();
        }
    }

    private static final class ConnectorHandler extends IoHandlerAdapter<IoSessionEx> {
        CountDownLatch latch;

        @Override
        protected void doMessageReceived(IoSessionEx session, Object message) {
            clientReceived.incrementAndGet();
            latch.countDown();
        }

        void setLatch(CountDownLatch latch) {
            this.latch = latch;
        }
    }

    // Or from command line:
    //
    // mvn clean install
    // java -jar target/benchmarks.jar -wi 5 -i 10 -t 8 -f 1
    // (we requested 5 measurement/warmup iterations, with 4 threads, single fork)
    //
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(UdpAcceptorBM.class.getSimpleName())
                .warmupIterations(5)
                .measurementIterations(10)
                .threads(8)
                .forks(1)
                .build();

        new Runner(opt).run();
    }

}
