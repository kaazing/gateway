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

import com.hazelcast.core.IMap;
import com.hazelcast.core.EntryView;
import com.hazelcast.map.listener.MapListener;

import java.util.concurrent.TimeUnit;

public abstract class IMapProxy<K, V> extends ConcurrentMapProxy<K, V> implements IMap<K, V> {

    @Override
    public String addEntryListener(MapListener listener, boolean includeValue) {
        return getDelegate().addEntryListener(listener, includeValue);
    }

    @Override
    public EntryView<K, V> getEntryView(K key) {
        return getDelegate().getEntryView(key);
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
    public boolean removeEntryListener(String name) {
        return getDelegate().removeEntryListener(name);
    }

    @Override
    public boolean tryLock(K key, long time, TimeUnit timeunit) {
        try {
            return getDelegate().tryLock(key, time, timeunit);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return false;
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
    protected abstract IMap<K, V> getDelegate();
}
