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
 * RFC-6455, section 4.1 "Client-Side Requirements"
 * RFC-6455, section 4.2 "Server-Side Requirements"
 */
public class OpeningHandshakeIT {

    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/ws/opening");

    private GatewayRule gateway = new GatewayRule() {
        {
            // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                        .service()
                            .accept(URI.create("ws://localhost:8080/path"))
                            .type("echo")
                            .crossOrigin()
                                .allowOrigin("*")
                            .done()
                        .done()
                        .service()
                            .accept(URI.create("ws://localhost:8080/preflight"))
                            .type("echo")
                            .crossOrigin()
                                .allowOrigin("*")
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

    // TODO:
    // proxy => HTTP CONNECT w/ optional authorization, auto-configuration via ws://, wss://
    // TLS (not SSL) w/ SNI for wss://

    @Test
    @Specification({
        "connection.established/handshake.request"
        })
    public void shouldEstablishConnection() throws Exception {
        k3po.finish();
    }

    @Test
    @Ignore("This spec test will not work. Preflight cookie request not 'a thing'. Talk to engineers.")
    @Specification({
        "request.header.cookie/handshake.request"
        })
    public void shouldEstablishConnectionWithCookieRequestHeader() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "request.headers.random.case/handshake.request"
        })
    public void shouldEstablishConnectionWithRandomCaseRequestHeaders() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "response.headers.random.case/handshake.request"
        })
    public void shouldEstablishConnectionWithRandomCaseResponseHeaders() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "request.header.origin/handshake.request"
        })
    public void shouldEstablishConnectionWithRequestHeaderOrigin() throws Exception {
        k3po.finish();
    }

    @Test
    @Ignore("Missing HTTP header")
    @Specification({
        "request.header.sec.websocket.protocol/handshake.request"
        })
    public void shouldEstablishConnectionWithRequestHeaderSecWebSocketProtocol() throws Exception {
        k3po.finish();
    }

    @Test
    @Ignore("Missing HTTP header")
    @Specification({
        "request.header.sec.websocket.extensions/handshake.request"
        })
    public void shouldEstablishConnectionWithRequestHeaderSecWebSocketExtensions() throws Exception {
        k3po.finish();
    }

    @Test
    @Ignore("java.lang.InterruptedException")
    @Specification({
        "response.header.sec.websocket.extensions.partial.agreement/handshake.request"
        })
    public void shouldEstablishConnectionWithSomeExtensionsNegotiated() throws Exception {
        k3po.finish();
    }

    @Test
    @Ignore("Internal Error: Script not found")
    @Specification({
        "response.header.sec.websocket.extensions.reordered/handshake.request"
        })
    public void shouldEstablishConnectionWhenOrderOfExtensionsNegotiatedChanged() throws Exception {
        k3po.finish();
    }
    
    // Gateway sending payload
    @Test
    @Ignore("Gateway sending payload with 405 Not Allowed, <html><head></head><body><h1>405 Method Not Allowed</h1></body></html>")
    @Specification({
        "request.method.not.get/handshake.request"
        })
    public void shouldFailHandshakeWhenMethodNotGet() throws Exception {
        k3po.finish();
    }

    @Test
    @Ignore("Did not fail and should")
    @Specification({
        "request.version.not.http.1.1/handshake.request"
        })
    public void shouldFailHandshakeWhenVersionNotHttp11() throws Exception {
        k3po.finish();
    }

    @Test
   @Ignore("Did not fail and should")
    @Specification({
        "request.header.host.missing/handshake.request"
        })
    public void shouldFailHandshakeWhenRequestHeaderHostMissing() throws Exception {
        k3po.finish();
    }

    @Test
    @Ignore("Gateway sending payload with 400, <html><head></head><body><h1>400 Websocket Upgrade Failure</h1></body></html>")
    @Specification({
        "request.header.upgrade.missing/handshake.request"
        })
    public void shouldFailHandshakeWhenRequestHeaderUpgradeMissing() throws Exception {
        k3po.finish();
    }

    @Test
    @Ignore("Gateway sending payload with 404, <html><head></head><body><h1>404 Not Found</h1></body></html>")
    @Specification({
        "request.header.upgrade.not.websocket/handshake.request"
        })
    public void shouldFailHandshakeWhenRequestHeaderUpgradeNotWebSocket() throws Exception {
        k3po.finish();
    }

    @Test
    @Ignore("Gateway sending payload with 400, <html><head></head><body><h1>400 Websocket Upgrade Failure</h1></body></html>")
    @Specification({
        "request.header.connection.missing/handshake.request"
        })
    public void shouldFailHandshakeWhenRequestHeaderConnectionMissing() throws Exception {
        k3po.finish();
    }

    @Test
    @Ignore("Gateway sending payload with 400 Bad Request, <html><head></head><body><h1>400 Bad Request</h1></body></html>")
    @Specification({
        "request.header.connection.not.upgrade/handshake.request"
        })
    public void shouldFailHandshakeWhenRequestHeaderConnectionNotUpgrade() throws Exception {
        k3po.finish();
    }

    @Test
    @Ignore("'disconnected', not 'read closed'")
    @Specification({
        "request.header.sec.websocket.key.missing/handshake.request"})
    public void shouldFailHandshakeWhenRequestHeaderSecWebSocketKeyMissing() throws Exception {
        k3po.finish();
    }

    @Test
    @Ignore("java.lang.InterruptedException")
    @Specification({
        "request.header.sec.websocket.key.not.16bytes.base64/handshake.request"
        })
    public void shouldFailHandshakeWhenRequestHeaderSecWebSocketKeyNot16BytesBase64() throws Exception {
        k3po.finish();
    }

    @Test
    @Ignore("'Missing HTTP header: Sec-WebSocket-Version', instead of 'read header ...'")
    @Specification({
        "request.header.sec.websocket.version.not.13/handshake.request"
        })
    public void shouldFailHandshakeWhenRequestHeaderSecWebSocketVersionNot13() throws Exception {
        k3po.finish();
    }

/*   
 *  Client-only Tests
 *   
    @Test
    @Specification({
        "response.header.connection.not.upgrade/handshake.request",
        "response.header.connection.not.upgrade/handshake.response" })
    public void shouldFailConnectionWhenResponseHeaderConnectionNotUpgrade() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "response.header.connection.missing/handshake.request",
        "response.header.connection.missing/handshake.response" })
    public void shouldFailConnectionWhenResponseHeaderConnectionMissing() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "response.header.upgrade.not.websocket/handshake.request",
        "response.header.upgrade.not.websocket/handshake.response" })
    public void shouldFailConnectionWhenResponseHeaderUpgradeNotWebSocket() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "response.header.upgrade.missing/handshake.request",
        "response.header.upgrade.missing/handshake.response" })
    public void shouldFailConnectionWhenResponseHeaderUpgradeMissing() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "response.header.sec.websocket.accept.not.hashed/handshake.request",
        "response.header.sec.websocket.accept.not.hashed/handshake.response" })
    public void shouldFailConnectionWhenResponseHeaderSecWebSocketAcceptNotHashed() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "response.header.sec.websocket.accept.missing/handshake.request",
        "response.header.sec.websocket.accept.missing/handshake.response" })
    public void shouldFailConnectionWhenResponseHeaderSecWebSocketAcceptMissing() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "response.header.sec.websocket.extensions.not.negotiated/handshake.request",
        "response.header.sec.websocket.extensions.not.negotiated/handshake.response" })
    public void shouldFailConnectionWhenResponseHeaderSecWebSocketExtensionsNotNegotiated() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "response.header.sec.websocket.protocol.not.negotiated/handshake.request",
        "response.header.sec.websocket.protocol.not.negotiated/handshake.response" })
    public void shouldFailConnectionWhenResponseHeaderSecWebSocketProtocolNotNegotiated() throws Exception {
        k3po.finish();
    }
    */

    @Test
    @Specification({
        "multiple.connections.established/handshake.requests"
        })
    public void shouldEstablishMultipleConnections() throws Exception {
        k3po.finish();
    }
}