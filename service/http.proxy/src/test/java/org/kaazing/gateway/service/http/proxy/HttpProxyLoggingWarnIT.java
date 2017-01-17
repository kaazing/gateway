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

package org.kaazing.gateway.service.http.proxy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.model.Statement;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.gateway.util.feature.EarlyAccessFeatures;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.test.util.MemoryAppender;
import org.kaazing.test.util.MethodExecutionTrace;

@RunWith(Parameterized.class)
public class HttpProxyLoggingWarnIT {

    private final K3poRule robot = new K3poRule();
    private List<String> expectedPatterns;
    private final String serviceName;
    private final String expectedMessage;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"TEST", "http.proxy service TEST received an HTTP 1.0 request. HTTP 1.0 is not explicitly supported."},
                {null, "http.proxy service received an HTTP 1.0 request. HTTP 1.0 is not explicitly supported."}});
    }

    public HttpProxyLoggingWarnIT(String serviceName, String expectedMessage) {
        this.serviceName = serviceName;
        this.expectedMessage = expectedMessage;
        this.chain = RuleChain.outerRule(new MethodExecutionTrace()).around(robot).around(checkLogMessageRule).around(getGatewayRule());
    }

    private TestRule checkLogMessageRule = (base, description) -> new Statement() {
        @Override
        public void evaluate() throws Throwable {
            base.evaluate();
            MemoryAppender.assertMessagesLogged(expectedPatterns, null, null, true);
        }
    };

    private GatewayRule getGatewayRule() {
        return new GatewayRule() {
            {
            // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                        .property(EarlyAccessFeatures.HTTP_PROXY_SERVICE.getPropertyName(), "true")
                        .service()
                            .name(serviceName)
                            .accept("http://localhost:8110")
                            .connect("http://localhost:8080")
                            .type("http.proxy")
                        .done()
                    .done();
            // @formatter:on
            init(configuration);
            }
        };
    }

    @Rule
    public TestRule chain;

    @Specification("http.proxy.http.1.0.request")
    @Test
    public void sendHttp_1_0_Request() throws Exception {
        robot.finish();
        expectedPatterns = new ArrayList<String>(Arrays.asList(new String[] {
                expectedMessage
        }));
    }
}
