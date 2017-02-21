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
import org.jmock.lib.concurrent.Synchroniser;
import org.jmock.lib.legacy.ClassImposteriser;
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
import org.kaazing.gateway.transport.http.HttpAcceptorRule;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.test.util.MethodExecutionTrace;

public class HttpAcceptorAuthenticationIT {

    private static final String REALM_NAME = "Test Realm";
    private static final String BIND_ADDRESS = "http://localhost:8000/resource";
    private static final String BASIC_CHALLENGE_SCHEME = "Basic";
    private static final Object[] ADDITIONAL_CHALLENGES = new Object[]{"param=\"value\""};

    private final HttpAcceptorRule acceptor = new HttpAcceptorRule();

    private final TestRule trace = new MethodExecutionTrace();
    private final K3poRule k3po = new K3poRule();
    private final TestRule timeoutRule = new DisableOnDebug(new Timeout(5, SECONDS));

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
            setThreadingPolicy(new Synchroniser());
        }
    };

    @Rule
    public TestRule chain = RuleChain.outerRule(trace).around(acceptor).around(k3po).around(timeoutRule);

    @Test
    @Specification("client.no.credentials.unauthorized.empty.additional.challenges")
    public void testGatewayReturns401WithEmptyAdditionalChallengeDataWhenNoAuthenticationCredentialsProvided() throws Exception {
        final LoginContextFactory loginContextFactoryMock = context.mock(DefaultLoginContextFactory.class);
        final ResultAwareLoginContext loginContextMock = context.mock(ResultAwareLoginContext.class);
        final DefaultLoginResult loginResultMock = context.mock(DefaultLoginResult.class);
        final HttpRealmInfo[] realms = new HttpRealmInfo[1];
        realms[0] = new DefaultHttpRealmInfo(REALM_NAME, BASIC_CHALLENGE_SCHEME, REALM_NAME, new String[]{}, new String[]{},
                new String[]{}, loginContextFactoryMock, null);

        acceptor.getAcceptOptions().put("http.requiredRoles", new String[]{"*"});
        acceptor.getAcceptOptions().put("http.realms", realms);

        context.checking(new Expectations() {
            {

                oneOf(loginContextFactoryMock).createLoginContext(with(aNonNull(TypedCallbackHandlerMap.class)));
                will(returnValue(loginContextMock));
                oneOf(loginContextMock).login();
                // exception message needed to avoid NPE in HttpLoginSecurityFilter.loginMissingToken()
                will(throwException(new LoginException("Authentication required")));
                oneOf(loginContextMock).getLoginResult();
                will(returnValue(loginResultMock));
                oneOf(loginResultMock).getType();
                will(returnValue(LoginResult.Type.CHALLENGE));
                oneOf(loginResultMock).getLoginChallengeData();
                will(returnValue(null));
            }
        });

        acceptor.bind(BIND_ADDRESS, new IoHandlerAdapter<>());
        k3po.finish();
    }

    @Test
    @Specification("client.no.credentials.unauthorized.with.additional.challenges")
    public void testGatewayReturns401WithAdditionalChallengeDataWhenNoAuthenticationCredentialsProvided()
            throws Exception {
        final LoginContextFactory loginContextFactoryMock = context.mock(DefaultLoginContextFactory.class);
        final ResultAwareLoginContext loginContextMock = context.mock(ResultAwareLoginContext.class);
        final DefaultLoginResult loginResultMock = context.mock(DefaultLoginResult.class);
        final HttpRealmInfo[] realms = new HttpRealmInfo[1];
        realms[0] = new DefaultHttpRealmInfo(REALM_NAME, BASIC_CHALLENGE_SCHEME, REALM_NAME, new String[]{}, new String[]{},
                new String[]{}, loginContextFactoryMock, null);

        acceptor.getAcceptOptions().put("http.requiredRoles", new String[]{"*"});
        acceptor.getAcceptOptions().put("http.realms", realms);

        context.checking(new Expectations() {
            {

                oneOf(loginContextFactoryMock).createLoginContext(with(aNonNull(TypedCallbackHandlerMap.class)));
                will(returnValue(loginContextMock));
                oneOf(loginContextMock).login();
                // exception message needed to avoid NPE in HttpLoginSecurityFilter.loginMissingToken()
                will(throwException(new LoginException("Authentication required")));
                oneOf(loginContextMock).getLoginResult();
                will(returnValue(loginResultMock));
                oneOf(loginResultMock).getType();
                will(returnValue(LoginResult.Type.CHALLENGE));
                oneOf(loginResultMock).getLoginChallengeData();
                will(returnValue(ADDITIONAL_CHALLENGES));
            }
        });

        acceptor.bind(BIND_ADDRESS, new IoHandlerAdapter<>());
        k3po.finish();
    }

    @Test
    @Specification("client.forbidden")
    public void testGatewayReturnsHTTPStatus403WhenAuthenticationFailsAndNoAdditionalChallengesProvided() throws Exception {
        final LoginContextFactory loginContextFactoryMock = context.mock(DefaultLoginContextFactory.class);
        final ResultAwareLoginContext loginContextMock = context.mock(ResultAwareLoginContext.class);
        final DefaultLoginResult loginResultMock = context.mock(DefaultLoginResult.class);
        final HttpRealmInfo[] realms = new HttpRealmInfo[1];
        realms[0] = new DefaultHttpRealmInfo(REALM_NAME, BASIC_CHALLENGE_SCHEME, REALM_NAME, new String[]{}, new String[]{}, new String[]{},
                loginContextFactoryMock, null);

        acceptor.getAcceptOptions().put("http.requiredRoles", new String[]{"*"});
        acceptor.getAcceptOptions().put("http.realms", realms);

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

        acceptor.bind(BIND_ADDRESS, new IoHandlerAdapter<>());
        k3po.finish();
    }

    @Test
    @Specification("client.with.credentials.unauthorized.empty.additional.challenges")
    public void testGatewayReturnsHTTPStatus401WhenAuthenticationFailsButChallengeCalledWithNoData() throws Exception {
        final LoginContextFactory loginContextFactoryMock = context.mock(DefaultLoginContextFactory.class);
        final ResultAwareLoginContext loginContextMock = context.mock(ResultAwareLoginContext.class);
        final DefaultLoginResult loginResultMock = context.mock(DefaultLoginResult.class);
        final HttpRealmInfo[] realms = new HttpRealmInfo[1];
        realms[0] = new DefaultHttpRealmInfo(REALM_NAME, BASIC_CHALLENGE_SCHEME, REALM_NAME, new String[]{}, new String[]{}, new String[]{},
                loginContextFactoryMock, null);

        acceptor.getAcceptOptions().put("http.requiredRoles", new String[]{"*"});
        acceptor.getAcceptOptions().put("http.realms", realms);

        context.checking(new Expectations() {
            {

                oneOf(loginContextFactoryMock).createLoginContext(with(aNonNull(TypedCallbackHandlerMap.class)));
                will(returnValue(loginContextMock));
                oneOf(loginContextMock).login();
                will(throwException(new LoginException()));
                exactly(2).of(loginContextMock).getLoginResult();
                will(returnValue(loginResultMock));
                exactly(2).of(loginResultMock).getType();
                will(returnValue(LoginResult.Type.CHALLENGE));
                oneOf(loginResultMock).getLoginChallengeData();
                will(returnValue(null));
            }
        });

        acceptor.bind(BIND_ADDRESS, new IoHandlerAdapter<>());
        k3po.finish();
    }

    @Test
    @Specification("client.with.credentials.unauthorized.with.additional.challenges")
    public void testGatewayReturnsHTTPStatus401WhenAuthenticationFailsButChallengeCalledWithAdditionalData() throws Exception {
        final LoginContextFactory loginContextFactoryMock = context.mock(DefaultLoginContextFactory.class);
        final ResultAwareLoginContext loginContextMock = context.mock(ResultAwareLoginContext.class);
        final DefaultLoginResult loginResultMock = context.mock(DefaultLoginResult.class);
        final HttpRealmInfo[] realms = new HttpRealmInfo[1];
        realms[0] = new DefaultHttpRealmInfo(REALM_NAME, BASIC_CHALLENGE_SCHEME, REALM_NAME, new String[]{}, new String[]{}, new String[]{},
                loginContextFactoryMock, null);

        acceptor.getAcceptOptions().put("http.requiredRoles", new String[]{"*"});
        acceptor.getAcceptOptions().put("http.realms", realms);

        context.checking(new Expectations() {
            {

                oneOf(loginContextFactoryMock).createLoginContext(with(aNonNull(TypedCallbackHandlerMap.class)));
                will(returnValue(loginContextMock));
                oneOf(loginContextMock).login();
                will(throwException(new LoginException()));
                exactly(2).of(loginContextMock).getLoginResult();
                will(returnValue(loginResultMock));
                exactly(2).of(loginResultMock).getType();
                will(returnValue(LoginResult.Type.CHALLENGE));
                oneOf(loginResultMock).getLoginChallengeData();
                will(returnValue(ADDITIONAL_CHALLENGES));
            }
        });

        acceptor.bind(BIND_ADDRESS, new IoHandlerAdapter<>());
        k3po.finish();
    }

    @Test
    @Specification("client.forbidden")
    public void testGatewayReturnsHTTPStatus403WhenAuthenticationSucceedsAndAuthorizationFailsButNoAdditionalChallengesProvided() throws Exception {

        final LoginContextFactory loginContextFactoryMock = context.mock(DefaultLoginContextFactory.class);
        final ResultAwareLoginContext loginContextMock = context.mock(ResultAwareLoginContext.class);
        final DefaultLoginResult loginResultMock = context.mock(DefaultLoginResult.class);
        final HttpRealmInfo[] realms = new HttpRealmInfo[1];
        realms[0] = new DefaultHttpRealmInfo(REALM_NAME, BASIC_CHALLENGE_SCHEME, REALM_NAME, new String[]{}, new String[]{}, new String[]{},
                loginContextFactoryMock, null);

        acceptor.getAcceptOptions().put("http.requiredRoles", new String[]{"AUTHORIZED"});
        acceptor.getAcceptOptions().put("http.realms", realms);

        context.checking(new Expectations() {
            {

                oneOf(loginContextFactoryMock).createLoginContext(with(aNonNull(TypedCallbackHandlerMap.class)));
                will(returnValue(loginContextMock));
                oneOf(loginContextMock).login();
                oneOf(loginContextMock).getLoginResult();
                will(returnValue(loginResultMock));
                allowing(loginResultMock).getType();
                will(returnValue(LoginResult.Type.SUCCESS));
                oneOf(loginContextMock).getSubject();
                will(returnValue(new Subject()));
            }
        });

        acceptor.bind(BIND_ADDRESS, new IoHandlerAdapter<>());
        k3po.finish();
    }

    @Test
    @Specification("client.with.credentials.unauthorized.with.additional.challenges")
    public void testGatewayReturnsHTTPStatus401WhenAuthenticationSucceedsAndAuthorizationFailsButAdditionalChallengesProvided() throws Exception {

        final LoginContextFactory loginContextFactoryMock = context.mock(DefaultLoginContextFactory.class);
        final ResultAwareLoginContext loginContextMock = context.mock(ResultAwareLoginContext.class);
        final DefaultLoginResult loginResultMock = context.mock(DefaultLoginResult.class);
        final HttpRealmInfo[] realms = new HttpRealmInfo[1];
        realms[0] = new DefaultHttpRealmInfo(REALM_NAME, BASIC_CHALLENGE_SCHEME, REALM_NAME, new String[]{}, new String[]{}, new String[]{},
                loginContextFactoryMock, null);

        acceptor.getAcceptOptions().put("http.requiredRoles", new String[]{"AUTHORIZED"});
        acceptor.getAcceptOptions().put("http.realms", realms);

        context.checking(new Expectations() {
            {

                oneOf(loginContextFactoryMock).createLoginContext(with(aNonNull(TypedCallbackHandlerMap.class)));
                will(returnValue(loginContextMock));
                oneOf(loginContextMock).login();
                oneOf(loginContextMock).getLoginResult();
                will(returnValue(loginResultMock));
                allowing(loginResultMock).getType();
                will(returnValue(LoginResult.Type.CHALLENGE));
                oneOf(loginContextMock).getSubject();
                will(returnValue(new Subject()));
                oneOf(loginResultMock).getLoginChallengeData();
                will(returnValue(ADDITIONAL_CHALLENGES));
            }
        });

        acceptor.bind(BIND_ADDRESS, new IoHandlerAdapter<>());
        k3po.finish();
    }

}
