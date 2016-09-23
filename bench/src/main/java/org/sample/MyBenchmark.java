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
package org.sample;

import org.apache.mina.core.buffer.IoBuffer;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.TransportFactory;
import org.kaazing.gateway.transport.nio.internal.NioDatagramAcceptor;
import org.kaazing.gateway.transport.nio.internal.NioSocketAcceptor;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Timeout;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.kaazing.gateway.transport.nio.NioSystemProperty.UDP_IDLE_TIMEOUT;
//import static org.kaazing.gateway.transport.nio.NioSystemProperty.UDP_IDLE_TIMEOUT;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
public class MyBenchmark {

    private static final int PORT = 8080;
    private static final String URI = "udp://127.0.0.1:8080";

    private SchedulerProvider schedulerProvider;
    private NioDatagramAcceptor udpAcceptor;
    private NioSocketAcceptor tcpAcceptor;
    private DatagramSocket udpClient;

    @Setup
    public void init() throws Exception {
        Map<String, Object> configuration = new HashMap<>();
        configuration.put(UDP_IDLE_TIMEOUT.getPropertyName(), "2");

        ResourceAddressFactory addressFactory = ResourceAddressFactory.newResourceAddressFactory();
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

        ResourceAddress address = addressFactory.newResourceAddress(URI);
        System.out.println("Binding ...");
        udpAcceptor.bind(address, new EchoHandler(), null);

        udpClient = new DatagramSocket();
        udpClient.setSoTimeout(1000);
        udpClient.connect(new InetSocketAddress("localhost", PORT));

    }

    @TearDown
    public void destroy() throws Exception {
        System.out.println("Unbinding ...");
        tcpAcceptor.dispose();
        udpAcceptor.dispose();
        schedulerProvider.shutdownNow();
        udpClient.close();
    }

    @Benchmark
    @Measurement(iterations = 40)
    @Timeout(time = 5)
    public void writer() throws Exception {
        String str = "Hello World";
        byte[] sendBuf = str.getBytes(UTF_8);
        DatagramPacket sendDp = new DatagramPacket(sendBuf, sendBuf.length);
        udpClient.send(sendDp);

        try {
            byte[] recvBuf = new byte[20];
            DatagramPacket recvDp = new DatagramPacket(recvBuf, 0, recvBuf.length);
            udpClient.receive(recvDp);
            String got = new String(recvDp.getData(), recvDp.getOffset(), recvDp.getLength(), UTF_8);
            assertEquals(str, got);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static final class EchoHandler extends IoHandlerAdapter<IoSessionEx> {

        @Override
        protected void doMessageReceived(IoSessionEx session, Object message) {
            session.write(message);
        }
    }

}
