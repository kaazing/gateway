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

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

/**
 * RFC-6455, section 5.5 "Control Frames"
 */
public class ControlIT {

    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/ws/control");

    private final GatewayRule gateway = new GatewayRule() {
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
        "client.send.close.payload.length.0/handshake.request.and.frame"
        })
    public void shouldEchoClientCloseFrameWithEmptyPayload() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.close.payload.length.1/handshake.request.and.frame"
        })
    public void shouldEchoClientCloseFrameWithPayloadSize1() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.close.payload.length.125/handshake.request.and.frame"
        })
    public void shouldEchoClientCloseFrameWithPayload() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.close.payload.length.126/handshake.request.and.frame"
        })
    public void shouldFailWebSocketConnectionWhenClientSendCloseFrameWithPayloadTooLong() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.ping.payload.length.0/handshake.request.and.frame"
        })
    public void shouldPongClientPingFrameWithEmptyPayload() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.ping.payload.length.125/handshake.request.and.frame"
        })
    public void shouldPongClientPingFrameWithPayload() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.ping.payload.length.126/handshake.request.and.frame"
        })
    public void shouldFailWebSocketConnectionWhenClientSendPingFrameWithPayloadTooLong() throws Exception {
        k3po.finish();
    }

    @Test
    @Ignore("Timeout error")
    @Specification({
        "client.send.pong.payload.length.0/handshake.request.and.frame"
        })
    public void shouldReceiveClientPongFrameWithEmptyPayload() throws Exception {
        k3po.finish();
    }

    @Test
    @Ignore("Timeout error")
    @Specification({
        "client.send.pong.payload.length.125/handshake.request.and.frame"
        })
    public void shouldReceiveClientPongFrameWithPayload() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.pong.payload.length.126/handshake.request.and.frame"
        })
    public void shouldFailWebSocketConnectionWhenClientSendPongFrameWithPayloadTooLong() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.opcode.0x0b/handshake.request.and.frame"
        })
    public void shouldFailWebSocketConnectionWhenClientSendOpcode11Frame() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.opcode.0x0c/handshake.request.and.frame"
        })
    public void shouldFailWebSocketConnectionWhenClientSendOpcode12Frame() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.opcode.0x0d/handshake.request.and.frame"
        })
    public void shouldFailWebSocketConnectionWhenClientSendOpcode13Frame() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.opcode.0x0e/handshake.request.and.frame"
        })
    public void shouldFailWebSocketConnectionWhenClientSendOpcode14Frame() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.opcode.0x0f/handshake.request.and.frame"})
    public void shouldFailWebSocketConnectionWhenClientSendOpcode15Frame() throws Exception {
        k3po.finish();
    }
}
