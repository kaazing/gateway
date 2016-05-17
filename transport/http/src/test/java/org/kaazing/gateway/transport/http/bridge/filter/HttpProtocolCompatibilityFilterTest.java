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

import static org.jmock.Expectations.returnValue;
import static org.junit.Assert.assertSame;
import static org.kaazing.gateway.transport.http.HttpMethod.GET;
import static org.kaazing.gateway.transport.http.HttpMethod.POST;
import static org.kaazing.gateway.transport.http.HttpStatus.SERVER_NOT_IMPLEMENTED;
import static org.kaazing.gateway.transport.http.HttpVersion.HTTP_1_1;
import static org.kaazing.gateway.transport.http.bridge.HttpContentMessage.EMPTY;

import java.net.URI;
import java.util.HashMap;
import java.util.List;

import javax.security.auth.Subject;

import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.session.IoSession;
import org.jmock.lib.concurrent.Synchroniser;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Test;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.security.auth.context.ResultAwareLoginContext;
import org.kaazing.gateway.transport.BridgeSession;
import org.kaazing.gateway.transport.http.DefaultHttpSession;
import org.kaazing.gateway.transport.http.HttpAcceptSession;
import org.kaazing.gateway.transport.http.HttpMethod;
import org.kaazing.gateway.transport.http.bridge.HttpRequestMessage;
import org.kaazing.gateway.transport.http.bridge.HttpResponseMessage;
import org.kaazing.gateway.transport.http.bridge.filter.HttpProtocolCompatibilityFilter.HttpElevateEmulatedRequestFilter;
import org.kaazing.gateway.transport.test.Expectations;
import org.kaazing.mina.core.filterchain.DefaultIoFilterChain;
import org.kaazing.test.util.Mockery;

public class HttpProtocolCompatibilityFilterTest {

    private Mockery context = new Mockery() {{
        setImposteriser(ClassImposteriser.INSTANCE);
    }};
    private HttpAcceptSession serverSession = context.mock(HttpAcceptSession.class);
    private DefaultIoFilterChain filterChain = context.mock(DefaultIoFilterChain.class);
    private NextFilter nextFilter = context.mock(NextFilter.class);
    private ResourceAddress localAddress = context.mock(ResourceAddress.class);

    @Test
    public void shouldRemoveRandomNumberQueryParam() throws Exception {
        context.setThreadingPolicy(new Synchroniser());

        final HttpRequestMessage expectedRequest = new HttpRequestMessage();
        expectedRequest.setVersion(HTTP_1_1);
        expectedRequest.setMethod(GET);
        expectedRequest.setRequestURI(URI.create("/path?param=value"));

        context.checking(new Expectations() { {
            oneOfUnconditionalWrappedResponseRequiredCheck(this);
            oneOfIsEmulatedWebSocketRequest(this, expectedRequest);

            oneOf(nextFilter).messageReceived(with(serverSession), with(equal(expectedRequest)));
        } });

        HttpRequestMessage httpRequest = new HttpRequestMessage();
        httpRequest.setVersion(HTTP_1_1);
        httpRequest.setMethod(GET);
        httpRequest.setRequestURI(URI.create("/path?param=value&.krn=12345"));

        HttpProtocolCompatibilityFilter filter = new HttpProtocolCompatibilityFilter();
        filter.messageReceived(nextFilter, serverSession, httpRequest);

        context.assertIsSatisfied();
    }

    @Test
    public void shouldRemoveValidHttpVersionHeader() throws Exception {
        context.setThreadingPolicy(new Synchroniser());

        final HttpRequestMessage expectedRequest = new HttpRequestMessage();
        expectedRequest.setVersion(HTTP_1_1);
        expectedRequest.setMethod(POST);
        expectedRequest.setRequestURI(URI.create("/path"));
        expectedRequest.setContent(EMPTY);

        context.checking(new Expectations() { {
            oneOfUnconditionalWrappedResponseRequiredCheck(this);
            oneOfIsEmulatedWebSocketRequest(this, expectedRequest);

            oneOf(nextFilter).messageReceived(with(serverSession), with(equal(expectedRequest)));
        } });

        HttpRequestMessage httpRequest = new HttpRequestMessage();
        httpRequest.setVersion(HTTP_1_1);
        httpRequest.setMethod(POST);
        httpRequest.setRequestURI(URI.create("/path"));
        httpRequest.setHeader("X-Http-Version", "httpe-1.0");
        httpRequest.setContent(EMPTY);

        HttpProtocolCompatibilityFilter filter = new HttpProtocolCompatibilityFilter();
        filter.messageReceived(nextFilter, serverSession, httpRequest);

        context.assertIsSatisfied();
    }

    @Test
    public void shouldRejectInvalidHttpVersionHeader() throws Exception {
        context.setThreadingPolicy(new Synchroniser());

        final HttpResponseMessage expectedResponse = new HttpResponseMessage();
        expectedResponse.setVersion(HTTP_1_1);
        expectedResponse.setStatus(SERVER_NOT_IMPLEMENTED);
        expectedResponse.setReason("Http-Version not supported");

        final HttpRequestMessage httpRequest = new HttpRequestMessage();
        httpRequest.setVersion(HTTP_1_1);
        httpRequest.setMethod(POST);
        httpRequest.setRequestURI(URI.create("/path"));
        httpRequest.setHeader("X-Http-Version", "unrecognized");

        context.checking(new Expectations() { {
            oneOfUnconditionalWrappedResponseRequiredCheck(this);
            oneOfIsEmulatedWebSocketRequest(this, httpRequest);

            oneOf(nextFilter).filterWrite(with(serverSession), with(hasMessage(expectedResponse)));
            oneOf(nextFilter).filterClose(with(serverSession));
        } });


        HttpProtocolCompatibilityFilter filter = new HttpProtocolCompatibilityFilter();
        filter.messageReceived(nextFilter, serverSession, httpRequest);

        context.assertIsSatisfied();
    }

    @Test
    public void shouldNotRejectMissingHttpVersionHeader() throws Exception {
        context.setThreadingPolicy(new Synchroniser());

        final HttpRequestMessage expectedRequest = new HttpRequestMessage();
        expectedRequest.setVersion(HTTP_1_1);
        expectedRequest.setMethod(POST);
        expectedRequest.setRequestURI(URI.create("/path"));
        expectedRequest.setContent(EMPTY);

        context.checking(new Expectations() { {
            oneOfUnconditionalWrappedResponseRequiredCheck(this);
            oneOfIsEmulatedWebSocketRequest(this, expectedRequest);


            oneOf(nextFilter).messageReceived(with(serverSession), with(equal(expectedRequest)));
        } });

        HttpRequestMessage httpRequest = new HttpRequestMessage();
        httpRequest.setVersion(HTTP_1_1);
        httpRequest.setMethod(POST);
        httpRequest.setRequestURI(URI.create("/path"));
        httpRequest.setContent(EMPTY);

        HttpProtocolCompatibilityFilter filter = new HttpProtocolCompatibilityFilter();
        filter.messageReceived(nextFilter, serverSession, httpRequest);

        context.assertIsSatisfied();
    }

    @Test
    public void shouldNotRejectRedundantHttpVersionHeader() throws Exception {
        context.setThreadingPolicy(new Synchroniser());

        final HttpRequestMessage expectedRequest = new HttpRequestMessage();
        expectedRequest.setVersion(HTTP_1_1);
        expectedRequest.setMethod(POST);
        expectedRequest.setRequestURI(URI.create("/path"));
        expectedRequest.setHeader("X-Http-Version", "unrecognized");
        expectedRequest.setHeader("X-Next-Protocol", "explicit");
        expectedRequest.setContent(EMPTY);

        context.checking(new Expectations() { {
            oneOfUnconditionalWrappedResponseRequiredCheck(this);
            oneOfIsEmulatedWebSocketRequest(this, expectedRequest);

            oneOf(nextFilter).messageReceived(with(serverSession), with(equal(expectedRequest)));
        } });

        HttpRequestMessage httpRequest = new HttpRequestMessage();
        httpRequest.setVersion(HTTP_1_1);
        httpRequest.setMethod(POST);
        httpRequest.setRequestURI(URI.create("/path"));
        httpRequest.setHeader("X-Http-Version", "unrecognized");
        httpRequest.setHeader("X-Next-Protocol", "explicit");
        httpRequest.setContent(EMPTY);

        HttpProtocolCompatibilityFilter filter = new HttpProtocolCompatibilityFilter();
        filter.messageReceived(nextFilter, serverSession, httpRequest);

        context.assertIsSatisfied();
    }

    @Test
    public void shouldNotOverrideNextProtocolBasedOnXOriginHeader() throws Exception {
        context.setThreadingPolicy(new Synchroniser());

        final HttpRequestMessage expectedRequest = new HttpRequestMessage();
        expectedRequest.setVersion(HTTP_1_1);
        expectedRequest.setMethod(POST);
        expectedRequest.setRequestURI(URI.create("/path"));
        expectedRequest.setHeader("X-Next-Protocol", "other");
        expectedRequest.setHeader("X-Origin", "http://localhost:8000");
        expectedRequest.setContent(EMPTY);

        context.checking(new Expectations() { {
            oneOfUnconditionalWrappedResponseRequiredCheck(this);
            oneOfIsEmulatedWebSocketRequest(this, expectedRequest);

            oneOf(nextFilter).messageReceived(with(serverSession), with(equal(expectedRequest)));
        } });

        HttpRequestMessage httpRequest = new HttpRequestMessage();
        httpRequest.setVersion(HTTP_1_1);
        httpRequest.setMethod(POST);
        httpRequest.setRequestURI(URI.create("/path"));
        httpRequest.setHeader("X-Next-Protocol", "other");
        httpRequest.setHeader("X-Origin", "http://localhost:8000");
        httpRequest.setContent(EMPTY);

        HttpProtocolCompatibilityFilter filter = new HttpProtocolCompatibilityFilter();
        filter.messageReceived(nextFilter, serverSession, httpRequest);

        context.assertIsSatisfied();
    }

    @Test
    public void shouldNotOverrideNextProtocolBasedOnOriginQueryParam() throws Exception {
        context.setThreadingPolicy(new Synchroniser());

        final HttpRequestMessage expectedRequest = new HttpRequestMessage();
        expectedRequest.setVersion(HTTP_1_1);
        expectedRequest.setMethod(POST);
        expectedRequest.setRequestURI(URI.create("/path?.ko=http://localhost:8000"));
        expectedRequest.setHeader("X-Next-Protocol", "other");
        expectedRequest.setContent(EMPTY);

        context.checking(new Expectations() { {
            oneOfUnconditionalWrappedResponseRequiredCheck(this);
            oneOfIsEmulatedWebSocketRequest(this, expectedRequest);

            oneOf(nextFilter).messageReceived(with(serverSession), with(equal(expectedRequest)));
        } });

        HttpRequestMessage httpRequest = new HttpRequestMessage();
        httpRequest.setVersion(HTTP_1_1);
        httpRequest.setMethod(POST);
        httpRequest.setRequestURI(URI.create("/path?.ko=http://localhost:8000"));
        httpRequest.setHeader("X-Next-Protocol", "other");
        httpRequest.setContent(EMPTY);

        HttpProtocolCompatibilityFilter filter = new HttpProtocolCompatibilityFilter();
        filter.messageReceived(nextFilter, serverSession, httpRequest);

        context.assertIsSatisfied();
    }

    @Test
    public void shouldDeriveCreateEncodingFromCreateBinaryPathExtension() throws Exception {
        context.setThreadingPolicy(new Synchroniser());

        final HttpRequestMessage expectedRequest = new HttpRequestMessage();
        expectedRequest.setVersion(HTTP_1_1);
        expectedRequest.setMethod(POST);
        final URI requestURI = URI.create("/path/;e/cb");
        expectedRequest.setRequestURI(requestURI);
        expectedRequest.setHeader("X-Create-Encoding", "binary");
        expectedRequest.setHeader("X-Next-Protocol", "wse/1.0");
        expectedRequest.setContent(EMPTY);

        context.checking(new Expectations() { {
            oneOfUnconditionalWrappedResponseRequiredCheck(this);
            oneOfIsEmulatedWebSocketRequest(this, expectedRequest);

            allowing(localAddress).getResource();
            will(returnValue(requestURI));

            oneOf(nextFilter).messageReceived(with(serverSession), with(equal(expectedRequest)));
        }

        });

        HttpRequestMessage httpRequest = new HttpRequestMessage();
        httpRequest.setVersion(HTTP_1_1);
        httpRequest.setMethod(POST);
        httpRequest.setRequestURI(requestURI);
        httpRequest.setContent(EMPTY);

        HttpProtocolCompatibilityFilter filter = new HttpProtocolCompatibilityFilter();
        filter.messageReceived(nextFilter, serverSession, httpRequest);

        context.assertIsSatisfied();
    }


    @Test
    public void shouldDeriveCreateEncodingFromCreateTextPathExtension() throws Exception {
        context.setThreadingPolicy(new Synchroniser());

        final HttpRequestMessage expectedRequest = new HttpRequestMessage();
        expectedRequest.setVersion(HTTP_1_1);
        expectedRequest.setMethod(POST);
        final URI requestURI = URI.create("/path/;e/ct");
        expectedRequest.setRequestURI(requestURI);
        expectedRequest.setHeader("X-Create-Encoding", "text");
        expectedRequest.setHeader("X-Next-Protocol", "wse/1.0");
        expectedRequest.setContent(EMPTY);

        context.checking(new Expectations() { {
            oneOfUnconditionalWrappedResponseRequiredCheck(this);
            oneOfIsEmulatedWebSocketRequest(this, expectedRequest);

            allowing(localAddress).getResource();
            will(returnValue(requestURI));

            oneOf(nextFilter).messageReceived(with(serverSession), with(equal(expectedRequest)));
        } });

        HttpRequestMessage httpRequest = new HttpRequestMessage();
        httpRequest.setVersion(HTTP_1_1);
        httpRequest.setMethod(POST);
        httpRequest.setRequestURI(requestURI);
        httpRequest.setContent(EMPTY);

        HttpProtocolCompatibilityFilter filter = new HttpProtocolCompatibilityFilter();
        filter.messageReceived(nextFilter, serverSession, httpRequest);

        context.assertIsSatisfied();
    }

    @Test
    public void shouldDeriveCreateEncodingFromCreateTextEscapedPathExtension() throws Exception {
        context.setThreadingPolicy(new Synchroniser());

        final HttpRequestMessage expectedRequest = new HttpRequestMessage();
        expectedRequest.setVersion(HTTP_1_1);
        expectedRequest.setMethod(POST);
        final URI requestURI = URI.create("/path/;e/cte");
        expectedRequest.setRequestURI(requestURI);
        expectedRequest.setHeader("X-Create-Encoding", "text-escaped");
        expectedRequest.setHeader("X-Next-Protocol", "wse/1.0");
        expectedRequest.setContent(EMPTY);

        context.checking(new Expectations() { {
            oneOfUnconditionalWrappedResponseRequiredCheck(this);
            oneOfIsEmulatedWebSocketRequest(this, expectedRequest);

            allowing(localAddress).getResource();
            will(returnValue(requestURI));

            oneOf(nextFilter).messageReceived(with(serverSession), with(equal(expectedRequest)));
        } });

        HttpRequestMessage httpRequest = new HttpRequestMessage();
        httpRequest.setVersion(HTTP_1_1);
        httpRequest.setMethod(POST);
        httpRequest.setRequestURI(requestURI);
        httpRequest.setContent(EMPTY);

        HttpProtocolCompatibilityFilter filter = new HttpProtocolCompatibilityFilter();
        filter.messageReceived(nextFilter, serverSession, httpRequest);

        context.assertIsSatisfied();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void asElevatedRequestShouldPropagateSubjectAndLoginContext() throws Exception {
        final DefaultHttpSession session = context.mock(DefaultHttpSession.class, "session");
        Subject subject = new Subject();
        final ResultAwareLoginContext loginContext = context.mock(ResultAwareLoginContext.class, "loginContext");

        final HttpRequestMessage expectedRequest = new HttpRequestMessage();
        expectedRequest.setVersion(HTTP_1_1);
        expectedRequest.setMethod(POST);
        final URI requestURI = URI.create("/path/;e/cte");
        expectedRequest.setRequestURI(requestURI);
        expectedRequest.setContent(EMPTY);

        context.checking(new Expectations() { {
            oneOf(session).getVersion(); will(returnValue(HTTP_1_1));
            oneOf(session).getMethod(); will(returnValue(POST));
            oneOf(session).getParameters();
            oneOf(session).getRequestURI(); will(returnValue(requestURI));
            oneOf(session).isSecure();
            oneOf(session).getReadCookies();
            oneOf(session).getReadHeader("Content-Length"); will(returnValue(null));
            allowing(session).getReadHeaders(); will(returnValue(new HashMap<String, List<String>>()));
            oneOf(session).getSubject(); will(returnValue(subject));
            oneOf(session).getLoginContext(); will(returnValue(loginContext));
            oneOf(session).setReadHeaders(with(any(HashMap.class)));
        } });


        HttpRequestMessage request = HttpElevateEmulatedRequestFilter.asElevatedRequest(session);
        assertSame(subject, request.getSubject());
        assertSame(loginContext, request.getLoginContext());

        context.assertIsSatisfied();
    }

    private void oneOfIsEmulatedWebSocketRequest(Expectations exp, HttpRequestMessage expectedRequest) {
        exp.allowing(serverSession).getRequestURI();
        exp.will(returnValue(expectedRequest.getRequestURI()));
    }

    private void oneOfUnconditionalWrappedResponseRequiredCheck(Expectations exp) {
        exp.allowing(serverSession).getAttribute(BridgeSession.LOCAL_ADDRESS);
        exp.will(returnValue(localAddress));
        exp.allowing(serverSession).getReadHeader("X-Origin");
        exp.will(returnValue(null));
        exp.allowing(serverSession).getReadHeader("X-Next-Protocol");
        exp.will(returnValue(null));
        exp.allowing(serverSession).getParameter(".knp");
        exp.will(returnValue(null));
        exp.allowing(serverSession).getParameter(".kv");
        exp.will(returnValue("10.05"));
        exp.allowing(serverSession).getParameter(".kl");
        exp.will(returnValue("Y"));
        exp.allowing(serverSession).getParameter(".kac");
        exp.will(returnValue(null));
        ((IoSession)exp.allowing(serverSession)).getLocalAddress();
        exp.will(returnValue(localAddress));
        exp.allowing(serverSession).getLocalAddress();
        exp.will(returnValue(localAddress));
        exp.allowing(localAddress).getOption(ResourceAddress.NEXT_PROTOCOL);
        exp.will(returnValue("httpxe/1.1"));
        exp.allowing(serverSession).getMethod();
        exp.will(returnValue(HttpMethod.POST));

        exp.allowing(serverSession).setMethod(HttpMethod.POST);
        exp.allowing(serverSession).getFilterChain();
        exp.will(returnValue(filterChain));
        exp.allowing(filterChain).addBefore(exp.with(Expectations.any(String.class)),
                                         exp.with(Expectations.any(String.class)),
                                         exp.with(Expectations.any(org.apache.mina.core.filterchain.IoFilter.class)));
    }
}
