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
 * RFC-6455, section 5.4 "Fragmentation"
 */
public class FragmentationIT {

    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/ws/fragmentation");
    
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

    @Test
    @Specification({
        "client.send.continuation.payload.length.125.not.fragmented/handshake.request.and.frame"
        })
    public void shouldFailWebSocketConnectionWhenClientSendContinuationFrameWithPayloadNotFragmented() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.continuation.payload.length.125.fragmented/handshake.request.and.frames"
        })
    public void shouldFailWebSocketConnectionWhenClientSendContinuationFrameWithPayloadFragmented() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.echo.text.payload.length.125.not.fragmented/handshake.request.and.frame"
        })
    public void shouldEchoClientSendTextFrameWithPayloadNotFragmented() throws Exception {
        k3po.finish();
    }

    @Test
    @Ignore("Gateway disconnected instead of echoing code, client did not read")
    @Specification({
        "client.echo.text.payload.length.0.fragmented/handshake.request.and.frames"
        })
    public void shouldEchoClientSendTextFrameWithEmptyPayloadFragmented() throws Exception {
        k3po.finish();
    }

    @Test
    @Ignore("Gateway writing wrong bytes back to client, read does not match")
    @Specification({
        "client.echo.text.payload.length.0.fragmented.with.injected.ping.pong/handshake.request.and.frames"
        })
    public void shouldEchoClientSendTextFrameWithEmptyPayloadFragmentedAndInjectedPingPong() throws Exception {
        k3po.finish();
    }

    @Test
    @Ignore("Incorrect data write by Gateway, not echoing")
    @Specification({
        "client.echo.text.payload.length.125.fragmented/handshake.request.and.frames"
        })
    public void shouldEchoClientSendTextFrameWithPayloadFragmented() throws Exception {
        k3po.finish();
    }

    @Test
    @Ignore("Incorrect data write by Gateway, not echoing")
    @Specification({
        "client.echo.text.payload.length.125.fragmented.with.some.empty.fragments/handshake.request.and.frames"
        })
    public void shouldEchoClientSendTextFrameWithPayloadFragmentedWithSomeEmptyFragments() throws Exception {
        k3po.finish();
    }

    @Test
    @Ignore("Read value for client differs from expected. Did not echo full payload")
    @Specification({
        "client.echo.text.payload.length.125.fragmented.but.not.utf8.aligned/handshake.request.and.frames"
        })
    public void shouldEchoClientSendTextFrameWithPayloadFragmentedEvenWhenNotUTF8Aligned() throws Exception {
        k3po.finish();
    }

    @Test
    @Ignore("Read value for client differs from expected. Did not echo full payload")
    @Specification({
        "client.echo.text.payload.length.125.fragmented.with.injected.ping.pong/handshake.request.and.frames"
        })
    public void shouldEchoClientSendTextFrameWithPayloadFragmentedAndInjectedPingPong() throws Exception {
        k3po.finish();
    }

    @Test
    @Ignore("Read value for client differs from expected.")
    @Specification({
        "client.send.text.payload.length.125.fragmented.but.not.continued/handshake.request.and.frames"
        })
    public void shouldFailWebSocketConnectionWhenClientSendTextFrameWithPayloadFragmentedButNotContinued() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.echo.binary.payload.length.125.not.fragmented/handshake.request.and.frame"
        })
    public void shouldEchoClientSendBinaryFrameWithPayloadNotFragmented() throws Exception {
        k3po.finish();
    }

    @Test
    @Ignore("disconnected early, not echoing data")
    @Specification({
        "client.echo.binary.payload.length.0.fragmented/handshake.request.and.frames"
        })
    public void shouldEchoClientSendBinaryFrameWithEmptyPayloadFragmented() throws Exception {
        k3po.finish();
    }

    @Test
    @Ignore("Received Pong, but did not receive the data")
    @Specification({
        "client.echo.binary.payload.length.0.fragmented.with.injected.ping.pong/handshake.request.and.frames"
        })
    public void shouldEchoClientSendBinaryFrameWithEmptyPayloadFragmentedAndInjectedPingPong() throws Exception {
        k3po.finish();
    }

    @Test
    @Ignore("Read value for client differs from expected. Did not echo full payload")
    @Specification({
        "client.echo.binary.payload.length.125.fragmented/handshake.request.and.frames"
        })
    public void shouldEchoClientSendBinaryFrameWithPayloadFragmented() throws Exception {
        k3po.finish();
    }

    @Test
    @Ignore("Read value for client differs from expected. Did not echo full payload")
    @Specification({
        "client.echo.binary.payload.length.125.fragmented.with.some.empty.fragments/handshake.request.and.frames"
        })
    public void shouldEchoClientSendBinaryFrameWithPayloadFragmentedWithSomeEmptyFragments() throws Exception {
        k3po.finish();
    }

    @Test
    @Ignore("Read value for client differs from expected. Did not echo full payload")
    @Specification({
        "client.echo.binary.payload.length.125.fragmented.with.injected.ping.pong/handshake.request.and.frames"
        })
    public void shouldEchoClientSendBinaryFrameWithPayloadFragmentedAndInjectedPingPong() throws Exception {
        k3po.finish();
    }

    @Test
    @Ignore("Read value for client differs from expected. Gateway may have written incorrect data")
    @Specification({
        "client.send.binary.payload.length.125.fragmented.but.not.continued/handshake.request.and.frames"
        })
    public void shouldFailWebSocketConnectionWhenClientSendBinaryFrameWithPayloadFragmentedButNotContinued() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.close.payload.length.2.fragmented/handshake.request.and.frames"
        })
    public void shouldFailWebSocketConnectionWhenClientSendCloseFrameWithPayloadFragmented() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.ping.payload.length.0.fragmented/handshake.request.and.frames"
        })
    public void shouldFailWebSocketConnectionWhenClientSendPingFrameWithPayloadFragmented() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.pong.payload.length.0.fragmented/handshake.request.and.frames"
        })
    public void shouldFailWebSocketConnectionWhenClientSendPongFrameWithPayloadFragmented() throws Exception {
        k3po.finish();
    }

/*    
 *  Client-only Tests
 *  
    @Test
    @Specification({
        "server.echo.binary.payload.length.0.fragmented/handshake.request.and.frame",
        "server.echo.binary.payload.length.0.fragmented/handshake.response.and.frames" })
    public void shouldEchoServerSendBinaryFrameWithEmptyPayloadFragmented() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "server.echo.binary.payload.length.0.fragmented.with.injected.ping.pong/handshake.request.and.frame",
        "server.echo.binary.payload.length.0.fragmented.with.injected.ping.pong/handshake.response.and.frames" })
    public void shouldEchoServerSendBinaryFrameWithEmptyPayloadFragmentedAndInjectedPingPong() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "server.echo.binary.payload.length.125.fragmented/handshake.request.and.frame",
        "server.echo.binary.payload.length.125.fragmented/handshake.response.and.frames" })
    public void shouldEchoServerSendBinaryFrameWithPayloadFragmented() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "server.echo.binary.payload.length.125.fragmented.with.injected.ping.pong/handshake.request.and.frame",
        "server.echo.binary.payload.length.125.fragmented.with.injected.ping.pong/handshake.response.and.frames" })
    public void shouldEchoServerSendBinaryFrameWithPayloadFragmentedAndInjectedPingPong() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "server.echo.binary.payload.length.125.fragmented.with.some.empty.fragments/handshake.request.and.frame",
        "server.echo.binary.payload.length.125.fragmented.with.some.empty.fragments/handshake.response.and.frames" })
    public void shouldEchoServerSendBinaryFrameWithPayloadFragmentedWithSomeEmptyFragments() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "server.echo.binary.payload.length.125.not.fragmented/handshake.request.and.frame",
        "server.echo.binary.payload.length.125.not.fragmented/handshake.response.and.frame" })
    public void shouldEchoServerSendBinaryFrameWithPayloadNotFragmented() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "server.echo.text.payload.length.0.fragmented/handshake.request.and.frame",
        "server.echo.text.payload.length.0.fragmented/handshake.response.and.frames" })
    public void shouldEchoServerSendTextFrameWithEmptyPayloadFragmented() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "server.echo.text.payload.length.0.fragmented.with.injected.ping.pong/handshake.request.and.frame",
        "server.echo.text.payload.length.0.fragmented.with.injected.ping.pong/handshake.response.and.frames" })
    public void shouldEchoServerSendTextFrameWithEmptyPayloadFragmentedAndInjectedPingPong() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "server.echo.text.payload.length.125.fragmented/handshake.request.and.frame",
        "server.echo.text.payload.length.125.fragmented/handshake.response.and.frames" })
    public void shouldEchoServerSendTextFrameWithPayloadFragmented() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "server.echo.text.payload.length.125.fragmented.but.not.utf8.aligned/handshake.request.and.frame",
        "server.echo.text.payload.length.125.fragmented.but.not.utf8.aligned/handshake.response.and.frames" })
    public void shouldEchoServerSendTextFrameWithPayloadFragmentedEvenWhenNotUTF8Aligned() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "server.echo.text.payload.length.125.fragmented.with.injected.ping.pong/handshake.request.and.frame",
        "server.echo.text.payload.length.125.fragmented.with.injected.ping.pong/handshake.response.and.frames" })
    public void shouldEchoServerSendTextFrameWithPayloadFragmentedAndInjectedPingPong() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "server.echo.text.payload.length.125.fragmented.with.some.empty.fragments/handshake.request.and.frame",
        "server.echo.text.payload.length.125.fragmented.with.some.empty.fragments/handshake.response.and.frames" })
    public void shouldEchoServerSendTextFrameWithPayloadFragmentedWithSomeEmptyFragments() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "server.echo.text.payload.length.125.not.fragmented/handshake.request.and.frame",
        "server.echo.text.payload.length.125.not.fragmented/handshake.response.and.frame" })
    public void shouldEchoServerSendTextFrameWithPayloadNotFragmented() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "server.send.binary.payload.length.125.fragmented.but.not.continued/handshake.request.and.frame",
        "server.send.binary.payload.length.125.fragmented.but.not.continued/handshake.response.and.frames" })
    public void shouldFailWebSocketConnectionWhenServerSendBinaryFrameWithPayloadFragmentedButNotContinued() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "server.send.close.payload.length.2.fragmented/handshake.request.and.frame",
        "server.send.close.payload.length.2.fragmented/handshake.response.and.frames" })
    public void shouldFailWebSocketConnectionWhenServerSendCloseFrameWithPayloadFragmented() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "server.send.continuation.payload.length.125.fragmented/handshake.request.and.frame",
        "server.send.continuation.payload.length.125.fragmented/handshake.response.and.frames" })
    public void shouldFailWebSocketConnectionWhenServerSendContinuationFrameWithPayloadFragmented() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "server.send.continuation.payload.length.125.not.fragmented/handshake.request.and.frame",
        "server.send.continuation.payload.length.125.not.fragmented/handshake.response.and.frame" })
    public void shouldFailWebSocketConnectionWhenServerSendContinuationFrameWithPayloadNotFragmented() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "server.send.ping.payload.length.0.fragmented/handshake.request.and.frame",
        "server.send.ping.payload.length.0.fragmented/handshake.response.and.frames" })
    public void shouldFailWebSocketConnectionWhenServerSendPingFrameWithPayloadFragmented() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "server.send.pong.payload.length.0.fragmented/handshake.request.and.frame",
        "server.send.pong.payload.length.0.fragmented/handshake.response.and.frames" })
    public void shouldFailWebSocketConnectionWhenServerSendPongFrameWithPayloadFragmented() throws Exception {
        k3po.finish();
    }*/

}