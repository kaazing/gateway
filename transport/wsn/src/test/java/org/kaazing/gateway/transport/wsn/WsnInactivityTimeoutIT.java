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

public class WsnInactivityTimeoutIT {

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
                        .acceptOption("ws.inactivity.timeout", "2sec")
                    .done()
                    .service()
                        .accept("ws://localhost:80/echo80")
                        .type("echo")
                        .crossOrigin()
                            .allowOrigin("*")
                        .done()
                        .acceptOption("ws.inactivity.timeout", "2sec")
                        .acceptOption("tcp.bind", "8080")
                    .done()
                .done();

            init(configuration);
        }
    };

    @Rule
    //4s should suffice (twice the expected 2 second timeout), but leave a margin just in case:
    public TestRule chain = createRuleChain(gateway, robot, 8, SECONDS);

    @Specification("shouldInactivityTimeout")
    @Test
    public void shouldInactivityTimeout() throws Exception {
        robot.finish();
    }

    @Specification("shouldInactivityTimeoutWithPingPongExtension")
    @Test
    public void shouldInactivityTimeoutWithPingPongExtension() throws Exception {
        robot.finish();
    }

    @Specification("shouldInactivityTimeoutWithPingPongExtensionAndExtendedHandshake")
    @Test
    public void shouldInactivityTimeoutWithPingPongExtensionAndExtendedHandshake() throws Exception {
        robot.finish();
    }

    @Specification("shouldInactivityTimeoutWithPingPongExtensionAndExtendedHandshakePort80")
    @Test
    // Test case for KG-10384
    // This test originally was required to run against port 80, however that was FUBAR because failsafe doesn't
    // have permission to run against port 80 mac or linux.  So we have used a tcp.bind to 8080 and this was shown
    // to be sufficient to run the test and reproduce the issue
    public void shouldInactivityTimeoutWithPingPongExtensionAndExtendedHandshakePort80() throws Exception {
        robot.finish();
    }

    @Specification("shouldInactivityTimeoutWhenNetworkFailsDuringExtendedHandshake")
    @Test
    public void shouldInactivityTimeoutWhenNetworkFailsDuringExtendedHandshake() throws Exception {
        robot.finish();
    }

    @Test
    @Specification("shouldSendPingForExtendedHandshake")
    public void shouldSendPingForExtendedHandshake() throws Exception {
        robot.finish();
    }

}
