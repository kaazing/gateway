/**
 * Copyright (c) 2007-2014 Kaazing Corporation. All rights reserved.
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.kaazing.gateway.service.broadcast;

import static java.lang.String.format;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.service.ServiceContext;
import org.kaazing.gateway.service.ServiceFactory;
import org.kaazing.gateway.service.ServiceProperties;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;
import org.kaazing.test.util.MethodExecutionTrace;

public class BroadcastServiceTest {
    @Rule
    public TestRule testExecutionTrace = new MethodExecutionTrace();

    private BroadcastService service;

    @Before
    public void setup() {
        service = (BroadcastService)ServiceFactory.newServiceFactory().newService("broadcast");
        service.setSchedulerProvider(new SchedulerProvider());
    }

    @Test
    public void testReconnect() throws Exception {
        final ServiceContext serviceContext = new TestServiceContext(service, "test-broadcast", URI.create("tcp://localhost:9880"), URI.create("tcp://localhost:9897"));

        // set up the configuration -- empty properties so the values just default
        service.setConfiguration(new Properties());

        TestBackendService backend = new TestBackendService();
        Thread t = new Thread(backend, "BackendServiceThread");
        t.start();

        service.init(serviceContext);
        service.start();

        // wait for initial connection to back-end
        CountDownLatch latch = backend.getLatch();
        if (!latch.await(5, TimeUnit.SECONDS)) {
            Assert.fail("Failed to establish connection to backend in 5 seconds");
        }

        // quit back-end
        backend.stop();
        t.join();

        // restart back-end, wait for service to connect
        backend = new TestBackendService();
        t = new Thread(backend, "BackendServiceThread2");
        t.start();

        latch = backend.getLatch();
        if (!latch.await(5, TimeUnit.SECONDS)) {
            // Default reconnect is 3 seconds, so connection should get established within 3 seconds, make it 5 to be sure
            Assert.fail("Failed to reconnect to backend in 5 seconds");
        }

        backend.stop();
        t.join();

        service.stop();
        service.destroy();
    }

    @Test
    public void testBroadcast() throws Exception {
        final TestServiceContext serviceContext = new TestServiceContext(service, "test-broadcast", URI.create("tcp://localhost:9880"), null);
        ServiceProperties serviceProperties = serviceContext.getProperties();
        serviceProperties.put("accept", "tcp://localhost:9090");

        // set up the configuration -- empty properties so the values just default
        service.setConfiguration(new Properties());

        service.init(serviceContext);
        service.start();

        TestBackendProducer producer = new TestBackendProducer();
        Thread t = new Thread(producer, "BackendProducerThread");
        t.start();

        // wait for initial connection from producer
        if (!producer.getLatch().await(5, TimeUnit.SECONDS)) {
            Assert.fail("Failed to receive connection from producer in 5 seconds");
        }

        TestClient c1 = new TestClient(1);
        TestClient c2 = new TestClient(2);
        TestClient c3 = new TestClient(3);

        Thread tc1 = new Thread(c1, "Client 1");
        Thread tc2 = new Thread(c2, "Client 2");
        Thread tc3 = new Thread(c3, "Client 3");

        tc1.start();
        tc2.start();
        tc3.start();

        if (!c1.getLatch().await(5, TimeUnit.SECONDS)) {
            Assert.fail("Client 1 failed to receive message in 5 seconds");
        }
        if (!c2.getLatch().await(5, TimeUnit.SECONDS)) {
            Assert.fail("Client 2 failed to receive message in 5 seconds");
        }
        if (!c3.getLatch().await(5, TimeUnit.SECONDS)) {
            Assert.fail("Client 3 failed to receive message in 5 seconds");
        }

        c1.stop();
        c2.stop();
        c3.stop();

        tc1.join();
        tc2.join();
        tc3.join();

        // quit back-end
        producer.stop();
        t.join();

        service.stop();
        service.destroy();
    }

    @Test
    public void testSlowConsumer() throws Exception {
        final TestServiceContext serviceContext = new TestServiceContext(service, "test-broadcast", URI.create("tcp://localhost:9880"), null);
        ServiceProperties serviceProperties = serviceContext.getProperties();
        serviceProperties.put("accept", "tcp://localhost:9090");

        // set up the configuration -- empty properties so the values just default
        Properties slowConsumerProps = new Properties();
        slowConsumerProps.setProperty("org.kaazing.gateway.server.service.broadcast.MAXIMUM_PENDING_BYTES", "40000"); // FIXME: why this number?
        service.setConfiguration(slowConsumerProps);

        service.init(serviceContext);
        service.start();

        FastTestBackendProducer producer = new FastTestBackendProducer();
        Thread t = new Thread(producer, "FastBackendProducerThread");

        SlowTestClient c1 = new SlowTestClient(1);
        Thread tc1 = new Thread(c1, "SlowClient 1");

        try {
            t.start();

            // wait for initial connection from producer
            if (!producer.getLatch().await(5, TimeUnit.SECONDS)) {
                Assert.fail("Failed to receive connection from producer in 5 seconds");
            }

            tc1.start();

            if (!c1.getLatch().await(5, TimeUnit.SECONDS)) {
                Assert.fail("SlowClient 1 failed to connect in 5 seconds");
            }

            // Wait up to 15 seconds for slow client to get killed, after that the test has timed out and failure is asserted
            tc1.join(15 * 1000);
            if (tc1.isAlive()) {
                c1.stop();
                tc1.join();
                Assert.fail("SlowClient 1 was not terminated in 30 seconds");
            }
	} finally {
            // quit back-end
            producer.stop();
            t.join();
	}

        service.stop();
        service.destroy();
    }

    private class TestBackendService implements Runnable {
        // a simple Backend that has a listener (ServerSocket) and has an API for producing a message
        private ServerSocket s;
        private boolean running = false;
        private CountDownLatch latch = new CountDownLatch(1);

        private TestBackendService() {
            try {
                s = new ServerSocket(9897);
                running = true;
            } catch (IOException ex) {
                throw new RuntimeException("Failed to create TestBackendSerivce", ex);
            }
        }

        @Override
        public void run() {
            try {
                Socket socket = s.accept();
                OutputStream os = socket.getOutputStream();

                latch.countDown(); // someone connected, count down
                while (isRunning()) {
                    // The bytes for ">|<"
                    os.write(new byte[] { 0x3E, 0x7C, 0x3C});
                    try {
                        Thread.sleep(500); // sending 2 messages / second
                    } catch (InterruptedException interEx) {
                        // ignore
                    }
                }
                socket.close();
            } catch (IOException ex) {
                throw new RuntimeException("Issue in TestBackendService.run()", ex);
            }
        }

        private CountDownLatch getLatch() {
            return latch;
        }

        private boolean isRunning() {
            return running;
        }

        private void stop() {
            running = false;
            try {
                s.close();
            } catch (IOException ex) {
                throw new RuntimeException("Failed to close ServerSocket", ex);
            }
        }
    }

    private class TestBackendProducer implements Runnable {
        private CountDownLatch latch = new CountDownLatch(1);
        private boolean running = true;

        @Override
        public void run() {
            try {
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress("localhost", 9090));
                OutputStream os = socket.getOutputStream();

                latch.countDown(); // someone connected, count down
                while (running == true) {
                    // The bytes for ">|<"
                    os.write(new byte[] { 0x3E, 0x7C, 0x3C});
                    try {
                        Thread.sleep(500); // sending 2 messages / second
                    } catch (InterruptedException interEx) {
                        // ignore
                    }
                }
                socket.close();
            } catch (IOException ex) {
                throw new RuntimeException("Issue in TestBackendProducer.run()", ex);
            }
        }

        private void stop() {
            running = false;
        }

        private CountDownLatch getLatch() {
            return latch;
        }
    }

    private class TestClient implements Runnable {
        private CountDownLatch latch = new CountDownLatch(1);
        private boolean running = true;
        private int clientNumber;

        private TestClient(int num) {
            clientNumber = num;
        }

        @Override
        public void run() {
            try {
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress("localhost", 9880));
                InputStream in = socket.getInputStream();

                // read a message, countdown the latch, then keep reading messages until stop is called
                byte[] b = new byte[3];
                int numBytes = in.read(b);

                if (numBytes == 3) {
                    System.out.println(format("TestClient %d:  received message %s", clientNumber, new String(b)));
                    latch.countDown(); // someone connected, count down
                } else {
                    System.out.println(format("Failure in TestClient %d:  read returned %d", clientNumber, numBytes));
                }

                while (running == true) {
                    numBytes = in.read(b);
                    if (numBytes < 0) {
                        System.out.println(format("TestClient %d:  EOF, done reading, quitting client"));
                        break;
                    }

                    if (numBytes == 3) {
                        System.out.println(format("TestClient %d:  received message %s", clientNumber, new String(b)));
                    } else {
                        System.out.println(format("Failure in TestClient %d:  read returned %d", clientNumber, numBytes));
                    }
                }
                socket.close();
            } catch (IOException ex) {
                throw new RuntimeException("Issue in TestClient.run()", ex);
            }
        }

        private void stop() {
            running = false;
        }

        private CountDownLatch getLatch() {
            return latch;
        }
    }

    private class FastTestBackendProducer implements Runnable {
        private CountDownLatch latch = new CountDownLatch(1);
        private boolean running = true;

        @Override
        public void run() {
            try {
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress("localhost", 9090));
                OutputStream os = socket.getOutputStream();

                // Some diagnostics for the test
                System.out.println(format("FastTestBackendProducer receive buffer size: %d", socket.getReceiveBufferSize()));
                System.out.println(format("FastTestBackendProducer send buffer size: %d", socket.getSendBufferSize()));

                // The bytes for ">|<"
                byte[] packet = new byte[] { 0x3E, 0x7C, 0x3C };
                int messagesPerSecond = 20;
                int testLengthTime = 4;
                long targetBytes = 2 * (socket.getReceiveBufferSize() + socket.getSendBufferSize());
                long batchSize = targetBytes / (messagesPerSecond * testLengthTime);
                long totalBytesSent = 0;
                long bytesSent = 0;

                System.out.println(format("FastTestBackendProducer targetBytes: %d", targetBytes));
                System.out.println(format("FastTestBackendProducer batch size: %d", batchSize));

                long startTime = System.currentTimeMillis();
                latch.countDown(); // someone connected, count down
                while (running) {
                    while (bytesSent < batchSize) {
                        os.write(packet);
                        bytesSent += packet.length;
                    }

                    totalBytesSent += bytesSent;
                    bytesSent = 0;
                    long delta = (System.currentTimeMillis() - startTime);

                    // In the event the test fails, print some diagnostics
                    System.out.println(format("Time: %d  Producer, sent %d bytes", delta, totalBytesSent));
                    try {
                        Thread.sleep(1000 / messagesPerSecond); // sending 20 messages / second
                    } catch (InterruptedException interEx) {
                        // ignore
                    }
                }
                socket.close();
            } catch (IOException ex) {
                throw new RuntimeException("Issue in TestBackendProducer.run()", ex);
            }
        }

        private void stop() {
            running = false;
        }

        private CountDownLatch getLatch() {
            return latch;
        }
    }

    private class SlowTestClient implements Runnable {
        private CountDownLatch latch = new CountDownLatch(1);
        private int clientNumber;
        private SlowTestClient(int num) {
            clientNumber = num;
        }

        public void stop() {
        }

        @Override
        public void run() {
            try {
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress("localhost", 9880));
                System.out.println(format("SlowTestClient %d: socket is %s", clientNumber, socket.toString()));
                System.out.println(format("SlowTestClient receive buffer size: %d", socket.getReceiveBufferSize()));
                System.out.println(format("SlowTestClient send buffer size: %d", socket.getSendBufferSize()));
                InputStream in = socket.getInputStream();
                OutputStream os = socket.getOutputStream();
                latch.countDown(); // SlowTestClient successfully connected, now won't read

                int iteration = 0;
                while (!socket.isClosed() && socket.isConnected() && !socket.isInputShutdown()) {
                    // take little naps, mmmmmmmmm naps....
                    try {
                        Thread.sleep(1000);
                        System.out.println(format("SlowTestClient %d: iteration #%d has %d bytes available", clientNumber, iteration++, in.available()));
                        try {
                            os.write(new byte[] { 0x21 }); // write '!' back to the Gateway
                        } catch (IOException writeIOEx) {
                            // Expected as this is the way the socket is tested for being closed, just quit the loop
                            break;
                        }
                    } catch (InterruptedException interEx) {
                        System.out.println(format("SlowTestClient %d: someone woke me from my slumber!", clientNumber));
                        break;
                    }
                }

                System.out.println(format("SlowTestClient %d: connection closed, that'll teach me", clientNumber));

                socket.close();
            } catch (IOException ex) {
                throw new RuntimeException("Issue in TestClient.run()", ex);
            }
        }

        private CountDownLatch getLatch() {
            return latch;
        }
    }
}
