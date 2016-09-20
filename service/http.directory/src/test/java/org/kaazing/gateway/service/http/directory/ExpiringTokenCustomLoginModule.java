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
package org.kaazing.gateway.service.http.directory;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.kaazing.gateway.server.ExpiringState;
import org.kaazing.gateway.server.spi.security.AuthenticationToken;
import org.kaazing.gateway.server.spi.security.AuthenticationTokenCallback;
import org.kaazing.gateway.server.spi.security.LoginResult;
import org.kaazing.gateway.server.spi.security.LoginResultCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExpiringTokenCustomLoginModule implements LoginModule {
    public static final String CLASS_NAME = ExpiringTokenCustomLoginModule.class.getName();
    public static final String LOG_PREFIX = "[LM] ";
    public static final Logger logger = LoggerFactory.getLogger(CLASS_NAME);
    private static final String NUMBER_OF_ATTEMPTS_KEY = "NUMBER_OF_ATTEMPTS";
    private Subject subject;
    private CallbackHandler handler;
    private Map<String, ?> options;
    private UserPrincipal userPrincipal;
    private RolePrincipal rolePrincipal;
    private ExpiringState expiringState;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        logDebug("initialize()");

        this.subject = subject;
        this.handler = callbackHandler;
        this.options = options;

        Set<?> keys = this.options.keySet();
        if (keys.size() > 0) {
            logDebug("Options:");
            for (Object key : keys) {
                logDebug("  " + key + "=" + this.options.get(key));
            }
        }
        expiringState = (ExpiringState) options.get("ExpiringState");

    }

    @Override
    public boolean login() throws LoginException {

        Integer numOfAttempts = (Integer) expiringState.get(NUMBER_OF_ATTEMPTS_KEY);
        if(numOfAttempts != null || numOfAttempts > 0){
            // pass login send 200
            // TO CONFIRM I think you return true
            return true;
        }
        expiringState.putIfAbsent(NUMBER_OF_ATTEMPTS_KEY, new Integer(1), 10, TimeUnit.SECONDS);
        //fail login / send 401
        logDebug("401 - login failed!!");

        return false;
    }

    @Override
    public boolean commit() throws LoginException {
        logDebug("commit()");
        if (this.userPrincipal == null) {
            logDebug("No user principal added");
            return false;
        }
        this.subject.getPrincipals().add(this.userPrincipal);

        logDebug("User principal added: " + this.userPrincipal.getName());

        if (this.rolePrincipal == null) {
            logDebug("No role granted");
            return false;
        }
        this.subject.getPrincipals().add(this.rolePrincipal);

        logDebug("User granted role: " + this.rolePrincipal.getName());

        Set<Principal> principals = this.subject.getPrincipals();
        for (Principal principal : principals) {
            logDebug("Principal: " + principal.getName() + " [" + principal.getClass().getName() + "]");
        }
        return true;
    }

    @Override
    public boolean abort() throws LoginException {
        logDebug("abort()");
        if (this.userPrincipal == null) {
            logDebug("No user principal");
            return false;
        }
        if (this.rolePrincipal == null) {
            logDebug("No role granted");
            return false;
        }
        try {
            clearRole();

            return true;
        } catch (Exception ex) {
            LoginException e = new LoginException("Unexpected error during abort().");
            e.initCause(ex);
            logError(e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public boolean logout() throws LoginException {
        logDebug("logout()");
        try {
            clearRole();

            return true;
        } catch (Exception ex) {
            LoginException e = new LoginException("Unexpected error during logout().");
            e.initCause(ex);
            logError(e.getMessage(), e);
            throw e;
        }
    }

    private void logDebug(String message) {
        logger.debug(LOG_PREFIX + message);
    }

    private void logError(String message, Exception exception) {
        logger.error(LOG_PREFIX + message, exception);
    }

    private LoginResult getLoginResultFromCallback() {
        LoginResultCallback loginResultCallback = new LoginResultCallback();
        try {
            this.handler.handle(new Callback[]{loginResultCallback});
        } catch (IOException ioe) {
            logError("Encountered exception handling loginResultCallback", ioe);
            return null;
        } catch (UnsupportedCallbackException uce) {
            logError("UnsupportedCallbackException handling loginResultCallback", uce);
            return null;
        }
        return loginResultCallback.getLoginResult();
    }

    private AuthenticationToken getTokenFromCallback() {
        AuthenticationTokenCallback authTokenCallback = new AuthenticationTokenCallback();
        try {
            this.handler.handle(new Callback[]{authTokenCallback});
        } catch (IOException ioe) {
            logError("Encountered exception handling authTokenCallback", ioe);
            return null;
        } catch (UnsupportedCallbackException uce) {
            logError("UnsupportedCallbackException handling authenticationTokenCallback", uce);
            return null;
        }
        AuthenticationToken authToken = authTokenCallback.getAuthenticationToken();

        return authToken;
    }

    private String isTokenValid(String tokenData) throws FailedLoginException {
        if (tokenData == null) {
            throw new FailedLoginException("No Siteminder Token available to validate");
        }

        if (tokenData.compareToIgnoreCase("trump") == 0) {
            throw new FailedLoginException("This user is not authorized");
        }

        return tokenData;
    }

    private void clearRole() {
        this.subject.getPrincipals().remove(this.userPrincipal);
        this.subject.getPrincipals().remove(this.rolePrincipal);
    }

    static class RolePrincipal implements Principal {
        @Override
        public String getName() {
            return "TEST";
        }
    }

    static class UserPrincipal implements Principal {
        String username;

        public UserPrincipal(String username) {
            this.username = username;
        }

        @Override
        public String getName() {
            return username;
        }
    }

}