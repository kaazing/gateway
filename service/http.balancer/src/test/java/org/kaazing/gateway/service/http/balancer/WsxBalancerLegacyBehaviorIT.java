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
import static org.kaazing.test.util.ITUtil.createRuleChain;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

public class WsxBalancerLegacyBehaviorIT {

    private final K3poRule k3po = new K3poRule();

    private final GatewayRule gateway = new GatewayRule() {
        {
            GatewayConfiguration configuration = new GatewayConfigurationBuilder()
                    // balancer service to echo
                    .service()
                        .type("balancer")
                        .accept("ws://gateway.kaazing.com:8001/echo")
                        .acceptOption("tcp.bind", "localhost:8001")
                        .crossOrigin()
                            .allowOrigin("*")
                        .done()
                    .done()
                    // echo service
                    .service()
                        .type("echo")
                        .accept("ws://node.kaazing.com:8001/echo")
                        .balance("ws://gateway.kaazing.com:8001/echo")
                        .acceptOption("tcp.bind", "localhost:8001")
                        .crossOrigin()
                            .allowOrigin("*")
                        .done()
                    .done()
            .done();

            init(configuration);
        }
    };


    @Rule
    public TestRule chain = createRuleChain(gateway, k3po,  10, SECONDS);

    @Test
    @Specification("legacy.wsx.balancer.request")
    public void balancerFrameShouldIncludeClusterMembersURL() throws Exception {
        k3po.finish();
    }

}
