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
package org.kaazing.gateway.service.http.balancer;


import static org.junit.rules.RuleChain.outerRule;

import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
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
import org.kaazing.test.util.MethodExecutionTrace;
@Ignore // https://github.com/k3po/k3po/issues/297
public class HttpBalancerIT {

    private final K3poRule robot = new K3poRule();

    public TestRule timeout = new DisableOnDebug(new Timeout(10, TimeUnit.SECONDS));
    public MethodExecutionTrace testExecutionTrace = new MethodExecutionTrace();

    public GatewayRule gateway = new GatewayRule() {
        {
            GatewayConfiguration configuration = new GatewayConfigurationBuilder()
                    // balancer service to echo
                    .service()
                        .type("balancer")
                        .accept("ws://localhost:8001/echo")
                        .crossOrigin()
                            .allowOrigin("*")
                        .done()
                    .done()
                    // echo service
                    .service()
                        .type("echo")
                        .accept("ws://localhost:8001/echo1")
                        .balance("ws://localhost:8001/echo")
                        .crossOrigin()
                            .allowOrigin("*")
                        .done()
                    .done()
            .done();

            init(configuration);
        }
    };


    @Rule
    public TestRule chain = outerRule(robot).around(gateway).around(timeout).around(testExecutionTrace);

    @Test
    @Specification("http.balancer.request.server.initiated.close")
    public void serverInitiatedClose() throws Exception {
        robot.finish();
    }

    @Test
    @Specification("http.balancer.request.client.initiated.close")
    public void clientInitiatedClose() throws Exception {
        robot.finish();
    }

}
