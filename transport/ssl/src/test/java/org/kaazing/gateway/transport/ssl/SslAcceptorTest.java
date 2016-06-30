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
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.PropertyConfigurator;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.session.IoSession;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.TransportFactory;
import org.kaazing.gateway.transport.nio.internal.NioSocketAcceptor;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;
import org.kaazing.mina.core.future.UnbindFuture;

public class SslAcceptorTest {
    private static final String SSL_WANT_CLIENT_AUTH = "ssl.wantClientAuth";
    private static final String SSL_NEED_CLIENT_AUTH = "ssl.needClientAuth";
    private static final String NEXT_PROTOCOL = "nextProtocol";

    private static final boolean DEBUG = false;

    @BeforeClass
    public static void init()
            throws Exception {

        if (DEBUG) {
            PropertyConfigurator.configure("src/test/resources/log4j-trace.properties");
        }
    }

    private ResourceAddressFactory resourceAddressFactory = ResourceAddressFactory.newResourceAddressFactory();
    TransportFactory transportFactory = TransportFactory.newTransportFactory(Collections.emptyMap());
    private BridgeServiceFactory bridgeServiceFactory = new BridgeServiceFactory(transportFactory);
    private SslAcceptor sslAcceptor;
    private NioSocketAcceptor tcpAcceptor;
    private SchedulerProvider schedulerProvider = new SchedulerProvider();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private KeyStore keyStore;
    private String keyStoreFile;
    private String keyStoreFilePath;
    private String keyStorePassword;
    private String keyStorePasswordFile;
    private KeyStore trustStore;
    private String trustStoreFile;
    private String trustStoreFilePath;

    @After
    public void tearDown() throws Exception {
        if (sslAcceptor != null) {
            sslAcceptor.dispose();
        }
        if (tcpAcceptor != null) {
            tcpAcceptor.dispose();
        }
        schedulerProvider.shutdownNow();
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
        KeyStore ks = KeyStore.getInstance("JCEKS");
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        File f = new File(loader.getResource(file).toURI());
        trustStoreFile = f.getPath();
        trustStoreFilePath = f.getAbsolutePath();
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(f);
            ks.load(fis, null);

        }
        finally {
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

    @Test
    public void shouldNotBindUsingWildcardCert()
        throws Exception {

        VirtualHostKeySelectorTest.assumeDNSNameAccessible("one.example.test", "two.example.test");
        thrown.expect(RuntimeException.class);
        thrown.expectMessage("does not have any certificate entries matching all possible hostnames bound");

        keyStorePassword = getPassword("keystore.pw");
        keyStore = getKeyStore("keystore.db");
        trustStore = getTrustStore("truststore-JCEKS.db");

        TestSecurityContext securityContext = getSecurityContext();

        sslAcceptor = (SslAcceptor)transportFactory.getTransport("ssl").getAcceptor();

        tcpAcceptor = (NioSocketAcceptor)transportFactory.getTransport("tcp").getAcceptor();

        schedulerProvider = new SchedulerProvider();

        sslAcceptor.setBridgeServiceFactory(bridgeServiceFactory);
        sslAcceptor.setResourceAddressFactory(resourceAddressFactory);
        sslAcceptor.setSecurityContext(securityContext);

        tcpAcceptor.setSchedulerProvider(schedulerProvider);

        final IoHandlerAdapter<IoSession> acceptHandler = new IoHandlerAdapter<IoSession>() {
            @Override
            protected void doSessionOpened(final IoSession session)
                throws Exception {
            }

            @Override
            protected void doMessageReceived(IoSession session,
                                             Object message)
                throws Exception {
            }
        };

        Map<String, Object> opts = buildSslOptionsMap();

        // These two DNS names resolve to the same IP address deliberately.
        // This test relies on this behavior.
        InetSocketAddress firstAddress = new InetSocketAddress("one.example.test", 443);
        InetSocketAddress secondAddress = new InetSocketAddress("two.example.test", 443);
        if (!firstAddress.getAddress().getHostAddress().equals(secondAddress.getAddress().getHostAddress())) {
            // The two DNS names do NOT resolve to the same IP address; just
            // bail out now.
            return;
        }

        String firstURI = "ssl://one.example.test:443";
        ResourceAddress firstAccept =
            resourceAddressFactory.newResourceAddress(firstURI, opts);
        sslAcceptor.bind(firstAccept, acceptHandler, null);

        // The opts are mutated on first bind, so build them again
        opts = buildSslOptionsMap();

        try {
            String secondURI = "ssl://two.example.test:443";
            ResourceAddress secondAccept =
                resourceAddressFactory.newResourceAddress(secondURI, opts);
            sslAcceptor.bind(secondAccept, acceptHandler, null);

        } finally {
            UnbindFuture unbindFuture = sslAcceptor.unbind(firstAccept);
            unbindFuture.addListener(new IoFutureListener<UnbindFuture>() {
                @Override
                public void operationComplete(UnbindFuture future) {
                    schedulerProvider.shutdownNow();
                }
            });
            unbindFuture.awaitUninterruptibly(5, TimeUnit.SECONDS);
            if (!unbindFuture.isUnbound()) {
                throw new Exception(String.format("Failed to unbind from %s within 5 seconds", firstAccept));
            }
        }
    }

    @Test
    public void shouldNotBindUsingUnknownHostName() throws Exception {

        VirtualHostKeySelectorTest.assumeDNSNameAccessible("one.example.test");
        thrown.expect(RuntimeException.class);
        thrown.expectMessage("Keystore does not have a certificate entry for otherhost");

        keyStorePassword = getPassword("keystore.pw");
        keyStore = getKeyStore("keystore.db");
        trustStore = getTrustStore("truststore-JCEKS.db");

        TestSecurityContext securityContext = getSecurityContext();

        sslAcceptor = (SslAcceptor)transportFactory.getTransport("ssl").getAcceptor();

        tcpAcceptor = (NioSocketAcceptor)transportFactory.getTransport("tcp").getAcceptor();

        schedulerProvider = new SchedulerProvider();

        sslAcceptor.setBridgeServiceFactory(bridgeServiceFactory);
        sslAcceptor.setResourceAddressFactory(resourceAddressFactory);
        sslAcceptor.setSecurityContext(securityContext);

        tcpAcceptor.setSchedulerProvider(schedulerProvider);

        final IoHandlerAdapter<IoSession> acceptHandler = new IoHandlerAdapter<IoSession>() {
            @Override
            protected void doSessionOpened(final IoSession session)
                throws Exception {
            }

            @Override
            protected void doMessageReceived(IoSession session,
                                             Object message)
                throws Exception {
            }
        };

        Map<String, Object> opts = buildSslOptionsMap();

        String firstURI = "ssl://one.example.test:443";
        ResourceAddress firstAccept =
            resourceAddressFactory.newResourceAddress(firstURI, opts);
        sslAcceptor.bind(firstAccept, acceptHandler, null);

        // The opts are mutated on first bind, so build them again
        opts = buildSslOptionsMap();

        try {
            opts.put(ResourceAddress.TRANSPORT_URI.name(), "tcp://127.0.0.1:443");
            String secondURI = "ssl://otherhost:443";
            ResourceAddress secondAccept =
                resourceAddressFactory.newResourceAddress(secondURI, opts);
            sslAcceptor.bind(secondAccept, acceptHandler, null);

        } finally {
            UnbindFuture unbindFuture = sslAcceptor.unbind(firstAccept);
            unbindFuture.addListener(new IoFutureListener<UnbindFuture>() {
                @Override
                public void operationComplete(UnbindFuture future) {
                    schedulerProvider.shutdownNow();
                }
            });
            unbindFuture.awaitUninterruptibly(5, TimeUnit.SECONDS);
            if (!unbindFuture.isUnbound()) {
                throw new Exception(String.format("Failed to unbind from %s within 5 seconds", firstAccept));
            }
        }
    }

    private Map<String, Object> buildSslOptionsMap() {
        Map<String, Object> opts = new HashMap<>();
        opts.put(SSL_WANT_CLIENT_AUTH, Boolean.FALSE);
        opts.put(SSL_NEED_CLIENT_AUTH, Boolean.FALSE);
        opts.put(NEXT_PROTOCOL, "test-protocol");

        // This is used to avoid the "Can't assign requested address" exception
        // that would result from trying to bind to an IP address that
        // is not hosted by the running machine.
        opts.put("tcp.bind", "127.0.0.1:8765");
        return opts;
    }

    @Test
    public void shouldBindUsingWildcardCert()
        throws Exception {

        VirtualHostKeySelectorTest.assumeDNSNameAccessible("one.kaazing.test", "two.kaazing.test");

        keyStorePassword = getPassword("keystore.pw");
        keyStore = getKeyStore("keystore.db");
        trustStore = getTrustStore("truststore-JCEKS.db");

        TestSecurityContext securityContext = getSecurityContext();

        sslAcceptor = (SslAcceptor)transportFactory.getTransport("ssl").getAcceptor();

        tcpAcceptor = (NioSocketAcceptor)transportFactory.getTransport("tcp").getAcceptor();

        schedulerProvider = new SchedulerProvider();

        sslAcceptor.setBridgeServiceFactory(bridgeServiceFactory);
        sslAcceptor.setResourceAddressFactory(resourceAddressFactory);
        sslAcceptor.setSecurityContext(securityContext);

        tcpAcceptor.setSchedulerProvider(schedulerProvider);

        final IoHandlerAdapter<IoSession> acceptHandler = new IoHandlerAdapter<IoSession>() {
            @Override
            protected void doSessionOpened(final IoSession session)
                throws Exception {
            }

            @Override
            protected void doMessageReceived(IoSession session,
                                             Object message)
                throws Exception {
            }
        };

        Map<String, Object> opts = new HashMap<>();
        opts.put(SSL_WANT_CLIENT_AUTH, Boolean.FALSE);
        opts.put(SSL_NEED_CLIENT_AUTH, Boolean.FALSE);
        opts.put(NEXT_PROTOCOL, "test-protocol");

        // This is used to avoid the "Can't assign requested address" exception
        // that would result from trying to bind to an IP address that
        // is not hosted by the running machine.
        opts.put("tcp.bind", "127.0.0.1:8776");

        // These two DNS names resolve to the same IP address deliberately.
        // This test relies on this behavior.
        InetSocketAddress firstAddress = new InetSocketAddress("one.kaazing.test", 443);
        InetSocketAddress secondAddress = new InetSocketAddress("two.kaazing.test", 443);
        if (!firstAddress.getAddress().getHostAddress().equals(secondAddress.getAddress().getHostAddress())) {
            // The two DNS names do NOT resolve to the same IP address; just
            // bail out now.
            return;
        }

        String firstURI = "ssl://one.kaazing.test:443";
        ResourceAddress firstAccept =
            resourceAddressFactory.newResourceAddress(firstURI, opts);
        sslAcceptor.bind(firstAccept, acceptHandler, null);

        opts = new HashMap<>();
        opts.put(SSL_WANT_CLIENT_AUTH, Boolean.FALSE);
        opts.put(SSL_NEED_CLIENT_AUTH, Boolean.FALSE);
        opts.put(NEXT_PROTOCOL, "test-protocol");

        // This is used to avoid the "Can't assign requested address" exception
        // that would result from trying to bind to an IP address that
        // is not hosted by the running machine.
        opts.put("tcp.bind", "127.0.0.1:8778");

        String secondURI = "ssl://two.kaazing.test:443";
        ResourceAddress secondAccept =
            resourceAddressFactory.newResourceAddress(secondURI, opts);
        sslAcceptor.bind(secondAccept, acceptHandler, null);

        UnbindFuture unbindFuture = sslAcceptor.unbind(firstAccept);
        unbindFuture.awaitUninterruptibly(5, TimeUnit.SECONDS);
        if (!unbindFuture.isUnbound()) {
            throw new Exception(String.format("Failed to unbind from %s within 5 seconds", firstAccept));
        }

        unbindFuture = sslAcceptor.unbind(secondAccept);
        unbindFuture.addListener(new IoFutureListener<UnbindFuture>() {
            @Override
            public void operationComplete(UnbindFuture future) {
                schedulerProvider.shutdownNow();
            }
        });
        unbindFuture.awaitUninterruptibly(5, TimeUnit.SECONDS);
        if (!unbindFuture.isUnbound()) {
            throw new Exception(String.format("Failed to unbind from %s within 5 seconds", secondAccept));
        }

    }
}
