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

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.rules.RuleChain.outerRule;

import java.io.FileInputStream;
import java.security.KeyStore;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.ScriptProperty;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.test.util.MethodExecutionTrace;

public class SecureBalancerIT {

    private static KeyStore keyStore;
    private static KeyStore trustStore;
    private static char[] password;

    private final K3poRule k3po = new K3poRule();
    private final TestRule timeout = new DisableOnDebug(new Timeout(10, SECONDS));
    private final MethodExecutionTrace trace = new MethodExecutionTrace();

    private final GatewayRule balancerGateway = new GatewayRule() {
        {
            GatewayConfiguration configuration = new GatewayConfigurationBuilder()
                    .service()
                        .type("balancer")
                        .accept("wss://localhost:8002/echo")
                        .crossOrigin()
                            .allowOrigin("*")
                        .done()
                    .done()
                    .service()
                        .type("echo")
                        .accept("wss://localhost:8003/echo")
                        .balance("wss://localhost:8002/echo")
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

    private final GatewayRule proxyGateway = new GatewayRule() {
        {
            GatewayConfiguration configuration = new GatewayConfigurationBuilder()
                    .service()
                        .type("proxy")
                        .accept("tcp://localhost:8001/")
                        .connect("ssl://localhost:8002")
                        .crossOrigin()
                            .allowOrigin("*")
                        .done()
                    .done()
                    .security()
                        .trustStore(trustStore)
                    .done()
            .done();

            init(configuration);
        }
    };


    @Rule
    public TestRule chain = outerRule(trace).around(balancerGateway).around(proxyGateway).around(k3po).around(timeout);

    @BeforeClass
    public static void initClass() throws Exception {
        password = "ab987c".toCharArray();

        /// Initialize gateway keystore
        keyStore = KeyStore.getInstance("JCEKS");
        FileInputStream kis = new FileInputStream("target/truststore/keystore.db");
        keyStore.load(kis, password);
        kis.close();

        // Initialize gateway truststore 
        trustStore = KeyStore.getInstance("JKS");
        FileInputStream tis = new FileInputStream("target/truststore/truststore.db");
        trustStore.load(tis, null);
        tis.close();
    }

    @Test
    @Specification("wsx.balancer.request")
    @ScriptProperty({"hostHeader 'localhost:8002'", "redirectAddress 'https://localhost:8003/echo'"})
    public void getSecureRedirectOnWsxAsHttpPayload() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification("legacy.wsx.balancer.request")
    @ScriptProperty({"hostHeader 'localhost:8002'", "redirectAddress 'wss://localhost:8003/echo?.kl=Y'"})
    public void getLegacySecureRedirectOnWsxAsBalancerFrame() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification("wsn.balancer.request")
    @ScriptProperty({"hostHeader 'localhost:8002'", "redirectAddress 'https://localhost:8003/echo'"})
    public void getSecureRedirectOnWsn() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification("legacy.wsn.balancer.request")
    @ScriptProperty({"hostHeader 'localhost:8002'", "redirectAddress 'wss://localhost:8003/echo?.kl=Y'"})
    public void getLegacySecureRedirectOnWsn() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification("wse.balancer.request")
    @ScriptProperty({"hostHeader 'localhost:8002'", "redirectAddress 'https://localhost:8003/echo/;e/ctm'"})
    public void getSecureRedirectOnWse() throws Exception {
        k3po.finish();
    }
}
