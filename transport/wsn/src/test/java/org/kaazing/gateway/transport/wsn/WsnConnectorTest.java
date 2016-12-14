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
package org.kaazing.gateway.transport.wsn;

import static java.nio.ByteBuffer.wrap;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.kaazing.gateway.util.Utils.asByteBuffer;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IoSession;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.BridgeSession;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.TransportFactory;
import org.kaazing.gateway.transport.http.HttpAcceptor;
import org.kaazing.gateway.transport.http.HttpConnector;
import org.kaazing.gateway.transport.nio.internal.NioSocketAcceptor;
import org.kaazing.gateway.transport.nio.internal.NioSocketConnector;
import org.kaazing.gateway.transport.ws.WsAcceptor;
import org.kaazing.gateway.transport.ws.WsConnector;
import org.kaazing.gateway.transport.ws.bridge.filter.WsBuffer;
import org.kaazing.gateway.transport.ws.bridge.filter.WsBufferAllocator;
import org.kaazing.gateway.transport.ws.extension.WebSocketExtensionFactory;
import org.kaazing.gateway.util.Utils;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.buffer.SimpleBufferAllocator;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.test.util.MethodExecutionTrace;

public class WsnConnectorTest {
    private static final int NETWORK_OPERATION_WAIT_SECS = 10; // was 3, increasing for loaded environments

    @Rule
    public MethodExecutionTrace testExecutionTrace = new MethodExecutionTrace();

    @Rule
    public TestRule timeoutRule = new DisableOnDebug(new Timeout(10, SECONDS));


    private ResourceAddressFactory addressFactory;

    private NioSocketConnector tcpConnector;
    private HttpConnector httpConnector;
    private WsnConnector wsnConnector;

    private NioSocketAcceptor tcpAcceptor;
    private HttpAcceptor httpAcceptor;
    private WsnAcceptor wsnAcceptor;
    private WsConnector wsConnector;

    @Before
    public void init() {
        SchedulerProvider schedulerProvider = new SchedulerProvider();

        addressFactory = ResourceAddressFactory.newResourceAddressFactory();
        TransportFactory transportFactory = TransportFactory.newTransportFactory(Collections.emptyMap());
        BridgeServiceFactory serviceFactory = new BridgeServiceFactory(transportFactory);

        tcpAcceptor = (NioSocketAcceptor)transportFactory.getTransport("tcp").getAcceptor();

        tcpAcceptor.setResourceAddressFactory(addressFactory);
        tcpAcceptor.setBridgeServiceFactory(serviceFactory);
        tcpAcceptor.setSchedulerProvider(schedulerProvider);

        tcpConnector = (NioSocketConnector)transportFactory.getTransport("tcp").getConnector();
        tcpConnector.setResourceAddressFactory(addressFactory);
        tcpConnector.setBridgeServiceFactory(serviceFactory);
        tcpConnector.setTcpAcceptor(tcpAcceptor);

        httpAcceptor = (HttpAcceptor)transportFactory.getTransport("http").getAcceptor();
        httpAcceptor.setBridgeServiceFactory(serviceFactory);
        httpAcceptor.setResourceAddressFactory(addressFactory);
        httpAcceptor.setSchedulerProvider(schedulerProvider);

        httpConnector = (HttpConnector)transportFactory.getTransport("http").getConnector();
        httpConnector.setBridgeServiceFactory(serviceFactory);
        httpConnector.setResourceAddressFactory(addressFactory);

        wsnAcceptor = (WsnAcceptor)transportFactory.getTransport("wsn").getAcceptor();
        wsnAcceptor.setConfiguration(new Properties());
        wsnAcceptor.setBridgeServiceFactory(serviceFactory);
        wsnAcceptor.setResourceAddressFactory(addressFactory);
        wsnAcceptor.setSchedulerProvider(schedulerProvider);
        WsAcceptor wsAcceptor = new WsAcceptor(WebSocketExtensionFactory.newInstance());
        wsnAcceptor.setWsAcceptor(wsAcceptor);

        wsnConnector = (WsnConnector)transportFactory.getTransport("wsn").getConnector();
        wsnConnector.setConfiguration(new Properties());
        wsnConnector.setBridgeServiceFactory(serviceFactory);
        wsnConnector.setSchedulerProvider(schedulerProvider);
        wsnConnector.setResourceAddressFactory(addressFactory);
        wsConnector = (WsConnector) transportFactory.getTransport("ws").getConnector();
        wsnConnector.setWsConnector(wsConnector);
    }

    @After
    public void disposeConnector() {
        if (tcpAcceptor != null) {
            tcpAcceptor.dispose();
        }
        if (httpAcceptor != null) {
            httpAcceptor.dispose();
        }
        if (wsnAcceptor != null) {
            wsnAcceptor.dispose();
        }
        if (tcpConnector != null) {
            tcpConnector.dispose();
        }
        if (httpConnector != null) {
            httpConnector.dispose();
        }
        if (wsnConnector != null) {
            wsnConnector.dispose();
        }
        if (wsConnector != null) {
            wsConnector.dispose();
        }
    }

    @Test
    public void shouldCloseWsnSessionWhenTransportClosesCleanlyButUnexpectedly() throws Exception {

        String location = "wsn://localhost:8000/echo";
        Map<String, Object> addressOptions = Collections.emptyMap(); //Collections.<String, Object>singletonMap("http.transport", URI.create("pipe://internal"));
        ResourceAddress address = addressFactory.newResourceAddress(location, addressOptions);
        final CountDownLatch waitForClientParentSessionCloseListenerEstabished = new CountDownLatch(1);
        IoHandler acceptHandler = new IoHandlerAdapter<IoSessionEx>() {


            @Override
            protected void doMessageReceived(IoSessionEx session, Object message)
                    throws Exception {
                // echo message
                IoBuffer buf = (IoBuffer)message;
                WriteFuture future = session.write(buf.duplicate());

                // close session abruptly, without triggering WebSocket close handshake
                future.addListener(new IoFutureListener<WriteFuture>() {
                    @Override
                    public void operationComplete(WriteFuture future) {
                        IoSession session = future.getSession();
                        WsnSession wsnSession = (WsnSession)session;
                        IoSession parentSession = wsnSession.getParent();
                        IoFilterChain parentFilterChain = parentSession.getFilterChain();

                        // remove WebSocket close filter to avoid intercepting filterClose
                        wsnConnector.removeBridgeFilters(parentFilterChain);

                        try {
                            boolean ok = waitForClientParentSessionCloseListenerEstabished.await(2, TimeUnit.SECONDS);
                            if ( !ok )  {
                                throw new RuntimeException("Failed to establish close listener on client-side parent session in time");
                            }
                        } catch (InterruptedException e) {
                            throw new RuntimeException("Failed to establish close listener on client-side parent session in time", e);
                        }
                        parentSession.close(true);
                    }
                });
            }

        };
        wsnAcceptor.bind(address, acceptHandler, null);

        IoHandler connectHandler = new IoHandlerAdapter<IoSessionEx>() {

            @Override
            protected void doSessionOpened(IoSessionEx session) throws Exception {
                IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
                session.write(allocator.wrap(wrap("Hello, world".getBytes())));
            }

        };

        ConnectFuture connectFuture = wsnConnector.connect(address, connectHandler, null);
        final WsnSession wsnSession = (WsnSession)connectFuture.await().getSession();
        IoSession parentSession = wsnSession.getParent();
        CloseFuture parentCloseFuture = parentSession.getCloseFuture();
        parentCloseFuture.addListener(new IoFutureListener<CloseFuture>() {
            @Override
            public void operationComplete(CloseFuture future) {
                // the parent session close future listener fires before
                // delivering close event to parent session filter chain,
                // so we can use this opportunity to force a pending write
                // when reacting to the parent session closing
                assert !wsnSession.isClosing();
                IoBufferAllocatorEx<? extends WsBuffer> wsnAllocator = wsnSession.getBufferAllocator();
                wsnSession.write(wsnAllocator.wrap(wrap("Goodbye, world".getBytes())));
            }
        });
        waitForClientParentSessionCloseListenerEstabished.countDown();
        CloseFuture closeFuture = wsnSession.getCloseFuture();
        assertTrue("WsnSession closed", closeFuture.await().isClosed());
    }

    @Test
    public void shouldCorrectlyConstructLocalAndRemoteAddressesForConnectedWsnSessions() throws Exception {

        final String location = "ws://localhost:8000/echo";
        Map<String, Object> addressOptions = Collections.emptyMap();
        ResourceAddress address = addressFactory.newResourceAddress(location, addressOptions);
        TransportTestIoHandlerAdapter acceptHandler = new TransportTestIoHandlerAdapter(1) {

            @Override
            public String getCheckpointFailureMessage() {
                return "Failed to construct accept session local/remote addresses correctly.";
            }

            @Override
            public void doMessageReceived(final IoSessionEx session, Object message)
                    throws Exception {
                // echo message
                IoBufferEx buf = (IoBufferEx)message;
                WriteFuture future = session.write(buf.duplicate());

                // close session abruptly, without triggering WebSocket close handshake
                future.addListener(new IoFutureListener<WriteFuture>() {
                    @Override
                    public void operationComplete(WriteFuture future) {
                        BridgeSession bridgeSession = (BridgeSession) session;
                        URI locationURI = URI.create(location);
                        assertEquals("remote address of accept session was not "+location, locationURI, BridgeSession.REMOTE_ADDRESS.get(bridgeSession).getResource());
                        assertEquals("local  address of accept session was not "+location, locationURI, BridgeSession.LOCAL_ADDRESS.get(bridgeSession).getResource());
                        checkpoint();
                    }
                });
            }

        };
        wsnAcceptor.bind(address, acceptHandler, null);

        TransportTestIoHandlerAdapter connectHandler = new TransportTestIoHandlerAdapter(1) {

            @Override
            public String getCheckpointFailureMessage() {
                return "Failed to construct connect session local/remote addresses correctly.";
            }

            @Override
            public void doSessionCreated(final IoSessionEx session) throws Exception {
                final IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
                session.write(allocator.wrap(ByteBuffer.wrap("Hello, world".getBytes()))).addListener(new IoFutureListener<IoFuture>() {
                    @Override
                    public void operationComplete(IoFuture future) {
                        BridgeSession bridgeSession = (BridgeSession) session;
                        URI locationURI = URI.create(location);
                        assertEquals("remote address of connect session was not " + location, locationURI, BridgeSession.REMOTE_ADDRESS.get(bridgeSession).getResource());
                        assertEquals("local  address of connect session was not " + location, locationURI, BridgeSession.LOCAL_ADDRESS.get(bridgeSession).getResource());
                        checkpoint();
                    }
                });
            }

        };

        ConnectFuture connectFuture = wsnConnector.connect(address, connectHandler, null);
        connectFuture.await(3000, TimeUnit.MILLISECONDS);
        assert connectFuture.isConnected();

        acceptHandler.await(3000, TimeUnit.MILLISECONDS);
        connectHandler.await(3000, TimeUnit.MILLISECONDS);

    }

    @Test
    @Ignore("Failing on travis CI https://github.com/kaazing/gateway/issues/162")
    public void shouldNotHangOnToHttpConnectSessionsWhenEstablishingAndTearingDownWsnConnectorSessions() throws Exception {

        long iterations = 100;

        final String location = "wsn://localhost:8000/echo";
        Map<String, Object> addressOptions = Collections.emptyMap(); //Collections.<String, Object>singletonMap("http.transport", URI.create("pipe://internal"));
        ResourceAddress address = addressFactory.newResourceAddress(location, addressOptions);


        //
        // Accept stuff, do once
        //
        final CountDownLatch acceptSessionClosed = new CountDownLatch(1);


        IoHandler acceptHandler = new IoHandlerAdapter<IoSessionEx>() {
            @Override
            public void doMessageReceived(final IoSessionEx session, Object message)
                    throws Exception {
                // echo message
                IoBufferEx buf = (IoBufferEx) message;
                IoSessionEx sessionEx = session;
                System.out.println("Acceptor: received message: " + Utils.asString(buf.buf()));
                IoBufferAllocatorEx<?> allocator = sessionEx.getBufferAllocator();
                session.write(allocator.wrap(asByteBuffer("Reply from acceptor"))).addListener(new IoFutureListener<IoFuture>() {
                    @Override
                    public void operationComplete(IoFuture future) {
                        session.close(true);
                    }
                });
            }

            @Override
            public void doSessionClosed(IoSessionEx session) throws Exception {
                acceptSessionClosed.countDown();
            }
        };
        wsnAcceptor.bind(address, acceptHandler, null);

        do {

            System.out.println("Managed http sessions: "+httpConnector.getManagedSessionCount());
            System.out.println("Managed wsn sessions: "+wsnConnector.getManagedSessionCount());

            final CountDownLatch echoReceived = new CountDownLatch(1);
            IoHandler connectHandler = new IoHandlerAdapter<IoSessionEx>() {

                @Override
                public void doSessionOpened(IoSessionEx session) throws Exception {
                    //session.write(wrap("Hello, world".getBytes()));
                }

                @Override
                public void doMessageReceived(IoSessionEx session, Object message) throws Exception {
                }

                @Override
                protected void doSessionClosed(IoSessionEx session) throws Exception {
                    echoReceived.countDown();
                }
            };

            ConnectFuture connectFuture = wsnConnector.connect(address, connectHandler, null);
            final WsnSession session = (WsnSession)connectFuture.await().getSession();
            session.write(new WsBufferAllocator(SimpleBufferAllocator.BUFFER_ALLOCATOR).wrap(Utils.asByteBuffer("Message from connector")));
            waitForLatch(echoReceived, NETWORK_OPERATION_WAIT_SECS, TimeUnit.SECONDS, "echo not received");





        } while (--iterations > 0);

        System.out.println("Managed http sessions: "+httpConnector.getManagedSessionCount());
        System.out.println("Managed wsn sessions: "+wsnConnector.getManagedSessionCount());

        Assert.assertEquals(0, wsnConnector.getManagedSessionCount());
        Assert.assertEquals(0, httpConnector.getManagedSessionCount());
    }

    private static void waitForLatch(CountDownLatch l,
                                    final int delay,
                                    final TimeUnit unit,
                                    final String failureMessage)
            throws InterruptedException {

        l.await(delay, unit);
        if ( l.getCount() != 0 ) {
            fail(failureMessage);
        }
    }

}
