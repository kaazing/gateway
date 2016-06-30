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

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.kaazing.test.util.ITUtil.createRuleChain;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.security.KeyStore;
import java.security.Security;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.Gateway;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;

public class Sslv3Test {

    private KeyStore keyStore;
    private char[] password;
    private KeyStore trustStore;

    private SSLSocketFactory clientSocketFactory;

    @Rule
    public TestRule chain = createRuleChain(10, SECONDS);

    @BeforeClass
    public static void initClass() throws Exception {
        // SSLv3 is part of disabled algorithms, reset for SSLv3 to work
        // But before JSSE initialized (so run the tests in separate jvm)
        Security.setProperty("jdk.tls.disabledAlgorithms", "true");
    }

    @Before
    public void init() throws Exception {
        // Initialize KeyStore of gateway
        password = "ab987c".toCharArray();
        keyStore = KeyStore.getInstance("JCEKS");
        FileInputStream kis = new FileInputStream("target/truststore/keystore.db");
        keyStore.load(kis, password);
        kis.close();

        // Configure client socket factory to trust the gateway's certificate
        trustStore = KeyStore.getInstance("JKS");
        FileInputStream tis = new FileInputStream("target/truststore/truststore.db");
        trustStore.load(tis, null);
        tis.close();
        SSLContext sslContext = SSLContext.getInstance("SSLv3");
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);
        sslContext.init(null, tmf.getTrustManagers(), null);
        clientSocketFactory = sslContext.getSocketFactory();
    }

    // Gateway doesn't enable SSLv3 by default, but configured explicitly to use *only* SSLv3
    // using ssl.protocols option. Client is also configured to use *only* SSLv3, so we expect
    // SSL handshake go through
    @Test
    public void acceptSucceedsWithSslv3() throws Exception {
        Gateway gateway = new Gateway();
        SSLSocket socket = null;
        try {
            // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                            .service()
                                .accept("ssl://localhost:8555")
                                .type("echo")
                                .acceptOption("ssl.protocols", "SSLv3")
                            .done()
                            .security()
                                .keyStore(keyStore)
                                .keyStorePassword(password)
                            .done()
                    .done();
            // @formatter:on
            gateway.start(configuration);

            socket = (SSLSocket) clientSocketFactory.createSocket("localhost", 8555);
            socket.setEnabledProtocols(new String[]{"SSLv3"});
            socket.startHandshake();
            BufferedWriter w = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            BufferedReader r = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String expected = "Hello World!";
            w.write(expected);
            w.newLine();
            w.flush();
            String got = r.readLine();
            assertEquals(expected, got);
            w.close();
            r.close();
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch(IOException ioe) {
                    //noop
                }
            }
            gateway.stop();
        }
    }

    // Gateway doesn't enable SSLv3 by default. But client is configured to use *only* SSLv3,
    // so we expect SSL handshake to *not* go through
    @Test
    public void acceptFailsWithoutSslv3() throws Exception {
        Gateway gateway = new Gateway();
        SSLSocket socket = null;
        try {
            // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                            .service()
                                .accept("ssl://localhost:8555")
                                .type("echo")
                            .done()
                            .security()
                                .keyStore(keyStore)
                                .keyStorePassword(password)
                            .done()
                    .done();
            // @formatter:on
            gateway.start(configuration);

            socket = (SSLSocket) clientSocketFactory.createSocket("localhost", 8555);
            socket.setEnabledProtocols(new String[]{"SSLv3"});
            socket.startHandshake();
            fail("Shouldn't establish SSL connection since SSLv3 is not enabled by default");
        } catch(SSLHandshakeException e) {
            // noop - expected exception
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch(IOException ioe) {
                    //noop
                }
            }
            gateway.stop();
        }
    }


    // 8567 (only SSLv3 by config) -> 8658 (only SSLv3 by config), so we expect SSL handshake to go through
    @Test
    public void connectSucceedsWithSslv3() throws Exception {
        Gateway gateway = new Gateway();
        Socket socket = null;
        try {
            // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                            .service()
                                .accept("tcp://localhost:8557")
                                .connect("ssl://localhost:8558")
                                .type("proxy")
                                .connectOption("ssl.protocols", "SSLv3")
                            .done()
                            .service()
                                .accept("ssl://localhost:8558")
                                .type("echo")
                                .acceptOption("ssl.protocols", "SSLv3")
                            .done()
                            .security()
                                .keyStore(keyStore)
                                .keyStorePassword(password)
                                .trustStore(trustStore)
                            .done()
                    .done();
            // @formatter:on
            gateway.start(configuration);

            socket = SocketFactory.getDefault().createSocket("localhost", 8557);
            BufferedWriter w = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            BufferedReader r = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String expected = "Hello World!";
            w.write(expected);
            w.newLine();
            w.flush();
            String got = r.readLine();
            assertEquals(expected, got);
            w.close();
            r.close();
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch(IOException ioe) {
                    //noop
                }
            }
            gateway.stop();
        }
    }

    // 9557 (no SSLv3 by default) -> 9588 (no SSLv3 by default), so we expect SSL handshake to go through
    // using some protocol other than SSLv3
    @Test
    public void connectSucceedsWithoutSslv3() throws Exception {
        Gateway gateway = new Gateway();
        Socket socket = null;
        try {
            // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                            .service()
                                .accept("tcp://localhost:9557")
                                .connect("ssl://localhost:9558")
                                .type("proxy")
                            .done()
                            .service()
                                .accept("ssl://localhost:9558")
                                .type("echo")
                            .done()
                            .security()
                                .keyStore(keyStore)
                                .keyStorePassword(password)
                                .trustStore(trustStore)
                            .done()
                        .done();
            // @formatter:on
            gateway.start(configuration);

            socket = SocketFactory.getDefault().createSocket("localhost", 9557);
            BufferedWriter w = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            BufferedReader r = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String expected = "Hello World!";
            w.write(expected);
            w.newLine();
            w.flush();
            String got = r.readLine();
            assertEquals(expected, got);
            w.close();
            r.close();
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch(IOException ioe) {
                    //noop
                }
            }
            gateway.stop();
        }
    }

    // 8567 (only SSLv3) -> 8658 (no SSLv3 by default), so we expect SSL handshake to *not* go through
    @Test
    public void connectFailsWithSslv3() throws Exception {
        Gateway gateway = new Gateway();
        Socket socket = null;
        try {
            // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                            .service()
                                .accept("tcp://localhost:8567")
                                .connect("ssl://localhost:8568")
                                .type("proxy")
                                .connectOption("ssl.protocols", "SSLv3")
                            .done()
                            .service()
                                .accept("ssl://localhost:8568")
                                .type("echo")
                            .done()
                            .security()
                                .keyStore(keyStore)
                                .keyStorePassword(password)
                                .trustStore(trustStore)
                            .done()
                    .done();
            // @formatter:on
            gateway.start(configuration);

            socket = SocketFactory.getDefault().createSocket("localhost", 8567);
            BufferedWriter w = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            BufferedReader r = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String expected = "Hello World!";
            w.write(expected);
            w.newLine();
            w.flush();
            String got = r.readLine();      // gets null because of EOF
            assertNull(got);
            w.close();
            r.close();
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch(IOException ioe) {
                    //noop
                }
            }
            gateway.stop();
        }
    }

    // 9567 (no SSLv3 by default) -> 9658 (only SSLv3 by config), so we expect SSL handshake to *not* go through
    @Test
    public void connectFailsWithoutSslv3() throws Exception {
        Gateway gateway = new Gateway();
        Socket socket = null;
        try {
            // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                            .service()
                                .accept("tcp://localhost:9567")
                                .connect("ssl://localhost:9568")
                                .type("proxy")
                            .done()
                            .service()
                                .accept("ssl://localhost:9568")
                                .type("echo")
                                .acceptOption("ssl.protocols", "SSLv3")
                            .done()
                            .security()
                                .keyStore(keyStore)
                                .keyStorePassword(password)
                                .trustStore(trustStore)
                            .done()
                        .done();
            // @formatter:on
            gateway.start(configuration);

            socket = SocketFactory.getDefault().createSocket("localhost", 9567);
            BufferedWriter w = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            BufferedReader r = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String expected = "Hello World!";
            w.write(expected);
            w.newLine();
            w.flush();
            String got = r.readLine();      // gets null because of EOF
            assertNull(got);
            w.close();
            r.close();
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch(IOException ioe) {
                    //noop
                }
            }
            gateway.stop();
        }
    }
}
