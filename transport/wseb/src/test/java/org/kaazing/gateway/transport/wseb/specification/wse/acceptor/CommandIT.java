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
package org.kaazing.gateway.transport.wseb.specification.wse.acceptor;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.kaazing.gateway.util.InternalSystemProperty.WSE_SPECIFICATION;
import static org.kaazing.test.util.ITUtil.timeoutRule;

import org.junit.Rule;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.test.util.MethodExecutionTrace;

public class CommandIT {

    private K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/wse/control");

    private GatewayRule gateway = new GatewayRule() {
        {
         // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                        .property(WSE_SPECIFICATION.getPropertyName(), "true")
                        .service()
                            .accept("wse://localhost:8080/path")
                            .type("echo")
                        .done()
                    .done();
            // @formatter:on
            init(configuration);
        }
    };

    private final TestRule trace = new MethodExecutionTrace();
    private final TestRule timeoutRule = timeoutRule(5, SECONDS);

    @Rule
    public TestRule chain = RuleChain.outerRule(trace).around(gateway).around(k3po)
            .around(timeoutRule);

    // Client test only
    @Specification("server.send.nop/request")
    void shouldReceiveNop() throws Exception {
        k3po.finish();
    }

    // Client test only
    @Specification("server.send.reconnect/request")
    void shouldReceiveReconnect() throws Exception {
        k3po.finish();
    }

    // Client test only
    @Specification("server.send.close/request")
    void shouldReceiveClose() throws Exception {
        k3po.finish();
    }}
