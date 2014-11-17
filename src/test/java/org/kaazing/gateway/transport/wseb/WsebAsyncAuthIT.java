/**
 * Copyright (c) 2007-2014 Kaazing Corporation. All rights reserved.
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.kaazing.gateway.transport.wseb;

import static org.junit.rules.RuleChain.outerRule;

import java.net.URI;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.robot.junit.annotation.Robotic;
import org.kaazing.robot.junit.rules.RobotRule;

public class WsebAsyncAuthIT {

    private RobotRule robot = new RobotRule();

    public GatewayRule gateway = new GatewayRule() {
        {
            GatewayConfiguration configuration = new GatewayConfigurationBuilder()
                //.licenseDirectory(new File("src/test/resources/gateway/license"))
                .service()
                    .accept(URI.create("wse://localhost:8001/jms"))
                    .type("echo")
                    .realmName("demo")
                        .authorization()
                        .requireRole("USER")
                    .done()
                .done()
                .security()
                    .realm()
                        .name("demo")
                        .description("Kaazing WebSocket Gateway Demo")
                        .httpChallengeScheme("Application Token")
                        .loginModule()
                            .type("class:org.kaazing.gateway.security.auth.YesLoginModule")
                            .success("requisite")
                            .option("roles", "USER")
                        .done()
                    .done()
                .done()
            .done();
            init(configuration);
        }
    };

	@Rule
	public TestRule chain = outerRule(robot).around(gateway);

	@Robotic(script = "asyncAuthWsebSuccess")
    @Test(timeout = 5000)
	public void asyncAuthWsebSuccess() throws Exception {
		robot.join();
	}

}
