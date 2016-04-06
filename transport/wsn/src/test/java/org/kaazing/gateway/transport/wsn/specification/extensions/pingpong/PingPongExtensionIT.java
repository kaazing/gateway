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
package org.kaazing.gateway.transport.wsn.specification.extensions.pingpong;

import static org.kaazing.test.util.ITUtil.createRuleChain;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

public class PingPongExtensionIT {

    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/ws.extensions/x-kaazing-ping-pong");

    private GatewayRule gateway = new GatewayRule() {
        {
            GatewayConfiguration configuration = new GatewayConfigurationBuilder()
                .service()
                    .accept("wsn://localhost:8001/echo")
                    .type("echo")
                    .crossOrigin()
                        .allowOrigin("*")
                    .done()
                .done()
            .done();
            init(configuration);
        }
    };

    @Rule
    public TestRule chain = createRuleChain(gateway, k3po);

    @Test
    @Specification({"server.should.reply.to.standard.ping.with.standard.pong/request" })
    public void serverShouldReplyToStandardPingWithStandardPong() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({"server.should.reply.to.extended.ping.with.extended.pong/request" })
    public void serverShouldReplyToExtendedPingWithExtendedPong() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({"should.escape.text.frame.starting.with.control.bytes/request" })
    public void shouldEscapeTextFrameStartingWithControlBytes() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({"should.not.escape.binary.frame/request" })
    public void shouldNotEscapeBinaryFrame() throws Exception {
        k3po.finish();
    }

    /* The following test cases are client tests only in the k3po project
         - client.should.reply.to.extended.ping.with.extended.pong
         - client.should.receive.extended.pong.frame
         - client.should.reply.to.standard.ping.with.standard.pong
         - client.should.disconnect.if.wrong.control.bytes.length
         - client.should.disconnect.if.no.control.bytes.sent
     * The following scenario is tested in the PingPongExtensionTimeoutIT class
         - server.should.timeout.if.client.does.not.respond.to.extended.ping
    */
}
