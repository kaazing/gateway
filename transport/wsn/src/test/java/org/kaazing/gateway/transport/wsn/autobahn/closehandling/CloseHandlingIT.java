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
package org.kaazing.gateway.transport.wsn.autobahn.closehandling;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
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

public class CloseHandlingIT {
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
    public TestRule chain = createRuleChain(gateway, robot, 5000, MILLISECONDS);

    @Specification("sendTextMessageThenCloseFrame")
    @Test
    public void sendTextMessageThenCloseFrame() throws Exception {
        robot.finish();
    }

    @Specification("sendTwoCloseFrames")
    @Test
    public void sendTwoCloseFrames() throws Exception {
        robot.finish();
    }

    @Specification("sendPingAfterCloseMessage")
    @Test
    public void sendPingAfterCloseMessage() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageAfterCloseFrame")
    @Test
    public void sendTextMessageAfterCloseFrame() throws Exception {
        robot.finish();
    }

    @Specification("sendCloseFrameWithPayloadLengthZero")
    @Test
    public void sendCloseFrameWithPayloadLengthZero() throws Exception {
        robot.finish();
    }

    @Specification("sendCloseFrameWithCloseCode")
    @Test
    public void sendCloseFrameWithCloseCode() throws Exception {
        robot.finish();
    }

    @Specification("sendCloseFrameWithCloseCodeAndCloseReason")
    @Test
    public void sendCloseFrameWithCloseCodeAndCloseReason() throws Exception {
        robot.finish();
    }

    @Specification("sendCloseFrameWithCloseCodeAndCloseReasonOfMaximumLength")
    @Test
    public void sendCloseFrameWithCloseCodeAndCloseReasonOfMaximumLength() throws Exception {
        robot.finish();
    }

    @Specification("sendCloseWithValidCloseCode1000")
    @Test
    public void sendCloseWithValidCloseCode1000() throws Exception {
        robot.finish();
    }

    @Specification("sendCloseWithValidCloseCode1001")
    @Test
    public void sendCloseWithValidCloseCode1001() throws Exception {
        robot.finish();
    }

    @Specification("sendCloseWithValidCloseCode1002")
    @Test
    public void sendCloseWithValidCloseCode1002() throws Exception {
        robot.finish();
    }

    @Specification("sendCloseWithValidCloseCode1003")
    @Test
    public void sendCloseWithValidCloseCode1003() throws Exception {
        robot.finish();
    }

    @Specification("sendCloseWithValidCloseCode1007")
    @Test
    public void sendCloseWithValidCloseCode1007() throws Exception {
        robot.finish();
    }

    @Specification("sendCloseWithValidCloseCode1008")
    @Test
    public void sendCloseWithValidCloseCode1008() throws Exception {
        robot.finish();
    }

    @Specification("sendCloseWithValidCloseCode1009")
    @Test
    public void sendCloseWithValidCloseCode1009() throws Exception {
        robot.finish();
    }

    @Specification("sendCloseWithValidCloseCode1010")
    @Test
    public void sendCloseWithValidCloseCode1010() throws Exception {
        robot.finish();
    }

    @Specification("sendCloseWithValidCloseCode1011")
    @Test
    public void sendCloseWithValidCloseCode1011() throws Exception {
        robot.finish();
    }

    @Specification("sendCloseWithValidCloseCode3000")
    @Test
    public void sendCloseWithValidCloseCode3000() throws Exception {
        robot.finish();
    }

    @Specification("sendCloseWithValidCloseCode3999")
    @Test
    public void sendCloseWithValidCloseCode3999() throws Exception {
        robot.finish();
    }

    @Specification("sendCloseWithValidCloseCode4000")
    @Test
    public void sendCloseWithValidCloseCode4000() throws Exception {
        robot.finish();
    }

    @Specification("sendCloseWithValidCloseCode4999")
    @Test
    public void sendCloseWithValidCloseCode4999() throws Exception {
        robot.finish();
    }

    @Specification("sendCloseWithInvalidCloseCodeZero")
    @Test
    public void sendCloseWithInvalidCloseCodeZero() throws Exception {
        robot.finish();
    }

    @Specification("sendMessageFragment1FollowedByCloseThenFragment")
    @Test
    public void sendMessageFragment1FollowedByCloseThenFragment() throws Exception {
        robot.finish();
    }

    @Specification("sendCloseWithInvalidCloseCode1005")
    @Test
    public void	sendCloseWithInvalidCloseCode1005() throws Exception {
        robot.finish();
    }

    @Specification("sendCloseFrameWithPayloadLength1")
    @Test
    public void	sendCloseFrameWithPayloadLength1() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendCloseFrameWithInvalidUTF8Payload")
    @Test
    public void	sendCloseFrameWithInvalidUTF8Payload() throws Exception {
        robot.finish();
    }

    @Specification("sendCloseFrameWithInvalidCloseCode999")
    @Test
    public void	sendCloseFrameWithInvalidCloseCode999() throws Exception {
        robot.finish();
    }

    @Specification("sendCloseFrameWithInvalidCloseCode1004")
    @Test
    public void	sendCloseFrameWithInvalidCloseCode1004() throws Exception {
        robot.finish();
    }

    @Specification("sendCloseFrameWithInvalidCloseCode1006")
    @Test
    public void	sendCloseFrameWithInvalidCloseCode1006() throws Exception {
        robot.finish();
    }

    @Specification("sendCloseFrameWithInvalidCloseCode1012")
    @Test
    public void	sendCloseFrameWithInvalidCloseCode1012() throws Exception {
        robot.finish();
    }

    @Specification("sendCloseFrameWithInvalidCloseCode1013")
    @Test
    public void	sendCloseFrameWithInvalidCloseCode1013() throws Exception {
        robot.finish();
    }

    @Specification("sendCloseFrameWithInvalidCloseCode1014")
    @Test
    public void	sendCloseFrameWithInvalidCloseCode1014() throws Exception {
        robot.finish();
    }

    @Specification("sendCloseFrameWithInvalidCloseCode1015")
    @Test
    public void	sendCloseFrameWithInvalidCloseCode1015() throws Exception {
        robot.finish();
    }

    @Specification("sendCloseFrameWithInvalidCloseCode1016")
    @Test
    public void	sendCloseFrameWithInvalidCloseCode1016() throws Exception {
        robot.finish();
    }

    @Specification("sendCloseFrameWithInvalidCloseCode1100")
    @Test
    public void	sendCloseFrameWithInvalidCloseCode1100() throws Exception {
        robot.finish();
    }

    @Specification("sendCloseFrameWithInvalidCloseCode2000")
    @Test
    public void	sendCloseFrameWithInvalidCloseCode2000() throws Exception {
        robot.finish();
    }

    @Specification("sendCloseFrameWithInvalidCloseCode2999")
    @Test
    public void	sendCloseFrameWithInvalidCloseCode2999() throws Exception {
        robot.finish();
    }

    @Specification("sendCloseFrameWithCloseCodeAndCloseReasonWhichIsTooLong")
    @Test
    public void	sendCloseFrameWithCloseCodeAndCloseReasonWhichIsTooLong() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12377")
    @Specification("send256KMessageFollowedByCloseThenPing")
    @Test
    public void	send256KMessageFollowedByCloseThenPing() throws Exception {
        robot.finish();
    }

    // behavior is undefined by spec but appears reasonable
    @Specification("sendCloseWithCloseCode5000")
    @Test
    public void	sendCloseWithCloseCode5000() throws Exception {
        robot.finish();
    }

    // behavior is undefined by spec but appears reasonable
    @Specification("sendCloseWithCloseCode65535")
    @Test
    public void	sendCloseWithCloseCode65535() throws Exception {
        robot.finish();
    }
}
