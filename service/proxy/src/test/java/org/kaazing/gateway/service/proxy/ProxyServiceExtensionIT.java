/**
 * Copyright 2007-2015, Kaazing Corporation. All rights reserved.
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
package org.kaazing.gateway.service.proxy;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.test.util.ITUtil;

public class ProxyServiceExtensionIT {
    private K3poRule k3po = new K3poRule();

    public GatewayRule gateway = new GatewayRule() {
        {
            GatewayConfiguration configuration = new GatewayConfigurationBuilder()
                .service()
                    .accept("tcp://localhost:8001")
                    .connect("tcp://localhost:8002")
                    .type("proxy")
                    .crossOrigin()
                        .allowOrigin("*")
                    .done()
                .done()
            .done();

            init(configuration);
        }
    };

    @Rule
    public TestRule chain = ITUtil.createRuleChain(gateway, k3po);

    @Specification("shouldInjectBytesBeforeForwardingMessages")
    @Test
    public void shouldInjectBytesBeforeForwardingMessages() throws Exception {
        k3po.finish();
    }

}
