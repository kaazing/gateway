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
package org.kaazing.gateway.management.jmx;

import static org.kaazing.gateway.management.test.util.TlsTestUtil.getKeystoreFileLocation;

import org.junit.Test;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;

/**
 * Variant of JmxSessionPrincipalsIT which refers to the principal classes by an interface they implement
 * (UserInterface, RoleInterface) rather than the concrete class name.
 */
public class JmxSessionPrincipalsSupertypeIT extends JmxSessionPrincipalsIT {

    @Override
    protected GatewayConfiguration getGatewayConfiguration() {
            // @formatter:off
            @SuppressWarnings("deprecation")
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                        .property("org.kaazing.gateway.transport.ws.CLOSE_TIMEOUT",  "1s") // speed up the test
                        .service()
                            .accept(WS_URI)
                            .type("echo")
                            .name(ECHO_WS_SERVICE)
                            .crossOrigin()
                                .allowOrigin("*")
                            .done()
                            .realmName("demo")
                            .authorization()
                                .requireRole("TEST")
                            .done()
                        .done()
                        .service()
                            .property("connector.server.address", "jmx://localhost:2020/")
                            .type("management.jmx")
                            .authorization()
                                .requireRole(ADMIN)
                            .done()
                            .realmName("jmxrealm")
                        .done()
                        .security()
                            .trustStore(trustStore)
                            .keyStore(keyStore)
                            .keyStorePassword(password)
                            .keyStoreFile(getKeystoreFileLocation())
                            .realm()
                                .name("demo")
                                .description("Kaazing WebSocket Gateway Demo")
                                .httpChallengeScheme("Application Token")
                                .httpQueryParameter("token")
                                // Specify supertypes (an implemented interface) for the principles, this should work
                                .userPrincipalClass("org.kaazing.gateway.management.test.util.TokenCustomLoginModule$RoleInterface")
                                .userPrincipalClass("org.kaazing.gateway.management.test.util.TokenCustomLoginModule$UserInterface")
                                .loginModule()
                                    .type("class:org.kaazing.gateway.management.test.util.TokenCustomLoginModule")
                                    .success("required")
                                .done()
                            .done()
                            .realm()
                                .name("jmxrealm")
                                .description("realm for jmx")
                                .httpChallengeScheme("Application Basic")
                                .loginModule()
                                    .type("class:org.kaazing.gateway.management.test.util.TestLoginModule")
                                    .success("required")
                                .done()
                            .done()
                        .done()
                    .done();
            // @formatter:on
            return configuration;
    };

    // Test should only kill sessions that have the "joe" user Principal
    @Specification({
        "wsn.session.with.user.principal.joe",
        "wse.session.with.user.principal.joe",
        "wsn.session.with.user.principal.ann" })
    @Test
    public void shouldCloseSessionsByUserPrincipal() throws Exception {
        shouldCloseSessionsByUserPrincipal("org.kaazing.gateway.management.test.util.TokenCustomLoginModule$UserInterface");
    }

    // Test should kill all sessions that have "TEST" as a role Principal
    // please see "Jmx should KillSessions By Role Principal can fail if invoked early in session initialization #448"
    @Specification({
        "wsn.session.with.user.principal.joe",
        "wse.session.with.user.principal.joe",
        "wsn.session.with.user.principal.ann" })
    @Test
    public void shouldCloseSessionsByRolePrincipal() throws Exception {
        shouldCloseSessionsByRolePrincipal("org.kaazing.gateway.management.test.util.TokenCustomLoginModule$RoleInterface");
    }

}
