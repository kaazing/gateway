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
package org.kaazing.gateway.transport.wseb;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.RuleChain;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;

public class DuplicateBindTest {

    private final GatewayRule gateway = new GatewayRule() {
        {
            // @formatter:off
            GatewayConfiguration configuration =
                new GatewayConfigurationBuilder()
                    .service()
                        .name("echo1")
                        .type("echo")
                        .accept("wse://localhost:8000/")
                    .done()
                    .service()
                        .name("echo2")
                        .type("echo")
                        .accept("wse://localhost:8000/")
                    .done()
                    .done();
            // @formatter:on
            init(configuration);
        }
    };

    private final ExpectedException thrown = ExpectedException.none();
    {
        thrown.expect(RuntimeException.class);
        thrown.expectMessage(
                "Error binding to ws://localhost:8000/: Tried to bind address [ws://localhost:8000/ (wse://localhost:8000/)]");
    }

    @Rule
    public RuleChain chain = RuleChain.outerRule(thrown).around(gateway);

    @Test
    public void connectingOnService1ShouldNotGetAccessToService2() throws Exception {
    }
}
