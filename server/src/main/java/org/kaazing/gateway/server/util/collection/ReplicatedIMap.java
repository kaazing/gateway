/**
 * Copyright (c) 2007-2014 Kaazing Corporation. All rights reserved.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.kaazing.gateway.server.util.collection;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.IMap;
import com.hazelcast.monitor.LocalMapStats;
import com.hazelcast.query.Expression;
import com.hazelcast.query.Predicate;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.kaazing.gateway.service.cluster.EntryListenerSupport;

public class ReplicatedIMap<K, V> extends IMapProxy<K, V> implements Serializable {

    private static final long serialVersionUID = 1L;

    private final IMap<K, V> delegate;
    private transient EntryListenerSupport<K, V> listenerSupport;

    private transient Map<K, V> localCache;
    private transient Lock localCacheLock;

    public ReplicatedIMap(IMap<K, V> delegate) {
        if (delegate == null) {
            throw new NullPointerException("delegate");
        }
        this.delegate = delegate;

        init();
    }

    @Override
    public void addEntryListener(EntryListener<K, V> listener, boolean includeValue) {
        listenerSupport.addEntryListener(listener, includeValue);
    }

    @Override
    public void addEntryListener(EntryListener<K, V> listener, K key, boolean includeValue) {
        listenerSupport.addEntryListener(listener, key, includeValue);
    }

    @Override
    public void removeEntryListener(EntryListener<K, V> listener, K key) {
        listenerSupport.removeEntryListener(listener, key);
    }

    @Override
    public void removeEntryListener(EntryListener<K, V> listener) {
        listenerSupport.removeEntryListener(listener);
    }

    @Override
    public V putIfAbsent(K key, V value) {
        V oldValue = super.putIfAbsent(key, value);
        if (oldValue == null) {
            localCache.put(key, value);
            return null;
        } else {
            return localCache.get(key);
        }
    }

    @Override
    public V get(Object key) {
        return localCache.get(key);
//        if( localValue != null ) {
//            return localValue;
//        }
//        V clusterValue = super.get(key);
//        if( clusterValue != null ) {
//            localCache.put((K)key, clusterValue);
//        }
//        return clusterValue;
    }

    @Override
    protected IMap<K, V> getDelegate() {
        return delegate;
    }

    private void init() {
        this.listenerSupport = new EntryListenerSupport<>();

        // support eviction and instance equality
        this.localCache = new HashMap<>(delegate);
        this.localCacheLock = new ReentrantLock();

        delegate.addEntryListener(new EntryListener<K, V>() {

            @Override
            @SuppressWarnings("unchecked")
            public void entryAdded(EntryEvent<K, V> event) {
                K key = event.getKey();
                V newValue = event.getValue();

                EntryEvent localEvent = event;
                try {
                    localCacheLock.lock();
                    V value = localCache.get(key);
                    if (value == null) {
                        localCache.put(key, newValue);
                    } else {
                        localEvent = new EntryEvent(event.getName(), null, EntryEvent.TYPE_ADDED, key, value);
                    }
                } finally {
                    localCacheLock.unlock();
                }

                listenerSupport.entryAdded(localEvent);
            }

            @Override
            public void entryEvicted(EntryEvent<K, V> event) {
                // ignore, we need instance equality
            }

            @Override
            @SuppressWarnings("unchecked")
            public void entryRemoved(EntryEvent<K, V> event) {
                K key = event.getKey();
                V oldValue = localCache.remove(key);
                EntryEvent localEvent = new EntryEvent(event.getName(), null, EntryEvent.TYPE_REMOVED, key, oldValue);
                listenerSupport.entryRemoved(localEvent);
            }

            @Override
            public void entryUpdated(EntryEvent<K, V> event) {
                K key = event.getKey();
                V newValue = event.getValue();
                localCache.put(key, newValue);
                listenerSupport.entryUpdated(event);
            }

        }, true);
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        init();
    }

    @Override
    public Set<java.util.Map.Entry<K, V>> entrySet() {
        return localCache.entrySet();
    }

    @Override
    public Set<K> keySet() {
        return localCache.keySet();
    }

    @Override
    public Collection<V> values() {
        return localCache.values();
    }

    @Override
    public String toString() {
        return localCache.toString();
    }

    @Override
    public void addIndex(String attribute, boolean ordered) {
        throw new UnsupportedOperationException("addIndex");
    }

    @Override
    public Set<java.util.Map.Entry<K, V>> entrySet(Predicate predicate) {
        throw new UnsupportedOperationException("entrySet");
    }

    @Override
    public boolean evict(Object key) {
        if (localCache.containsKey(key)) {
            localCache.remove(key);
            return true;
        }
        return false;
    }

    @Override
    public LocalMapStats getLocalMapStats() {
        throw new UnsupportedOperationException("getLocalMapStats");
    }

    @Override
    public Set<K> keySet(Predicate predicate) {
        throw new UnsupportedOperationException("keySet");
    }

    @Override
    public Set<K> localKeySet() {
        throw new UnsupportedOperationException("localKeySet");
    }

    @Override
    public Set<K> localKeySet(Predicate predicate) {
        throw new UnsupportedOperationException("localKeySet");
    }

    @Override
    public boolean lockMap(long time, TimeUnit timeunit) {
        throw new UnsupportedOperationException("lockMap");
    }

    @Override
    public V put(K key, V value, long ttl, TimeUnit timeunit) {
        throw new UnsupportedOperationException("put");
    }

    @Override
    public V putIfAbsent(K key, V value, long ttl, TimeUnit timeunit) {
        throw new UnsupportedOperationException("putIfAbsent");
    }

    @Override
    public boolean tryPut(K key, V value, long timeout, TimeUnit timeunit) {
        throw new UnsupportedOperationException("tryPut");
    }

    @Override
    public void unlockMap() {
        throw new UnsupportedOperationException("unlockMap");
    }

    @Override
    public Collection<V> values(Predicate predicate) {
        throw new UnsupportedOperationException("values");
    }

    @Override
    public Future<V> getAsync(K key) {
        throw new UnsupportedOperationException("getAsync");
    }

    @Override
    public Future<V> putAsync(K key, V value) {
        throw new UnsupportedOperationException("putAsync");
    }

    @Override
    public void addIndex(Expression<?> arg0, boolean arg1) {
        throw new UnsupportedOperationException("addIndex");
    }

    @Override
    public void addLocalEntryListener(EntryListener<K, V> arg0) {
        throw new UnsupportedOperationException("addLocalEntryListener");
    }

    @Override
    public void flush() {
        throw new UnsupportedOperationException("flush");
    }

    @Override
    public Map<K, V> getAll(Set<K> arg0) {
        throw new UnsupportedOperationException("getAll");
    }

    @Override
    public void putAndUnlock(K arg0, V arg1) {
        throw new UnsupportedOperationException("putAndUnlock");
    }

    @Override
    public void putTransient(K arg0, V arg1, long arg2, TimeUnit arg3) {
        throw new UnsupportedOperationException("putTransient");
    }

    @Override
    public Future<V> removeAsync(K arg0) {
        throw new UnsupportedOperationException("removeAsync");
    }

    @Override
    public V tryLockAndGet(K arg0, long arg1, TimeUnit arg2)
            throws TimeoutException {
        throw new UnsupportedOperationException("tryLockAndGet");
    }

    @Override
    public Object tryRemove(K arg0, long arg1, TimeUnit arg2)
            throws TimeoutException {
        throw new UnsupportedOperationException("tryRemove");
    }

}
