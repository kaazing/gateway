/*
 * Copyright 2014, Kaazing Corporation. All rights reserved.
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

import java.net.URI;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

public class PingPongExtensionIT {

    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/ws.extensions/x-kaazing-ping-pong/wsn");

    private GatewayRule gateway = new GatewayRule() {
        {
            GatewayConfiguration configuration = new GatewayConfigurationBuilder()
                .service()
                    .accept(URI.create("wsn://localhost:8001/echo"))
                    .type("echo")
                    .crossOrigin()
                        .allowOrigin("*")
                    .done()
                    .acceptOption("ws.inactivity.timeout", "4secs")
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
    @Specification({"server.may.send.extended.ping.control.frames/request" })
    public void serverMaySendExtendedPingControlFrames() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({"client.should.reply.to.extended.ping.with.extended.pong/request" })
    public void clientShouldReplyToExtendedPingWithExtendedPong() throws Exception {
        k3po.finish();
    }

    /* The following test cases are client tests only in the k3po project
         - server.may.send.extended.ping.control.frames
         - client.should.reply.to.standard.ping.with.standard.pong
         - client.should.disconnect.if.wrong.control.bytes.length
         - client.should.disconnect.if.wrong.control.bytes.value
         - client.should.disconnect.if.no.control.bytes.sent
    */
}
