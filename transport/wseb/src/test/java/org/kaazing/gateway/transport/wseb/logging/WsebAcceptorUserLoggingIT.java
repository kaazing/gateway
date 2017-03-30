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
package org.kaazing.gateway.transport.wseb.logging;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.kaazing.gateway.util.InternalSystemProperty.WSE_SPECIFICATION;

import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.test.util.LoggingTestRule;
import org.kaazing.test.util.MethodExecutionTrace;

/**
 * WsebAcceptorUserLoggingIT - verifies that the principal name displayed in the principal class is logged accordingly
 * Logging on different layers:
 * - TCP: hostname:port
 * - HTTP: principal hostname:port
 * - WSEB: principal hostname:port
 */
public class WsebAcceptorUserLoggingIT {
    private static final String ROLE = "USER";
    private static final String DEMO_REALM = "demo";
    private static final String TEST_PRINCIPAL_PASS = "testPrincipalPass";
    private static final String TEST_PRINCIPAL_NAME = "testPrincipalName";
    private static final String FILTER_PATTERN = ".*\\[.*#.*].*";

    private final K3poRule k3po = new K3poRule();
    private LoggingTestRule checkLogMessageRule = new LoggingTestRule();
    {
        checkLogMessageRule.setFilterPattern(FILTER_PATTERN);
    }

    public GatewayRule gateway = new GatewayRule() {
        {
            GatewayConfiguration configuration = new GatewayConfigurationBuilder()
                .property(WSE_SPECIFICATION.getPropertyName(), "true")
                .service()
                    .accept("ws://localhost:8080/path")
                    .type("echo")
                    .crossOrigin()
                        .allowOrigin("http://localhost:8001")
                    .done()
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
                        .loginModule()
                            .type("class:org.kaazing.gateway.transport.wseb.logging.loginmodule.BasicLoginModuleWithDefaultUserConfig")
                            .success("requisite")
                            .option("roles", ROLE)
                        .done()
                    .done()
                .done()
            .done();
            // @formatter:on

            init(configuration);
        }
    };

    private TestRule timeoutRule = new DisableOnDebug(new Timeout(10, SECONDS));

    @Rule
    // Special ordering: gateway around k3po allows gateway to detect k3po closing any still open connections
    // to make sure we get the log messages for the abrupt close
    public final TestRule chain = RuleChain.outerRule(new MethodExecutionTrace()).around(gateway).around(checkLogMessageRule)
            .around(k3po).around(timeoutRule);

    @Specification("echo.payload.length.127.with.basic.auth")
    @Test
    public void verifyPrincipalNameLoggedInLayersAboveHttp() throws Exception {
        k3po.finish();
        checkLogMessageRule.setExpectedPatterns(Arrays.asList(
            "tcp#.* [^/]*:\\d*] OPENED",
            "tcp#.* [^/]*:\\d*] WRITE",
            "tcp#.* [^/]*:\\d*] RECEIVED",
            "tcp#.* [^/]*:\\d*] CLOSED",
            "http#[^" + TEST_PRINCIPAL_NAME + "]*" + TEST_PRINCIPAL_NAME + " [^/]*:\\d*] OPENED",
            "http#[^" + TEST_PRINCIPAL_NAME + "]*" + TEST_PRINCIPAL_NAME + " [^/]*:\\d*] CLOSED",
            "http#[^wseb#]*wseb#[^ ]* [^/]*:\\d*] OPENED",
            "http#[^wseb#]*wseb#[^ ]* [^/]*:\\d*] CLOSED",
            "wseb#[^" + TEST_PRINCIPAL_NAME + "]*" + TEST_PRINCIPAL_NAME + " [^/]*:\\d*] OPENED",
            "wseb#[^" + TEST_PRINCIPAL_NAME + "]*" + TEST_PRINCIPAL_NAME + " [^/]*:\\d*] WRITE",
            "wseb#[^" + TEST_PRINCIPAL_NAME + "]*" + TEST_PRINCIPAL_NAME + " [^/]*:\\d*] RECEIVED",
            "wseb#[^" + TEST_PRINCIPAL_NAME + "]*" + TEST_PRINCIPAL_NAME + " [^/]*:\\d*] CLOSED"
        ));
        checkLogMessageRule.setForbiddenPatterns(Arrays.asList(
                TEST_PRINCIPAL_PASS
        ));
    }

}
