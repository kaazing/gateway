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

import java.security.Principal;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.security.auth.Subject;

import org.apache.mina.core.session.IoSession;
import org.kaazing.gateway.transport.SuspendableIoFilterAdapter;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.gateway.transport.http.bridge.HttpMessage;
import org.kaazing.gateway.transport.http.bridge.HttpRequestMessage;
import org.kaazing.gateway.transport.http.bridge.HttpResponseMessage;
import org.kaazing.mina.core.future.DefaultWriteFutureEx;
import org.kaazing.mina.core.write.DefaultWriteRequestEx;
import org.slf4j.Logger;

public abstract class HttpBaseSecurityFilter extends SuspendableIoFilterAdapter {
    protected static final String AUTHORIZATION_HEADER = "Authorization";
    protected static final String WWW_AUTHENTICATE_HEADER = "WWW-Authenticate";
    protected static final Subject SUBJECT_FORBIDDEN = new Subject();

    public static final String AUTHORIZATION_MODE_CHALLENGE = "challenge";

    protected final Logger logger;

    /**
     * Prefix to the authentication scheme to indicate that the Kaazing client application will handle the challenge rather than
     * delegate to the browser or the native platform.
     */
    public static final String AUTH_SCHEME_BASIC = "Basic";
    public static final String AUTH_SCHEME_NEGOTIATE = "Negotiate";
    public static final String AUTH_SCHEME_APPLICATION_TOKEN = "Application Token";

    public HttpBaseSecurityFilter() {
        this(null);
    }

    public HttpBaseSecurityFilter(Logger logger) {
        this.logger = logger;
    }

    public Logger getLogger() {
        return logger;
    }

    /**
     * Write a response back to the client (downstream) with a particular Http Status code.
     *
     * @param httpStatus the http status to write onto the response
     * @param httpBodyReason the http status reason to write into the <em>body</em> of the message
     * @param nextFilter the next filter to write the reply to
     * @param session    the client session
     * @param httpRequest the originating http request
     */
    protected void writeResponse(final HttpStatus httpStatus,
                                 final String httpBodyReason,
                                 final NextFilter nextFilter,
                                 final IoSession session,
                                 final HttpRequestMessage httpRequest) {
        // service context not found, create, for example, a HTTP 404 Not Found response
        HttpResponseMessage httpResponse = new HttpResponseMessage();
        httpResponse.setVersion(httpRequest.getVersion());
        httpResponse.setStatus(httpStatus);
        if (httpBodyReason != null) {
            httpResponse.setBodyReason(httpBodyReason);
        }
        nextFilter.filterWrite(session, new DefaultWriteRequestEx(httpResponse, new DefaultWriteFutureEx(session)));
    }

    /**
     * Write a response back to the client (downstream) with a particular Http Status code.
     *
     * @param httpStatus the http status to write onto the response
     * @param nextFilter the next filter to write the reply to
     * @param session    the client session
     * @param httpRequest the originating http request
     */
    protected void writeResponse(final HttpStatus httpStatus, final NextFilter nextFilter,
                                 final IoSession session, final HttpRequestMessage httpRequest) {
        writeResponse(httpStatus, null, nextFilter, session, httpRequest);
    }

    /**
     * Write a challenge response (http status code 401) from the server to the client.
     * The challenge is encoded in the "WWW-Authenticate" header of the provided httpResponse.
     *
     * @param httpResponse    the response contaiing the challenge and a 40 status code
     * @param nextFilter      the next filter to write the response with
     * @param session         the client session
     * @param httpChallengeScheme the challenge scheme for this session
     */
    protected void writeChallenge(final HttpResponseMessage httpResponse,
                                  final NextFilter nextFilter,
                                  final IoSession session,
                                  final String httpChallengeScheme) {
        if (logger != null && logger.isTraceEnabled()) {
            logger.trace("Sending HTTP challenge for auth scheme {}",
                    httpChallengeScheme);
        }
        nextFilter.filterWrite(session, new DefaultWriteRequestEx(httpResponse,
                                                                  new DefaultWriteFutureEx(session)));
    }

    protected Collection<String> getAuthorizedRoles(Subject subject) {
        Set<Principal> principals = subject.getPrincipals();
        Collection<String> authorizedRoles = new HashSet<>();
        for (Principal principal : principals) {
            authorizedRoles.add(principal.getName());
        }
        return authorizedRoles;
    }

    protected boolean loggerEnabled() {
        return logger != null && logger.isTraceEnabled();
    }

    protected boolean httpRequestMessageReceived(NextFilter nextFilter, IoSession session, Object message) throws Exception {
        if (HttpMessage.Kind.REQUEST != ((HttpMessage) message).getKind()) {
            super.doMessageReceived(nextFilter, session, message);
            return false;
        }

        assert HttpMessage.Kind.REQUEST == ((HttpMessage) message).getKind();
        return true;
    }

}

