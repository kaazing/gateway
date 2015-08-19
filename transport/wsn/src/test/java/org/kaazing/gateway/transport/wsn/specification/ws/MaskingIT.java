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

import org.junit.Ignore;
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
 * RFC-6455
 * section 5.1 "Overview"
 * section 5.3 "Client-to-Server Masking"
 */
public class MaskingIT {

    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/ws/masking");

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

/*    
 *  Client-only Tests
 *  
    @Test
    @Specification({
        "server.send.masked.text/handshake.request.and.frame",
        "server.send.masked.text/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendsMaskWithTextFrame() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "server.send.masked.binary/handshake.request.and.frame",
        "server.send.masked.binary/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendsMaskWithBinaryFrame() throws Exception {
        k3po.finish();
    }
*/
    
    @Test
    @Ignore("Read value for client differs from expected.")
    @Specification({
        "send.text.payload.not.masked/handshake.request.and.frame"
        })
    public void shouldFailWebSocketConnectionWhenSendTextFrameNotMasked() throws Exception {
        k3po.finish();
    }

    @Test
    @Ignore("Read value for client differs from expected.")
    @Specification({
        "send.binary.payload.not.masked/handshake.request.and.frame"
        })
    public void shouldFailWebSocketConnectionWhenSendBinaryFrameNotMasked() throws Exception {
        k3po.finish();
    }

}