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

import org.junit.Assert;
import org.junit.Test;

import org.kaazing.gateway.server.test.Gateway;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.gateway.util.feature.EarlyAccessFeatures;

import java.security.KeyStore;

public class TestConfigurationIT {
    private static final String ACCEPT_URL = "http://localhost:8000/";

    @Test
    public void multipleCredentialsGenerators() throws Exception {
        Gateway gateway = new Gateway();

        // @formatter:off
        GatewayConfiguration configuration =
                new GatewayConfigurationBuilder()
                    .property(EarlyAccessFeatures.TURN_REST_SERVICE.getPropertyName(), "true")
                    .service()
                        .accept(ACCEPT_URL)
                        .type("turn.rest")

                        .property("key.alias", "turn.shared.secret")
                        .property("key.algorithm", "HmacSHA1")
                        .property("credentials.generator", "class:" + DefaultCredentialsGenerator.class.getName())
                        .property("credentials.generator", "class:" + DefaultCredentialsGenerator.class.getName())
                        .property("credentials.ttl", "43200")
                        .property("username.separator", ":")
                        .property("url", "turn:192.168.99.100:3478?transport=tcp")
                    .done()

                .done();
        // @formatter:on

        try {
            gateway.start(configuration);
        }
        catch(IllegalArgumentException e){
            Assert.assertEquals(e.getMessage(),"Unknown credential generator class: org.kaazing.gateway.service.turn.rest.DefaultCredentialsGenerator,class:org.kaazing.gateway.service.turn.rest.DefaultCredentialsGenerator");
        }
        finally {
            gateway.stop();
        }
    }

    @Test
    public void earlyAccessFeatureIsDisabled() throws Exception {
        Gateway gateway = new Gateway();

        // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                        .property(EarlyAccessFeatures.TURN_REST_SERVICE.getPropertyName(), "false")
                        .service()
                            .accept(ACCEPT_URL)
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

        try {
            gateway.start(configuration);
        }
        catch(UnsupportedOperationException e){
            Assert.assertEquals(e.getMessage(),"Feature \"turn.rest\" (TURN REST Service) not enabled");
        }
        finally {
            gateway.stop();
        }
    }

    @Test
    public void keyStoreIsNull() throws Exception {
        Gateway gateway = new Gateway();

        KeyStore keyStore = null;
        char[] password = "ab987c".toCharArray();

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
                    .done();
        // @formatter:on

        try {
            gateway.start(configuration);
        }
        catch(NullPointerException e){
            Assert.assertEquals(e.getMessage(),null);
        }
        finally {
            gateway.stop();
        }
    }

}
