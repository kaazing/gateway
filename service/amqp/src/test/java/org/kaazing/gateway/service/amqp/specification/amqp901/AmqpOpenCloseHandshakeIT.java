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
package org.kaazing.gateway.service.amqp.specification.amqp901;

import static org.kaazing.test.util.ITUtil.createRuleChain;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.ScriptProperty;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

public class AmqpOpenCloseHandshakeIT {


    private K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/amqp");

    private GatewayRule gateway = new GatewayRule() {
        {
            // @formatter:off
            GatewayConfiguration configuration = new GatewayConfigurationBuilder()
                    .service()
                    .accept("wsn://localhost:8001/amqp")
                    .connect("tcp://localhost:8010")
                    .type("amqp.proxy")
                    .property("service.domain","localhost")
                    .property("encryption.key.alias", "session")
                .done()
            .done();
            // @formatter:on
            init(configuration);
        }
    };

    @Rule
    public TestRule chain = createRuleChain(gateway, k3po);
    
    
    @Test
    @ScriptProperty({ "connectLocation \"http://localhost:8001/amqp\"", "acceptLocation \"tcp://localhost:8010\"" })
    @Specification({ "ws/open/identity/request", "ws/close/request",  
                     "tcp/open/identity/response", "tcp/close/response"})
    public void openAndCloseHandshakeWithIdentity() throws Exception {
            k3po.finish();
    }

    @Test
    @ScriptProperty({ "connectLocation \"http://localhost:8001/amqp\"", "acceptLocation \"tcp://localhost:8010\"" })
    @Specification({ "ws/open/noidentity/request", "ws/close/request",  
                     "tcp/open/noidentity/response", "tcp/close/response"})
    public void openAndCloseHandshakeWithNoIdentoty() throws Exception {
            k3po.finish();
    }
    
}
