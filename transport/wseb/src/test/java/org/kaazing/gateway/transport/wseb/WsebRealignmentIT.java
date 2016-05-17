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

/**
 * This test reproduces issue https://github.com/kaazing/gateway/issues/421 with multiple data packets being
 * read during wseb upstream realignment. We have not found a way to reproduce it using the wse specification
 * scripts it is very specific to the particular implementation of the wseb and http transports. Both test
 * methods need to be present in order to see the bug, although it is only concurrentConnections that fails.
 */
public class WsebRealignmentIT {

    private final K3poRule k3po = new K3poRule().setScriptRoot("./");

    private final TestRule timeout = new DisableOnDebug(new Timeout(15, TimeUnit.SECONDS));

    private final GatewayRule gateway1 = new GatewayRule() {
        {
            GatewayConfiguration configuration = new GatewayConfigurationBuilder()
                    //.property(WSE_SPECIFICATION.getPropertyName(), "true")
                    .service()
                        .type("proxy")
                        .accept("tcp://localhost:8080")
                        .connect("wse://localhost:8081/app1")
                        .connectOption("ws.inactivity.timeout", "1m")
                        .crossOrigin()
                            .allowOrigin("*")
                        .done()
                    .done()
            .done();

            init(configuration);
        }
    };

    private final GatewayRule gateway2 = new GatewayRule() {
        {
            GatewayConfiguration configuration = new GatewayConfigurationBuilder()
                    //.property(WSE_SPECIFICATION.getPropertyName(), "true")
                    .service()
                        .type("proxy")
                        .accept("wse://localhost:8081/app1")
                        .connect("tcp://localhost:3101")
                        .acceptOption("ws.inactivity.timeout", "1m")
                    .done()
            .done();

            init(configuration);
        }
    };

    private TestRule trace = new MethodExecutionTrace();

    @Rule
    public final TestRule chain = outerRule(trace).around(k3po).around(timeout).around(gateway1).around(gateway2);

    @Test
    @Specification({
            "org/kaazing/specification/tcp/rfc793/server.close/client",
            "org/kaazing/gateway/transport/wseb/realignment/tcp.server.close.server" })
    // This test needs to be present to reproduce the issue
    public void serverClose() throws Exception {
        k3po.finish();
    }

    @Test
    @Specification({
            "org/kaazing/specification/tcp/rfc793/concurrent.connections/client",
            "org/kaazing/gateway/transport/wseb/realignment/tcp.concurrent.connections.server" })
    public void concurrentConnections() throws Exception {
        k3po.finish();
    }
}

