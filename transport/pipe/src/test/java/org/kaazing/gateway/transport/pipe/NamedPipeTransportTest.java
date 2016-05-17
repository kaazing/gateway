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
package org.kaazing.gateway.transport.pipe;

import static org.junit.Assert.fail;
import static org.kaazing.gateway.resource.address.ResourceAddressFactory.newResourceAddressFactory;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;
import org.apache.mina.util.ConcurrentHashSet;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.TransportFactory;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;
import org.kaazing.mina.core.future.UnbindFuture;
import org.kaazing.test.util.MethodExecutionTrace;

public class NamedPipeTransportTest {

    @Rule
    public TestRule testExecutionTrace = new MethodExecutionTrace();

    @BeforeClass
    public static void init()
            throws Exception {
    }

    ResourceAddressFactory resourceAddressFactory = newResourceAddressFactory();
    BridgeServiceFactory bridgeServiceFactory;
    SchedulerProvider schedulerProvider = new SchedulerProvider();
    
    NamedPipeAcceptor pipeAcceptor;
    NamedPipeConnector pipeConnector;
    
    Set<Exception> failures = new ConcurrentHashSet<>();

    @Before
    public void before() {
        schedulerProvider = new SchedulerProvider();
        Map<String, ?> config = Collections.emptyMap();
        TransportFactory transportFactory = TransportFactory.newTransportFactory(config);
        bridgeServiceFactory = new BridgeServiceFactory(transportFactory);

        pipeAcceptor = (NamedPipeAcceptor)transportFactory.getTransport("pipe").getAcceptor();
        pipeConnector = (NamedPipeConnector)transportFactory.getTransport("pipe").getConnector();

        initBridgeServices();
        
        failures = new ConcurrentHashSet<>();
    }

    @After
    public void after() {
        pipeAcceptor.dispose();
        pipeConnector.dispose();
    }

    private void initBridgeServices() {
        pipeAcceptor.setResourceAddressFactory(resourceAddressFactory);
        pipeAcceptor.setBridgeServiceFactory(bridgeServiceFactory);

        pipeConnector.setResourceAddressFactory(resourceAddressFactory);
        pipeConnector.setBridgeServiceFactory(bridgeServiceFactory);
        pipeConnector.setNamedPipeAcceptor(pipeAcceptor);
    }

    @Test
    public void sendAndClose() {

        final CountDownLatch httpClientSessionOpened = new CountDownLatch(1);
        final CountDownLatch httpServerSessionClosed = new CountDownLatch(1);

        final IoHandlerAdapter connectHandler = new IoHandlerAdapter() {
            @Override
            protected void doSessionCreated(IoSession session) throws Exception {
                System.out.println("Client  Session created");
            }
            @Override
            protected void doSessionOpened(IoSession session) throws Exception {
                System.out.println("Client  Session opened");
                httpClientSessionOpened.countDown();
            }
        };

        final IoHandlerAdapter acceptHandler = new IoHandlerAdapter() {
            @Override
            protected void doMessageReceived(IoSession session, Object message) throws Exception {
                System.out.println("Server  Session message received: "+message);
                // ADVICE: add to failures structure to capture test failures in handlers.

                session.close(false).addListener( new IoFutureListener<CloseFuture>() {
                    @Override
                    public void operationComplete(CloseFuture future) {
                        System.out.println("Server  Session closed");
                        httpServerSessionClosed.countDown();
                    }
                });
            }
        };

        Map<String, Object> bindOptions = new HashMap<>();
        final Map<String, Object> connectOptions = new HashMap<>();

        pipeConnectorToAcceptor("pipe://transport",
                                acceptHandler,
                                connectHandler,
                                bindOptions,
                                connectOptions,
                                httpServerSessionClosed);

        if ( failures.size() > 0 ) {
            StringBuilder b = new StringBuilder();
            for (Exception e: failures) {
                b.append(e.getMessage());
                b.append("\n");
            }
            fail("Detected "+failures.size()+" failures: "+b.toString());
        }
    }

    private void pipeConnectorToAcceptor(final String connectURI, IoHandlerAdapter acceptHandler,
                                         IoHandlerAdapter connectHandler,
                                         Map<String, Object> acceptOptions,
                                         Map<String, Object> connectOptions,
                                         final CountDownLatch latch) {

        final ResourceAddress bindAddress =
                resourceAddressFactory.newResourceAddress(
                        connectURI,
                        acceptOptions);

        final ResourceAddress connectAddress =
                resourceAddressFactory.newResourceAddress(
                        connectURI,
                        connectOptions);
        pipeAcceptor.bind(bindAddress, acceptHandler, null);


        ConnectFuture future = pipeConnector.connect(connectAddress,
                connectHandler, new IoSessionInitializer<ConnectFuture>() {
            @Override
            public void initializeSession(IoSession session, ConnectFuture future) {
                final NamedPipeSession s = ((NamedPipeSession)session);
                s.write(s.getBufferAllocator().wrap(ByteBuffer.wrap("Hello Kitty".getBytes()))).addListener(new IoFutureListener<WriteFuture>() {
                    @Override
                    public void operationComplete(WriteFuture future) {
                        latch.countDown(); // request sent
                    }
                });
            }
        });

        future.awaitUninterruptibly(TimeUnit.SECONDS.toMillis(3));

        UnbindFuture unbindFuture;
        try {
            if (!future.isConnected()) {
                fail("Failed to connect: " + future.getException());
            }
            latch.await(3, TimeUnit.SECONDS);
            if (latch.getCount() != 0) {
                fail("Did not connect out and loop back with an open connect session in time.");
            }
        } catch (InterruptedException e) {
            fail("Did not connect out and loop back with an open connect session in time.");
        } finally {
            unbindFuture = pipeAcceptor.unbind(bindAddress);
            unbindFuture.addListener(new IoFutureListener<UnbindFuture>() {
                @Override
                public void operationComplete(UnbindFuture future) {
                    schedulerProvider.shutdownNow();
                }
            });
        }

        unbindFuture.awaitUninterruptibly(5, TimeUnit.SECONDS);
        if (!unbindFuture.isUnbound()) {
            throw new RuntimeException("Failed to unbind http acceptor.");
        }

    }




    @Test
    public void shouldBindAndUnbindLeavingEmptyBindingsMaps() throws Exception {

        Map<String, Object> acceptOptions = new HashMap<>();

        final String connectURIString = "pipe://transport";
        final ResourceAddress bindAddress =
                resourceAddressFactory.newResourceAddress(
                        connectURIString,
                        acceptOptions);

        final IoHandler ioHandler = new IoHandlerAdapter();

        int[] rounds = new int[]{1,2,10};
        for ( int iterationCount: rounds ) {
            for ( int i = 0; i < iterationCount; i++) {
                pipeAcceptor.bind(bindAddress, ioHandler, null);
            }
            for (int j = 0; j < iterationCount; j++) {
                pipeAcceptor.unbind(bindAddress);
            }
            Assert.assertTrue(pipeAcceptor.emptyBindings());

        }
    }

}
