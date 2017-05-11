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

import static java.util.concurrent.TimeUnit.SECONDS;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;

import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.jmock.lib.action.VoidAction;
import org.jmock.lib.concurrent.Synchroniser;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.gateway.resource.address.http.DefaultHttpRealmInfo;
import org.kaazing.gateway.resource.address.http.HttpRealmInfo;
import org.kaazing.gateway.security.LoginContextFactory;
import org.kaazing.gateway.security.TypedCallbackHandlerMap;
import org.kaazing.gateway.security.auth.DefaultLoginResult;
import org.kaazing.gateway.security.auth.context.DefaultLoginContextFactory;
import org.kaazing.gateway.security.auth.context.ResultAwareLoginContext;
import org.kaazing.gateway.server.spi.security.ExpiringState;
import org.kaazing.gateway.server.spi.security.LoginResult;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.http.HttpAcceptSession;
import org.kaazing.gateway.transport.http.HttpAcceptorRule;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.test.util.MethodExecutionTrace;

public class MismatchedAuthSchemeOnMultipleRealmsIT {

    private static final String REALM_NAME_TOKEN = "Application Token";
    private static final String REALM_NAME_BASIC = "Basic";
    private static final String BIND_ADDRESS = "http://localhost:8008/echo";
    private static final String APPLICATION_TOKEN_CHALLENGE_SCHEME = "Application Token";
    private static final String BASIC_CHALLENGE_SCHEME = "Basic";
    private static final String[] ANY_ROLE = new String[] {"*"};
    private static final String[] EMPTY_STRING_ARRAY = new String[]{};

    private static final IoHandlerAdapter<HttpAcceptSession> HTTP_ACCEPT_HANDLER = new IoHandlerAdapter<HttpAcceptSession>() {
        @Override
        protected void doSessionOpened(HttpAcceptSession session) throws Exception {
            session.setStatus(HttpStatus.SUCCESS_OK);
            session.close(false);
        }
    };

    private final HttpAcceptorRule acceptor = new HttpAcceptorRule();
    private final TestRule trace = new MethodExecutionTrace();
    private final K3poRule k3po = new K3poRule();
    private final TestRule timeoutRule = new DisableOnDebug(new Timeout(5, SECONDS));

    private LoginContextFactory loginContextFactoryMock;
    private ResultAwareLoginContext loginContextMock;
    private DefaultLoginResult loginResultMock;
    private ExpiringState expiringStateMock;

    private HttpRealmInfo[] realms;

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
            setThreadingPolicy(new Synchroniser());
        }
    };

    @Rule
    public TestRule chain = RuleChain.outerRule(trace).around(acceptor).around(k3po).around(timeoutRule);

    @Before
    public void setUp() {
        // set up mocks
        loginContextFactoryMock = context.mock(DefaultLoginContextFactory.class);
        loginContextMock = context.mock(ResultAwareLoginContext.class);
        loginResultMock = context.mock(DefaultLoginResult.class);
        expiringStateMock = context.mock(ExpiringState.class);

        // set up http acceptor
        acceptor.setExpiringState(expiringStateMock);

        realms = new HttpRealmInfo[2];
        realms[0] = new DefaultHttpRealmInfo(REALM_NAME_TOKEN, APPLICATION_TOKEN_CHALLENGE_SCHEME, REALM_NAME_TOKEN, EMPTY_STRING_ARRAY,
                EMPTY_STRING_ARRAY, EMPTY_STRING_ARRAY, loginContextFactoryMock, null);

        realms[1] = new DefaultHttpRealmInfo(REALM_NAME_BASIC, BASIC_CHALLENGE_SCHEME, REALM_NAME_BASIC, EMPTY_STRING_ARRAY,
                EMPTY_STRING_ARRAY, EMPTY_STRING_ARRAY, loginContextFactoryMock, null);

        acceptor.getAcceptOptions().put("http.realms", realms);
    }

    @Test
    @Specification("shouldGet401ResponseDueToMismatchingAuthSchemes")
    public void shouldGet401ResponseDueToMismatchingAuthSchemes() throws Exception {
        acceptor.getAcceptOptions().put("http.requiredRoles", ANY_ROLE);
        final Subject subject = new Subject();
        LoginContext[] loginContextArrayForSecondRealm = new LoginContext[] {loginContextMock, null};

        context.checking(new Expectations() {
            {
                allowing(loginContextFactoryMock).createLoginContext(with(aNonNull(TypedCallbackHandlerMap.class)));
                will(returnValue(loginContextMock));
                allowing(loginContextMock).login();
                will(VoidAction.INSTANCE);
                exactly(2).of(loginContextMock).getLoginResult();
                will(returnValue(loginResultMock));
                exactly(2).of(loginResultMock).getType();
                will(onConsecutiveCalls(
                        returnValue(LoginResult.Type.SUCCESS),
                        returnValue(LoginResult.Type.CHALLENGE),
                        returnValue(LoginResult.Type.SUCCESS)));
                exactly(4).of(loginContextMock).getSubject();
                will(returnValue(subject));
                exactly(2).of(loginResultMock).hasLoginAuthorizationAttachment();
                will(returnValue(Boolean.FALSE));
                exactly(2).of(expiringStateMock).get(with(any(String.class)));
                will(onConsecutiveCalls(
                        returnValue(loginContextArrayForSecondRealm),
                        returnValue(loginContextArrayForSecondRealm)));
                allowing(expiringStateMock).putIfAbsent(
                        with(any(String.class)),
                        with(equal(loginContextArrayForSecondRealm)),
                        with(30L),
                        with(SECONDS));
                will(returnValue(null));
            }
        });

        acceptor.bind(BIND_ADDRESS, HTTP_ACCEPT_HANDLER);
        k3po.finish();
    }
}
