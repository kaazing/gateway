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
package org.kaazing.gateway.transport.wseb;

import static org.kaazing.test.util.ITUtil.createRuleChain;

import java.security.Principal;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

public class WsebAuthIT {

    private final K3poRule robot = new K3poRule();

    private GatewayRule gateway = new GatewayRule() {
        {
            GatewayConfiguration configuration = new GatewayConfigurationBuilder()
                .service()
                    .accept("wse://localhost:8001/basic")
                    .type("echo")
                    .realmName("basic")
                    .crossOrigin()
                        .allowOrigin("*")
                    .done()
                    .authorization()
                        .requireRole("AUTHORIZED")
                    .done()
                .done()
                .security()
                    .realm()
                        .name("basic")
                        .description("Kaazing WebSocket Gateway Demo")
                        .httpChallengeScheme("Basic")
                        .loginModule()
                            .type("class:" + TestLoginModule.class.getName())
                            .success("requisite")
                        .done()
                    .done()
                .done()
            .done();

            init(configuration);
        }
    };

    @Rule
    public TestRule chain = createRuleChain(gateway, robot);

    @Before
    public void initLoginModule() {
        TestLoginModule.once = false;
    }

    // Tests to make sure up and down streams don't go through authentication
    @Test
    @Specification("up.and.down.streams.httpxe.no.authentication")
    public void httpxeAuthentication() throws Exception {
        robot.finish();
    }

    @Test
    @Specification("up.and.down.streams.http.no.authentication")
    public void httpAuthentication() throws Exception {
        robot.finish();
    }

    /*
     * A login module which succeeds only first time (i.e for create requests)
     */
    public static class TestLoginModule implements LoginModule {
        // LoginContext.login() creates new instance of the LoginModule
        // Hence using a static field
        static boolean once;
        private static final Principal ROLE_PRINCIPAL = new RolePrincipal();

        private Subject subject;

        @Override
        public void initialize(Subject subject, CallbackHandler callbackHandler,
                    Map<String, ?> sharedState, Map<String, ?> options) {
            this.subject = subject;
        }

        @Override
        public boolean login() throws LoginException {
            if (once) {
                throw new LoginException("LoginModule shouldn't be invoked for upstream and downstream requests");
            }
            once = true;
            return true;
        }

        @Override
        public boolean commit() throws LoginException {
            subject.getPrincipals().add(ROLE_PRINCIPAL);
            return true;
        }

        @Override
        public boolean abort() throws LoginException {
            return true;
        }

        @Override
        public boolean logout() throws LoginException {
            return true;
        }

    }

    static class RolePrincipal implements Principal {
        @Override
        public String getName() {
            return "AUTHORIZED";
        }
    }

}
