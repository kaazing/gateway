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

import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_SEC_CHALLENGE_IDENTITY;

import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;

import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.http.HttpRealmInfo;
import org.kaazing.gateway.resource.address.http.HttpResourceAddress;
import org.kaazing.gateway.security.TypedCallbackHandlerMap;
import org.kaazing.gateway.security.auth.DefaultLoginResult;
import org.kaazing.gateway.security.auth.NamedSubjectCallbackHandler;
import org.kaazing.gateway.security.auth.context.ResultAwareLoginContext;
import org.kaazing.gateway.security.auth.token.DefaultAuthenticationToken;
import org.kaazing.gateway.server.spi.security.ExpiringState;
import org.kaazing.gateway.server.spi.security.NamedSubjectCallback;
import org.kaazing.gateway.transport.http.DefaultHttpSession;
import org.kaazing.gateway.transport.http.HttpCookie;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.gateway.transport.http.bridge.HttpMessage;
import org.kaazing.gateway.transport.http.bridge.HttpRequestMessage;
import org.kaazing.gateway.transport.http.bridge.HttpResponseMessage;
import org.kaazing.gateway.transport.http.security.auth.token.AuthenticationTokenExtractor;
import org.kaazing.gateway.transport.http.security.auth.token.DefaultAuthenticationTokenExtractor;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;
import org.kaazing.mina.core.session.IoSessionEx;
import org.slf4j.Logger;


public class HttpSubjectSecurityFilter extends HttpLoginSecurityFilter {

    /**
     * Prefix to the authentication scheme to indicate that the Kaazing client application will handle the challenge rather than
     * delegate to the browser or the native platform.
     */
    public static final String AUTH_SCHEME_APPLICATION_PREFIX = "Application ";
    public static final String AUTH_SCHEME_BASIC = "Basic";
    public static final String AUTH_SCHEME_NEGOTIATE = "Negotiate";

    static final AttributeKey NEW_SESSION_COOKIE_KEY = new AttributeKey(HttpSubjectSecurityFilter.class, "sessionCookie");

    private final AuthorizationMap authorizationMap;

    private ScheduledExecutorService scheduler;

    public HttpSubjectSecurityFilter() {
        this(null, null);
    }

    public HttpSubjectSecurityFilter(Logger logger) {
    	this(logger, null);
    }

    public HttpSubjectSecurityFilter(Logger logger, ExpiringState expiringState) {
        super(logger, expiringState);
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

        // Make sure we start with the subject from the underlying transport session in case it already has an authenticated subject
        // (e.g. we are httpxe and our transport is http or transport is SSL with a client certificate)
        if (httpRequest.getSubject() == null) {
            httpRequest.setSubject( ((IoSessionEx)session).getSubject() );
        }

        ResourceAddress httpAddress = httpRequest.getLocalAddress();
        HttpRealmInfo[] realms = httpAddress.getOption(HttpResourceAddress.REALMS);
        if (realms.length == 0) {
            ResultAwareLoginContext loginContext = null;
            // Make sure we propagate the login context from the layer below in httpxe case
            if (session instanceof DefaultHttpSession) {
                loginContext = ((DefaultHttpSession)session).getLoginContext();
            }
            if (loginContext != null) {
                httpRequest.setLoginContext(loginContext);
            }
            else {
                setUnprotectedLoginContext(httpRequest);
            }

            final boolean loggerIsEnabled = logger != null && logger.isTraceEnabled();
            if (loggerIsEnabled) {
                logger.trace("HttpSubjectSecurityFilter skipped because no realm is configured.");
            }
            super.doMessageReceived(nextFilter, session, message);
            return;
        }

        securityMessageReceived(nextFilter, session, httpRequest);
    }

    @Override
    protected void writeSessionCookie(IoSession session, HttpRequestMessage httpRequest, DefaultLoginResult loginResult) {
        // secure requests always have cookie accessible, even
        // on first access
        final HttpCookie newSessionCookie = (HttpCookie) loginResult.getLoginAuthorizationAttachment();

        httpRequest.addCookie(newSessionCookie);

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
        
        if (alreadyLoggedIn(httpRequest)) {
            // KG-3232, KG-3267: we should never leave the login context unset
            // for unprotected services.
            if (httpRequest.getLoginContext() == null) {
                setUnprotectedLoginContext(httpRequest);
            }
            if (loggerIsEnabled) {
                logger.trace("HttpSubjectSecurityFilter skipped because we are already allowed or logged in.");
            }

            super.doMessageReceived(nextFilter, session, message);
            return;
        }

        HttpRealmInfo[] realms = httpAddress.getOption(HttpResourceAddress.REALMS);
        if (realms.length == 0) {
            setUnprotectedLoginContext(httpRequest);
            if (loggerIsEnabled) {
                logger.trace("HttpSecurityStandardFilter skipped because no realm is configured.");
            }
            super.doMessageReceived(nextFilter, session, message);
            return;
        }
        
        String challengeIdentity = httpRequest.getHeader(HEADER_SEC_CHALLENGE_IDENTITY);
        LoginContext[] loginContexts = getLoginContexts(challengeIdentity);
        int realmIndex = findCurrentRealm(loginContexts);
        HttpRealmInfo realm = realms[realmIndex];

        AuthenticationTokenExtractor tokenExtractor = DefaultAuthenticationTokenExtractor.INSTANCE;

        //
        // Login using the token; if login fails, the appropriate reply has already been sent from this filter
        // so stop the filter chain here.
        //

        DefaultAuthenticationToken authToken = (DefaultAuthenticationToken) tokenExtractor.extract(httpRequest, realm);

        // If the client request provided authentication data which has
        // a challenge scheme, make sure that the client-sent challenge
        // scheme matches what we expect.  If not, it's a badly formatted
        // request, and the client should be informed of this.

        String clientChallengeScheme = authToken.getScheme();
        String expectedChallengeScheme = getBaseAuthScheme(realm.getChallengeScheme());

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

        TypedCallbackHandlerMap additionalCallbacks = null;
        if (realmIndex > 0) {
            additionalCallbacks = new TypedCallbackHandlerMap();
            Function<String, Subject> subjects = name -> findNamedSubject(name, realms, realmIndex, loginContexts);
            NamedSubjectCallbackHandler callbackHandler = new NamedSubjectCallbackHandler(subjects);
            additionalCallbacks.put(NamedSubjectCallback.class, callbackHandler);
        }

        // Schedule LoginContext.login() execution using a separate thread
        LoginContextTask loginContextTask = new LoginContextTask(nextFilter, session, httpRequest, authToken, additionalCallbacks, realms, realmIndex, loginContexts);
        scheduler.execute(loginContextTask);
    }

    private Subject findNamedSubject(String name, HttpRealmInfo[] realms, int realmIndex, LoginContext[] loginContexts) {
        for (int i = realmIndex - 1; i >= 0; i--) {
            if (realms[i].getName().equals(name)) {
                return loginContexts[i].getSubject();
            }
        }
        return null;
    }

    protected static int findCurrentRealm(LoginContext[] loginContexts) {
        if (loginContexts == null || loginContexts.length == 0) {
            return 0;
        }

        for (int i = 0; i < loginContexts.length; i++) {
            if (loginContexts[i] == null) {
                return i;
            }
        }

        return 0;
    }

	// Task for running LoginContext.login() in a separate thread(other than I/O thread)
    private final class LoginContextTask implements Runnable {
        private final NextFilter nextFilter;
        private final IoSession session;
        private final HttpRequestMessage httpRequest;
        private final DefaultAuthenticationToken authToken;
        private final TypedCallbackHandlerMap additionalCallbacks;
        private final long createdTime;
        private final HttpRealmInfo[] realms;
        private final int realmStartAt;
        private final LoginContext[] loginContexts;

        LoginContextTask(NextFilter nextFilter, IoSession session, HttpRequestMessage httpRequest,
                         DefaultAuthenticationToken authToken, TypedCallbackHandlerMap additionalCallbacks,
                         HttpRealmInfo[] realms, int realmIndex, LoginContext[] loginContexts) {
            this.nextFilter = nextFilter;
            this.session = session;
            this.httpRequest = httpRequest;
            this.authToken = authToken;
            this.additionalCallbacks = additionalCallbacks;
            this.createdTime = System.currentTimeMillis();
            this.realms = realms;
            this.realmStartAt = realmIndex;
            this.loginContexts = loginContexts != null ? loginContexts : new LoginContext[realms.length];
        }

        @Override
        public void run() {
            if (loggerEnabled()) {
                logger.trace("Executing login task %d ms after scheduling for session %s",
                        (System.currentTimeMillis() - createdTime) , session);
            }

            boolean succeeded = true;
            for (int realmIndex = realmStartAt; succeeded && realmIndex < realms.length; realmIndex++) {
                succeeded &= login(nextFilter, session, httpRequest, authToken, additionalCallbacks, realms, realmIndex, loginContexts);
            }

            //
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
