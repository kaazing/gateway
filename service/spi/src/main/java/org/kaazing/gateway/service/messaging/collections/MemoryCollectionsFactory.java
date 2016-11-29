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
package org.kaazing.gateway.service.messaging.collections;

import static java.lang.System.currentTimeMillis;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.kaazing.gateway.service.cluster.EntryListenerSupport;
import org.kaazing.gateway.util.AtomicCounter;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryEventType;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.EntryView;
import com.hazelcast.core.ExecutionCallback;
import com.hazelcast.core.ICollection;
import com.hazelcast.core.ICompletableFuture;
import com.hazelcast.core.ICondition;
import com.hazelcast.core.IList;
import com.hazelcast.core.ILock;
import com.hazelcast.core.IMap;
import com.hazelcast.core.IQueue;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.ItemEvent;
import com.hazelcast.core.ItemEventType;
import com.hazelcast.core.ItemListener;
import com.hazelcast.core.MapEvent;
import com.hazelcast.map.EntryProcessor;
import com.hazelcast.map.MapInterceptor;
import com.hazelcast.map.listener.MapListener;
import com.hazelcast.map.listener.MapPartitionLostListener;
import com.hazelcast.mapreduce.JobTracker;
import com.hazelcast.mapreduce.aggregation.Aggregation;
import com.hazelcast.mapreduce.aggregation.Supplier;
import com.hazelcast.monitor.LocalMapStats;
import com.hazelcast.monitor.LocalQueueStats;
import com.hazelcast.query.Predicate;

public class MemoryCollectionsFactory implements CollectionsFactory {

    private final ConcurrentMap<String, IMapImpl<?, ?>> maps;
    private final ConcurrentMap<String, IListImpl<?>> lists;
    private final Map<String, ILockImpl> locks;
    private final ConcurrentMap<String, AtomicCounter> atomicCounters;

    public MemoryCollectionsFactory() {
        // TODO: avoid memory leak
        maps = new ConcurrentHashMap<>();
        lists = new ConcurrentHashMap<>();
        locks = Collections.synchronizedMap(new WeakHashMap<>());
        atomicCounters = new ConcurrentHashMap<>();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E> IList<E> getList(String name) {
        IListImpl<E> list = (IListImpl<E>)lists.get(name);
        if (list == null) {
            IListImpl<E> newList = new IListImpl<>(name);
            list = (IListImpl<E>)lists.putIfAbsent(name, newList);
            if (list == null) {
                list = newList;
            }
        }
        return list;
    }

    @Override
    public <E> IQueue<E> getQueue(String name) {
        // TODO: what should be the queue size
        return new IQueueImpl<>(name, 100);
    }

    @Override
    public <E> ITopic<E> getTopic(String name) {
        throw new UnsupportedOperationException("getTopic");
    }

    @SuppressWarnings("unchecked")
    @Override
    public <K, V> IMap<K, V> getMap(String name) {
        IMapImpl<K, V> map = (IMapImpl<K, V>)maps.get(name);
        if (map == null) {
            IMapImpl<K, V> newMap = new IMapImpl<>(name);
            map = (IMapImpl<K, V>)maps.putIfAbsent(name, newMap);
            if (map == null) {
                map = newMap;
            }
        }
        return map;
    }

    @Override
    public ILock getLock(String name) {
        synchronized (locks) {
            ILock lock = locks.get(name);
            if (lock == null) {
                ILockImpl newLock = new ILockImpl(name);
                locks.put(name, newLock);
                lock = newLock;
            }
            assert (lock != null);
            return lock;
        }
    }

    @Override @SuppressWarnings("unchecked")
    public <K, V> void addEntryListener(EntryListener<K, V> listener, String name) {
        IMapImpl<K ,V> map = (IMapImpl<K, V>)getMap(name);  // force create if not already created.
        map.addEntryListener(listener, true);
    }

    @Override
    public AtomicCounter getAtomicCounter(String name) {
        if (atomicCounters.containsKey(name)) {
            return atomicCounters.get(name);
        } else {
            StandaloneAtomicCounter counter = new StandaloneAtomicCounter(new AtomicLong(0));
            AtomicCounter existingCounter = atomicCounters.putIfAbsent(name, counter);
            if (existingCounter == null) {
                existingCounter = counter;
            }
            return existingCounter;
        }
    }

    private class StandaloneAtomicCounter implements AtomicCounter {
        private AtomicLong atomicLong;

        private StandaloneAtomicCounter(AtomicLong number) {
            atomicLong = number;
        }

        @Override
        public long get() {
            return atomicLong.get();
        }

        @Override
        public long incrementAndGet() {
            return atomicLong.incrementAndGet();
        }

        @Override
        public long decrementAndGet() {
            return atomicLong.decrementAndGet();
        }

        @Override
        public boolean compareAndSet(long expect, long update) {
            return atomicLong.compareAndSet(expect, update);
        }
    }

    private class IMapImpl<K, V> implements IMap<K, V> {

        private static final String OPERATION_NOT_SUPPORTED_MESSAGE = "Operation %s not supported";

        private final ConcurrentHashMap<K, V> map;
        private final ConcurrentHashMap<K, Long> keyExpirations;
        private final EntryListenerSupport<K,V> listenerSupport;
        private final ConcurrentMap<Object, Lock> locks;
        private final String name;

        public IMapImpl(String name) {
            this.name = name;
            this.map = new ConcurrentHashMap<>();
            this.listenerSupport = new EntryListenerSupport<>();
            this.locks = new ConcurrentHashMap<>();
            this.keyExpirations = new ConcurrentHashMap<>();
        }

        @Override
        public int size() {
            removeExpiredEntries();
            return map.size();
        }

        @Override
        public boolean isEmpty() {
            removeExpiredEntries();
            return map.isEmpty();
        }

        @Override
        public String getPartitionKey() {
            return null;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getServiceName() {
            return null;
        }

        @Override
        public void destroy() {
            maps.remove(getName());
            listenerSupport.removeAllListeners();
            keyExpirations.clear();
            map.clear();
            locks.clear();
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> m) {
            removeExpiredEntries();
            // No need to remove from keyExpirations if updating an expiring key
            Set<? extends K> keys = m.keySet();
            for (K key : keys) {
                put(key, m.get(key));
            }
        }

        @Override
        public boolean containsKey(Object key) {
            return !evictEntryIfExpired(key) && map.containsKey(key);
            //return map.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value) {
            removeExpiredEntries();
            return map.containsValue(value);
        }

        @Override
        public V get(Object key) {
            if (evictEntryIfExpired(key)) {
                return null;
            } else {
                return map.get(key);
            }
            //return map.get(key);
        }

        @Override
        public V put(K key, V value) {
            V oldValue = map.put(key, value);
            if (oldValue != null) {
                EntryEvent<K, V> event = new EntryEvent<>(name, null, EntryEventType.UPDATED.getType(), key, value);
                listenerSupport.entryUpdated(event);
            }
            else {
                EntryEvent<K, V> event = new EntryEvent<>(name, null, EntryEventType.ADDED.getType(), key, value);
                listenerSupport.entryAdded(event);
            }
            return oldValue;
        }

        @SuppressWarnings("unchecked")
        @Override
        public V remove(Object key) {
            removeExpiredEntries();
            V value = map.remove(key);
            boolean removed = (value !=  null);
            if (removed) {
                this.keyExpirations.remove(key);
                EntryEvent<K, V> event = new EntryEvent<>(name, null, EntryEventType.REMOVED.getType(), (K) key, value);
                listenerSupport.entryRemoved(event);
            }
            return value;
        }

        private void removeExpiredEntries() {
            long currentMillis = currentTimeMillis();
            this.keyExpirations.entrySet().removeIf(e -> {
                final Long expiration = e.getValue();
                if (currentMillis >= expiration.longValue()) {
                    map.remove(e.getKey());
                    return true;
                }
                return false;
            });
        }

        private boolean evictEntryIfExpired(Object key) {
            long currentMillis = currentTimeMillis();
            final Long expiration = keyExpirations.get(key);
            if (expiration != null) {
                if (currentMillis >= expiration.longValue()) {
                    V value = map.remove(key);
                    keyExpirations.remove(key);
                    listenerSupport.entryEvicted(new EntryEvent<>(name, null, EntryEventType.EVICTED.getType(), (K) key, (V) value));
                    return true;
                }
            }
            return false;
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean remove(Object key, Object value) {
            boolean wasRemoved = map.remove(key, value);
            if (wasRemoved) {
                EntryEvent<K, V> event = new EntryEvent<>(name, null, EntryEventType.REMOVED.getType(), (K) key, (V) value);
                listenerSupport.entryRemoved(event);
            }
            return wasRemoved;
        }

        @Override
        public void delete(Object key) {
            throw new UnsupportedOperationException(String.format(OPERATION_NOT_SUPPORTED_MESSAGE, "delete"));
        }

        @Override
        public void flush() {
            throw new UnsupportedOperationException(String.format(OPERATION_NOT_SUPPORTED_MESSAGE, "flush"));
        }

        @Override
        public Map<K, V> getAll(Set<K> keys) {
            throw new UnsupportedOperationException(String.format(OPERATION_NOT_SUPPORTED_MESSAGE, "getAll"));
/*            removeExpiredEntries();
            if (keys == null || keys.isEmpty()) {
                return emptyMap();
            }

            if (keys.contains(null)) {
                throw new NullPointerException("Null key is not allowed");
            }

            return keys.stream()
                .filter(map::containsKey)
                .collect(Collectors.toMap(Function.identity(), map::get));*/
        }

        @Override
        public void loadAll(boolean replaceExistingValues) {
            throw new UnsupportedOperationException(String.format(OPERATION_NOT_SUPPORTED_MESSAGE, "loadAll"));
        }

        @Override
        public void loadAll(Set<K> keys, boolean replaceExistingValues) {
            throw new UnsupportedOperationException(String.format(OPERATION_NOT_SUPPORTED_MESSAGE, "loadAll"));
        }

        @Override
        public void clear() {
            if (map.isEmpty()) {
                return;
            }

            MapEvent event = new MapEvent(name, null, EntryEventType.CLEAR_ALL.getType(), map.size());
            map.clear();
            listenerSupport.mapCleared(event);
        }

        @Override
        public ICompletableFuture<V> getAsync(K key) {
            throw new UnsupportedOperationException(String.format(OPERATION_NOT_SUPPORTED_MESSAGE, "getAsync"));
        }

        @Override
        public ICompletableFuture<V> putAsync(K key, V value) {
            throw new UnsupportedOperationException(String.format(OPERATION_NOT_SUPPORTED_MESSAGE, "putAsync"));
        }

        @Override
        public ICompletableFuture<V> putAsync(K key, V value, long ttl, TimeUnit timeunit) {
            throw new UnsupportedOperationException(String.format(OPERATION_NOT_SUPPORTED_MESSAGE, "putAsync"));
        }

        @Override
        public ICompletableFuture<Void> setAsync(K key, V value) {
            throw new UnsupportedOperationException(String.format(OPERATION_NOT_SUPPORTED_MESSAGE, "setAsync"));
        }

        @Override
        public ICompletableFuture<Void> setAsync(K key, V value, long ttl, TimeUnit timeunit) {
            throw new UnsupportedOperationException(String.format(OPERATION_NOT_SUPPORTED_MESSAGE, "setAsync"));
        }

        @Override
        public ICompletableFuture<V> removeAsync(K key) {
            throw new UnsupportedOperationException(String.format(OPERATION_NOT_SUPPORTED_MESSAGE, "removeAsync"));
        }

        @Override
        public boolean tryRemove(K key, long timeout, TimeUnit timeunit) {
            throw new UnsupportedOperationException(String.format(OPERATION_NOT_SUPPORTED_MESSAGE, "tryRemove"));
        }

        @Override
        public boolean tryPut(K key, V value, long timeout, TimeUnit timeunit) {
            throw new UnsupportedOperationException(String.format(OPERATION_NOT_SUPPORTED_MESSAGE, "tryPut"));
        }

        @Override
        public V put(K key, V value, long ttl, TimeUnit timeunit) {
            throw new UnsupportedOperationException(String.format(OPERATION_NOT_SUPPORTED_MESSAGE, "put"));
        }

        @Override
        public void putTransient(K key, V value, long ttl, TimeUnit timeunit) {
            throw new UnsupportedOperationException(String.format(OPERATION_NOT_SUPPORTED_MESSAGE, "putTransient"));
        }

        @Override
        public V putIfAbsent(K key, V value) {
            removeExpiredEntries();
            V oldValue = map.putIfAbsent(key, value);
            if (oldValue == null) {
                listenerSupport.entryAdded(new EntryEvent<>(name, null, EntryEventType.ADDED.getType(), (K) key, (V) value));
            }
            return oldValue;
        }

        @Override
        public V putIfAbsent(K key, V value, long ttl, TimeUnit timeunit) {
            removeExpiredEntries();
            V oldValue = map.putIfAbsent(key, value);
            if (oldValue == null) {
                keyExpirations.put(key, currentTimeMillis() + timeunit.toMillis(ttl));
                listenerSupport.entryAdded(new EntryEvent<>(name, null, EntryEventType.ADDED.getType(), (K) key, (V) value));
            }
            return oldValue;
        }

        @Override
        public boolean replace(K key, V oldValue, V newValue) {
            return map.replace(key, oldValue, newValue);
        }

        @Override
        public V replace(K key, V value) {
            return map.replace(key, value);
        }

        @Override
        public void set(K key, V value) {
            throw new UnsupportedOperationException(String.format(OPERATION_NOT_SUPPORTED_MESSAGE, "set"));
        }

        @Override
        public void set(K key, V value, long ttl, TimeUnit timeunit) {
            throw new UnsupportedOperationException(String.format(OPERATION_NOT_SUPPORTED_MESSAGE, "set"));
        }

        @Override
        public void lock(K key) {
            throw new UnsupportedOperationException(String.format(OPERATION_NOT_SUPPORTED_MESSAGE, "lock"));
        }

        @Override
        public void lock(K key, long leaseTime, TimeUnit timeUnit) {
            throw new UnsupportedOperationException(String.format(OPERATION_NOT_SUPPORTED_MESSAGE, "lock"));
        }

        @Override
        public boolean isLocked(K key) {
            throw new UnsupportedOperationException(String.format(OPERATION_NOT_SUPPORTED_MESSAGE, "isLocked"));
        }

        @Override
        public boolean tryLock(K key) {
            throw new UnsupportedOperationException(String.format(OPERATION_NOT_SUPPORTED_MESSAGE, "tryLock"));
        }

        @Override
        public boolean tryLock(K key, long time, TimeUnit timeunit) throws InterruptedException {
            throw new UnsupportedOperationException(String.format(OPERATION_NOT_SUPPORTED_MESSAGE, "tryLock"));
        }

        @Override
        public boolean tryLock(K key, long time, TimeUnit timeunit, long leaseTime, TimeUnit leaseTimeunit)
                throws InterruptedException {
            throw new UnsupportedOperationException(String.format(OPERATION_NOT_SUPPORTED_MESSAGE, "tryLock"));
        }

        @Override
        public void unlock(K key) {
            throw new UnsupportedOperationException(String.format(OPERATION_NOT_SUPPORTED_MESSAGE, "unlock"));
        }

        @Override
        public void forceUnlock(K key) {
            throw new UnsupportedOperationException(String.format(OPERATION_NOT_SUPPORTED_MESSAGE, "forceUnlock"));
        }

        @Override
        public String addLocalEntryListener(MapListener listener) {
            throw new UnsupportedOperationException(String.format(OPERATION_NOT_SUPPORTED_MESSAGE, "addLocalEntryListener"));
        }

        @SuppressWarnings("rawtypes")
        @Override
        public String addLocalEntryListener(EntryListener listener) {
            throw new UnsupportedOperationException(String.format(OPERATION_NOT_SUPPORTED_MESSAGE, "addLocalEntryListener"));
        }

        @Override
        public String addLocalEntryListener(MapListener listener, Predicate<K, V> predicate, boolean includeValue) {
            throw new UnsupportedOperationException(String.format(OPERATION_NOT_SUPPORTED_MESSAGE, "addLocalEntryListener"));
        }

        @SuppressWarnings("rawtypes")
        @Override
        public String addLocalEntryListener(EntryListener listener, Predicate<K, V> predicate, boolean includeValue) {
            throw new UnsupportedOperationException(String.format(OPERATION_NOT_SUPPORTED_MESSAGE, "addLocalEntryListener"));
        }

        @Override
        public String addLocalEntryListener(MapListener listener, Predicate<K, V> predicate, K key, boolean includeValue) {
            throw new UnsupportedOperationException(String.format(OPERATION_NOT_SUPPORTED_MESSAGE, "addLocalEntryListener"));
        }

        @SuppressWarnings("rawtypes")
        @Override
        public String addLocalEntryListener(EntryListener listener, Predicate<K, V> predicate, K key, boolean includeValue) {
            throw new UnsupportedOperationException(String.format(OPERATION_NOT_SUPPORTED_MESSAGE, "addLocalEntryListener"));
        }

        @Override
        public String addInterceptor(MapInterceptor interceptor) {
            throw new UnsupportedOperationException(String.format(OPERATION_NOT_SUPPORTED_MESSAGE, "addInterceptor"));
        }

        @Override
        public void removeInterceptor(String id) {
            throw new UnsupportedOperationException(String.format(OPERATION_NOT_SUPPORTED_MESSAGE, "removeInterceptor"));
        }

        @Override
        public String addEntryListener(MapListener listener, boolean includeValue) {
            throw new UnsupportedOperationException(String.format(OPERATION_NOT_SUPPORTED_MESSAGE, "addEntryListener"));
        }

        @SuppressWarnings("rawtypes")
        @Override
        public String addEntryListener(EntryListener listener, boolean includeValue) {
            return listenerSupport.addEntryListener(listener, includeValue);
        }

        @Override
        public boolean removeEntryListener(String id) {
            return listenerSupport.removeEntryListener(id);
        }

        @Override
        public String addPartitionLostListener(MapPartitionLostListener listener) {
            throw new UnsupportedOperationException(String.format(OPERATION_NOT_SUPPORTED_MESSAGE, "addPartitionLostListener"));
        }

        @Override
        public boolean removePartitionLostListener(String id) {
            throw new UnsupportedOperationException(String.format(OPERATION_NOT_SUPPORTED_MESSAGE, "removePartitionLostListener"));
        }

        @Override
        public String addEntryListener(MapListener listener, K key, boolean includeValue) {
            throw new UnsupportedOperationException(String.format(OPERATION_NOT_SUPPORTED_MESSAGE, "addEntryListener"));
        }

        @SuppressWarnings("rawtypes")
        @Override
        public String addEntryListener(EntryListener listener, K key, boolean includeValue) {
            return listenerSupport.addEntryListener(listener, key, includeValue);
        }

        @Override
        public String addEntryListener(MapListener listener, Predicate<K, V> predicate, boolean includeValue) {
            throw new UnsupportedOperationException(String.format(OPERATION_NOT_SUPPORTED_MESSAGE, "addEntryListener"));
        }

        @SuppressWarnings("rawtypes")
        @Override
        public String addEntryListener(EntryListener listener, Predicate<K, V> predicate, boolean includeValue) {
            throw new UnsupportedOperationException(String.format(OPERATION_NOT_SUPPORTED_MESSAGE, "addEntryListener"));
        }

        @Override
        public String addEntryListener(MapListener listener, Predicate<K, V> predicate, K key, boolean includeValue) {
            throw new UnsupportedOperationException(String.format(OPERATION_NOT_SUPPORTED_MESSAGE, "addEntryListener"));
        }

        @SuppressWarnings("rawtypes")
        @Override
        public String addEntryListener(EntryListener listener, Predicate<K, V> predicate, K key, boolean includeValue) {
            throw new UnsupportedOperationException(String.format(OPERATION_NOT_SUPPORTED_MESSAGE, "addEntryListener"));
        }

        @Override
        public EntryView<K, V> getEntryView(K key) {
            throw new UnsupportedOperationException(String.format(OPERATION_NOT_SUPPORTED_MESSAGE, "getEntryView"));
        }

        @Override
        public boolean evict(K key) {
            //throw new UnsupportedOperationException(String.format(OPERATION_NOT_SUPPORTED_MESSAGE, "evict"));
            V value = map.remove(key);
            if (value != null) {
                    keyExpirations.remove(key);
                    listenerSupport.entryEvicted(new EntryEvent<>(name, null, EntryEventType.EVICTED.getType(), (K) key, (V) value));
                    return true;
            }
            return false;
        }

        @Override
        public void evictAll() {
            throw new UnsupportedOperationException(String.format(OPERATION_NOT_SUPPORTED_MESSAGE, "evictAll"));
        }

        @Override
        public Set<K> keySet() {
            removeExpiredEntries();
            return map.keySet();
        }

        @Override
        public Collection<V> values() {
            removeExpiredEntries();
            return map.values();
        }

        @Override
        public Set<java.util.Map.Entry<K, V>> entrySet() {
            removeExpiredEntries();
            return map.entrySet();
        }

        @SuppressWarnings("rawtypes")
        @Override
        public Set<K> keySet(Predicate predicate) {
            return entrySet().stream().filter(predicate::apply).map(e -> e.getKey()).collect(Collectors.toSet());
        }

        @SuppressWarnings("rawtypes")
        @Override
        public Set<java.util.Map.Entry<K, V>> entrySet(Predicate predicate) {
            return entrySet().stream().filter(predicate::apply).collect(Collectors.toSet());
        }

        @SuppressWarnings("rawtypes")
        @Override
        public Collection<V> values(Predicate predicate) {
            return entrySet().stream().filter(predicate::apply).map(e -> e.getValue()).collect(Collectors.toSet());
        }

        @Override
        public Set<K> localKeySet() {
            return keySet();
        }

        @SuppressWarnings("rawtypes")
        @Override
        public Set<K> localKeySet(Predicate predicate) {
            return keySet(predicate);
        }

        @Override
        public void addIndex(String attribute, boolean ordered) {
            //throw new UnsupportedOperationException(String.format(OPERATION_NOT_SUPPORTED_MESSAGE, "addIndex"));
        }

        @Override
        public LocalMapStats getLocalMapStats() {
            throw new UnsupportedOperationException(String.format(OPERATION_NOT_SUPPORTED_MESSAGE, "getLocalMapStats"));
        }

        @SuppressWarnings("rawtypes")
        @Override
        public Object executeOnKey(K key, EntryProcessor entryProcessor) {
            throw new UnsupportedOperationException(String.format(OPERATION_NOT_SUPPORTED_MESSAGE, "executeOnKey"));
        }

        @SuppressWarnings("rawtypes")
        @Override
        public Map<K, Object> executeOnKeys(Set<K> keys, EntryProcessor entryProcessor) {
            throw new UnsupportedOperationException(String.format(OPERATION_NOT_SUPPORTED_MESSAGE, "executeOnKeys"));
        }

        @SuppressWarnings("rawtypes")
        @Override
        public void submitToKey(K key, EntryProcessor entryProcessor, ExecutionCallback callback) {
            throw new UnsupportedOperationException(String.format(OPERATION_NOT_SUPPORTED_MESSAGE, "submitToKey"));
        }

        @SuppressWarnings("rawtypes")
        @Override
        public ICompletableFuture submitToKey(K key, EntryProcessor entryProcessor) {
            throw new UnsupportedOperationException(String.format(OPERATION_NOT_SUPPORTED_MESSAGE, "submitToKey"));
        }

        @SuppressWarnings("rawtypes")
        @Override
        public Map<K, Object> executeOnEntries(EntryProcessor entryProcessor) {
            throw new UnsupportedOperationException(String.format(OPERATION_NOT_SUPPORTED_MESSAGE, "executeOnEntries"));
        }

        @SuppressWarnings("rawtypes")
        @Override
        public Map<K, Object> executeOnEntries(EntryProcessor entryProcessor, Predicate predicate) {
            throw new UnsupportedOperationException(String.format(OPERATION_NOT_SUPPORTED_MESSAGE, "executeOnEntries"));
        }

        @Override
        public <SuppliedValue, Result> Result aggregate(Supplier<K, V, SuppliedValue> supplier,
            Aggregation<K, SuppliedValue, Result> aggregation) {
            throw new UnsupportedOperationException(String.format(OPERATION_NOT_SUPPORTED_MESSAGE, "aggregate"));
        }

        @Override
        public <SuppliedValue, Result> Result aggregate(Supplier<K, V, SuppliedValue> supplier,
            Aggregation<K, SuppliedValue, Result> aggregation, JobTracker jobTracker) {
            throw new UnsupportedOperationException(String.format(OPERATION_NOT_SUPPORTED_MESSAGE, "aggregate"));
        }

    }

    private class ILockImpl implements ILock {

        private final String name;
        private final Lock lock;

        public ILockImpl(String name) {
            this.name = name;
            this.lock = new ReentrantLock();
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            lock.lockInterruptibly();
        }

        @Override
        public String getPartitionKey() {
            return null;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getServiceName() {
            return null;
        }

        @Override
        public void destroy() {
            locks.remove(name);
        }

        @Override
        @Deprecated
        public Object getKey() {
            return getName();
        }

        @Override
        public void lock() {
            lock.lock();
        }

        @Override
        public boolean tryLock() {
            return lock.tryLock();
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            return lock.tryLock(time, unit);
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit, long leaseTime, TimeUnit leaseUnit) throws InterruptedException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void unlock() {
            lock.unlock();
        }

        @Override
        public void lock(long leaseTime, TimeUnit timeUnit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void forceUnlock() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Condition newCondition() {
            return lock.newCondition();
        }

        @Override
        public ICondition newCondition(String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isLocked() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isLockedByCurrentThread() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getLockCount() {
            throw new UnsupportedOperationException();
        }

        @Override
        public long getRemainingLeaseTime() {
            throw new UnsupportedOperationException();
        }

    }

    private abstract class ICollectionImpl<E> implements ICollection<E> {

        private final String name;
        protected final ItemListenerSupport<E> listenerSupport;

        public ICollectionImpl(String name) {
            this.name = name;
            this.listenerSupport = new ItemListenerSupport<>();
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String addItemListener(ItemListener<E> listener, boolean includeValue) {
            return listenerSupport.addItemListener(listener, includeValue);
        }

        @Override
        public boolean removeItemListener(String registrationId) {
            return listenerSupport.removeItemListener(registrationId);
        }

    }

    private class IListImpl<E> extends ICollectionImpl<E> implements IList<E> {

        private List<E> list;
        // TODO: use read/write locks to synchronize?

        public IListImpl(String name) {
            super(name);
            list = new LinkedList<>();
        }

        @Override
        public boolean add(E element) {
            if (list.add(element)) {
                listenerSupport.fireItemAdded(getName(), element);
                return true;
            }
            return false;
        }

        @Override
        public void add(int index, E element) {
            list.add(index, element);
            listenerSupport.fireItemAdded(getName(), element);
        }

        @Override
        public boolean addAll(Collection<? extends E> c) {
            boolean result = false;
            for (E element : c) {
                result |= add(element);
            }
            return result;
        }

        @Override
        public boolean addAll(int index, Collection<? extends E> c) {
            for (E element : c) {
                add(index++, element);
            }
            return true;
        }

        @Override
        public void clear() {
            Iterator<E> iterator = list.iterator();
            while (iterator.hasNext()) {
                E element = iterator.next();
                iterator.remove();
                listenerSupport.fireItemRemoved(getName(), element);
            }
        }

        @Override
        public boolean contains(Object o) {
            return list.contains(o);
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return list.containsAll(c);
        }

        @Override
        public E get(int index) {
            return list.get(index);
        }

        @Override
        public int indexOf(Object o) {
            return list.indexOf(o);
        }

        @Override
        public boolean isEmpty() {
            return list.isEmpty();
        }

        @Override
        public Iterator<E> iterator() {
            return list.iterator();
        }

        @Override
        public int lastIndexOf(Object o) {
            return list.lastIndexOf(o);
        }

        @Override
        public ListIterator<E> listIterator() {
            return list.listIterator();
        }

        @Override
        public ListIterator<E> listIterator(int index) {
            return list.listIterator(index);
        }

        @Override
        public E remove(int index) {
            E element = list.remove(index);
            listenerSupport.fireItemRemoved(getName(), element);
            return element;
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean remove(Object o) {
            if (list.remove(o)) {
                listenerSupport.fireItemRemoved(getName(), (E) o);
                return true;
            }

            return false;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            boolean modified = false;
            Iterator<E> iterator = list.iterator();
            while (iterator.hasNext()) {
                E element = iterator.next();
                if (c.contains(element)) {
                    iterator.remove();
                    listenerSupport.fireItemRemoved(getName(), element);
                    modified = true;
                }
            }

            return modified;
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            boolean modified = false;
            Iterator<E> iterator = list.iterator();
            while (iterator.hasNext()) {
                E element = iterator.next();
                if (!c.contains(element)) {
                    iterator.remove();
                    listenerSupport.fireItemRemoved(getName(), element);
                    modified = true;
                }
            }

            return modified;
        }

        @Override
        public E set(int index, E element) {
            E oldValue = list.set(index, element);
            listenerSupport.fireItemRemoved(getName(), oldValue);
            listenerSupport.fireItemAdded(getName(), element);

            return oldValue;
        }

        @Override
        public int size() {
            return list.size();
        }

        @Override
        public List<E> subList(int fromIndex, int toIndex) {
            return list.subList(fromIndex, toIndex);
        }

        @Override
        public Object[] toArray() {
            return list.toArray();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return list.toArray(a);
        }

        @Override
        public String getPartitionKey() {
            return null;
        }

        @Override
        public String getServiceName() {
            return null;
        }

        @Override
        public void destroy() {
            lists.remove(getName(), this);
            list = new LinkedList<>();
            listenerSupport.destroy();
        }

    }

    private class IQueueImpl<E> implements IQueue<E> {

        private ArrayBlockingQueue<E> queue;
        //private ReentrantLock lock;
        private static final long serialVersionUID = 1L;
        private final String name;
        private final int capacity;
        private final ItemListenerSupport<E> itemListenerSupport;

        public IQueueImpl(String name, int capacity) {
            this.name = name;
            this.capacity = capacity;
            this.queue = new ArrayBlockingQueue<>(capacity);
            //this.lock = new ReentrantLock();
            this.itemListenerSupport = new ItemListenerSupport<>();
        }

        @Override
        public String addItemListener(ItemListener<E> listener, boolean includeValue) {
            return this.itemListenerSupport.addItemListener(listener, includeValue);
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public boolean removeItemListener(String registrationId) {
            return this.itemListenerSupport.removeItemListener(registrationId);
        }

        @Override
        public void destroy() {
            /*final ReentrantLock lock = this.lock;
            try {
                lock.lock();*/
                itemListenerSupport.destroy();
                queue.clear();
                /*this.lock = new ReentrantLock();
            } finally {
                lock.unlock();
            }*/
        }

        @Override
        public boolean add(E e) {
            if (queue.add(e)) {
                itemListenerSupport.fireItemAdded(name, e);
                return true;
            }
            return false;
        }

        @Override
        public void clear() {
            /*final ReentrantLock lock = this.lock;
            try {
                lock.lock();*/
                Iterator<E> iterator = queue.iterator();
                while (iterator.hasNext()) {
                    E element = iterator.next();
                    iterator.remove();
                    itemListenerSupport.fireItemRemoved(name, element);
                }
            /*} finally {
                lock.unlock();
            }*/
        }

        @Override
        public int drainTo(Collection<? super E> c, int maxElements) {
            /*ReentrantLock lock = this.lock;
            try {
                lock.lock();*/
                Iterator<E> iterator = queue.iterator();
                int i = 0;
                while (iterator.hasNext() && i < maxElements) {
                    E element = iterator.next();
                    iterator.remove();
                    c.add(element);
                    itemListenerSupport.fireItemRemoved(name, element);
                    i++;
                }
                return i;
            /*} finally {
                lock.unlock();
            }*/
        }

        @Override
        public int drainTo(Collection<? super E> c) {
            return drainTo(c, Integer.MAX_VALUE);
        }

        @Override
        public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
            if (queue.offer(e, timeout, unit)) {
                itemListenerSupport.fireItemAdded(name, e);
                return true;
            }
            return false;
        }

        @Override
        public boolean offer(E e) {
            if (queue.offer(e)) {
                itemListenerSupport.fireItemAdded(name, e);
                return true;
            }
            return false;
        }

        @Override
        public E poll() {
            E item = queue.poll();
            if (item != null) {
                itemListenerSupport.fireItemRemoved(name, item);
            }
            return item;
        }

        @Override
        public E poll(long timeout, TimeUnit unit) throws InterruptedException {
            E item = queue.poll(timeout, unit);
            if (item != null) {
                itemListenerSupport.fireItemRemoved(name, item);
            }
            return item;
        }

        @Override
        public void put(E e) throws InterruptedException {
            queue.put(e);
            itemListenerSupport.fireItemAdded(name, e);
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean remove(Object o) {
            if (queue.remove(o)) {
                itemListenerSupport.fireItemRemoved(name, (E) o);
                return true;
            }
            return false;
        }

        @Override
        public E take() throws InterruptedException {
            E item = queue.take();
            itemListenerSupport.fireItemRemoved(name, item);
            return item;
        }

        @Override
        public boolean addAll(Collection<? extends E> c) {
            boolean modified = false;
            for (E element : c) {
                modified |= add(element);
            }
            return modified;
        }

        @Override
        public E remove() {
            E item = queue.remove();
            itemListenerSupport.fireItemRemoved(name, item);
            return item;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            boolean modified = false;
            Iterator<E> iterator = queue.iterator();
            while (iterator.hasNext()) {
                E element = iterator.next();
                if (c.contains(element)) {
                    iterator.remove();
                    itemListenerSupport.fireItemRemoved(name, element);
                    modified = true;
                }
            }
            return modified;
        }

        @Override
        public boolean contains(Object o) {
            return queue.contains(o);
        }

        @Override
        public int remainingCapacity() {
            return queue.remainingCapacity();
        }

        @Override
        public E element() {
            return queue.element();
        }

        @Override
        public E peek() {
            return queue.peek();
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return queue.containsAll(c);
        }

        @Override
        public boolean isEmpty() {
            return queue.isEmpty();
        }

        @Override
        public Iterator<E> iterator() {
            return queue.iterator();
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            boolean modified = false;
            Iterator<E> iterator = queue.iterator();
            while (iterator.hasNext()) {
                E element = iterator.next();
                if (!c.contains(element)) {
                    iterator.remove();
                    itemListenerSupport.fireItemRemoved(name, element);
                    modified = true;
                }
            }
            return modified;
        }

        @Override
        public int size() {
            return queue.size();
        }

        @Override
        public Object[] toArray() {
            return queue.toArray();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return queue.toArray(a);
        }

        @Override
        public LocalQueueStats getLocalQueueStats() {
            throw new UnsupportedOperationException("getLocalQueueStats");
        }

        @Override
        public String getPartitionKey() {
            return null;
        }

        @Override
        public String getServiceName() {
            return null;
        }

    }

    private class ItemListenerSupport<E> {

        private ConcurrentHashMap<String, ItemListenerEntry<E>> listenersMap = new ConcurrentHashMap<>();

        public String addItemListener(ItemListener<E> listener, boolean includeValue) {
            String registrationId = UUID.randomUUID().toString();
            listenersMap.put(registrationId, new ItemListenerEntry<>(listener, includeValue));
            return registrationId;
        }

        public boolean removeItemListener(String registrationId) {
            return listenersMap.remove(registrationId) != null;
        }

        public void fireItemAdded(String listName, E element) {
            ItemEvent<E> addedEventWithValue =
                    new ItemEvent<E>(listName, ItemEventType.ADDED, element, null);
            ItemEvent<E> addedEventWithoutValue =
                    new ItemEvent<E>(listName, ItemEventType.ADDED, null, null);
            for (ItemListenerEntry<E> listenerEntry : listenersMap.values()) {
                listenerEntry.itemListener.itemAdded(listenerEntry.includeValue ? addedEventWithValue : addedEventWithoutValue);
            }
        }

        public void fireItemRemoved(String listName, E element) {
            ItemEvent<E> removedEventWithValue = new ItemEvent<E>(listName, ItemEventType.REMOVED, element, null);
            ItemEvent<E> removedEventWithoutValue = new ItemEvent<E>(listName, ItemEventType.REMOVED, null, null);
            for (ItemListenerEntry<E> listenerEntry : listenersMap.values()) {
                listenerEntry.itemListener
                        .itemRemoved(listenerEntry.includeValue ? removedEventWithValue : removedEventWithoutValue);
            }
        }

        public void destroy() {
            listenersMap.clear();
        }
    }

    private static class ItemListenerEntry<E> {

        private ItemListener<E> itemListener;
        private boolean includeValue;

        public ItemListenerEntry(ItemListener<E> listener, boolean includeValue) {
            this.itemListener = listener;
            this.includeValue = includeValue;
        }

    }
}
