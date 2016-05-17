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
package org.kaazing.gateway.server.context.resolve;

import static org.junit.Assert.assertTrue;
import static org.kaazing.gateway.util.InternalSystemProperty.WSE_SPECIFICATION;

import org.junit.Test;
import org.kaazing.gateway.server.test.Gateway;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;

public class ServiceContextRealmInvalidUserPrincipalClassTest {

    private static final String ROLE = "USER";
    private static final String DEMO_REALM = "demo";
    private static final String PRINCIPAL = "com.kaazing.gateway.server.auth.config.parse.DefaultUserConfig1";

    @Test
    public void verifyExceptionThrownInvalidUserPrincipalClass() throws Exception {
        Gateway gateway = new Gateway();
        GatewayConfiguration configuration = new GatewayConfigurationBuilder()
            .property(WSE_SPECIFICATION.getPropertyName(), "true")
            .service()
                .accept("ws://localhost:8080/path")
                .type("echo")
                .crossOrigin()
                    .allowOrigin("http://localhost:8001")
                .done()
                .realmName(DEMO_REALM)
                    .authorization()
                    .requireRole(ROLE)
                .done()
            .done()
            .security()
                .realm()
                    .name(DEMO_REALM)
                    .description("Kaazing WebSocket Gateway Demo")
                    .httpChallengeScheme("Basic")
                    .userPrincipalClass(PRINCIPAL)
                    .loginModule()
                        .type("class:org.kaazing.gateway.server.context.resolve.BasicLoginModuleWithDefaultUserConfig")
                        .success("requisite")
                        .option("roles", ROLE)
                    .done()
                .done()
            .done()
        .done();
        try {
            gateway.start(configuration);
            throw new AssertionError("RuntimeException was not thrown");
        }
        catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Class " + PRINCIPAL
                    + " could not be loaded. Please check the gateway configuration xml and confirm that user-principal-class value(s) are spelled correctly for realm "
                    + DEMO_REALM + "."));
        }
        finally {
            gateway.stop();
        }
    }

}