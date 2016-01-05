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
package org.kaazing.gateway.transport.wsn.networkinterface.resolution;
import static org.kaazing.test.util.ITUtil.createRuleChain;

import java.net.URI;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

public class NetworkInterfaceResolutionIT {
    
    private GatewayRule gateway = new GatewayRule() {
        {
            // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                        .cluster()
                            .accept(URI.create("tcp://localhost:2345"))
                            .connect(URI.create("tcp://localhost:5432"))
                            .name("clusterName")
                        .done()
                        .service()
                            .accept("tcp://[@Software Loopback Interface 1]:7080")
                                .acceptOption("tcp.bind", "[@Software Loopback Interface 1]:7082")
                                .acceptOption("http.transport", "tcp//[@Software Loopback Interface 1]:7081")
                            .connect(URI.create("tcp://localhost:7083"))
                            .type("proxy")
                        .done()
                        .service()
                            .accept(URI.create("ws://localhost:8555/echo"))
                            .type("echo")
                            .crossOrigin()
                                .allowOrigin("http://localhost:8001")
                            .done()
                        .done()
                        .service()
                            .accept(URI.create("tcp://localhost:8001"))
                            .connect(URI.create("tcp://localhost:8002"))
                            .type("proxy")
                            .crossOrigin()
                                .allowOrigin("*")
                            .done()
                        .done()
//                        .service()
//                            .accept("tcp://[@Software Loopback Interface 1]:8001")
//                            .connect(URI.create("tcp://localhost:8002"))
//                            .type("proxy")
//                            .crossOrigin()
//                                .allowOrigin("*")
//                            .done()
//                        .done()
                    .done();
            // @formatter:on
            init(configuration);
        }
    };

    private K3poRule robot = new K3poRule();

    @Rule
    public TestRule chain = createRuleChain(gateway, robot);

    @Specification("network.interface.accept")
    @Test
    public void networkInterfaceAccept() throws Exception {
        robot.finish();
    }

    @Specification("network.interface.cluster")
    @Test
    @Ignore
    public void networkInterfaceCluster() throws Exception {
        robot.finish();
    }

    @Specification("network.interface.accept.option.tcp.bind")
    @Test
    @Ignore
    public void networkInterfaceAcceptOptionTcpBind() throws Exception {
        robot.finish();
    }

    @Specification("network.interface.accept.option.http.transport")
    @Test
    @Ignore
    public void networkInterfaceAcceptOptionHttpTransport() throws Exception {
        robot.finish();
    }
}
