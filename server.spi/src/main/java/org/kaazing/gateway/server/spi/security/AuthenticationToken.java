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

/**
 * An authentication token is comprised of (usually) one or more component strings.
 * {@code AuthenticationToken} models the collection of components as an integer-indexed
 * and/or as a name-indexed collection that together form the complete "token".
 */
public abstract class AuthenticationToken {

    /**
     * Return the authentication scheme (<i>e.g.</i> "Basic", <i>etc</i>) that
     * was used by the client to provide this token to the server.
     * <b>Note</b> that the scheme can be <code>null</code>, <i>i.e.</i> if the
     * token data came from cookies.
     * @return the HTTP authentication scheme, or <code>null</code> is no HTTP
     * authentication scheme was used to provide the token data.
     */
    public abstract String getScheme();

    /**
     * Return the first component within this token.
     * @return the first component within this token, or <code>null</code> if no such component exists.
     */
    public abstract String get();

    /**
     * Retrieve the component at the specified index from this {@code AuthenticationToken} object.
     * @param index the index of the component to retrieve
     * @return the component at the specified index from this {@code AuthenticationToken} object.
     */
    public abstract String get(int index);

    /**
     * Retrieve the named component from this {@code DefaultAuthenticationToken} object.
     * @param name the name of the component to retrieve
     * @return the named component from this {@code DefaultAuthenticationToken} object,
     * or <code>null</code> if no such named component exists.
     */
    public abstract String get(String name);

    /**
     * Return the number of components comprising this {@code AuthenticationToken}.
     * @return the number of components comprising this {@code AuthenticationToken}.
     */
    public abstract int size();

    /**
     * Return false iff any non-null components have been added to this token.
     * @return false iff any non-null components have been added to this token.
     */
    public abstract boolean isEmpty();

}
