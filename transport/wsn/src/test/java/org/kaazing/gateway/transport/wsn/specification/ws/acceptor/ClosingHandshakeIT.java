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
 * RFC-6455, section 7 "Closing the Connection"
 */
public class ClosingHandshakeIT {

    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/ws/closing");

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
        "client.send.empty.close.frame/handshake.request.and.frame"})
    public void shouldCompleteCloseHandshakeWhenClientSendEmptyCloseFrame() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.close.frame.with.code.1000/handshake.request.and.frame"})
    public void shouldCompleteCloseHandshakeWhenClientSendCloseFrameWithCode1000() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.close.frame.with.code.1000.and.reason/handshake.request.and.frame"})
    public void shouldCompleteCloseHandshakeWhenClientSendCloseFrameWithCode1000AndReason() throws Exception {
        k3po.finish();
    }

    @Test
    @Ignore("Read code different than expected and did not fail")
    @Specification({
        "client.send.close.frame.with.code.1000.and.invalid.utf8.reason/handshake.request.and.frame"})
    public void shouldFailWebSocketConnectionWhenClientSendCloseFrameWithCode1000AndInvalidUTF8Reason() throws Exception {
        k3po.finish();
    }

    @Test
    @Ignore("Read code off by byte, not matching spec test")
    @Specification({
        "client.send.close.frame.with.code.1001/handshake.request.and.frame"})
    public void shouldCompleteCloseHandshakeWhenClientSendCloseFrameWithCode1001() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.close.frame.with.code.1005/handshake.request.and.frame"})
    public void shouldFailWebSocketConnectionWhenClientSendCloseFrameWithCode1005() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.close.frame.with.code.1006/handshake.request.and.frame"})
    public void shouldFailWebSocketConnectionWhenClientSendCloseFrameWithCode1006() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "client.send.close.frame.with.code.1015/handshake.request.and.frame"})
    public void shouldFailWebSocketConnectionWhenClientSendCloseFrameWithCode1015() throws Exception {
        k3po.finish();
    }
}
