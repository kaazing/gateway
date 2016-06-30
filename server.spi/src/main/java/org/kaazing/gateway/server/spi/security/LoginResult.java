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
package org.kaazing.gateway.server.spi.security;

import javax.security.auth.login.LoginException;

/**
 * {@code LoginResult} describes to the gateway the result of a login attempt, including
 * additional authentication parameters as are necessary for service connection authentication.
 * <p>
 * When used within a {@link javax.security.auth.spi.LoginModule} via a {@link LoginResultCallback},
 * this class can communicate additional authentication parameters to the gateway.  Specifically,
 * this class can establish a session timeout parameter for this {@code LoginResult} to establish
 * a lifetime for the WebSocket connection built based upon this {@code LoginResult}; and an authentication
 * lifetime parameter for the {@code LoginResult} to establish a time before which the WebSocket connection
 * expects to be re-authenticated lest it be closed.
 */
public abstract class LoginResult {

    /**
     * {@code Type} describes the result type of a Gateway Login attempt.
     */
    public enum Type {

        /**
         * Login attempt was successful.
         */
        SUCCESS,

        /**
         * Login attempt was not successful, and further authentication should not proceed.
         */
        FAILURE,

        /**
         * Login attempt was not successful, and further authentication may proceed.
         */
        CHALLENGE
    }

    protected LoginResult() {
    }

    /**
     * This result describes login {@link org.kaazing.gateway.server.spi.security.LoginResult.Type#SUCCESS}.
     */
    public abstract void success();

    /**
     * This result describes login {@link org.kaazing.gateway.server.spi.security.LoginResult.Type#FAILURE}.
     *
     * @param e An exception describing the cause for failure.
     *          May be <code>null</code> if no exception is available.
     */
    public abstract void failure(LoginException e);

    /**
     * This result describes {@link org.kaazing.gateway.server.spi.security.LoginResult.Type#CHALLENGE}.
     * Further authentication with the client may proceed, using the provided challenge data.
     *
     * @param challengeData Login-specific data to be used to continue to communicate with the client
     *             process to achieve authentication.  Should not be <code>null</code> or zero-length
     *             if authentication is to proceed.  By design, the challenge data provided here should
     *             appear space-separated in the WWW-Authenticate: HTTP header in the response to the client, provided
     *             the final result of the login process is to challenge to client.
     */
    public abstract void challenge(Object... challengeData);

    /**
     * Obtain the type of this login result.
     *
     * @return the {@link org.kaazing.gateway.server.spi.security.LoginResult.Type} this object contains.
     */
    public abstract Type getType();

    /**
     * The session timeout is the time interval (in seconds) after which any WebSocket sessions authenticated with
     * this login result will become invalid and closed.  When specified, this session timeout property is the
     * way for a {@code LoginModule} implementor to enforce the session timeout of WebSocket sessions.
     *
     * <p>
     * If the session timeout is not specified on this {@code LoginResult},
     * the WebSocket session created using this {@code LoginResult} does not have any lifetime restrictions
     * imposed upon it.
     * <p>
     * Setting the session timeout property to 0 indicates that this login result is not valid.
     * Login will fail in this case.
     * <p>
     * Setting the session timeout property to a non-negative integer indicates that this login result
     * is valid for that number of seconds.  The WebSocket session constructed on the basis of this
     * login result will close after that many seconds.
     * <p>
     * Most often, it is expected that the value for {@link #setSessionTimeout(long)} is specified for
     * the purpose of providing a guarantee of a session timeout of a WebSocket session.
     * <p>
     * This property is similar to the {@code maxAge} property of cookies in
     * <a href="http://www.ietf.org/rfc/rfc2109.txt">RFC 2109</a>, in the sense that the lifetime of the cookie
     * is determined by that property, and setting the property to 0 indicates invalidation of the cookie.
     *
     * @param deltaSeconds the time (in seconds) for which WebSocket sessions built based on this LoginResult are open.
     * @throws IllegalArgumentException when an invalid (negative) session timeout is attempted.
     *
     */
    public abstract void setSessionTimeout(long deltaSeconds) throws IllegalArgumentException;

}
