/**
 * Copyright (c) 2007-2014 Kaazing Corporation. All rights reserved.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.kaazing.gateway.transport.http.bridge.filter;


import static java.lang.String.format;
import static org.kaazing.gateway.transport.BridgeSession.REMOTE_ADDRESS;
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_FORWARDED;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.URI;
import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

import javax.security.auth.Subject;

import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.http.HttpResourceAddress;
import org.kaazing.gateway.security.TypedCallbackHandlerMap;
import org.kaazing.gateway.security.auth.DefaultLoginResult;
import org.kaazing.gateway.security.auth.RealmNameCallback;
import org.kaazing.gateway.security.auth.RealmNameCallbackHandler;
import org.kaazing.gateway.security.auth.token.DefaultAuthenticationToken;
import org.kaazing.gateway.transport.http.HttpCookie;
import org.kaazing.gateway.transport.http.HttpProtocol;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.gateway.transport.http.HttpUtils;
import org.kaazing.gateway.transport.http.MutableHttpCookie;
import org.kaazing.gateway.transport.http.bridge.HttpMessage;
import org.kaazing.gateway.transport.http.bridge.HttpRequestMessage;
import org.kaazing.gateway.transport.http.bridge.HttpResponseMessage;
import org.kaazing.gateway.transport.http.security.AuthorizationMapCallback;
import org.kaazing.gateway.transport.http.security.AuthorizationMapCallbackHandler;
import org.kaazing.gateway.transport.http.security.HttpRequestMessageCallback;
import org.kaazing.gateway.transport.http.security.HttpRequestMessageCallbackHandler;
import org.kaazing.gateway.transport.http.security.auth.token.AuthenticationTokenExtractor;
import org.kaazing.gateway.transport.http.security.auth.token.DefaultAuthenticationTokenExtractor;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;
import org.kaazing.mina.core.session.IoSessionEx;
import org.slf4j.Logger;


public class HttpSubjectSecurityFilter extends HttpLoginSecurityFilter {

    public static final String NAME = HttpProtocol.NAME + "#security";

    public static final String SESSION_COOKIE_NAME = "KSESSIONID";

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String WWW_AUTHENTICATE_HEADER = "WWW-Authenticate";

    /**
     * Prefix to the authentication scheme to indicate that the Kaazing client application will handle the challenge rather than
     * delegate to the browser or the native platform.
     */
    public static final String AUTH_SCHEME_APPLICATION_PREFIX = "Application ";
    public static final String AUTH_SCHEME_BASIC = "Basic";
    public static final String AUTH_SCHEME_NEGOTIATE = "Negotiate";

    private static final String HEADER_FORWARDED_REMOTE_IP_ADDRESS = "for=%s";

    static final AttributeKey NEW_SESSION_COOKIE_KEY = new AttributeKey(HttpSubjectSecurityFilter.class, "sessionCookie");

    private final AuthorizationMap authorizationMap;

    private ScheduledExecutorService scheduler;

    public HttpSubjectSecurityFilter() {
        this(null);
    }

    public HttpSubjectSecurityFilter(Logger logger) {
        super(logger);
        // Each filter has it's own map.  There's only one filter though.
        // Reset the map when the filter is constructed to allow an embedded gateway to repeatedly launch
        // (e.g. for integration tests)
        authorizationMap = new AuthorizationMap();
    }

    public void setSchedulerProvider(SchedulerProvider provider) {
        this.scheduler = provider.getScheduler("loginmodule", false);
    }

    // --------------------------------------------------------
    // Security code for subject-security LEGACY


    @Override
    public void doMessageReceived(NextFilter nextFilter, IoSession session, Object message) throws Exception {
        // GL.debug("http", getClass().getSimpleName() + " request received.");

        if (! httpRequestMessageReceived(nextFilter, session, message)) return;

        HttpRequestMessage httpRequest = (HttpRequestMessage) message;
        final boolean loggerIsEnabled = logger != null && logger.isTraceEnabled();

        String forwarded = httpRequest.getHeader(HEADER_FORWARDED);
        if ((forwarded == null) || (forwarded.length() == 0)) {
            String remoteIpAddress = null;
            ResourceAddress resourceAddress = REMOTE_ADDRESS.get(session);
            ResourceAddress tcpResourceAddress = resourceAddress.findTransport("tcp");

            if (tcpResourceAddress != null) {
                URI resource = tcpResourceAddress.getResource();
                remoteIpAddress = resource.getHost();

                if (loggerIsEnabled) {
                    logger.trace(format("HttpSubjectSecurityFilter: Remote IP Address: '%s'", remoteIpAddress));
                }
            }

            if (remoteIpAddress != null) {
                httpRequest.setHeader(HEADER_FORWARDED, format(HEADER_FORWARDED_REMOTE_IP_ADDRESS, remoteIpAddress));
            }
        }

        // Make sure we start with the subject from the underlying transport session in case it already has an authenticated subject
        // (e.g. we are httpxe and our transport is http or transport is SSL with a client certificate)
        if (httpRequest.getSubject() == null) {
            httpRequest.setSubject( ((IoSessionEx)session).getSubject() );
        }

        ResourceAddress httpAddress = httpRequest.getLocalAddress();
        String realmName = httpAddress.getOption(HttpResourceAddress.REALM_NAME);

        if ( realmName == null ) {
            setUnprotectedLoginContext(session);
            if (loggerIsEnabled) {
                logger.trace("HttpSubjectSecurityFilter skipped because no realm is configured.");
            }
            super.doMessageReceived(nextFilter, session, message);
            return;
        }

        String authorizationMode = httpAddress.getOption(HttpResourceAddress.REALM_AUTHORIZATION_MODE);

        if ("recycle".equals(authorizationMode)) {
            legacySecurityMessageReceived(nextFilter, session, httpRequest);
        }

        if (!"recycle".equals(authorizationMode)) {
            securityMessageReceived(nextFilter, session, httpRequest);
        }
    }

    void legacySecurityMessageReceived(NextFilter nextFilter, IoSession session, Object message) throws Exception {

        if (! httpRequestMessageReceived(nextFilter, session, message)) return;

        HttpRequestMessage httpRequest = (HttpRequestMessage) message;
        ResourceAddress httpAddress = httpRequest.getLocalAddress();

        String realmName = httpAddress.getOption(HttpResourceAddress.REALM_NAME);
        String realmAuthorizationMode = httpAddress.getOption(HttpResourceAddress.REALM_AUTHORIZATION_MODE);
        String realmChallengeScheme = httpAddress.getOption(HttpResourceAddress.REALM_CHALLENGE_SCHEME);

        //
        // Grab the authorization header/session cookie if any
        //
        String authorization = httpRequest.getHeader(AUTHORIZATION_HEADER);
        MutableHttpCookie sessionCookie = getSessionCookie(httpRequest);

        final boolean loggerIsEnabled = logger != null && logger.isTraceEnabled();
        if (loggerIsEnabled) {
            logger.trace("HTTP session cookie received: {}", sessionCookie);
            logger.trace("HTTP authorization header received: {}", authorization);
        }

        if (! AUTHORIZATION_MODE_RECYCLE.equals(realmAuthorizationMode)) {
            if (loggerIsEnabled) {
                logger.trace("HttpSubjectSecurityFilter skipped because authorization mode is not recycle.");
            }
            super.doMessageReceived(nextFilter, session, message);
            return;
        }

        if ( alreadyLoggedIn(session, httpAddress)) {
            // KG-3232, KG-3267: we should never leave the login context unset
            // for unprotected services.
            if (LOGIN_CONTEXT_KEY.get(session) == null) {
                setUnprotectedLoginContext(session);
            }
            if (loggerIsEnabled) {
                logger.trace("HttpSubjectSecurityFilter skipped because we are already logged in.");
            }
            super.doMessageReceived(nextFilter, session, message);
            return;
        }

        DefaultAuthenticationToken authToken = new DefaultAuthenticationToken();
        authToken.setScheme(getBaseAuthScheme(realmChallengeScheme));
        authToken.add(AUTHORIZATION_HEADER, authorization);
        if ( sessionCookie != null ) {
            authToken.add(SESSION_COOKIE_NAME, sessionCookie.getValue());
        }

        TypedCallbackHandlerMap additionalCallbacks = new TypedCallbackHandlerMap();
        additionalCallbacks.put(RealmNameCallback.class, new RealmNameCallbackHandler(realmName));
        additionalCallbacks.put(AuthorizationMapCallback.class, new AuthorizationMapCallbackHandler(authorizationMap));
        additionalCallbacks.put(HttpRequestMessageCallback.class, new HttpRequestMessageCallbackHandler(httpRequest, HttpRequestMessageCallback.class));

        if (loggerIsEnabled) {
            logger.trace("HttpSubjectSecurityFilter calling login().");
        }

        // Suspend incoming events into this filter. Will resume after LoginContext.login() completion
        suspendIncoming(session);

        // Schedule LoginContext.login() execution using a separate thread
        LoginContextTask loginContextTask = new LoginContextTask(nextFilter, session, httpRequest, authToken, additionalCallbacks);
        scheduler.execute(loginContextTask);
    }

    protected MutableHttpCookie getSessionCookie(HttpRequestMessage httpRequest) {
        MutableHttpCookie sessionCookie = null;
        Set<HttpCookie> cookies = httpRequest.getCookies();
        for (HttpCookie cookie : cookies) {
            if (SESSION_COOKIE_NAME.equals(cookie.getName())) {
                sessionCookie = (MutableHttpCookie) cookie;
                break;
            }
        }
        return sessionCookie;
    }


    protected void writeSessionCookie(IoSession session, HttpRequestMessage httpRequest, DefaultLoginResult loginResult) {
        MutableHttpCookie sessionCookie = getSessionCookie(httpRequest);

        // secure requests always have cookie accessible, even
        // on first access
        final HttpCookie newSessionCookie = (HttpCookie) loginResult.getLoginAuthorizationAttachment();

        httpRequest.addCookie(newSessionCookie);

        // if old cookie is still present, remove it from the cookies set
        if (sessionCookie != null) {
            httpRequest.removeCookie(sessionCookie);
        }

        session.setAttribute(NEW_SESSION_COOKIE_KEY, newSessionCookie);
        if (loggerEnabled()) {
           logger.trace("Sending session cookie {}", newSessionCookie);
       }
    }

    @Override
    public void filterWrite(NextFilter nextFilter, IoSession session, WriteRequest writeRequest) throws Exception {

        // include new session cookie in response
        Object message = writeRequest.getMessage();
        HttpMessage httpMessage = (HttpMessage) message;
        switch (httpMessage.getKind()) {
        case RESPONSE:
            HttpResponseMessage httpResponse = (HttpResponseMessage) httpMessage;
            HttpCookie sessionCookie = (HttpCookie) session.removeAttribute(NEW_SESSION_COOKIE_KEY);
            if (sessionCookie != null) {
                httpResponse.addCookie(sessionCookie);
            }
            break;
        default:
            break;
        }

        super.filterWrite(nextFilter, session, writeRequest);
    }


    @Override
    public void exceptionCaught(NextFilter nextFilter, IoSession session, Throwable cause) throws Exception {
        if (loggerEnabled()) {
            logger.trace("Caught exception.", cause);
        }
        super.exceptionCaught(nextFilter, session, cause);
    }

    @Override
    public void destroy() throws Exception {
        super.destroy();
    }

    /**
     * <strong>For testing only</strong>
     *
     * Allows for the authorizationMap to be accessed from unit tests.
     *
     */
    AuthorizationMap getAuthorizationMap() {
        return authorizationMap;
    }

    /**
     * Captures the notion of a Subject object being valid for a certain time (e.g. inactivity-timeout).
     */
    public static class TimedCredential {
        private Subject subject;
        private Long expirationTimestamp;

        public TimedCredential(Subject subject, Long expirationTimestamp) {
            if (subject == null) {
                throw new IllegalArgumentException("subject was null");
            }
            this.subject = subject;
            this.expirationTimestamp = expirationTimestamp;
        }

        public Subject getSubject() {
            return subject;
        }

        public boolean hasExpirationTimestamp() {
            return expirationTimestamp != null;
        }

        public Long getExpirationTimestamp() {
            return expirationTimestamp;
        }

        public void setExpirationTimestamp(Long expirationTimestamp) {
            this.expirationTimestamp = expirationTimestamp;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[TimedCredential: Subject(");
            for ( Principal p: subject.getPrincipals()) {
                sb.append(p.getName()).append('/');
            }
            if ( subject.getPrincipals().size()>0) {
                sb.deleteCharAt(sb.length()-1);
            }
            sb.append(") ");
            if ( expirationTimestamp != null ) {
                String expires = new SimpleDateFormat("yyyyMMdd HH:mm:ss").format(new Date(expirationTimestamp*1000L));
                sb.append("; expires on ").append(expires);
            }
            sb.append(" ]");
            return sb.toString();
        }
    }


    /**
     * Maintain a mapping of authorization key strings, to which subject they correspond
     * and for how long the mapping is valid for read.
     * <p/>
     * In addition, establish a reverse mapping from Subject to authorization key.
     * <p/>
     * Combined, this allows one to lookup, validation expiration by authorization key, and also to
     * clear the authentication map by Subject as well.
     */
    public static class AuthorizationMap {

        private Map<String, TimedCredential> keyToTimedCredentialMap = new ConcurrentHashMap<>();
        private Map<Subject, String> subjectToKeyMap = new ConcurrentHashMap<>();


        // For testing
        TimedCredential get(String key) {
            return keyToTimedCredentialMap.get(key);
        }


        public TimedCredential get(String realmName, String key) {
            return keyToTimedCredentialMap.get(realmName + key);
        }

        public synchronized void put(String realmName, String key, TimedCredential value) {
            keyToTimedCredentialMap.put(realmName + key, value);
            subjectToKeyMap.put(value.subject, realmName + key);
        }

        public synchronized TimedCredential removeByKey(String realmName, String key) {
            TimedCredential removedValue = keyToTimedCredentialMap.remove(realmName + key);
            if (removedValue != null && removedValue.subject != null) {
                subjectToKeyMap.remove(removedValue.subject);
            }
            return removedValue;
        }

        public synchronized String removeBySubject(Subject subject) {
            String removedKey = subjectToKeyMap.remove(subject);
            if (removedKey != null) {
                keyToTimedCredentialMap.remove(removedKey);
            }
            return removedKey;
        }

        public boolean containsKey(String key) {
            return keyToTimedCredentialMap.containsKey(key);
        }

        public boolean containsSubject(Subject subject) {
            return subjectToKeyMap.containsKey(subject);
        }

        public String getKey(Subject subject) {
            return subjectToKeyMap.get(subject);
        }

        public int size() {
            return keyToTimedCredentialMap.size();
        }
    }


    // --------------------------------------------------------
    // Security code for subject-security going forward

    void securityMessageReceived(NextFilter nextFilter, IoSession session, Object message) throws Exception {
        final boolean loggerIsEnabled = logger != null && logger.isTraceEnabled();

        if (! httpRequestMessageReceived(nextFilter, session, message)) return;

        HttpRequestMessage httpRequest = (HttpRequestMessage) message;

        ResourceAddress httpAddress = httpRequest.getLocalAddress();

        String realmName = httpAddress.getOption(HttpResourceAddress.REALM_NAME);
        String realmAuthorizationMode = httpAddress.getOption(HttpResourceAddress.REALM_AUTHORIZATION_MODE);
        String realmChallengeScheme = httpAddress.getOption(HttpResourceAddress.REALM_CHALLENGE_SCHEME);

        if ( alreadyLoggedIn(session, httpAddress)) {
            // KG-3232, KG-3267: we should never leave the login context unset
            // for unprotected services.
            if (LOGIN_CONTEXT_KEY.get(session) == null) {
                setUnprotectedLoginContext(session);
            }
            if (loggerIsEnabled) {
                logger.trace("HttpSubjectSecurityFilter skipped because we are already allowed or logged in.");
            }

            super.doMessageReceived(nextFilter, session, message);
            return;
        }

        //
        // This filter method cannot handle "recycle" realms.
        //

        if (realmAuthorizationMode != null && AUTHORIZATION_MODE_RECYCLE.equals(realmAuthorizationMode)) {
            if (loggerEnabled()) {
                logger.trace(String.format("Cannot process authorization mode 'recycle' for requested URI '%s'.", HttpUtils.getRequestURI(httpRequest, session)));
            }
            writeResponse(HttpStatus.CLIENT_BAD_REQUEST, nextFilter, session, httpRequest);
            return;
        }

        if ( realmName == null ) {
            setUnprotectedLoginContext(session);
            if (loggerIsEnabled) {
                logger.trace("HttpSecurityStandardFilter skipped because no realm is configured.");
            }
            super.doMessageReceived(nextFilter, session, message);
            return;
        }


        AuthenticationTokenExtractor tokenExtractor = DefaultAuthenticationTokenExtractor.INSTANCE;

        //
        // Login using the token; if login fails, the appropriate reply has already been sent from this filter
        // so stop the filter chain here.
        //

        DefaultAuthenticationToken authToken = (DefaultAuthenticationToken) tokenExtractor.extract(httpRequest);

        // If the client request provided authentication data which has
        // a challenge scheme, make sure that the client-sent challenge
        // scheme matches what we expect.  If not, it's a badly formatted
        // request, and the client should be informed of this.

        String clientChallengeScheme = authToken.getScheme();
        String expectedChallengeScheme = getBaseAuthScheme(realmChallengeScheme);

        if (clientChallengeScheme != null &&
                clientChallengeScheme.equals(expectedChallengeScheme) == false) {
            if (loggerEnabled()) {
                logger.trace(String.format("A websocket request used the '%s' challenge scheme when we expected the '%s' challenge scheme", clientChallengeScheme, expectedChallengeScheme));
            }

            String reason = String.format("Expected challenge scheme '%s' not found", expectedChallengeScheme);
            writeResponse(HttpStatus.CLIENT_BAD_REQUEST, reason, nextFilter, session, httpRequest);
            return;
        }

        // Now set the expected challenge scheme on the AuthToken.  If the
        // client provided a scheme, the above check ensures that the
        // provided scheme matches our expected scheme, so calling setScheme()
        // does not harm anything.  If the client did NOT provide a scheme,
        // this properly sets one, for the benefit of login modules which
        // check for such things.
        authToken.setScheme(expectedChallengeScheme);

        // Suspend incoming events into this filter. Will resume after LoginContext.login() completion
        suspendIncoming(session);

        // Schedule LoginContext.login() execution using a separate thread
        LoginContextTask loginContextTask = new LoginContextTask(nextFilter, session, httpRequest, authToken, null);
        scheduler.execute(loginContextTask);
    }

    // Task for running LoginContext.login() in a separate thread(other than I/O thread)
    private final class LoginContextTask implements Runnable {
        private final NextFilter nextFilter;
        private final IoSession session;
        private final HttpRequestMessage httpRequest;
        private final DefaultAuthenticationToken authToken;
        private final TypedCallbackHandlerMap additionalCallbacks;
        private final long createdTime;

        LoginContextTask(NextFilter nextFilter, IoSession session, HttpRequestMessage httpRequest,
                         DefaultAuthenticationToken authToken, TypedCallbackHandlerMap additionalCallbacks) {
            this.nextFilter = nextFilter;
            this.session = session;
            this.httpRequest = httpRequest;
            this.authToken = authToken;
            this.additionalCallbacks = additionalCallbacks;
            this.createdTime = System.currentTimeMillis();
        }

        @Override
        public void run() {
            if (loggerEnabled()) {
                logger.trace("Executing login task %d ms after scheduling for session %s",
                        (System.currentTimeMillis() - createdTime) , session);
            }
            boolean succeeded = login(nextFilter, session, httpRequest, authToken, additionalCallbacks);
            try {
                if (succeeded) {
                    // Complete the rest of the filter chain
                    HttpSubjectSecurityFilter.super.doMessageReceived(nextFilter, session, httpRequest);
                }
                // If there are any events buffered during suspension, resume them
                HttpSubjectSecurityFilter.super.resumeIncoming(session);
            } catch (Exception e) {
                session.getFilterChain().fireExceptionCaught(e);
            }
            if (loggerEnabled()) {
                logger.trace("Finished login task after %d ms for session %s",
                        (System.currentTimeMillis() - createdTime), session);
            }
        }
    }

}
