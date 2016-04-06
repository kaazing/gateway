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
package org.kaazing.gateway.service.broadcast;

import static org.kaazing.test.util.ITUtil.createRuleChain;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

public class BroadcastServiceIT {

    private K3poRule robot = new K3poRule();

    private GatewayRule gateway = new GatewayRule() {
        {
            // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                        .service()
                            .accept("tcp://localhost:8090")
                            .connect("tcp://localhost:7788")
                            .type("broadcast")
                        .done()
                        .service()
                            .accept("tcp://localhost:8091")
                            .connect("tcp://localhost:7789")
                            .type("broadcast")
                            .property("on.client.message", "broadcast")
                        .done()
                    .done();
            // @formatter:on
            init(configuration);
        }
    };

    @Rule
    public TestRule chain = createRuleChain(gateway, robot);

    @Specification("broadcast.backend.tcp.connect.and.close")
    @Test
    public void tcpConnectionToBackendAndClose() throws Exception {
        robot.finish();
    }

    @Specification("broadcast.backend.tcp.connect.send.and.close")
    @Test
    public void tcpConnectionToBackendSendAndClose() throws Exception {
        robot.finish();
    }

    @Specification("broadcast.frontend.tcp.connect.and.close")
    @Test
    public void tcpConnectToFrontendAndClose() throws Exception {
        robot.finish();
    }

    @Specification("tcp.frontend.broadcast")
    @Test
    public void tcpFrontEndBroadcast() throws Exception {
        robot.finish();
    }

    @Specification("tcp.frontend.broadcast.many")
    @Test
    public void tcpFrontEndBroadcastMany() throws Exception {
        robot.finish();
    }

}
