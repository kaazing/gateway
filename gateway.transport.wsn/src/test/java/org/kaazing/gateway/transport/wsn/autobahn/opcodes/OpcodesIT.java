/**
 * Copyright (c) 2007-2014 Kaazing Corporation. All rights reserved.
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.kaazing.gateway.transport.wsn.autobahn.opcodes;

import static org.junit.rules.RuleChain.outerRule;

import java.net.URI;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.robot.junit.annotation.Robotic;
import org.kaazing.robot.junit.rules.RobotRule;

public class OpcodesIT {
	private RobotRule robot = new RobotRule();

    private GatewayRule gateway = new GatewayRule() {
        {
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                        .service()
                            .accept(URI.create("ws://localhost:8555/echo"))
                            .type("echo")
                        .done()
                    .done();
            init(configuration);
        }
    };

    @Rule
    public TestRule chain = outerRule(robot).around(gateway);
    
    @Robotic(script = "sendFrameWithReservedNonControlOpcodeEquals3")
    @Test(timeout = 1500)
    public void sendFrameWithReservedNonControlOpcodeEquals3() throws Exception {
        robot.join();
    }
    
    @Robotic(script = "sendFrameWithReservedNonControlOpcodeEquals4AndNonEmptyPayload")
    @Test(timeout = 1500)
    public void sendFrameWithReservedNonControlOpcodeEquals4AndNonEmptyPayload() throws Exception {
        robot.join();
    }
    
    @Robotic(script = "sendSmallTextMessageThenFrameWithReservedNonControlOpcodeEquals5ThenPing")
    @Test(timeout = 1500)
    public void sendSmallTextMessageThenFrameWithReservedNonControlOpcodeEquals5ThenPing() throws Exception {
        robot.join();
    }
    
    @Robotic(script = "sendSmallTextMessageThenFrameWithReservedNonControlOpcodeEquals6AndNonEmptyPayloadThenPing")
    @Test(timeout = 1500)
    public void sendSmallTextMessageThenFrameWithReservedNonControlOpcodeEquals6AndNonEmptyPayloadThenPing() throws Exception {
        robot.join();
    }
    
    @Robotic(script = "sendSmallTextMessageThenFrameWithReservedNonControlOpcodeEquals7AndNonEmptyPayloadThenPing")
    @Test(timeout = 1500)
    public void sendSmallTextMessageThenFrameWithReservedNonControlOpcodeEquals7AndNonEmptyPayloadThenPing() throws Exception {
        robot.join();
    }
    
    @Robotic(script = "sendFrameWithReservedControlOpcodeEquals11")
    @Test(timeout = 1500)
    public void sendFrameWithReservedControlOpcodeEquals11() throws Exception {
        robot.join();
    }
    
    @Robotic(script = "sendFrameWithReservedControlOpcodeEquals12AndNonEmptyPayload")
    @Test(timeout = 1500)
    public void sendFrameWithReservedControlOpcodeEquals12AndNonEmptyPayload() throws Exception {
        robot.join();
    }
    
    @Robotic(script = "sendFrameWithReservedControlOpcodeEquals13ThenPing")
    @Test(timeout = 1500)
    public void sendFrameWithReservedNonControlOpcodeEquals13ThenPing() throws Exception {
        robot.join();
    }
    
    @Robotic(script = "sendSmallTextMessageThenFrameWithReservedControlOpcodeEquals14AndNonEmptyPayloadThenPing")
    @Test(timeout = 1500)
    public void sendSmallTextMessageThenFrameWithReservedControlOpcodeEquals14AndNonEmptyPayloadThenPing() throws Exception {
        robot.join();
    }
    
    @Robotic(script = "sendSmallTextMessageThenFrameWithReservedControlOpcodeEquals15AndNonEmptyPayloadThenPing")
    @Test(timeout = 1500)
    public void sendSmallTextMessageThenFrameWithReservedControlOpcodeEquals15AndNonEmptyPayloadThenPing() throws Exception {
        robot.join();
    }
}
