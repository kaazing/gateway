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

import static org.kaazing.test.util.ITUtil.createRuleChain;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

public class DirectoryServiceEmbeddedGWTestIT {

    private static String DIRECTORY_SERVICE_ACCEPT = "http://localhost:8000/";
    private static String AUTH_DIRECTORY_SERVICE_ACCEPT = "http://localhost:8008/auth";

    private final K3poRule robot = new K3poRule();
    private final GatewayRule gateway = new GatewayRule() {
        {
            // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                        .webRootDirectory(new File("src/test/webapp"))
                        .tempDirectory(new File("target/test-gateway-temp-directory"))
                        .service()
                            .accept(DIRECTORY_SERVICE_ACCEPT)
                            .type("directory")
                            .property("directory", "/public")
                                .crossOrigin().allowOrigin("*")
                            .done()
                        .done()
                        .service()
                            .accept(AUTH_DIRECTORY_SERVICE_ACCEPT)
                            .type("directory")
                            .realmName("demo")
                            .property("directory", "/public")
                            .authorization()
                                 .requireRole("AUTHORIZED")
                            .done()
                        .done()
                        .security()
                            .realm()
                                .name("demo")
                                .description("Kaazing WebSocket Gateway Demo")
                                .httpChallengeScheme("Application Token")
                                .authorizationMode("challenge")
                                .loginModule()
                                .type("class:org.kaazing.gateway.security.auth.YesLoginModule")
                                    .success("requisite")
                                    .option("roles", "AUTHORIZED, ADMINISTRATOR")
                            .done()
                        .done()
                     .done()
                .done();
            // @formatter:on
            init(configuration);
        }
    };

    @Rule
    public TestRule chain = createRuleChain(gateway, robot);

    @Specification("get.content.type.xsj")
    @Test
    public void testGetContentTypeXSJ() throws Exception {
        robot.finish();
    }
}
