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
package org.kaazing.gateway.transport.wsn.proxy;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

public class ProxyOrphanedConnectionIT {

    private K3poRule robot = new K3poRule();

    public GatewayRule gateway = new GatewayRule() {
        {
            // @formatter:off
            GatewayConfiguration configuration = new GatewayConfigurationBuilder()
                .service()
                    .name("proxy")
                    .description("proxy")
                    .accept("ws://localhost:8555/")
                    .connect("ws://localhost:8556/")
                    .type("proxy")
                .done()
            .done();
            // @formatter:on
            init(configuration);
        }
    };

    private TestRule timeout = new DisableOnDebug(Timeout.seconds(5));

    @Rule
    public TestRule chain = RuleChain.outerRule(gateway).around(robot).around(timeout);

    @Specification("connectToFrontEndProxyAndKillFrontBeforeBackendIsEstablished")
    @Test
    public void closeOnFrontBeforeConnectedFullyOnBackShouldKillBack() throws Exception {
        robot.finish();
    }
}
