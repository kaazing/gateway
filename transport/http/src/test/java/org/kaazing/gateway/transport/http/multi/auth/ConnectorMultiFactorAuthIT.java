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
package org.kaazing.gateway.transport.http.multi.auth;

import static java.net.Authenticator.setDefault;
import static org.kaazing.gateway.transport.http.HttpMethod.GET;
import static org.kaazing.gateway.util.feature.EarlyAccessFeatures.HTTP_AUTHENTICATOR;
import static org.kaazing.test.util.ITUtil.createRuleChain;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.HashMap;
import java.util.Map;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;
import org.jmock.Expectations;
import org.jmock.States;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.transport.http.HttpConnectSession;
import org.kaazing.gateway.transport.http.HttpConnectorRule;
import org.kaazing.gateway.transport.http.security.auth.connector.ResetAuthenticatorRule;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

public class ConnectorMultiFactorAuthIT {
    private final HttpConnectorRule connector =
            new HttpConnectorRule().addProperty(HTTP_AUTHENTICATOR.getPropertyName(), "true");
    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/http/multi/auth");
    private States testState;

    @Rule
    public TestRule chain = createRuleChain(connector, k3po);

    @Rule
    public ResetAuthenticatorRule resetAuthenticatorRule = new ResetAuthenticatorRule();

    AuthenticatorMock authenticator;

    Synchroniser syncronizer;

    public abstract class AuthenticatorMock extends Authenticator {
        public abstract PasswordAuthentication getPasswordAuthentication();
    }

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery() {
        {
            setThreadingPolicy(new Synchroniser());
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    @Before
    public void before(){
        testState = context.states("testState").startsAs("initial-state");
        syncronizer = new Synchroniser();
        context.setThreadingPolicy(syncronizer);
        authenticator = context.mock(AuthenticatorMock.class);
        // inner class this
        setDefault(authenticator);
    }

    @Test
    @Specification({"request.with.secure.challenge.identity/server",})
    public void serverMayGiveSecChallengeIdentityHeaderWith401() throws Exception {
        connector.getConnectOptions().put("http.max.authentication.attempts", "5");
        final IoHandler handler = context.mock(IoHandler.class);
        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSession.class)));
                oneOf(handler).sessionOpened(with(any(IoSession.class)));
                exactly(2).of(authenticator).getPasswordAuthentication();
                will(onConsecutiveCalls(
                        returnValue(new PasswordAuthentication("joe", new char[]{'w', 'e', 'l', 'c', 'o', 'm', 'e'})),
                        returnValue(new PasswordAuthentication("pin", new char[]{'1', '2', '3', '4'}))));
                oneOf(handler).sessionClosed(with(any(IoSession.class)));
                then(testState.is("finished"));
            }
        });
        Map<String, Object> connectOptions = new HashMap<>();

        connector.connect("http://localhost:8000/resource", handler, new IoSessionInitializer<ConnectFuture>() {
            @Override
            public void initializeSession(IoSession session, ConnectFuture future) {
                HttpConnectSession connectSession = (HttpConnectSession) session;
                connectSession.setMethod(GET);
            }
        }, connectOptions);

        k3po.finish();
        syncronizer.waitUntil(testState.is("finished"));
    }

    @Test
    @Specification({"response.with.secure.challenge.identity/server"})
    public void clientShouldAttachSecChallengeIdentityToFollowingRequests() throws Exception {
        connector.getConnectOptions().put("http.max.authentication.attempts", "1");
        final IoHandler handler = context.mock(IoHandler.class);
        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSession.class)));
                oneOf(handler).sessionOpened(with(any(IoSession.class)));
                oneOf(authenticator).getPasswordAuthentication();
                will(returnValue(new PasswordAuthentication("joe", new char[]{'w', 'e', 'l', 'c', 'o', 'm', 'e'})));
                allowing(handler).sessionClosed(with(any(IoSession.class)));
            }
        });
        Map<String, Object> connectOptions = new HashMap<>();

        connector.connect("http://localhost:8000/resource", handler, new IoSessionInitializer<ConnectFuture>() {
            @Override
            public void initializeSession(IoSession session, ConnectFuture future) {
                HttpConnectSession connectSession = (HttpConnectSession) session;
                connectSession.setMethod(GET);
            }
        }, connectOptions);

        k3po.finish();
    }

//    @Test
//    @Ignore("In SPEC but not applicable to client")
//    @Specification({"request.missing.secure.challenge.identity/server"})
//    public void serverShouldChallengeFirstFactorWhenSecChallengeIdentityHeaderMissing() throws Exception {
//    }
}
