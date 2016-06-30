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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.security.auth.login.LoginException;

import org.kaazing.gateway.server.spi.security.LoginResult;

/**
 * Represents the result of logging in to the gateway.
 * This default implementation is assumed by the LoginContextFactory code.
 */
public class DefaultLoginResult extends LoginResult {


    /* Constant for 5 minutes as seconds */
    private static final long FIVE_MINUTES = TimeUnit.MINUTES.toSeconds(5);
    static final long[] NO_PERIODS_AVAILABLE = new long[]{};

    Type loginResultType;
    LoginException loginException;

    /**
     * The challenge data to be sent back to the client when login fails and a challenge is necessary
     */
    Set<Object> loginChallengeData = new HashSet<>();

    /**
     * The authorization data to be made available to the gateway when login succeeds.
     * See KG-3179.
     */
    Object loginAuthorizationAttachment;

    /**
     * Session timeout of this login result or <code>null</code> if none specified
     */
    private Long sessionTimeout;

    /**
     * Once calculated, this is a cache for revalidate period[0] and timeout[1] values
     */
    private long[] calculatedPeriodTimeoutValues = NO_PERIODS_AVAILABLE;

    public DefaultLoginResult() {
        super();
        // Default to success to minimize the need to use the login result facility
        // if the customer does not want to use it.
        loginResultType = Type.SUCCESS;
    }

    @Override
    public Type getType() {
        return loginResultType;
    }

    @Override
    public void challenge(Object... challengeData) {
        loginResultType = Type.CHALLENGE;

        // Append any given challenge data to the existing list
        if (challengeData != null) {
            Collections.addAll(loginChallengeData, challengeData);
        }
    }

    public void setAuthorizationAttachment(Object authorizationAttachment) {
        if (authorizationAttachment != null) {
            this.loginAuthorizationAttachment = authorizationAttachment;
        }
    }

    @Override
    public void failure(LoginException e) {
        loginResultType = Type.FAILURE;
        loginException = e;
    }

    @Override
    public void success() {
        loginResultType = Type.SUCCESS;
    }


    public void clearTimeouts() {
        this.sessionTimeout = null;
    }

    @Override
    public void setSessionTimeout(long deltaSeconds) throws IllegalArgumentException {
        if (deltaSeconds < 0) {
            throw new IllegalArgumentException("session timeout cannot be negative.");
        }
        if (deltaSeconds == 0) {
            failure(new LoginException("session timeout was set to zero, thereby invalidating this Login Result."));
        } else {
            sessionTimeout = deltaSeconds;
        }
    }

    /**
     * The session timeout specified for this {@code LoginResult}, or <code>null</code>
     * if no session timeout has been specified.
     *
     * @return the session timeout specified for this {@code LoginResult}, or <code>null</code>
     *         if no session timeout has been specified.
     */
    public Long getSessionTimeout() {
        return sessionTimeout;
    }

    public LoginException getLoginException() {
        return loginException;
    }

    public Object[] getLoginChallengeData() {
        if (loginChallengeData.isEmpty()) {
            return null;
        }

        return loginChallengeData.toArray();
    }

    public boolean hasLoginAuthorizationAttachment() {
        return this.loginAuthorizationAttachment != null;
    }

    public Object getLoginAuthorizationAttachment() {
        return loginAuthorizationAttachment;
    }


}
