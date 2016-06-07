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
package org.apache.mina.core.session;

import java.util.Set;

/**
 * Stores the user-defined attributes which is provided per {@link IoSession}.
 * All user-defined attribute accesses in {@link IoSession} are forwarded to
 * the instance of {@link IoSessionAttributeMap}. 
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface IoSessionAttributeMap {

    /**
     * Returns the value of user defined attribute associated with the
     * specified key.  If there's no such attribute, the specified default
     * value is associated with the specified key, and the default value is
     * returned.  This method is same with the following code except that the
     * operation is performed atomically.
     * <pre>
     * if (containsAttribute(key)) {
     *     return getAttribute(key);
     * } else {
     *     setAttribute(key, defaultValue);
     *     return defaultValue;
     * }
     * </pre>
     */
    Object getAttribute(IoSession session, Object key, Object defaultValue);

    /**
     * Sets a user-defined attribute.
     *
     * @param key   the key of the attribute
     * @param value the value of the attribute
     * @return The old value of the attribute.  <tt>null</tt> if it is new.
     */
    Object setAttribute(IoSession session, Object key, Object value);

    /**
     * Sets a user defined attribute if the attribute with the specified key
     * is not set yet.  This method is same with the following code except
     * that the operation is performed atomically.
     * <pre>
     * if (containsAttribute(key)) {
     *     return getAttribute(key);
     * } else {
     *     return setAttribute(key, value);
     * }
     * </pre>
     */
    Object setAttributeIfAbsent(IoSession session, Object key, Object value);

    /**
     * Removes a user-defined attribute with the specified key.
     *
     * @return The old value of the attribute.  <tt>null</tt> if not found.
     */
    Object removeAttribute(IoSession session, Object key);

    /**
     * Removes a user defined attribute with the specified key if the current
     * attribute value is equal to the specified value.  This method is same
     * with the following code except that the operation is performed
     * atomically.
     * <pre>
     * if (containsAttribute(key) && getAttribute(key).equals(value)) {
     *     removeAttribute(key);
     *     return true;
     * } else {
     *     return false;
     * }
     * </pre>
     */
    boolean removeAttribute(IoSession session, Object key, Object value);

    /**
     * Replaces a user defined attribute with the specified key if the
     * value of the attribute is equals to the specified old value.
     * This method is same with the following code except that the operation
     * is performed atomically.
     * <pre>
     * if (containsAttribute(key) && getAttribute(key).equals(oldValue)) {
     *     setAttribute(key, newValue);
     *     return true;
     * } else {
     *     return false;
     * }
     * </pre>
     */
    boolean replaceAttribute(IoSession session, Object key, Object oldValue, Object newValue);

    /**
     * Returns <tt>true</tt> if this session contains the attribute with
     * the specified <tt>key</tt>.
     */
    boolean containsAttribute(IoSession session, Object key);

    /**
     * Returns the set of keys of all user-defined attributes.
     */
    Set<Object> getAttributeKeys(IoSession session);
    
    /**
     * Disposes any releases associated with the specified session.
     * This method is invoked on disconnection.
     */
    void dispose(IoSession session) throws Exception;
}
