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
 * RFC-6455, section 5.8 "Extensibility"
 */
public class ExtensibilityIT {

    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/ws/extensibility");

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
        "client.send.text.frame.with.rsv.1/handshake.request.and.frame"
        })
    public void shouldFailWebSocketConnectionWhenClientSendTextFrameWithRsv1() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.binary.frame.with.rsv.1/handshake.request.and.frame"
        })
    public void shouldFailWebSocketConnectionWhenClientSendBinaryFrameWithRsv1() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.close.frame.with.rsv.1/handshake.request.and.frame"
        })
    public void shouldFailWebSocketConnectionWhenClientSendCloseFrameWithRsv1() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.ping.frame.with.rsv.1/handshake.request.and.frame"
        })
    public void shouldFailWebSocketConnectionWhenClientSendPingFrameWithRsv1() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.pong.frame.with.rsv.1/handshake.request.and.frame"
        })
    public void shouldFailWebSocketConnectionWhenClientSendPongFrameWithRsv1() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.text.frame.with.rsv.2/handshake.request.and.frame"
        })
    public void shouldFailWebSocketConnectionWhenClientSendTextFrameWithRsv2() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.binary.frame.with.rsv.2/handshake.request.and.frame"
        })
    public void shouldFailWebSocketConnectionWhenClientSendBinaryFrameWithRsv2() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.close.frame.with.rsv.2/handshake.request.and.frame"
        })
    public void shouldFailWebSocketConnectionWhenClientSendCloseFrameWithRsv2() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.ping.frame.with.rsv.2/handshake.request.and.frame"
        })
    public void shouldFailWebSocketConnectionWhenClientSendPingFrameWithRsv2() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.pong.frame.with.rsv.2/handshake.request.and.frame"
        })
    public void shouldFailWebSocketConnectionWhenClientSendPongFrameWithRsv2() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.text.frame.with.rsv.3/handshake.request.and.frame"
        })
    public void shouldFailWebSocketConnectionWhenClientSendTextFrameWithRsv3() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.binary.frame.with.rsv.3/handshake.request.and.frame"
        })
    public void shouldFailWebSocketConnectionWhenClientSendBinaryFrameWithRsv3() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.close.frame.with.rsv.3/handshake.request.and.frame"
        })
    public void shouldFailWebSocketConnectionWhenClientSendCloseFrameWithRsv3() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.ping.frame.with.rsv.3/handshake.request.and.frame"
        })
    public void shouldFailWebSocketConnectionWhenClientSendPingFrameWithRsv3() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.pong.frame.with.rsv.3/handshake.request.and.frame"
        })
    public void shouldFailWebSocketConnectionWhenClientSendPongFrameWithRsv3() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.text.frame.with.rsv.4/handshake.request.and.frame"
        })
    public void shouldFailWebSocketConnectionWhenClientSendTextFrameWithRsv4() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.binary.frame.with.rsv.4/handshake.request.and.frame"
        })
    public void shouldFailWebSocketConnectionWhenClientSendBinaryFrameWithRsv4() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.close.frame.with.rsv.4/handshake.request.and.frame"
        })
    public void shouldFailWebSocketConnectionWhenClientSendCloseFrameWithRsv4() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.ping.frame.with.rsv.4/handshake.request.and.frame"
        })
    public void shouldFailWebSocketConnectionWhenClientSendPingFrameWithRsv4() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.pong.frame.with.rsv.4/handshake.request.and.frame"
        })
    public void shouldFailWebSocketConnectionWhenClientSendPongFrameWithRsv4() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.text.frame.with.rsv.5/handshake.request.and.frame"
        })
    public void shouldFailWebSocketConnectionWhenClientSendTextFrameWithRsv5() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.binary.frame.with.rsv.5/handshake.request.and.frame"
        })
    public void shouldFailWebSocketConnectionWhenClientSendBinaryFrameWithRsv5() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.close.frame.with.rsv.5/handshake.request.and.frame"
        })
    public void shouldFailWebSocketConnectionWhenClientSendCloseFrameWithRsv5() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.ping.frame.with.rsv.5/handshake.request.and.frame"
        })
    public void shouldFailWebSocketConnectionWhenClientSendPingFrameWithRsv5() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.pong.frame.with.rsv.5/handshake.request.and.frame"
        })
    public void shouldFailWebSocketConnectionWhenClientSendPongFrameWithRsv5() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.text.frame.with.rsv.6/handshake.request.and.frame"
        })
    public void shouldFailWebSocketConnectionWhenClientSendTextFrameWithRsv6() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.binary.frame.with.rsv.6/handshake.request.and.frame"
        })
    public void shouldFailWebSocketConnectionWhenClientSendBinaryFrameWithRsv6() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.close.frame.with.rsv.6/handshake.request.and.frame"
        })
    public void shouldFailWebSocketConnectionWhenClientSendCloseFrameWithRsv6() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.ping.frame.with.rsv.6/handshake.request.and.frame"
        })
    public void shouldFailWebSocketConnectionWhenClientSendPingFrameWithRsv6() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.pong.frame.with.rsv.6/handshake.request.and.frame"
        })
    public void shouldFailWebSocketConnectionWhenClientSendPongFrameWithRsv6() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.text.frame.with.rsv.7/handshake.request.and.frame"
        })
    public void shouldFailWebSocketConnectionWhenClientSendTextFrameWithRsv7() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.binary.frame.with.rsv.7/handshake.request.and.frame"
        })
    public void shouldFailWebSocketConnectionWhenClientSendBinaryFrameWithRsv7() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.close.frame.with.rsv.7/handshake.request.and.frame"
        })
    public void shouldFailWebSocketConnectionWhenClientSendCloseFrameWithRsv7() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.ping.frame.with.rsv.7/handshake.request.and.frame"
        })
    public void shouldFailWebSocketConnectionWhenClientSendPingFrameWithRsv7() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.pong.frame.with.rsv.7/handshake.request.and.frame"
        })
    public void shouldFailWebSocketConnectionWhenClientSendPongFrameWithRsv7() throws Exception {
        k3po.finish();
    }
}
