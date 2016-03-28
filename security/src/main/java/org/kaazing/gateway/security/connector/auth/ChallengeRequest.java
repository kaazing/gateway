/**
 * Copyright 2007-2015, Kaazing Corporation. All rights reserved.
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

package org.kaazing.gateway.security.connector.auth;

/**
 * An immutable object representing the challenge presented by the server when the client accessed
 * the URI represented by a location.
 * <p/>
 * According to <a href="http://tools.ietf.org/html/rfc2617#section-1.2">RFC 2617</a>,
 * <pre>
 *     challenge   = auth-scheme 1*SP 1#auth-param
 * </pre>
 * so we model the authentication scheme and parameters in this class.
 * <p/>
 * This class is also responsible for detecting and adapting the {@code Application Basic}
 * and {@code Application Negotiate} authentication schemes into their {@code Basic} and {@code Negotiate}
 * counterpart authentication schemes.
 */
public class ChallengeRequest {

    private static final String APPLICATION_PREFIX = "Application ";

    String location;
    String authenticationScheme;
    String authenticationParameters;

    /**
     * Constructor from the protected URI location triggering the challenge,
     * and an entire server-provided 'WWW-Authenticate:' string.
     *
     * @param location  the protected URI location triggering the challenge
     * @param challenge an entire server-provided 'WWW-Authenticate:' string
     */
    public ChallengeRequest(String location, String challenge) {
        if (location == null) {
            throw new NullPointerException("location");
        }
        if (challenge == null) {
            return;
        }

        if (challenge.startsWith(APPLICATION_PREFIX)) {
            challenge = challenge.substring(APPLICATION_PREFIX.length());
        }

        this.location = location;
        this.authenticationParameters = null;

        int space = challenge.indexOf(' ');
        if (space == -1) {
            this.authenticationScheme = challenge;
        } else {
            this.authenticationScheme = challenge.substring(0, space);
            if (challenge.length() > (space + 1)) {
                this.authenticationParameters = challenge.substring(space + 1);
            }
        }
    }

    /**
     * Return the protected URI the access of which triggered this challenge as a {@code String}.
     * <p/>
     * For some authentication schemes, the production of a response to the challenge may require
     * access to the location of the URI triggering the challenge.
     *
     * @return the protected URI the access of which triggered this challenge as a {@code String}
     */
    public String getLocation() {
        return location;
    }

    /**
     * Return the authentication scheme with which the server is challenging.
     *
     * @return the authentication scheme with which the server is challenging.
     */
    public String getAuthenticationScheme() {
        return authenticationScheme;
    }

    /**
     * Return the string after the space separator, not including the authentication scheme nor the space itself,
     * or {@code null} if no such string exists.
     *
     * @return the string after the space separator, not including the authentication scheme nor the space itself,
     * or {@code null} if no such string exists.
     */
    public String getAuthenticationParameters() {
        return authenticationParameters;
    }

    @Override
    public String toString() {
        return String.format("%s: %s: %s %s", super.toString(), location, authenticationScheme, authenticationParameters);
    }
}
