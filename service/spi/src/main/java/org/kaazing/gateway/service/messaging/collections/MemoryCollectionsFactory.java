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

import java.util.logging.Level;

import org.kaazing.gateway.util.AtomicCounter;

import com.hazelcast.config.Config;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IAtomicLong;
import com.hazelcast.core.IList;
import com.hazelcast.core.ILock;
import com.hazelcast.core.IMap;
import com.hazelcast.core.IQueue;
import com.hazelcast.core.ITopic;
import com.hazelcast.instance.GroupProperties;

public class MemoryCollectionsFactory implements CollectionsFactory {
    private static HazelcastInstance hazelcastInstance ;

    static {
        Config config = new Config("StandAlone");
        config.setProperty(GroupProperties.PROP_PHONE_HOME_ENABLED, "false");
        config.setProperty("hazelcast.io.thread.count", "1");
        config.getNetworkConfig().setPortAutoIncrement(false);
        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
        hazelcastInstance = Hazelcast.newHazelcastInstance(config);
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger("com.hazelcast");
        logger.setLevel(Level.OFF);

    }
    
   
    public MemoryCollectionsFactory() {
    }

    @Override
    public <E> IList<E> getList(String name) {
        return hazelcastInstance.getList(name);
    }

    @Override
    public <K, V> IMap<K, V> getMap(String name) {
        return hazelcastInstance.getMap(name);
    }

    @Override
    public <E> IQueue<E> getQueue(String name) {
        return hazelcastInstance.getQueue(name);
    }

    @Override
    public <E> ITopic<E> getTopic(String name) {
        return hazelcastInstance.getTopic(name);
    }

    @Override
    public ILock getLock(String name) {
        return hazelcastInstance.getLock(name);
    }

    @Override
    public <K, V> void addEntryListener(EntryListener<K, V> listener, String name) {
        IMap<K, V> map = hazelcastInstance.getMap(name);
        if (map != null) {
            map.addEntryListener(listener, true);
        }
    }

    @Override
    public AtomicCounter getAtomicCounter(String name) {
        return new ClusterAtomicCounter(hazelcastInstance.getAtomicLong(name));
    }

    private final class ClusterAtomicCounter implements AtomicCounter {
        private IAtomicLong atomicNumber;

        private ClusterAtomicCounter(IAtomicLong number) {
            atomicNumber = number;
        }

        @Override
        public long get() {
            return atomicNumber.get();
        }

        @Override
        public long incrementAndGet() {
            return atomicNumber.incrementAndGet();
        }

        @Override
        public long decrementAndGet() {
            return atomicNumber.decrementAndGet();
        }

        @Override
        public boolean compareAndSet(long expect, long update) {
            return atomicNumber.compareAndSet(expect, update);
        }
    }

//
//    private final ConcurrentMap<String, IMapImpl<?, ?>> maps;
//    private final ConcurrentMap<String, IListImpl<?>> lists;
//    private final Map<Object, ILockImpl> locks;
//   // private final ConcurrentMap<String, AtomicCounter> atomicCounters;
//
//    public MemoryCollectionsFactory() {
//        // TODO: avoid memory leak
//        maps = new ConcurrentHashMap<>();
//        lists = new ConcurrentHashMap<>();
//        locks = Collections.synchronizedMap(new WeakHashMap<>());
//     //   atomicCounters = new ConcurrentHashMap<>();
//    }
//
////    @SuppressWarnings("unchecked")
////    @Override
////    public <E> IList<E> getList(String name) {
////        IListImpl<E> list = (IListImpl<E>)lists.get(name);
////        if (list == null) {
////            IListImpl<E> newList = new IListImpl<>(name);
////            list = (IListImpl<E>)lists.putIfAbsent(name, newList);
////            if (list == null) {
////                list = newList;
////            }
////        }
////        return list;
////    }
//
//    @Override
//    public <E> IQueue<E> getQueue(String name) {
//        throw new UnsupportedOperationException("getQueue");
//
//      //  return new IQueueImpl<>(name, 100);
//    }
//
//    @Override
//    public <E> ITopic<E> getTopic(String name) {
//        throw new UnsupportedOperationException("getTopic");
//    }
//
//    @SuppressWarnings("unchecked")
//    @Override
//    public <K, V> IMap<K, V> getMap(String name) {
//        IMapImpl<K, V> map = (IMapImpl<K, V>)maps.get(name);
//        if (map == null) {
//            IMapImpl<K, V> newMap = new IMapImpl<>(name);
//            map = (IMapImpl<K, V>)maps.putIfAbsent(name, newMap);
//            if (map == null) {
//                map = newMap;
//            }
//        }
//        return map;
//    }
//
//    @Override
//    public ILock getLock(String owner) {
//        throw new UnsupportedOperationException("getLock");
//    }
//
//    @Override
//    public ILock getLock(String owner) {
//        synchronized (locks) {
//            ILock lock = locks.get(owner);
//            if (lock == null) {
//                ILockImpl newLock = new ILockImpl(owner);
//                locks.put(owner, newLock);
//                lock = newLock;
//            }
//            assert (lock != null);
//            return lock;
//        }
//    }
//
//    @Override @SuppressWarnings("unchecked")
//    public <K, V> void addEntryListener(EntryListener<K, V> listener, String name) {
////        IMapImpl<K ,V> map = (IMapImpl<K, V>)getMap(name);  // force create if not already created.
////        map.addEntryListener(listener, true);
//        throw new UnsupportedOperationException("addEntryListener");
//    }
//
//    @Override
//    public AtomicCounter getAtomicCounter(String name) {
//        throw new UnsupportedOperationException("getAtomicCounter");
//    }
////        if (atomicCounters.containsKey(name)) {
////            return atomicCounters.get(name);
////        } else {
////            StandaloneAtomicCounter counter = new StandaloneAtomicCounter(new AtomicLong(0));
////            AtomicCounter existingCounter = atomicCounters.putIfAbsent(name, counter);
////            if (existingCounter == null) {
////                existingCounter = counter;
////            }
////            return existingCounter;
////        }
////    }
///*/
//    private class StandaloneAtomicCounter implements AtomicCounter {
//        private AtomicLong atomicLong;
//
//        private StandaloneAtomicCounter(AtomicLong number) {
//            atomicLong = number;
//        }
//
//        @Override
//        public long get() {
//            return atomicLong.get();
//        }
//
//        @Override
//        public long incrementAndGet() {
//            return atomicLong.incrementAndGet();
//        }
//
//        @Override
//        public long decrementAndGet() {
//            return atomicLong.decrementAndGet();
//        }
//
//        @Override
//        public boolean compareAndSet(long expect, long update) {
//            return atomicLong.compareAndSet(expect, update);
//        }
//    }
///*/
//  
//    private class MapEntryViewImpl<K, V> implements EntryView<K, V> {
//        private final K key;
//        private final V value;
//
//        public MapEntryViewImpl(K key, V value) {
//            this.key = key;
//            this.value = value;
//        }
//
//        @Override
//        public long getCost() {
//            return 0;
//        }
//
//        @Override
//        public long getCreationTime() {
//            return 0;
//        }
//
//        @Override
//        public long getHits() {
//            return 1;
//        }
//
//        @Override
//        public long getExpirationTime() {
//            return Long.MAX_VALUE;
//        }
//
//        @Override
//        public long getLastAccessTime() {
//            return Long.MAX_VALUE;
//        }
//
//        @Override
//        public long getLastStoredTime() {
//            return 0;
//        }
//
//        @Override
//        public long getLastUpdateTime() {
//            return 0;
//        }
//
//        @Override
//        public long getVersion() {
//            return 1;
//        }
//
//        @Override
//        public boolean equals(Object o) {//TODO double check this !!!!
//            if (o == null ||
//                    !(o instanceof EntryView)) {
//                return false;
//            }
//
//            EntryView that = (EntryView) o;
//
//            return that.getKey().equals(getKey()) && that.getValue().equals(getValue());
//
//        }
//
//        @Override
//        public K getKey() {
//            return key;
//        }
//
//        @Override
//        public V getValue() {
//            return value;
//        }
//
//        @Override
//        public int hashCode() {
//            int hashCode = key.hashCode();
//            hashCode ^= value.hashCode();
//            return hashCode;
//        }
//
//        @Override
//        public String toString() {
//            StringBuilder sb = new StringBuilder();
//            sb.append("{ ");
//            sb.append("key=").append(key);
//            sb.append(", value=").append(value);
//            sb.append(" }");
//            return sb.toString();
//        }
//
//        @Override
//        public long getTtl() {
//            // TODO Auto-generated method stub
//            return 0;
//        }
//    }
//
//    private class IMapImpl<K, V> implements IMap<K, V> {
//
//        private final ConcurrentHashMap<K, V> map;
//        private final EntryListenerSupport<K,V> listenerSupport;
//        private final ConcurrentMap<Object, Lock> locks;
//        private final String name;
//
//        public IMapImpl(String name) {
//            this.name = name;
//            this.map = new ConcurrentHashMap<>();
//            this.listenerSupport = new EntryListenerSupport<>();
//            this.locks = new ConcurrentHashMap<>();
//        }
//
//        @Override
//        public void addEntryListener(EntryListener<K, V> listener, boolean includeValue) {
//            listenerSupport.addEntryListener(listener, includeValue);
//        }
//
//        @Override
//        public void addEntryListener(EntryListener<K, V> listener, K key, boolean includeValue) {
//            listenerSupport.addEntryListener(listener, key, includeValue);
//
//        }
//
//        @Override
//        public EntryView<K, V> getEntryView(K key) {
//            V value = map.get(key);
//            return new MapEntryViewImpl<>(key, value);
//        }
//
//        @Override
//        public String getName() {
//            return name;
//        }
//
//        @Override
//        public void lock(K key) {
//            supplyLock(key).lock();
//        }
//
//        @Override
//        public void removeEntryListener(EntryListener<K, V> listener) {
//            listenerSupport.removeEntryListener(listener);
//        }
//
//        @Override
//        public void removeEntryListener(EntryListener<K, V> listener, K key) {
//            listenerSupport.removeEntryListener(listener, key);
//        }
//
//        @Override
//        public boolean tryLock(K key) {
//            return supplyLock(key).tryLock();
//        }
//
//        @Override
//        public boolean tryLock(K key, long time, TimeUnit timeunit) {
//            try {
//                return supplyLock(key).tryLock(time, timeunit);
//            }
//            catch (InterruptedException e) {
//                e.printStackTrace();
//                return false;
//            }
//        }
//
//        @Override
//        public void unlock(K key) {
//            supplyLock(key).unlock();
//        }
//
//        @Override
//        public V putIfAbsent(K key, V value) {
//            V oldValue = map.putIfAbsent(key, value);
//            if (oldValue == null) {
//                EntryEvent<K, V> event = new EntryEvent<>(name, null, EntryEventType.ADDED.getType(), key, oldValue, value);
//                listenerSupport.entryAdded(event);
//            }
//            else {
//                EntryEvent<K, V> event = new EntryEvent<>(name, null, EntryEventType.UPDATED.getType(), key, oldValue, value);
//                listenerSupport.entryUpdated(event);
//            }
//            return oldValue;
//        }
//
//        @SuppressWarnings("unchecked")
//        @Override
//        public boolean remove(Object key, Object value) {
//            boolean wasRemoved = map.remove(key, value);
//            if (wasRemoved) {
//                EntryEvent<K, V> event = (EntryEvent<K, V>) new EntryEvent<>(name, null, EntryEventType.REMOVED.getType(), key, value, null);
//                listenerSupport.entryRemoved(event);
//            }
//            return wasRemoved;
//        }
//
//        @Override
//        public V replace(K key, V value) {
//            V oldValue = map.replace(key, value);
//            if (oldValue != null) {
//                EntryEvent<K, V> event = new EntryEvent<>(name, null, EntryEventType.UPDATED.getType(), key, oldValue, value);
//                listenerSupport.entryUpdated(event);
//            }
//            else {
//                EntryEvent<K, V> event = new EntryEvent<>(name, null, EntryEventType.ADDED.getType(), key, oldValue, value);
//                listenerSupport.entryAdded(event);
//            }
//            return oldValue;
//        }
//
//        @Override
//        public boolean replace(K key, V oldValue, V newValue) {
//            boolean wasReplaced = map.replace(key, oldValue, newValue);
//            if (wasReplaced) {
//                EntryEvent<K, V> event = new EntryEvent<>(name, null, EntryEventType.UPDATED.getType(), key, oldValue, newValue);
//                listenerSupport.entryUpdated(event);
//            }
//            return wasReplaced;
//        }
//
//        @Override
//        public void clear() {
//            Set<Map.Entry<K, V>> entries = map.entrySet(); 
//
//            // Rather than calling "map.clear()", we do it the hard way,
//            // so that the EntryListeners get invoked properly.
//            Iterator<Map.Entry<K, V>> iter = entries.iterator();
//            while (iter.hasNext()) {
//                Map.Entry<K, V> entry = iter.next();
//                remove(entry.getKey(), entry.getValue());
//            }
//        }
//
//        @Override
//        public boolean containsKey(Object key) {
//            return map.contains(key);
//        }
//
//        @Override
//        public boolean containsValue(Object value) {
//            return map.containsValue(value);
//        }
//
//        @Override
//        public Set<Map.Entry<K, V>> entrySet() {
//            return map.entrySet();
//        }
//
//        @Override
//        public V get(Object key) {
//            return map.get(key);
//        }
//
//        @Override
//        public boolean isEmpty() {
//            return map.isEmpty();
//        }
//
//        @Override
//        public Set<K> keySet() {
//            return map.keySet();
//        }
//
//        @Override
//        public V put(K key, V value) {
//            V oldValue = map.put(key, value);
//            if (oldValue != null) {
//                EntryEvent<K, V> event = new EntryEvent<>(name, null, EntryEventType.UPDATED.getType(), key, oldValue, value);
//                listenerSupport.entryUpdated(event);
//            }
//            else {
//                EntryEvent<K, V> event = new EntryEvent<>(name, null, EntryEventType.ADDED.getType(), key, oldValue, value);
//                listenerSupport.entryAdded(event);
//            }
//            return oldValue;
//        }
//
//        @Override
//        public void putAll(Map<? extends K, ? extends V> m) {
//            Set<? extends K> keys = m.keySet();
//            for (K key : keys) {
//                put(key, m.get(key));
//            }
//        }
//
//        @Override
//        public int size() {
//            return map.size();
//        }
//
//        @Override
//        public Collection<V> values() {
//            return map.values();
//        }
//
//        @Override
//        public void destroy() {
//            map.clear();
//        }
//
//        private Lock supplyLock(Object key) {
//            Lock lock = locks.get(key);
//            if (lock == null) {
//                Lock newLock = new ReentrantLock();
//                lock = locks.putIfAbsent(key, newLock);
//                if (lock == null) {
//                    lock = newLock;
//                }
//            }
//            assert (lock != null);
//            return lock;
//        }
//
//        @Override
//        public void addIndex(String attribute, boolean ordered) {
//            // Indices are used to speed up queries across the cluster;
//            // as an in-memory collection, we don't need to support queries.
//        }
//
//        @Override
//        public Set<Map.Entry<K, V>> entrySet(Predicate predicate) {//TODO validate
//            Set<Map.Entry<K, V>> entrySet = new LinkedHashSet<>();
//            for (Map.Entry<K, V> entry : map.entrySet()) {
//                EntryView me = getEntryView(entry.getKey());
//                if (predicate.apply((java.util.Map.Entry) me)) {
//                    entrySet.add(entry);
//                }
//                // MapEntry me = getMapEntry(entry.getKey());
////                if (predicate.apply(me)) {
////                    entrySet.add(entry);
////                }
//            }
//
//            return entrySet;
//        }
//
//        @SuppressWarnings("unchecked")
//        @Override
//        public boolean evict(Object key) {
//            Object value = map.remove(key);
//            boolean removed = (value !=  null);
//            if (removed) {
//                EntryEvent<K, V> event = new EntryEvent<>(name, null, EntryEventType.EVICTED.getType(), (K)key, (V) value, null);
//                listenerSupport.entryEvicted(event);
//            }
//            return removed;
//        }
//
//        @Override
//        public LocalMapStats getLocalMapStats() {
//            throw new UnsupportedOperationException("getLocalMapStats");
//        }
//
//        @Override
//        public Set<K> keySet(Predicate predicate) {//TODO validate 
//            Set<K> keySet = new LinkedHashSet<>();
//            for (K key : map.keySet()) {
//                EntryView me = getEntryView(key);
//                if (predicate.apply((java.util.Map.Entry) me)) {
//                    keySet.add(key);
//                }
//            }
//
//            return keySet;
//        }
//
//        @Override
//        public Set<K> localKeySet() {
//            return map.keySet();
//        }
//
//        @Override
//        public Set<K> localKeySet(Predicate predicate) {
//            return keySet(predicate);
//        }
//
//        @Override
//        public V put(K key, V value, long ttl, TimeUnit timeunit) {
//            throw new UnsupportedOperationException("put");
//        }
//
//        @Override
//        public V putIfAbsent(K key, V value, long ttl, TimeUnit timeunit) {
//            throw new UnsupportedOperationException("putIfAbsent");
//        }
//
//        @Override
//        public boolean tryPut(K key, V value, long timeout, TimeUnit timeunit) {
//            throw new UnsupportedOperationException("tryPut");
//        }
//
//        @Override
//        public Collection<V> values(Predicate predicate) {
//            Set<V> values = new LinkedHashSet<>();
//            for (Map.Entry<K, V> entry : map.entrySet()) {
//                EntryView<K, V> me = getEntryView(entry.getKey());
//                if (predicate.apply((java.util.Map.Entry) me)) {
//                    values.add(entry.getValue());
//                }
//            }
//
//            return values;
//        }
//
//	@SuppressWarnings("unchecked")
//	@Override
//	public V remove(Object key) {
//            V value = map.remove(key);
//            boolean removed = (value !=  null);
//            if (removed) {
//                EntryEvent<K, V> event = new EntryEvent<>(name, null, EntryEventType.REMOVED.getType(), (K)key, value, null);
//                listenerSupport.entryRemoved(event);
//            }
//            return value;
//		}
//
//        @Override
//        public Future<V> getAsync(K key) {
//            throw new UnsupportedOperationException("getAsync");
//        }
//
//        @Override
//        public Future<V> putAsync(K key, V value) {
//            throw new UnsupportedOperationException("putAsync");
//        }
//
//        @Override
//        public String addLocalEntryListener(MapListener paramMapListener) {
//           
//            throw new UnsupportedOperationException("addLocalEntryListener");
//        }
//
//        @Override
//        public void flush() {
//            throw new UnsupportedOperationException("flush");
//        }
//
//        @Override
//        public Map<K, V> getAll(Set<K> arg0) {
//            throw new UnsupportedOperationException("getAll");
//        }
//
//        @Override
//        public void putTransient(K arg0, V arg1, long arg2, TimeUnit arg3) {
//            throw new UnsupportedOperationException("putTransient");
//        }
//
//        @Override
//        public Future<V> removeAsync(K arg0) {
//            throw new UnsupportedOperationException("removeAsync");
//        }
//
//        @Override
//        public boolean tryRemove(K paramK, long paramLong, TimeUnit paramTimeUnit) {
//            throw new UnsupportedOperationException("tryRemove");
//        }
//        @Override
//        public String getPartitionKey() {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public String getServiceName() {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public void delete(Object paramObject) {
//            // TODO Auto-generated method stub
//            
//        }
//
//        @Override
//        public void loadAll(boolean paramBoolean) {
//            // TODO Auto-generated method stub
//            
//        }
//
//        @Override
//        public void loadAll(Set<K> paramSet, boolean paramBoolean) {
//            // TODO Auto-generated method stub
//            
//        }
//
//        @Override
//        public Future<V> putAsync(K paramK, V paramV, long paramLong, TimeUnit paramTimeUnit) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public void set(K paramK, V paramV) {
//            // TODO Auto-generated method stub
//            
//        }
//
//        @Override
//        public void set(K paramK, V paramV, long paramLong, TimeUnit paramTimeUnit) {
//            // TODO Auto-generated method stub
//            
//        }
//
//        @Override
//        public void lock(K paramK, long paramLong, TimeUnit paramTimeUnit) {
//            // TODO Auto-generated method stub
//            
//        }
//
//        @Override
//        public boolean isLocked(K paramK) {
//            // TODO Auto-generated method stub
//            return false;
//        }
//
//        @Override
//        public boolean tryLock(K paramK,
//                               long paramLong1,
//                               TimeUnit paramTimeUnit1,
//                               long paramLong2,
//                               TimeUnit paramTimeUnit2) throws InterruptedException {
//            // TODO Auto-generated method stub
//            return false;
//        }
//
//        @Override
//        public void forceUnlock(K paramK) {
//            // TODO Auto-generated method stub
//            
//        }
//
//
//        @Override
//        public String addInterceptor(MapInterceptor paramMapInterceptor) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public void removeInterceptor(String paramString) {
//            // TODO Auto-generated method stub
//            
//        }
//
//        @Override
//        public boolean removeEntryListener(String paramString) {
//            // TODO Auto-generated method stub
//            return false;
//        }
//
//        @Override
//        public String addPartitionLostListener(MapPartitionLostListener paramMapPartitionLostListener) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public boolean removePartitionLostListener(String paramString) {
//            // TODO Auto-generated method stub
//            return false;
//        }
//
//        @Override
//        public void evictAll() {
//            // TODO Auto-generated method stub
//            
//        }
//
//        @Override
//        public Object executeOnKey(K paramK, EntryProcessor paramEntryProcessor) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public Map<K, Object> executeOnKeys(Set<K> paramSet, EntryProcessor paramEntryProcessor) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public void submitToKey(K paramK, EntryProcessor paramEntryProcessor, ExecutionCallback paramExecutionCallback) {
//            // TODO Auto-generated method stub
//            
//        }
//
//        @Override
//        public Future submitToKey(K paramK, EntryProcessor paramEntryProcessor) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public Map<K, Object> executeOnEntries(EntryProcessor paramEntryProcessor) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public Map<K, Object> executeOnEntries(EntryProcessor paramEntryProcessor, Predicate paramPredicate) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public <SuppliedValue, Result> Result aggregate(Supplier<K, V, SuppliedValue> paramSupplier,
//                                                        Aggregation<K, SuppliedValue, Result> paramAggregation) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public <SuppliedValue, Result> Result aggregate(Supplier<K, V, SuppliedValue> paramSupplier,
//                                                        Aggregation<K, SuppliedValue, Result> paramAggregation,
//                                                        JobTracker paramJobTracker) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//    }
//
////    private abstract class InstanceImpl implements HazelcastInstance {
////
////        @Override
////        public abstract Collection<DistributedObject> getDistributedObjects();
////
////        @Override
////        public String getName() {
////            return this.getName();
////        }
////
////    }
///*/
//    private class ILockImpl extends InstanceImpl implements ILock {
//
//        private final String owner;
//        private final Lock lock;
//
//        public ILockImpl(String owner) {
//            this.owner = owner;
//            this.lock = new ReentrantLock();
//        }
//
//        @Override
//        public void lock() {
//            lock.lock();
//        }
//
//        @Override
//        public void lockInterruptibly() throws InterruptedException {
//            lock.lockInterruptibly();
//        }
//
//        @Override
//        public Condition newCondition() {
//            return lock.newCondition();
//        }
//
//        @Override
//        public boolean tryLock() {
//            return lock.tryLock();
//        }
//
//        @Override
//        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
//            return lock.tryLock(time, unit);
//        }
//
//        @Override
//        public void unlock() {
//            lock.unlock();
//        }
//
//        @Override
//        public void destroy() {
//            locks.remove(owner);
//        }
//
//        @Override
//        public String getPartitionKey() {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public String getServiceName() {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public <E> IQueue<E> getQueue(String paramString) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public <E> ITopic<E> getTopic(String paramString) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public <E> ISet<E> getSet(String paramString) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public <E> IList<E> getList(String paramString) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public <K, V> IMap<K, V> getMap(String paramString) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public <K, V> ReplicatedMap<K, V> getReplicatedMap(String paramString) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public JobTracker getJobTracker(String paramString) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public <K, V> MultiMap<K, V> getMultiMap(String paramString) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public ILock getLock(String paramString) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public <E> Ringbuffer<E> getRingbuffer(String paramString) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public <E> ITopic<E> getReliableTopic(String paramString) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public Cluster getCluster() {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public Endpoint getLocalEndpoint() {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public IExecutorService getExecutorService(String paramString) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public <T> T executeTransaction(TransactionalTask<T> paramTransactionalTask) throws TransactionException {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public <T> T executeTransaction(TransactionOptions paramTransactionOptions,
//                                        TransactionalTask<T> paramTransactionalTask) throws TransactionException {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public TransactionContext newTransactionContext() {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public TransactionContext newTransactionContext(TransactionOptions paramTransactionOptions) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public IdGenerator getIdGenerator(String paramString) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public IAtomicLong getAtomicLong(String paramString) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public <E> IAtomicReference<E> getAtomicReference(String paramString) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public ICountDownLatch getCountDownLatch(String paramString) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public ISemaphore getSemaphore(String paramString) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public String addDistributedObjectListener(DistributedObjectListener paramDistributedObjectListener) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public boolean removeDistributedObjectListener(String paramString) {
//            // TODO Auto-generated method stub
//            return false;
//        }
//
//        @Override
//        public Config getConfig() {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public PartitionService getPartitionService() {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public QuorumService getQuorumService() {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public ClientService getClientService() {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public LoggingService getLoggingService() {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public LifecycleService getLifecycleService() {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public <T extends DistributedObject> T getDistributedObject(String paramString1, String paramString2) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public ConcurrentMap<String, Object> getUserContext() {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public HazelcastXAResource getXAResource() {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public void shutdown() {
//            // TODO Auto-generated method stub
//            
//        }
//
//        @Override
//        public void forceUnlock() {
//            // TODO Auto-generated method stub
//            
//        }
//
//        @Override
//        public Object getKey() {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public int getLockCount() {
//            // TODO Auto-generated method stub
//            return 0;
//        }
//
//        @Override
//        public long getRemainingLeaseTime() {
//            // TODO Auto-generated method stub
//            return 0;
//        }
//
//        @Override
//        public boolean isLocked() {
//            // TODO Auto-generated method stub
//            return false;
//        }
//
//        @Override
//        public boolean isLockedByCurrentThread() {
//            // TODO Auto-generated method stub
//            return false;
//        }
//
//        @Override
//        public void lock(long paramLong, TimeUnit paramTimeUnit) {
//            // TODO Auto-generated method stub
//            
//        }
//
//        @Override
//        public ICondition newCondition(String paramString) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public boolean tryLock(long paramLong1, TimeUnit paramTimeUnit1, long paramLong2, TimeUnit paramTimeUnit2)
//                throws InterruptedException {
//            // TODO Auto-generated method stub
//            return false;
//        }
//
//        @Override
//        public Collection<DistributedObject> getDistributedObjects() {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//    }
///*/
// /*/
//    private abstract class ICollectionImpl<E> extends InstanceImpl implements ICollection<E> {
//
//        private final String name;
//        protected final ItemListenerSupport<E> listenerSupport;
//
//        public ICollectionImpl(String name) {
//            this.name = name;
//            this.listenerSupport = new ItemListenerSupport<>();
//        }
//
//        @Override
//        public String getName() {
//            return name;
//        }
//
//        @Override
//        public String addItemListener(ItemListener<E> listener, boolean includeValue) {
//            listenerSupport.addItemListener(listener.toString(), includeValue);
//            return name;
//        }
//
//        @Override
//        public void removeItemListener(String name) {
//            listenerSupport.removeItemListener(name);
//        }
//
//    }
///*/
//    /*/
//    private class IListImpl<E> extends ICollectionImpl<E> implements IList<E> {
//        private List<E> list;
//
//        public IListImpl(String name) {
//            super(name);
//            list = new LinkedList<>();
//        }
//
//        @Override
//        public boolean add(E e) {
//            return list.add(e);
//        }
//
//        @Override
//        public void add(int index, E element) {
//            list.add(index, element);
//            listenerSupport.fireItemAdded(element);
//        }
//
//        @Override
//        public boolean addAll(Collection<? extends E> c) {
//            return list.addAll(c);
//        }
//
//        @Override
//        public boolean addAll(int index, Collection<? extends E> c) {
//            return list.addAll(index, c);
//        }
//
//        @Override
//        public void clear() {
//            Iterator<E> iterator = list.iterator();
//            listenerSupport.fireItemRemoved(iterator);
//            list = new LinkedList<>();
//        }
//
//        @Override
//        public boolean contains(Object o) {
//            return list.contains(o);
//        }
//
//        @Override
//        public boolean containsAll(Collection<?> c) {
//            return list.containsAll(c);
//        }
//
//        @Override
//        public E get(int index) {
//            return list.get(index);
//        }
//
//        @Override
//        public int indexOf(Object o) {
//            return list.indexOf(o);
//        }
//
//        @Override
//        public boolean isEmpty() {
//            return list.isEmpty();
//        }
//
//        @Override
//        public Iterator<E> iterator() {
//            return list.iterator();
//        }
//
//        @Override
//        public int lastIndexOf(Object o) {
//            return list.lastIndexOf(o);
//        }
//
//        @Override
//        public ListIterator<E> listIterator() {
//            return list.listIterator();
//        }
//
//        @Override
//        public ListIterator<E> listIterator(int index) {
//            return list.listIterator(index);
//        }
//
//        @Override
//        public E remove(int index) {
//            E element = list.remove(index);
//            listenerSupport.fireItemRemoved(element);
//            return element;
//        }
//
//        @Override
//        public boolean remove(Object o) {
//            boolean wasRemoved = list.remove(o);
//            if (wasRemoved) {
//                listenerSupport.fireItemRemoved(o);
//            }
//            return wasRemoved;
//        }
//
//        @Override
//        public boolean removeAll(Collection<?> c) {
//            boolean wasAnyRemoved = false;
//            for (Object o : c) {
//                wasAnyRemoved |= remove(o);
//            }
//            return wasAnyRemoved;
//        }
//
//        @Override
//        public boolean retainAll(Collection<?> c) {
//            //return list.retainAll(c);
//            throw new UnsupportedOperationException("retainAll does not support notifications");
//        }
//
//        @Override
//        public E set(int index, E element) {
//            E oldValue = list.set(index, element);
//            if (oldValue != element) {
//                listenerSupport.fireItemRemoved(oldValue);
//                listenerSupport.fireItemAdded(element);
//            }
//            return oldValue;
//        }
//
//        @Override
//        public int size() {
//            return list.size();
//        }
//
//        @Override
//        public List<E> subList(int fromIndex, int toIndex) {
//            return list.subList(fromIndex, toIndex);
//        }
//
//        @Override
//        public Object[] toArray() {
//            return list.toArray();
//        }
//
//        @Override
//        public <T> T[] toArray(T[] a) {
//            return list.toArray(a);
//        }
//
//        @Override
//        public String addItemListener(ItemListener<E> paramItemListener, boolean paramBoolean) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public void removeItemListener(String paramString) {
//            // TODO Auto-generated method stub
//        }
//
//        @Override
//        public String getPartitionKey() {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public String getServiceName() {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public void destroy() {
//            // TODO Auto-generated method stub
//            
//        }
//
//        @Override
//        public <E> IQueue<E> getQueue(String paramString) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public <E> ITopic<E> getTopic(String paramString) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public <E> ISet<E> getSet(String paramString) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public <E> IList<E> getList(String paramString) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public <K, V> IMap<K, V> getMap(String paramString) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public <K, V> ReplicatedMap<K, V> getReplicatedMap(String paramString) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public JobTracker getJobTracker(String paramString) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public <K, V> MultiMap<K, V> getMultiMap(String paramString) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public ILock getLock(String paramString) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public <E> Ringbuffer<E> getRingbuffer(String paramString) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public <E> ITopic<E> getReliableTopic(String paramString) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public Cluster getCluster() {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public Endpoint getLocalEndpoint() {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public IExecutorService getExecutorService(String paramString) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public <T> T executeTransaction(TransactionalTask<T> paramTransactionalTask) throws TransactionException {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public <T> T executeTransaction(TransactionOptions paramTransactionOptions,
//                                        TransactionalTask<T> paramTransactionalTask) throws TransactionException {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public TransactionContext newTransactionContext() {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public TransactionContext newTransactionContext(TransactionOptions paramTransactionOptions) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public IdGenerator getIdGenerator(String paramString) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public IAtomicLong getAtomicLong(String paramString) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public <E> IAtomicReference<E> getAtomicReference(String paramString) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public ICountDownLatch getCountDownLatch(String paramString) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public ISemaphore getSemaphore(String paramString) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public String addDistributedObjectListener(DistributedObjectListener paramDistributedObjectListener) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public boolean removeDistributedObjectListener(String paramString) {
//            // TODO Auto-generated method stub
//            return false;
//        }
//
//        @Override
//        public Config getConfig() {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public PartitionService getPartitionService() {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public QuorumService getQuorumService() {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public ClientService getClientService() {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public LoggingService getLoggingService() {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public LifecycleService getLifecycleService() {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public <T extends DistributedObject> T getDistributedObject(String paramString1, String paramString2) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public ConcurrentMap<String, Object> getUserContext() {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public HazelcastXAResource getXAResource() {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public void shutdown() {
//            // TODO Auto-generated method stub
//            
//        }
//
//        @Override
//        public Collection<DistributedObject> getDistributedObjects() {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//    }
//*/
//
// /*   
//    private class IQueueImpl<E> implements IQueue<E> {
//
//        private ArrayBlockingQueue<E> queue;
//        private static final long serialVersionUID = 1L;
//        private final String name;
//        private final int capacity;
//        private final ItemListenerSupport<E> itemListenerSupport;
//
//        public IQueueImpl(String name, int capacity) {
//            this.name = name;
//            this.capacity = capacity;
//            this.queue = new ArrayBlockingQueue<>(capacity);
//            this.itemListenerSupport = new ItemListenerSupport<>();
//        }
//
//        @Override
//        public String addItemListener(ItemListener<E> listener, boolean includeValue) {
//            this.itemListenerSupport.addItemListener(listener, includeValue);
//            return name;
//        }
//
//        @Override
//        public String getName() {
//            return this.name;
//        }
//
//        @Override
//        public boolean removeItemListener(String name) {
//            this.itemListenerSupport.removeItemListener(name);
//        }
//
//        @Override
//        public void destroy() {
//            queue.clear();
//        }
//
//        @Override
//        public boolean add(E e) {
//            boolean added = queue.add(e);
//            itemListenerSupport.fireItemAdded(e);
//            return added;
//        }
//
//        @Override
//        public void clear() {
//            ArrayBlockingQueue<E> oldQueue = queue;
//            queue = new ArrayBlockingQueue<>(capacity);
//            itemListenerSupport.fireItemRemoved(oldQueue.iterator());
//            oldQueue.clear();
//        }
//
//        @Override
//        public int drainTo(Collection<? super E> c, int maxElements) {
//            int drained = queue.drainTo(c, maxElements);
//            itemListenerSupport.fireItemRemoved(c.iterator());
//            return drained;
//        }
//
//        @Override
//        public int drainTo(Collection<? super E> c) {
//            int drained = queue.drainTo(c);
//            itemListenerSupport.fireItemRemoved(c.iterator());
//            return drained;
//        }
//
//        @Override
//        public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
//            boolean offered = queue.offer(e, timeout, unit);
//            itemListenerSupport.fireItemAdded(e);
//            return offered;
//        }
//
//        @Override
//        public boolean offer(E e) {
//            boolean offered = queue.offer(e);
//            itemListenerSupport.fireItemAdded(e);
//            return offered;
//        }
//
//        @Override
//        public E poll() {
//            E item = queue.poll();
//            itemListenerSupport.fireItemRemoved(item);
//            return item;
//        }
//
//        @Override
//        public E poll(long timeout, TimeUnit unit) throws InterruptedException {
//            E item = queue.poll(timeout, unit);
//            itemListenerSupport.fireItemRemoved(item);
//            return item;
//        }
//
//        @Override
//        public void put(E e) throws InterruptedException {
//            queue.put(e);
//            itemListenerSupport.fireItemAdded(e);
//        }
//
//        @Override
//        public boolean remove(Object o) {
//            boolean removed = queue.remove(o);
//            itemListenerSupport.fireItemRemoved(o);
//            return removed;
//        }
//
//        @Override
//        public E take() throws InterruptedException {
//            E item = queue.take();
//            itemListenerSupport.fireItemRemoved(item);
//            return item;
//        }
//
//        @Override
//        public boolean addAll(Collection<? extends E> c) {
//            boolean allAdded = queue.addAll(c);
//            itemListenerSupport.fireItemAdded(c.iterator());
//            return allAdded;
//        }
//
//        @Override
//        public E remove() {
//            E item = queue.remove();
//            itemListenerSupport.fireItemRemoved(item);
//            return item;
//        }
//
//        @Override
//        public boolean removeAll(Collection<?> c) {
//            boolean allRemoved = queue.removeAll(c);
//            itemListenerSupport.fireItemRemoved(c.iterator());
//            return allRemoved;
//        }
//
//        @Override
//        public boolean contains(Object o) {
//            return queue.contains(o);
//        }
//
//        @Override
//        public int remainingCapacity() {
//            return queue.remainingCapacity();
//        }
//
//        @Override
//        public E element() {
//            return queue.element();
//        }
//
//        @Override
//        public E peek() {
//            return queue.peek();
//        }
//
//        @Override
//        public boolean containsAll(Collection<?> c) {
//            return queue.containsAll(c);
//        }
//
//        @Override
//        public boolean isEmpty() {
//            return queue.isEmpty();
//        }
//
//        @Override
//        public Iterator<E> iterator() {
//            return queue.iterator();
//        }
//
//        @Override
//        public boolean retainAll(Collection<?> c) {
//            return queue.retainAll(c);
//        }
//
//        @Override
//        public int size() {
//            return queue.size();
//        }
//
//        @Override
//        public Object[] toArray() {
//            return queue.toArray();
//        }
//
//        @Override
//        public <T> T[] toArray(T[] a) {
//            return queue.toArray(a);
//        }
//
//        @Override
//        public LocalQueueStats getLocalQueueStats() {
//            throw new UnsupportedOperationException("getLocalQueueStats");
//        }
//
//        @Override
//        public String getPartitionKey() {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public String getServiceName() {
//            // TODO Auto-generated method stub
//            return null;
//        }
//    }
//
//   
//    private class ItemListenerSupport<E> {
//
//        private ConcurrentHashMap<ItemListener<E>, Boolean> listenersMap = new ConcurrentHashMap<>();
//
//        public void addItemListener(ItemListener<E> listener, boolean includeValue) {
//            listenersMap.put(listener, includeValue);
//        }
//
//        public void removeItemListener(ItemListener<E> listener) {
//            listenersMap.remove(listener);
//        }
//
//        public void fireItemAdded(Iterator<? extends E> iterator) {
//            while (iterator.hasNext()) {
//                fireItemAdded(iterator.next());
//            }
//        }
//
//        public void fireItemAdded(E item) {
//            for (ItemListener<E> listener : listenersMap.keySet()) {
//               listener.itemAdded(listenersMap.get(listener) ? item : null);
//            }
//        }
//
//        public void fireItemRemoved(Iterator<? super E> iterator) {
//            while (iterator.hasNext()) {
//                fireItemRemoved(iterator.next());
//            }
//        }
//
//        @SuppressWarnings("unchecked")
//        public void fireItemRemoved(Object item) {
//            for (ItemListener<E> listener : listenersMap.keySet()) {
//                listener.itemRemoved(listenersMap.get(listener) ? (E)item : null);
//            }
//        }
//
//    }
//*/ 
}
