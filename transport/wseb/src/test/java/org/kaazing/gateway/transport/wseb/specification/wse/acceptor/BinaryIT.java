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
package org.kaazing.gateway.transport.wseb.specification.wse.acceptor;

import static org.kaazing.gateway.util.InternalSystemProperty.WSE_SPECIFICATION;
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

public class BinaryIT {

    private final K3poRule k3po = new K3poRule()
            .setScriptRoot("org/kaazing/specification/wse/data");

    private GatewayRule gateway = new GatewayRule() {
        {
            // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                        .property(WSE_SPECIFICATION.getPropertyName(), "true")
                        .service()
                            .accept("ws://localhost:8080/path")
                            .type("echo")
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
        "echo.binary.payload.length.0/request"
        })
    @Ignore("Bug Gateway #254: echo service does not properly handle 0 length data frames")
    public void shouldEchoFrameWithPayloadLength0() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "echo.binary.payload.length.127/request"
        })
    public void shouldEchoFrameWithPayloadLength127() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "echo.binary.payload.length.128/request"
        })
    public void shouldEchoFrameWithPayloadLength128() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "echo.binary.payload.length.65535/request"
        })
    public void shouldEchoFrameWithPayloadLength65535() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "echo.binary.payload.length.65536/request"
         })
    public void shouldEchoFrameWithPayloadLength65536() throws Exception {
        k3po.finish();
    }
}