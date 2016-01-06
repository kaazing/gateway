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

import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.util.Enumeration;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.resource.address.ResolutionUtils;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkInterfaceResolutionIT {
    private static final Logger LOG = LoggerFactory.getLogger(ResolutionUtils.class);
    private static String networkInterface = "";
    static {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface resolvedNetworkInterface = interfaces.nextElement();
                if (resolvedNetworkInterface.isLoopback()) {
                    networkInterface = resolvedNetworkInterface.getDisplayName();
                    break;
                }
            }
        } catch (SocketException socketEx) {
            LOG.debug("server", "Unable to resolve device URIs, processing URIs without device resolution.");
        }
        LOG.debug("server", "Unable to resolve device URIs, processing URIs without device resolution.");
    }

    private GatewayRule gateway = new GatewayRule() {
        {
            // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                        .cluster()
                            .accept("tcp://[@" + networkInterface + "]:2345")
                            .connect(URI.create("tcp://localhost:5432"))
                            .name("clusterName")
                        .done()
                        .service()
                            .accept("tcp://localhost:8003")
                                .acceptOption("tcp.bind", "[@" + networkInterface + "]:7082")
                            .connect(URI.create("tcp://localhost:8004"))
                            .type("proxy")
                        .done()
                        .service()
                            .accept(URI.create("http://localhost:8110"))
                                .acceptOption("http.transport", "tcp://[@" + networkInterface + "]:8111")
                            .connect(URI.create("http://localhost:8080"))
                            .type("proxy")
                        .done()
                        .service()
                            .accept("tcp://[@" + networkInterface + "]:8001")
                            .connect(URI.create("tcp://localhost:8002"))
                            .type("proxy")
                            .crossOrigin()
                                .allowOrigin("*")
                            .done()
                        .done()
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
    public void networkInterfaceCluster() throws Exception {
        robot.finish();
    }

    @Specification("network.interface.accept.option.tcp.bind")
    @Test
    public void networkInterfaceAcceptOptionTcpBind() throws Exception {
        robot.finish();
    }

    @Specification("network.interface.accept.option.http.transport")
    @Test
    public void networkInterfaceAcceptOptionHttpTransport() throws Exception {
        robot.finish();
    }
}
