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

package org.kaazing.gateway.transport.wseb.logging;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.kaazing.gateway.util.InternalSystemProperty.WSE_SPECIFICATION;
import static org.kaazing.test.util.ITUtil.timeoutRule;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.test.util.ITUtil;
import org.kaazing.test.util.MemoryAppender;
import org.kaazing.test.util.MethodExecutionTrace;

public class WsebAcceptorLoggingIT {

    private final K3poRule k3po = new K3poRule()
            .setScriptRoot("org/kaazing/specification/wse");
    private List<String> expectedPatterns;
    private List<String> forbiddenPatterns;
    private TestRule checkLogMessageRule = new TestRule() {
        @Override
        public Statement apply(final Statement base, Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    base.evaluate();
                    MemoryAppender.assertMessagesLogged(expectedPatterns,
                            forbiddenPatterns, ".*\\[.*#.*].*", true);
                }
            };
        }
    };

    private GatewayRule gateway = new GatewayRule() {
        {
            // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                        .property(WSE_SPECIFICATION.getPropertyName(), "true")
                        .service()
                            .accept(URI.create("ws://localhost:8080/path"))
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
    public final TestRule chain = RuleChain.outerRule(new MethodExecutionTrace()).around(checkLogMessageRule)
            .around(gateway).around(k3po).around(timeoutRule(5, SECONDS));

    @Test
    @Specification({
        "data/binary/echo.payload.length.127/request"
        })
    public void shouldLogOpenWriteReceivedAndAbruptClose() throws Exception {
        k3po.finish();

        expectedPatterns = new ArrayList<String>(Arrays.asList(new String[] {
            "tcp#.*OPENED",
            "tcp#.*WRITE",
            "tcp#.*RECEIVED",
            "tcp#.*CLOSED",
            "http#.*OPENED",
            "http#.*WRITE",
            "http#.*RECEIVED",
            "http#.*EXCEPTION",
            "http#.*CLOSED",
            "wseb#.*OPENED",
            "wseb#.*WRITE",
            "wseb#.*RECEIVED",
            "wseb#.*EXCEPTION",
            "wseb#.*CLOSED"
        }));

        forbiddenPatterns = Collections.emptyList();
    }

    @Test
    @Specification({
        "closing/client.send.close/request"
        })
    public void shouldLogOpenAndCleanClientClose() throws Exception {
        k3po.finish();

        expectedPatterns = new ArrayList<String>(Arrays.asList(new String[] {
            "tcp#.*OPENED",
            "tcp#.*WRITE",
            "tcp#.*RECEIVED",
            "tcp#.*CLOSED",
            "http#.*OPENED",
            "http#.*WRITE",
            "http#.*RECEIVED",
            "http#.*CLOSED",
            "wseb#.*OPENED",
            "wseb#.*CLOSED"
        }));

        forbiddenPatterns = Arrays.asList("#.*EXCEPTION");
    }

}
