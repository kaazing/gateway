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

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.test.util.MemoryAppender;
import org.kaazing.test.util.MethodExecutionTrace;

public class LimitsMaxSizeDefaultIT {
    private static String WS_ECHO_SERVICE_ACCEPT = "ws://localhost:8080/echo";

    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/ws/limits");

    private GatewayRule gateway = new GatewayRule() {
        {
            // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                        .service()
                            .accept(WS_ECHO_SERVICE_ACCEPT)
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

    private List<String> forbiddenPatterns;
    private TestRule checkLogMessageRule = new TestRule() {
        @Override
        public Statement apply(final Statement base, Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    base.evaluate();
                    MemoryAppender.assertMessagesLogged(Collections.emptyList(),
                            forbiddenPatterns, ".*ProtocolDecoderException.*", true);
                }
            };
        }
    };
    private MethodExecutionTrace trace = new MethodExecutionTrace();
    private TestRule timeoutRule = new DisableOnDebug(Timeout.builder().withTimeout(10, SECONDS)
            .withLookingForStuckThread(true).build());

    @Rule
    public TestRule chain = RuleChain.outerRule(trace).around(checkLogMessageRule).around(gateway).around(k3po)
            .around(timeoutRule);

    @Test
    @Specification({
        "should.fail.binary.payload.length.131073/handshake.request.and.frame"
        })
    public void shouldRefuseBinaryFrameWithPayloadLengthExceeding128KiB() throws Exception {
        k3po.finish();
        // Check we are closing the connection immediately and not attempting to decode subsequent incoming data
        forbiddenPatterns = Arrays.asList("Unknown WebSocket opcode", "RSV1 is set", "RSV2 is set");
    }

    @Test
    @Specification({
        "should.fail.text.payload.length.131073/handshake.request.and.frame"
        })
    public void shouldRefuseTextFrameWithPayloadLengthExceeding128KiB() throws Exception {
        k3po.finish();
        // Check we are closing the connection immediately and not attempting to decode subsequent incoming data
        forbiddenPatterns = Arrays.asList("Unknown WebSocket opcode", "RSV1 is set", "RSV2 is set");
    }

}
