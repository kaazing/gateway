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
package org.kaazing.gateway.service.http.directory;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.GatewayClusterRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.test.util.ResolutionTestUtils;

import java.io.File;
import java.util.concurrent.TimeUnit;

import static org.kaazing.test.util.ITUtil.createRuleChain;


public class ExpiringStateIT {



    private final K3poRule k3po = new K3poRule();

//    private static String networkInterface = ResolutionTestUtils.getLoopbackInterface();

    private GatewayClusterRule gwRule = new GatewayClusterRule() {
        {

            String clusterMember1URI = "http://localhost:8081";
            String clusterMember2URI = "http://localhost:9081";
            String clusterMember1URIcl = "tcp://localhost:5941";
            String clusterMember2URIcl = "tcp://localhost:5942";


            GatewayConfiguration config1 = createGatewayConfigBuilder(clusterMember1URIcl, clusterMember2URIcl, clusterMember1URI, "8011", "8012");
            GatewayConfiguration config2 = createGatewayConfigBuilder(clusterMember2URIcl, clusterMember1URIcl, clusterMember2URI, "9011", "9012");

            init(config1, config2);
//            init(config2);
        }
    };


    private GatewayConfiguration createGatewayConfigBuilder(String acceptClusterLink,
                                                            String connectClusterLink,
                                                            String acceptMemberLink,
                                                            String wsbindPort,
                                                            String servicePort) {
        String balancerURI1 = "http://localhost:9090/index.html";
        return new GatewayConfigurationBuilder()
                // @formatter:off
                .property("login.module.expiring.state", "true")
                .webRootDirectory(new File("src/test/webapp"))
                .cluster()
                  .accept(acceptClusterLink)
                  .connect(connectClusterLink)
                  .name("clusterName")
                .done()
                .service()
                .type("balancer")
                .accept(balancerURI1)
                .acceptOption("ws.bind", wsbindPort)
                .done()
//                .service()
//                .type("echo")
//                .accept("tcp://localhost"+ servicePort)
//                .accept(acceptMemberLink)
//                  .property("directory", "/public")
//                  .property("welcome-file", "index.html")
//                .realmName("demo")
//                .authorization()
//                .requireRole("AUTHORIZED")
//                .done()
//                .balance(balancerURI1)
//                .done()
                .service()
                  .type("directory")
                  .accept("http://localhost:"+ servicePort)
//                  .accept(acceptMemberLink)
                  .balance(balancerURI1)
                    .property("directory", "/public")
                      .crossOrigin().allowOrigin("*")
                    .done()
                    .property("welcome-file", "index.html")
                  .realmName("demo")
                    .authorization()
                      .requireRole("AUTHORIZED")
                    .done()
                .done()
                .security()
                  .realm()
                    .name("demo")
                    .description("Kaazing WebSocket Gateway Demo")
                    .httpChallengeScheme("Application Token")
                    .loginModule()
                      .type("class:org.kaazing.gateway.service.http.directory.ExpiringTokenCustomLoginModule")
                      .success("required")
                    .done()
                  .done()
                .done()
                .done();
        // @formatter:on

    }

    @Rule
    public TestRule chain = createRuleChain(30, TimeUnit.SECONDS).around(k3po).around(gwRule);


    @Test
    @Specification({ "expState.challenge.rejected.then.accepted" })
    public void expiringState() throws Exception {
        k3po.finish();
    }





}
