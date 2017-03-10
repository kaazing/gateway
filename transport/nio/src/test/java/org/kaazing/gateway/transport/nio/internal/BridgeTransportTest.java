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

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertTrue;
import static org.kaazing.gateway.resource.address.ResourceAddress.TRANSPORT;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.session.IoSession;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.resource.address.ResourceOptions;
import org.kaazing.gateway.resource.address.tcp.BridgeResourceAddressFactory;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.TransportFactory;
import org.kaazing.gateway.transport.nio.internal.datagram.NioDatagramAcceptor;
import org.kaazing.gateway.transport.nio.internal.datagram.NioDatagramConnector;
import org.kaazing.gateway.transport.nio.internal.socket.NioSocketAcceptor;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;
import org.kaazing.mina.core.buffer.SimpleBufferAllocator;
import org.kaazing.mina.core.future.UnbindFuture;
import org.kaazing.test.util.MethodExecutionTrace;

public class BridgeTransportTest {


    TestRule trace = new MethodExecutionTrace();
    TestRule timeoutRule = new DisableOnDebug(Timeout.builder().withTimeout(20, SECONDS).withLookingForStuckThread(true).build());

    @Rule
    public RuleChain chain = RuleChain.outerRule(trace).around(timeoutRule);

    @Test
    public void shouldBridgeUdpOverTcpSessions() throws InterruptedException {
        final String HELLO_MESSAGE = "Hello from connector";
        final String ACCEPT_URI = "udp://127.0.0.1:8080";

        CountDownLatch messageReceived = new CountDownLatch(2);
        CountDownLatch acceptSessionClosed = new CountDownLatch(1);
        CountDownLatch connectSessionClosed = new CountDownLatch(2);

        Map<String, ?> config = Collections.emptyMap();
        TransportFactory transportFactory = TransportFactory.newTransportFactory(config);
        BridgeServiceFactory bridgeServiceFactory = new BridgeServiceFactory(transportFactory);
        SchedulerProvider schedulerProvider = new SchedulerProvider();
        ResourceAddressFactory resourceAddressFactory = new BridgeResourceAddressFactory(ACCEPT_URI);

        Map<String, Object> resources = new HashMap<>();
        resources.put("configuration", new Properties());
        resources.put("bridgeServiceFactory", bridgeServiceFactory);
        resources.put("resourceAddressFactory", resourceAddressFactory);
        resources.put("schedulerProvider", schedulerProvider);
        transportFactory.injectResources(resources);

        ResourceOptions resourceOptions = ResourceOptions.FACTORY.newResourceOptions();
        ResourceAddress transportAddress = resourceAddressFactory.newResourceAddress("bridge://127.0.0.1:8080");
        resourceOptions.setOption(TRANSPORT, transportAddress);
        ResourceAddress acceptAddress = resourceAddressFactory.newResourceAddress(ACCEPT_URI, resourceOptions);

        NioSocketAcceptor tcpAcceptor = (NioSocketAcceptor) transportFactory.getTransport("tcp").getAcceptor();
        tcpAcceptor.setResourceAddressFactory(resourceAddressFactory);
        tcpAcceptor.setBridgeServiceFactory(bridgeServiceFactory);
        tcpAcceptor.setSchedulerProvider(schedulerProvider);

        NioDatagramAcceptor acceptor = (NioDatagramAcceptor) transportFactory.getTransport("udp").getAcceptor();
        acceptor.setTcpAcceptor(tcpAcceptor);
        acceptor.setResourceAddressFactory(resourceAddressFactory);
        acceptor.setBridgeServiceFactory(bridgeServiceFactory);
        acceptor.setSchedulerProvider(schedulerProvider);

        NioDatagramConnector connector = (NioDatagramConnector) transportFactory.getTransport("udp").getConnector();
        connector.setResourceAddressFactory(resourceAddressFactory);
        connector.setBridgeServiceFactory(bridgeServiceFactory);
        connector.setTcpAcceptor(tcpAcceptor);

        acceptor.bind(acceptAddress, new IoHandlerAdapter<IoSession>() {


            @Override
            protected void doMessageReceived(IoSession session, Object message) throws Exception {
                if (HELLO_MESSAGE.equals( new String(((IoBuffer) message).array()))) {
                    messageReceived.countDown();
                    session.write(message);
                }
            }

            @Override
            protected void doSessionClosed(IoSession session) throws Exception {
                acceptSessionClosed.countDown();
            }
        }, null);

        ConnectFuture connectFuture = connector.connect(acceptAddress, new IoHandlerAdapter<IoSession>() {

            @Override
            protected void doSessionCreated(IoSession session) throws Exception {
                try {
                    session.write(SimpleBufferAllocator.BUFFER_ALLOCATOR.wrap(HELLO_MESSAGE.getBytes()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            protected void doMessageReceived(IoSession session, Object message) throws Exception {
                if (HELLO_MESSAGE.equals( new String(((IoBuffer) message).array()))) {
                    messageReceived.countDown();
                }
                session.close(true);
            }

            @Override
            protected void doSessionClosed(IoSession session) throws Exception {
                connectSessionClosed.countDown(); // TODO why called 2 times ?
            }

        }, null);

        try {
            connectFuture.await(5, SECONDS);
            assertTrue(connectFuture.isConnected());

            assertTrue(messageReceived.await(50, SECONDS));

            assertTrue(acceptSessionClosed.await(2, SECONDS));
            assertTrue(connectSessionClosed.await(2, SECONDS));


            UnbindFuture ub = acceptor.unbind(acceptAddress);
            assertTrue(ub.await(10, SECONDS));

            ub = tcpAcceptor.unbind(transportAddress);
            assertTrue(ub.await(10, SECONDS));

        } finally {
            tcpAcceptor.dispose();
            acceptor.dispose();
            connector.dispose();

            schedulerProvider.shutdownNow();
        }

        // TODO remove below comment
//        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
//        Thread[] threadArray = threadSet.toArray(new Thread[threadSet.size()]);
//        for (Thread t: threadArray) {
//            System.out.println(t.getName());
//            if ("New I/O boss #5".equals(t.getName())) {
//                for (StackTraceElement st: t.getStackTrace()) {
//                    System.out.println(st);
//                }
//            }
//        }
    }
}
