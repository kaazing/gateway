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

package org.kaazing.gateway.transport.wsn.autobahn.framing;

import static org.junit.rules.RuleChain.outerRule;

import java.net.URI;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.robot.junit.annotation.Robotic;
import org.kaazing.robot.junit.rules.RobotRule;

public class FramingBinaryMessagesIT {
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
    
    @Robotic(script = "sendBinaryMessageWithPayloadLength125")
    @Test(timeout = 1500)
    public void sendBinaryMessageWithPayloadLength125() throws Exception {
        robot.join();
    }
    
    @Robotic(script = "sendBinaryMessageWithPayloadLength126")
    @Test(timeout = 1500)
    public void sendBinaryMessageWithPayloadLength126() throws Exception {
        robot.join();
    }
    
    @Robotic(script = "sendBinaryMessageWithPayloadLength127")
    @Test(timeout = 1500)
    public void sendBinaryMessageWithPayloadLength127() throws Exception {
        robot.join();
    }
    
    @Robotic(script = "sendBinaryMessageWithPayloadLength128")
    @Test(timeout = 1500)
    public void sendBinaryMessageWithPayloadLength128() throws Exception {
        robot.join();
    }
    
    @Robotic(script = "sendBinaryMessageWithPayloadLength65535")
    @Test(timeout = 1500)
    public void sendBinaryMessageWithPayloadLength65535() throws Exception {
        robot.join();
    }
    
    @Robotic(script = "sendBinaryMessageWithPayloadLength65536")
    @Test(timeout = 1500)
    public void sendBinaryMessageWithPayloadLength65536() throws Exception {
        robot.join();
    }
    
    @Robotic(script = "sendBinaryMessageWithPayloadLength65536InChopsOf997Octets")
    @Test(timeout = 1500)
    public void sendBinaryMessageWithPayloadLength65536InChopsOf997Octets() throws Exception {
        robot.join();
    }
    
    @Ignore("KG-12366")
    @Robotic(script = "sendBinaryMessageWithPayloadLengthZero")
    @Test(timeout = 1500)
    public void sendBinaryMessageWithPayloadLengthZero() throws Exception {
        robot.join();
    }
}
