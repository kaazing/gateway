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
package org.kaazing.gateway.transport.wsn.specification.httpx;

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

public class ExtendedHandshakeIT {

    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/httpx/extended");

    private GatewayRule gateway = new GatewayRule() {
        {
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                        .service()
                            .accept("ws://localhost:8080/path")
                            .type("echo")
                            .crossOrigin()
                                .allowOrigin("*")
                            .done()
                            .acceptOption("ws.inactivity.timeout", "123secs")
                        .done()
                        .service()
                            .accept("ws://localhost:8080/auth")
                            .type("echo")
                            .realmName("Kaazing WebSocket Gateway Demo")
                            .crossOrigin()
                                .allowOrigin("*")
                            .done()
                            .authorization()
                                .requireRole("AUTHORIZED")
                            .done()
                            .acceptOption("ws.inactivity.timeout", "123secs")
                        .done()
                        .security()
                            .realm()
                                .name("Kaazing WebSocket Gateway Demo")
                                .description("Kaazing WebSocket Gateway Demo")
                                .httpChallengeScheme("Application Basic")
                                .authorizationMode("challenge")
                                .loginModule()
                                    .type("class:org.kaazing.gateway.security.auth.YesLoginModule")
                                    .success("requisite")
                                    .option("roles", "AUTHORIZED")
                                .done()
                            .done()
                        .done()
                    .done();
            init(configuration);
        }
    };

    @Rule
    public TestRule chain = createRuleChain(gateway, k3po);

    @Test
    @Specification({ "connection.established.with.authorization/request" })
    public void shouldEstablishConnectionWithAuthorization() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({ "connection.established/request" })
    public void shouldEstablishConnection() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({"connection.established.data.exchanged.close/request"})
    public void shouldEstablishConnectionAndExchangeDataAndClose() throws Exception {
        k3po.finish();
    }

    @Test
    @Ignore("https://github.com/kaazing/tickets/issues/434")
    @Specification({ "client.sends.message.between.opening.and.extended.handshake/request" })
    public void shouldFailWhenClientSendsMessageBetweenOpeningAndExtendedHandshake() throws Exception {
        k3po.finish();
    }

    // Client only test
    @Specification({"server.sends.message.between.opening.and.extended.handshake/request"})
    void shouldFailWhenServerSendsMessageBetweenOpeningAndExtendedHandshake() throws Exception {
        k3po.finish();
    }

    @Test
    @Ignore("https://github.com/kaazing/tickets/issues/313")
    @Specification({ "extension.in.opening.and.extended.handshake/request" })
    public void shouldFailWhenExtendedHandshakeHasExtensionFromOpeningHandshake() throws Exception {
        k3po.finish();
    }

    @Test
    @Ignore("https://github.com/kaazing/tickets/issues/312")
    @Specification({ "extension.in.opening.handshake/request" })
    public void shouldPassWhenExtensionIsNegotiatedInOpeningHandshake() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({ "extension.in.extended.handshake/request" })
    public void shouldPassWhenExtensionIsNegotiatedInExtendedHandshake() throws Exception {
        k3po.finish();
    }

    // Client only test
    @Specification({"extended.handshake.response.code.200/request"})
    void shouldFailWhenWebSocketProtocolGets200StatusCode() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({ "extended.handshake.response.code.101/request"})
    public void shouldPassWhenWebSocketProtocolGets101StatusCode() throws Exception {
        k3po.finish();
    }

    // NOTE: shouldPassWhenWebSocketProtocolGets302StatusCode() is defined in gateway.service.http.balancer's
    //       ExtendedHandshakeBalancerIT to avoid circular dependency.

    @Test
    @Specification({ "extended.handshake.response.code.401/request" })
    public void shouldPassWhenWebSocketProtocolGets401StatusCode() throws Exception {
        k3po.finish();
    }
}
