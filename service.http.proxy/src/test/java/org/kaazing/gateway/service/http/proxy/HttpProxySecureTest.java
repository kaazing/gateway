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

package org.kaazing.gateway.service.http.proxy;

import org.apache.log4j.BasicConfigurator;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.kaazing.gateway.server.test.Gateway;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;

import javax.net.ServerSocketFactory;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.security.KeyStore;

import static java.nio.charset.StandardCharsets.UTF_8;
import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;

public class HttpProxySecureTest {

    private KeyStore keyStore;
    private char[] password;
    private KeyStore trustStore;

    private SSLSocketFactory clientSocketFactory;

    @BeforeClass
    public static void initClass() throws Exception {
        BasicConfigurator.configure();
    }

    @Before
    public void init() throws Exception {
        // Initialize KeyStore of gateway
        password = "ab987c".toCharArray();
        keyStore = KeyStore.getInstance("JCEKS");
        FileInputStream kis = new FileInputStream("target/truststore/keystore.db");
        keyStore.load(kis, password);
        kis.close();

        // Initialize TrustStore of gateway
        trustStore = KeyStore.getInstance("JKS");
        FileInputStream tis = new FileInputStream("target/truststore/truststore.db");
        trustStore.load(tis, null);
        tis.close();

        // Configure client socket factory to trust the gateway's certificate
        SSLContext sslContext = SSLContext.getInstance("TLS");
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);
        sslContext.init(null, tmf.getTrustManagers(), null);
        clientSocketFactory = sslContext.getSocketFactory();
    }


    // client <---- ssl/http ---> gateway <---- ssl/http -----> origin server
    @Test(timeout = 5000)
    public void proxyWithTLS() throws Exception {
        Gateway gateway = new Gateway();
        SslOriginServer originServer = new SslOriginServer(keyStore, password);

        try {
            // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                            .service()
                                .accept(URI.create("https://localhost:8110"))
                                .connect(URI.create("https://localhost:8080"))
                                .type("http.proxy")
                            .done()
                            .security()
                                .trustStore(trustStore)
                                .keyStore(keyStore)
                                .keyStorePassword(password)
                            .done()
                        .done();
            // @formatter:on
            gateway.start(configuration);
            originServer.start();

            HttpsURLConnection con  = (HttpsURLConnection) new URL("https://localhost:8110/index.html").openConnection();
            con.setSSLSocketFactory(clientSocketFactory);
            try(BufferedReader r = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                String line = r.readLine();
                assertEquals("<html>Hellooo</html>", line);
                assertNull(null, r.readLine());
            }
        } finally {
            gateway.stop();
            originServer.stop();
        }
    }
    

    // HTTPS origin server
    static class SslOriginServer implements Runnable {
        private final KeyStore keyStore;
        private final char[] password;

        enum State {
            START, SLASH_R, SLASH_RN, SLASH_RNR, END
        }
        
        private volatile boolean stopped;
        private SSLServerSocket socket;
        
        SslOriginServer(KeyStore keyStore, char[] password) {
            this.keyStore = keyStore;
            this.password = password;
        }
        
        void start() throws Exception {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, password);
            sslContext.init(kmf.getKeyManagers(), null, null);
            ServerSocketFactory serverSocketFactory = sslContext.getServerSocketFactory();

            socket = (SSLServerSocket) serverSocketFactory.createServerSocket(8080);
            new Thread(this, "SSL Origin Server").start();
        }
        
        @Override
        public void run() {
            while (!stopped) {
                try(SSLSocket acceptSocket = (SSLSocket) socket.accept();
                    InputStream in = acceptSocket.getInputStream();
                    OutputStream out = acceptSocket.getOutputStream()) {
                    
                    State state = State.START;
                    while (state != State.END) {
                        int i = in.read();
                        switch (state) {
                            case START:
                                state = (i == '\r') ? State.SLASH_R : State.START;
                                break;
                            case SLASH_R:
                                state = (i == '\n') ? State.SLASH_RN : State.START;
                                break;
                            case SLASH_RN:
                                state = (i == '\r') ? State.SLASH_RNR : State.START;
                                break;
                            case SLASH_RNR:
                                state = (i == '\n') ? State.END : State.START;
                                break;
                        }
                    }

                    String res = 
                            "HTTP/1.1 200 OK\r\n" +
                            "Server: Apache-Coyote/1.1\r\n" +
                            "Content-Type: text/html;charset=UTF-8\r\n" +
                            "Transfer-Encoding: chunked\r\n" +
                            "Date: Tue, 10 Feb 2015 02:17:15 GMT\r\n" +
                            "Connection: close\r\n" +
                            "\r\n" +
                            "14\r\n" +
                            "<html>Hellooo</html>\r\n" +
                            "0\r\n" +
                            "\r\n";
                    
                    out.write(res.getBytes(UTF_8));
                } catch (IOException ioe) {
                    // no-op
                }
            }
        }
        
        void stop() throws IOException {
            stopped = true;
            socket.close();
        }
        
    }

}
