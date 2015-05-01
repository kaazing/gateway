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

package org.kaazing.gateway.service.messaging.collections;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.ICollection;
import com.hazelcast.core.IList;
import com.hazelcast.core.ILock;
import com.hazelcast.core.IMap;
import com.hazelcast.core.IQueue;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Instance;
import com.hazelcast.core.ItemListener;
import com.hazelcast.core.MapEntry;
import com.hazelcast.monitor.LocalLockStats;
import com.hazelcast.monitor.LocalMapStats;
import com.hazelcast.monitor.LocalQueueStats;
import com.hazelcast.query.Expression;
import com.hazelcast.query.Predicate;
import org.kaazing.gateway.service.cluster.EntryListenerSupport;
import org.kaazing.gateway.util.AtomicCounter;

public class MemoryCollectionsFactory implements CollectionsFactory {

    private final ConcurrentMap<String, IMapImpl<?, ?>> maps;
    private final ConcurrentMap<String, IListImpl<?>> lists;
    private final Map<Object, ILockImpl> locks;
    private final ConcurrentMap<String, AtomicCounter> atomicCounters;

    public MemoryCollectionsFactory() {
        // TODO: avoid memory leak
        maps = new ConcurrentHashMap<>();
        lists = new ConcurrentHashMap<>();
        locks = Collections.synchronizedMap(new WeakHashMap<Object, ILockImpl>());
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
    public ILock getLock(Object owner) {
        synchronized (locks) {
            ILock lock = locks.get(owner);
            if (lock == null) {
                ILockImpl newLock = new ILockImpl(owner);
                locks.put(owner, newLock);
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

    private class MapEntryImpl<K, V> implements MapEntry<K, V> {
        private final K key;
        private final V value;

        public MapEntryImpl(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public long getCost() {
            return 0;
        }

        @Override
        public long getCreationTime() {
            return 0;
        }

        @Override
        public int getHits() {
            return 1;
        }

        @Override
        public long getExpirationTime() {
            return Long.MAX_VALUE;
        }

        @Override
        public long getLastAccessTime() {
            return Long.MAX_VALUE;
        }

        @Override
        public long getLastStoredTime() {
            return 0;
        }

        @Override
        public long getLastUpdateTime() {
            return 0;
        }

        @Override
        public long getVersion() {
            return 1;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null ||
                    !(o instanceof MapEntry)) {
                return false;
            }

            MapEntry that = (MapEntry) o;

            return that.getKey().equals(getKey()) && that.getValue().equals(getValue());

        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            // This method isn't supported, and really should not be.
            // To change a value, simply call setValue() on the map itself,
            // not on this MapEntry object.
            throw new UnsupportedOperationException("setValue");
        }

        @Override
        public int hashCode() {
            int hashCode = key.hashCode();
            hashCode ^= value.hashCode();
            return hashCode;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("{ ");
            sb.append("key=").append(key);
            sb.append(", value=").append(value);
            sb.append(" }");
            return sb.toString();
        }
    }

    private class IMapImpl<K, V> implements IMap<K, V> {

        private final ConcurrentHashMap<K, V> map;
        private final EntryListenerSupport<K,V> listenerSupport;
        private final ConcurrentMap<Object, Lock> locks;
        private final String name;

        public IMapImpl(String name) {
            this.name = name;
            this.map = new ConcurrentHashMap<>();
            this.listenerSupport = new EntryListenerSupport<>();
            this.locks = new ConcurrentHashMap<>();
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
        public MapEntry<K, V> getMapEntry(K key) {
            V value = map.get(key);
            return new MapEntryImpl<>(key, value);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void lock(K key) {
            supplyLock(key).lock();
        }

        @Override
        public void removeEntryListener(EntryListener<K, V> listener) {
            listenerSupport.removeEntryListener(listener);
        }

        @Override
        public void removeEntryListener(EntryListener<K, V> listener, K key) {
            listenerSupport.removeEntryListener(listener, key);
        }

        @Override
        public boolean tryLock(K key) {
            return supplyLock(key).tryLock();
        }

        @Override
        public boolean tryLock(K key, long time, TimeUnit timeunit) {
            try {
                return supplyLock(key).tryLock(time, timeunit);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        public void unlock(K key) {
            supplyLock(key).unlock();
        }

        @Override
        public V putIfAbsent(K key, V value) {
            V oldValue = map.putIfAbsent(key, value);
            if (oldValue == null) {
                EntryEvent<K, V> event = new EntryEvent<>(name, null, EntryEvent.TYPE_ADDED, key, value);
                listenerSupport.entryAdded(event);
            }
            else {
                EntryEvent<K, V> event = new EntryEvent<>(name, null, EntryEvent.TYPE_UPDATED, key, value);
                listenerSupport.entryUpdated(event);
            }
            return oldValue;
        }

        @SuppressWarnings("unchecked")
		@Override
        public boolean remove(Object key, Object value) {
            boolean wasRemoved = map.remove(key, value);
            if (wasRemoved) {
                EntryEvent<K, V> event = new EntryEvent<>(name, null, EntryEvent.TYPE_REMOVED, (K)key, (V)value);
                listenerSupport.entryRemoved(event);
            }
            return wasRemoved;
        }

        @Override
        public V replace(K key, V value) {
            V oldValue = map.replace(key, value);
            if (oldValue != null) {
                EntryEvent<K, V> event = new EntryEvent<>(name, null, EntryEvent.TYPE_UPDATED, key, value);
                listenerSupport.entryUpdated(event);
            }
            else {
                EntryEvent<K, V> event = new EntryEvent<>(name, null, EntryEvent.TYPE_ADDED, key, value);
                listenerSupport.entryAdded(event);
            }
            return oldValue;
        }

        @Override
        public boolean replace(K key, V oldValue, V newValue) {
            boolean wasReplaced = map.replace(key, oldValue, newValue);
            if (wasReplaced) {
                EntryEvent<K, V> event = new EntryEvent<>(name, null, EntryEvent.TYPE_UPDATED, key, newValue);
                listenerSupport.entryUpdated(event);
            }
            return wasReplaced;
        }

        @Override
        public void clear() {
            Set<Map.Entry<K, V>> entries = map.entrySet(); 

            // Rather than calling "map.clear()", we do it the hard way,
            // so that the EntryListeners get invoked properly.
            Iterator<Map.Entry<K, V>> iter = entries.iterator();
            while (iter.hasNext()) {
                Map.Entry<K, V> entry = iter.next();
                remove(entry.getKey(), entry.getValue());
            }
        }

        @Override
        public boolean containsKey(Object key) {
            return map.contains(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return map.containsValue(value);
        }

        @Override
        public Set<Map.Entry<K, V>> entrySet() {
            return map.entrySet();
        }

        @Override
        public V get(Object key) {
            return map.get(key);
        }

        @Override
        public boolean isEmpty() {
            return map.isEmpty();
        }

        @Override
        public Set<K> keySet() {
            return map.keySet();
        }

        @Override
        public V put(K key, V value) {
            V oldValue = map.put(key, value);
            if (oldValue != null) {
                EntryEvent<K, V> event = new EntryEvent<>(name, null, EntryEvent.TYPE_UPDATED, key, value);
                listenerSupport.entryUpdated(event);
            }
            else {
                EntryEvent<K, V> event = new EntryEvent<>(name, null, EntryEvent.TYPE_ADDED, key, value);
                listenerSupport.entryAdded(event);
            }
            return oldValue;
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> m) {
            Set<? extends K> keys = m.keySet();
            for (K key : keys) {
                put(key, m.get(key));
            }
        }

        @Override
        public int size() {
            return map.size();
        }

        @Override
        public Collection<V> values() {
            return map.values();
        }

        @Override
        public void destroy() {
            map.clear();
        }

        @Override
        public Object getId() {
            return this;
        }

        @Override
        public InstanceType getInstanceType() {
            return InstanceType.MAP;
        }

        private Lock supplyLock(Object key) {
            Lock lock = locks.get(key);
            if (lock == null) {
                Lock newLock = new ReentrantLock();
                lock = locks.putIfAbsent(key, newLock);
                if (lock == null) {
                    lock = newLock;
                }
            }
            assert (lock != null);
            return lock;
        }

        @Override
        public void addIndex(String attribute, boolean ordered) {
            // Indices are used to speed up queries across the cluster;
            // as an in-memory collection, we don't need to support queries.
        }

        @Override
        public Set<Map.Entry<K, V>> entrySet(Predicate predicate) {
            Set<Map.Entry<K, V>> entrySet = new LinkedHashSet<>();
            for (Map.Entry<K, V> entry : map.entrySet()) {
                MapEntry me = getMapEntry(entry.getKey());
                if (predicate.apply(me)) {
                    entrySet.add(entry);
                }
            }

            return entrySet;
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean evict(Object key) {
            Object value = map.remove(key);
            boolean removed = (value !=  null);
            if (removed) {
                EntryEvent<K, V> event = new EntryEvent<>(name, null, EntryEvent.TYPE_EVICTED, (K)key, (V) value);
                listenerSupport.entryEvicted(event);
            }
            return removed;
        }

        @Override
        public LocalMapStats getLocalMapStats() {
            throw new UnsupportedOperationException("getLocalMapStats");
        }

        @Override
        public Set<K> keySet(Predicate predicate) {
            Set<K> keySet = new LinkedHashSet<>();
            for (K key : map.keySet()) {
                MapEntry me = getMapEntry(key);
                if (predicate.apply(me)) {
                    keySet.add(key);
                }
            }

            return keySet;
        }

        @Override
        public Set<K> localKeySet() {
            return map.keySet();
        }

        @Override
        public Set<K> localKeySet(Predicate predicate) {
            return keySet(predicate);
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
            Set<V> values = new LinkedHashSet<>();
            for (Map.Entry<K, V> entry : map.entrySet()) {
                MapEntry me = getMapEntry(entry.getKey());
                if (predicate.apply(me)) {
                    values.add(entry.getValue());
                }
            }

            return values;
        }

	@SuppressWarnings("unchecked")
	@Override
	public V remove(Object key) {
            V value = map.remove(key);
            boolean removed = (value !=  null);
            if (removed) {
                EntryEvent<K, V> event = new EntryEvent<>(name, null, EntryEvent.TYPE_REMOVED, (K)key, value);
                listenerSupport.entryRemoved(event);
            }
            return value;
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
            throw new UnsupportedOperationException("putAndLock");
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

    private abstract class InstanceImpl implements Instance {

        @Override
        public abstract InstanceType getInstanceType();

        @Override
        public Object getId() {
            return this;
        }

        @Override
        public void destroy() {
        }

    }

    private class ILockImpl extends InstanceImpl implements ILock {

        private final Object owner;
        private final Lock lock;

        public ILockImpl(Object owner) {
            this.owner = owner;
            this.lock = new ReentrantLock();
        }

        @Override
        public void lock() {
            lock.lock();
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            lock.lockInterruptibly();
        }

        @Override
        public Condition newCondition() {
            return lock.newCondition();
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
        public void unlock() {
            lock.unlock();
        }

        @Override
        public InstanceType getInstanceType() {
            return InstanceType.LOCK;
        }

        @Override
        public Object getLockObject() {
            return owner;
        }

        @Override
        public void destroy() {
            locks.remove(owner);
        }

        @Override
        public LocalLockStats getLocalLockStats() {
            // TODO Auto-generated method stub
            return null;
        }

    }

    private abstract class ICollectionImpl<E> extends InstanceImpl implements ICollection<E> {

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
        public void addItemListener(ItemListener<E> listener, boolean includeValue) {
            listenerSupport.addItemListener(listener, includeValue);
        }

        @Override
        public void removeItemListener(ItemListener<E> listener) {
            listenerSupport.removeItemListener(listener);
        }

    }

    private class IListImpl<E> extends ICollectionImpl<E> implements IList<E> {
        private List<E> list;

        public IListImpl(String name) {
            super(name);
            list = new LinkedList<>();
        }

        @Override
        public InstanceType getInstanceType() {
            return InstanceType.LIST;
        }

        @Override
        public boolean add(E e) {
            return list.add(e);
        }

        @Override
        public void add(int index, E element) {
            list.add(index, element);
            listenerSupport.fireItemAdded(element);
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
            Iterator<E> iterator = list.iterator();
            listenerSupport.fireItemRemoved(iterator);
            list = new LinkedList<>();
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
            listenerSupport.fireItemRemoved(element);
            return element;
        }

        @Override
        public boolean remove(Object o) {
            boolean wasRemoved = list.remove(o);
            if (wasRemoved) {
                listenerSupport.fireItemRemoved(o);
            }
            return wasRemoved;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            boolean wasAnyRemoved = false;
            for (Object o : c) {
                wasAnyRemoved |= remove(o);
            }
            return wasAnyRemoved;
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            //return list.retainAll(c);
            throw new UnsupportedOperationException("retainAll does not support notifications");
        }

        @Override
        public E set(int index, E element) {
            E oldValue = list.set(index, element);
            if (oldValue != element) {
                listenerSupport.fireItemRemoved(oldValue);
                listenerSupport.fireItemAdded(element);
            }
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

    }

    private class IQueueImpl<E> implements IQueue<E> {

        private ArrayBlockingQueue<E> queue;
        private static final long serialVersionUID = 1L;
        private final String name;
        private final int capacity;
        private final ItemListenerSupport<E> itemListenerSupport;

        public IQueueImpl(String name, int capacity) {
            this.name = name;
            this.capacity = capacity;
            this.queue = new ArrayBlockingQueue<>(capacity);
            this.itemListenerSupport = new ItemListenerSupport<>();
        }

        @Override
        public void addItemListener(ItemListener<E> listener, boolean includeValue) {
            this.itemListenerSupport.addItemListener(listener, includeValue);
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public void removeItemListener(ItemListener<E> listener) {
            this.itemListenerSupport.removeItemListener(listener);
        }

        @Override
        public void destroy() {
            queue.clear();
        }

        @Override
        public Object getId() {
            return this.name;
        }

        @Override
        public InstanceType getInstanceType() {
            return InstanceType.QUEUE;
        }

        @Override
        public boolean add(E e) {
            boolean added = queue.add(e);
            itemListenerSupport.fireItemAdded(e);
            return added;
        }

        @Override
        public void clear() {
            ArrayBlockingQueue<E> oldQueue = queue;
            queue = new ArrayBlockingQueue<>(capacity);
            itemListenerSupport.fireItemRemoved(oldQueue.iterator());
            oldQueue.clear();
        }

        @Override
        public int drainTo(Collection<? super E> c, int maxElements) {
            int drained = queue.drainTo(c, maxElements);
            itemListenerSupport.fireItemRemoved(c.iterator());
            return drained;
        }

        @Override
        public int drainTo(Collection<? super E> c) {
            int drained = queue.drainTo(c);
            itemListenerSupport.fireItemRemoved(c.iterator());
            return drained;
        }

        @Override
        public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
            boolean offered = queue.offer(e, timeout, unit);
            itemListenerSupport.fireItemAdded(e);
            return offered;
        }

        @Override
        public boolean offer(E e) {
            boolean offered = queue.offer(e);
            itemListenerSupport.fireItemAdded(e);
            return offered;
        }

        @Override
        public E poll() {
            E item = queue.poll();
            itemListenerSupport.fireItemRemoved(item);
            return item;
        }

        @Override
        public E poll(long timeout, TimeUnit unit) throws InterruptedException {
            E item = queue.poll(timeout, unit);
            itemListenerSupport.fireItemRemoved(item);
            return item;
        }

        @Override
        public void put(E e) throws InterruptedException {
            queue.put(e);
            itemListenerSupport.fireItemAdded(e);
        }

        @Override
        public boolean remove(Object o) {
            boolean removed = queue.remove(o);
            itemListenerSupport.fireItemRemoved(o);
            return removed;
        }

        @Override
        public E take() throws InterruptedException {
            E item = queue.take();
            itemListenerSupport.fireItemRemoved(item);
            return item;
        }

        @Override
        public boolean addAll(Collection<? extends E> c) {
            boolean allAdded = queue.addAll(c);
            itemListenerSupport.fireItemAdded(c.iterator());
            return allAdded;
        }

        @Override
        public E remove() {
            E item = queue.remove();
            itemListenerSupport.fireItemRemoved(item);
            return item;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            boolean allRemoved = queue.removeAll(c);
            itemListenerSupport.fireItemRemoved(c.iterator());
            return allRemoved;
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
            return queue.retainAll(c);
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

    }

    private class ItemListenerSupport<E> {

        private ConcurrentHashMap<ItemListener<E>, Boolean> listenersMap = new ConcurrentHashMap<>();

        public void addItemListener(ItemListener<E> listener, boolean includeValue) {
            listenersMap.put(listener, includeValue);
        }

        public void removeItemListener(ItemListener<E> listener) {
            listenersMap.remove(listener);
        }

        public void fireItemAdded(Iterator<? extends E> iterator) {
            while (iterator.hasNext()) {
                fireItemAdded(iterator.next());
            }
        }

        public void fireItemAdded(E item) {
            for (ItemListener<E> listener : listenersMap.keySet()) {
                listener.itemAdded(listenersMap.get(listener) ? item : null);
            }
        }

        public void fireItemRemoved(Iterator<? super E> iterator) {
            while (iterator.hasNext()) {
                fireItemRemoved(iterator.next());
            }
        }

        @SuppressWarnings("unchecked")
        public void fireItemRemoved(Object item) {
            for (ItemListener<E> listener : listenersMap.keySet()) {
                listener.itemRemoved(listenersMap.get(listener) ? (E)item : null);
            }
        }

    }
}
