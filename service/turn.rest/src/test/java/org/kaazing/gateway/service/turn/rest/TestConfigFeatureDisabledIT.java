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
import org.junit.rules.ExpectedException;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.gateway.util.feature.EarlyAccessFeatures;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

import javax.crypto.spec.SecretKeySpec;
import java.io.FileInputStream;
import java.nio.charset.UnmappableCharacterException;
import java.security.KeyStore;

import static java.nio.charset.Charset.forName;
import static org.kaazing.test.util.ITUtil.createRuleChain;

public class TestConfigFeatureDisabledIT {
    private static final String ACCEPT_URL = "http://localhost:8000/";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private final K3poRule robot = new K3poRule();

    private final GatewayRule gateway = new GatewayRule() {
        {
            thrown.expect(UnsupportedOperationException.class);
            thrown.expectMessage("Feature \"turn.rest\" (TURN REST Service) not enabled");

            // @formatter:off

            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                        .property(EarlyAccessFeatures.TURN_REST_SERVICE.getPropertyName(), "false")
                        .service()
                            .accept(ACCEPT_URL)
                            .type("turn.rest")

                            .property("generate.credentials", "class:" + DefaultCredentialsGenerator.class.getName())
                            .property("key.alias", "turn.shared.secret")
                            .property("key.algorithm", "HmacSHA1")
                            .nestedProperty("uris")
                                .property("uri", "uri1")
                                .property("uri", "uri2")
                                .property("uri", "uri3")
                            .done()
                            .nestedProperty("options")
                                .property("credentials.ttl", "43200")
                                .property("username.separator", ":")
                            .done()
                        .done()

                    .done();
            // @formatter:on+
            init(configuration);
        }
    };


    @Rule
    public TestRule chain = createRuleChain(gateway, robot).around(thrown);

    @Test
    public void earlyAccessFeatureIsDisabledIT() throws Exception {
    }

}
