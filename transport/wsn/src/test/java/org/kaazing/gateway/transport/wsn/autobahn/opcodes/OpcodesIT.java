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
package org.kaazing.gateway.transport.wsn.autobahn.opcodes;

import static org.kaazing.test.util.ITUtil.createRuleChain;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

public class OpcodesIT {
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

    @Specification("sendFrameWithReservedNonControlOpcodeEquals3")
    @Test
    public void sendFrameWithReservedNonControlOpcodeEquals3() throws Exception {
        robot.finish();
    }

    @Specification("sendFrameWithReservedNonControlOpcodeEquals4AndNonEmptyPayload")
    @Test
    public void sendFrameWithReservedNonControlOpcodeEquals4AndNonEmptyPayload() throws Exception {
        robot.finish();
    }

    @Specification("sendSmallTextMessageThenFrameWithReservedNonControlOpcodeEquals5ThenPing")
    @Test
    public void sendSmallTextMessageThenFrameWithReservedNonControlOpcodeEquals5ThenPing() throws Exception {
        robot.finish();
    }

    @Specification("sendSmallTextMessageThenFrameWithReservedNonControlOpcodeEquals6AndNonEmptyPayloadThenPing")
    @Test
    public void sendSmallTextMessageThenFrameWithReservedNonControlOpcodeEquals6AndNonEmptyPayloadThenPing() throws Exception {
        robot.finish();
    }

    @Specification("sendSmallTextMessageThenFrameWithReservedNonControlOpcodeEquals7AndNonEmptyPayloadThenPing")
    @Test
    public void sendSmallTextMessageThenFrameWithReservedNonControlOpcodeEquals7AndNonEmptyPayloadThenPing() throws Exception {
        robot.finish();
    }

    @Specification("sendFrameWithReservedControlOpcodeEquals11")
    @Test
    public void sendFrameWithReservedControlOpcodeEquals11() throws Exception {
        robot.finish();
    }

    @Specification("sendFrameWithReservedControlOpcodeEquals12AndNonEmptyPayload")
    @Test
    public void sendFrameWithReservedControlOpcodeEquals12AndNonEmptyPayload() throws Exception {
        robot.finish();
    }

    @Specification("sendFrameWithReservedControlOpcodeEquals13ThenPing")
    @Test
    public void sendFrameWithReservedNonControlOpcodeEquals13ThenPing() throws Exception {
        robot.finish();
    }

    @Specification("sendSmallTextMessageThenFrameWithReservedControlOpcodeEquals14AndNonEmptyPayloadThenPing")
    @Test
    public void sendSmallTextMessageThenFrameWithReservedControlOpcodeEquals14AndNonEmptyPayloadThenPing() throws Exception {
        robot.finish();
    }

    @Specification("sendSmallTextMessageThenFrameWithReservedControlOpcodeEquals15AndNonEmptyPayloadThenPing")
    @Test
    public void sendSmallTextMessageThenFrameWithReservedControlOpcodeEquals15AndNonEmptyPayloadThenPing() throws Exception {
        robot.finish();
    }
}
