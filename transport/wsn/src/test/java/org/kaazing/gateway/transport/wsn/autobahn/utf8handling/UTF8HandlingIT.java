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
package org.kaazing.gateway.transport.wsn.autobahn.utf8handling;

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

public class UTF8HandlingIT {
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

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment2")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment2() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment3")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment3() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment4")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment4() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment5")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment5() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment6")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment6() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment7")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment7() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment8")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment8() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment9")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment9() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment10")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment10() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment11")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment11() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment12")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment12() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment13")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment13() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment14")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment14() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment15")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment15() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment16")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment16() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment17")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment17() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment18")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment18() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment19")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment19() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment20")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment20() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment21")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment21() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment22")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment22() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment23")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment23() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment24")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment24() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment25")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment25() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment26")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment26() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment27")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment27() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment28")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment28() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment29")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment29() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment30")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment30() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment31")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment31() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment32")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment32() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment33")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment33() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment34")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment34() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment35")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment35() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment36")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment36() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment37")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment37() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment38")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment38() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment39")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment39() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment40")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment40() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment41")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment41() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment42")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment42() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment43")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment43() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment44")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment44() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment45")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment45() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment46")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment46() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment47")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment47() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment48")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment48() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment49")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment49() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment50")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment50() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment51")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment51() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment52")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment52() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment53")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment53() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment54")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment54() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment55")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment55() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment56")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment56() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment57")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment57() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment58")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment58() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment59")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment59() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment60")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment60() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment61")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment61() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment62")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment62() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment63")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment63() throws Exception {
        robot.finish();
    }

    @Specification("sendTextMessageWithValidUTF8PayloadInOneFragment64")
    @Test
    public void sendTextMessageWithValidUTF8PayloadInOneFragment64() throws Exception {
        robot.finish();
    }

    @Ignore("Requires updates from branch KG-10406-continuation-frame and fix for KG-12373")
    @Specification("sendTextMessageWithValidUTF8PayloadInTwoFragmentsFragmentedOnCodePointBoundary")
    @Test
    public void	sendTextMessageWithValidUTF8PayloadInTwoFragmentsFragmentedOnCodePointBoundary() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12366")
    @Specification("sendTextMessageOfLengthZero")
    @Test
    public void	sendTextMessageOfLengthZero() throws Exception {
        robot.finish();
    }

    @Ignore("Requires updates from branch KG-10406-continuation-frame and fix for KG-12366")
    @Specification("sendThreeFragmentedTextMessagesOfLengthZero")
    @Test
    public void	sendThreeFragmentedTextMessagesOfLengthZero() throws Exception {
        robot.finish();
    }

    @Ignore("Requires updates from branch KG-10406-continuation-frame and fix for KG-12366")
    @Specification("sendThreeFragmentedTextMessagesFirstAndLastLengthZeroMiddleNonEmpty")
    @Test
    public void	sendThreeFragmentedTextMessagesFirstAndLastLengthZeroMiddleNonEmpty() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment2")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment2() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment3")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment3() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment4")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment4() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment5")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment5() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment6")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment6() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment7")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment7() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment8")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment8() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment9")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment9() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment10")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment10() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment11")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment11() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment12")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment12() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment13")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment13() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment14")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment14() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment15")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment15() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment16")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment16() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment17")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment17() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment18")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment18() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment19")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment19() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment20")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment20() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment21")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment21() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment22")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment22() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment23")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment23() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment24")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment24() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment25")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment25() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment26")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment26() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment27")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment27() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment28")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment28() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment29")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment29() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment30")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment30() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment31")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment31() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment32")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment32() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment33")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment33() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment34")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment34() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment35")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment35() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment36")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment36() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment37")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment37() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment38")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment38() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment39")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment39() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment40")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment40() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment41")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment41() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment42")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment42() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment43")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment43() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment44")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment44() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment45")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment45() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment46")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment46() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment47")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment47() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment48")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment48() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment49")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment49() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment50")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment50() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment51")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment51() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment52")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment52() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment53")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment53() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment54")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment54() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment55")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment55() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment56")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment56() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment57")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment57() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment58")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment58() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment59")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment59() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment60")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment60() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment61")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment61() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment62")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment62() throws Exception {
        robot.finish();
    }

    @Ignore("Requires updates from branch KG-10406-continuation-frame and fix for KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment63")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment63() throws Exception {
        robot.finish();
    }

    @Ignore("Requires updates from branch KG-10406-continuation-frame and fix for KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment64")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment64() throws Exception {
        robot.finish();
    }

    @Ignore("Requires updates from branch KG-10406-continuation-frame and fix for KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment65")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment65() throws Exception {
        robot.finish();
    }

    @Ignore("Requires updates from branch KG-10406-continuation-frame and fix for KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment66")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment66() throws Exception {
        robot.finish();
    }

    @Ignore("Requires updates from branch KG-10406-continuation-frame and fix for KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment67")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment67() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment68")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment68() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment69")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment69() throws Exception {
        robot.finish();
    }

    @Ignore("KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneFragment70")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneFragment70() throws Exception {
        robot.finish();
    }

    @Ignore("Requires updates from branch KG-10406-continuation-frame and fix for KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadInOneOctetFragments")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadInOneOctetFragments() throws Exception {
        robot.finish();
    }

    @Ignore("Requires updates from branch KG-10406-continuation-frame and fix for KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadIn3Fragments")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadIn3Fragments() throws Exception {
        robot.finish();
    }

    @Ignore("Requires updates from branch KG-10406-continuation-frame and fix for KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadIn3Fragments2")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadIn3Fragments2() throws Exception {
        robot.finish();
    }

    @Ignore("Requires updates from branch KG-10406-continuation-frame and fix for KG-12373")
    @Specification("sendTextMessagewithInvalidUTF8PayloadIn3Chops")
    @Test
    public void	sendTextMessagewithInvalidUTF8PayloadIn3Chops() throws Exception {
        robot.finish();
    }

    @Ignore("Requires updates from branch KG-10406-continuation-frame and fix for KG-12373")
    @Specification("sendTextMessageWithInvalidUTF8PayloadIn3Chops2")
    @Test
    public void	sendTextMessageWithInvalidUTF8PayloadIn3Chops2() throws Exception {
        robot.finish();
    }

    @Ignore("Requires updates from branch KG-10406-continuation-frame and fix for KG-12373")
    @Specification("sendTextMessageWithValidUTF8PayloadInOneOctetFragments")
    @Test
    public void	sendTextMessageWithValidUTF8PayloadInOneOctetFragments() throws Exception {
        robot.finish();
    }

    @Ignore("Requires updates from branch KG-10406-continuation-frame and fix for KG-12373")
    @Specification("sendTextMessageWithValidUTF8PayloadInOneOctetFragments2")
    @Test
    public void	sendTextMessageWithValidUTF8PayloadInOneOctetFragments2() throws Exception {
        robot.finish();
    }
}
