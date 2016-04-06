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
package org.kaazing.gateway.service.http.directory.specification;

import static org.kaazing.test.util.ITUtil.createRuleChain;

import java.io.File;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

public class Rfc7235SpecificationIT {

    private final K3poRule robot = new K3poRule().setScriptRoot("org/kaazing/specification/http/rfc7235");

    private final GatewayRule gateway = new GatewayRule() {
       {
           GatewayConfiguration configuration = new GatewayConfigurationBuilder()
                    .webRootDirectory(new File("src/test/webapp"))
                    .service()
                        .accept("http://localhost:8000")
                        .type("directory")
                        .property("directory", "/public")
                        .property("welcome-file", "resource")
                        .realmName("demo")
                        .authorization()
                            .requireRole("AUTHORIZED")
                        .done()
                        .crossOrigin()
                            .allowOrigin("*")
                        .done()
                    .done()
                    .security()
                        .realm()
                            .name("demo")
                            .description("Kaazing Gateway Demo")
                            .httpChallengeScheme("Basic")
                            .authorizationMode("challenge")
                            .loginModule()
                                 .type("file")
                                 .success("required")
                                 .option("file", "src/test/resources/gateway/conf/jaas-config.xml")
                            .done()
                        .done()
                    .done()
                .done();
            init(configuration);
        }
    };

    @Rule
    public TestRule chain = createRuleChain(gateway, robot);

    @Test
    @Specification("status/valid.credentials/request")
    public void shouldRespond200WithValidCredentials() throws Exception {
        robot.finish();
    }

    @Test
    @Specification("status/multiple.requests.with.invalid.credentials/request")
    public void shouldRespondWithMultiple401sWithMultipleInvalidRequests() throws Exception {
        robot.finish();
    }

    @Test
    @Specification("headers/invalid.user/request")
    public void shouldRespond401ToInvalidUser() throws Exception {
        robot.finish();
    }

    @Ignore("Gateway doesn't respond with Forbidden Status code.(403)")
    @Test
    @Specification("framework/forbidden/request")
    public void shouldRespondWithForbiddenStatusCode() throws Exception {
        robot.finish();
    }

    @Test
    @Specification("framework/invalid.then.valid.credentials/request")
    public void shouldRespondWithUnauthorizedStatusCodeWithInvalidCredentials() throws Exception {
        robot.finish();
    }

    @Test
    @Specification("framework/missing.then.valid.credentials/request")
    public void shouldRespondWithUnauthorizedStatusCodeWithMissingCredentials() throws Exception {
        robot.finish();
    }

    @Test
    @Specification("framework/partial.then.valid.credentials/request")
    public void shouldRespondWithUnauthorizedStatusCodeWithPartialCredentials() throws Exception {
        robot.finish();
    }

    @Ignore("Gateway doesn't support proxy requests")
    @Test
    @Specification("headers/invalid.proxy.user/request")
    public void secureProxyShouldSend407ToAnyUnAuthorizedRequest() throws Exception {
        robot.finish();
    }

}

