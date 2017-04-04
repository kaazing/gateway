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

import java.security.Principal;

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
public class HttpSubjectSecurityIT {

    private static final String REALM_NAME = "Test Realm";
    private static final String BIND_ADDRESS = "http://localhost:8000/resource";
    private static final String BASIC_CHALLENGE_SCHEME = "Basic";
    private static final String AUTHORIZED_ROLE_STRING = "AUTHORIZED";
    private static final String[] AUTHORIZED_ROLE = new String[] {AUTHORIZED_ROLE_STRING};
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
    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/http/rfc7235/framework");
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
        realms[0] = new DefaultHttpRealmInfo(REALM_NAME, BASIC_CHALLENGE_SCHEME, REALM_NAME, EMPTY_STRING_ARRAY,
                EMPTY_STRING_ARRAY, EMPTY_STRING_ARRAY, loginContextFactoryMock, null);

        acceptor.getAcceptOptions().put("http.realms", realms);
    }

    @Test
    @Specification("missing.then.valid.credentials/request")
    public void testGatewayReturns401WhenLoginModuleChainFailsBecauseNoCredentialsButProvidesEmptyChallengeData()
            throws Exception {
        final Subject subject = new Subject();
        acceptor.getAcceptOptions().put("http.requiredRoles", ANY_ROLE);

        context.checking(new Expectations() {
            {
                exactly(2).of(loginContextFactoryMock).createLoginContext(with(aNonNull(TypedCallbackHandlerMap.class)));
                will(returnValue(loginContextMock));
                exactly(2).of(loginContextMock).login();
                will(onConsecutiveCalls(
                        throwException(new LoginException()),
                        VoidAction.INSTANCE));
                exactly(2).of(loginContextMock).getLoginResult();
                will(returnValue(loginResultMock));
                exactly(2).of(loginResultMock).getType();
                will(onConsecutiveCalls(
                        returnValue(LoginResult.Type.CHALLENGE),
                        returnValue(LoginResult.Type.SUCCESS)));
                oneOf(loginResultMock).getLoginChallengeData();
                will(returnValue(null));
                exactly(2).of(loginContextMock).getSubject();
                will(returnValue(subject));
                oneOf(loginResultMock).hasLoginAuthorizationAttachment();
                will(returnValue(Boolean.FALSE));
            }
        });

        acceptor.bind(BIND_ADDRESS, HTTP_ACCEPT_HANDLER);
        k3po.finish();
    }

    @Test
    @Specification("missing.then.valid.credentials/request")
    public void testGatewayReturns401WhenLoginModuleChainFailsBecauseNoCredentialsButProvidesAdditionalChallengeData()
            throws Exception {
        final Subject subject = new Subject();
        acceptor.getAcceptOptions().put("http.requiredRoles", ANY_ROLE);

        context.checking(new Expectations() {
            {
                exactly(2).of(loginContextFactoryMock).createLoginContext(with(aNonNull(TypedCallbackHandlerMap.class)));
                will(returnValue(loginContextMock));
                exactly(2).of(loginContextMock).login();
                will(onConsecutiveCalls(
                        throwException(new LoginException()),
                        VoidAction.INSTANCE));
                exactly(2).of(loginContextMock).getLoginResult();
                will(returnValue(loginResultMock));
                exactly(2).of(loginResultMock).getType();
                will(returnValue(LoginResult.Type.CHALLENGE));
                oneOf(loginResultMock).getLoginChallengeData();
                will(returnValue(ADDITIONAL_CHALLENGES));
                exactly(2).of(loginContextMock).getSubject();
                will(returnValue(subject));
                oneOf(loginResultMock).hasLoginAuthorizationAttachment();
                will(returnValue(Boolean.FALSE));
            }
        });

        acceptor.bind(BIND_ADDRESS, HTTP_ACCEPT_HANDLER);
        k3po.finish();
    }

    @Test
    @Specification("forbidden/request")
    public void testGatewayReturns403WhenLoginModuleChainFailsOnInvalidCredentialsWithoutProvidingAdditionalChallengeData()
            throws Exception {
        acceptor.getAcceptOptions().put("http.requiredRoles", ANY_ROLE);

        context.checking(new Expectations() {
            {
                oneOf(loginContextFactoryMock).createLoginContext(with(aNonNull(TypedCallbackHandlerMap.class)));
                will(returnValue(loginContextMock));
                oneOf(loginContextMock).login();
                will(throwException(new LoginException()));
                oneOf(loginContextMock).getLoginResult();
                will(returnValue(loginResultMock));
                oneOf(loginResultMock).getType();
                will(returnValue(LoginResult.Type.SUCCESS));
            }
        });

        acceptor.bind(BIND_ADDRESS, HTTP_ACCEPT_HANDLER);
        k3po.finish();
    }

    @Test
    @Specification("invalid.then.valid.credentials/request")
    public void testGatewayReturns401WhenLoginModuleChainFailsOnInvalidCredentialsButProvidesEmptyChallengeData()
            throws Exception {
        final Subject subject = new Subject();
        acceptor.getAcceptOptions().put("http.requiredRoles", ANY_ROLE);

        context.checking(new Expectations() {
            {
                exactly(2).of(loginContextFactoryMock).createLoginContext(with(aNonNull(TypedCallbackHandlerMap.class)));
                will(returnValue(loginContextMock));
                exactly(2).of(loginContextMock).login();
                will(onConsecutiveCalls(
                        throwException(new LoginException()),
                        VoidAction.INSTANCE));
                exactly(3).of(loginContextMock).getLoginResult();
                will(returnValue(loginResultMock));
                exactly(3).of(loginResultMock).getType();
                will(returnValue(LoginResult.Type.CHALLENGE));
                oneOf(loginResultMock).getLoginChallengeData();
                will(returnValue(null));
                exactly(2).of(loginContextMock).getSubject();
                will(returnValue(subject));
                oneOf(loginResultMock).hasLoginAuthorizationAttachment();
                will(returnValue(Boolean.FALSE));
            }
        });

        acceptor.bind(BIND_ADDRESS, HTTP_ACCEPT_HANDLER);
        k3po.finish();
    }

    @Test
    @Specification("invalid.then.valid.credentials/request")
    public void testGatewayReturns401WhenLoginModuleChainFailsOnInvalidCredentialsButProvidesAdditionalChallengeData()
            throws Exception {
        final Subject subject = new Subject();
        acceptor.getAcceptOptions().put("http.requiredRoles", ANY_ROLE);

        context.checking(new Expectations() {
            {
                exactly(2).of(loginContextFactoryMock).createLoginContext(with(aNonNull(TypedCallbackHandlerMap.class)));
                will(returnValue(loginContextMock));
                exactly(2).of(loginContextMock).login();
                will(onConsecutiveCalls(
                        throwException(new LoginException()),
                        VoidAction.INSTANCE));
                exactly(3).of(loginContextMock).getLoginResult();
                will(returnValue(loginResultMock));
                exactly(3).of(loginResultMock).getType();
                will(onConsecutiveCalls(
                        returnValue(LoginResult.Type.CHALLENGE),
                        returnValue(LoginResult.Type.CHALLENGE),
                        returnValue(LoginResult.Type.SUCCESS)));
                oneOf(loginResultMock).getLoginChallengeData();
                will(returnValue(ADDITIONAL_CHALLENGES));
                exactly(2).of(loginContextMock).getSubject();
                will(returnValue(subject));
                oneOf(loginResultMock).hasLoginAuthorizationAttachment();
                will(returnValue(Boolean.FALSE));
            }
        });

        acceptor.bind(BIND_ADDRESS, HTTP_ACCEPT_HANDLER);
        k3po.finish();
    }

    @Test
    @Specification("forbidden/request")
    public void testGatewayReturns403WhenLoginModuleChainSucceedsWithoutProvidingAdditionalChallengesAndAuthorizationConstraintsAreNotMet()
            throws Exception {
        acceptor.getAcceptOptions().put("http.requiredRoles", AUTHORIZED_ROLE);

        context.checking(new Expectations() {
            {
                oneOf(loginContextFactoryMock).createLoginContext(with(aNonNull(TypedCallbackHandlerMap.class)));
                will(returnValue(loginContextMock));
                oneOf(loginContextMock).login();
                oneOf(loginContextMock).getLoginResult();
                will(returnValue(loginResultMock));
                oneOf(loginResultMock).getType();
                will(returnValue(LoginResult.Type.SUCCESS));
                oneOf(loginContextMock).getSubject();
                will(returnValue(new Subject()));
            }
        });

        acceptor.bind(BIND_ADDRESS, HTTP_ACCEPT_HANDLER);
        k3po.finish();
    }

    @Test
    @Specification("partial.then.valid.credentials/request")
    public void testGatewayReturns401WhenLoginModuleChainSucceedsProvidingAdditionalChallengesAndAuthorizationConstraintsAreNotMet()
            throws Exception {
        final Subject unauthorizedSubject = new Subject();
        final Subject authorizedSubject = new Subject();
        authorizedSubject.getPrincipals().add(new Principal() {
            @Override
            public String getName() {
                return AUTHORIZED_ROLE_STRING;
            }
        });
        acceptor.getAcceptOptions().put("http.requiredRoles", AUTHORIZED_ROLE);

        context.checking(new Expectations() {
            {
                exactly(2).of(loginContextFactoryMock).createLoginContext(with(aNonNull(TypedCallbackHandlerMap.class)));
                will(returnValue(loginContextMock));
                exactly(2).of(loginContextMock).login();
                exactly(2).of(loginContextMock).getLoginResult();
                will(returnValue(loginResultMock));
                exactly(3).of(loginResultMock).getType();
                will(returnValue(LoginResult.Type.CHALLENGE));
                exactly(3).of(loginContextMock).getSubject();
                will(onConsecutiveCalls(
                        returnValue(unauthorizedSubject),
                        returnValue(authorizedSubject),
                        returnValue(authorizedSubject)));
                oneOf(loginResultMock).getLoginChallengeData();
                will(returnValue(ADDITIONAL_CHALLENGES));
                oneOf(loginResultMock).hasLoginAuthorizationAttachment();
                will(returnValue(Boolean.FALSE));
            }
        });

        acceptor.bind(BIND_ADDRESS, HTTP_ACCEPT_HANDLER);
        k3po.finish();
    }

}
