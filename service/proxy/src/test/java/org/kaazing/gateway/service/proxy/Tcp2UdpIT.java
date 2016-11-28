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
package org.kaazing.gateway.service.proxy;

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
import org.kaazing.test.util.MethodExecutionTrace;

public class Tcp2UdpIT {

    private final K3poRule k3po = new K3poRule().setScriptRoot("./");

    private final TestRule timeout = new DisableOnDebug(new Timeout(10, TimeUnit.SECONDS));

    private final GatewayRule gateway = new GatewayRule() {
        {
            GatewayConfiguration configuration = new GatewayConfigurationBuilder()
                    .service()
                        .type("proxy")
                        .accept("tcp://localhost:8080")
                        .connect("udp://localhost:8080")
                    .done()
            .done();

            init(configuration);
        }
    };

    private final TestRule trace = new MethodExecutionTrace();

    @Rule
    public final TestRule chain = outerRule(trace).around(k3po).around(gateway).around(timeout);

    @Test
    @Specification({
            "org/kaazing/specification/tcp/rfc793/echo.data/client",
            "org/kaazing/specification/udp/rfc768/echo.data/server"
    })
    public void bidirectionalData() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
            "org/kaazing/specification/tcp/rfc793/concurrent.connections/client",
            "org/kaazing/specification/udp/rfc768/concurrent.connections/server"
     })
    public void concurrentConnections() throws Exception {
        k3po.finish();
    }

}
