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
package org.kaazing.gateway.transport.wsn.logging;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.kaazing.test.util.ITUtil.timeoutRule;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.test.util.LoggingTestRule;
import org.kaazing.test.util.MethodExecutionTrace;

/**
 * RFC-6455, section 5.2 "Base Framing Protocol"
 */
public class WsxAcceptorLoggingIT {
    private static final String FILTER_PATTERN = ".*\\[.*#.*].*";

    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification");

    private LoggingTestRule checkLogMessageRule = new LoggingTestRule();
    {
        checkLogMessageRule.setFilterPattern(FILTER_PATTERN);
    }

    private GatewayRule gateway = new GatewayRule() {
        {
            // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                        .service()
                            .accept("ws://localhost:8080/path")
                            .type("echo")
                        .done()
                        .service()
                            .accept("ws://localhost:8001/echo")
                            .type("echo")
                            .acceptOption("ws.inactivity.timeout", "1sec")
                        .done()
                    .done();
            // @formatter:on
            init(configuration);
        }
    };

    private TestRule trace = new MethodExecutionTrace(); // trace level logging

    @Rule
    // Special ordering: gateway around k3po allows gateway to detect k3po closing any still open connections
    // to make sure we get the log messages for the abrupt close
    public final TestRule chain = RuleChain.outerRule(trace).around(gateway).around(checkLogMessageRule)
            .around(k3po).around(timeoutRule(5, SECONDS));

    @Test
    @Specification({
        "httpx/extended/connection.established.data.exchanged.close/request"
        })
    public void shouldLogOpenWriteReceivedAndClose() throws Exception {
        k3po.finish();
        checkLogMessageRule.setExpectedPatterns(Arrays.asList(
            "tcp#.*OPENED",
            "tcp#.*WRITE",
            "tcp#.*RECEIVED",
            "tcp#.*CLOSED",
            "http#.*OPENED",
            "http#.*CLOSED",
            "wsn#.*OPENED",
            "wsn#.*WRITE",
            "wsn#.*RECEIVED",
            "wsn#.*CLOSED"
        ));

        checkLogMessageRule.setForbiddenPatterns(Collections.singletonList("#.*EXCEPTION"));
    }

    @Test
    @Specification({
        "ws.extensions/x-kaazing-ping-pong/server.should.timeout.if.client.does.not.respond.to.extended.ping/request"
    })
    public void shouldLogOpenAndInactivityTimeoutClose() throws Exception {
        k3po.start();
        Thread.sleep(2000);
        k3po.finish();
        checkLogMessageRule.setExpectedPatterns(Arrays.asList(
            "tcp#.*OPENED",
            "tcp#.*WRITE",
            "tcp#.*RECEIVED",
            "tcp#.*CLOSED",
            "http#.*OPENED",
            "http#.*CLOSED",
            "wsn#.*OPENED",
            "wsn#.*EXCEPTION.*IOException",
            "wsn#.*CLOSED",
            "wsn#.*] java.io.IOException" // from EchoServiceHandler
        ));
    }
}

