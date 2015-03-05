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

package org.kaazing.gateway.transport.wsn.autobahn.fragmentation;

import static org.junit.rules.RuleChain.outerRule;

import java.net.URI;

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
    
    @Specification("sendContinuationFrameWithFINEqualsFalseWhenThereIsNothingToContinueThenTextMessageInTwoFragmentsTwice")
    @Test(timeout = 6500)
    public void sendContinuationFrameWithFINEqualsFalseWhenThereIsNothingToContinueThenTextMessageInTwoFragmentsTwice() throws Exception {
        robot.finish();
    }
   
    @Specification("sendContinuationFrameWithFINEqualsTrueWhenThereIsNothingToContinueThenTextMessageInTwoFragmentsTwice")
    @Test(timeout = 6500)
    public void sendContinuationFrameWithFINEqualsTrueWhenThereIsNothingToContinueThenTextMessageInTwoFragmentsTwice() throws Exception {
        robot.finish();
    }
    
    @Specification("sendPingInTwoFragments")
    @Test(timeout = 6500)
    public void sendPingInTwoFragments() throws Exception {
        robot.finish();
    }
    
    @Specification("sendPongInTwoFragments")
    @Test(timeout = 6500)
    public void sendPongInTwoFragments() throws Exception {
        robot.finish();
    }
    
    @Specification("sendTextMessageAfterContinuationframeWithFINEqualsFalseWhenThereIsNothingToContinueInFrameWiseChops")
    @Test(timeout = 6500)
    public void sendTextMessageAfterContinuationframeWithFINEqualsFalseWhenThereIsNothingToContinueInFrameWiseChops() throws Exception {
        robot.finish();
    }
    
    @Specification("sendTextMessageAfterContinuationframeWithFINEqualsFalseWhenThereIsNothingToContinueSentInOctetWiseChops")
    @Test(timeout = 6500)
    public void sendTextMessageAfterContinuationframeWithFINEqualsFalseWhenThereIsNothingToContinueSentInOctetWiseChops() throws Exception {
        robot.finish();
    }
    
    @Specification("sendTextMessageAfterContinuationFrameWithFINEqualsFalseWhenThereIsNothingToContinueSentInOneChop")
    @Test(timeout = 6500)
    public void sendTextMessageAfterContinuationFrameWithFINEqualsFalseWhenThereIsNothingToContinueSentInOneChop() throws Exception {
        robot.finish();
    }
    
    @Specification("sendTextMessageAfterContinuationFrameWithFINEqualsTrueWhenNothingToContinueSentInFrameWiseChops")
    @Test(timeout = 6500)
    public void sendTextMessageAfterContinuationFrameWithFINEqualsTrueWhenNothingToContinueSentInFrameWiseChops() throws Exception {
        robot.finish();
    }
    
    @Specification("sendTextMessageAfterContinuationFrameWithFINEqualsTrueWhenThereIsNothingToContinueSentInOctetWiseChops")
    @Test(timeout = 6500)
    public void sendTextMessageAfterContinuationFrameWithFINEqualsTrueWhenThereIsNothingToContinueSentInOctetWiseChops() throws Exception {
        robot.finish();
    }
    
    @Specification("sendTextMessageAfterContinuationFrameWithFINEqualsTrueWhenThereIsNothingToContinueSentInOneChop")
    @Test(timeout = 6500)
    public void sendTextMessageAfterContinuationFrameWithFINEqualsTrueWhenThereIsNothingToContinueSentInOneChop() throws Exception {
        robot.finish();
    }
    
    @Specification("sendTextMessageInMultipleFramesWithPingsWithPayloadsInBetween")
    @Test(timeout = 1500)
    public void sendTextMessageInMultipleFramesWithPingsWithPayloadsInBetween() throws Exception {
        robot.finish();
    }
    
    @Specification("sendTextMessageInMultipleFramesWithPingsWithPayloadsInBetweenAndAllFramesWithSYNCEqualsTrue")
    @Test(timeout = 1500)
    public void sendTextMessageInMultipleFramesWithPingsWithPayloadsInBetweenAndAllFramesWithSYNCEqualsTrue() throws Exception {
        robot.finish();
    }
    
    @Specification("sendTextMessageInTwoFragments")
    @Test(timeout = 1500)
    public void sendTextMessageInTwoFragments() throws Exception {
        robot.finish();
    }
    
    @Specification("sendTextMessageInTwoFragmentsInFrameWiseChops")
    @Test(timeout = 1500)
    public void sendTextMessageInTwoFragmentsInFrameWiseChops() throws Exception {
        robot.finish();
    }
    
    @Specification("sendTextMessageInTwoFragmentsInOctetWiseChops")
    @Test(timeout = 1500)
    public void sendTextMessageInTwoFragmentsInOctetWiseChops() throws Exception {
        robot.finish();
    }
    
    @Specification("sendTextMessageInTwoFragmentsThenContinuationWithFINEqualsFalseAndNothingToContinueThenUnfragmentedTextMessage")
    @Test(timeout = 1500)
    public void sendTextMessageInTwoFragmentsThenContinuationWithFINEqualsFalseAndNothingToContinueThenUnfragmentedTextMessage() throws Exception {
        robot.finish();
    }
    
    @Specification("sendTextMessageInTwoFragmentsWithBothFrameOpcodesSetToText")
    @Test(timeout = 1500)
    public void sendTextMessageInTwoFragmentsWithBothFrameOpcodesSetToText() throws Exception {
        robot.finish();
    }
    
    @Specification("sendTextMessageInTwoFragmentsWithOnePingWithPayloadInBetween")
    @Test(timeout = 1500)
    public void sendTextMessageInTwoFragmentsWithOnePingWithPayloadInBetween() throws Exception {
        robot.finish();
    }
    
    @Specification("sendTextMessageInTwoFragmentsWithOnePingWithPayloadInBetweenInFrameWiseChops")
    @Test(timeout = 1500)
    public void sendTextMessageInTwoFragmentsWithOnePingWithPayloadInBetweenInFrameWiseChopss() throws Exception {
        robot.finish();
    }
    
    @Specification("sendTextMessageInTwoFragmentsWithOnePingWithPayloadInBetweenInOctetWiseChops")
    @Test(timeout = 1500)
    public void sendTextMessageInTwoFragmentsWithOnePingWithPayloadInBetweenInOctetWiseChops() throws Exception {
        robot.finish();
    }
}
