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
package org.kaazing.gateway.service.http.balancer;

import static org.junit.Assert.assertEquals;
import static org.junit.rules.RuleChain.outerRule;

import java.io.FileInputStream;
import java.net.URL;
import java.security.KeyStore;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.test.util.MethodExecutionTrace;

public class BalanceToHttpServiceIT {

    private static final String SSL_BALANCER_URL ="https://localhost:8002/balancer";
    private static final String SSL_ECHO_URL = "wss://localhost:8002/echo";

    private static KeyStore keyStore;
    private static char[] password;
    private static SSLSocketFactory clientSocketFactory;

    private final K3poRule robot = new K3poRule();

    public TestRule timeout = new DisableOnDebug(new Timeout(10, TimeUnit.SECONDS));
    public MethodExecutionTrace testExecutionTrace = new MethodExecutionTrace();

    public GatewayRule gateway = new GatewayRule() {
        {
            GatewayConfiguration configuration = new GatewayConfigurationBuilder()
                    // balancer service to echo
                    .service()
                        .type("balancer")
                        .accept("http://localhost:8001/echo")
                        .crossOrigin()
                            .allowOrigin("*")
                        .done()
                    .done()
                    // echo service
                    .service()
                        .type("echo")
                        .accept("ws://localhost:8001/echo1")
                        .balance("http://localhost:8001/echo")
                        .crossOrigin()
                            .allowOrigin("*")
                        .done()
                    .done()
                    // ssl balancer service to echo
                    .service()
                        .type("balancer")
                        .accept(SSL_BALANCER_URL)
                        .crossOrigin()
                            .allowOrigin("*")
                        .done()
                    .done()
                    // ssl echo service
                    .service()
                        .type("echo")
                        .accept(SSL_ECHO_URL)
                        .balance(SSL_BALANCER_URL)
                        .crossOrigin()
                            .allowOrigin("*")
                        .done()
                    .done()
                    .security()
                        .keyStore(keyStore)
                        .keyStorePassword(password)
                    .done()
            .done();

            init(configuration);
        }
    };


    @Rule
    public TestRule chain = outerRule(testExecutionTrace).around(gateway).around(robot).around(timeout);

    @BeforeClass
    public static void initClass() throws Exception {
        password = "ab987c".toCharArray();

        /// Initialize gateway keystore
        keyStore = KeyStore.getInstance("JCEKS");
        FileInputStream kis = new FileInputStream("target/truststore/keystore.db");
        keyStore.load(kis, password);
        kis.close();

        // Initialize gateway truststore 
        KeyStore trustStore = KeyStore.getInstance("JKS");
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

    @Test
    @Specification("http.balancer.request")
    public void getsRedirect() throws Exception {
        robot.finish();
    }

    @Test
    public void getsSecureRedirect() throws Exception {
        URL url = new URL(SSL_BALANCER_URL);
        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
        con.setSSLSocketFactory(clientSocketFactory);
        con.setRequestMethod("GET");
        con.setRequestProperty("Host", "localhost:8002");
        con.setRequestProperty("Origin", "https://localhost:8002");
        con.setInstanceFollowRedirects(false);
        con.setConnectTimeout(10000);
        con.setReadTimeout(10000);
        con.connect();
        con.disconnect();
        assertEquals(HttpsURLConnection.HTTP_MOVED_TEMP, con.getResponseCode());
        assertEquals(SSL_ECHO_URL.replaceFirst("^ws", "http"), con.getHeaderField("location"));

    }

}
