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

import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runners.model.Statement;
import org.kaazing.gateway.server.test.Gateway;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.test.util.MemoryAppender;
import org.kaazing.test.util.MethodExecutionTrace;

/**
 * Unit tests for resolving gateway-config.xml.
 */
public class WsnAcceptConnectOptionsIT {

    private List<String> expectedPatterns;

    private TestRule checkLogMessageRule = (base, description) -> new Statement() {
        @Override
        public void evaluate() throws Throwable {
            base.evaluate();
            MemoryAppender.assertMessagesLogged(expectedPatterns, null, null, true);
        }
    };

    private TestRule trace = new MethodExecutionTrace();

    @Rule
    public final TestRule chain = RuleChain.outerRule(trace).around(checkLogMessageRule);

    @Test
    public void testAcceptConnectOptions() throws Exception {
        //@formatter:off
        GatewayConfiguration gc = new GatewayConfigurationBuilder()
            .service()
                .type("proxy")
                .name("proxy")
                .accept("ws://localhost:8001/echo")
                .connect("ws://localhost:8002/echo")
                .acceptOption("http.keepalive.timeout", "4 seconds")
                .acceptOption("ws.inactivity.timeout", "30 seconds")
                .connectOption("http.keepalive.timeout", "5 seconds")
                .connectOption("ws.inactivity.timeout", "30 seconds")
            .done()
        .done();
        Gateway gateway = new Gateway();
        gateway.start(gc);
        gateway.stop();
        //@formatter:on

        expectedPatterns = Arrays.asList(
            "http.keepalive.timeout=4 seconds should be greater-than-or-equal-to ws.inactivity.timeout=30 seconds in accept-options",
            "http.keepalive.timeout=5 seconds should be greater-than-or-equal-to ws.inactivity.timeout=30 seconds in connect-options"
        );
    }
}
