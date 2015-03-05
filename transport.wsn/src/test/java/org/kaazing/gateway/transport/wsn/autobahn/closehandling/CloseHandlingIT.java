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
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

public class CloseHandlingIT {
	private K3poRule robot = new K3poRule();

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
 
    @Specification("sendTextMessageThenCloseFrame")
    @Test(timeout = 1500)
    public void sendTextMessageThenCloseFrame() throws Exception {
        robot.finish();
    }
    
    @Specification("sendTwoCloseFrames")
    @Test(timeout = 1500)
    public void sendTwoCloseFrames() throws Exception {
        robot.finish();
    }
    
    @Specification("sendPingAfterCloseMessage")
    @Test(timeout = 1500)
    public void sendPingAfterCloseMessage() throws Exception {
        robot.finish();
    }
    
    @Specification("sendTextMessageAfterCloseFrame")
    @Test(timeout = 1500)
    public void sendTextMessageAfterCloseFrame() throws Exception {
        robot.finish();
    }
    
    @Specification("sendCloseFrameWithPayloadLengthZero")
    @Test(timeout = 1500)
    public void sendCloseFrameWithPayloadLengthZero() throws Exception {
        robot.finish();
    }
    
    @Specification("sendCloseFrameWithCloseCode")
    @Test(timeout = 1500)
    public void sendCloseFrameWithCloseCode() throws Exception {
        robot.finish();
    }
    
    @Specification("sendCloseFrameWithCloseCodeAndCloseReason")
    @Test(timeout = 1500)
    public void sendCloseFrameWithCloseCodeAndCloseReason() throws Exception {
        robot.finish();
    }
    
    @Specification("sendCloseFrameWithCloseCodeAndCloseReasonOfMaximumLength")
    @Test(timeout = 1500)
    public void sendCloseFrameWithCloseCodeAndCloseReasonOfMaximumLength() throws Exception {
        robot.finish();
    }

    @Specification("sendCloseWithValidCloseCode1000")
    @Test(timeout = 1500)
    public void sendCloseWithValidCloseCode1000() throws Exception {
        robot.finish();
    }

    @Specification("sendCloseWithValidCloseCode1001")
    @Test(timeout = 1500)
    public void sendCloseWithValidCloseCode1001() throws Exception {
        robot.finish();
    }

    @Specification("sendCloseWithValidCloseCode1002")
    @Test(timeout = 1500)
    public void sendCloseWithValidCloseCode1002() throws Exception {
        robot.finish();
    }

    @Specification("sendCloseWithValidCloseCode1003")
    @Test(timeout = 1500)
    public void sendCloseWithValidCloseCode1003() throws Exception {
        robot.finish();
    }

    @Specification("sendCloseWithValidCloseCode1007")
    @Test(timeout = 1500)
    public void sendCloseWithValidCloseCode1007() throws Exception {
        robot.finish();
    }

    @Specification("sendCloseWithValidCloseCode1008")
    @Test(timeout = 1500)
    public void sendCloseWithValidCloseCode1008() throws Exception {
        robot.finish();
    }

    @Specification("sendCloseWithValidCloseCode1009")
    @Test(timeout = 1500)
    public void sendCloseWithValidCloseCode1009() throws Exception {
        robot.finish();
    }

    @Specification("sendCloseWithValidCloseCode1010")
    @Test(timeout = 1500)
    public void sendCloseWithValidCloseCode1010() throws Exception {
        robot.finish();
    }

    @Specification("sendCloseWithValidCloseCode1011")
    @Test(timeout = 1500)
    public void sendCloseWithValidCloseCode1011() throws Exception {
        robot.finish();
    }

    @Specification("sendCloseWithValidCloseCode3000")
    @Test(timeout = 1500)
    public void sendCloseWithValidCloseCode3000() throws Exception {
        robot.finish();
    }
    
    @Specification("sendCloseWithValidCloseCode3999")
    @Test(timeout = 1500)
    public void sendCloseWithValidCloseCode3999() throws Exception {
        robot.finish();
    }
    
    @Specification("sendCloseWithValidCloseCode4000")
    @Test(timeout = 1500)
    public void sendCloseWithValidCloseCode4000() throws Exception {
        robot.finish();
    }
    
    @Specification("sendCloseWithValidCloseCode4999")
    @Test(timeout = 1500)
    public void sendCloseWithValidCloseCode4999() throws Exception {
        robot.finish();
    }

    @Specification("sendCloseWithInvalidCloseCodeZero")
    @Test(timeout = 1500)
    public void sendCloseWithInvalidCloseCodeZero() throws Exception {
        robot.finish();
    }
    
    @Specification("sendMessageFragment1FollowedByCloseThenFragment")
    @Test(timeout = 1500)
    public void sendMessageFragment1FollowedByCloseThenFragment() throws Exception {
        robot.finish();
    }
 
    @Specification("sendCloseWithInvalidCloseCode1005")
    @Test(timeout = 1500)
    public void	sendCloseWithInvalidCloseCode1005() throws Exception {
        robot.finish();
    }
    
    @Specification("sendCloseFrameWithPayloadLength1")
    @Test(timeout = 1500)
    public void	sendCloseFrameWithPayloadLength1() throws Exception {
        robot.finish();
    }
    
    @Ignore("KG-12373")
    @Specification("sendCloseFrameWithInvalidUTF8Payload")
    @Test(timeout = 1500)
    public void	sendCloseFrameWithInvalidUTF8Payload() throws Exception {
        robot.finish();
    }
    
    @Specification("sendCloseFrameWithInvalidCloseCode999")
    @Test(timeout = 1500)
    public void	sendCloseFrameWithInvalidCloseCode999() throws Exception {
        robot.finish();
    }
    
    @Specification("sendCloseFrameWithInvalidCloseCode1004")
    @Test(timeout = 1500)
    public void	sendCloseFrameWithInvalidCloseCode1004() throws Exception {
        robot.finish();
    }
    
    @Specification("sendCloseFrameWithInvalidCloseCode1006")
    @Test(timeout = 1500)
    public void	sendCloseFrameWithInvalidCloseCode1006() throws Exception {
        robot.finish();
    }
    
    @Specification("sendCloseFrameWithInvalidCloseCode1012")
    @Test(timeout = 1500)
    public void	sendCloseFrameWithInvalidCloseCode1012() throws Exception {
        robot.finish();
    }
    
    @Specification("sendCloseFrameWithInvalidCloseCode1013")
    @Test(timeout = 1500)
    public void	sendCloseFrameWithInvalidCloseCode1013() throws Exception {
        robot.finish();
    }
    
    @Specification("sendCloseFrameWithInvalidCloseCode1014")
    @Test(timeout = 1500)
    public void	sendCloseFrameWithInvalidCloseCode1014() throws Exception {
        robot.finish();
    }
    
    @Specification("sendCloseFrameWithInvalidCloseCode1015")
    @Test(timeout = 1500)
    public void	sendCloseFrameWithInvalidCloseCode1015() throws Exception {
        robot.finish();
    }
    
    @Specification("sendCloseFrameWithInvalidCloseCode1016")
    @Test(timeout = 1500)
    public void	sendCloseFrameWithInvalidCloseCode1016() throws Exception {
        robot.finish();
    }
    
    @Specification("sendCloseFrameWithInvalidCloseCode1100")
    @Test(timeout = 1500)
    public void	sendCloseFrameWithInvalidCloseCode1100() throws Exception {
        robot.finish();
    }
    
    @Specification("sendCloseFrameWithInvalidCloseCode2000")
    @Test(timeout = 1500)
    public void	sendCloseFrameWithInvalidCloseCode2000() throws Exception {
        robot.finish();
    }
    
    @Specification("sendCloseFrameWithInvalidCloseCode2999")
    @Test(timeout = 1500)
    public void	sendCloseFrameWithInvalidCloseCode2999() throws Exception {
        robot.finish();
    }
    
    @Specification("sendCloseFrameWithCloseCodeAndCloseReasonWhichIsTooLong")
    @Test(timeout = 1500)
    public void	sendCloseFrameWithCloseCodeAndCloseReasonWhichIsTooLong() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12377")
    @Specification("send256KMessageFollowedByCloseThenPing")
    @Test(timeout = 1500)
    public void	send256KMessageFollowedByCloseThenPing() throws Exception {
        robot.finish();
    }

    // behavior is undefined by spec but appears reasonable
    @Specification("sendCloseWithCloseCode5000")
    @Test(timeout = 1500)
    public void	sendCloseWithCloseCode5000() throws Exception {
        robot.finish();
    }
    
    // behavior is undefined by spec but appears reasonable
    @Specification("sendCloseWithCloseCode65535")
    @Test(timeout = 1500)
    public void	sendCloseWithCloseCode65535() throws Exception {
        robot.finish();
    }
}
