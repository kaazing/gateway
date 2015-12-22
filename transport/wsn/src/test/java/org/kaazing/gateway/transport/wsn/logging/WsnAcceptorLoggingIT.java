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

package org.kaazing.gateway.transport.wsn.logging;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.test.util.MemoryAppender;
import org.kaazing.test.util.MethodExecutionTrace;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;

/**
 * RFC-6455, section 5.2 "Base Framing Protocol"
 */
public class WsnAcceptorLoggingIT {
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
                            .accept(URI.create("ws://localhost:8080/echo"))
                            .type("echo")
                            .crossOrigin()
                                .allowOrigin("http://localhost:8001")
                            .done()
                        .done()
                    .done();
            // @formatter:on
            Properties log4j = new Properties();
            log4j.setProperty("log4j.rootLogger", "TRACE, A1");
            log4j.setProperty("log4j.appender.A1", "org.kaazing.test.util.MemoryAppender");
            log4j.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
            log4j.setProperty("log4j.appender.A1.layout.ConversionPattern", "%-4r %c [%t] %-5p %c{1} %x - %m%n");
            PropertyConfigurator.configure(log4j);

            init(configuration);
        }
    };
    
    private TestRule timeoutRule = new DisableOnDebug(new Timeout(10, SECONDS));

    @Rule
    public TestRule chain = RuleChain.outerRule(new MethodExecutionTrace()).around(k3po).around(timeoutRule).around(checkLogMessageRule).around(gateway);

    @Test
    @Specification({
        "ws/framing/echo.binary.payload.length.125/handshake.request.and.frame"
        })
    public void shouldLogOpenWriteReceivedAndAbruptClose() throws Exception {
        k3po.finish();
        expectedPatterns = new ArrayList<String>(Arrays.asList(new String[] {
            "tcp#.* [^/]*:\\d*] OPENED", // example: [tcp#34 192.168.4.126:49966] OPENED: (...
            "tcp#.* [^/]*:\\d*] WRITE",
            "tcp#.* [^/]*:\\d*] RECEIVED",
            "tcp#.* [^/]*:\\d*] CLOSED",
            "http#.* [^/]*:\\d*] OPENED",
            "http#.* [^/]*:\\d*] CLOSED",
            "wsn#.* [^/]*:\\d*] OPENED",
            "wsn#.* [^/]*:\\d*] WRITE",
            "wsn#.* [^/]*:\\d*] RECEIVED",
            "wsn#.* [^/]*:\\d*] EXCEPTION.*IOException"
        }));
        forbiddenPatterns = null;
    }

    @Test
    @Specification({
        "ws/closing/client.send.empty.close.frame/handshake.request.and.frame"
        })
    public void shouldLogOpenAndCleanClose() throws Exception {
        k3po.finish();
        expectedPatterns = new ArrayList<String>(Arrays.asList(new String[] {
            "tcp#.* [^/]*:\\d*] OPENED",
            "tcp#.* [^/]*:\\d*] WRITE",
            "tcp#.* [^/]*:\\d*] RECEIVED",
            "tcp#.* [^/]*:\\d*] CLOSED",
            "http#.* [^/]*:\\d*] OPENED",
            "http#.* [^/]*:\\d*] CLOSED",
            "wsn#.* [^/]*:\\d*] OPENED",
            "wsn#.* [^/]*:\\d*] CLOSED"
        }));
        forbiddenPatterns = Arrays.asList("#.*EXCEPTION");
    }

}