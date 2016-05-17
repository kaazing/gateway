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
package org.kaazing.gateway.transport.wsn.specification.ws.acceptor;

import static org.junit.Assert.assertTrue;
import static org.kaazing.test.util.ITUtil.createRuleChain;

import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

public class LimitsSessionTimeoutIT {
    public static final long TEST_LIFETIME = TimeUnit.SECONDS.toSeconds(3);

    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/ws/limits");

    private GatewayRule gateway = new GatewayRule() {
        {
            // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                        .service()
                            .accept("ws://localhost:8080/echoAuth")
                            .type("echo")
                            .realmName("demo")
                                .authorization()
                                .requireRole("USER")
                            .done()
                        .done()
                        .security()
                            .realm()
                                .name("demo")
                                .description("Kaazing WebSocket Gateway Demo")
                                .httpChallengeScheme("Basic")
                                .sessionTimeout(String.valueOf(TEST_LIFETIME))
                                .loginModule()
                                    .type("class:org.kaazing.gateway.transport.wsn.auth.AsyncBasicLoginModule")
                                    .success("requisite")
                                    .option("roles", "USER")
                                .done()
                            .done()
                        .done()
                    .done();
            // @formatter:on
            init(configuration);
        }
    };

    @Rule
    public TestRule chain = createRuleChain(gateway, k3po);

    @Test
    @Specification({
        "should.fail.max.lifetime.exceeded/handshake.request.and.frame"
        })
    public void shouldCloseWebSocketConnectionAfterSessionLifetimeIsUp() throws Exception {
        k3po.start();
        k3po.awaitBarrier("HANDSHAKE_COMPLETE");

        long startTime = System.currentTimeMillis();
        k3po.awaitBarrier("CLOSE_RECEIVED");
        long endTime = System.currentTimeMillis();

        long timePassed = endTime - startTime;
        //make sure the connection does not close immediately
        assertTrue(timePassed >= TimeUnit.SECONDS.toMillis(TEST_LIFETIME)/2);
        k3po.finish();
    }

}
