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

import static org.kaazing.test.util.ITUtil.createRuleChain;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

public class TurnRestIT {

    private static final String ACCEPT_URL = "http://localhost:8000/";

    private final K3poRule robot = new K3poRule();

    private final GatewayRule gateway = new GatewayRule() {
        {
            // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                        .service()
                            .accept(ACCEPT_URL)
                            .type("turn.rest")
                            .property("generate.credentials", "class:" + TestCredentialGenerator.class.getName())
                            .nestedProperty("options")
                                .property("secret", "secret")
                                .property("symbol", ":")
                                .nestedProperty("uris")
                                    .property("uri", "uri1")
                                    .property("uri", "uri2")
                                    .property("uri", "uri3")
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

    @Specification("generate.valid.response")
    @Test
    public void generateValidResponse() throws Exception {
        robot.finish();
    }
    
    @Specification("invalid.service.parameter")
    @Test
    public void testInvalidServiceParameter() throws Exception {
        robot.finish();
    }
}
