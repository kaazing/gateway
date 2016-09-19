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

import static org.kaazing.gateway.util.InternalSystemProperty.WS_CLOSE_TIMEOUT;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.BridgeAcceptor;
import org.kaazing.gateway.transport.BridgeConnector;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.BridgeSession;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.TransportFactory;
import org.kaazing.gateway.transport.http.HttpAcceptor;
import org.kaazing.gateway.transport.http.HttpConnector;
import org.kaazing.gateway.transport.nio.internal.NioSocketAcceptor;
import org.kaazing.gateway.transport.nio.internal.NioSocketConnector;
import org.kaazing.gateway.transport.ws.WsAcceptor;
import org.kaazing.gateway.transport.ws.WsCloseMessage;
import org.kaazing.gateway.transport.ws.WsConnector;
import org.kaazing.gateway.transport.ws.WsFilterAdapter;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.future.UnbindFuture;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.test.util.MethodExecutionTrace;

/**
* This class contains tests to verify our wsn transport layer conforms to RFC-6455 when closing WebSocket
* connections. It does this by creating communicating WsnAcceptor and WsnConnector instances. The
* expectation is that a clean close is done: closing side sends WS_CLOSE frame and waits a configurable
* time for a WS_CLOSE frame response before closing the underlying connection.
*/

public class WsCloseTransportTest {
    @Rule
    public TestRule testExecutionTrace = new MethodExecutionTrace();

//    private static final boolean DEBUG = false;
    private static final boolean DEBUG = true;

    private static final String CLIENT_CLOSE_FILTER = "CloseFilterTest#client";
    private static final String SERVER_CLOSE_FILTER = "CloseFilterTest#server";

    // In seconds
    private static final Integer SESSION_LATCH_TIMEOUT = 20; // 5;
    private static final Integer ADAPTER_LATCH_TIMEOUT = 12; // 2;

    // To speed up test execution, use a shorter time when we do not expect the event
    private static final Integer ADAPTER_LATCH_UNEXPECTED_TIMEOUT = 2;

    private final ResourceAddressFactory resourceAddressFactory = ResourceAddressFactory.newResourceAddressFactory();
    TransportFactory transportFactory = TransportFactory.newTransportFactory(Collections.emptyMap());
    private final BridgeServiceFactory bridgeServiceFactory = new BridgeServiceFactory(transportFactory);
//    private final ServiceRegistry serviceRegistry = new ServiceRegistry();
    private WsnAcceptor wsnAcceptor;
    private WsnConnector wsnConnector;
    private HttpAcceptor httpAcceptor;
    private HttpConnector httpConnector;
    private NioSocketAcceptor tcpAcceptor;
    private NioSocketConnector tcpConnector;
    private SchedulerProvider schedulerProvider;
    private WsConnector wsConnector;

    private void waitForLatch(CountDownLatch l,
                              final int delay,
                              final TimeUnit unit,
                              final long expectedCount,
                              final String failureMessage)
        throws InterruptedException {

        l.await(delay, unit);
        if (l.getCount() != expectedCount) {
            Assert.fail(failureMessage);
        }
    }

    private void waitForLatch(CountDownLatch l,
                              final int delay,
                              final TimeUnit unit,
                              final String failureMessage)
        throws InterruptedException {

        waitForLatch(l, delay, unit, 0, failureMessage);
    }

    @After
    public void tearDown() throws Exception {
        // For reasons unknown, tcpAcceptor.unbind does not actually free up the bound port until dispose is called.
        // So we must dispose to avoid the next test method failing to bind.
        wsnConnector.dispose(); httpConnector.dispose(); tcpConnector.dispose(); wsConnector.dispose();
        wsnAcceptor.dispose(); httpAcceptor.dispose(); tcpAcceptor.dispose();
        schedulerProvider.shutdownNow();
    }

    private void connectAndClose(final boolean clientStarts,
                                 final WsCloseMessage closeMessage,
                                 final WsFilterAdapter connectFilterAdapter,
                                 final WsFilterAdapter acceptFilterAdapter,
                                 final String wsConnectCloseTimeout,
                                 final String wsAcceptCloseTimeout)
        throws Exception {

//        Mockery context = new Mockery() {
//            {
//                setImposteriser(ClassImposteriser.INSTANCE);
//            }
//        };
//        context.setThreadingPolicy(new Synchroniser());

//        final ServiceContext serviceContext = context.mock(ServiceContext.class);
//        final AcceptOptionsContext acceptOptionsContext = context.mock(AcceptOptionsContext.class);

        wsnAcceptor = (WsnAcceptor)transportFactory.getTransport("wsn").getAcceptor();
        wsnConnector = (WsnConnector)transportFactory.getTransport("wsn").getConnector();
        httpAcceptor = (HttpAcceptor)transportFactory.getTransport("http").getAcceptor();
        httpConnector = (HttpConnector)transportFactory.getTransport("http").getConnector();
        tcpAcceptor = (NioSocketAcceptor)transportFactory.getTransport("tcp").getAcceptor();
        tcpConnector = (NioSocketConnector)transportFactory.getTransport("tcp").getConnector();
        schedulerProvider = new SchedulerProvider();
        WsAcceptor wsAcceptor = (WsAcceptor)transportFactory.getTransport("ws").getAcceptor();
        wsAcceptor.setWsnAcceptor(wsnAcceptor);
        wsAcceptor.setConfiguration(new Properties());
        wsConnector = (WsConnector)transportFactory.getTransport("ws").getConnector();
        wsConnector.setWsnConnector(wsnConnector);


        final Properties wsAcceptProperties = new Properties();
        if (wsAcceptCloseTimeout != null) {
            wsAcceptProperties.setProperty(WS_CLOSE_TIMEOUT.getPropertyName(), wsAcceptCloseTimeout);
        }

        final Properties wsConnectProperties = new Properties();
        if (wsConnectCloseTimeout != null) {
            wsConnectProperties.setProperty(WS_CLOSE_TIMEOUT.getPropertyName(), wsConnectCloseTimeout);
        }

        wsnAcceptor.setBridgeServiceFactory(bridgeServiceFactory);
        wsnAcceptor.setResourceAddressFactory(resourceAddressFactory);
        wsnAcceptor.setSchedulerProvider(schedulerProvider);
        wsnAcceptor.setWsAcceptor(wsAcceptor);
        if (wsAcceptCloseTimeout != null) {
            wsnAcceptor.setConfiguration(wsAcceptProperties);
        }

        wsnConnector.setBridgeServiceFactory(bridgeServiceFactory);
        wsnConnector.setSchedulerProvider(schedulerProvider);
        wsnConnector.setResourceAddressFactory(resourceAddressFactory);
        if (wsConnectCloseTimeout != null) {
            wsnConnector.setConfiguration(wsConnectProperties);
        }
        wsConnector = (WsConnector) transportFactory.getTransport("ws").getConnector();
        wsnConnector.setWsConnector(wsConnector);

        httpAcceptor.setBridgeServiceFactory(bridgeServiceFactory);
//        httpAcceptor.setServiceRegistry(serviceRegistry);
        httpAcceptor.setResourceAddressFactory(resourceAddressFactory);
        httpAcceptor.setSchedulerProvider(schedulerProvider);

        httpConnector.setBridgeServiceFactory(bridgeServiceFactory);
        httpConnector.setResourceAddressFactory(resourceAddressFactory);

        tcpAcceptor.setBridgeServiceFactory(bridgeServiceFactory);
        tcpAcceptor.setResourceAddressFactory(resourceAddressFactory);
        tcpAcceptor.setSchedulerProvider(schedulerProvider);
        tcpConnector.setBridgeServiceFactory(bridgeServiceFactory);
        tcpConnector.setResourceAddressFactory(resourceAddressFactory);
        tcpConnector.setTcpAcceptor(tcpAcceptor);

        // hit server then back to client with expected messages.

        final CountDownLatch clientSessionOpened = new CountDownLatch(1);
        final CountDownLatch serverSessionOpened = new CountDownLatch(1);
        final CountDownLatch messageReceived = new CountDownLatch(1);
        final CountDownLatch messageSent = new CountDownLatch(1);
        final CountDownLatch clientSessionClosed = new CountDownLatch(1);
        final CountDownLatch serverSessionClosed = new CountDownLatch(1);

        final IoHandlerAdapter<IoSessionEx> acceptHandler = new IoHandlerAdapter<IoSessionEx>() {

            @Override
            protected void doSessionCreated(final IoSessionEx session)
                throws Exception {

                if (DEBUG) {
                    System.out.println(String.format("WS server: doSessionCreated: %s", session));
                }

                if (acceptFilterAdapter != null) {
                    BridgeSession bridgeSession = (BridgeSession) session;
                    IoSession parentSession = bridgeSession.getParent();
                    IoFilterChain filterChain = parentSession.getFilterChain();
                    if (filterChain.contains(WsAcceptor.CLOSE_FILTER)) {
                        filterChain.addBefore(WsAcceptor.CLOSE_FILTER, SERVER_CLOSE_FILTER, acceptFilterAdapter);
                    }
                }
            }

            @Override
            protected void doSessionOpened(final IoSessionEx session)
                throws Exception {

                if (DEBUG) {
                    System.out.println(String.format("WS server: doSessionOpened: %s", session));
                }

                serverSessionOpened.countDown();

                if (!clientStarts) {
                    IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
                    WriteFuture writeFuture = session.write(allocator.wrap(ByteBuffer.wrap("How's it going Sven?".getBytes())));
                    writeFuture.addListener(new IoFutureListener<WriteFuture>() {
                        @Override
                        public void operationComplete(WriteFuture future) {
                            if (future.isWritten()) {
                                messageSent.countDown();
                            }
                        }
                    });
                }
            }

            @Override
            protected void doSessionClosed(final IoSessionEx session)
                throws Exception {

                if (DEBUG) {
                    System.out.println(String.format("WS server: doSessionClosed: %s", session));
                }

                // Do NOT count down our latch until the parent (i.e. underlying TCP) session closes
                BridgeSession bridge = (BridgeSession) session;
                IoSession parent = bridge.getParent();
                CloseFuture closeFuture = parent.getCloseFuture();
                closeFuture.addListener(new IoFutureListener<CloseFuture>() {
                    @Override
                    public void operationComplete(CloseFuture future) {
                        serverSessionClosed.countDown();
                    }
                });
            }

            @Override
            protected void doMessageReceived(final IoSessionEx session,
                                             Object message)
                throws Exception {

                if (DEBUG) {
                    System.out.println("WS server: doMessageReceived: " + message);
                }

                if (message instanceof IoBuffer) {
                    if (DEBUG) {
                        final IoBuffer buffer = (IoBuffer) message;
                        String incoming = new String(buffer.array(), buffer.arrayOffset(), buffer.remaining());
                        System.out.println("WS server message contents: " + incoming);
                    }

                    if (clientStarts) {
                        // We echo the message back, but do NOT close the session
                        session.write(message);
                    }
                }

                if (!clientStarts) {
                    messageReceived.countDown();

                    if (closeMessage != null) {
                        BridgeSession bridgeSession = (BridgeSession) session;
                        WriteFuture writeFuture = bridgeSession.getParent().write(closeMessage);
                        writeFuture.addListener(new IoFutureListener<WriteFuture>() {
                            @Override
                            public void operationComplete(WriteFuture future) {
                                if (future.isWritten()) {
                                    session.close(false);
                                }
                            }
                        });

                    } else {
                        session.close(false);
                    }
                }
            }
        };

        final IoHandlerAdapter<IoSessionEx> connectHandler = new IoHandlerAdapter<IoSessionEx>() {
            @Override
            protected void doSessionCreated(IoSessionEx session)
                throws Exception {

                if (DEBUG) {
                    System.out.println(String.format("WS client: doSessionCreated: %s", session));
                }

                if (connectFilterAdapter != null) {
                    BridgeSession bridgeSession = (BridgeSession) session;
                    IoSession parentSession = bridgeSession.getParent();
                    IoFilterChain filterChain = parentSession.getFilterChain();
                    if (filterChain.contains(WsAcceptor.CLOSE_FILTER)) {
                        filterChain.addBefore(WsAcceptor.CLOSE_FILTER, CLIENT_CLOSE_FILTER, connectFilterAdapter);
                    }
                }
            }

            @Override
            protected void doSessionOpened(IoSessionEx session)
                throws Exception {

                if (DEBUG) {
                    System.out.println(String.format("WS client: doSessionOpened: %s", session));
                }

                clientSessionOpened.countDown();

                if (clientStarts) {
                    IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
                    WriteFuture writeFuture = session.write(allocator.wrap(ByteBuffer.wrap("How's it going Sven?".getBytes())));
                    writeFuture.addListener(new IoFutureListener<WriteFuture>() {
                        @Override
                        public void operationComplete(WriteFuture future) {
                            if (future.isWritten()) {
                                messageSent.countDown();
                            }
                        }
                    });
                }
            }

            @Override
            protected void doSessionClosed(final IoSessionEx session)
                throws Exception {

                if (DEBUG) {
                    System.out.println(String.format("WS client: doSessionClosed: %s", session));
                }

                // Do NOT count down our latch until the parent (i.e. underlying TCP) session closes
                BridgeSession bridge = (BridgeSession) session;
                IoSession parent = bridge.getParent();
                CloseFuture closeFuture = parent.getCloseFuture();
                closeFuture.addListener(new IoFutureListener<CloseFuture>() {
                    @Override
                    public void operationComplete(CloseFuture future) {
                        clientSessionClosed.countDown();
                    }
                });
            }

            @Override
            protected void doMessageReceived(final IoSessionEx session,
                                             Object message)
                throws Exception {

                if (DEBUG) {
                    System.out.println("WS client: messageReceived " + message);
                }

                if (message instanceof IoBuffer) {
                    if (DEBUG) {
                        final IoBuffer buffer = (IoBuffer) message;
                        String incoming = new String(buffer.array(), buffer.arrayOffset(), buffer.remaining());
                        System.out.println("WS client message contents: " + incoming);
                    }

                    if (!clientStarts) {
                        // We echo the message back, but do NOT close the session
                        session.write(message);
                    }
                }

                if (clientStarts) {
                    messageReceived.countDown();

                    if (closeMessage != null) {
                        BridgeSession bridgeSession = (BridgeSession) session;
                        WriteFuture writeFuture = bridgeSession.getParent().write(closeMessage);
                        writeFuture.addListener(new IoFutureListener<WriteFuture>() {
                            @Override
                            public void operationComplete(WriteFuture future) {
                                if (future.isWritten()) {
                                    session.close(false);
                                }
                            }
                        });

                    } else {
                        session.close(false);
                    }
                }
            }
        };

        final String uri = "wsn://localhost:4444/echo";
        final List<String> accepts = new ArrayList<>(1);
        accepts.add(uri);

//        final Map<URI, ? extends CrossSiteConstraintContext> crossSiteConstraints = new HashMap<URI, DefaultCrossSiteConstraintContext>();

        final List<String> requiredRoles = new ArrayList<>(1);
        requiredRoles.add("*");

        final ResourceAddress resourceAddress =
            resourceAddressFactory.newResourceAddress(uri);

//        context.checking(new Expectations() {
//            {
//                allowing(serviceContext).getAccepts(); will(returnValue(accepts));
//                allowing(serviceContext).getCrossSiteConstraints(); will(returnValue(crossSiteConstraints));
//                allowing(serviceContext).getRequireRoles(); will(returnValue(requiredRoles));
//                //allowing(serviceContext).getHttpChallengeScheme(); will(returnValue("Basic"));
//                allowing(serviceContext).getAuthorizationMode(); will(returnValue(null));
//                allowing(serviceContext).getServiceRealm(); will(returnValue(null));
//                allowing(serviceContext).getAcceptOptionsContext(); will(returnValue(acceptOptionsContext));
//                allowing(acceptOptionsContext).getSessionIdleTimeout("http"); will(returnValue(null));
//            }
//        });

//        serviceRegistry.register(uri, serviceContext);

        BridgeAcceptor bridgeAcceptor = bridgeServiceFactory.newBridgeAcceptor(resourceAddress);
        bridgeAcceptor.bind(resourceAddress, acceptHandler, null);

        BridgeConnector bridgeConnector = bridgeServiceFactory.newBridgeConnector(resourceAddress);

        ConnectFuture future = bridgeConnector.connect(resourceAddress,
            connectHandler, null);

        future.awaitUninterruptibly(TimeUnit.SECONDS.toMillis(5));
        if (!future.isConnected()) {
            throw new RuntimeException(String.format("Failed to connect to %s: %s", uri, future.getException()), future.getException());
        }

        IoSession clientSession = future.getSession();

        UnbindFuture unbindFuture;
        try {
            waitForLatch(clientSessionOpened, SESSION_LATCH_TIMEOUT,
                TimeUnit.SECONDS,
                "Did not complete WS handshake on client in time");
            waitForLatch(serverSessionOpened, SESSION_LATCH_TIMEOUT,
                TimeUnit.SECONDS,
                "Did not complete WS handshake on server in time");
            waitForLatch(messageSent, SESSION_LATCH_TIMEOUT, TimeUnit.SECONDS,
                "Did not send out a message in time");
            waitForLatch(messageReceived, SESSION_LATCH_TIMEOUT, TimeUnit.SECONDS,
                "Did not receive a message in time");

            // This is a bit of testing hack.  If accept properties are present,
            // it probably means that the server/accept side will be timing out
            // the session, in which case the client session may not close
            // cleanly.
            if (wsAcceptCloseTimeout != null) {
                try {
                    Thread.sleep(750);

                } catch (Exception e) {
                    // ignore
                }

                clientSession.close(true);
            }

            waitForLatch(clientSessionClosed, SESSION_LATCH_TIMEOUT,
               TimeUnit.SECONDS, "Did not complete WS close on client in time");

            waitForLatch(serverSessionClosed, SESSION_LATCH_TIMEOUT,
               TimeUnit.SECONDS, "Did not complete WS close on server in time");

        } catch (InterruptedException e) {
            Assert.fail("Interrupted while waiting for latches");

        } finally {
            unbindFuture = bridgeAcceptor.unbind(resourceAddress);
            unbindFuture.addListener(new IoFutureListener<UnbindFuture>() {
                @Override
                public void operationComplete(UnbindFuture future) {
                    schedulerProvider.shutdownNow();
                }
            });
        }

        unbindFuture.awaitUninterruptibly(5, TimeUnit.SECONDS);
        if (!unbindFuture.isUnbound()) {
            Throwable t = unbindFuture.getException();

            throw new RuntimeException(String.format("Failed to unbind WS acceptor for %s: %s", uri, t != null ? t : "(timeout?)"));
        }
    }

    @Test
    public void shouldConnectAndCloseExplicitlyByClient()
        throws Exception {

        CloseLatchAdapter connectLatchAdapter = new CloseLatchAdapter();
        CloseLatchAdapter acceptLatchAdapter = new CloseLatchAdapter();
        connectAndClose(true, WsCloseMessage.NORMAL_CLOSE,
            connectLatchAdapter, acceptLatchAdapter, null, null);

        waitForLatch(acceptLatchAdapter.getReceivedClose(), ADAPTER_LATCH_TIMEOUT,
            TimeUnit.SECONDS, "Server did not receive CLOSE frame as expected");
        waitForLatch(acceptLatchAdapter.getSentClose(), ADAPTER_LATCH_TIMEOUT,
            TimeUnit.SECONDS, "Server did not send CLOSE frame as expected");
        waitForLatch(connectLatchAdapter.getReceivedClose(),
            ADAPTER_LATCH_TIMEOUT, TimeUnit.SECONDS,
            "Client did not receive CLOSE frame as expected");
        waitForLatch(connectLatchAdapter.getSentClose(), ADAPTER_LATCH_TIMEOUT,
            TimeUnit.SECONDS, "Client did not send CLOSE frame as expected");
    }

    @Test
    public void shouldConnectAndCloseExplicitlyByServer()
        throws Exception {

        CloseLatchAdapter connectLatchAdapter = new CloseLatchAdapter();
        CloseLatchAdapter acceptLatchAdapter = new CloseLatchAdapter();
        connectAndClose(false, WsCloseMessage.UNEXPECTED_CONDITION,
            connectLatchAdapter, acceptLatchAdapter, null, null);

        waitForLatch(acceptLatchAdapter.getReceivedClose(),
            ADAPTER_LATCH_TIMEOUT, TimeUnit.SECONDS,
            "Server did not receive CLOSE frame as expected");
        waitForLatch(acceptLatchAdapter.getSentClose(), ADAPTER_LATCH_TIMEOUT,
            TimeUnit.SECONDS, "Server did not send CLOSE frame as expected");
        waitForLatch(connectLatchAdapter.getReceivedClose(),
            ADAPTER_LATCH_TIMEOUT, TimeUnit.SECONDS,
            "Client did not receive CLOSE frame as expected");
        waitForLatch(connectLatchAdapter.getSentClose(), ADAPTER_LATCH_TIMEOUT,
            TimeUnit.SECONDS, "Client did not send CLOSE frame as expected");
    }

    @Test
    public void shouldConnectAndCloseImplicitlyByClient()
        throws Exception {

        CloseLatchAdapter connectLatchAdapter = new CloseLatchAdapter();
        CloseLatchAdapter acceptLatchAdapter = new CloseLatchAdapter();
        connectAndClose(true, null, connectLatchAdapter, acceptLatchAdapter,
            null, null);

        waitForLatch(acceptLatchAdapter.getReceivedClose(), ADAPTER_LATCH_TIMEOUT,
            TimeUnit.SECONDS, "Server did not receive CLOSE frame as expected");
        waitForLatch(acceptLatchAdapter.getSentClose(), ADAPTER_LATCH_TIMEOUT,
            TimeUnit.SECONDS, "Server did not send CLOSE frame as expected");
        waitForLatch(connectLatchAdapter.getReceivedClose(),
            ADAPTER_LATCH_TIMEOUT, TimeUnit.SECONDS,
            "Client did not receive CLOSE frame as expected");
        waitForLatch(connectLatchAdapter.getSentClose(), ADAPTER_LATCH_TIMEOUT,
            TimeUnit.SECONDS, "Client did not send CLOSE frame as expected");
    }

    @Test
    public void shouldConnectAndCloseImplicitlyByServer()
        throws Exception {

        CloseLatchAdapter connectLatchAdapter = new CloseLatchAdapter();
        CloseLatchAdapter acceptLatchAdapter = new CloseLatchAdapter();
        connectAndClose(false, null, connectLatchAdapter, acceptLatchAdapter,
            null, null);

        waitForLatch(acceptLatchAdapter.getReceivedClose(), ADAPTER_LATCH_TIMEOUT,
            TimeUnit.SECONDS, "Server did not receive CLOSE frame as expected");
        waitForLatch(acceptLatchAdapter.getSentClose(), ADAPTER_LATCH_TIMEOUT,
            TimeUnit.SECONDS, "Server did not send CLOSE frame as expected");
        waitForLatch(connectLatchAdapter.getReceivedClose(),
            ADAPTER_LATCH_TIMEOUT, TimeUnit.SECONDS,
            "Client did not receive CLOSE frame as expected");
        waitForLatch(connectLatchAdapter.getSentClose(), ADAPTER_LATCH_TIMEOUT,
            TimeUnit.SECONDS, "Client did not send CLOSE frame as expected");
    }

    @Test
    public void shouldConnectAndCloseTimeoutByServer()
        throws Exception {

        CloseLatchAdapter connectLatchAdapter = new NoCloseLatchAdapter();
        CloseLatchAdapter acceptLatchAdapter = new CloseLatchAdapter();

        // Configure a 500ms timeout for CLOSE frames
        connectAndClose(false, null, connectLatchAdapter, acceptLatchAdapter,
            null, "500ms");

        waitForLatch(acceptLatchAdapter.getReceivedClose(), ADAPTER_LATCH_UNEXPECTED_TIMEOUT,
            TimeUnit.SECONDS, 1, "Server received CLOSE frame unexpectedly");
        waitForLatch(acceptLatchAdapter.getSentClose(), ADAPTER_LATCH_TIMEOUT,
            TimeUnit.SECONDS, "Server did not send CLOSE frame as expected");
        waitForLatch(connectLatchAdapter.getReceivedClose(),
            ADAPTER_LATCH_UNEXPECTED_TIMEOUT, TimeUnit.SECONDS, 1,
            "Client received CLOSE frame unexpectedly");
        waitForLatch(connectLatchAdapter.getSentClose(), ADAPTER_LATCH_UNEXPECTED_TIMEOUT,
            TimeUnit.SECONDS, 1, "Client sent CLOSE frame unexpectedly");
    }

    private static class CloseLatchAdapter
        extends WsFilterAdapter {

        private final CountDownLatch sentClose;
        private final CountDownLatch receivedClose;

        public CloseLatchAdapter() {
            super();
            sentClose = new CountDownLatch(1);
            receivedClose = new CountDownLatch(1);
        }

        @Override
        protected void wsCloseReceived(NextFilter nextFilter,
                                       IoSession session,
                                       WsCloseMessage message)
            throws Exception {

            receivedClose.countDown();
            nextFilter.messageReceived(session, message);
        }

        @Override
        protected Object doFilterWriteWsClose(NextFilter nextFilter,
                                              IoSession session,
                                              WriteRequest request,
                                              WsCloseMessage message)
            throws Exception {

            // Wait for the request to complete before counting down
            // the latch.  This could be the cause of KG-7046.
            WriteFuture writeFuture = request.getFuture();
            writeFuture.addListener(new IoFutureListener<WriteFuture>() {
                @Override
                public void operationComplete(WriteFuture future) {
                    if (future.isWritten()) {
                        sentClose.countDown();
                    }
                }
            });

            return message;
        }

        public CountDownLatch getSentClose() {
            return sentClose;
        }

        public CountDownLatch getReceivedClose() {
            return receivedClose;
        }
    }

    private static class NoCloseLatchAdapter
        extends CloseLatchAdapter {

        public NoCloseLatchAdapter() {
            super();
        }

        @Override
        public void onPostAdd(IoFilterChain parent,
                              String name,
                              NextFilter nextFilter)
            throws Exception {

            // Find the WsCloseFilter in the filter chain, and remove it
            parent.remove(WsAcceptor.CLOSE_FILTER);
        }

        @Override
        protected void wsCloseReceived(NextFilter nextFilter,
                                       IoSession session,
                                       WsCloseMessage message)
            throws Exception {

            nextFilter.messageReceived(session, message);
        }

        @Override
        protected Object doFilterWriteWsClose(NextFilter nextFilter,
                                              IoSession session,
                                              WriteRequest request,
                                              WsCloseMessage message)
            throws Exception {

            return message;
        }
    }
}
