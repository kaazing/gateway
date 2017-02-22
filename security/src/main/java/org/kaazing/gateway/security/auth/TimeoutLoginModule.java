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
package org.kaazing.gateway.security.auth;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;

import org.kaazing.gateway.server.spi.security.LoginResult;
import org.kaazing.gateway.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This login module should be used to manage the session timeout and authorization timeout of the WebSocket session.
 * <p>
 *     Note: If configured for an HTTP service, this login module will not have any effect.
 * </p>
 *
 *  The JAAS success type is suggested to be requisite since if there is an error while establishing
 *  session lifetime settings there is no point to running further login modules in the chain.
 *
 *  The following options are available for use with the Lifetime Login mdoule:
 * <pre>
 *  <debug>true<debug>    When set to true, enables debug information at the DEBUG level to be sent.
 *  When false, no such logging information is sent to that logger.
 *
 *  &lt;session-timeout&gt;30 minutes&lt;/session-timeout&gt;    When specified, the WebSocket session will remain connected
 *
 *  no longer than the time interval provided.  Equivalent to {@link LoginResult#setSessionTimeout}.
 *
 *
 * </pre>
 */
public class TimeoutLoginModule extends BaseStateDrivenLoginModule {

    public static final String CLASS_NAME = TimeoutLoginModule.class.getName();
    public static final Logger LOGGER = LoggerFactory.getLogger(CLASS_NAME);

    private static final String INITIALIZATION_FAILED_MESSAGE = "[TimeoutLoginModule] Initialization failed";
    private static final String AUTHENTICATION_FAILED_MESSAGE = "[TimeoutLoginModule] Authentication failed";
    private static final String SESSION_TIMEOUT_KEY = "session-timeout";

    private boolean debug;
    private Long sessionTimeout;
    private boolean forceFailure;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler,
                           Map<String, ?> sharedState, Map<String, ?> options) {
        super.initialize(subject, callbackHandler, sharedState, options);
        this.debug = "true".equalsIgnoreCase((String) options.get("debug"));
        this.forceFailure = "true".equalsIgnoreCase((String) options.get("force-failure"));
        this.sessionTimeout = readOption(options, SESSION_TIMEOUT_KEY);
        if (sessionTimeout == null) {
            IllegalArgumentException iae =
                    new IllegalArgumentException(String.format("You must specify the \"%s\" option", SESSION_TIMEOUT_KEY));
            if (debug) {
                LOGGER.debug(INITIALIZATION_FAILED_MESSAGE, iae);
            }
            throw iae;
        }

        if (debug) {
            LOGGER.debug("[TimeoutLoginModule] session timeout configured as '{}'", sessionTimeout);
        }
    }


    @Override
    protected boolean doLogin() throws LoginException {
        if (loginResult == null) {
            LoginException le = new LoginException("Missing login result");
            if (debug) {
                LOGGER.debug(AUTHENTICATION_FAILED_MESSAGE, le);
            }
            throw le;
        }

        boolean performedAction = false;
        if (this.sessionTimeout != null) {
            if (debug) {
                LOGGER.debug("[TimeoutLoginModule] Setting session timeout to '{}'", sessionTimeout);
            }
            loginResult.setSessionTimeout(sessionTimeout);
            performedAction = true;

        }

        // Clean up in case of failure to ensure nothing is set.
        if (!performedAction || this.forceFailure) {
            LoginException le = new LoginException("Unable to set timeout");
            if (debug) {
                LOGGER.debug(AUTHENTICATION_FAILED_MESSAGE, le);
            }
            cleanState();
            throw le;
        }

        return performedAction;
    }

    @Override
    protected boolean doCommit() throws LoginException {
        return true;
    }

    @Override
    protected boolean doLogout() throws LoginException {
        cleanState();
        return true;
    }

    private Long readOption(Map<String, ?> options, final String key) {
        final String timeIntervalValue = (String) options.get(key);
        if (timeIntervalValue == null) {
            return null;
        } else {
            try {
                return  Utils.parseTimeInterval(timeIntervalValue, TimeUnit.SECONDS);
            } catch (NumberFormatException e) {
                LOGGER.error("[TimeoutLoginModule] Cannot determine the value for {}", key, e);
                throw e;
            }
        }
    }

    private void cleanState() {
        this.sessionTimeout = 0L;
        if (loginResult != null) {
            ((DefaultLoginResult) loginResult).clearTimeouts();
        }
    }
}
