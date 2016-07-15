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
package org.kaazing.gateway.transport.http.acceptor.specification.rfc7235;

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

@Ignore("Errors galore")
public class AuthorizationIT {

    private TestRule trace = new MethodExecutionTrace();
    private TestRule timeout = new DisableOnDebug(new Timeout(4, SECONDS));
    private final K3poRule robot = new K3poRule().setScriptRoot("org/kaazing/specification/http/rfc7235");

    public GatewayRule gateway = new GatewayRule() {
        {

            GatewayConfiguration configuration = new GatewayConfigurationBuilder()
                    .webRootDirectory(new File("src/test"))
                    .service()
                        .accept("http://localhost:8000")
                        .type("directory")
                        .property("directory", "/resources")
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
    @Specification("framework/invalid.then.valid.credentials/request")
    public void authorizedInvalidThenValidCredentials() throws Exception {
        robot.finish();
    }

    @Test
    @Specification("framework/missing.then.valid.credentials/request")
    public void authorizedMissingThenValidCredentials() throws Exception {
        robot.finish();
    }

    @Test
    @Specification("status/valid.credentials/request")
    public void authorizedValidCredentials() throws Exception {
        robot.finish();
    }

    @Specification("framework/forbidden/request")
    @Test
    public void forbiddenTest() throws Exception {
    	robot.finish();
    }
    
    @Test
    @Specification("framework/partial.then.valid.credentials/request")
    public void unauthorizedInvalidUsernameValidPassword() throws Exception {
        robot.finish();
    }

    @Test
    @Specification("status/multiple.requests.with.invalid.credentials/request")
    public void unauthorizedMultipleInvalidRequests() throws Exception {
        robot.finish();
    }

    @Test
    @Specification("headers/invalid.user/request")
    public void unauthorizedUnknownUser() throws Exception {
        robot.finish();
    }

}



