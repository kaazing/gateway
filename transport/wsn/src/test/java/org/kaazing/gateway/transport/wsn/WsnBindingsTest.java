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

import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.security.KeyStore;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.mina.core.service.IoHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.TransportFactory;
import org.kaazing.gateway.transport.http.HttpAcceptor;
import org.kaazing.gateway.transport.http.HttpConnector;
import org.kaazing.gateway.transport.nio.internal.NioSocketAcceptor;
import org.kaazing.gateway.transport.nio.internal.NioSocketConnector;
import org.kaazing.gateway.transport.ssl.SslAcceptor;
import org.kaazing.gateway.transport.ssl.bridge.filter.SslCertificateSelectionFilter;
import org.kaazing.gateway.transport.ws.WsAcceptor;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;
import org.kaazing.mina.core.future.UnbindFuture;

public class WsnBindingsTest {

    private SchedulerProvider schedulerProvider;

    private ResourceAddressFactory addressFactory;

    private NioSocketConnector tcpConnector;
    private HttpConnector httpConnector;
    private WsnConnector wsnConnector;

    private NioSocketAcceptor tcpAcceptor;
    private SslAcceptor sslAcceptor;
    private HttpAcceptor httpAcceptor;
    private WsnAcceptor wsnAcceptor;

    private KeyStore keyStore;
    private String keyStoreFile;
    private String keyStoreFilePath;
    private String keyStorePassword;
    private String keyStorePasswordFile;
    private KeyStore trustStore;
    private String trustStoreFile;
    private String trustStoreFilePath;

    private String getPassword(String file)
            throws Exception {

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

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        File f = new File(loader.getResource(file).toURI());
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


    @After
    public void tearDown() throws Exception {
        // For reasons unknown, tcpAcceptor.unbind does not actually free up the bound port until dispose is called.
        // This causes the next test method to fail to bind.
        tcpConnector.dispose();
        sslAcceptor.dispose();
        tcpAcceptor.dispose();
        schedulerProvider.shutdownNow();
    }

    @Rule
    public TestRule timeout = new DisableOnDebug(new Timeout(5, SECONDS));

    @Before
    public void init() throws Exception {
        keyStorePassword = getPassword("keystore.pw");
        keyStore = getKeyStore("keystore.db");
        trustStore = getTrustStore("truststore-JCEKS.db");

        SslCertificateSelectionFilter.setCurrentSession(null, true);
        SslCertificateSelectionFilter.setCurrentSession(null, false);

        TestSecurityContext securityContext = getSecurityContext();

        schedulerProvider = new SchedulerProvider();

        addressFactory = ResourceAddressFactory.newResourceAddressFactory();
        TransportFactory transportFactory = TransportFactory.newTransportFactory(Collections.EMPTY_MAP);
        BridgeServiceFactory serviceFactory = new BridgeServiceFactory(transportFactory);

        tcpAcceptor = (NioSocketAcceptor)transportFactory.getTransport("tcp").getAcceptor();
        tcpAcceptor.setResourceAddressFactory(addressFactory);
        tcpAcceptor.setBridgeServiceFactory(serviceFactory);
        tcpAcceptor.setSchedulerProvider(schedulerProvider);

        tcpConnector = (NioSocketConnector)transportFactory.getTransport("tcp").getConnector();
        tcpConnector.setResourceAddressFactory(addressFactory);
        tcpConnector.setBridgeServiceFactory(serviceFactory);

        sslAcceptor = (SslAcceptor)transportFactory.getTransport("ssl").getAcceptor();
        sslAcceptor.setSecurityContext(securityContext);
        sslAcceptor.setBridgeServiceFactory(serviceFactory);

        httpAcceptor = (HttpAcceptor)transportFactory.getTransport("http").getAcceptor();
        httpAcceptor.setBridgeServiceFactory(serviceFactory);
        httpAcceptor.setResourceAddressFactory(addressFactory);
        httpAcceptor.setSchedulerProvider(schedulerProvider);

        httpConnector = (HttpConnector)transportFactory.getTransport("http").getConnector();
        httpConnector.setBridgeServiceFactory(serviceFactory);
        httpConnector.setResourceAddressFactory(addressFactory);

        wsnAcceptor = (WsnAcceptor)transportFactory.getTransport("wsn").getAcceptor();
        wsnAcceptor.setBridgeServiceFactory(serviceFactory);
        wsnAcceptor.setResourceAddressFactory(addressFactory);
        wsnAcceptor.setSchedulerProvider(schedulerProvider);

        WsAcceptor wsAcceptor = (WsAcceptor) transportFactory.getTransport("ws").getAcceptor();
        wsAcceptor.setWsnAcceptor(wsnAcceptor);

        wsnConnector = (WsnConnector)transportFactory.getTransport("wsn").getConnector();
        wsnConnector.setBridgeServiceFactory(serviceFactory);
        wsnConnector.setResourceAddressFactory(addressFactory);

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
    }


    @Test
    public void shouldBindAndUnbindLeavingEmptyBindingsMaps() throws Exception {

        Map<String, Object> acceptOptions = new HashMap<>();

        final String connectURIString = "wsn://localhost:8004/echo";
        final ResourceAddress bindAddress =
                addressFactory.newResourceAddress(
                        connectURIString,
                        acceptOptions);

        final IoHandler ioHandler = new IoHandlerAdapter();

        for ( int i = 0; i < 10; i++) {
            wsnAcceptor.bind(bindAddress, ioHandler, null);
        }
        for (int j = 0; j < 10; j++) {
            UnbindFuture future = wsnAcceptor.unbind(bindAddress);
            org.junit.Assert.assertTrue("Unbind failed", future.await(10, TimeUnit.SECONDS));
        }
        org.junit.Assert.assertTrue(wsnAcceptor.emptyBindings());
        org.junit.Assert.assertTrue(httpAcceptor.emptyBindings());
        org.junit.Assert.assertTrue(tcpAcceptor.emptyBindings());
    }


    @Test
    public void shouldBindAndUnbindSecureAddressesLeavingEmptyBindingsMaps() throws Exception {

        Map<String, Object> acceptOptions = new HashMap<>();

        final String connectURIString = "wsn+ssl://localhost:8005/echo";
        final ResourceAddress bindAddress =
                addressFactory.newResourceAddress(
                        connectURIString,
                        acceptOptions);

        final IoHandler ioHandler = new IoHandlerAdapter();

        for ( int i = 0; i < 10; i++) {
            wsnAcceptor.bind(bindAddress, ioHandler, null);
        }
        for (int j = 0; j < 10; j++) {
            UnbindFuture future = wsnAcceptor.unbind(bindAddress);
            org.junit.Assert.assertTrue("Unbind failed", future.await(10, TimeUnit.SECONDS));
        }

        org.junit.Assert.assertTrue(wsnAcceptor.emptyBindings());
        org.junit.Assert.assertTrue(sslAcceptor.emptyBindings());
        org.junit.Assert.assertTrue(httpAcceptor.emptyBindings());
        org.junit.Assert.assertTrue(tcpAcceptor.emptyBindings());
    }
}
