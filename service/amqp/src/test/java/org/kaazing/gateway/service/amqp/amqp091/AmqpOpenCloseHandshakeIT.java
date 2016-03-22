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
package org.kaazing.gateway.service.amqp.amqp091;

import static org.kaazing.test.util.ITUtil.createRuleChain;

import java.security.Principal;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.ScriptProperty;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

public class AmqpOpenCloseHandshakeIT {


    private K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing");

    private GatewayRule gateway = new GatewayRule() {
        {
            // @formatter:off
            GatewayConfiguration configuration = new GatewayConfigurationBuilder()
                    .service()
                    .accept("wsn://localhost:8333/amqp")
                    .connect("tcp://localhost:8334")
                    .type("amqp.proxy")
                    .property("service.domain","localhost")
                    .property("encryption.key.alias", "session")
                    .realmName("demo")
                    .authorization()
                        .requireRole("AUTHORIZED")
                    .done()
                .done()
                .security()
                    .realm()
                        .name("demo")
                        .description("Kaazing WebSocket Gateway Demo")
                        .httpChallengeScheme("Basic")
                        .loginModule()
                            .type("class:" + BasicLoginModule.class.getName())
                            .success("requisite")
                        .done()
                    .done()
                .done()
            .done();
            // @formatter:on
            init(configuration);
        }
    };

    @Rule
    public TestRule chain = createRuleChain(gateway, k3po);
    
    
    @Test
    @ScriptProperty({ "connectLocation \"http://localhost:8333/amqp\"", "acceptLocation \"tcp://localhost:8334\"" })
    @Specification({ "specification/amqp/ws/ws.connect", "specification/amqp/ws/open/identity/request", "specification/amqp/ws/close/request",  
        "specification/amqp/tcp/open/tcp.accept", "specification/amqp/tcp/open/identity/response", "specification/amqp/tcp/close/response"})
    public void openAndCloseHandshake() throws Exception {
            k3po.finish();
    }
    
    public static class BasicLoginModule implements LoginModule {
        private Subject subject;
        private CallbackHandler callbackHandler;

        @Override
        public void initialize(Subject subject,
                               CallbackHandler callbackHandler,
                               Map<String, ?> sharedState,
                               Map<String, ?> options) {
            this.subject = subject;
            this.callbackHandler = callbackHandler;
        }

        @Override
        public boolean login() throws LoginException {
            return true;
        }

        @Override
        public boolean commit() throws LoginException {
            subject.getPrincipals().add(new Principal() {
                @Override
                public String getName() {
                    return "AUTHORIZED";
                }
            });
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


}
