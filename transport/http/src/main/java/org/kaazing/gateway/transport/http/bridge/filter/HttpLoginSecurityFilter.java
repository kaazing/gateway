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

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.kaazing.gateway.resource.address.ResourceAddress.NEXT_PROTOCOL;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.REQUIRED_ROLES;
import static org.kaazing.gateway.server.spi.security.LoginResult.Type.SUCCESS;
import static org.kaazing.gateway.transport.BridgeSession.LOCAL_ADDRESS;
import static org.kaazing.gateway.transport.BridgeSession.REMOTE_ADDRESS;
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_FORWARDED;
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_SEC_CHALLENGE_IDENTITY;
import static org.kaazing.gateway.transport.http.HttpHeaders.HEADER_WWW_AUTHENTICATE;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.Subject;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.mina.core.session.IoSession;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.http.HttpRealmInfo;
import org.kaazing.gateway.resource.address.http.HttpResourceAddress;
import org.kaazing.gateway.security.LoginContextFactory;
import org.kaazing.gateway.security.TypedCallbackHandlerMap;
import org.kaazing.gateway.security.auth.AuthenticationTokenCallbackHandler;
import org.kaazing.gateway.security.auth.DefaultLoginResult;
import org.kaazing.gateway.security.auth.InetAddressCallbackHandler;
import org.kaazing.gateway.security.auth.YesLoginModule;
import org.kaazing.gateway.security.auth.context.ResultAwareLoginContext;
import org.kaazing.gateway.server.ExpiringState;
import org.kaazing.gateway.server.spi.security.AuthenticationToken;
import org.kaazing.gateway.server.spi.security.AuthenticationTokenCallback;
import org.kaazing.gateway.server.spi.security.InetAddressCallback;
import org.kaazing.gateway.server.spi.security.LoginResult;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.gateway.transport.http.HttpUtils;
import org.kaazing.gateway.transport.http.bridge.HttpRequestMessage;
import org.kaazing.gateway.transport.http.bridge.HttpResponseMessage;
import org.kaazing.gateway.transport.http.security.auth.challenge.HttpChallengeFactories;
import org.kaazing.gateway.transport.http.security.auth.challenge.HttpChallengeFactory;
import org.slf4j.Logger;

public abstract class HttpLoginSecurityFilter extends HttpBaseSecurityFilter {

    protected static final String AUTH_SCHEME_APPLICATION_PREFIX = "Application ";

    /**
     * Used to model the result of a login when login is not required but is successful
     * (for example accessing a non-protected service)
     */
    private static final DefaultLoginResult LOGIN_RESULT_OK = new DefaultLoginResult();

    private static final Pattern PATTERN_HEADER_FORWARDED = Pattern.compile(".*for\\s*=\\s*(.*?)(?:\\s*;.*)?$");
    private static final String FORWARDED_URI = "scheme://%s";
    private static final String HEADER_FORWARDED_UNKNOWN_VALUE = "unknown";

    private final ExpiringState expiringState;

	public HttpLoginSecurityFilter() {
        super();
        this.expiringState = null;
    }

    public HttpLoginSecurityFilter(Logger logger, ExpiringState expiringState) {
        super(logger);
        this.expiringState = expiringState;
    }

    /**
     * A session is "already logged in" under either of these circumstances:
     * <ol>
     *     <li>The login module chain has already been run successfully.</li>
     *     <li>The session's subject has all required roles (e.g. none for unprotected services)</li>
     * </ol>
     *
     * @param session  the session that may or may not be already logged in
     * @return true iff the session is "already logged in" as defined above.
     */
    protected boolean alreadyLoggedIn(HttpRequestMessage httpRequest) {

        ResourceAddress localAddress = httpRequest.getLocalAddress();
        Subject subject = httpRequest.getSubject();

        Collection<String> requiredRoles = asList(localAddress.getOption(REQUIRED_ROLES));
        if  (requiredRoles == null || requiredRoles.size() == 0) {
            return true;
        }
        if (subject != null ) {
            Collection<String> authorizedRoles = getAuthorizedRoles(subject);
            return authorizedRoles.containsAll(requiredRoles);
        }
        return false;
    }

    /**
     * Models an success JAAS Configuration for use by the {@link #LOGIN_CONTEXT_OK}
     */
    private static class SuccessConfiguration extends javax.security.auth.login.Configuration {
        @Override
        public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
            return new AppConfigurationEntry[] {
                    new AppConfigurationEntry(YesLoginModule.class.getName(), AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                            new HashMap<>())};
        }
    }

    /**
     * Used to model the context of a login when login is not required but is successful
     * (for example accessing a non-protected service)
     */
    static final ResultAwareLoginContext LOGIN_CONTEXT_OK;



    static {
        try {
            LOGIN_CONTEXT_OK = new ResultAwareLoginContext("LOGIN_CONTEXT_OK", new Subject(), null,
                                                           new SuccessConfiguration(), LOGIN_RESULT_OK);
        } catch (LoginException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Issues challenges back to the client.
     */
    private HttpChallengeFactory challengeFactory = HttpChallengeFactories.create();



    /**
     * Handle the initial "login" attempt where the client has presumably
     * not sent any specific authentication token yet.
     * @return always returns false.
     */
    private boolean loginMissingToken(NextFilter nextFilter,
                                      IoSession session,
                                      HttpRequestMessage httpRequest,
                                      AuthenticationToken authToken,
                                      TypedCallbackHandlerMap additionalCallbacks,
                                      HttpRealmInfo[] realms,
                                      int realmIndex,
                                      LoginContext[] loginContexts) {

        HttpRealmInfo realm = realms[realmIndex];

        // We build a LoginContext here and call login() so that login
        // modules have a chance to any challenge parameters to the initial
        // challenge (KG-2191).
        ResultAwareLoginContext loginContext = null;

        try {


            LoginContextFactory loginContextFactory = realm.getLoginContextFactory();
            TypedCallbackHandlerMap callbackHandlerMap = new TypedCallbackHandlerMap();

            registerCallbacks(session, httpRequest, authToken, callbackHandlerMap);
            callbackHandlerMap.putAll(additionalCallbacks);

            loginContext = (ResultAwareLoginContext) loginContextFactory.createLoginContext(callbackHandlerMap);
            if (loginContext == null) {
                throw new LoginException("Login failed; cannot create a login context for authentication token '" + authToken + "\'.");
            }

            if (loggerEnabled()) {
                log("Login module login required; [%s].", authToken);
            }

            loginContext.login();
        } catch (LoginException le) {
            // Depending on the login modules configured, this could be
            // a very normal condition.
            if (loggerEnabled()) {

                // Ignore the common "all modules ignored case", it just gums
                // up the logs whilst adding no value.
                if (!le.getMessage().contains("all modules ignored")) {
                    log("Login failed: " + le.getMessage(), le);
                }
            }

            if (loginContext == null) {
                // This can happen if the given authentication token is
                // not one that we support.  And if that is the case, then
                // we will never able to handle the token, so return
                // 403 now.
                writeResponse(HttpStatus.CLIENT_FORBIDDEN, nextFilter, session, httpRequest);
                return false;
            }
        } catch (Exception e) {
            if (loggerEnabled()) {
                log("Login failed.", e);
            }
            writeResponse(HttpStatus.CLIENT_FORBIDDEN, nextFilter, session, httpRequest);
            return false;
        }

        DefaultLoginResult loginResult = loginContext.getLoginResult();
        String challenge = sendChallengeResponse(nextFilter, session, httpRequest, loginResult, realms, realmIndex, loginContexts);
        if (loggerEnabled()) {
            log(String.format("No authentication token was provided.  Issuing an authentication challenge '%s'.", challenge));
        }

        // No matter what happens, we know that the roles currently present
        // are not sufficient for logging in, so return false.
        ResourceAddress localAddress = LOCAL_ADDRESS.get(session);
        String nextProtocol = localAddress.getOption(NEXT_PROTOCOL);
        if ("http/1.1".equals(nextProtocol)) {
        	HttpMergeRequestFilter.INITIAL_HTTP_REQUEST_KEY.remove(session);
        }
        return false;
    }

    protected TypedCallbackHandlerMap makeAuthenticationTokenCallback(AuthenticationToken authToken) {
        AuthenticationTokenCallbackHandler authenticationTokenCallbackHandler
                = new AuthenticationTokenCallbackHandler(authToken);

        TypedCallbackHandlerMap map = new TypedCallbackHandlerMap();
        map.put(AuthenticationTokenCallback.class,
                authenticationTokenCallbackHandler);
        return map;
    }

    protected String getBaseAuthScheme(String authScheme) {
        if (authScheme != null && authScheme.startsWith(AUTH_SCHEME_APPLICATION_PREFIX)) {
            authScheme = authScheme.replaceFirst(AUTH_SCHEME_APPLICATION_PREFIX, "");
        }
        return authScheme;
    }

    /**
     * Login to the gateway using a specific authentication token.
     * @return true iff login succeeded.
     */
    protected boolean login(NextFilter nextFilter,
                            IoSession session,
                            HttpRequestMessage httpRequest,
                            AuthenticationToken authToken,
                            TypedCallbackHandlerMap additionalCallbacks,
                            HttpRealmInfo[] realms,
                            int realmIndex,
                            LoginContext[] loginContexts) {

    	HttpRealmInfo realm = realms[realmIndex];

        ResourceAddress address = httpRequest.getLocalAddress();
        String[] requiredRolesArray = address.getOption(HttpResourceAddress.REQUIRED_ROLES);

        boolean loginOK = true;  // should we be allowed through

        //
        // Try to establish a subject and the required and authorized roles from the login cache.
        //

        // Make sure we start with the subject from the underlying transport session (set into the request in
        // HttpSubjectSecurityFilter.doMessageReceived)
        Subject subject = httpRequest.getSubject();

        Collection<String> requireRoles = asList(requiredRolesArray);
        Collection<String> authorizedRoles = Collections.emptySet();
        boolean rolesAreSufficient = authorizedRoles.containsAll(requireRoles);

        if ( loggerEnabled() ) {
            log("Login starting; [token='%s',rolesAreSufficient=%s].", authToken==null?"N/A":authToken,
                     rolesAreSufficient);
        }

        // We have to issue a challenge when ALL of the below hold:
        //
        // - no authorization token was presented
        // - not all required roles (if any) are present
        //
        // Issue a challenge since authentication is required.
        if (authTokenIsMissing(authToken) &&
            rolesAreSufficient == false) {
            return loginMissingToken(nextFilter, session, httpRequest, authToken, additionalCallbacks, realms, realmIndex, loginContexts);
        }

        DefaultLoginResult loginResult = null;

        // We have a token to validate, or pre-authorized subject or sufficient roles already.
        // Using the cached subject lets clients reconnect and not pay the price for hitting the login module every time.

        ResultAwareLoginContext loginContext = null;

        if ( rolesAreSufficient ) {
            if ( loggerEnabled() ) {
                log("Login not required - subject has sufficient required roles; [%s].", authToken);
            }
            loginResult = LOGIN_RESULT_OK;
            loginContext = LOGIN_CONTEXT_OK;
            loginOK = true;
        }


        if (!rolesAreSufficient) {
            // We have a token to validate - let us validate it by logging in to a login module.
            final LoginContextFactory loginContextFactory = realm.getLoginContextFactory();

            try {
                TypedCallbackHandlerMap callbackHandlerMap = new TypedCallbackHandlerMap();

                registerCallbacks(session, httpRequest, authToken, callbackHandlerMap);

                callbackHandlerMap.putAll(additionalCallbacks);
                loginContext = (ResultAwareLoginContext) loginContextFactory.createLoginContext(callbackHandlerMap);
                if (loginContext == null) {
                    throw new LoginException("Login failed; cannot create a login context for authentication token '" + authToken+ "\'.");
                }
                if (loggerEnabled()) {
                    log("Login module login required; [%s].", authToken);
                }

                loginContext.login();
                loginResult = loginContext.getLoginResult();
                final LoginResult.Type resultType = loginResult.getType();
                if (resultType == LoginResult.Type.FAILURE) {
                    if ( loginResult.getLoginException() != null) {
                        throw loginResult.getLoginException();
                    } else {
                        throw new LoginException("Login Result Indicates Failure");
                    }
                }

                subject = loginContext.getSubject();
                authorizedRoles = getAuthorizedRoles(subject);
                boolean subjectAutomaticallyAuthorized = isSubjectAutomaticallyAuthorized(subject, requireRoles);
                rolesAreSufficient = authorizedRoles.containsAll(requireRoles);

                if (resultType == LoginResult.Type.CHALLENGE || (!subjectAutomaticallyAuthorized && !rolesAreSufficient)) {

                    if ( resultType == LoginResult.Type.CHALLENGE ) {
                        if (loggerEnabled()) {
                            log("Login module login succeeded but requires another challenge");
                        }
                        Object[] data = loginResult.getLoginChallengeData();
                        if (data == null || data.length == 0) {

                            //KG-2207, KG-3464, KG-3389
                            if (! HttpBaseSecurityFilter.AUTH_SCHEME_BASIC.equals(getBaseAuthScheme(realm.getChallengeScheme()))) {
                                if (loggerEnabled()) {
                                    log("Login module login succeeded but requires another challenge, however no new challenge data was provided.");
                                }
                                writeResponse(HttpStatus.CLIENT_FORBIDDEN, nextFilter, session, httpRequest);
                                return false;
                            }
                        }
                    }
                    if (realmIndex + 1 < realms.length && resultType == SUCCESS) {
                        // we only enforce subject at the end of the realm chain, otherwise we skip to the next realm
                        loginContexts[realmIndex] = loginContext;

                        // remember login context
                        httpRequest.setLoginContext(loginContext);

                        // remember subject
                        httpRequest.setSubject((loginContext == null || loginContext == LOGIN_CONTEXT_OK) ? subject : loginContext.getSubject());
                        return true;
                    }
                    // Login was successful, but the subject identified does not have required roles
                    // to create the web socket.  We re-issue a challenge here.
                    String challenge = sendChallengeResponse(nextFilter, session, httpRequest, loginResult, realms, realmIndex, loginContexts);
                    if (loggerEnabled()) {
                        if ( resultType != LoginResult.Type.CHALLENGE) {
                            log(String.format("Login module login succeeded but subject missing required roles; Issued another challenge '%s'.", challenge));
                        } else {
                            log(String.format("Login module login succeeded but login result requires a challenge; Issued another challenge '%s'.", challenge));
                        }
                    }
                    return false;
                }
            } catch (Exception e) {
                loginOK = false;

                if ( loggerEnabled() ) {
                    log("Login failed.", e);
                }

                //KG-2207, KG-3389
                if ( HttpBaseSecurityFilter.AUTH_SCHEME_BASIC.equals(getBaseAuthScheme(realm.getChallengeScheme()))) {
                    String challenge = sendChallengeResponse(nextFilter, session, httpRequest, loginResult, realms, realmIndex, loginContexts);
                    if (loggerEnabled()) {
                        log(String.format("Login module login failed; Issued another challenge '%s'.", challenge), e);
                    }
                } else {
                    writeResponse(HttpStatus.CLIENT_FORBIDDEN, nextFilter, session, httpRequest);
                }
            }
        }

        if ( loginOK && loginResult.hasLoginAuthorizationAttachment() ) {
            writeSessionCookie(session, httpRequest, loginResult);
        }

        if (loginOK) {
            // store information into the session
            try {
                loginContexts[realmIndex] = loginContext;

                // remember login context
                httpRequest.setLoginContext(loginContext);

                // remember subject
                httpRequest.setSubject((loginContext == null || loginContext == LOGIN_CONTEXT_OK) ? subject : loginContext.getSubject());
            } catch (Exception e) {
                if (loggerEnabled()) {
                    logger.trace("Login failed.", e);
                }
                loginOK = false;
            }
        }

        if ( loginOK && loggerEnabled() ) {
            log("Login succeeded; [%s].", authToken);
        }
        if ( !loginOK && loggerEnabled() ) {
            log("Login failed; [%s].", authToken);
        }
        return loginOK;
    }

    private String sendChallengeResponse(NextFilter nextFilter, IoSession session, HttpRequestMessage httpRequest,
        DefaultLoginResult loginResult, HttpRealmInfo[] realms, int realmIndex, LoginContext[] loginContexts) {

        HttpRealmInfo realm = realms[realmIndex];
        Object[] challengeData = loginResult != null && loginResult.getType() == LoginResult.Type.CHALLENGE
                ? loginResult.getLoginChallengeData() : null;
        HttpResponseMessage httpResponse = challengeFactory.createChallenge(httpRequest, realm, challengeData);

        if (realmIndex > 0) {
            String challengeIdentity;
            do {
                challengeIdentity = HttpUtils.newSessionId();
                httpResponse.setHeader(HEADER_SEC_CHALLENGE_IDENTITY, challengeIdentity);
            } while (expiringState != null && expiringState.putIfAbsent(challengeIdentity, loginContexts, 30, SECONDS) != null);
        }

        writeChallenge(httpResponse, nextFilter, session, realm.getChallengeScheme());

        return httpResponse.getHeader(HEADER_WWW_AUTHENTICATE);
    }

    /**
     * Set the appropriate login context for an unprotected service (no session context, no security configured).
     * This allows the rest of the pipeline to issue the login context is always set.
     * @param session
     */
    protected void setUnprotectedLoginContext(final HttpRequestMessage request) {
        request.setLoginContext(LOGIN_CONTEXT_OK);
    }

    private void registerCallbacks(
            IoSession session,
            HttpRequestMessage httpRequest,
            AuthenticationToken authToken,
            TypedCallbackHandlerMap callbackHandlerMap) {
        if (callbackHandlerMap == null) {
            throw new NullPointerException("Null callbackHandlerMap passed in");
        }

        AuthenticationTokenCallbackHandler authenticationTokenCallbackHandler
                                                 = new AuthenticationTokenCallbackHandler(authToken);
        callbackHandlerMap.put(AuthenticationTokenCallback.class, authenticationTokenCallbackHandler);

        String forwarded = httpRequest.getHeader(HEADER_FORWARDED);
        if (forwarded != null) {
            Matcher matcher = PATTERN_HEADER_FORWARDED.matcher(forwarded.toLowerCase());
            if (!matcher.matches()) {
                throw new IllegalStateException(format("Invalid format: '%s'", forwarded));
            }

            // RFC 7239(http://tools.ietf.org/html/rfc7239) allows 'Forwarded' header to include IPv4 and IPv6
            // addresses along with port number like this:
            // Forwarded: for=192.0.2.43:47011
            // Forwarded: for="[2001:db8:cafe::17]:47011"
            //
            // Get the IP address without the port number and the quotes(for IPv6).
            String ipAddress = matcher.group(1);
            if (ipAddress.charAt(0) == '"') {
                int length = ipAddress.length();
                assert length > 2;

                if (ipAddress.charAt(length - 1) != '"') {
                    throw new IllegalStateException(format("Invalid format: '%s'", forwarded));
                }

                // Quoted string represents an IPv6 address. Remove the quotes to create an InetAddress.
                ipAddress = ipAddress.substring(1, length - 2);
            }

            // RFC 7239(http://tools.ietf.org/html/rfc7239) allows 'Forwarded' header to include an 'unknown'
            // value like this:
            // Forwarded: for=unknown
            //
            // In for=unknown, Gateway does not register the InetAddressCallback. If a LoginModule uses
            // InetAddressCallback to retrieve the remote InetAddress, then it will detect it's absence and
            // throw an exception that will result in a 403 response.
            if (!ipAddress.equals(HEADER_FORWARDED_UNKNOWN_VALUE)) {
                URI uri = URI.create(format(FORWARDED_URI, ipAddress));
                populateRemoteAddress(callbackHandlerMap, uri);
            }
        } else {
            ResourceAddress resourceAddress = REMOTE_ADDRESS.get(session);
            ResourceAddress tcpResourceAddress = resourceAddress.findTransport("tcp");
            if (tcpResourceAddress != null) {
                URI resource = tcpResourceAddress.getResource();
                populateRemoteAddress(callbackHandlerMap, resource);
            }
        }
    }

    private void populateRemoteAddress(TypedCallbackHandlerMap callbackHandlerMap, URI resource) {
        String remoteIpAddress = resource.getHost();
        InetAddress remoteAddr;

        try {
            remoteAddr = InetAddress.getByName(remoteIpAddress);
        }
        catch (UnknownHostException e) {
            if (logger.isTraceEnabled()) {
                logger.trace(e.getMessage());
            }

            throw new IllegalStateException(e);
        }

        InetAddressCallbackHandler inetAddressCallbackHandler = new InetAddressCallbackHandler(remoteAddr);
        callbackHandlerMap.put(InetAddressCallback.class, inetAddressCallbackHandler);
    }

    protected void writeSessionCookie(IoSession session, HttpRequestMessage httpRequest, DefaultLoginResult loginResult) {
    }

    protected LoginContext[] getLoginContexts(String challengeIdentity) {
        return expiringState != null && challengeIdentity != null ? (LoginContext[]) expiringState.get(challengeIdentity) : null;
    }

    private boolean authTokenIsMissing(AuthenticationToken authToken) {
        return authToken == null || authToken.isEmpty();
    }

    private boolean isSubjectAutomaticallyAuthorized(Subject subject, Collection<String> requireRoles) {
        return requireRoles.contains("*") && subject != null;
    }

    private void log(String format, Object... values) {
        logger.trace(String.format(format, values));
    }

    private void log(String msg, Throwable t) {
        logger.trace(msg, t);
    }
}
