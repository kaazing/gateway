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
package org.kaazing.gateway.transport.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.kaazing.gateway.resource.address.ResourceAddress.TRANSPORT;
import static org.kaazing.gateway.resource.address.ResourceAddressFactory.newResourceAddressFactory;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.PropertyConfigurator;
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
import org.junit.Test;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.CommitFuture;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.TransportFactory;
import org.kaazing.gateway.transport.nio.internal.NioSocketAcceptor;
import org.kaazing.gateway.transport.nio.internal.NioSocketConnector;
import org.kaazing.gateway.transport.pipe.NamedPipeAcceptor;
import org.kaazing.gateway.transport.pipe.NamedPipeConnector;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;
import org.kaazing.mina.core.future.UnbindFuture;
import org.kaazing.mina.core.session.IoSessionEx;

public class HttpTransportTest {

    private static final boolean DEBUG = false;

    @BeforeClass
    public static void init()
            throws Exception {

        if (DEBUG) {
            PropertyConfigurator.configure("src/test/resources/log4j-trace.properties");
        }
    }

    ResourceAddressFactory resourceAddressFactory = newResourceAddressFactory();
    BridgeServiceFactory bridgeServiceFactory;
    SchedulerProvider schedulerProvider = new SchedulerProvider();

    NioSocketAcceptor tcpAcceptor;
    HttpAcceptor httpAcceptor;
    NamedPipeAcceptor pipeAcceptor;
    NioSocketConnector tcpConnector;
    HttpConnector httpConnector;
    NamedPipeConnector pipeConnector;
    
    Set<Exception> failures = new ConcurrentHashSet<>();

    @Before
    public void before() {
        Map<String, ?> config = Collections.emptyMap();
        TransportFactory transportFactory = TransportFactory.newTransportFactory(config);
        bridgeServiceFactory = new BridgeServiceFactory(transportFactory);

        schedulerProvider = new SchedulerProvider();

        tcpAcceptor = (NioSocketAcceptor)transportFactory.getTransport("tcp").getAcceptor();

        tcpAcceptor.setResourceAddressFactory(resourceAddressFactory);
        tcpAcceptor.setBridgeServiceFactory(bridgeServiceFactory);
        tcpAcceptor.setSchedulerProvider(schedulerProvider);

        tcpConnector = (NioSocketConnector)transportFactory.getTransport("tcp").getConnector();
        tcpConnector.setResourceAddressFactory(resourceAddressFactory);
        tcpConnector.setBridgeServiceFactory(bridgeServiceFactory);
        tcpConnector.setTcpAcceptor(tcpAcceptor);

        pipeAcceptor = (NamedPipeAcceptor)transportFactory.getTransport("pipe").getAcceptor();
        httpAcceptor = (HttpAcceptor)transportFactory.getTransport("http").getAcceptor();
        pipeConnector = (NamedPipeConnector)transportFactory.getTransport("pipe").getConnector();
        httpConnector = (HttpConnector)transportFactory.getTransport("http").getConnector();

        initBridgeServices();
        
        failures = new ConcurrentHashSet<>();
    }

    @After
    public void after() {
        tcpAcceptor.dispose(); tcpConnector.dispose();
        pipeAcceptor.dispose(); httpAcceptor.dispose();
        httpConnector.dispose(); pipeConnector.dispose();
    }

    private void initBridgeServices() {
        pipeAcceptor.setResourceAddressFactory(resourceAddressFactory);
        pipeAcceptor.setBridgeServiceFactory(bridgeServiceFactory);

        httpAcceptor.setBridgeServiceFactory(bridgeServiceFactory);
        httpAcceptor.setResourceAddressFactory(resourceAddressFactory);
        httpAcceptor.setSchedulerProvider(schedulerProvider);

        pipeConnector.setResourceAddressFactory(resourceAddressFactory);
        pipeConnector.setBridgeServiceFactory(bridgeServiceFactory);
        pipeConnector.setNamedPipeAcceptor(pipeAcceptor);

        httpConnector.setBridgeServiceFactory(bridgeServiceFactory);
        httpConnector.setResourceAddressFactory(resourceAddressFactory);

    }

    @Test
    public void sampleTest() {
        final CountDownLatch latch = new CountDownLatch(4);

        final IoHandlerAdapter connectHandler = new IoHandlerAdapter<IoSessionEx>() {
            @Override
            protected void doSessionCreated(IoSessionEx session) throws Exception {
                /*
                   local and remote addresses for connect session should be

                   [http://localhost:8000/path]
                       [pipe://transport http/1.1]
                */
                ResourceAddress localHttpAddress = ((HttpSession) session).getLocalAddress();
                ResourceAddress localTcpAddress = localHttpAddress.getTransport();
                assertEquals(localHttpAddress.getResource(), URI.create("http://localhost:8000/path"));
                assertEquals(localHttpAddress.getOption(ResourceAddress.NEXT_PROTOCOL), null);
                assertEquals(localTcpAddress.getResource(), URI.create("pipe://transport"));
                assertEquals(localTcpAddress.getOption(ResourceAddress.NEXT_PROTOCOL), "http/1.1");

                ResourceAddress remoteHttpAddress = ((HttpSession) session).getRemoteAddress();
                ResourceAddress remoteTcpAddress = remoteHttpAddress.getTransport();
                assertEquals(remoteHttpAddress.getResource(), URI.create("http://localhost:8000/path"));
                assertEquals(remoteHttpAddress.getOption(ResourceAddress.NEXT_PROTOCOL), null);
                assertEquals(localTcpAddress.getResource(), URI.create("pipe://transport"));
                assertEquals(remoteTcpAddress.getOption(ResourceAddress.NEXT_PROTOCOL), "http/1.1");

                latch.countDown();
            }
        };

        final IoHandlerAdapter acceptHandler = new IoHandlerAdapter<IoSessionEx>() {

            @Override
            public void doSessionCreated(final IoSessionEx session) throws Exception {
                /*
                   local and remote addresses for accept session should be

                   [http://localhost:8000/path]
                       [pipe://transport http/1.1]
                */
                ResourceAddress localHttpAddress = ((HttpSession) session).getLocalAddress();
                ResourceAddress localTcpAddress = localHttpAddress.getTransport();
                assertEquals(localHttpAddress.getResource(), URI.create("http://localhost:8000/path"));
                assertEquals(localHttpAddress.getOption(ResourceAddress.NEXT_PROTOCOL), null);
                assertEquals(localTcpAddress.getResource(), URI.create("pipe://transport"));
                assertEquals(localTcpAddress.getOption(ResourceAddress.NEXT_PROTOCOL), "http/1.1");

                ResourceAddress remoteHttpAddress = ((HttpSession) session).getRemoteAddress();
                ResourceAddress remoteTcpAddress = remoteHttpAddress.getTransport();
                assertEquals(remoteHttpAddress.getResource(), URI.create("http://localhost:8000/path"));
                assertEquals(remoteHttpAddress.getOption(ResourceAddress.NEXT_PROTOCOL), null);
                assertEquals(localTcpAddress.getResource(), URI.create("pipe://transport"));
                assertEquals(remoteTcpAddress.getOption(ResourceAddress.NEXT_PROTOCOL), "http/1.1");

                latch.countDown();
            }

            @Override
            protected void doMessageReceived(final IoSessionEx session, Object message) throws Exception {
                System.out.println("Server Http Session message received: "+message);
                CommitFuture commitFuture = ((DefaultHttpSession) session).commit();
                commitFuture.addListener(new IoFutureListener<CommitFuture>() {
                    @Override
                    public void operationComplete(CommitFuture future) {
                        session.close(false).addListener(new IoFutureListener<CloseFuture>() {
                            @Override
                            public void operationComplete(CloseFuture future) {
                                System.out.println("Server Http Session closed");

                                latch.countDown();
                            }
                        });
                    }
                });
            }
        };

        Map<String, Object> bindOptions = new HashMap<>();
        bindOptions.put(TRANSPORT.name(), "pipe://transport");
        final Map<String, Object> connectOptions = new HashMap<>();
        connectOptions.put(TRANSPORT.name(), "pipe://transport");

        httpConnectorToAcceptor("http://localhost:8000/path",
                acceptHandler, connectHandler,
                bindOptions, connectOptions, latch);
    }

    @Test
    public void shouldHaveCorrectAddressesForHttpOverTcp() {

        final CountDownLatch latch = new CountDownLatch(4);

        final IoHandlerAdapter connectHandler = new IoHandlerAdapter<IoSessionEx>() {

            @Override
            protected void doSessionCreated(IoSessionEx session) throws Exception {
                /*
                   local address for connect session should be

                   [http://localhost:8000/path]
                       [tcp://127.0.0.1:54227 http/1.1]
                */
                ResourceAddress localHttpAddress = ((HttpSession) session).getLocalAddress();
                ResourceAddress localTcpAddress = localHttpAddress.getTransport();
                assertEquals(localHttpAddress.getResource(), URI.create("http://localhost:8000/path"));
                assertEquals(localHttpAddress.getOption(ResourceAddress.NEXT_PROTOCOL), null);
                assertFalse(localTcpAddress.getResource().getAuthority().contains("8000"));
                assertEquals(localTcpAddress.getOption(ResourceAddress.NEXT_PROTOCOL), "http/1.1");

                /*
                   remote address for connect session should be

                  [http://localhost:8000/path]
                      [tcp://127.0.0.1:8000 (tcp://localhost:8000) http/1.1]
                */
                ResourceAddress remoteHttpAddress = ((HttpSession) session).getRemoteAddress();
                ResourceAddress remoteTcpAddress = remoteHttpAddress.getTransport();
                assertEquals(remoteHttpAddress.getResource(), URI.create("http://localhost:8000/path"));
                assertEquals(remoteHttpAddress.getOption(ResourceAddress.NEXT_PROTOCOL), null);
                assertTrue(remoteTcpAddress.getResource().getAuthority().contains("8000"));
                assertEquals(remoteTcpAddress.getOption(ResourceAddress.NEXT_PROTOCOL), "http/1.1");

                latch.countDown();
            }
        };

        final IoHandlerAdapter acceptHandler = new IoHandlerAdapter<IoSessionEx>() {

            @Override
            public void doSessionCreated(final IoSessionEx session) throws Exception {
                /*
                   local address for accept session should be

                  [http://localhost:8000/path]
                      [tcp://127.0.0.1:8000 (tcp://localhost:8000) http/1.1]
                */
                ResourceAddress localHttpAddress = ((HttpSession) session).getLocalAddress();
                ResourceAddress localTcpAddress = localHttpAddress.getTransport();
                assertEquals(localHttpAddress.getResource(), URI.create("http://localhost:8000/path"));
                assertEquals(localHttpAddress.getOption(ResourceAddress.NEXT_PROTOCOL), null);
                assertTrue(localTcpAddress.getResource().getAuthority().contains("8000"));
                assertEquals(localTcpAddress.getOption(ResourceAddress.NEXT_PROTOCOL), "http/1.1");

                /*
                   remote address for accept session should be

                   [http://localhost:8000/path]
                       [tcp://127.0.0.1:54227 http/1.1]
                */
                ResourceAddress remoteHttpAddress = ((HttpSession) session).getRemoteAddress();
                ResourceAddress remoteTcpAddress = remoteHttpAddress.getTransport();
                assertEquals(remoteHttpAddress.getResource(), URI.create("http://localhost:8000/path"));
                assertEquals(remoteHttpAddress.getOption(ResourceAddress.NEXT_PROTOCOL), null);
                assertFalse(remoteTcpAddress.getResource().getAuthority().contains("8000"));
                assertEquals(remoteTcpAddress.getOption(ResourceAddress.NEXT_PROTOCOL), "http/1.1");

                latch.countDown();
            }

            @Override
            protected void doMessageReceived(final IoSessionEx session, Object message) throws Exception {
                CommitFuture commitFuture = ((DefaultHttpSession) session).commit();
                commitFuture.addListener(new IoFutureListener<CommitFuture>() {
                    @Override
                    public void operationComplete(CommitFuture future) {
                        session.close(false).addListener(new IoFutureListener<CloseFuture>() {
                            @Override
                            public void operationComplete(CloseFuture future) {
                                latch.countDown();
                            }
                        });
                    }
                });
            }
        };

        Map<String, Object> bindOptions = Collections.emptyMap();
        Map<String, Object> connectOptions = Collections.emptyMap();
        httpConnectorToAcceptor("http://localhost:8000/path", acceptHandler, connectHandler,
                bindOptions, connectOptions, latch);
    }

    @Test
    public void shouldBindAndUnbindLeavingEmptyBindingsMaps() throws Exception {

        Map<String, Object> acceptOptions = new HashMap<>();
        acceptOptions.put(TRANSPORT.name(), "pipe://transport");

        final String connectURIString = "http://localhost:8000/path";
        final ResourceAddress bindAddress =
                resourceAddressFactory.newResourceAddress(
                        connectURIString,
                        acceptOptions);

        final IoHandler ioHandler = new IoHandlerAdapter();

        int[] rounds = new int[]{1,2,10};
        for ( int iterationCount: rounds ) {
            for ( int i = 0; i < iterationCount; i++) {
                httpAcceptor.bind(bindAddress, ioHandler, null);
            }
            for (int j = 0; j < iterationCount; j++) {
                httpAcceptor.unbind(bindAddress);
            }
            Assert.assertTrue(httpAcceptor.emptyBindings());

        }
    }

    private void httpConnectorToAcceptor(final String connectURI, IoHandlerAdapter acceptHandler,
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
        httpAcceptor.bind(bindAddress, acceptHandler, null);


        ConnectFuture future = httpConnector.connect(connectAddress,
                connectHandler, new IoSessionInitializer<ConnectFuture>() {
            @Override
            public void initializeSession(IoSession session, ConnectFuture future) {
                DefaultHttpSession s = ((DefaultHttpSession)session);
                s.setMethod(HttpMethod.POST);
                s.setRequestURI(URI.create("/path"));
                s.setVersion(HttpVersion.HTTP_1_1);
                s.setWriteHeader("Host", "localhost:8000");
                s.setWriteHeader("Content-Length", "11");

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
            unbindFuture = httpAcceptor.unbind(bindAddress);
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

}
