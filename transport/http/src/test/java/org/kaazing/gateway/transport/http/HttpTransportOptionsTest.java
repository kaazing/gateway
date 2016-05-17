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

import static org.junit.Assert.fail;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.PropertyConfigurator;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.BridgeSession;
import org.kaazing.gateway.transport.TransportFactory;
import org.kaazing.gateway.transport.nio.internal.NioSocketAcceptor;
import org.kaazing.gateway.transport.nio.internal.NioSocketConnector;
import org.kaazing.gateway.transport.test.TransportTestConnectSessionInitializer;
import org.kaazing.gateway.transport.test.TransportTestIoHandlerAdapter;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.future.UnbindFuture;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.test.util.ResolutionTestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpTransportOptionsTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private static String networkInterface = ResolutionTestUtils.getLoopbackInterface();

    private static  Logger logger;

    private static final boolean DEBUG = false;

    @BeforeClass
    public static void init()
            throws Exception {

        if (DEBUG) {
            PropertyConfigurator.configure("src/test/resources/log4j-trace.properties");
        }

        logger = LoggerFactory.getLogger(HttpTransportOptionsTest.class);
    }

    ResourceAddressFactory resourceAddressFactory = ResourceAddressFactory.newResourceAddressFactory();
    BridgeServiceFactory bridgeServiceFactory;
    SchedulerProvider schedulerProvider = new SchedulerProvider();

    
    NioSocketAcceptor tcpAcceptor;
    HttpAcceptor httpAcceptor;

    NioSocketConnector tcpConnector;
    HttpConnector httpConnector;

    AtomicInteger failures = new AtomicInteger(0);

    @Before
    public void before() {
        schedulerProvider = new SchedulerProvider();

        Map<String, ?> config = Collections.emptyMap();
        TransportFactory transportFactory = TransportFactory.newTransportFactory(config);
        bridgeServiceFactory = new BridgeServiceFactory(transportFactory);

        tcpAcceptor = (NioSocketAcceptor)transportFactory.getTransport("tcp").getAcceptor();
        tcpAcceptor.setSchedulerProvider(schedulerProvider);
        tcpAcceptor.setResourceAddressFactory(resourceAddressFactory);
        tcpAcceptor.setBridgeServiceFactory(bridgeServiceFactory);

        httpAcceptor = (HttpAcceptor)transportFactory.getTransport("http").getAcceptor();
        httpAcceptor.setBridgeServiceFactory(bridgeServiceFactory);
        httpAcceptor.setResourceAddressFactory(resourceAddressFactory);
        httpAcceptor.setSchedulerProvider(schedulerProvider);

        tcpConnector = (NioSocketConnector)transportFactory.getTransport("tcp").getConnector();
        tcpConnector.setResourceAddressFactory(resourceAddressFactory);
        tcpConnector.setBridgeServiceFactory(bridgeServiceFactory);
        tcpConnector.setTcpAcceptor(tcpAcceptor);

        httpConnector = (HttpConnector)transportFactory.getTransport("http").getConnector();
        httpConnector.setBridgeServiceFactory(bridgeServiceFactory);
        httpConnector.setResourceAddressFactory(resourceAddressFactory);

        failures = new AtomicInteger(0);
    }

    @After
    public void after() {
        httpConnector.dispose();
        tcpConnector.dispose();

        httpAcceptor.dispose();
        tcpAcceptor.dispose();
    }

    private TransportTestConnectSessionInitializer STANDARD_REQUEST_INITIALIZER = new TransportTestConnectSessionInitializer(1) {
        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            DefaultHttpSession s = ((DefaultHttpSession) session);
            IoBufferAllocatorEx<?> allocator = s.getBufferAllocator();
            s.setMethod(HttpMethod.POST);
            s.setRequestURI(URI.create("/path"));
            s.setVersion(HttpVersion.HTTP_1_1);
            s.setWriteHeader("Host", "localhost:8000");
            s.setWriteHeader("Content-Length", "11");
            s.write(allocator.wrap(ByteBuffer.wrap("Hello Kitty".getBytes()))).addListener(new IoFutureListener<WriteFuture>() {
                @Override
                public void operationComplete(WriteFuture future) {
                    checkpoint(); // request sent
                }
            });
        }

        @Override
        public String getCheckpointFailureMessage() {
            return "Failed to write a http request message from the connect session's initializer.";
        }
    };



    @Test
    public void simpleRequestResponseTest() {


        final TransportTestIoHandlerAdapter connectHandler = new TransportTestIoHandlerAdapter(1) {

            @Override
            protected void doSessionCreated(IoSessionEx session) throws Exception {
                if ( logger.isDebugEnabled() ) { logger.debug("Client Http Session created"); }
            }
            @Override
            protected void doSessionOpened(IoSessionEx session) throws Exception {
                if ( logger.isDebugEnabled() ) { logger.debug("Client Http Session opened"); }
            }
            @Override
            protected void doMessageReceived(IoSessionEx session, Object message) throws Exception {
                if ( logger.isDebugEnabled() ) { logger.debug("Client Http Session received OK"); }
                checkpoint();
            }
            @Override
            public String getCheckpointFailureMessage() {
                return "Http connect handler failed to receive a response from the server.";
            }
        };


        final TransportTestIoHandlerAdapter acceptHandler = new TransportTestIoHandlerAdapter(1) {
            @Override
            protected void doMessageReceived(final IoSessionEx session, Object message) throws Exception {
                if (logger.isDebugEnabled()) {
                    logger.debug("Server Http Session message received");
                }

                DefaultHttpSession httpSession = (DefaultHttpSession) session;
                IoBufferAllocatorEx<?> allocator = httpSession.getBufferAllocator();
                httpSession.setStatus(HttpStatus.SUCCESS_OK);
                httpSession.setVersion(HttpVersion.HTTP_1_1);
                httpSession.setWriteHeader("Server", "Test");
                httpSession.write(allocator.wrap(ByteBuffer.wrap("Purrr".getBytes()))).addListener(new IoFutureListener<IoFuture>() {
                    @Override
                    public void operationComplete(IoFuture future) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Server Http Session message sent back");
                        }
                        checkpoint();
                        session.close(false);
                    }
                });
            }

            @Override
            public String getCheckpointFailureMessage() {
                return "Failed to write a response and then close the Http session on the accept handler.";
            }
        };



        Map<String, Object> bindOptions = new HashMap<>();
        final Map<String, Object> connectOptions = Collections.emptyMap();


        httpConnectorToAcceptor("http://localhost:8000/path",
                connectHandler, acceptHandler, STANDARD_REQUEST_INITIALIZER,
                bindOptions, connectOptions);
    }

    @Test
    public void shouldConstructCorrectLocalAndRemoteAddressesForHttpAcceptAndConnectSessions() {
        helperConstructLocalRemoteAddressesForAcceptAndConnectSessions(null, null);
    }

    @Test
    public void shouldConstructCorrectLocalAndRemoteAddressesForHttpAcceptAndConnectSessionsOverrideTcpWithBrackets() {
        Map<String, Object> acceptOptionsParam = new HashMap<>();
        acceptOptionsParam.put("tcp.bind", "[@" + networkInterface + "]:8080");
        Map<String, Object> connectOptionsParam = new HashMap<>();
        connectOptionsParam.put("http.transport", "tcp://[@" + networkInterface + "]:8080");
        helperConstructLocalRemoteAddressesForAcceptAndConnectSessions(acceptOptionsParam, connectOptionsParam);
    }

    @Test
    public void shouldConstructCorrectLocalAndRemoteAddressesForHttpAcceptAndConnectSessionsOverrideTcpNoBrackets() {
        Map<String, Object> acceptOptionsParam = new HashMap<>();
        acceptOptionsParam.put("tcp.bind", "@" + networkInterface + ":8080");
        Map<String, Object> connectOptionsParam = new HashMap<>();
        connectOptionsParam.put("http.transport", "tcp://@" + networkInterface + ":8080");
        if (networkInterface.contains(" ")) {
            thrown.expect(IllegalArgumentException.class);
            thrown.expectMessage(String.format("Bind address \"%s\" should be in "
                    + "\"host/ipv4:port\", \"[ipv6]:port\", \"@network_interface:port\", \"[@network interface]:port\" or \"port\" "
                    + "format.", "@" + networkInterface + ":8080"));
        }
        helperConstructLocalRemoteAddressesForAcceptAndConnectSessions( acceptOptionsParam, connectOptionsParam);
    }

    @Test
    public void shouldConstructCorrectLocalAndRemoteAddressesForHttpAcceptAndConnectSessionsOverrideUdpWithBrackets() {
        Map<String, Object> acceptOptionsParam = new HashMap<>();
        acceptOptionsParam.put("tcp.bind", "[@" + networkInterface + "]:8080");
        Map<String, Object> connectOptionsParam = new HashMap<>();
        connectOptionsParam.put("http.transport", "tcp://[@" + networkInterface + "]:8080");
        helperConstructLocalRemoteAddressesForAcceptAndConnectSessions(acceptOptionsParam, connectOptionsParam);
    }

    @Test @Ignore("This test's sole purpose is to validate that a filter (SUBJECT_SECURITY) is not on the filter chain despite the fact that the filter is *always* on the filter chain.../facepalm")
    public void shouldNotConstructSecurityFiltersOnServerSideHttpBridgeFilterChain() {

        final String uri = "http://localhost:8000/path";

        final TransportTestIoHandlerAdapter connectHandler = new TransportTestIoHandlerAdapter(0) {
            @Override
            protected void doSessionCreated(IoSessionEx session) throws Exception {
            }

            @Override
            public String getCheckpointFailureMessage() {
                return "Connect handler should not.";
            }
        };

        final TransportTestIoHandlerAdapter acceptHandler = new TransportTestIoHandlerAdapter(2) {
            @Override
            protected void doMessageReceived(IoSessionEx session, Object message) throws Exception {
                IoFilterChain chain = ((BridgeSession)session).getParent().getFilterChain();
                if(chain.contains(HttpAcceptFilter.SUBJECT_SECURITY.filterName())) {
                    failures.incrementAndGet();
                }
                checkpoint(); // server-side session does not have security
                session.close(false).addListener( new IoFutureListener<CloseFuture>() {
                    @Override
                    public void operationComplete(CloseFuture future) {
                        checkpoint(); // server-side session does not have security
                    }
                });
            }

            @Override
            public String getCheckpointFailureMessage() {
                return "Failed to close server session after test completed in accept handler.";
            }
        };

        Map<String, Object> bindOptions = new HashMap<>();
        final Map<String, Object> connectOptions = Collections.emptyMap();

        httpConnectorToAcceptor(uri,
                connectHandler, acceptHandler, STANDARD_REQUEST_INITIALIZER,
                bindOptions, connectOptions);

        if ( failures.get() > 0 ) {
            fail("Detected "+failures.get()+" security filter(s) on the pipeline when none are expected.");
        }
    }

    /**
     * Helper method for constructing this sc3enario with varying accept/connect options
     * @param acceptOptionsParam
     * @param connectOptionsParam
     */
    private void helperConstructLocalRemoteAddressesForAcceptAndConnectSessions(Map<String, Object> acceptOptionsParam,
                                                                                    Map<String, Object> connectOptionsParam) {
        final String connectURI = "http://localhost:8000/path";

        final TransportTestIoHandlerAdapter connectHandler = new TransportTestIoHandlerAdapter(1) {

            @Override
            protected void doSessionCreated(IoSessionEx session) throws Exception {
                if ( logger.isDebugEnabled() ) { logger.debug("Client Http Session created"); }
            }
            @Override
            protected void doSessionOpened(IoSessionEx session) throws Exception {
                if ( logger.isDebugEnabled() ) { logger.debug("Client Http Session opened"); }
            }
            @Override
            protected void doMessageReceived(IoSessionEx session, Object message) throws Exception {
                if ( logger.isDebugEnabled() ) { logger.debug("Client Http Session received OK"); }

                BridgeSession bridgeSession = (BridgeSession) session;
                URI uriConnectURI = URI.create(connectURI);
                assertEquals("remote address of connect session was not " + connectURI, uriConnectURI, BridgeSession.REMOTE_ADDRESS.get(bridgeSession).getResource());
                assertEquals("local  address of connect session was not " + connectURI, uriConnectURI, BridgeSession.LOCAL_ADDRESS.get(bridgeSession).getResource());
                assertEquals("ephemeral port of local address' transport != ephemeral port of parent session's local address",
                             BridgeSession.LOCAL_ADDRESS.get(bridgeSession).getTransport().getResource().getPort(),
                             BridgeSession.LOCAL_ADDRESS.get(bridgeSession.getParent()).getResource().getPort());
                checkpoint();
            }
            @Override
            public String getCheckpointFailureMessage() {
                return "Failed to construct connect session local/remote addresses correctly.";
            }
        };


        final TransportTestIoHandlerAdapter acceptHandler = new TransportTestIoHandlerAdapter(1) {
            @Override
            protected void doMessageReceived(final IoSessionEx session, Object message) throws Exception {
                if ( logger.isDebugEnabled() ) { logger.debug("Server Http Session message received"); }

                DefaultHttpSession httpSession = (DefaultHttpSession) session;
                IoBufferAllocatorEx<?> allocator = httpSession.getBufferAllocator();
                httpSession.setStatus(HttpStatus.SUCCESS_OK);
                httpSession.setVersion(HttpVersion.HTTP_1_1);
                httpSession.setWriteHeader("Server", "Test");
                httpSession.write(allocator.wrap(ByteBuffer.wrap("Purrr".getBytes()))).addListener(new IoFutureListener<IoFuture>() {
                    @Override
                    public void operationComplete(IoFuture future) {

                        BridgeSession bridgeSession = (BridgeSession) session;
                        URI uriConnectURI = URI.create(connectURI);
                        assertEquals("remote address of accept session was not " + connectURI, uriConnectURI,
                                BridgeSession.REMOTE_ADDRESS.get(bridgeSession).getResource());
                        assertEquals("local  address of accept session was not " + connectURI, uriConnectURI,
                                BridgeSession.LOCAL_ADDRESS.get(bridgeSession).getResource());
                        assertEquals("ephemeral port of remote address' transport != ephemeral port of parent session's remote address",
                                     BridgeSession.REMOTE_ADDRESS.get(bridgeSession).getTransport().getResource().getPort(),
                                     BridgeSession.REMOTE_ADDRESS.get(bridgeSession.getParent()).getResource().getPort());
                        checkpoint();
                    }
                });
            }

            @Override
            public String getCheckpointFailureMessage() {
                return "Failed to construct accept session local/remote addresses correctly.";
            }
        };



        Map<String, Object> bindOptions = new HashMap<>();
        if (acceptOptionsParam != null) {
            bindOptions = acceptOptionsParam;
        }
        Map<String, Object> connectOptions = new HashMap<>();
        if (connectOptionsParam != null) {
            connectOptions = connectOptionsParam;
        }


        httpConnectorToAcceptor(connectURI,
                                connectHandler, acceptHandler, STANDARD_REQUEST_INITIALIZER,
                                bindOptions, connectOptions);
    }

    private void httpConnectorToAcceptor(final String connectURI,
                                         TransportTestIoHandlerAdapter connectHandler,
                                         TransportTestIoHandlerAdapter acceptHandler,
                                         final TransportTestConnectSessionInitializer connectSessionInitializer,
                                         Map<String, Object> acceptOptions,
                                         Map<String, Object> connectOptions) {

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
                connectHandler, connectSessionInitializer);

        future.awaitUninterruptibly(TimeUnit.SECONDS.toMillis(3));


        UnbindFuture unbindFuture;
        try {
            if (!future.isConnected()) {
                fail("Failed to connect: " + future.getException());
            }
            connectSessionInitializer.await(3, TimeUnit.SECONDS);
            acceptHandler.await(3, TimeUnit.SECONDS);
            connectHandler.await(3, TimeUnit.SECONDS);

        } catch (InterruptedException e) {
            fail("Interrupted while waiting for handlers to complete: "+e.getMessage());
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
