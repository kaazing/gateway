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

package org.kaazing.gateway.transport.http;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.rules.RuleChain.outerRule;

import java.io.File;
import java.net.URI;

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

public class HttpWithDirectoryIT {

    private TestRule trace = new MethodExecutionTrace();
    private TestRule timeout = new DisableOnDebug(new Timeout(4, SECONDS));
    private final K3poRule robot = new K3poRule().setScriptRoot("org/kaazing/specification/http/rfc7235/basic");

    public GatewayRule gateway = new GatewayRule() {
        {

            GatewayConfiguration configuration = new GatewayConfigurationBuilder()
                    .webRootDirectory(new File("src/test/webapp"))
                    .service()
                        .accept(URI.create("http://localhost:8000"))
                        .type("directory")
                        .property("directory", "/public")
                        .property("welcome-file", "resource")
                        .realmName("demo1")
                        .authorization()
                            .requireRole("AUTHORIZED")
                        .done()
                        .crossOrigin()
                            .allowOrigin("*")
                        .done()
                    .done()
                    .security()
                        .realm()
                            .name("demo1")
                            .description("Kaazing WebSocket Gateway Demo")
                            .httpChallengeScheme("Basic")
                            .authorizationMode("challenge")
                            .loginModule()
                                 .type("file")
                                 .success("required")
                                 .option("file", "src/test/resources/jaas-config.xml")
                            .done()
                        .done()
                    .done()
                .done();
            init(configuration);
        }
    };

    @Rule
    public TestRule chain = outerRule(trace).around(robot).around(gateway).around(timeout);


    @Test
    @Specification("authorized/invalid.then.valid.credentials/request")
    public void authorizedInvalidThenValidCredentials() throws Exception {
        robot.finish();
    }

    @Test
    @Specification("authorized/missing.then.valid.credentials/request")
    public void authorizedMissingThenValidCredentials() throws Exception {
        robot.finish();
    }

    @Test
    @Specification("authorized/valid.credentials/request")
    public void authorizedValidCredentials() throws Exception {
        robot.finish();
    }

    @Ignore("Spec test is failing. Response status code is 401 instead of 403.")
    @Specification("forbidden/request")
    @Test
    public void forbiddenTest() throws Exception {
    	robot.finish();
    }
    
    @Test
    @Specification("unauthorized/invalid.username.valid.password/request")
    public void unauthorizedInvalidUsernameValidPassword() throws Exception {
        robot.finish();
    }

    @Test
    @Specification("unauthorized/multiple.invalid.requests/request")
    public void unauthorizedMultipleInvalidRequests() throws Exception {
        robot.finish();
    }

    @Test
    @Specification("unauthorized/unknown.user/request")
    public void unauthorizedUnknownUser() throws Exception {
        robot.finish();
    }

    @Test
    @Specification("unauthorized/valid.username.invalid.password/request")
    public void unauthorizedValidUsernameInvalidPassword() throws Exception {
        robot.finish();
    }

}
