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

import java.util.concurrent.TimeUnit;

/**
 * ExpiringState is accessible from the @javax.security.auth.spi.LoginModule
 * via the Map<String,?> options, under the name "ExpiringState".
 *
 */
public interface ExpiringState {

    /**
     * Puts an entry into this map with a given ttl (time to live) value
     * if the specified key is not already associated with a value.
     * Entry will expire and get evicted after the ttl.
     *
     * @param key      key of the entry
     * @param value    value of the entry
     * @param ttl      maximum time for this entry to stay in the map
     * @param timeunit time unit for the ttl
     * @return null if absent, or returns the old value of the entry
     */
    Object putIfAbsent(String key, Object value, long ttl, TimeUnit timeunit);

    /**
     * Gets the given key.
     *
     * @param key of the entry
     * @return value of the entry, or null
     */
    Object get(String key);

    /**
     * Removes the given key.
     *
     * @param key The key of the map entry to remove.
     * @return A {@link java.util.concurrent.Future} from which the value
     *         removed from the map can be retrieved.
     */
    Object remove(String key, Object value);
}
