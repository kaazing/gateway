/**
 * Copyright 2007-2015, Kaazing Corporation. All rights reserved.
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

import java.net.URI;
import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.nio.internal.NioSocketAcceptor;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

public class LimitsIT {
    private static String WS_ECHO_SERVICE_ACCEPT_LIMIT = "ws://localhost:8080/echo";
    private static String WS_PROXY_SERVICE_ACCEPT_LIMIT = "ws://localhost:8010/proxy";
    private static int PROXY_SERVICE_CONNECT_PORT_LIMIT = 8123;
    private static String PROXY_SERVICE_CONNECT_LIMIT = "tcp://localhost:" + PROXY_SERVICE_CONNECT_PORT_LIMIT;

    private static String WS_ECHO_SERVICE_ACCEPT_DEFAULT = "ws://localhost:8081/echo";
    private static String WS_PROXY_SERVICE_ACCEPT_DEFAULT = "ws://localhost:8011/proxy";
    private static int PROXY_SERVICE_CONNECT_PORT_DEFAULT = 8124;
    private static String PROXY_SERVICE_CONNECT_DEFAULT = "tcp://localhost:" + PROXY_SERVICE_CONNECT_PORT_DEFAULT;
    
    
    private static int DEFAULT_WEBSOCKET_MAXIMUM_MESSAGE_SIZE = 128*1024;
    private static int LIMITED_WEBSOCKET_MAXIMUM_MESSAGE_SIZE_125 = 125;
    public static final long TEST_LIFETIME = TimeUnit.SECONDS.toSeconds(3);

    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/ws/limits");

    private GatewayRule gateway = new GatewayRule() {
        {
            // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                        .service()
                            .accept(URI.create(WS_PROXY_SERVICE_ACCEPT_LIMIT))
                            .connect(URI.create(PROXY_SERVICE_CONNECT_LIMIT))
                            .type("proxy")
                            .acceptOption("ws.maximum.message.size", Integer.toString(LIMITED_WEBSOCKET_MAXIMUM_MESSAGE_SIZE_125))
                        .done()
                        .service()
                            .accept(URI.create(WS_ECHO_SERVICE_ACCEPT_LIMIT))
                            .type("echo")
                            .crossOrigin()
                                .allowOrigin("*")
                            .done()
                            .acceptOption("ws.maximum.message.size", Integer.toString(LIMITED_WEBSOCKET_MAXIMUM_MESSAGE_SIZE_125))
                        .done()

                        .service()
                            .accept(URI.create(WS_PROXY_SERVICE_ACCEPT_DEFAULT))
                            .connect(URI.create(PROXY_SERVICE_CONNECT_DEFAULT))
                            .type("proxy")
                            .acceptOption("ws.maximum.message.size", Integer.toString(DEFAULT_WEBSOCKET_MAXIMUM_MESSAGE_SIZE))
                        .done()
                        .service()
                            .accept(URI.create(WS_ECHO_SERVICE_ACCEPT_DEFAULT))
                            .type("echo")
                            .crossOrigin()
                                .allowOrigin("*")
                            .done()
                            .acceptOption("ws.maximum.message.size", Integer.toString(DEFAULT_WEBSOCKET_MAXIMUM_MESSAGE_SIZE))
                        .done()
                    
                        .service()
                            .accept(URI.create("ws://localhost:8001/echoAuth"))
                            .type("echo")
                            .realmName("demo")
                                .authorization()
                                .requireRole("USER")
                            .done()
                        .done()
                        .security()
                            .realm()
                                .name("demo")
                                .description("Kaazing WebSocket Gateway Demo")
                                .httpChallengeScheme("Basic")
                                .sessionTimeout(String.valueOf(TEST_LIFETIME))
                                .loginModule()
                                    .type("class:org.kaazing.gateway.transport.wsn.auth.AsyncBasicLoginModule")
                                    .success("requisite")
                                    .option("roles", "USER")
                                .done()
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
        "should.fail.binary.payload.length.126/handshake.request.and.frame"
        })
    public void shouldRefuseBinaryFrameWithPayloadLengthExceeding125() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "should.fail.binary.payload.length.131073/handshake.request.and.frame"
        })
    public void shouldRefuseBinaryFrameWithPayloadLengthExceeding128KiB() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "should.fail.text.payload.length.126/handshake.request.and.frame"
        })
    public void shouldRefuseTextFrameWithPayloadLengthExceeding125() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "should.fail.text.payload.length.131073/handshake.request.and.frame"
        })
    public void shouldRefuseTextFrameWithPayloadLengthExceeding128KiB() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
        "should.fail.max.lifetime.exceeded/handshake.request.and.frame"
        })
    public void shouldFailSendingMessageAfterSessionLifetimeIsUp() throws Exception {
        k3po.start();
        k3po.awaitBarrier("HANDSHAKE_COMPLETE");
        Thread.sleep(3000);
        k3po.awaitBarrier("CLOSE_RECEIVED");
        k3po.finish();
    }

}
