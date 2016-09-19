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
package org.kaazing.gateway.transport.http.security.auth.connector;

import static java.net.Authenticator.setDefault;
import static org.kaazing.gateway.transport.http.HttpMethod.GET;
import static org.kaazing.gateway.transport.http.HttpStatus.CLIENT_UNAUTHORIZED;
import static org.kaazing.gateway.transport.http.HttpStatus.SUCCESS_OK;
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
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.States;
import org.jmock.lib.concurrent.Synchroniser;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.transport.http.HttpConnectSession;
import org.kaazing.gateway.transport.http.HttpConnectorRule;
import org.kaazing.gateway.transport.http.HttpSession;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.mina.core.buffer.IoBufferEx;

public class AuthenticatorIT {

    private final HttpConnectorRule connector = new HttpConnectorRule().addProperty(HTTP_AUTHENTICATOR.getPropertyName(), "true");
    private final K3poRule k3po = new K3poRule();
    private States testState;

    @Rule
    public TestRule chain = createRuleChain(connector, k3po);

    @Rule
    public ResetAuthenticatorRule resetAuthenticatorRule = new ResetAuthenticatorRule();

    AuthenticatorMock authenticator;
    private Mockery context;

    Synchroniser syncronizer;

    public abstract class AuthenticatorMock extends Authenticator {
        public abstract PasswordAuthentication getPasswordAuthentication();
    }

    @Before
    public void initialize() {
        context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
        testState = context.states("testState").startsAs("initial-state");
        syncronizer = new Synchroniser();
        context.setThreadingPolicy(syncronizer);
        authenticator = context.mock(AuthenticatorMock.class);
        // inner class this
        setDefault(authenticator);
    }

    @After
    public void after() {
        context.assertIsSatisfied();
    }

    private SessionWithStatus withHttpSessionOfStatus(HttpStatus status) {
        return new SessionWithStatus(status);
    }

    private class SessionWithStatus extends BaseMatcher<HttpSession> {

        private HttpStatus status;

        public SessionWithStatus(HttpStatus status) {
            this.status = status;
        }

        @Override
        public boolean matches(Object item) {
            return item instanceof HttpSession && status.equals(((HttpSession) item).getStatus());
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("Matches HttpSession with status " + status);
        }
    }

    @Specification("basic.challenge")
    @Test
    public void basicChallenge() throws Exception {
        connector.getConnectOptions().put("http.max.authentication.attempts", "0");
        final IoHandler handler = context.mock(IoHandler.class);
        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSession.class)));
                oneOf(handler).sessionOpened(with(any(IoSession.class)));
                oneOf(handler).messageReceived(with(withHttpSessionOfStatus(CLIENT_UNAUTHORIZED)), with(any(IoBufferEx.class)));
                oneOf(handler).sessionClosed(with(any(IoSession.class)));
                then(testState.is("finished"));
            }
        });
        Map<String, Object> connectOptions = new HashMap<>();
        connector.connect("http://localhost:8080/resource", handler, new IoSessionInitializer<ConnectFuture>() {
            @Override
            public void initializeSession(IoSession session, ConnectFuture future) {
                HttpConnectSession connectSession = (HttpConnectSession) session;
                connectSession.setMethod(GET);
            }
        }, connectOptions);

        k3po.finish();
        syncronizer.waitUntil(testState.is("finished"));
    }

    @Specification("basic.challenge.and.accept")
    @Test
    public void basicChallengeAndAccept() throws Exception {
        connector.getConnectOptions().put("http.max.authentication.attempts", "1");
        final IoHandler handler = context.mock(IoHandler.class);
        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSession.class)));
                oneOf(handler).sessionOpened(with(any(IoSession.class)));
                oneOf(authenticator).getPasswordAuthentication();
                will(returnValue(new PasswordAuthentication("joe", new char[]{'w', 'e', 'l', 'c', 'o', 'm', 'e'})));
                oneOf(handler).messageReceived(with(withHttpSessionOfStatus(SUCCESS_OK)), with(any(IoBufferEx.class)));
                oneOf(handler).sessionClosed(with(any(IoSession.class)));
                then(testState.is("finished"));
            }
        });
        Map<String, Object> connectOptions = new HashMap<>();

        connector.connect("http://localhost:8080/resource", handler, new IoSessionInitializer<ConnectFuture>() {
            @Override
            public void initializeSession(IoSession session, ConnectFuture future) {
                HttpConnectSession connectSession = (HttpConnectSession) session;
                connectSession.setMethod(GET);
            }
        }, connectOptions);

        k3po.finish();
        syncronizer.waitUntil(testState.is("finished"));
    }

    @Specification("basic.challenge.twice")
    @Test
    public void wontChallengeMoreThenNumberOfAttempts() throws Exception {
        connector.getConnectOptions().put("http.max.authentication.attempts", "1");
        final IoHandler handler = context.mock(IoHandler.class);
        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSession.class)));
                oneOf(handler).sessionOpened(with(any(IoSession.class)));
                oneOf(authenticator).getPasswordAuthentication();
                will(returnValue(new PasswordAuthentication("joe", new char[]{'w', 'e', 'l', 'c', 'o', 'm', 'e'})));
                oneOf(handler).messageReceived(with(withHttpSessionOfStatus(CLIENT_UNAUTHORIZED)), with(any(IoBufferEx.class)));
                oneOf(handler).sessionClosed(with(any(IoSession.class)));
                then(testState.is("finished"));
            }
        });
        Map<String, Object> connectOptions = new HashMap<>();

        connector.connect("http://localhost:8080/resource", handler, new IoSessionInitializer<ConnectFuture>() {
            @Override
            public void initializeSession(IoSession session, ConnectFuture future) {
                HttpConnectSession connectSession = (HttpConnectSession) session;
                connectSession.setMethod(GET);
            }
        }, connectOptions);

        k3po.finish();
        syncronizer.waitUntil(testState.is("finished"));
    }

    @Specification("basic.challenge.twice.and.accept")
    @Test
    public void willChallengeMultipleAttempts() throws Exception {
        connector.getConnectOptions().put("http.max.authentication.attempts", "2");
        final IoHandler handler = context.mock(IoHandler.class);
        context.checking(new Expectations() {
            {
                oneOf(handler).sessionCreated(with(any(IoSession.class)));
                oneOf(handler).sessionOpened(with(any(IoSession.class)));
                exactly(2).of(authenticator).getPasswordAuthentication();
                will(returnValue(new PasswordAuthentication("joe", new char[]{'w', 'e', 'l', 'c', 'o', 'm', 'e'})));
                oneOf(handler).messageReceived(with(withHttpSessionOfStatus(SUCCESS_OK)), with(any(IoBufferEx.class)));
                oneOf(handler).sessionClosed(with(any(IoSession.class)));
                then(testState.is("finished"));
            }
        });
        Map<String, Object> connectOptions = new HashMap<>();

        connector.connect("http://localhost:8080/resource", handler, new IoSessionInitializer<ConnectFuture>() {
            @Override
            public void initializeSession(IoSession session, ConnectFuture future) {
                HttpConnectSession connectSession = (HttpConnectSession) session;
                connectSession.setMethod(GET);
            }
        }, connectOptions);

        k3po.finish();
        syncronizer.waitUntil(testState.is("finished"));
    }

}
