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

package org.kaazing.gateway.transport.wsn.specification.ws;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.rules.RuleChain.outerRule;

import java.net.URI;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

/**
 * RFC-6455, section 5.6 "Data Frames"
 */
public class DataFramingIT {

    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/ws/data");

    private GatewayRule gateway = new GatewayRule() {
        {
            // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                        .service()
                            .accept(URI.create("ws://localhost:8080/echo"))
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

    private final TestRule timeout = new DisableOnDebug(new Timeout(5, SECONDS));

    @Rule
    public final TestRule chain = outerRule(k3po).around(gateway).around(timeout);

    // TODO: invalid UTF-8 in text frame (opcode 0x01) RFC-6455, section 8.1

    @Test
    @Specification({
        "client.send.opcode.0x03/handshake.request.and.frame"
        })
    public void shouldFailWebSocketConnectionWhenClientSendOpcode3Frame() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.opcode.0x04/handshake.request.and.frame"
        })
    public void shouldFailWebSocketConnectionWhenClientSendOpcode4Frame() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.opcode.0x05/handshake.request.and.frame"
        })
    public void shouldFailWebSocketConnectionWhenClientSendOpcode5Frame() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.opcode.0x06/handshake.request.and.frame"
        })
    public void shouldFailWebSocketConnectionWhenClientSendOpcode6Frame() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.opcode.0x07/handshake.request.and.frame"
        })
    public void shouldFailWebSocketConnectionWhenClientSendOpcode7Frame() throws Exception {
        k3po.finish();
    }

/*    
 * Client-only Tests
 * 
    @Test
    @Specification({
        "server.send.opcode.0x03/handshake.request.and.frame",
        "server.send.opcode.0x03/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendOpcode3Frame() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "server.send.opcode.0x04/handshake.request.and.frame",
        "server.send.opcode.0x04/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendOpcode4Frame() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "server.send.opcode.0x05/handshake.request.and.frame",
        "server.send.opcode.0x05/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendOpcode5Frame() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "server.send.opcode.0x06/handshake.request.and.frame",
        "server.send.opcode.0x06/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendOpcode6Frame() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "server.send.opcode.0x07/handshake.request.and.frame",
        "server.send.opcode.0x07/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendOpcode7Frame() throws Exception {
        k3po.finish();
    }*/

}