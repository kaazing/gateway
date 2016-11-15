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

import static org.junit.Assert.assertTrue;
import static org.kaazing.gateway.management.test.util.TlsTestUtil.getKeystoreFileLocation;
import static org.kaazing.gateway.management.test.util.TlsTestUtil.keyStore;
import static org.kaazing.gateway.management.test.util.TlsTestUtil.password;
import static org.kaazing.gateway.management.test.util.TlsTestUtil.trustStore;

import java.security.KeyStore;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.Gateway;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.test.util.MethodExecutionTrace;

/**
 * This test contains a negative tests for the configuration of user principle classes.
 */
public class BadUserPrincipalsIT {

    private static final String WS_URI = "ws://localhost:8001/echo";

    protected static final String ADMIN = "AUTHORIZED";
    protected static final String ECHO_WS_SERVICE = "echoWS";

    private final KeyStore keyStore = keyStore();
    private final char[] password = password();
    private final KeyStore trustStore = trustStore();

    @Rule
    public TestRule trace = new MethodExecutionTrace();


    @Test(expected=IllegalArgumentException.class)
    public void shouldFailGatewayStartWhenCantLoadUserPrincipalClass() throws Exception {
        GatewayConfiguration configuration = createGatewayConfiguration("i.dont.Exist");
        Gateway gateway = new Gateway();
        try {
            gateway.start(configuration);
        }
        catch(IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().matches(".*Unable to load.*i.dont.Exist.*ClassNotFoundException.*"));
            throw e;
        }
    }

    @Test(expected=IllegalArgumentException.class)
    public void shouldFailGatewayStartWhenNamedUserPrincipalClassIsNotAPrincipal() throws Exception {
        GatewayConfiguration configuration = createGatewayConfiguration(NotAPrincipal.class.getName());
        Gateway gateway = new Gateway();
        try {
            gateway.start(configuration);
        }
        catch(IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().matches(".*NotAPrincipal is not of type Principal"));
            throw e;
        }
    }

    private GatewayConfiguration createGatewayConfiguration(String userPrincipalClassName) throws Exception {

        // @formatter:off
        @SuppressWarnings("deprecation")
        GatewayConfiguration configuration =
            new GatewayConfigurationBuilder()
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
                        .userPrincipalClass(userPrincipalClassName)
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
    }

    public class NotAPrincipal {

    }

}