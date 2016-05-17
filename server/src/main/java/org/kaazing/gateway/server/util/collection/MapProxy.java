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

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public abstract class MapProxy<K, V> implements Map<K, V> {

    @Override
    public void clear() {
        getDelegate().clear();
    }

    @Override
    public boolean containsKey(Object key) {
        return getDelegate().containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return getDelegate().containsValue(value);
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return getDelegate().entrySet();
    }

    @Override
    public V get(Object key) {
        return getDelegate().get(key);
    }

    @Override
    public boolean isEmpty() {
        return getDelegate().isEmpty();
    }

    @Override
    public Set<K> keySet() {
        return getDelegate().keySet();
    }

    @Override
    public V put(K key, V value) {
        return getDelegate().put(key, value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        getDelegate().putAll(m);
    }

    @Override
    public V remove(Object key) {
        return getDelegate().remove(key);
    }

    @Override
    public int size() {
        return getDelegate().size();
    }

    @Override
    public Collection<V> values() {
        return getDelegate().values();
    }

    protected abstract Map<K, V> getDelegate();
}
