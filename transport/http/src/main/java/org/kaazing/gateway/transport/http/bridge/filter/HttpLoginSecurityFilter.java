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

import static org.kaazing.gateway.resource.address.ResourceAddress.NEXT_PROTOCOL;
import static org.kaazing.gateway.transport.BridgeSession.LOCAL_ADDRESS;
import static java.util.Arrays.asList;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import javax.security.auth.Subject;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.LoginException;

import org.apache.mina.core.session.IoSession;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.http.HttpResourceAddress;
import org.kaazing.gateway.security.AccessControlLoginException;
import org.kaazing.gateway.security.LoginContextFactory;
import org.kaazing.gateway.security.TypedCallbackHandlerMap;
import org.kaazing.gateway.security.auth.AuthenticationTokenCallbackHandler;
import org.kaazing.gateway.security.auth.DefaultLoginResult;
import org.kaazing.gateway.security.auth.YesLoginModule;
import org.kaazing.gateway.security.auth.context.ResultAwareLoginContext;
import org.kaazing.gateway.server.spi.security.AuthenticationToken;
import org.kaazing.gateway.server.spi.security.AuthenticationTokenCallback;
import org.kaazing.gateway.server.spi.security.LoginResult;
import org.kaazing.gateway.transport.TypedAttributeKey;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.gateway.transport.http.bridge.HttpRequestMessage;
import org.kaazing.gateway.transport.http.bridge.HttpResponseMessage;
import org.kaazing.gateway.transport.http.security.auth.challenge.HttpChallengeFactories;
import org.kaazing.gateway.transport.http.security.auth.challenge.HttpChallengeFactory;
import org.kaazing.mina.core.session.IoSessionEx;
import org.slf4j.Logger;

public abstract class HttpLoginSecurityFilter extends HttpBaseSecurityFilter {

    protected static final String AUTH_SCHEME_APPLICATION_PREFIX = "Application ";

    /**
     * Session key used for communicating the login context to higher level sessions.
     */
    public static final TypedAttributeKey<ResultAwareLoginContext> LOGIN_CONTEXT_KEY =
            new TypedAttributeKey<>(HttpLoginSecurityFilter.class, "loginContext");


    /**
     * Used to model the result of a login when login is not required but is successful
     * (for example accessing a non-protected service)
     */
    private static final DefaultLoginResult LOGIN_RESULT_OK = new DefaultLoginResult();


    public HttpLoginSecurityFilter() {
        super();
    }

    public HttpLoginSecurityFilter(Logger logger) {
        super(logger);
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
    protected boolean alreadyLoggedIn(IoSession session, ResourceAddress address) {

        Collection<String> requiredRoles = asList(address.getOption(HttpResourceAddress.REQUIRED_ROLES));
        if  (requiredRoles == null || requiredRoles.size() == 0) {
            return true;
        }
        Subject subject = ((IoSessionEx)session).getSubject();
        if (subject != null ) {
            Collection<String> authorizedRoles = getAuthorizedRoles(subject);
            return authorizedRoles.containsAll(requiredRoles);
        }
        return false;
    }

    public static void cleanup(IoSession session) {
        LOGIN_CONTEXT_KEY.remove(session);
    }

    /**
     * Models an success JAAS Configuration for use by the {@link #LOGIN_CONTEXT_OK}
     */
    private static class SuccessConfiguration extends javax.security.auth.login.Configuration {
        @Override
        public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
            return new AppConfigurationEntry[] {
                    new AppConfigurationEntry(YesLoginModule.class.getName(), AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                            new HashMap<String, Object>())};
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
                                      TypedCallbackHandlerMap additionalCallbacks) {

        DefaultLoginResult loginResult = null;

        // We build a LoginContext here and call login() so that login
        // modules have a chance to any challenge parameters to the initial
        // challenge (KG-2191).
        ResultAwareLoginContext loginContext = null;
        ResourceAddress address = httpRequest.getLocalAddress();

        String httpChallengeScheme = address.getOption(HttpResourceAddress.REALM_CHALLENGE_SCHEME);
        try {


            LoginContextFactory loginContextFactory = address.getOption(HttpResourceAddress.LOGIN_CONTEXT_FACTORY);
            TypedCallbackHandlerMap callbackHandlerMap = new TypedCallbackHandlerMap();

            // Register callbacks. This is the hook for the Enterprise to add more callbacks for LoginModules
            // that are Enterprise-specific.
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

        } catch (AccessControlLoginException ace) {
            if (loggerEnabled()) {
                log("Login failed: ", ace);
            }

            writeResponse(HttpStatus.CLIENT_FORBIDDEN, nextFilter, session, httpRequest);
            return false;
        } catch (LoginException le) {
            // Depending on the login modules configured, this could be
            // a very normal condition.
            if (loggerEnabled()) {
                log("Login failed", le);

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

        loginResult = loginContext.getLoginResult();
        final LoginResult.Type resultType = loginResult.getType();

        // Now check to see if any of the login modules added any challenge
        // parameters/data.

        Object[] challengeData = null;

        if (resultType == LoginResult.Type.CHALLENGE) {
            challengeData = loginResult.getLoginChallengeData();
        }

        HttpResponseMessage httpResponse = challengeFactory.createChallenge(httpRequest, challengeData);

        if (loggerEnabled()) {
            String challenge = httpResponse.getHeader(HttpSubjectSecurityFilter.WWW_AUTHENTICATE_HEADER);
            log(String.format("No authentication token was provided.  Issuing an authentication challenge '%s'.", challenge));
        }
        writeChallenge(httpResponse, nextFilter, session, httpChallengeScheme);

        // No matter what happens, we know that the roles currently present
        // are not sufficient for logging in, so return false.
        ResourceAddress localAddress = LOCAL_ADDRESS.get(session);
        String nextProtocol = localAddress.getOption(NEXT_PROTOCOL);
        if ("http/1.1".equals(nextProtocol)) {
        	HttpMergeRequestFilter.INITIAL_HTTP_REQUEST_KEY.remove(session);
        }
        return false;
    }

    /**
     * This method will be overridden in the Enterprise Gateway to add Callbacks for Enterprise-specific LoginModules.
     *
     * @param session
     * @param httpRequest
     * @param authToken
     * @param callbacks
     */
    protected void registerCallbacks(
            IoSession session,
            HttpRequestMessage httpRequest,
            AuthenticationToken authToken,
            TypedCallbackHandlerMap callbacks) {
        if (callbacks == null) {
            throw new NullPointerException("Null callbacks map passed in");
        }

        AuthenticationTokenCallbackHandler authenticationTokenCallbackHandler
                                                 = new AuthenticationTokenCallbackHandler(authToken);
        callbacks.put(AuthenticationTokenCallback.class, authenticationTokenCallbackHandler);
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
                            TypedCallbackHandlerMap additionalCallbacks) {

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
            return loginMissingToken(nextFilter, session, httpRequest, authToken, additionalCallbacks);
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
            final LoginContextFactory loginContextFactory = address.getOption(HttpResourceAddress.LOGIN_CONTEXT_FACTORY);

            try {
                TypedCallbackHandlerMap callbackHandlerMap = new TypedCallbackHandlerMap();

                // Register callbacks. This is the hook for the Enterprise to add more callbacks for LoginModules
                // that are Enterprise-specific.
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
                            if (! HttpBaseSecurityFilter.AUTH_SCHEME_BASIC.equals(getBaseAuthScheme(address.getOption(HttpResourceAddress.REALM_CHALLENGE_SCHEME)))) {
                                if (loggerEnabled()) {
                                    log("Login module login succeeded but requires another challenge, however no new challenge data was provided.");
                                }
                                writeResponse(HttpStatus.CLIENT_FORBIDDEN, nextFilter, session, httpRequest);
                                return false;
                            }
                        }
                    }
                    // Login was successful, but the subject identified does not have required roles
                    // to create the web socket.  We re-issue a challenge here.
                    String challenge = sendChallengeResponse(nextFilter, session, httpRequest, loginResult);
                    if (loggerEnabled()) {
                        if ( resultType != LoginResult.Type.CHALLENGE) {
                            log(String.format("Login module login succeeded but subject missing required roles; Issued another challenge '%s'.", challenge));
                        } else {
                            log(String.format("Login module login succeeded but login result requires a challenge; Issued another challenge '%s'.", challenge));
                        }
                    }
                    return false;
                }

            } catch (AccessControlLoginException ace) {
                loginOK = false;

                if (loggerEnabled()) {
                    log("Login failed: ", ace);
                }
                writeResponse(HttpStatus.CLIENT_FORBIDDEN, nextFilter, session, httpRequest);
            } catch (Exception e) {
                loginOK = false;

                if (loggerEnabled()) {
                    log("Login failed.", e);
                }

                //KG-2207, KG-3389
                if ( HttpBaseSecurityFilter.AUTH_SCHEME_BASIC.equals(getBaseAuthScheme(address.getOption(HttpResourceAddress.REALM_CHALLENGE_SCHEME))) ) {
                    String challenge = sendChallengeResponse(nextFilter, session, httpRequest, loginResult);
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

        if (loginOK ) {
            // store information into the session
            try {

                // remember login context
                LOGIN_CONTEXT_KEY.set(session, loginContext);

                // remember subject
                httpRequest.setSubject((loginContext == null || loginContext == LOGIN_CONTEXT_OK) ? subject : loginContext.getSubject());
            } catch (Exception e) {
                if (loggerEnabled()) {
                    logger.trace("Login failed.", e);
                }
                loginOK = false;
            }
        } else {
            // forget login context
            LOGIN_CONTEXT_KEY.remove(session);
        }

        if ( loginOK && loggerEnabled() ) {
            log("Login succeeded; [%s].", authToken);
        }
        if ( !loginOK && loggerEnabled() ) {
            log("Login failed; [%s].", authToken);
        }
        return loginOK;
    }

    private String sendChallengeResponse(NextFilter nextFilter, IoSession session, HttpRequestMessage httpRequest,  DefaultLoginResult loginResult) {
        ResourceAddress localAddress = httpRequest.getLocalAddress();
        Object[] challengeData = loginResult == null ? null : loginResult.getLoginChallengeData();
        HttpResponseMessage httpResponse = challengeFactory.createChallenge(httpRequest, challengeData);
        writeChallenge(httpResponse, nextFilter, session, localAddress.getOption(HttpResourceAddress.REALM_CHALLENGE_SCHEME));
        return httpResponse.getHeader(HttpSubjectSecurityFilter.WWW_AUTHENTICATE_HEADER);
    }

    /**
     * Set the appropriate login context for an unprotected service (no session context, no security configured).
     * This allows the rest of the pipeline to issue the login context is always set.
     * @param session
     */
    protected void setUnprotectedLoginContext(final IoSession session) {
        LOGIN_CONTEXT_KEY.set(session, LOGIN_CONTEXT_OK);
    }



    protected void writeSessionCookie(IoSession session, HttpRequestMessage httpRequest, DefaultLoginResult loginResult) {
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

    // Aggressive removal of TCP session attributes as and when session closes.
    @Override
    public void filterClose(NextFilter nextFilter, IoSession session) throws Exception {
        LOGIN_CONTEXT_KEY.remove(session);
        super.filterClose(nextFilter, session);
    }

    @Override
    public void doSessionClosed(NextFilter nextFilter, IoSession session) throws Exception {
        LOGIN_CONTEXT_KEY.remove(session);
        super.doSessionClosed(nextFilter, session);
    }

}
