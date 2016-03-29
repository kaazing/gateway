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

package org.kaazing.gateway.transport.http.security.auth.connector;

import org.kaazing.gateway.security.connector.auth.ChallengeHandler;

/**
 * Challenge handler for Basic authentication as defined in <a href="http://tools.ietf.org/html/rfc2617#section-2">RFC 2617.</a>
 * <p/>
 * This BasicChallengeHandler can be loaded and instantiated using
 * {@link BasicChallengeHandler#create()}, and registered
 * at a location using {@link DispatchChallengeHandler#register(String, ChallengeHandler)}.
 * <p/>
 * In addition, one can install general and realm-specific {@link LoginHandler} objects onto this
 * {@link BasicChallengeHandler} to assist in handling challenges associated
 * with any or specific realms.  This can be achieved using {@link #setLoginHandler(LoginHandler)} and
 * {@link #setRealmLoginHandler(String, LoginHandler)} methods.
 * <p/>
 *
 * @see <a href="http://tools.ietf.org/html/rfc2616">RFC 2616 - HTTP 1.1</a>
 * @see <a href="http://tools.ietf.org/html/rfc2617#section-2">RFC 2617 Section 2 - Basic Authentication</a>
 */
public abstract class BasicChallengeHandler extends ChallengeHandler {

    /**
     * Set a Login Handler to be used if and only if a challenge request has
     * a realm parameter matching the provided realm.
     *
     * @param realm  the realm upon which to apply the {@code loginHandler}.
     * @param loginHandler the login handler to use for the provided realm.
     */
    public abstract void setRealmLoginHandler(String realm, LoginHandler loginHandler);


    /**
     * Provide a general (non-realm-specific) login handler to be used in association with this challenge handler.
     * The login handler is used to assist in obtaining credentials to respond to any Basic
     * challenge requests when no realm-specific login handler matches the realm parameter of the request (if any).
     *
     * @param loginHandler a login handler for credentials.
     */
    public abstract BasicChallengeHandler setLoginHandler(LoginHandler loginHandler);

    /**
     * Get the general (non-realm-specific) login handler associated with this challenge handler.
     * A login handler is used to assist in obtaining credentials to respond to challenge requests.
     *
     * @return a login handler to assist in providing credentials, or {@code null} if none has been established yet.
     */
    public abstract LoginHandler getLoginHandler() ;
}
