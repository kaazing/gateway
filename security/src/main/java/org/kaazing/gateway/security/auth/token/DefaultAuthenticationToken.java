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
package org.kaazing.gateway.security.auth.token;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.kaazing.gateway.server.spi.security.AuthenticationToken;

/**
 * An authentication token is comprised of (usually) one or more component strings.
 * {@code DefaultAuthenticationToken} models the collection of components as an integer-indexed collection that
 * together form the complete "token".
 */
public class DefaultAuthenticationToken extends AuthenticationToken {

    /**
     * Name of the HTTP authentication scheme used to provide the token data,
     * or null if no such scheme was used (e.g. as when the data come from
     * cookies).
     */
    private String scheme;

    /**
     * An integer-indexed collection to hold token components.
     */
    private List<String> components;


    private Map<String, String> namedComponents;

    /**
     * Default constructor.
     * <p/>
     * By default, this class expects 1 component to form the token.
     */
    public DefaultAuthenticationToken() {
        this(1);
    }

    /**
     * Constructor to use when the number of components forming a token is known.
     * @param initialCapacity a positive integer denoting the number of token components.
     */
    public DefaultAuthenticationToken(int initialCapacity) {
        this.components = new ArrayList<>(initialCapacity);
        this.namedComponents = new LinkedHashMap<>(initialCapacity);
    }

    public DefaultAuthenticationToken(String component) {
        this();
        add(component);
    }

    public DefaultAuthenticationToken(String scheme, String component) {
        this();
        add(component);
        this.scheme = scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    /**
     * Add a component to this {@code DefaultAuthenticationToken} object.
     * @param component the component to add to this authentication token object.
     */
    public void add(String component) {
        components.add(component);
    }

    /**
     * Add a component to this {@code DefaultAuthenticationToken} object.
     * @param name the name under which this component can be accessed.
     * @param namedComponent the component to add to this authentication token object.
     */
    public void add(String name, String namedComponent) {
        namedComponents.put(name, namedComponent);
    }

    /**
     * Return the first component within this token.
     * @return the first component within this token, or <code>null</code> if no such component exists.
     */
    @Override
    public String get() {
        if (size() == 0) {
            return null;
        }

        return get(0);
    }

    /**
     * Retrieve the component at the specified index from this {@code DefaultAuthenticationToken} object.
     * @param index the index of the component to retrieve
     * @return the component at the specified index from this {@code DefaultAuthenticationToken} object.
     * @throws IndexOutOfBoundsException when the specified index is larger than the number of added components.
     */
    @Override
    public String get(int index) {
        if (index >= 0 && index < components.size()) {
            return components.get(index);
        }

        if (index >= components.size() && index < components.size() + namedComponents.size()) {
            int namedIndex = index - components.size();
            List<String> namedValues = new ArrayList<>(namedComponents.values());
            return namedValues.get(namedIndex);
        }

        throw new IndexOutOfBoundsException("Attempting access token component " + index +
                                            " when only " + size() + " components are available.");

    }

    /**
     * Retrieve the named component from this {@code DefaultAuthenticationToken} object.
     * @param name the name of the component to retrieve
     * @return the named component from this {@code DefaultAuthenticationToken} object,
     * or <code>null</code> if no such named component exists.
     */
    @Override
    public String get(String name) {
        return namedComponents.get(name);
    }

    /**
     * Retrieve the HTTP authentication scheme used to provide this token,
     * or <code>null</code> if no HTTP authentication scheme was used
     * (<i>e.g.</i> from cookies).
     * @return the HTTP authentication scheme, or <code>null</code> if no scheme
     * was used.
     */
    @Override
    public String getScheme() {
        return scheme;
    }

    /**
     * Return the number of components comprising this {@code DefaultAuthenticationToken}.
     * @return the number of components comprising this {@code DefaultAuthenticationToken}.
     */
    @Override
    public int size() {
        return components.size() + namedComponents.size();
    }

    @Override
    public boolean isEmpty() {
        boolean isEmpty = true;
        if (size() == 0) {
            return true;
        }
        for (int i = 0; i < size(); i++) {
            String v = get(i);
            if (v != null && v.length() > 0) {
                isEmpty = false;
            }
        }
        return isEmpty;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        sb.append(" scheme=").append(scheme);

        if (size() == 0) {
            sb.append(" <empty authentication token> ]");
            return sb.toString();
        }

        for (String component: components) {
            if (component == null) {
                sb.append(' ').append('{').append(component).append('}');

            } else {
                sb.append(' ').append("{'").append(component).append("'}");
            }
        }

        for (String name: namedComponents.keySet()) {
            sb.append(' ').append("{'").append(name).append("'->'" + namedComponents.get(name) + "'}");
        }

        sb.append(" ]");
        return sb.toString();
    }

}
