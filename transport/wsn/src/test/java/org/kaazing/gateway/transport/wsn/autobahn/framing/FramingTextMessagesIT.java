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
package org.kaazing.gateway.transport.wsn.autobahn.framing;

import static org.kaazing.test.util.ITUtil.createRuleChain;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

public class FramingTextMessagesIT {
	private K3poRule robot = new K3poRule();

    private GatewayRule gateway = new GatewayRule() {
        {
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                        .service()
                            .accept("ws://localhost:8555/echo")
                            .type("echo")
                        .done()
                    .done();
            init(configuration);
        }
    };

    @Rule
    public TestRule chain = createRuleChain(gateway, robot);


    @Specification("sendTextMessageWithPayloadLength125")
    @Test(timeout = 5000)
    public void sendTextMessageWithPayloadLength125() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithPayloadLength126")
    @Test(timeout = 5000)
    public void sendTextMessageWithPayloadLength126() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithPayloadLength127")
    @Test(timeout = 5000)
    public void sendTextMessageWithPayloadLength127() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithPayloadLength128")
    @Test(timeout = 5000)
    public void sendTextMessageWithPayloadLength128() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithPayloadLength65535")
    @Test(timeout = 5000)
    public void sendTextMessageWithPayloadLength65535() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithPayloadLength65536")
    @Test(timeout = 5000)
    public void sendTextMessageWithPayloadLength65536() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithPayloadLength65536InChopsOf997Octets")
    @Test(timeout = 5000)
    public void sendTextMessageWithPayloadLength65536InChopsOf997Octets() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12366")
    @Specification("sendTextMessageWithEmptyPayload")
    @Test(timeout = 5000)
    public void sendTextMessageWithEmptyPayload() throws Exception {
        robot.finish();
    }
}
