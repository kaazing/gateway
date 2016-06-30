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
package org.kaazing.gateway.transport;

import java.io.Serializable;

import org.apache.mina.core.session.IoSession;

@SuppressWarnings("unchecked")
public class TypedAttributeKey<T> implements Serializable {
    /**
     * The serial version UID
     */
    private static final long serialVersionUID = -5633856647355355804L;

    /**
     * The attribute's name
     */
    private final String name;

    private final T defaultValue;


    /**
     * Creates a new instance of TypedAttributeKey.
     */
    public TypedAttributeKey(Class<?> source, String name) {
        this(source, name, null);
    }

    /**
     * Creates a new instance of TypedAttributeKeywith a default value.
     */
    public TypedAttributeKey(Class<?> source, String name, T defaultValue) {
        this.name = source.getName() + '.' + name + '@' + Integer.toHexString(this.hashCode());
        this.defaultValue = defaultValue;
    }


    /**
     * The String representation of this object is its constructed name.
     */
    @Override
    public String toString() {
        return name;
    }

    public T set(IoSession session, T value) {
        return (T)session.setAttribute(this, value);
    }

    public T setIfAbsent(IoSession session, T value) {
        return (T)session.setAttributeIfAbsent(this, value);
    }

    public T get(IoSession session) {
        return defaultValue == null ? (T)session.getAttribute(this) : get(session, defaultValue);
    }

    public T get(IoSession session, T defaultValue) {
        return (T)session.getAttribute(this, defaultValue);
    }

    public T remove(IoSession session) {
        return (T)session.removeAttribute(this);
    }

    public boolean remove(IoSession session, T value) {
        return session.removeAttribute(this, value);
    }

    public boolean replace(IoSession session, T oldValue, T newValue) {
        return session.replaceAttribute(this, oldValue, newValue);
    }

    public boolean exists(IoSession session) {
        return session.containsAttribute(this);
    }

}
