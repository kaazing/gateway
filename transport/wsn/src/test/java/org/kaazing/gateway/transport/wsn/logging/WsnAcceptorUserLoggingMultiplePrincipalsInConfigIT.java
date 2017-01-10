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
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.test.util.MemoryAppender;
import org.kaazing.test.util.MethodExecutionTrace;

/**
 * WsnAcceptorUserLoggingMultiplePrincipalsInConfigIT - verifies that if multiple Principals are defined in the config
 * only the first one is used
 *
 * Logging on different layers:
 * - TCP: hostname:port
 * - HTTP: principal hostname:port
 * - WSN: principal hostname:port
 */

public class WsnAcceptorUserLoggingMultiplePrincipalsInConfigIT {
    private static final String ROLE = "USER";
    private static final String DEMO_REALM = "demo";
    private static final String TEST_PRINCIPAL_PASS = "testPrincipalPass";
    private static final String TEST_PRINCIPAL_NAME = "testPrincipalName";
    private List<String> expectedPatterns;
    private List<String> forbiddenPatterns;
    private final K3poRule k3po = new K3poRule();

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
    public GatewayRule gateway = new GatewayRule() {
        {
            // @formatter:off
            GatewayConfiguration configuration = new GatewayConfigurationBuilder()

                .service()
                    .accept("ws://localhost:8001/echoAuth")
                    .type("echo")
                    .realmName(DEMO_REALM)
                        .authorization()
                        .requireRole(ROLE)
                    .done()
                .done()
                .security()
                    .realm()
                        .name(DEMO_REALM)
                        .description("Kaazing WebSocket Gateway Demo")
                        .httpChallengeScheme("Basic")
                        .userPrincipalClass("org.kaazing.gateway.security.auth.config.parse.DefaultUserConfig")
                        .userPrincipalClass("com.sun.security.auth.UnixPrincipal")
                        .loginModule()
                            .type("class:org.kaazing.gateway.transport.wsn.auth.BasicLoginModuleWithMultiplePrincipalsInConfig")
                            .success("requisite")
                            .option("roles", ROLE)
                        .done()
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
    // Special ordering: gateway around k3po allows gateway to detect k3po closing any still open connections
    // to make sure we get the log messages for the abrupt close
    public final TestRule chain = RuleChain.outerRule(new MethodExecutionTrace()).around(checkLogMessageRule)
            .around(gateway).around(k3po).around(timeoutRule);


    @Specification("asyncBasicLoginModuleSuccess")
    @Test
    public void verifyPrincipalNameLoggedWhenMultiplePrincipalsInConfig() throws Exception {
        k3po.finish();
        expectedPatterns = Arrays.asList(
            "tcp#.* [^/]*:\\d*] OPENED",
            "tcp#.* [^/]*:\\d*] WRITE",
            "tcp#.* [^/]*:\\d*] RECEIVED",
            "tcp#.* [^/]*:\\d*] CLOSED",
            "http#[^" + TEST_PRINCIPAL_NAME + "]*" + TEST_PRINCIPAL_NAME + " [^/]*:\\d*] OPENED",
            "http#[^" + TEST_PRINCIPAL_NAME + "]*" + TEST_PRINCIPAL_NAME + " [^/]*:\\d*] CLOSED",
            "wsn#[^" + TEST_PRINCIPAL_NAME + "]*" + TEST_PRINCIPAL_NAME + " [^/]*:\\d*] OPENED",
            "wsn#[^" + TEST_PRINCIPAL_NAME + "]*" + TEST_PRINCIPAL_NAME + " [^/]*:\\d*] WRITE",
            "wsn#[^" + TEST_PRINCIPAL_NAME + "]*" + TEST_PRINCIPAL_NAME + " [^/]*:\\d*] RECEIVED",
            "wsn#[^" + TEST_PRINCIPAL_NAME + "]*" + TEST_PRINCIPAL_NAME + " [^/]*:\\d*] EXCEPTION.*IOException",
            "wsn#[^" + TEST_PRINCIPAL_NAME + "]*" + TEST_PRINCIPAL_NAME + " [^/]*:\\d*] CLOSED"
        );
        forbiddenPatterns = new ArrayList<>(Arrays.asList(new String[]{
                TEST_PRINCIPAL_PASS
        }));
    }

}
