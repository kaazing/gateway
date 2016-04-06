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
package org.kaazing.gateway.transport.wseb;

import static org.junit.rules.RuleChain.outerRule;

import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

public class WsebCookiesIT {

    private final GatewayRule gateway = new GatewayRule() {
        {
            // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                        .service()
                            .accept("wse://localhost:8000/echo")
                            .type("echo")
                            .crossOrigin()
                                .allowOrigin("*")
                            .done()
                        .done()
                .done();
            // @formatter:on
            init(configuration);
        }
    };
    
    private final TestRule timeout = new DisableOnDebug(new Timeout(10, TimeUnit.SECONDS));

    private final K3poRule robot = new K3poRule();
    
    @Rule
    public final TestRule chain = outerRule(robot).around(gateway).around(timeout);

    // httpe should give wrapped response (Flash client)
    @Test
    @Specification("httpe.get.cookies.should.return.none")
    public void httpeGetCookiesShouldReturnNone() throws Exception {
        robot.finish();
    }

    // httpe should give wrapped response (Flash client)
    @Test
    @Specification("httpe.set.cookies.should.return.set.cookie.headers")
    public void httpeSetCookiesShouldReturnSetCookieHeaders() throws Exception {
        robot.finish();
    }
}
