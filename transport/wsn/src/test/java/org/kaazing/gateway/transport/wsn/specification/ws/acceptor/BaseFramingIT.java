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

/**
 * RFC-6455, section 5.2 "Base Framing Protocol"
 */
public class BaseFramingIT {

    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/ws/framing");

    private GatewayRule gateway = new GatewayRule() {
        {
            // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                        .service()
                            .accept("ws://localhost:8080/echo")
                            .type("echo")
                            .crossOrigin()
                                .allowOrigin("http://localhost:8001")
                            .done()
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
        "echo.binary.payload.length.0/handshake.request.and.frame"
        })
    public void shouldEchoBinaryFrameWithPayloadLength0() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "echo.binary.payload.length.125/handshake.request.and.frame"
        })
    public void shouldEchoBinaryFrameWithPayloadLength125() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "echo.binary.payload.length.126/handshake.request.and.frame"
        })
    public void shouldEchoBinaryFrameWithPayloadLength126() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "echo.binary.payload.length.127/handshake.request.and.frame"
        })
    public void shouldEchoBinaryFrameWithPayloadLength127() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "echo.binary.payload.length.128/handshake.request.and.frame"
        })
    public void shouldEchoBinaryFrameWithPayloadLength128() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "echo.binary.payload.length.65535/handshake.request.and.frame"
        })
    public void shouldEchoBinaryFrameWithPayloadLength65535() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "echo.binary.payload.length.65536/handshake.request.and.frame"
        })
    public void shouldEchoBinaryFrameWithPayloadLength65536() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "echo.text.payload.length.0/handshake.request.and.frame"
        })
    public void shouldEchoTextFrameWithPayloadLength0() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "echo.text.payload.length.125/handshake.request.and.frame"
        })
    public void shouldEchoTextFrameWithPayloadLength125() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "echo.text.payload.length.126/handshake.request.and.frame"
        })
    public void shouldEchoTextFrameWithPayloadLength126() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "echo.text.payload.length.127/handshake.request.and.frame"
        })
    public void shouldEchoTextFrameWithPayloadLength127() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "echo.text.payload.length.128/handshake.request.and.frame"
        })
    public void shouldEchoTextFrameWithPayloadLength128() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "echo.text.payload.length.65535/handshake.request.and.frame"
        })
    public void shouldEchoTextFrameWithPayloadLength65535() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "echo.text.payload.length.65536/handshake.request.and.frame"
        })
    public void shouldEchoTextFrameWithPayloadLength65536() throws Exception {
        k3po.finish();
    }
}
