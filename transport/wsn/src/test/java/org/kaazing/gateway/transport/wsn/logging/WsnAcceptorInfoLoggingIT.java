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
import java.util.List;
import java.util.Properties;

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
import org.kaazing.test.util.MemoryAppender;
import org.kaazing.test.util.MethodExecutionTrace;

/**
 * RFC-6455, section 5.2 "Base Framing Protocol"
 */
public class WsnAcceptorInfoLoggingIT {
    private List<String> expectedPatterns;
    private List<String> forbiddenPatterns;
    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification");
    private TestRule checkLogMessageRule = new TestRule() {
        @Override
        public Statement apply(final Statement base, Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    base.evaluate();
                    MemoryAppender.assertMessagesLogged(expectedPatterns, forbiddenPatterns, ".*\\[.*#.*].*", true);
                }
            };
        }
    };

    private GatewayRule gateway = new GatewayRule() {
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

    Properties log4j;
    { log4j = new Properties();
        log4j.setProperty("log4j.rootLogger", "INFO, A1");
        log4j.setProperty("log4j.appender.A1", "org.kaazing.test.util.MemoryAppender");
        log4j.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
        log4j.setProperty("log4j.appender.A1.layout.ConversionPattern", "%-4r %c [%t] %-5p %c{1} %x - %m%n");
    }

    private TestRule trace = new MethodExecutionTrace(log4j); // info level logging

    @Rule
    // Special ordering: gateway around k3po allows gateway to detect k3po closing any still open connections
    // to make sure we get the log messages for the abrupt close
    public final TestRule chain = RuleChain.outerRule(trace).around(checkLogMessageRule)
            .around(gateway).around(k3po).around(timeoutRule(5, SECONDS));

    @Test
    @Specification({
        "ws/extensibility/client.send.text.frame.with.rsv.1/handshake.request.and.frame"
        })
    public void shouldLogProtocolException() throws Exception {
        k3po.finish();
        expectedPatterns = Arrays.asList(
            "tcp#.*OPENED",
            "tcp#.*CLOSED",
            "http#.*OPENED",
            "http#.*CLOSED",
            "wsn#.*OPENED",
            "tcp#.*EXCEPTION.*Protocol.*Exception",
            "wsn#.*EXCEPTION.*IOException.*caused by.*Protocol.*Exception",
            "wsn#.*CLOSED"
        );
        forbiddenPatterns = null;
    }

    @Test
    @Specification({
        "ws/framing/echo.binary.payload.length.125/handshake.request.and.frame"
        })
    public void shouldLogOpenWriteReceivedAndAbruptClose() throws Exception {
        k3po.finish();
        expectedPatterns = Arrays.asList(
            "tcp#.* [^/]*:\\d*] OPENED", // example: [tcp#34 192.168.4.126:49966] OPENED: (...
            "tcp#.* [^/]*:\\d*] CLOSED",
            "http#.* [^/]*:\\d*] OPENED",
            "http#.* [^/]*:\\d*] CLOSED",
            "wsn#.* [^/]*:\\d*] OPENED",
            "wsn#.* [^/]*:\\d*] EXCEPTION.*IOException",
            "wsn#.* [^/]*:\\d*] CLOSED"
        );
        forbiddenPatterns = null;
    }

    @Test
    @Specification({
        "ws/closing/client.send.empty.close.frame/handshake.request.and.frame"
        })
    public void shouldLogOpenAndCleanClose() throws Exception {
        k3po.finish();
        expectedPatterns = Arrays.asList(
            "tcp#.* [^/]*:\\d*] OPENED",
            "tcp#.* [^/]*:\\d*] CLOSED",
            "http#.* [^/]*:\\d*] OPENED",
            "http#.* [^/]*:\\d*] CLOSED",
            "wsn#.* [^/]*:\\d*] OPENED",
            "wsn#.* [^/]*:\\d*] CLOSED"
        );
        forbiddenPatterns = Collections.singletonList("#.*EXCEPTION");
    }

}