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

package org.kaazing.gateway.transport.wsn.autobahn.reservedbits;

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

public class ReservedBitsIT {
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
    
    @Robotic(script = "sendCloseWithRSVEquals7")
    @Test(timeout = 1500)
    public void sendCloseWithRSVEquals() throws Exception {
        robot.join();
    }
    
    @Robotic(script = "sendSmallTextMessageWithRSVEquals1")
    @Test(timeout = 1500)
    public void sendSmallTextMessageWithRSVEquals1() throws Exception {
        robot.join();
    }
    
    @Robotic(script = "sendSmallTextMessageThenSmallTextMessageWithRSVEquals2ThenSendPing")
    @Test(timeout = 1500)
    public void sendSmallTextMessageThenSmallTextMessageWithRSVEquals2ThenSendPing() throws Exception {
        robot.join();
    }
    
    @Robotic(script = "sendSmallTextMessageThenSmallTextMessageWithRSVEquals3ThenSendPingInFrameWiseChops")
    @Test(timeout = 1500)
    public void sendSmallTextMessageThenSmallTextMessageWithRSVEquals3ThenSendPingInFrameWiseChops() throws Exception {
        robot.join();
    }

    @Ignore("KG-12368")
    @Robotic(script = "sendSmallTextMessageThenSmallTextMessageWithRSVEquals4ThenSendPingInOctetWiseChops")
    @Test(timeout = 1500)
    public void sendSmallTextMessageThenSmallTextMessageWithRSVEquals3ThenSendPingInOctetWiseChops() throws Exception {
        robot.join();
    }
    
    @Robotic(script = "sendSmallBinaryMessageWithRSVEquals5")
    @Test(timeout = 1500)
    public void sendSmallBinaryMessageWithRSVEquals5() throws Exception {
        robot.join();
    }
    
    @Robotic(script = "sendPingWithRSVEquals6")
    @Test(timeout = 1500)
    public void sendPingWithRSVEquals6() throws Exception {
        robot.join();
    }
    
}
