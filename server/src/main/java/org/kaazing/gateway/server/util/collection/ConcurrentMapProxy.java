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
package org.kaazing.gateway.server.util.collection;

import java.util.concurrent.ConcurrentMap;

public abstract class ConcurrentMapProxy<K, V> extends MapProxy<K, V> implements ConcurrentMap<K, V> {

    @Override
    public V putIfAbsent(K key, V value) {
        return getDelegate().putIfAbsent(key, value);
    }

    @Override
    public boolean remove(Object key, Object value) {
        return getDelegate().remove(key, value);
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        return getDelegate().replace(key, oldValue, newValue);
    }

    @Override
    public V replace(K key, V value) {
        return getDelegate().replace(key, value);
    }

    @Override
    protected abstract ConcurrentMap<K, V> getDelegate();
}
