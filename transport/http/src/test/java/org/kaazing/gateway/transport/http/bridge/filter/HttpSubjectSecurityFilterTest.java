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
package org.kaazing.gateway.transport.http.bridge.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URI;
import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;

import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.jmock.lib.action.VoidAction;
import org.jmock.lib.concurrent.Synchroniser;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Test;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.http.DefaultHttpRealmInfo;
import org.kaazing.gateway.resource.address.http.HttpRealmInfo;
import org.kaazing.gateway.resource.address.http.HttpResourceAddress;
import org.kaazing.gateway.resource.address.tcp.TcpResourceAddressFactorySpi;
import org.kaazing.gateway.security.LoginContextFactory;
import org.kaazing.gateway.security.TypedCallbackHandlerMap;
import org.kaazing.gateway.security.auth.DefaultLoginResult;
import org.kaazing.gateway.security.auth.context.DefaultLoginContextFactory;
import org.kaazing.gateway.security.auth.context.ResultAwareLoginContext;
import org.kaazing.gateway.server.spi.security.LoginResult;
import org.kaazing.gateway.transport.http.HttpMethod;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.gateway.transport.http.HttpVersion;
import org.kaazing.gateway.transport.http.bridge.HttpRequestMessage;
import org.kaazing.gateway.transport.http.bridge.HttpResponseMessage;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.mina.core.write.DefaultWriteRequestEx;
import org.slf4j.LoggerFactory;

public class HttpSubjectSecurityFilterTest {

    private static final Executor HTTP_SUBJECT_SECURITY_FILTER_TEST_EXECUTOR = new Executor() {
        @Override
        public void execute(Runnable task) {
            new Thread(task, "HttpSubjectSecurityFilterTest").start();
        }

        @Override
        public String toString() {
            return "HttpSubjectSecurityFilterTestExecutor";
        }
    };

    public HttpSubjectSecurityFilterTest() {
    }

    @Test
    public void testNonHttpRequestMessage() throws Exception {
        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
        context.setThreadingPolicy(new Synchroniser());
        final NextFilter nextFilter = context.mock(NextFilter.class);
        final IoSession session = context.mock(IoSession.class);
        final HttpResponseMessage message = new HttpResponseMessage();
        context.checking(new Expectations() {
            {
                oneOf(nextFilter).messageReceived(session, message);
            }
        });
        HttpSubjectSecurityFilter filter = new HttpSubjectSecurityFilter();
        filter.securityMessageReceived(nextFilter, session, message);
        context.assertIsSatisfied();
    }

    @Test
    public void testGetWithoutAuthorization() throws Exception {
        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
        context.setThreadingPolicy(new Synchroniser());
        final NextFilter nextFilter = context.mock(NextFilter.class);
        final IoSession session = context.mock(IoSession.class);
        final HttpRequestMessage message = new HttpRequestMessage();
        message.setMethod(HttpMethod.GET);
        final URI serviceURI = URI.create("http://localhost:8000/echo");
        message.setRequestURI(serviceURI);
        message.addHeader("Host", "localhost:8000");
        message.setLoginContext(HttpLoginSecurityFilter.LOGIN_CONTEXT_OK);
        final ResourceAddress address = context.mock(ResourceAddress.class);
        message.setLocalAddress(address);
        context.checking(new Expectations() {
            {
                allowing(address).getOption(HttpResourceAddress.REALMS);
                will(returnValue(new HttpRealmInfo[0]));

                allowing(address).getOption(HttpResourceAddress.REQUIRED_ROLES);
                will(returnValue(null));

                oneOf(nextFilter).messageReceived(session, message);
            }
        });
        HttpSubjectSecurityFilter filter = new HttpSubjectSecurityFilter();
        filter.securityMessageReceived(nextFilter, session, message);
        context.assertIsSatisfied();
    }

    @Test
    public void testGetWithFakeAuthorization() throws Exception {
        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
        context.setThreadingPolicy(new Synchroniser());
        final NextFilter nextFilter = context.mock(NextFilter.class);
        final IoSession session = context.mock(IoSession.class);
        final HttpRequestMessage message = new HttpRequestMessage();
        final URI serviceURI = URI.create("http://localhost:8000/echo");
        message.setMethod(HttpMethod.GET);
        message.setRequestURI(serviceURI);
        message.addHeader("Host", "localhost:8000");
        message.setHeader("Authorization", "gobbledegook");
        final ResourceAddress address = context.mock(ResourceAddress.class);
        message.setLocalAddress(address);

        context.checking(new Expectations() {
            {

                allowing(address).getOption(HttpResourceAddress.REALMS);
                will(returnValue(new HttpRealmInfo[0]));

                allowing(address).getOption(HttpResourceAddress.REQUIRED_ROLES);
                will(returnValue(null));

                oneOf(nextFilter).messageReceived(session, message);
            }
        });
        HttpSubjectSecurityFilter filter = new HttpSubjectSecurityFilter();
        filter.securityMessageReceived(nextFilter, session, message);
        assertEquals(HttpLoginSecurityFilter.LOGIN_CONTEXT_OK, message.getLoginContext());
        context.assertIsSatisfied();
    }

    public static final String BASE_URI = "http://localhost:8000/dirservice";
    public static final Principal AUTHORIZED_PRINCIPAL = new Principal() {
        @Override
        public String getName() {
            return "AUTHORIZED";
        }
    };

    @Test
    public void filterShouldPassThroughIfMessageIsNotHttpRequestMessage() throws Exception {
        Mockery context = new Mockery() {{setImposteriser(ClassImposteriser.INSTANCE);}};
        context.setThreadingPolicy(new Synchroniser());
        final NextFilter nextFilter = context.mock(NextFilter.class);
        final IoSession session = context.mock(IoSession.class);
        final HttpResponseMessage message = new HttpResponseMessage();
        context.checking(new Expectations() {
            {
                oneOf(nextFilter).messageReceived(session, message);
            }
        });
        HttpSubjectSecurityFilter filter = new HttpSubjectSecurityFilter();
        filter.securityMessageReceived(nextFilter, session, message);
        context.assertIsSatisfied();
    }

    @Test
    public void filterShouldPassRequestThroughIfNoRolesAreRequired() throws Exception {
        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
        context.setThreadingPolicy(new Synchroniser());
        final NextFilter nextFilter = context.mock(NextFilter.class);
        final IoSession session = context.mock(IoSession.class);
        final ResourceAddress address = context.mock(ResourceAddress.class);
        final HttpRequestMessage message = new HttpRequestMessage();

        final URI serviceURI = URI.create("ws://localhost:8001/echo");
        message.setRequestURI(serviceURI);
        message.addHeader("Host", "localhost:8000");
        message.addHeader("Connection", "upgrade");
        message.setLocalAddress(address);

        context.checking(new Expectations() {
            {
                allowing(address).getOption(HttpResourceAddress.REALMS);
                will(returnValue(new HttpRealmInfo[0]));

                allowing(address).getOption(HttpResourceAddress.REQUIRED_ROLES);
                will(returnValue(new String[]{}));

                oneOf(nextFilter).messageReceived(session, message);
                never(session).setAttribute(with(any(String.class)), with(any(Subject.class)));
            }
        });
        HttpSubjectSecurityFilter filter = new HttpSubjectSecurityFilter(LoggerFactory.getLogger("security"));
        filter.securityMessageReceived(nextFilter, session, message);
        assertEquals(HttpLoginSecurityFilter.LOGIN_CONTEXT_OK, message.getLoginContext());
        context.assertIsSatisfied();
    }

    @Test
    public void filterShouldPassThroughIfNoServiceSecurityRealmIsConfigured() throws Exception {
        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
        context.setThreadingPolicy(new Synchroniser());
        final NextFilter nextFilter = context.mock(NextFilter.class);
        final IoSessionEx session = context.mock(IoSessionEx.class);
        final HttpRequestMessage message = new HttpRequestMessage();
        message.setMethod(HttpMethod.GET);
        message.setVersion(HttpVersion.HTTP_1_1);
        message.setRequestURI(URI.create(BASE_URI));
        message.addHeader("Connection", "Upgrade");
        message.addHeader("Upgrade", "WebSocket");
        message.addHeader("Host", "localhost:8000");
        message.addHeader("Authorization", "gobbledegook");

        final ResourceAddress address = context.mock(ResourceAddress.class);
        message.setLocalAddress(address);


        context.checking(new Expectations() {
            {
                allowing(address).getOption(HttpResourceAddress.REALMS);
                will(returnValue(new HttpRealmInfo[0]));

                // alreadyLoggedIn == false
                allowing(address).getOption(HttpResourceAddress.REQUIRED_ROLES);
                will(returnValue(new String[]{"ADMINISTRATOR"}));

                // pass through
                oneOf(nextFilter).messageReceived(session, message);
                never(session).setAttribute(with(any(String.class)), with(any(Subject.class)));

            }
        });
        HttpSubjectSecurityFilter filter = new HttpSubjectSecurityFilter();
        filter.securityMessageReceived(nextFilter, session, message);

        // KG-3232, KG-3267: we should never leave the login context unset
        assertEquals(HttpLoginSecurityFilter.LOGIN_CONTEXT_OK, message.getLoginContext());

        context.assertIsSatisfied();
    }

    @Test
    public void filterShouldPassThroughWhenLoginSucceeds() throws Exception {
        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
                setThreadingPolicy(new Synchroniser());
            }
        };
        context.setThreadingPolicy(new Synchroniser());
        final NextFilter nextFilter = context.mock(NextFilter.class);
        final IoSessionEx session = context.mock(IoSessionEx.class);
        final ResourceAddress address = context.mock(ResourceAddress.class);
        TcpResourceAddressFactorySpi factory = new TcpResourceAddressFactorySpi();
        String addressURI = "tcp://localhost:2020";
        ResourceAddress tcpResourceAddress = factory.newResourceAddress(addressURI);

        final HttpRequestMessage message = new HttpRequestMessage();
        message.setMethod(HttpMethod.GET);
        message.setVersion(HttpVersion.HTTP_1_1);
        message.setRequestURI(URI.create(BASE_URI));

        message.addHeader("Connection", "Upgrade");
        message.addHeader("Upgrade", "WebSocket");
        message.addHeader("Host", "localhost:8000");
        message.addHeader("Authorization", "Token gobbledegook");
        message.setLocalAddress(address);

        final ResultAwareLoginContext loginContext = context.mock(ResultAwareLoginContext.class);
        final LoginContextFactory loginContextFactory = context.mock(DefaultLoginContextFactory.class);
        final DefaultLoginResult loginResult  = context.mock(DefaultLoginResult.class);

        final Set<Principal> principals = new HashSet<>();
        principals.add(AUTHORIZED_PRINCIPAL);
        final Subject subject = new Subject(false, principals, Collections.EMPTY_SET, Collections.EMPTY_SET);

        final HttpSubjectSecurityFilter filter = new HttpSubjectSecurityFilter();
        filter.setSchedulerProvider(new SchedulerProvider());

        final CountDownLatch latch = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(session).getRemoteAddress(); will(returnValue(tcpResourceAddress));

                allowing(address).getOption(HttpResourceAddress.REALMS);
                final HttpRealmInfo[] realms = new HttpRealmInfo[1];
                realms[0] = new DefaultHttpRealmInfo("demo", "Application Token", null, new String[]{"foo"},  new String[]{}, new String[]{}, loginContextFactory, null);
                will(returnValue(realms));

                allowing(address).getOption(HttpResourceAddress.REQUIRED_ROLES);
                will(returnValue(new String[]{"AUTHORIZED"}));

                // not already logged in, DPW -- Not sure why I needed to change this to allowing, double check
                allowing(session).getSubject();

                // login() method itself
                oneOf(loginContextFactory).createLoginContext(with(aNonNull(TypedCallbackHandlerMap.class)));
                will(returnValue(loginContext));

                oneOf(session).suspendRead();

                oneOf(loginContext).login();
                will(VoidAction.INSTANCE);

                oneOf(loginContext).getLoginResult();
                will(returnValue(loginResult));

                oneOf(loginResult).getType();
                will(returnValue(LoginResult.Type.SUCCESS));

                oneOf(loginResult).hasLoginAuthorizationAttachment();
                will(returnValue(false));

                atMost(2).of(loginContext).getSubject();
                will(returnValue(subject));


                oneOf(nextFilter).messageReceived(session, message);

                oneOf(session).getIoExecutor();
                will(returnValue(HTTP_SUBJECT_SECURITY_FILTER_TEST_EXECUTOR));

                oneOf(session).resumeRead();
                will(new LoginContextTaskDoneAction(latch, "login context task done"));
            }
        });
        filter.messageReceived(nextFilter, session, message);
        latch.await(2000, TimeUnit.MILLISECONDS);
        assertNotNull(message.getLoginContext());
        context.assertIsSatisfied();
    }

    @Test
    public void filterShouldPassThroughWhenLoginSucceedsWithForwardedHeader() throws Exception {
        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
                setThreadingPolicy(new Synchroniser());
            }
        };
        context.setThreadingPolicy(new Synchroniser());
        final NextFilter nextFilter = context.mock(NextFilter.class);
        final IoSessionEx session = context.mock(IoSessionEx.class);
        final ResourceAddress address = context.mock(ResourceAddress.class);

        final HttpRequestMessage message = new HttpRequestMessage();
        message.setMethod(HttpMethod.GET);
        message.setVersion(HttpVersion.HTTP_1_1);
        message.setRequestURI(URI.create(BASE_URI));

        message.addHeader("Connection", "Upgrade");
        message.addHeader("Upgrade", "WebSocket");
        message.addHeader("Host", "localhost:8000");
        message.addHeader("Forwarded", "for=\"[0:0:0:0:0:0:0:1]:8000\";by=198.51.100.17;proto=http");
        message.addHeader("Authorization", "Token gobbledegook");
        message.setLocalAddress(address);

        final ResultAwareLoginContext loginContext = context.mock(ResultAwareLoginContext.class);
        final LoginContextFactory loginContextFactory = context.mock(DefaultLoginContextFactory.class);
        final DefaultLoginResult loginResult  = context.mock(DefaultLoginResult.class);

        final Set<Principal> principals = new HashSet<>();
        principals.add(AUTHORIZED_PRINCIPAL);
        final Subject subject = new Subject(false, principals, Collections.EMPTY_SET, Collections.EMPTY_SET);

        final HttpSubjectSecurityFilter filter = new HttpSubjectSecurityFilter();
        filter.setSchedulerProvider(new SchedulerProvider());

        final CountDownLatch latch = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                oneOf(session).getSubject(); will(returnValue(null));

                allowing(address).getOption(HttpResourceAddress.REQUIRED_ROLES);
                will(returnValue(new String[]{"AUTHORIZED"}));

                allowing(address).getOption(HttpResourceAddress.REALMS);
                final HttpRealmInfo[] realms = new HttpRealmInfo[1];
                realms[0] = new DefaultHttpRealmInfo("demo", "Application Token", null, new String[]{"foo"},  new String[]{}, new String[]{}, loginContextFactory, null);
                will(returnValue(realms));

                oneOf(loginContextFactory).createLoginContext(with(aNonNull(TypedCallbackHandlerMap.class)));
                will(returnValue(loginContext));

                oneOf(session).suspendRead();

                oneOf(loginContext).login();
                will(VoidAction.INSTANCE);

                oneOf(loginContext).getLoginResult();
                will(returnValue(loginResult));

                oneOf(loginResult).getType();
                will(returnValue(LoginResult.Type.SUCCESS));

                oneOf(loginResult).hasLoginAuthorizationAttachment();
                will(returnValue(false));

                atMost(2).of(loginContext).getSubject();
                will(returnValue(subject));


                oneOf(nextFilter).messageReceived(session, message);

                oneOf(session).getIoExecutor();
                will(returnValue(HTTP_SUBJECT_SECURITY_FILTER_TEST_EXECUTOR));

                oneOf(session).resumeRead();
                will(new LoginContextTaskDoneAction(latch, "login context task done"));
            }
        });
        filter.messageReceived(nextFilter, session, message);
        latch.await(200000, TimeUnit.MILLISECONDS);
        assertNotNull(message.getLoginContext());
        context.assertIsSatisfied();
    }

    @Test
    public void filterShouldEndChainWhenLoginFailsHard() throws Exception {
        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
                setThreadingPolicy(new Synchroniser());
            }
        };
        context.setThreadingPolicy(new Synchroniser());
        final NextFilter nextFilter = context.mock(NextFilter.class);
        final IoSessionEx session = context.mock(IoSessionEx.class);
        final ResourceAddress address = context.mock(ResourceAddress.class);

        final HttpRequestMessage message = new HttpRequestMessage();
        message.setMethod(HttpMethod.GET);
        message.setVersion(HttpVersion.HTTP_1_1);
        message.setRequestURI(URI.create(BASE_URI));
        message.addHeader("Connection", "Upgrade");
        message.addHeader("Upgrade", "WebSocket");
        message.addHeader("Host", "localhost:8000");
        message.addHeader("Forwarded", "for=127.0.0.1");
        message.addHeader("Authorization", "Token gobbledegook");
        message.setLocalAddress(address);

        final ResultAwareLoginContext loginContext = context.mock(ResultAwareLoginContext.class);
        final LoginContextFactory loginContextFactory = context.mock(DefaultLoginContextFactory.class);

        final CountDownLatch latch = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                allowing(address).getOption(HttpResourceAddress.REALMS);
                final HttpRealmInfo[] realms = new HttpRealmInfo[1];
                realms[0] = new DefaultHttpRealmInfo("demo", "Application Token", null, new String[]{"foo"},  new String[]{}, new String[]{}, loginContextFactory, null);
                will(returnValue(realms));

                allowing(address).getOption(HttpResourceAddress.REQUIRED_ROLES);
                will(returnValue(new String[]{"AUTHORIZED"}));

                oneOf(loginContextFactory).createLoginContext(with(aNonNull(TypedCallbackHandlerMap.class)));
                will(returnValue(loginContext));
                oneOf(session).suspendRead();
                oneOf(loginContext).login();
                will(throwException(new LoginException()));
                oneOf(nextFilter).filterWrite(with(same(session)),
                        with(writeRequest(withStatus(HttpStatus.CLIENT_FORBIDDEN))));
                will(VoidAction.INSTANCE);

                never(nextFilter).messageReceived(session, message);
                never(session).setAttribute(with(any(String.class)), with(any(Subject.class)));
                oneOf(session).getIoExecutor();
                will(returnValue(HTTP_SUBJECT_SECURITY_FILTER_TEST_EXECUTOR));
                oneOf(session).resumeRead();
                will(new LoginContextTaskDoneAction(latch, "login context task done"));
            }
        });
        HttpSubjectSecurityFilter filter = new HttpSubjectSecurityFilter();
        filter.setSchedulerProvider(new SchedulerProvider());
        filter.securityMessageReceived(nextFilter, session, message);
        latch.await(2000, TimeUnit.MILLISECONDS);
        context.assertIsSatisfied();
    }

    @Test
    public void filterShouldEndChainWhenLoginFailsSoft() throws Exception {
        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
                setThreadingPolicy(new Synchroniser());

            }
        };
        context.setThreadingPolicy(new Synchroniser());
        final NextFilter nextFilter = context.mock(NextFilter.class);
        final IoSessionEx session = context.mock(IoSessionEx.class);
        final ResourceAddress address = context.mock(ResourceAddress.class);

        final HttpRequestMessage message = new HttpRequestMessage();
        message.setMethod(HttpMethod.GET);
        message.setVersion(HttpVersion.HTTP_1_1);
        message.setRequestURI(URI.create(BASE_URI));
        message.addHeader("Connection", "Upgrade");
        message.addHeader("Upgrade", "WebSocket");
        message.addHeader("Host", "localhost:8000");
        message.addHeader("Forwarded", "for=127.0.0.1");
        message.addHeader("Authorization", "Token gobbledegook");
        message.setLocalAddress(address);

        final ResultAwareLoginContext loginContext = context.mock(ResultAwareLoginContext.class);
        final DefaultLoginResult loginResult = context.mock(DefaultLoginResult.class);
        final LoginContextFactory loginContextFactory = context.mock(DefaultLoginContextFactory.class);

        final Set<Principal> principals = new HashSet<>();
        principals.add(AUTHORIZED_PRINCIPAL);
        final Subject subject = new Subject(false, principals, Collections.EMPTY_SET, Collections.EMPTY_SET);
        final Matcher<WriteRequest> writeRequestMatcher =
                writeRequest(withStatus(HttpStatus.CLIENT_FORBIDDEN));

        final CountDownLatch latch = new CountDownLatch(1);

        context.checking(new Expectations() {
            {
                allowing(address).getOption(HttpResourceAddress.REALMS);
                final HttpRealmInfo[] realms = new HttpRealmInfo[1];
                realms[0] = new DefaultHttpRealmInfo("demo", "Application Token", null, new String[]{"foo"},  new String[]{}, new String[]{}, loginContextFactory, null);
                will(returnValue(realms));

                allowing(address).getOption(HttpResourceAddress.REQUIRED_ROLES);
                will(returnValue(new String[]{"AUTHORIZED"}));

                oneOf(loginContextFactory).createLoginContext(with(aNonNull(TypedCallbackHandlerMap.class)));
                will(returnValue(loginContext));
                oneOf(session).suspendRead();
                oneOf(loginContext).login();
                oneOf(loginContext).getLoginResult();
                will(returnValue(loginResult));
                oneOf(loginResult).getType();
                will(returnValue(LoginResult.Type.CHALLENGE));
                oneOf(loginContext).getSubject();
                will(returnValue(subject));
                oneOf(loginResult).getLoginChallengeData();
                will(returnValue(null));
                oneOf(nextFilter).filterWrite(with(same(session)), with(writeRequestMatcher));
                will(VoidAction.INSTANCE);
                never(nextFilter).messageReceived(session, message);
                never(session).setAttribute(with(any(String.class)), with(any(Subject.class)));
                oneOf(session).getIoExecutor();
                will(returnValue(HTTP_SUBJECT_SECURITY_FILTER_TEST_EXECUTOR));
                oneOf(session).resumeRead();
                will(new LoginContextTaskDoneAction(latch, "login context task done"));
            }
        });
        HttpSubjectSecurityFilter filter = new HttpSubjectSecurityFilter();
        filter.setSchedulerProvider(new SchedulerProvider());
        filter.securityMessageReceived(nextFilter, session, message);
        latch.await(2000, TimeUnit.MILLISECONDS);
        context.assertIsSatisfied();
    }



    private WriteRequest withStatus(final HttpStatus httpStatus) {
        return new DefaultWriteRequestEx(httpResponseWith(httpStatus));
    }

    private HttpResponseMessage httpResponseWith(final HttpStatus httpStatus) {
        final HttpResponseMessage httpResponse = new HttpResponseMessage();
        httpResponse.setStatus(httpStatus);
        httpResponse.setVersion(HttpVersion.HTTP_1_1);
        return httpResponse;
    }

    private Matcher<WriteRequest> writeRequest(final WriteRequest writeRequest) {
        return new BaseMatcher<WriteRequest>() {
            @Override
            public boolean matches(Object o) {
                if (o != null &&
                        o instanceof WriteRequest) {
                    if ( writeRequest.getMessage().equals(((WriteRequest) o).getMessage()) ){
                        return true;
                    }
                }
                return false;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(writeRequest.toString());
            }
        };
    }

    private static final class LoginContextTaskDoneAction extends CustomAction {
        private final CountDownLatch latch;

        LoginContextTaskDoneAction(CountDownLatch latch, String description) {
            super(description);
            this.latch = latch;
        }

        @Override
        public Object invoke(Invocation invocation) throws Throwable {
            latch.countDown();
            return null;
        }
    }

}
