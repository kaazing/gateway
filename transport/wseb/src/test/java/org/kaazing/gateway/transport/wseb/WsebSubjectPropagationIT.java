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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.kaazing.gateway.util.Utils.asByteBuffer;
import static org.kaazing.test.util.ITUtil.createRuleChain;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.Gateway;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.gateway.service.Service;
import org.kaazing.gateway.service.ServiceContext;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.session.IoSessionEx;

/**
 * This test verifies that the authenticated Subject is made available on the WsebSession.
 */
public class WsebSubjectPropagationIT {

    private K3poRule robot = new K3poRule();

    public GatewayRule gateway = new GatewayRule() {
        {
            GatewayConfiguration configuration = new GatewayConfigurationBuilder()
                .property(Gateway.GATEWAY_CONFIG_DIRECTORY_PROPERTY,
                    "src/test/resources/gateway/conf")
                .service()
                    .accept("wse://localhost:8001/basic")
                    .type("class:" + SubjectService.class.getName())
                    // Websocket level  authentication with revalidate
                    .realmName("basic")
                    .crossOrigin()
                        .allowOrigin("*")
                     .done()
                    .authorization()
                        .requireRole("AUTHORIZED")
                    .done()
                .done()
                .service()
                    .accept("wse://localhost:8001/appbasic")
                    .type("class:" + SubjectService.class.getName())
                    // Websocket level  authentication with revalidate
                    .realmName("appbasic")
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
                    .realm()
                        .name("appbasic")
                        .description("Kaazing WebSocket Gateway Demo")
                        .httpChallengeScheme("Application Basic")
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

    @Specification("shouldPropagateSubjectBasic")
    @Test
    public void shouldPropagateSubjectBasic() throws Exception {
        robot.finish();
    }

    @Specification("shouldPropagateSubjectApplicationBasic")
    @Test
    public void shouldPropagateSubjectApplicationBasic() throws Exception {
        robot.finish();
    }

    public static class TestLoginModule implements LoginModule {
        private Subject subject;
        private CallbackHandler callbackHandler;
        private Principal userPrincipal;

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
            Object[] userPass = getUserPass();
            userPrincipal = new UserPrincipal((String)userPass[0], (char[]) userPass[1]);
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
            subject.getPrincipals().add(userPrincipal);
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

        private Object[] getUserPass() throws LoginException {
            NameCallback nameCB = new NameCallback("username");
            PasswordCallback passwordCB = new PasswordCallback("password", false);

            try {
                callbackHandler.handle(new Callback[] { nameCB, passwordCB });
            } catch (IOException | UnsupportedCallbackException e) {
                throw new LoginException(e.toString());
            }

            String username = nameCB.getName();
            char[] password = passwordCB.getPassword();
            passwordCB.clearPassword();
            return new Object[] { username, password };
        }

    }

    public static class SubjectService implements Service {
        SubjectServiceHandler handler;
        ServiceContext serviceContext;

        @Override
        public String getType() {
            return "$" + getClass().getName() + "$";
        }

        @Override
        public void init(ServiceContext serviceContext) throws Exception {
            this.serviceContext = serviceContext;
            handler = new SubjectServiceHandler();
        }

        @Override
        public void start() throws Exception {
            serviceContext.bind(serviceContext.getAccepts(), handler);
        }

        @Override
        public void stop() throws Exception {
            // TODO Auto-generated method stub

        }

        @Override
        public void quiesce() throws Exception {
            // TODO Auto-generated method stub

        }

        @Override
        public void destroy() throws Exception {
            // TODO Auto-generated method stub

        }

    }

    static class SubjectServiceHandler extends IoHandlerAdapter<IoSessionEx> {

        @Override
        protected void doSessionCreated(IoSessionEx session) throws Exception {
            assertTrue(checkSubject(session));
            super.doSessionCreated(session);
        }

        @Override
        protected void doSessionOpened(IoSessionEx session) throws Exception {
            assertTrue(checkSubject(session));
            super.doSessionCreated(session);
        }

        @Override
        protected void doMessageReceived(IoSessionEx session, Object message) throws Exception {
            String result = checkSubject(session) ? "OK" : "FAIL";
            final IoBufferEx buf = session.getBufferAllocator().wrap(asByteBuffer(result));
            session.write(buf);
        }

        private boolean checkSubject(IoSessionEx session) {
            Subject subject = session.getSubject();
            assertNotNull(subject);
            Set<Principal> principles = subject.getPrincipals();
            assertEquals(2, principles.size());
            Principal user = subject.getPrincipals(UserPrincipal.class).iterator().next();
            return user != null && user.getName().equals("joe");
        }

    }

    public static class UserPrincipal implements Principal {
        private final String name;
        private final char[] password;


        public UserPrincipal(String name, char[] password) {
            this.name = name;
            this.password = password;
        }

        @Override
        public String getName() {
            return name;
        }

        public char[] getPassword() {
            return password;
        }
    }

}
