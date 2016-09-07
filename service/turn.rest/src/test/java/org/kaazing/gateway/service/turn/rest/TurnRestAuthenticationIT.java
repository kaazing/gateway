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
package org.kaazing.gateway.service.turn.rest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.gateway.util.feature.EarlyAccessFeatures;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

import javax.crypto.spec.SecretKeySpec;
import java.io.FileInputStream;
import java.security.KeyStore;

import static java.nio.charset.Charset.forName;
import static org.kaazing.test.util.ITUtil.createRuleChain;

public class TurnRestAuthenticationIT {
    private static final String ACCEPT_URL = "http://localhost:8001/";

    private final K3poRule robot = new K3poRule();

    private final GatewayRule gateway = new GatewayRule() {
        {
            KeyStore keyStore = null;
            char[] password = "ab987c".toCharArray();
            try {
                FileInputStream fileInStr =
                        new FileInputStream(System.getProperty("user.dir") + "/target/truststore/keystore.db");
                keyStore = KeyStore.getInstance("JCEKS");
                keyStore.load(fileInStr, "ab987c".toCharArray());
                // as per https://github.com/kaazing/gateway/pull/674#discussion_r75177451, we encrypt using the same
                // password as the keystore
                keyStore.setKeyEntry("turn.shared.secret",
                        new SecretKeySpec("turnAuthenticationSharedSecret".getBytes(forName("UTF-8")), "PBEWithMD5AndDES"),
                        password, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
            // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                        .property(EarlyAccessFeatures.TURN_REST_SERVICE.getPropertyName(), "true")
                        .service()
                            .accept(ACCEPT_URL)
                            .type("turn.rest")
                            .realmName("turn")
                                .authorization()
                                .requireRole("username")
                            .done()

                            .property("key.alias", "turn.shared.secret")
                            .property("key.algorithm", "HmacSHA1")
                            .property("credentials.generator", "class:" + DefaultCredentialsGenerator.class.getName())
                            .property("credentials.ttl", "43200")
                            .property("username.separator", ":")
                            .property("url", "turn:192.168.99.100:3478?transport=tcp")
                        .done()
                        .security()
                            .keyStore(keyStore)
                            .keyStorePassword(password)
                            .realm()
                                .name("turn")
                                .description("TURN REST Login Module Test")
                                .httpChallengeScheme("Basic")
                                .loginModule()
                                    .type("class:" + TestLoginModule.class.getName())
                                    .success("requisite")
                                    .option("roles", "username")
                                .done()
                            .done()
                        .done()


                        .service()
                            .accept("http://localhost:8002/")
                            .type("turn.rest")
                            .property("key.alias", "turn.shared.secret")
                            .property("key.algorithm", "HmacSHA1")
                            .property("credentials.generator", "class:" + DefaultCredentialsGenerator.class.getName())
                            .property("credentials.ttl", "43200")
                            .property("username.separator", ":")
                            .property("url", "turn:192.168.99.100:3478?transport=tcp")
                        .done()

                    .done();
            // @formatter:on
            init(configuration);
        }
    };


    @Rule
    public TestRule chain = createRuleChain(gateway, robot);

    @Specification("no.auth.credentials")
    @Test
    public void noCredentials() throws Exception {
       robot.finish();
    }

    @Specification("invalid.auth.credentials")
    @Test
    public void invalidCredentials() throws Exception {
        robot.finish();
    }

    @Specification("valid.auth.credentials")
    @Test
    public void validCredentials() throws Exception {
        robot.finish();
    }

    @Specification("no.security.realm")
    @Test
    public void serviceWithoutSecurityRealm() throws Exception {
        robot.finish();
    }

}
