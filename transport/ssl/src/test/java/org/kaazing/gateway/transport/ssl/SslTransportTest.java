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
package org.kaazing.gateway.transport.ssl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.PropertyConfigurator;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
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
import org.kaazing.gateway.transport.BridgeSession;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.TransportFactory;
import org.kaazing.gateway.transport.nio.internal.NioSocketAcceptor;
import org.kaazing.gateway.transport.nio.internal.NioSocketConnector;
import org.kaazing.gateway.transport.ssl.bridge.filter.SslCertificateSelectionFilter;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.future.UnbindFuture;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.test.util.MethodExecutionTrace;

public class SslTransportTest {
    private static final String SSL_WANT_CLIENT_AUTH = "ssl.wantClientAuth";
    private static final String SSL_NEED_CLIENT_AUTH = "ssl.needClientAuth";
    private static final String NEXT_PROTOCOL = "nextProtocol";

    @Rule
    public TestRule testExecutionTrace = new MethodExecutionTrace();

    private static int NETWORK_OPERATION_WAIT_SECS = 10; // was 3, increasing for loaded environments
    private static final long NETWORK_OPERATION_WAIT_MILLIS = TimeUnit.SECONDS.toMillis(NETWORK_OPERATION_WAIT_SECS);

    private final ResourceAddressFactory resourceAddressFactory = ResourceAddressFactory.newResourceAddressFactory();
    private final TransportFactory transportFactory = TransportFactory.newTransportFactory(Collections.emptyMap());
    private final BridgeServiceFactory bridgeServiceFactory = new BridgeServiceFactory(transportFactory);

    // For development of test
    private static final boolean DEBUG = false;

    private KeyStore keyStore;
    private String keyStoreFile;
    private String keyStoreFilePath;
    private String keyStorePassword;
    private String keyStorePasswordFile;
    private KeyStore trustStore;
    private String trustStoreFile;
    private String trustStoreFilePath;
    private SslAcceptor sslAcceptor;
    private SslConnector sslConnector;
    private NioSocketConnector tcpConnector;
    private NioSocketAcceptor tcpAcceptor;
    private SchedulerProvider schedulerProvider;

    @BeforeClass
    public static void init()
        throws Exception {

        if (DEBUG) {
            PropertyConfigurator.configure("src/test/resources/log4j-trace.properties");
        }
    }

    private String getPassword(String file) throws Exception {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        File f = new File(loader.getResource(file).toURI());
        keyStorePasswordFile = f.getPath();
        FileReader fr = new FileReader(f);
        BufferedReader br = new BufferedReader(fr);
        String line = br.readLine();
        br.close();

        return line;
    }

    private KeyStore getKeyStore(String file) throws Exception {
        KeyStore ks = KeyStore.getInstance("JCEKS");

        File f = new File("target/truststore/" + file);
        keyStoreFile = f.getPath();
        keyStoreFilePath = f.getAbsolutePath();

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(f);
            ks.load(fis, keyStorePassword.toCharArray());

        } finally {
            if (fis != null) {
                fis.close();
            }
        }

        return ks;
    }

    private KeyStore getTrustStore(String file) throws Exception {

        KeyStore ks = KeyStore.getInstance("JKS");

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        File f = new File(loader.getResource(file).toURI());
        trustStoreFile = f.getPath();
        trustStoreFilePath = f.getAbsolutePath();
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(f);
            ks.load(fis, null);

        } finally {
            if (fis != null) {
                fis.close();
            }
        }

        return ks;
    }


    private TestSecurityContext getSecurityContext() throws Exception {
        return new TestSecurityContext(keyStore, keyStoreFile, keyStoreFilePath, keyStorePassword.toCharArray(),
                                   keyStorePasswordFile, trustStore, trustStoreFile, trustStoreFilePath, null);
    }

    @Before
    public void setUp()
        throws Exception {

        keyStorePassword = getPassword("keystore.pw");
        keyStore = getKeyStore("keystore.db");
        trustStore = getTrustStore("truststore.db");

        SslCertificateSelectionFilter.setCurrentSession(null, true);
        SslCertificateSelectionFilter.setCurrentSession(null, false);
    }

    @After
    public void tearDown() throws Exception {
        // For reasons unknown, tcpAcceptor.unbind does not actually free up the bound port until dispose is called.
        // This causes the next test method to fail to bind.
        sslConnector.dispose();
        tcpConnector.dispose();
        sslAcceptor.dispose();
        tcpAcceptor.dispose();
        schedulerProvider.shutdownNow();
    }

    @Test
    public void shouldConnect()
        throws Exception {

        TestSecurityContext securityContext = getSecurityContext();

        sslAcceptor = (SslAcceptor)transportFactory.getTransport("ssl").getAcceptor();
        sslConnector = (SslConnector)transportFactory.getTransport("ssl").getConnector();

        tcpAcceptor = (NioSocketAcceptor)transportFactory.getTransport("tcp").getAcceptor();
        tcpConnector = (NioSocketConnector)transportFactory.getTransport("tcp").getConnector();
        tcpAcceptor.setResourceAddressFactory(resourceAddressFactory);
        schedulerProvider = new SchedulerProvider();

        sslAcceptor.setBridgeServiceFactory(bridgeServiceFactory);
        sslAcceptor.setResourceAddressFactory(resourceAddressFactory);
        sslAcceptor.setSecurityContext(securityContext);

        sslConnector.setBridgeServiceFactory(bridgeServiceFactory);
        sslConnector.setResourceAddressFactory(resourceAddressFactory);
        sslConnector.setSecurityContext(securityContext);

        tcpConnector.setResourceAddressFactory(resourceAddressFactory);
        tcpConnector.setTcpAcceptor(tcpAcceptor);
        tcpConnector.setBridgeServiceFactory(bridgeServiceFactory);
        tcpAcceptor.setSchedulerProvider(schedulerProvider);

        // hit server then back to client with expected messages.

        final CountDownLatch clientSessionCreated = new CountDownLatch(1);
        final CountDownLatch serverSessionCreated = new CountDownLatch(1);
        final CountDownLatch serverMessageReceived = new CountDownLatch(1);
        final CountDownLatch serverMessageSent = new CountDownLatch(1);


        final String uri = "ssl://localhost:4444";
        Map<String, Object> opts = new HashMap<>();
        opts.put(SSL_WANT_CLIENT_AUTH, Boolean.FALSE);
        opts.put(SSL_NEED_CLIENT_AUTH, Boolean.FALSE);
        opts.put(NEXT_PROTOCOL, "test-protocol");

        final ResourceAddress resourceAddress =
                resourceAddressFactory.newResourceAddress(uri, opts);



        final IoHandlerAdapter<IoSessionEx> acceptHandler = new IoHandlerAdapter<IoSessionEx>() {

            @Override
            protected void doSessionOpened(final IoSessionEx session)
                throws Exception {

                System.out.println("SSL server: doSessionOpened");

                BridgeSession bridgeSession = (BridgeSession) session;
                URI uriAsURI = URI.create(uri);
                Assert.assertEquals("remote address of accept session was not "+uri, uriAsURI, BridgeSession.REMOTE_ADDRESS.get(bridgeSession).getResource());
                Assert.assertEquals("local  address of accept session was not "+uri, uriAsURI, BridgeSession.LOCAL_ADDRESS.get(bridgeSession).getResource());
                Assert.assertEquals("ephemeral port of remote address' transport != ephemeral port of parent session's remote address",
                                    BridgeSession.REMOTE_ADDRESS.get(bridgeSession).getTransport().getResource().getPort(),
                                    BridgeSession.REMOTE_ADDRESS.get(bridgeSession.getParent()).getResource().getPort());
                serverSessionCreated.countDown();

                IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
                session.write(allocator.wrap(ByteBuffer.wrap("How's it going Sven?".getBytes())));
                serverMessageSent.countDown();
            }

            @Override
            protected void doMessageReceived(IoSessionEx session,
                                             Object message)
                throws Exception {

                System.out.println("SSL server: doMessageReceived: "+message);
                if (message instanceof IoBuffer) {
                    final IoBuffer buffer = (IoBuffer) message;
                    String incoming = new String(buffer.array(), buffer.arrayOffset(), buffer.remaining());
                    System.out.println("SSL server message contents: "+incoming);
                }
                session.close(false).addListener(new IoFutureListener<IoFuture>() {
                    @Override
                    public void operationComplete(IoFuture future) {
                        serverMessageReceived.countDown();
                    }
                });

            }
        };

        final IoHandlerAdapter<IoSessionEx> connectHandler = new IoHandlerAdapter<IoSessionEx>() {
            @Override
            protected void doSessionCreated(IoSessionEx session)
                throws Exception {

                System.out.println("SSL client: doSessionCreated");
            }

            @Override
            protected void doSessionOpened(IoSessionEx session)
                throws Exception {

                System.out.println("SSL client: doSessionOpened");
                BridgeSession bridgeSession = (BridgeSession) session;

                URI uriAsURI = URI.create(uri);
                Assert.assertEquals("remote address of connect session was not "+uri, uriAsURI, BridgeSession.REMOTE_ADDRESS.get(bridgeSession).getResource());
                Assert.assertEquals("local  address of connect session was not "+uri, uriAsURI, BridgeSession.LOCAL_ADDRESS.get(bridgeSession).getResource());
                Assert.assertEquals("ephemeral port of local address' transport != ephemeral port of parent session's local address",
                                    BridgeSession.LOCAL_ADDRESS.get(bridgeSession).getTransport().getResource().getPort(),
                                    BridgeSession.LOCAL_ADDRESS.get(bridgeSession.getParent()).getResource().getPort());

                clientSessionCreated.countDown();
            }

            @Override
            protected void doMessageReceived(final IoSessionEx session,
                                             Object message)
                throws Exception {

                System.out.println("SSL client: messageReceived " + message);
                if ( message instanceof IoBuffer ) {
                    session.write(message).addListener(IoFutureListener.CLOSE);
                }
            }
        };




        sslAcceptor.bind(resourceAddress, acceptHandler, null);

        ConnectFuture future = sslConnector.connect(resourceAddress,
                connectHandler, null);

        future.awaitUninterruptibly(NETWORK_OPERATION_WAIT_MILLIS);

        UnbindFuture unbindFuture;
        try {
            waitForLatch(clientSessionCreated, NETWORK_OPERATION_WAIT_SECS, TimeUnit.SECONDS, "Did not complete SSL handshake on client in time");
            waitForLatch(serverSessionCreated, NETWORK_OPERATION_WAIT_SECS, TimeUnit.SECONDS, "Did not complete SSL handshake on server in time");
            waitForLatch(serverMessageSent, NETWORK_OPERATION_WAIT_SECS, TimeUnit.SECONDS, "Did not send out a message from SSL client to server in time");
            waitForLatch(serverMessageReceived, NETWORK_OPERATION_WAIT_SECS, TimeUnit.SECONDS, "Did not receive a message on SSL server from client in time");

        } catch (InterruptedException e) {
            Assert.fail("Interrupted while waiting for latches.");

        } finally {
            unbindFuture = sslAcceptor.unbind(resourceAddress);
            unbindFuture.addListener(new IoFutureListener<UnbindFuture>() {
                @Override
                public void operationComplete(UnbindFuture future) {
                    schedulerProvider.shutdownNow();
                }
            });
        }

        unbindFuture.awaitUninterruptibly(NETWORK_OPERATION_WAIT_SECS, TimeUnit.SECONDS);
        if (!unbindFuture.isUnbound()) {
            throw new RuntimeException("Failed to unbind SSL acceptor");
        }

    }

    @Test
    public void shouldConnectWithClientAuth()
        throws Exception {

        TestSecurityContext securityContext = getSecurityContext();

        sslAcceptor = (SslAcceptor)transportFactory.getTransport("ssl").getAcceptor();
        sslConnector = (SslConnector)transportFactory.getTransport("ssl").getConnector();

        tcpAcceptor = (NioSocketAcceptor)transportFactory.getTransport("tcp").getAcceptor();
        tcpConnector = (NioSocketConnector)transportFactory.getTransport("tcp").getConnector();
        tcpAcceptor.setResourceAddressFactory(resourceAddressFactory);
        schedulerProvider = new SchedulerProvider();

        sslAcceptor.setBridgeServiceFactory(bridgeServiceFactory);
        sslAcceptor.setResourceAddressFactory(resourceAddressFactory);
        sslAcceptor.setSecurityContext(securityContext);

        sslConnector.setBridgeServiceFactory(bridgeServiceFactory);
        sslConnector.setResourceAddressFactory(resourceAddressFactory);
        sslConnector.setSecurityContext(securityContext);
        sslConnector.setResourceAddressFactory(resourceAddressFactory);

        tcpConnector.setResourceAddressFactory(resourceAddressFactory);
        tcpConnector.setTcpAcceptor(tcpAcceptor);
        tcpConnector.setBridgeServiceFactory(bridgeServiceFactory);
        tcpAcceptor.setSchedulerProvider(schedulerProvider);

        // hit server then back to client with expected messages.

        final CountDownLatch clientSessionCreated = new CountDownLatch(1);
        final CountDownLatch serverSessionCreated = new CountDownLatch(1);
        final CountDownLatch serverMessageReceived = new CountDownLatch(1);
        final CountDownLatch serverMessageSent = new CountDownLatch(1);

        final IoHandlerAdapter<IoSessionEx> acceptHandler = new IoHandlerAdapter<IoSessionEx>() {
            @Override
            protected void doSessionOpened(final IoSessionEx session)
                throws Exception {

                System.out.println("SSL server: doSessionOpened");
                serverSessionCreated.countDown();

                IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
                session.write(allocator.wrap(ByteBuffer.wrap("How's it going Sven?".getBytes())));
                serverMessageSent.countDown();
            }

            @Override
            protected void doMessageReceived(IoSessionEx session,
                                             Object message)
                throws Exception {

                System.out.println("SSL server: doMessageReceived: "+message);
                if (message instanceof IoBuffer) {
                    final IoBuffer buffer = (IoBuffer) message;
                    String incoming = new String(buffer.array(), buffer.arrayOffset(), buffer.remaining());
                    System.out.println("SSL server message contents: "+incoming);
                }
                session.close(false).addListener(new IoFutureListener<IoFuture>() {
                    @Override
                    public void operationComplete(IoFuture future) {
                        serverMessageReceived.countDown();
                    }
                });

            }
        };

        final IoHandlerAdapter<IoSessionEx> connectHandler = new IoHandlerAdapter<IoSessionEx>() {
            @Override
            protected void doSessionCreated(IoSessionEx session)
                throws Exception {

                System.out.println("SSL client: doSessionCreated");
            }

            @Override
            protected void doSessionOpened(IoSessionEx session)
                throws Exception {

                System.out.println("SSL client: doSessionOpened");
                clientSessionCreated.countDown();
            }

            @Override
            protected void doMessageReceived(final IoSessionEx session,
                                             Object message)
                throws Exception {

                System.out.println("SSL client: messageReceived " + message);
                if ( message instanceof IoBuffer ) {
                    session.write(message).addListener(IoFutureListener.CLOSE);
                }
            }
        };
//
//        sslAcceptor.init();
//        sslConnector.init();

        String serverURI = "ssl://localhost:4445";
        Map<String, Object> opts = new HashMap<>();
        opts.put(SSL_WANT_CLIENT_AUTH, Boolean.FALSE);
        opts.put(SSL_NEED_CLIENT_AUTH, Boolean.TRUE);
        opts.put(NEXT_PROTOCOL, "test-protocol");

        final ResourceAddress serverAddress =
            resourceAddressFactory.newResourceAddress(serverURI, opts);

        sslAcceptor.bind(serverAddress, acceptHandler, null);

        String clientURI = "ssl://localhost:4445";
        opts = new HashMap<>();
        opts.put(SSL_WANT_CLIENT_AUTH, Boolean.FALSE);
        opts.put(SSL_NEED_CLIENT_AUTH, Boolean.FALSE);

        final ResourceAddress clientAddress =
            resourceAddressFactory.newResourceAddress(clientURI, opts);

        ConnectFuture future = sslConnector.connect(clientAddress,
                connectHandler, null);

        future.awaitUninterruptibly(NETWORK_OPERATION_WAIT_MILLIS);

        UnbindFuture unbindFuture;
        try {
            waitForLatch(clientSessionCreated, NETWORK_OPERATION_WAIT_SECS, TimeUnit.SECONDS, "Did not complete SSL handshake on client in time");
            waitForLatch(serverSessionCreated, NETWORK_OPERATION_WAIT_SECS, TimeUnit.SECONDS, "Did not complete SSL handshake on server in time");
            waitForLatch(serverMessageSent, NETWORK_OPERATION_WAIT_SECS, TimeUnit.SECONDS, "Did not send out a message from SSL client to server in time");
            waitForLatch(serverMessageReceived, NETWORK_OPERATION_WAIT_SECS, TimeUnit.SECONDS, "Did not receive a message on SSL server from client in time");

        } catch (InterruptedException e) {
            Assert.fail("Interrupted while waiting for latches.");

        } finally {
            unbindFuture = sslAcceptor.unbind(serverAddress);
            unbindFuture.addListener(new IoFutureListener<UnbindFuture>() {
                @Override
                public void operationComplete(UnbindFuture future) {
                    schedulerProvider.shutdownNow();
                }
            });
        }

        unbindFuture.awaitUninterruptibly(5, TimeUnit.SECONDS);
        if (!unbindFuture.isUnbound()) {
            throw new RuntimeException("Failed to unbind SSL acceptor");
        }
    }

    private void waitForLatch(CountDownLatch l,
                              final int delay,
                              final TimeUnit unit,
                              final String failureMessage)
            throws InterruptedException {

        l.await(delay, unit);
        if (l.getCount() != 0) {
            Assert.fail(failureMessage);
        }
    }
}
