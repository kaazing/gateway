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

package org.kaazing.gateway.transport.wsn.autobahn.closehandling;

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

public class CloseHandlingIT {
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
 
    @Robotic(script = "sendTextMessageThenCloseFrame")
    @Test(timeout = 1500)
    public void sendTextMessageThenCloseFrame() throws Exception {
        robot.join();
    }
    
    @Robotic(script = "sendTwoCloseFrames")
    @Test(timeout = 1500)
    public void sendTwoCloseFrames() throws Exception {
        robot.join();
    }
    
    @Robotic(script = "sendPingAfterCloseMessage")
    @Test(timeout = 1500)
    public void sendPingAfterCloseMessage() throws Exception {
        robot.join();
    }
    
    @Robotic(script = "sendTextMessageAfterCloseFrame")
    @Test(timeout = 1500)
    public void sendTextMessageAfterCloseFrame() throws Exception {
        robot.join();
    }
    
    @Robotic(script = "sendCloseFrameWithPayloadLengthZero")
    @Test(timeout = 1500)
    public void sendCloseFrameWithPayloadLengthZero() throws Exception {
        robot.join();
    }
    
    @Robotic(script = "sendCloseFrameWithCloseCode")
    @Test(timeout = 1500)
    public void sendCloseFrameWithCloseCode() throws Exception {
        robot.join();
    }
    
    @Robotic(script = "sendCloseFrameWithCloseCodeAndCloseReason")
    @Test(timeout = 1500)
    public void sendCloseFrameWithCloseCodeAndCloseReason() throws Exception {
        robot.join();
    }
    
    @Robotic(script = "sendCloseFrameWithCloseCodeAndCloseReasonOfMaximumLength")
    @Test(timeout = 1500)
    public void sendCloseFrameWithCloseCodeAndCloseReasonOfMaximumLength() throws Exception {
        robot.join();
    }

    @Robotic(script = "sendCloseWithValidCloseCode1000")
    @Test(timeout = 1500)
    public void sendCloseWithValidCloseCode1000() throws Exception {
        robot.join();
    }

    @Robotic(script = "sendCloseWithValidCloseCode1001")
    @Test(timeout = 1500)
    public void sendCloseWithValidCloseCode1001() throws Exception {
        robot.join();
    }

    @Robotic(script = "sendCloseWithValidCloseCode1002")
    @Test(timeout = 1500)
    public void sendCloseWithValidCloseCode1002() throws Exception {
        robot.join();
    }

    @Robotic(script = "sendCloseWithValidCloseCode1003")
    @Test(timeout = 1500)
    public void sendCloseWithValidCloseCode1003() throws Exception {
        robot.join();
    }

    @Robotic(script = "sendCloseWithValidCloseCode1007")
    @Test(timeout = 1500)
    public void sendCloseWithValidCloseCode1007() throws Exception {
        robot.join();
    }

    @Robotic(script = "sendCloseWithValidCloseCode1008")
    @Test(timeout = 1500)
    public void sendCloseWithValidCloseCode1008() throws Exception {
        robot.join();
    }

    @Robotic(script = "sendCloseWithValidCloseCode1009")
    @Test(timeout = 1500)
    public void sendCloseWithValidCloseCode1009() throws Exception {
        robot.join();
    }

    @Robotic(script = "sendCloseWithValidCloseCode1010")
    @Test(timeout = 1500)
    public void sendCloseWithValidCloseCode1010() throws Exception {
        robot.join();
    }

    @Robotic(script = "sendCloseWithValidCloseCode1011")
    @Test(timeout = 1500)
    public void sendCloseWithValidCloseCode1011() throws Exception {
        robot.join();
    }

    @Robotic(script = "sendCloseWithValidCloseCode3000")
    @Test(timeout = 1500)
    public void sendCloseWithValidCloseCode3000() throws Exception {
        robot.join();
    }
    
    @Robotic(script = "sendCloseWithValidCloseCode3999")
    @Test(timeout = 1500)
    public void sendCloseWithValidCloseCode3999() throws Exception {
        robot.join();
    }
    
    @Robotic(script = "sendCloseWithValidCloseCode4000")
    @Test(timeout = 1500)
    public void sendCloseWithValidCloseCode4000() throws Exception {
        robot.join();
    }
    
    @Robotic(script = "sendCloseWithValidCloseCode4999")
    @Test(timeout = 1500)
    public void sendCloseWithValidCloseCode4999() throws Exception {
        robot.join();
    }

    @Robotic(script = "sendCloseWithInvalidCloseCodeZero")
    @Test(timeout = 1500)
    public void sendCloseWithInvalidCloseCodeZero() throws Exception {
        robot.join();
    }
    
    @Robotic(script = "sendMessageFragment1FollowedByCloseThenFragment")
    @Test(timeout = 1500)
    public void sendMessageFragment1FollowedByCloseThenFragment() throws Exception {
        robot.join();
    }
 
    @Robotic(script = "sendCloseWithInvalidCloseCode1005")
    @Test(timeout = 1500)
    public void	sendCloseWithInvalidCloseCode1005() throws Exception {
        robot.join();
    }
    
    @Robotic(script = "sendCloseFrameWithPayloadLength1")
    @Test(timeout = 1500)
    public void	sendCloseFrameWithPayloadLength1() throws Exception {
        robot.join();
    }
    
    @Ignore("KG-12373")
    @Robotic(script = "sendCloseFrameWithInvalidUTF8Payload")
    @Test(timeout = 1500)
    public void	sendCloseFrameWithInvalidUTF8Payload() throws Exception {
        robot.join();
    }
    
    @Robotic(script = "sendCloseFrameWithInvalidCloseCode999")
    @Test(timeout = 1500)
    public void	sendCloseFrameWithInvalidCloseCode999() throws Exception {
        robot.join();
    }
    
    @Robotic(script = "sendCloseFrameWithInvalidCloseCode1004")
    @Test(timeout = 1500)
    public void	sendCloseFrameWithInvalidCloseCode1004() throws Exception {
        robot.join();
    }
    
    @Robotic(script = "sendCloseFrameWithInvalidCloseCode1006")
    @Test(timeout = 1500)
    public void	sendCloseFrameWithInvalidCloseCode1006() throws Exception {
        robot.join();
    }
    
    @Robotic(script = "sendCloseFrameWithInvalidCloseCode1012")
    @Test(timeout = 1500)
    public void	sendCloseFrameWithInvalidCloseCode1012() throws Exception {
        robot.join();
    }
    
    @Robotic(script = "sendCloseFrameWithInvalidCloseCode1013")
    @Test(timeout = 1500)
    public void	sendCloseFrameWithInvalidCloseCode1013() throws Exception {
        robot.join();
    }
    
    @Robotic(script = "sendCloseFrameWithInvalidCloseCode1014")
    @Test(timeout = 1500)
    public void	sendCloseFrameWithInvalidCloseCode1014() throws Exception {
        robot.join();
    }
    
    @Robotic(script = "sendCloseFrameWithInvalidCloseCode1015")
    @Test(timeout = 1500)
    public void	sendCloseFrameWithInvalidCloseCode1015() throws Exception {
        robot.join();
    }
    
    @Robotic(script = "sendCloseFrameWithInvalidCloseCode1016")
    @Test(timeout = 1500)
    public void	sendCloseFrameWithInvalidCloseCode1016() throws Exception {
        robot.join();
    }
    
    @Robotic(script = "sendCloseFrameWithInvalidCloseCode1100")
    @Test(timeout = 1500)
    public void	sendCloseFrameWithInvalidCloseCode1100() throws Exception {
        robot.join();
    }
    
    @Robotic(script = "sendCloseFrameWithInvalidCloseCode2000")
    @Test(timeout = 1500)
    public void	sendCloseFrameWithInvalidCloseCode2000() throws Exception {
        robot.join();
    }
    
    @Robotic(script = "sendCloseFrameWithInvalidCloseCode2999")
    @Test(timeout = 1500)
    public void	sendCloseFrameWithInvalidCloseCode2999() throws Exception {
        robot.join();
    }
    
    @Robotic(script = "sendCloseFrameWithCloseCodeAndCloseReasonWhichIsTooLong")
    @Test(timeout = 1500)
    public void	sendCloseFrameWithCloseCodeAndCloseReasonWhichIsTooLong() throws Exception {
        robot.join();
    }

    @Ignore("KG-12377")
    @Robotic(script = "send256KMessageFollowedByCloseThenPing")
    @Test(timeout = 1500)
    public void	send256KMessageFollowedByCloseThenPing() throws Exception {
        robot.join();
    }

    // behavior is undefined by spec but appears reasonable
    @Robotic(script = "sendCloseWithCloseCode5000")
    @Test(timeout = 1500)
    public void	sendCloseWithCloseCode5000() throws Exception {
        robot.join();
    }
    
    // behavior is undefined by spec but appears reasonable
    @Robotic(script = "sendCloseWithCloseCode65535")
    @Test(timeout = 1500)
    public void	sendCloseWithCloseCode65535() throws Exception {
        robot.join();
    }
}
