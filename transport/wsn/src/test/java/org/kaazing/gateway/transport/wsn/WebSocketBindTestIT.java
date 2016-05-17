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
package org.kaazing.gateway.transport.wsn;

import static org.kaazing.test.util.ITUtil.createRuleChain;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

public class WebSocketBindTestIT {

    private final K3poRule robot = new K3poRule();

    private final GatewayRule gateway = new GatewayRule() {
        {
            // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                        .service()
                            .accept("ws://localhost:8001/echo")
                            .accept("ws://localhost:8001")
                            .type("echo")
                            .crossOrigin()
                                .allowOrigin("*")
                            .done()
                        .done()
                    .done();
            // @formatter:on
            init(configuration);
        }
    };

    @Rule
    public TestRule chain = createRuleChain(gateway, robot);

    @Specification("canConnectUsingNormalWebSocketURI")
    @Test
    public void canConnectUsingNormalWebSocketURI() throws Exception {
        robot.finish();
    }

    @Specification("canConnectUsingNormalWebSocketURIWithExtraSlashAfterEcho")
    @Test
    public void canConnectUsingNormalWebSocketURIWithExtraSlash() throws Exception {
        robot.finish();
    }

    @Specification("canConnectUsingNormalWebSocketURIWithoutLoadBalancing")
    @Test
    //TODO:KG-8523
    public void canConnectUsingNormalWebSocketURIWithoutLoadBalancing() throws Exception {
        robot.finish();
    }

    @Specification("cannotConnectUsingWebSocketURIWithExtraPathElement")
    @Test
    public void cannotConnectUsingWebSocketURIWithExtraPathElement() throws Exception {
        robot.finish();
    }

    @Specification("canConnectUsingPathlessWebSocketURI")
    @Test
    public void canConnectUsingPathlessWebSocketURI() throws Exception {
        robot.finish();
    }

    @Specification("canConnectUsingPathlessWebSocketURIWithExtraSlash")
    @Test
    public void canConnectUsingPathlessWebSocketURIWithExtraSlash() throws Exception {
        robot.finish();
    }

    @Specification("cannotConnectToPathlessWebSocketURIWithExtraPathElement")
    @Test
    public void cannotConnectToPathlessWebSocketURIWithExtraPathElement() throws Exception {
        robot.finish();
    }
}
