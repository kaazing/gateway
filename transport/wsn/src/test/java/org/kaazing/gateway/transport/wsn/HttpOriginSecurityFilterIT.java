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

public class HttpOriginSecurityFilterIT {

    private K3poRule robot = new K3poRule();

    private GatewayRule gateway = new GatewayRule() {
        {
            // @formatter:off
            GatewayConfiguration gc = new GatewayConfigurationBuilder()
                .service()
                    .name("echoAuth")
                    .accept("ws://localhost:8084/echoAuth")
                    .type("echo")
                .done()
//                .service()
//                .name("echoAuth")
//                .accept(URI.create("ws://localhost:8084/echoAuth"))
//                .type("echo")
//                .done()
            .done();
            // @formatter:on
            init(gc);
        }
    };

    @Rule
    public TestRule chain = createRuleChain(gateway, robot);

    @Specification("test.privileged.origin.request.connect")
    @Test
    public void testPrivilegedOriginRequest() throws Exception {
        robot.finish();
    }
}
