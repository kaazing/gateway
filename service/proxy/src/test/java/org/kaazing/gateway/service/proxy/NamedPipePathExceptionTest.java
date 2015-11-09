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

package org.kaazing.gateway.service.proxy;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.net.URI;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.gateway.resource.address.pipe.NamedPipePathException;
import org.kaazing.gateway.server.test.Gateway;
import org.kaazing.test.util.MethodExecutionTrace;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;

public class NamedPipePathExceptionTest {

    private TestRule timeoutRule = new DisableOnDebug(new Timeout(10, SECONDS));

    @Rule
    public TestRule chain = RuleChain.outerRule(new MethodExecutionTrace()).around(timeoutRule);

    @Test(expected = NamedPipePathException.class)
    public void shouldThrowExceptionOnStartupWhenPipeURLWithPathIsUsed() throws Exception {
        Gateway gatewayPipePath = new Gateway();
        // @formatter:off
               GatewayConfiguration configuration =
                       new GatewayConfigurationBuilder()
                           .service()
                               .name("ServiceWithPipe")
                               .accept(URI.create("pipe://customera/app1"))
                               .connect(URI.create("http://localhost:8080/"))
                               .type("proxy")
                            .done()
                       .done();
        // @formatter:on
        try {
            gatewayPipePath.start(configuration);
        } finally {
            gatewayPipePath.stop();  
        }

    }

}
