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

import static java.util.concurrent.TimeUnit.SECONDS;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.gateway.util.feature.EarlyAccessFeatures;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.test.util.MethodExecutionTrace;

public class HttpProxyStreamingIT {

    private final K3poRule k3po = new K3poRule();

    private final GatewayRule gateway = new GatewayRule() {
        {
            // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                            .property(EarlyAccessFeatures.HTTP_PROXY_SERVICE.getPropertyName(), "true")
                            .service()
                                .accept("http://localhost:8110")
                                .connect("http://localhost:8080")
                                .type("http.proxy")
                                .connectOption("http.keepalive", "disabled")
                            .done()
                    .done();
            // @formatter:on
            init(configuration);
        }
    };

    TestRule trace = new MethodExecutionTrace();
    TestRule timeoutRule = new DisableOnDebug(Timeout.builder().withTimeout(10, SECONDS)
            .withLookingForStuckThread(true).build());

    @Rule
    public TestRule chain = RuleChain.outerRule(trace).around(gateway).around(k3po).around(timeoutRule);

    @Test
    @Specification("http.proxy.origin.server.response.streaming")
    public void originServerResponseStreaming() throws Exception {

        // Simulates sleep for the robot script
        try(ServerSocket listen = new ServerSocket()) {
            listen.setReuseAddress(true);
            listen.bind(new InetSocketAddress("localhost", 61234));

            // port is bound, start the robot
            k3po.start();

            try (Socket socket = listen.accept()) {
                Thread.sleep(500);
                socket.getOutputStream().write(("WakeUp").getBytes());
            }
        }

        k3po.finish();
    }

    @Test
    @Specification("http.proxy.client.request.streaming")
    public void clientRequestStreaming() throws Exception {
        k3po.finish();
    }

}
