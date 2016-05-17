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

import static org.kaazing.test.util.ITUtil.createRuleChain;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

public class ApplicationTokenIT {

    private final K3poRule robot = new K3poRule();

    public GatewayRule gateway = new GatewayRule() {
        {
            GatewayConfiguration configuration = new GatewayConfigurationBuilder()
                    .service()
                        .accept("ws://localhost:8001/echo")
                        .type("echo")
                        .crossOrigin()
                            .allowOrigin("*")
                        .done()
                        .realmName("auth-required")
                    .done()
                    .security()
                        .realm()
                            .name("auth-required")
                            .description("Kaazing WebSocket Gateway Demo")
                            .httpChallengeScheme("Application Token")
                            .authorizationMode("challenge")
                            .loginModule()
                                .type("class:org.kaazing.gateway.security.auth.YesLoginModule")
                                .success("requisite")
                                .option("roles", "AUTHORIZED, ADMINISTRATOR")
                            .done()
                        .done()
                    .done()
                .done();
            init(configuration);
        }
    };

    @Rule
    public TestRule chain = createRuleChain(gateway, robot);

    @Specification("should.get.403.response.when.trying.native.connect")
    @Test
    public void connectingFromRFC6455NonKaazingClientShouldFail() throws Exception {
        robot.finish();
    }
}
