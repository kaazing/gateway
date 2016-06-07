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
package org.kaazing.gateway.transport.wseb;

import static org.kaazing.test.util.ITUtil.createRuleChain;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.Gateway;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

public class WseHandshakeIT {

    private static final String ECHO_SERVICE_ACCEPT = "wse://localhost:8001/echo";

    private final K3poRule robot = new K3poRule();

    private final GatewayRule gateway = new GatewayRule() {
        {
            // @formatter:off
                GatewayConfiguration configuration =
                        new GatewayConfigurationBuilder()
                            .webRootDirectory(new File("src/test/webapp"))
                            .property(Gateway.GATEWAY_CONFIG_DIRECTORY_PROPERTY,"src/test/resources/gateway/conf")
                            .service()
                                .accept(ECHO_SERVICE_ACCEPT)
                                .type("echo")
                            .done()
                        .done();
                // @formatter:on
            init(configuration);
        }
    };

    @Rule
    public final TestRule chain = createRuleChain(gateway, robot);

    @Specification("wse.handshake.send.receive.3_5")
    @Test
    public void testHandshakeSendReceiveVersion3_5() throws Exception {
        robot.finish();
    }

    @Specification("wse.handshake.send.receive")
    @Test
    public void testHandshakeSendReceive() throws Exception {
        robot.finish();
    }

    @Specification("closeDownstreamShouldUnbindUpstream")
    @Test
    public void closeDownstreamShouldUnbindUpstream() throws Exception {
        robot.finish();
    }

}
