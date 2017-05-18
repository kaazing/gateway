/**
 * Copyright (c) 2007-2017, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.demo.loginmodules;

import java.io.IOException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.json.JSONException;
import org.json.JSONObject;
import org.kaazing.gateway.server.spi.security.AuthenticationToken;
import org.kaazing.gateway.server.spi.security.AuthenticationTokenCallback;
import org.kaazing.gateway.server.spi.security.ExpiringState;
import org.kaazing.gateway.server.spi.security.LoginResult;
import org.kaazing.gateway.server.spi.security.LoginResultCallback;

/**
 * This is an example custom login module that reads a token to extract the username. If successful, the username is placed into
 * shared state for login modules downstream in the chain to continue authorization. For example, a downstream login module may
 * retrieve the user's role or roles from LDAP and inject them into the Subject.
 *
 * A good starting point for understanding login modules and recommended practices is the "Java Authentication and Authorization
 * Service(JAAS): LoginModule Developer's Guide":
 *
 * http://docs.oracle.com/javase/8/docs/technotes/guides/security/jaas/JAASLMDevGuide.html
 */
public class TokenLoginModule implements LoginModule {

    private static final String USERNAME_KEY = "javax.security.auth.login.name";

    private Logger logger;

    private CallbackHandler callbackHandler;
    private Map<String, Object> sharedState;

    private ExpiringState expiringState;

    private String username;

    @Override
    @SuppressWarnings("unchecked")
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {

        this.logger = configureLogger(this, options);

        this.callbackHandler = callbackHandler;

        this.sharedState = (Map<String, Object>) sharedState;

        this.expiringState = (ExpiringState) options.get("org.kaazing.gateway.EXPIRING_STATE");

        if (logger.isLoggable(Level.FINE)) {
            if (options.size() > 0) {
                logger.fine("Options:");
                options.forEach((k, v) -> logger.fine(String.format("  %s=%s", k, v)));
            }
        }
    }

    @Override
    public boolean login() throws LoginException {

        try {

            if (callbackHandler == null) {
                throw new LoginException("No CallbackHandler available");
            }

            AuthenticationToken authToken = getTokenFromCallback();
            if ((authToken == null) || (authToken.isEmpty())) {
                /*
                 * Since a token was expected, issue a challenge to the client to provide one. Calling challenge() will result in a
                 * 401 HTTP response back to the client when an exception is thrown that results in authorization failing. If an
                 * exception is thrown without first calling challenge(), the client will receive a 403 HTTP response instead.
                 */
                LoginResult loginResult = getLoginResultFromCallback();
                loginResult.challenge();
                throw new LoginException("No tokens provided");
            }
            logger.fine(String.format("Login: Obtained AuthenticationToken: %s", authToken));

            /*
             * Ensure that the token data uses the "Token" authentication scheme. Note: Compare to "Token" rather than
             * "Application Token" because the scheme is just the scheme type which is Basic, Negotiate, or Token.
             */
            String authScheme = authToken.getScheme();
            if (!"Token".equalsIgnoreCase(authScheme)) {
                throw new LoginException(
                    "Expected authentication token scheme 'Token', but received '" + authToken.getScheme() + "'");
            }

            // The AuthenticationToken contains extra information (such as the scheme), so extract the token data specifically.
            String tokenData = authToken.get();

            processToken(tokenData);

            logger.fine(String.format("Login: Saving username to shared state: %s", username));
            sharedState.put(USERNAME_KEY, username);

            return true;

        }
        catch (LoginException e) {
            logger.fine(String.format("Login: Got a LoginException: %s", e.getMessage()));
            throw e;
        }
        catch (Exception e) {
            logger.fine(String.format("Login: Got an Exception: %s", e.getMessage()));
            throw new LoginException(e.getMessage());
        }

    }

    @Override
    public boolean commit() throws LoginException {
        /*
         * This method associates relevant Principals and Credentials with the Subject. This login module does not perform
         * authorization, but extracts the username from a token for downstream login modules to authorize. Therefore there is
         * nothing to commit here.
         */
        logger.fine("Commit: Nothing to commit, all is good");
        return true;
    }

    @Override
    public boolean abort() throws LoginException {
        /*
         * Clear state here, if any was created, particularly anything stored in the sharedState object or sensitive data stored in
         * memory, like passwords.
         */
        logger.fine("Abort: Clearing shared state");
        sharedState.remove(USERNAME_KEY);
        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        /*
         * This method removes Principals, and removes/destroys credentials associated with the Subject during the commit operation.
         * This method should not touch those Principals or credentials previously existing in the Subject, or those added by other
         * LoginModules.
         */
        logger.fine(String.format("Logout: Logging out %s", username));

        return true;
    }

    private LoginResult getLoginResultFromCallback() throws LoginException, IOException, UnsupportedCallbackException {
        LoginResultCallback loginResultCallback = new LoginResultCallback();
        callbackHandler.handle(new Callback[] {
            loginResultCallback
        });
        return loginResultCallback.getLoginResult();
    }

    private AuthenticationToken getTokenFromCallback() throws LoginException {
        AuthenticationTokenCallback authTokenCallback = new AuthenticationTokenCallback();
        try {
            callbackHandler.handle(new Callback[] {
                authTokenCallback
            });
        }
        catch (Exception e) {
            throw new LoginException(e.getMessage());
        }
        return authTokenCallback.getAuthenticationToken();
    }

    /**
     * Validate the token and extract the username to add to shared state. An exception is thrown if the token is found to be
     * invalid
     *
     * This method expects the token to be in JSON format:
     *
     * {
     *   username: "joe",
     *   nonce: "5171483440790326",
     *   tokenExpires: "2017-04-20T15:48:49.187Z"
     * }
     *
     */
    private void processToken(String tokenData) throws JSONException, LoginException {

        JSONObject json = new JSONObject(tokenData);

        // Validate that the token hasn't expired.
        ZonedDateTime expires = ZonedDateTime.parse(json.getString("tokenExpires"));
        ZonedDateTime now = ZonedDateTime.now(expires.getZone());
        if (expires.isBefore(now)) {
            throw new LoginException("Token has expired");
        }

        /*
         * The following check that expiringState is not null can be removed once the expiring state API is made public. Until then,
         * it can be enabled by setting the early access flag "login.module.expiring.state". See the documentation for how to set
         * the early access flag:
         *
         * https://kaazing.com/doc/5.0/admin-reference/p_configure_gateway_opts/index.html#enable-early-access-features
         */
        if (expiringState != null) {
            // Validate that the token hasn't been used already. If not, then store it until it expires in case it is replayed.
            long duration = Duration.between(now, expires).toMillis();
            String nonce = json.getString("nonce");
            if (expiringState.putIfAbsent(nonce, nonce, duration, TimeUnit.MILLISECONDS) != null) {
                throw new LoginException(String.format("Token nonce has already been used: %s", nonce));
            }
        }

        // Token has passed validity checks. Store the username to be used by the commit() method.
        this.username = json.getString("username");
        logger.fine(String.format("Login: Token is valid for user %s", username));

    }

    private static Logger configureLogger(Object owner, Map<String, ?> options) {
        Logger logger = Logger.getLogger(String.format("%s.%d", owner.getClass().getName(), System.identityHashCode(owner)));
        if ("true".equals(options.get("debug"))) {
            Level level = Level.FINE;
            Handler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(level);
            consoleHandler.setFormatter(new Formatter() {
                @Override
                public String format(LogRecord record) {
                    return String.format("[%s] %s\n", owner.getClass().getSimpleName(), record.getMessage());
                }
            });
            logger.addHandler(consoleHandler);
            logger.setUseParentHandlers(false);
            logger.setLevel(level);
        }
        return logger;
    }

}
