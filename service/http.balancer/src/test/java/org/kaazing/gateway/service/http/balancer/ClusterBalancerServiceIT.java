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
package org.kaazing.gateway.service.http.balancer;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.net.URI;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.kaazing.gateway.server.test.GatewayClusterRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.test.util.ITUtil;

public class ClusterBalancerServiceIT {

    private GatewayClusterRule rule = new GatewayClusterRule() {
        {
            String balancerURI1 = "ws://gateway.example.com:8001";
            URI clusterMember1URI = URI.create("tcp://localhost:8555");
            URI clusterMember2URI = URI.create("tcp://localhost:8556");
            
            GatewayConfiguration config1 = new GatewayConfigurationBuilder()
                    .cluster()
                        .accept(clusterMember1URI)
                        .connect(clusterMember2URI)
                        .name("clusterName")
                    .done()
                    .service()
                        .type("balancer")
                        .accept(balancerURI1)
                        .acceptOption("ws.bind", "7001")
                    .done()
                    .service()
                        .type("echo")
                        .accept("tcp://localhost:8000")
                        .balance(URI.create(balancerURI1))
                    .done()
                .done();
            GatewayConfiguration config2= new GatewayConfigurationBuilder()
                    .cluster()
                    .name("clusterName")
                    .accept(clusterMember2URI)
                    .connect(clusterMember1URI)
                .done()
                .service()
                    .type("balancer")
                    .accept(balancerURI1)
                    .acceptOption("ws.bind", "7000")
                .done()
                .service()
                    .type("echo")
                    .accept("tcp://localhost:8001")
                    .balance(URI.create(balancerURI1))
                .done()
            .done();
            init(config1, config2);
        }
    };
    
    @Rule
    public RuleChain chain = ITUtil.createRuleChain(rule, 15, SECONDS);

    @Test
    public void testLaunchBalancerService() throws Exception {
        //only throwing exception when trace data needed
        // this test should always pass
       // throw new Exception("Excpetion");
    }
}
