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

import com.hazelcast.core.EntryListener;
import com.hazelcast.core.IMap;
import com.hazelcast.core.MapEntry;
import java.util.concurrent.TimeUnit;

public abstract class IMapProxy<K, V> extends ConcurrentMapProxy<K, V> implements IMap<K, V> {

    @Override
    public void addEntryListener(EntryListener<K, V> listener, boolean includeValue) {
        getDelegate().addEntryListener(listener, includeValue);
    }

    @Override
    public void addEntryListener(EntryListener<K, V> listener, K key,
                                 boolean includeValue) {
        getDelegate().addEntryListener(listener, key, includeValue);
    }

    @Override
    public MapEntry<K, V> getMapEntry(K key) {
        return getDelegate().getMapEntry(key);
    }

    @Override
    public String getName() {
        return getDelegate().getName();
    }

    @Override
    public void lock(K key) {
        getDelegate().lock(key);
    }

    @Override
    public void removeEntryListener(EntryListener<K, V> listener, K key) {
        getDelegate().removeEntryListener(listener, key);
    }

    @Override
    public void removeEntryListener(EntryListener<K, V> listener) {
        getDelegate().removeEntryListener(listener);
    }

    @Override
    public boolean tryLock(K key, long time, TimeUnit timeunit) {
        return getDelegate().tryLock(key, time, timeunit);
    }

    @Override
    public boolean tryLock(K key) {
        return getDelegate().tryLock(key);
    }

    @Override
    public void unlock(K key) {
        getDelegate().unlock(key);
    }

    @Override
    public void destroy() {
        getDelegate().destroy();
    }

    @Override
    public Object getId() {
        return getDelegate().getId();
    }

    @Override
    public InstanceType getInstanceType() {
        return getDelegate().getInstanceType();
    }

    @Override
    protected abstract IMap<K, V> getDelegate();
}
