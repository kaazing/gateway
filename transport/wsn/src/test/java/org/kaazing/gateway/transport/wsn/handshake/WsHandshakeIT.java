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
package org.kaazing.gateway.transport.wsn.handshake;

import static org.kaazing.test.util.ITUtil.createRuleChain;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

public class WsHandshakeIT {

    private K3poRule robot = new K3poRule();

    private GatewayRule gateway = new GatewayRule() {
        {
            // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                        .service()
                            .accept("ws://localhost:8010/jms")
                            .type("echo")
                            .crossOrigin()
                                .allowOrigin("http://localhost:8000")
                            .done()
                        .done()
                        .service()
                            .name("jms")
                            .accept("ws://localhost:8005/jms")
                            .accept("wss://foo.example.com:443/jms")
                            .type("echo")
                            .acceptOption("ssl.encryption", "disabled")
                            .acceptOption("wss.bind", "8011")
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

    @Specification("websocket.handshake")
    @Test
    public void shouldNegotiateKaazingHandshakeSubProtocol() throws Exception {
        robot.finish();
    }

    @Specification("websocket.handshake.missing.upgrade")
    @Test
    public void websocketHandshakeMissingUpgrade() throws Exception {
        robot.finish();
    }

    // Test for legacy websocket handshake
    // Deleting httpxdraft, wsxdraft resource address dependencies will fail the test
    // Instead of sending 101, gateway sends 404 in that case as there is no binding
    // for ws/draft-7x protocol
    @Test
    @Specification("websocket.handshake.legacy")
    public void shouldNegotiateLegacyKaazingHandshake() throws Exception {
        robot.finish();
    }
}
