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
package org.kaazing.gateway.transport.wsn.specification.ws.acceptor;

import static org.kaazing.test.util.ITUtil.createRuleChain;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

public class LimitsMaxSize126IT {
    private static String WS_ECHO_SERVICE_ACCEPT = "ws://localhost:8080/echo";

    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/ws/limits");

    private GatewayRule gateway = new GatewayRule() {
        {
            // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                        .service()
                            .accept(WS_ECHO_SERVICE_ACCEPT)
                            .type("echo")
                            .crossOrigin()
                                .allowOrigin("*")
                            .done()
                            .acceptOption("ws.maximum.message.size", Integer.toString(125))
                        .done()
                    .done();
            // @formatter:on
            init(configuration);
        }
    };

    @Rule
    public TestRule chain = createRuleChain(gateway, k3po);

    @Test
    @Specification({
        "should.fail.binary.payload.length.126/handshake.request.and.frame"
        })
    public void shouldRefuseBinaryFrameWithPayloadLengthExceeding125() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "should.fail.text.payload.length.126/handshake.request.and.frame"
        })
    public void shouldRefuseTextFrameWithPayloadLengthExceeding125() throws Exception {
        k3po.finish();
    }

}
