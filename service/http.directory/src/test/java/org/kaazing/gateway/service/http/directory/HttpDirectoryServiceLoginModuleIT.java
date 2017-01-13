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
package org.kaazing.gateway.service.http.directory;

import static org.kaazing.test.util.ITUtil.createRuleChain;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.Gateway;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.gateway.util.feature.EarlyAccessFeatures;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

public class HttpDirectoryServiceLoginModuleIT {

    private static String BASIC_AUTH_DIRECTORY_SERVICE_ACCEPT = "http://localhost:8008/auth";

    private final K3poRule robot = new K3poRule();

    private final GatewayRule gateway = new GatewayRule() {
        {

            KeyStore keyStore = null;
            KeyStore trustStore = null;
            try {
                FileInputStream fileInStr = new FileInputStream(System.getProperty("user.dir")
                        + "/target/truststore/keystore.db");
                keyStore = KeyStore.getInstance("JCEKS");
                keyStore.load(fileInStr, "ab987c".toCharArray());

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
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                        .webRootDirectory(new File("src/test/webapp"))
                        .property(Gateway.GATEWAY_CONFIG_DIRECTORY_PROPERTY, "src/test/resources/gateway/conf")
                        .property(EarlyAccessFeatures.LOGIN_MODULE_EXPIRING_STATE.getPropertyName(), "true")
                        .service()
                            .accept(BASIC_AUTH_DIRECTORY_SERVICE_ACCEPT)
                            .type("directory")
                            .property("directory", "/public")
                            .property("welcome-file", "index.html")
                            .realmName("basic")
                            .authorization()
                                .requireRole("AUTHORIZED")
                            .done()
                        .done()
                        .security()
                            .keyStore(keyStore)
                            .trustStore(trustStore)
                            .realm()
                                .name("basic")
                                .description("Basic Authentication")
                                .httpChallengeScheme("Basic")
                                .authorizationMode("challenge")
                                .loginModule()
                                    .type("class:org.kaazing.gateway.service.http.directory.ConfirmLoginOptionsExistModule")
                                    .success("required")
                                .done()
                             .done()
                        .done()
                    .done();
            // @formatter:on
            init(configuration);
        }
    };

    @Rule
    public TestRule chain = createRuleChain(gateway, robot);

    @Specification("auth/basic.authorized.access.with.valid.credentials")
    @Test
    public void testBasicAuthorizedWithValidCredentials() throws Exception {
        robot.finish();
    }
}
