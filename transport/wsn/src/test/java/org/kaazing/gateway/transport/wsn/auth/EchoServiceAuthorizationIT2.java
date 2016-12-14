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
package org.kaazing.gateway.transport.wsn.auth;

import static org.kaazing.gateway.util.feature.EarlyAccessFeatures.HTTP_REALM_ACCEPT_OPTION;
import static org.kaazing.test.util.ITUtil.createRuleChain;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.Gateway;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

public class EchoServiceAuthorizationIT2 {

    private static final String ACCEPT = "ws://localhost:8008/echo";

    private final K3poRule robot = new K3poRule();

    private final GatewayRule gateway = new GatewayRule() {
        {
            KeyStore keyStore = null;
            try {
                FileInputStream fileInStr = new FileInputStream(System.getProperty("user.dir")
                        + "/target/truststore/keystore.db");
                keyStore = KeyStore.getInstance("JCEKS");
                keyStore.load(fileInStr, "ab987c".toCharArray());
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                        .property(HTTP_REALM_ACCEPT_OPTION.getPropertyName(), "true")
                        .webRootDirectory(new File("src/test/webapp"))
                        .property(Gateway.GATEWAY_CONFIG_DIRECTORY_PROPERTY,"src/test/resources/gateway/conf")
                        .service()
                            .accept(ACCEPT)
                            .type("echo")
                            .crossOrigin()
                                .allowOrigin("*")
                            .done()
                            .authorization()
                                .requireRole("AUTHORIZED")
                            .done()
                            .acceptOption("http.realm", "firstFactorLoginModule secondFactorLoginModule")
                        .done()
                        .security()
                            .keyStore(keyStore)
                            .realm()
                                .name("firstFactorLoginModule")
                                .description("username-password")
                                .httpChallengeScheme("Basic")
                                .authorizationMode("challenge")
                                .loginModule()
                                    .type("file")
                                    .success("required")
                                    .option("file", "jaas-config.xml")
                                .done()
                             .done()
                             .realm()
                                 .name("secondFactorLoginModule")
                                 .description("pin")
                                 .httpChallengeScheme("Basic")
                                 .authorizationMode("challenge")
                                     .loginModule()
                                     .type("file")
                                     .success("required")
                                     .option("file", "pin-config.xml")
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

    @Specification("basic.2factor.authorized.access.with.valid.credentials")
    @Test
    public void testAuthorizedWithValidCredentials() throws Exception {
        robot.finish();
    }

    @Specification("basic.2factor.authorized.access.with.invalid.credentials")
    @Test
    public void testAppBasicAuthorizedWithInvalidCredentials() throws Exception {
        robot.finish();
    }

}
