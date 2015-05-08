/**
 * Copyright (c) 2007-2014, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.demo.loginmodules;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kaazing.gateway.server.spi.security.AuthenticationToken;
import com.kaazing.gateway.server.spi.security.AuthenticationTokenCallback;
import com.kaazing.gateway.server.spi.security.LoginResult;
import com.kaazing.gateway.server.spi.security.LoginResultCallback;

public class CustomLoginModule implements LoginModule {

    public static final String CLASS_NAME = CustomLoginModule.class.getName();

    /**
     * The prefix that appears on all log output making it easier to identify.
     */
    public static final String LOG_PREFIX = "[LM]";

    /**
     * <p>
     * Logger for runtime, debugging, or trace information. You set the logger level and where the output should go in
     * GATEWAY_HOME/conf/log4j-config.xml. For example, add the following to the log4-config.xml file to show log output
     * in the Gateway console and the log file:
     * </p>
     * 
     * <pre>
     * <code> &lt;logger name="com.kaazing.demo.loginmodules.MyTokenLoginModule">
     *     &lt;level value="trace"/>
     *     &lt;appender-ref ref="STDOUT"/>
     * &lt;/logger></code>
     * </pre>
     */
    public static final Logger logger = LoggerFactory.getLogger(CLASS_NAME);

    /**
     * The subject to be authenticated.
     */
    private Subject subject;

    /**
     * The callback handler to retrieve inputs from the login context.
     */
    private CallbackHandler handler;

    /**
     * Options specified from the Gateway configuration.
     */
    private Map<String, ?> options;

    /**
     * The principal that will be attached the LoginResult upon a successful authentication.
     */
    private Principal principal;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {

        logDebug("initialize()");

        this.subject = subject;
        this.handler = callbackHandler;
        this.options = options;

        // Print out all of the options passed in.
        //
        Set<?> keys = this.options.keySet();
        if ( keys.size() > 0 ) {
            logDebug("Options:");
            for (Object key : keys) {
                logDebug("  " + key + "=" + this.options.get(key));
            }
        }
    }

    @Override
    public boolean login() throws LoginException {
        logDebug("login()");

        // Get a copy of the LoginRe
        LoginResult loginResult = getLoginResultFromCallback();
        if ( loginResult == null ) {
            logDebug("No LoginResult");
            return false;
        }

        // Get the authentication token container which contains any tokens. If
        // it is empty, that means there were no tokens present.
        AuthenticationToken authToken = getTokenFromCallback();
        if ( authToken == null || authToken.isEmpty() ) {
            logDebug("No tokens provided");

            // No token when we were expecting one, so challenge the client.
            // Return false to indicate this LoginModule can be ignored for
            // this invocation.
            loginResult.challenge();

            // NOTE: Optionally, the LoginModule can send data to the client
            // along with the challenge. The following commented out line shows
            // an example of how to do that. An example might be for something
            // like when using Facebook or Google, the client will need to submit
            // an app id when requesting a token.
            //
            // On the client side, this data will be included in the challenge
            // request and can be extracted by the client to be used in any way.
            //
            // loginResult.challenge("app-id=123456789");

            return false;
        }

        logDebug("Obtained AuthenticationToken: " + authToken);

        // Make sure that the token data uses the "Token" authentication
        // scheme. If not, then it is not token data that we want to
        // parse/use.
        String authScheme = authToken.getScheme();
        // Note: Compare to "Token" because the scheme is just the scheme "type"
        // which is Basic, Negotiate, or Token.
        if ( !"Token".equals(authScheme) ) {
            // Since this Login Module is for processing data based on the Token
            // authentication scheme (as opposed to Application Basic, for example),
            // we can ignore this invocation.
            //
            // Return false to indicate this LoginModule can be ignored because it
            // is orthogonal to the authentication attempt.
            //
            // From http://docs.oracle.com/javase/7/docs/technotes/guides/security/jaas/JAASLMDevGuide.html:
            //
            // "Determine whether or not this LoginModule should be ignored. One example
            // of when it should be ignored is when a user attempts to authenticate
            // under an identity irrelevant to this LoginModule (if a user attempts
            // to authenticate as root using NIS, for example).
            // If this LoginModule should be ignored, login should return false."
            logDebug("Expected authentication token scheme 'Token', but received '" + authToken.getScheme() + "'. Ignoring token");
            return false;
        }

        logDebug("Extracting token from HTTP Authorization header");
        String tokenData = authToken.get();

        if ( tokenData == null ) {
            logDebug("No token data supplied");
            throw new LoginException("No token data supplied");
        }

        logDebug("Extracted token data: [" + tokenData + "]");

        // Validate the token.
        // Replace this with a the way you validate your token.
        String errorMessage = isTokenValid(tokenData);
        if ( errorMessage != null ) {
            LoginException e = new LoginException(errorMessage);
            logError(errorMessage, e);
            throw e;
        }

        logDebug("Token is valid");

        // If we got here then the token was successfully validated. Set the
        // principal (i.e. the role) now so it can be
        // returned in the commit() method.
        principal = new Principal() {
            @Override
            public String getName() {
                return "AUTHORIZED";
            }
        };

        return true;

    }

    @Override
    public boolean commit() throws LoginException {
        logDebug("commit()");

        // Commit can be invoked even if login() didn't fully complete. So in
        // order to know if login() fully completed
        // or not, check whether the principal is set.
        if ( principal == null ) {
            logDebug("No role granted");
            return false;
        }

        // Add the principal to the subject, so it will be available to other
        // parts of the Gateway that have access to this connection's subject
        // information.
        subject.getPrincipals().add(principal);

        logDebug("User granted role: " + principal.getName());

        // Print out all Principals on the Subject. Useful for debugging.
        Set<Principal> principals = subject.getPrincipals();
        for (Principal principal : principals) {
            logDebug("Principal: " + principal.getName() + " [" + principal.getClass().getName() + "]");
        }

        return true;
    }

    @Override
    public boolean abort() throws LoginException {

        // abort() is called when login() and/or commit() fail.
        // Contract is to return false if login failed. Otherwise
        // clean up the principal and return true. If any error
        // happens during cleanup, throw an exception.

        logDebug("abort()");

        // If login failed, returned false to indicate
        // we should ignore this abort() because there is
        // no state to clean up.
        if ( principal == null ) {
            logDebug("No role granted");
            return false;
        }

        try {
            clearRole();

            // Abort Success: state is cleaned, return true.
            return true;
        }
        catch (Exception ex) {
            // Abort failure: throw Login Exception
            LoginException e = new LoginException("Unexpected error during abort().");
            e.initCause(ex);
            logError(e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public boolean logout() throws LoginException {

        // The logout() contract is to clean up the credentials if
        // we can, then return true, otherwise throw an exception.
        // No real provision in JaaS for returning false here.

        logDebug("logout()");

        try {
            clearRole();
            // Logout Success: state is cleaned, return true.
            return true;
        }
        catch (Exception ex) {
            LoginException e = new LoginException("Unexpected error during logout().");
            e.initCause(ex);
            logError(e.getMessage(), e);
            throw e;
        }
    }

    private void logDebug(String message) {
        logger.debug(LOG_PREFIX + " " + message);
    }

    private void logError(String message, Exception exception) {
        logger.error(LOG_PREFIX + " " + message, exception);
    }

    /**
     * Retrieve the LoginResult from the Login Context.
     * 
     * @return the LoginResult for this Login Module chain
     */
    private LoginResult getLoginResultFromCallback() {
        final LoginResultCallback loginResultCallback = new LoginResultCallback();
        try {
            handler.handle(new Callback[]{loginResultCallback});

        }
        catch (IOException ioe) {
            logError("Encountered exception handling loginResultCallback", ioe);
            return null;
        }
        catch (UnsupportedCallbackException uce) {
            logError("UnsupportedCallbackException handling loginResultCallback", uce);
            return null;
        }

        return loginResultCallback.getLoginResult();
    }

    /**
     * Retrieve the authentication token
     * 
     * @return the authentication token from the client
     */
    private AuthenticationToken getTokenFromCallback() {
        final AuthenticationTokenCallback authTokenCallback = new AuthenticationTokenCallback();

        try {
            handler.handle(new Callback[]{authTokenCallback});
        }
        catch (IOException ioe) {
            logError("Encountered exception handling authTokenCallback", ioe);
            return null;
        }
        catch (UnsupportedCallbackException uce) {
            logError("UnsupportedCallbackException handling authenticationTokenCallback", uce);
            return null;
        }

        AuthenticationToken authToken = authTokenCallback.getAuthenticationToken();

        return authToken;
    }

    /**
     * Validate the token.
     * 
     * @param token the token to be validated
     * @return null if token is valid, otherwise return a string that describes the error
     */
    private String isTokenValid(String token) {
        // The token is invalid if it contains the string "invalid". This is an
        // example error condition.
        if ( token.indexOf("invalid") != -1 ) {
            return "Token is invalid";
        }
        return null;
    }

    /**
     * Clean up the Subject state to ensure that the principal is not added to prevent accidental login.
     */
    private void clearRole() {
        subject.getPrincipals().remove(principal);
    }

}
