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
package org.kaazing.gateway.transport.http.security.auth;

import static java.util.concurrent.TimeUnit.SECONDS;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;

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
import org.kaazing.gateway.server.spi.security.LoginResult;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.http.HttpAcceptSession;
import org.kaazing.gateway.transport.http.HttpAcceptorRule;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.test.util.MethodExecutionTrace;

/*
 * These tests are used to verify the behavior of the HttpSubjectSecurityFilter class
 * against possible outcomes of login module chain authentication.
 */
public class MismatchedAuthSchemeSendsExtended401AuthTestIT {

    private static final String REALM_NAME = "Test Realm";
    private static final String BIND_ADDRESS = "http://localhost:8001/echoAuth";
    private static final String APPLICATION_TOKEN_CHALLENGE_SCHEME = "Application Token";
    private static final String[] ANY_ROLE = new String[] {"*"};
    private static final String[] EMPTY_STRING_ARRAY = new String[]{};
    private static final Object[] ADDITIONAL_CHALLENGES = new Object[]{"param=\"value\""};

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
        loginContextFactoryMock = context.mock(DefaultLoginContextFactory.class);
        loginContextMock = context.mock(ResultAwareLoginContext.class);
        loginResultMock = context.mock(DefaultLoginResult.class);

        realms = new HttpRealmInfo[1];
        realms[0] = new DefaultHttpRealmInfo(REALM_NAME, APPLICATION_TOKEN_CHALLENGE_SCHEME, REALM_NAME, EMPTY_STRING_ARRAY,
                EMPTY_STRING_ARRAY, EMPTY_STRING_ARRAY, loginContextFactoryMock, null);

        acceptor.getAcceptOptions().put("http.realms", realms);
    }

    @Test
    @Specification("shouldChallengeDueToMismatchingAuthSchemes")
    public void shouldChallengeDueToMismatchingAuthSchemes2()
            throws Exception {
        acceptor.getAcceptOptions().put("http.requiredRoles", ANY_ROLE);

        context.checking(new Expectations() {
            {
                oneOf(loginContextFactoryMock).createLoginContext(with(aNonNull(TypedCallbackHandlerMap.class)));
                will(returnValue(loginContextMock));
                oneOf(loginContextMock).login();
                will(onConsecutiveCalls(
                        throwException(new LoginException()),
                        VoidAction.INSTANCE));
                oneOf(loginContextMock).getLoginResult();
                will(returnValue(loginResultMock));
                oneOf(loginResultMock).getType();
                will(returnValue(LoginResult.Type.CHALLENGE));
                oneOf(loginResultMock).getLoginChallengeData();
                will(returnValue(ADDITIONAL_CHALLENGES));
            }
        });

        acceptor.bind(BIND_ADDRESS, HTTP_ACCEPT_HANDLER);
        k3po.finish();
    }

    @Test
    @Specification("shouldNotChallengeDueToMismatchingAuthSchemes")
    public void shouldNotChallengeDueToMismatchingAuthSchemes2()
            throws Exception {
        acceptor.getAcceptOptions().put("http.requiredRoles", ANY_ROLE);

        context.checking(new Expectations() {
            {
                oneOf(loginContextFactoryMock).createLoginContext(with(aNonNull(TypedCallbackHandlerMap.class)));
                will(returnValue(loginContextMock));
                oneOf(loginContextMock).login();
                will(onConsecutiveCalls(
                        throwException(new LoginException()),
                        VoidAction.INSTANCE));
                oneOf(loginContextMock).getLoginResult();
                will(returnValue(loginResultMock));
                oneOf(loginResultMock).getType();
                will(returnValue(LoginResult.Type.CHALLENGE));
                oneOf(loginResultMock).getLoginChallengeData();
                will(returnValue(ADDITIONAL_CHALLENGES));
            }
        });

        acceptor.bind(BIND_ADDRESS, HTTP_ACCEPT_HANDLER);
        k3po.finish();
    }

    @Test
    @Specification("shouldGet101ResponseWithCaseInsensitiveMatchedAuthSchemes")
    public void shouldGet101ResponseWithCaseInsensitiveMatchedAuthSchemes()
            throws Exception {
        acceptor.getAcceptOptions().put("http.requiredRoles", ANY_ROLE);
        final Subject subject = new Subject();
        context.checking(new Expectations() {
            {
                oneOf(loginContextFactoryMock).createLoginContext(with(aNonNull(TypedCallbackHandlerMap.class)));
                will(returnValue(loginContextMock));
                oneOf(loginContextMock).login();
                will(onConsecutiveCalls(
                        VoidAction.INSTANCE));
                oneOf(loginContextMock).getLoginResult();
                will(returnValue(loginResultMock));
                oneOf(loginResultMock).getType();
                will(returnValue(LoginResult.Type.CHALLENGE));
                exactly(2).of(loginContextMock).getSubject();
                will(returnValue(subject));
                oneOf(loginResultMock).hasLoginAuthorizationAttachment();
                will(returnValue(Boolean.FALSE));

            }
        });

        acceptor.bind(BIND_ADDRESS, HTTP_ACCEPT_HANDLER);
        k3po.finish();
    }

}
