/**
 * Copyright (c) 2007-2017, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.demo.loginmodules;

import java.io.IOException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
import org.kaazing.gateway.server.spi.security.LoginModuleOptions;
import org.kaazing.gateway.server.spi.security.LoginResult;
import org.kaazing.gateway.server.spi.security.LoginResultCallback;

// This is an example custom login module that reads a token to extract the username. If
// successful, the username is placed into shared state for login modules downstream in
// the chain to continue authorization. For example, a downstream login module may retrieve
// the user's role or roles from LDAP and inject them into the Subject.
//
// A good starting point for understanding login modules and recommended practices is
// the Java Authentication and Authorization Service (JAAS): LoginModule Developer's Guide:
//
// http://docs.oracle.com/javase/8/docs/technotes/guides/security/jaas/JAASLMDevGuide.html

public class TokenLoginModule implements LoginModule {

    private static final String USERNAME_KEY = "javax.security.auth.login.name";

    private Subject subject;
    private CallbackHandler handler;
    private Map<String, Object> sharedState;

    private ExpiringState expiringState;

    private String username;

    // Flag to enable or disable debugging output.
    private boolean debug = false;

    @Override
    @SuppressWarnings("unchecked")
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {

        this.debug = ((String) options.get("debug")).equalsIgnoreCase("true");

        this.subject = subject;
        this.handler = callbackHandler;

        this.sharedState = (Map<String, Object>) sharedState;

        // Expiring state is passed in to login modules via the options. It allows data to be
        // shared across login module invocations, and also across gateways in the same cluster.
        this.expiringState = LoginModuleOptions.EXPIRING_STATE.get(options);

        if (debug) {
            if (options.size() > 0) {
                debug("Options:");
                options.forEach((k, v) -> debug(String.format("  %s=%s", k, v)));
            }

            if (sharedState.size() > 0) {
                debug("Shared state:");
                sharedState.forEach((k, v) -> debug(String.format("  %s=%s", k, v)));
            }
        }
    }

    @Override
    public boolean login() throws LoginException {

        try {

            AuthenticationToken authToken = getTokenFromCallback();
            if ((authToken == null) || (authToken.isEmpty())) {
                // Since a token was expected, issue a challenge to the client to provide one.
                // This will result in a 401 HTTP response back to the client.
                LoginResult loginResult = getLoginResultFromCallback();
                loginResult.challenge();

                // NOTE: Optionally, the login module can send data to the client
                // along with the challenge. The following commented out line shows
                // an example of how to do that.
                //
                // On the client side, this data will be included in the challenge
                // request and can be extracted by the client to be used in any way.
                //
                // loginResult.challenge("app-id=123456789");

                throw new LoginException("No tokens provided");
            }
            debug(String.format("Login: Obtained AuthenticationToken: %s", authToken));

            // Ensure that the token data uses the "Token" authentication scheme.
            // Note: Compare to "Token" rather than "Application Token" because the scheme
            // is just the scheme type which is Basic, Negotiate, or Token.
            String authScheme = authToken.getScheme();
            if (!authScheme.equals("Token")) {
                throw new LoginException(
                    "Expected authentication token scheme 'Token', but received '" + authToken.getScheme() + "'");
            }

            // The AuthenticationToken contains extra information (such as the scheme), so
            // extract the token data specifically.
            String tokenData = authToken.get();

            processToken(tokenData);

            return true;

        }
        catch (LoginException e) {
            debug(String.format("Login: Got a LoginException: %s", e.getMessage()));
            throw e;
        }
        catch (Exception e) {
            debug(String.format("Login: Got an Exception: %s", e.getMessage()));
            throw new LoginException(e.getMessage());
        }

    }

    @Override
    public boolean commit() throws LoginException {
        // Save the username into shared state for downstream login modules to authorize.
        debug(String.format("Commit: Saving username to shared state: %s", username));

        sharedState.put(USERNAME_KEY, username);

        if (debug) {
            if (sharedState.size() > 0) {
                debug("Commit: Shared state:");
                sharedState.forEach((k, v) -> debug(String.format("Commit:   %s=%s", k, v)));
            }
        }

        return true;
    }

    @Override
    public boolean abort() throws LoginException {
        // Clear state here, if any was created, particularly anything stored in
        // the sharedState object or sensitive data stored in memory, like passwords.

        debug("Abort: Clearing shared state");

        sharedState.remove(USERNAME_KEY);

        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        // This method removes Principals, and removes/destroys credentials associated with the
        // Subject during the commit operation. This method should not touch those Principals
        // or credentials previously existing in the Subject, or those added by other LoginModules.

        debug(String.format("Logout: Logging out %s", username));

        if (!subject.isReadOnly()) {
            // Example:
            // subject.getPrincipals(MyPrincipal.class).clear();
        }

        return true;
    }

    // Retrieve the LoginResult from the Login Context.
    private LoginResult getLoginResultFromCallback() throws LoginException, IOException, UnsupportedCallbackException {
        LoginResultCallback loginResultCallback = new LoginResultCallback();
        handler.handle(new Callback[] {
            loginResultCallback
        });
        return loginResultCallback.getLoginResult();
    }

    // Retrieve the authentication token.
    private AuthenticationToken getTokenFromCallback() throws LoginException {
        AuthenticationTokenCallback authTokenCallback = new AuthenticationTokenCallback();
        try {
            handler.handle(new Callback[] {
                authTokenCallback
            });
        }
        catch (Exception e) {
            throw new LoginException(e.getMessage());
        }
        return authTokenCallback.getAuthenticationToken();
    }

    // Validate the token and extract the username to add to shared state. If the token
    // is found to be invalid, an exception is thrown.
    //
    // This method expects the token to be in JSON format:
    //
    // {
    //   username: "joe",
    //   nonce: "5171483440790326",
    //   tokenExpires: "2017-04-20T15:48:49.187Z"
    // }
    //
    private void processToken(String tokenData) throws JSONException, LoginException {

        JSONObject json = new JSONObject(tokenData);

        // Validate that the token hasn't expired.
        ZonedDateTime expires = ZonedDateTime.parse(json.getString("tokenExpires"));
        ZonedDateTime now = ZonedDateTime.now(expires.getZone());
        if (expires.isBefore(now)) {
            throw new LoginException("Token has expired");
        }

        // The following check that expiringState is not null can be removed once the expiring state API is made
        // public. Until then, it can be enabled by setting the early access flag "login.module.expiring.state".
        // See the documentation for how to set the early access flag:
        // https://kaazing.com/doc/5.0/admin-reference/p_configure_gateway_opts/index.html#enable-early-access-features
        if (expiringState != null) {

            // Validate that the token hasn't been used already. If not, then store it until it expires in case
            // it is replayed.
            long duration = Duration.between(now, expires).toMillis();
            String nonce = json.getString("nonce");
            if (expiringState.putIfAbsent(nonce, nonce, duration, TimeUnit.MILLISECONDS) != null) {
                throw new LoginException(String.format("Token nonce has already been used: %s", nonce));
            }

        }

        // Token has passed validity checks. Store the username to be used by the commit() method.
        this.username = json.getString("username");

        debug(String.format("Login: Token is valid for user %s", username));

    }

    // Write out debug statements with a prefix so they're easy to see in the output.
    private void debug(String message) {
        if (debug) {
            System.out.println(String.format("[%s] %s", this.getClass().getSimpleName(), message));
        }
    }

}
