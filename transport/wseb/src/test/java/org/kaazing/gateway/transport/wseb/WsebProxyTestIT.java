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
package org.kaazing.gateway.transport.wseb;

import static org.kaazing.test.util.ITUtil.createRuleChain;

import java.io.FileInputStream;
import java.security.KeyStore;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.Gateway;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

public class WsebProxyTestIT {

    private K3poRule robot = new K3poRule();
    KeyStore keyStore = null;
    char[] password = "ab987c".toCharArray();

    private GatewayRule gateway = new GatewayRule() {
        {
            try {
                keyStore = KeyStore.getInstance("JCEKS");
                FileInputStream in = new FileInputStream("target/truststore/keystore.db");
                keyStore.load(in, password);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                        .property(Gateway.GATEWAY_CONFIG_DIRECTORY_PROPERTY,
                                  "src/test/resources/gateway/conf")
                        .service()
                            .accept("wse://localhost:8001/echo")
                             .accept("wse+ssl://localhost:9002/echo")
                            .accept("wse+ssl://localhost:9001/echo")

                            .type("echo")
                            .crossOrigin()
                                .allowOrigin("*")
                            .done()
                        .done()
                        .service()
                            .accept("wse+ssl://localhost:9003/echo")
                            .type("echo")
                            .crossOrigin()
                                .allowOrigin("*")
                            .done()
                            .acceptOption("ssl.encryption", "disabled")
                        .done()
                    .security()
                        .keyStore(keyStore)
                        .keyStorePassword(password)
                        .done()
                    .done();
            // @formatter:on
            init(configuration);
        }
    };

    @Rule
    public TestRule chain = createRuleChain(gateway, robot);


    @Specification("VerifyProxyModeFallbackFromInsecureToSecureInWseb")
    @Test
    @Ignore("KG-11239")
    public void VerifyProxyModeFallbackFromInsecureToSecureInWseb() throws Exception {
        robot.finish();
    }

    @Specification("BluecoatHeaderDetectionAndFallbackToProxyMode")
    @Test
    @Ignore("KG-11239")
    public void BluecoatHeaderDetectionAndFallbackToProxyMode() throws Exception {
        robot.finish();
    }

    @Specification("wsebLongPolling")
    @Test
    public void wsebLongPolling() throws Exception {
        robot.finish();
    }

    @Specification("wsebLongPollingWithSequenceNumber")
    @Test
    public void wsebLongPollingWithSequenceNumber() throws Exception {
        robot.finish();
    }

    @Specification("wsebSecureLongPolling")
    @Test
    public void wsebSecureLongPolling() throws Exception {
        robot.finish();
    }

    @Specification("wsebLongPollingByHeader")
    @Test
    public void wsebLongPollingTriggeredByHeader() throws Exception {
        robot.finish();
    }

}
