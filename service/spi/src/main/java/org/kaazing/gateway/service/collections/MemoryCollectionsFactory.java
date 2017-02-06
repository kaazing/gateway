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
package org.kaazing.gateway.service.collections;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.kaazing.gateway.util.AtomicCounter;

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
import com.hazelcast.core.ItemListener;
import com.hazelcast.map.EntryProcessor;
import com.hazelcast.map.MapInterceptor;
import com.hazelcast.map.listener.MapListener;
import com.hazelcast.map.listener.MapPartitionLostListener;
import com.hazelcast.mapreduce.JobTracker;
import com.hazelcast.mapreduce.aggregation.Aggregation;
import com.hazelcast.mapreduce.aggregation.Supplier;
import com.hazelcast.monitor.LocalMapStats;
import com.hazelcast.query.Predicate;

public class MemoryCollectionsFactory implements CollectionsFactory {

    private static final String OPERATION_NOT_SUPPORTED_MESSAGE = "Operation %s not supported";

    private final ConcurrentMap<String, IMapImpl<?, ?>> maps;
    private final ConcurrentMap<String, IListImpl<?>> lists;
    private final Map<String, ILockImpl> locks;
    private final ConcurrentMap<String, AtomicCounter> atomicCounters;
    private final ConcurrentMap<String, ITopic<?>> topics;

    public MemoryCollectionsFactory() {
        // TODO: avoid memory leak
        maps = new ConcurrentHashMap<>();
        lists = new ConcurrentHashMap<>();
        locks = Collections.synchronizedMap(new WeakHashMap<>());
        atomicCounters = new ConcurrentHashMap<>();
        topics = new ConcurrentHashMap<>();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E> IList<E> getList(String name) {
        IListImpl<E> list = (IListImpl<E>) lists.get(name);
        if (list == null) {
            IListImpl<E> newList = new IListImpl<>(name);
            list = (IListImpl<E>) lists.putIfAbsent(name, newList);
            if (list == null) {
                list = newList;
            }
        }
        return list;
    }

    @Override
    public <E> IQueue<E> getQueue(String name) {
        throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "getQueue"));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E> ITopic<E> getTopic(String name) {
        return (ITopic<E>) topics.computeIfAbsent(name, s -> new MemoryTopic<E>(s));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <K, V> IMap<K, V> getMap(String name) {
        IMapImpl<K, V> map = (IMapImpl<K, V>) maps.get(name);
        if (map == null) {
            IMapImpl<K, V> newMap = new IMapImpl<>(name);
            map = (IMapImpl<K, V>) maps.putIfAbsent(name, newMap);
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

        private final ConcurrentHashMap<K, V> map;
        private final ConcurrentHashMap<K, Long> keyExpirations;
        private final String name;

        public IMapImpl(String name) {
            this.name = name;
            this.map = new ConcurrentHashMap<>();
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
            keyExpirations.clear();
            map.clear();
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> m) {
            removeExpiredEntries();
            map.putAll(m);
        }

        @Override
        public boolean containsKey(Object key) {
            return !evictEntryIfExpired(key) && map.containsKey(key);
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
        }

        @Override
        public V put(K key, V value) {
            removeExpiredEntries();
            return map.put(key, value);
        }

        @Override
        public V remove(Object key) {
            removeExpiredEntries();
            keyExpirations.remove(key);
            return map.remove(key);
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
                    map.remove(key);
                    keyExpirations.remove(key);
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean remove(Object key, Object value) {
            keyExpirations.remove(key, value);
            return map.remove(key, value);
        }

        @Override
        public void delete(Object key) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "delete"));
        }

        @Override
        public void flush() {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "flush"));
        }

        @Override
        public Map<K, V> getAll(Set<K> keys) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "getAll"));
        }

        @Override
        public void loadAll(boolean replaceExistingValues) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "loadAll"));
        }

        @Override
        public void loadAll(Set<K> keys, boolean replaceExistingValues) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "loadAll"));
        }

        @Override
        public void clear() {
            map.clear();
            keyExpirations.clear();
        }

        @Override
        public ICompletableFuture<V> getAsync(K key) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "getAsync"));
        }

        @Override
        public ICompletableFuture<V> putAsync(K key, V value) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "putAsync"));
        }

        @Override
        public ICompletableFuture<V> putAsync(K key, V value, long ttl, TimeUnit timeunit) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "putAsync"));
        }

        @Override
        public ICompletableFuture<Void> setAsync(K key, V value) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "setAsync"));
        }

        @Override
        public ICompletableFuture<Void> setAsync(K key, V value, long ttl, TimeUnit timeunit) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "setAsync"));
        }

        @Override
        public ICompletableFuture<V> removeAsync(K key) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "removeAsync"));
        }

        @Override
        public boolean tryRemove(K key, long timeout, TimeUnit timeunit) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "tryRemove"));
        }

        @Override
        public boolean tryPut(K key, V value, long timeout, TimeUnit timeunit) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "tryPut"));
        }

        @Override
        public V put(K key, V value, long ttl, TimeUnit timeunit) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "put"));
        }

        @Override
        public void putTransient(K key, V value, long ttl, TimeUnit timeunit) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "putTransient"));
        }

        @Override
        public V putIfAbsent(K key, V value) {
            removeExpiredEntries();
            return map.putIfAbsent(key, value);
        }

        @Override
        public V putIfAbsent(K key, V value, long ttl, TimeUnit timeunit) {
            removeExpiredEntries();
            V oldValue = map.putIfAbsent(key, value);
            if (oldValue == null) {
                keyExpirations.put(key, currentTimeMillis() + timeunit.toMillis(ttl));
            }
            return oldValue;
        }

        @Override
        public boolean replace(K key, V oldValue, V newValue) {
            removeExpiredEntries();
            return map.replace(key, oldValue, newValue);
        }

        @Override
        public V replace(K key, V value) {
            removeExpiredEntries();
            return map.replace(key, value);
        }

        @Override
        public void set(K key, V value) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "set"));
        }

        @Override
        public void set(K key, V value, long ttl, TimeUnit timeunit) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "set"));
        }

        @Override
        public void lock(K key) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "lock"));
        }

        @Override
        public void lock(K key, long leaseTime, TimeUnit timeUnit) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "lock"));
        }

        @Override
        public boolean isLocked(K key) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "isLocked"));
        }

        @Override
        public boolean tryLock(K key) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "tryLock"));
        }

        @Override
        public boolean tryLock(K key, long time, TimeUnit timeunit) throws InterruptedException {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "tryLock"));
        }

        @Override
        public boolean tryLock(K key, long time, TimeUnit timeunit, long leaseTime, TimeUnit leaseTimeunit)
                throws InterruptedException {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "tryLock"));
        }

        @Override
        public void unlock(K key) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "unlock"));
        }

        @Override
        public void forceUnlock(K key) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "forceUnlock"));
        }

        @Override
        public String addLocalEntryListener(MapListener listener) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "addLocalEntryListener"));
        }

        @SuppressWarnings("rawtypes")
        @Override
        public String addLocalEntryListener(EntryListener listener) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "addLocalEntryListener"));
        }

        @Override
        public String addLocalEntryListener(MapListener listener, Predicate<K, V> predicate, boolean includeValue) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "addLocalEntryListener"));
        }

        @SuppressWarnings("rawtypes")
        @Override
        public String addLocalEntryListener(EntryListener listener, Predicate<K, V> predicate, boolean includeValue) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "addLocalEntryListener"));
        }

        @Override
        public String addLocalEntryListener(MapListener listener, Predicate<K, V> predicate, K key, boolean includeValue) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "addLocalEntryListener"));
        }

        @SuppressWarnings("rawtypes")
        @Override
        public String addLocalEntryListener(EntryListener listener, Predicate<K, V> predicate, K key, boolean includeValue) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "addLocalEntryListener"));
        }

        @Override
        public String addInterceptor(MapInterceptor interceptor) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "addInterceptor"));
        }

        @Override
        public void removeInterceptor(String id) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "removeInterceptor"));
        }

        @Override
        public String addEntryListener(MapListener listener, boolean includeValue) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "addEntryListener"));
        }

        @SuppressWarnings("rawtypes")
        @Override
        public String addEntryListener(EntryListener listener, boolean includeValue) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "addEntryListener"));
        }

        @Override
        public boolean removeEntryListener(String id) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "removeEntryListener"));
        }

        @Override
        public String addPartitionLostListener(MapPartitionLostListener listener) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "addPartitionLostListener"));
        }

        @Override
        public boolean removePartitionLostListener(String id) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "removePartitionLostListener"));
        }

        @Override
        public String addEntryListener(MapListener listener, K key, boolean includeValue) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "addEntryListener"));
        }

        @SuppressWarnings("rawtypes")
        @Override
        public String addEntryListener(EntryListener listener, K key, boolean includeValue) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "addEntryListener"));
        }

        @Override
        public String addEntryListener(MapListener listener, Predicate<K, V> predicate, boolean includeValue) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "addEntryListener"));
        }

        @SuppressWarnings("rawtypes")
        @Override
        public String addEntryListener(EntryListener listener, Predicate<K, V> predicate, boolean includeValue) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "addEntryListener"));
        }

        @Override
        public String addEntryListener(MapListener listener, Predicate<K, V> predicate, K key, boolean includeValue) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "addEntryListener"));
        }

        @SuppressWarnings("rawtypes")
        @Override
        public String addEntryListener(EntryListener listener, Predicate<K, V> predicate, K key, boolean includeValue) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "addEntryListener"));
        }

        @Override
        public EntryView<K, V> getEntryView(K key) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "getEntryView"));
        }

        @Override
        public boolean evict(K key) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "evict"));
        }

        @Override
        public void evictAll() {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "evictAll"));
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
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "keySet"));
        }

        @SuppressWarnings("rawtypes")
        @Override
        public Set<java.util.Map.Entry<K, V>> entrySet(Predicate predicate) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "entrySet"));
        }

        @SuppressWarnings("rawtypes")
        @Override
        public Collection<V> values(Predicate predicate) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "values"));
        }

        @Override
        public Set<K> localKeySet() {
            return keySet();
        }

        @SuppressWarnings("rawtypes")
        @Override
        public Set<K> localKeySet(Predicate predicate) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "localKeySet"));
        }

        @Override
        public void addIndex(String attribute, boolean ordered) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "addIndex"));
        }

        @Override
        public LocalMapStats getLocalMapStats() {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "getLocalMapStats"));
        }

        @SuppressWarnings("rawtypes")
        @Override
        public Object executeOnKey(K key, EntryProcessor entryProcessor) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "executeOnKey"));
        }

        @SuppressWarnings("rawtypes")
        @Override
        public Map<K, Object> executeOnKeys(Set<K> keys, EntryProcessor entryProcessor) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "executeOnKeys"));
        }

        @SuppressWarnings("rawtypes")
        @Override
        public void submitToKey(K key, EntryProcessor entryProcessor, ExecutionCallback callback) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "submitToKey"));
        }

        @SuppressWarnings("rawtypes")
        @Override
        public ICompletableFuture submitToKey(K key, EntryProcessor entryProcessor) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "submitToKey"));
        }

        @SuppressWarnings("rawtypes")
        @Override
        public Map<K, Object> executeOnEntries(EntryProcessor entryProcessor) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "executeOnEntries"));
        }

        @SuppressWarnings("rawtypes")
        @Override
        public Map<K, Object> executeOnEntries(EntryProcessor entryProcessor, Predicate predicate) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "executeOnEntries"));
        }

        @Override
        public <SuppliedValue, Result> Result aggregate(Supplier<K, V, SuppliedValue> supplier,
            Aggregation<K, SuppliedValue, Result> aggregation) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "aggregate"));
        }

        @Override
        public <SuppliedValue, Result> Result aggregate(Supplier<K, V, SuppliedValue> supplier,
            Aggregation<K, SuppliedValue, Result> aggregation, JobTracker jobTracker) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "aggregate"));
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
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "tryLock with lease time"));
        }

        @Override
        public void unlock() {
            lock.unlock();
        }

        @Override
        public void lock(long leaseTime, TimeUnit timeUnit) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "lock with lease time"));
        }

        @Override
        public void forceUnlock() {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "forceUnlock"));
        }

        @Override
        public Condition newCondition() {
            return lock.newCondition();
        }

        @Override
        public ICondition newCondition(String name) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "newCondition"));
        }

        @Override
        public boolean isLocked() {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "isLocked"));
        }

        @Override
        public boolean isLockedByCurrentThread() {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "isLockedByCurrentThread"));
        }

        @Override
        public int getLockCount() {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "getLockCount"));
        }

        @Override
        public long getRemainingLeaseTime() {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "getRemainingLeaseTime"));
        }

    }

    private abstract class ICollectionImpl<E> implements ICollection<E> {

        private final String name;

        public ICollectionImpl(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String addItemListener(ItemListener<E> listener, boolean includeValue) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "addItemListener"));
        }

        @Override
        public boolean removeItemListener(String registrationId) {
            throw new UnsupportedOperationException(format(OPERATION_NOT_SUPPORTED_MESSAGE, "removeItemListener"));
        }

    }

    private class IListImpl<E> extends ICollectionImpl<E> implements IList<E> {

        private List<E> list;

        public IListImpl(String name) {
            super(name);
            list = Collections.synchronizedList(new LinkedList<>());
        }

        @Override
        public boolean add(E element) {
            return list.add(element);
        }

        @Override
        public void add(int index, E element) {
            list.add(index, element);
        }

        @Override
        public boolean addAll(Collection<? extends E> c) {
            return list.addAll(c);
        }

        @Override
        public boolean addAll(int index, Collection<? extends E> c) {
            return list.addAll(index, c);
        }

        @Override
        public void clear() {
            list.clear();
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
            return list.remove(index);
        }

        @Override
        public boolean remove(Object o) {
           return list.remove(o);
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return list.removeAll(c);
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return list.retainAll(c);
        }

        @Override
        public E set(int index, E element) {
            return list.set(index, element);
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
            list = Collections.synchronizedList(new LinkedList<>());
        }

    }

}
