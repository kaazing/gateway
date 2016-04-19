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
package org.kaazing.gateway.transport.wsn.autobahn.fragmentation;

import static org.kaazing.test.util.ITUtil.createRuleChain;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

public class FragmentationIT {
	private K3poRule robot = new K3poRule();

    private GatewayRule gateway = new GatewayRule() {
        {
            //@formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                        .service()
                            .accept("ws://localhost:8555/echo")
                            .type("echo")
                        .done()
                    .done();
            init(configuration);
            //@formatter:on
        }
    };

    @Rule
    public TestRule chain = createRuleChain(gateway, robot);

    @Specification("sendContinuationFrameWithFINEqualsFalseWhenThereIsNothingToContinueThenTextMessageInTwoFragmentsTwice")
    @Test
    public void sendContinuationFrameWithFINEqualsFalseWhenThereIsNothingToContinueThenTextMessageInTwoFragmentsTwice() throws Exception {
        robot.finish();
    }

    @Specification("sendContinuationFrameWithFINEqualsTrueWhenThereIsNothingToContinueThenTextMessageInTwoFragmentsTwice")
    @Test
    public void sendContinuationFrameWithFINEqualsTrueWhenThereIsNothingToContinueThenTextMessageInTwoFragmentsTwice() throws Exception {
        robot.finish();
    }

    @Specification("sendPingInTwoFragments")
    @Test
    public void sendPingInTwoFragments() throws Exception {
        robot.finish();
    }

    @Specification("sendPongInTwoFragments")
    @Test
    public void sendPongInTwoFragments() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageAfterContinuationframeWithFINEqualsFalseWhenThereIsNothingToContinueInFrameWiseChops")
    @Test
    public void sendTextMessageAfterContinuationframeWithFINEqualsFalseWhenThereIsNothingToContinueInFrameWiseChops() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageAfterContinuationframeWithFINEqualsFalseWhenThereIsNothingToContinueSentInOctetWiseChops")
    @Test
    public void sendTextMessageAfterContinuationframeWithFINEqualsFalseWhenThereIsNothingToContinueSentInOctetWiseChops() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageAfterContinuationFrameWithFINEqualsFalseWhenThereIsNothingToContinueSentInOneChop")
    @Test
    public void sendTextMessageAfterContinuationFrameWithFINEqualsFalseWhenThereIsNothingToContinueSentInOneChop() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageAfterContinuationFrameWithFINEqualsTrueWhenNothingToContinueSentInFrameWiseChops")
    @Test
    public void sendTextMessageAfterContinuationFrameWithFINEqualsTrueWhenNothingToContinueSentInFrameWiseChops() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageAfterContinuationFrameWithFINEqualsTrueWhenThereIsNothingToContinueSentInOctetWiseChops")
    @Test
    public void sendTextMessageAfterContinuationFrameWithFINEqualsTrueWhenThereIsNothingToContinueSentInOctetWiseChops() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageAfterContinuationFrameWithFINEqualsTrueWhenThereIsNothingToContinueSentInOneChop")
    @Test
    public void sendTextMessageAfterContinuationFrameWithFINEqualsTrueWhenThereIsNothingToContinueSentInOneChop() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageInMultipleFramesWithPingsWithPayloadsInBetween")
    @Test
    public void sendTextMessageInMultipleFramesWithPingsWithPayloadsInBetween() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageInMultipleFramesWithPingsWithPayloadsInBetweenAndAllFramesWithSYNCEqualsTrue")
    @Test
    public void sendTextMessageInMultipleFramesWithPingsWithPayloadsInBetweenAndAllFramesWithSYNCEqualsTrue() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageInTwoFragments")
    @Test
    public void sendTextMessageInTwoFragments() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageInTwoFragmentsInFrameWiseChops")
    @Test
    public void sendTextMessageInTwoFragmentsInFrameWiseChops() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageInTwoFragmentsInOctetWiseChops")
    @Test
    public void sendTextMessageInTwoFragmentsInOctetWiseChops() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageInTwoFragmentsThenContinuationWithFINEqualsFalseAndNothingToContinueThenUnfragmentedTextMessage")
    @Test
    public void sendTextMessageInTwoFragmentsThenContinuationWithFINEqualsFalseAndNothingToContinueThenUnfragmentedTextMessage() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageInTwoFragmentsWithBothFrameOpcodesSetToText")
    @Test
    public void sendTextMessageInTwoFragmentsWithBothFrameOpcodesSetToText() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageInTwoFragmentsWithOnePingWithPayloadInBetween")
    @Test
    public void sendTextMessageInTwoFragmentsWithOnePingWithPayloadInBetween() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageInTwoFragmentsWithOnePingWithPayloadInBetweenInFrameWiseChops")
    @Test
    public void sendTextMessageInTwoFragmentsWithOnePingWithPayloadInBetweenInFrameWiseChopss() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageInTwoFragmentsWithOnePingWithPayloadInBetweenInOctetWiseChops")
    @Test
    public void sendTextMessageInTwoFragmentsWithOnePingWithPayloadInBetweenInOctetWiseChops() throws Exception {
        robot.finish();
    }
}
