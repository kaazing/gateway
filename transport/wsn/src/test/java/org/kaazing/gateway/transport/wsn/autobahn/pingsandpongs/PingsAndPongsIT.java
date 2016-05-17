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
package org.kaazing.gateway.transport.wsn.autobahn.pingsandpongs;

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

public class PingsAndPongsIT {
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

    @Specification("sendPingWithoutPayload")
    @Test
    public void sendPingWithoutPayload() throws Exception {
        robot.finish();
    }

    @Specification("sendPingWithSmallTextPayload")
    @Test
    public void sendPingWithSmallTextPayload() throws Exception {
        robot.finish();
    }

    @Specification("sendPingWithSmallBinaryPayload")
    @Test
    public void sendPingWithSmallBinaryPayload() throws Exception {
        robot.finish();
    }

    @Specification("sendPingWithBinaryPayloadOf125Octets")
    @Test
    public void sendPingWithBinaryPayloadOf125Octets() throws Exception {
        robot.finish();
    }

    @Specification("sendPingWithBinaryPayloadOf125OctetsInOctetWiseChops")
    @Test
    public void sendPingWithBinaryPayloadOf125OctetsInOctetWiseChops() throws Exception {
        robot.finish();
    }

    @Specification("sendUnsolicitedPongWithoutPayload")
    @Test
    public void sendUnsolicitedPongWithoutPayload() throws Exception {
        robot.finish();
    }

    @Specification("sendUnsolicitedPongWithPayload")
    @Test
    public void sendUnsolicitedPongWithPayload() throws Exception {
        robot.finish();
    }

    @Specification("sendUnsolicitedPongWithPayloadThenPingWithPayload")
    @Test
    public void sendUnsolicitedPongWithPayloadThenPingWithPayload() throws Exception {
        robot.finish();
    }

    @Specification("sendTenPingsWithPayload")
    @Test
    public void sendTenPingsWithPayload() throws Exception {
        robot.finish();
    }

    @Specification("sendTenPingsWithPayloadInOctetWiseChops")
    @Test
    public void sendTenPingsWithPayloadInOctetWiseChops() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12367")
    @Specification("sendPingWithBinaryPayloadOf126Octets")
    @Test
    public void sendPingWithBinaryPayloadOf126Octets() throws Exception {
        robot.finish();
    }
}
