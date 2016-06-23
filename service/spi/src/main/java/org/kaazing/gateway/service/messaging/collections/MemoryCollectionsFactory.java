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

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.hazelcast.core.EntryListener;
import com.hazelcast.core.EntryView;
import com.hazelcast.core.ExecutionCallback;
import com.hazelcast.core.IMap;
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

    private final ConcurrentMap<String, IMapImpl<?, ?>> maps;

    public MemoryCollectionsFactory() {
         maps = new ConcurrentHashMap<>();
    }

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
    public <K, V> void addEntryListener(MapListener listener, String name) {
        throw new UnsupportedOperationException("addEntryListener");
        
    }

    private class MapEntryViewImpl<K, V> implements EntryView<K, V> {
        private final K key;
        private final V value;

        public MapEntryViewImpl(K key, V value) {
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
        public long getHits() {
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
        public long getTtl() {
            return 0;
        }

        @Override
        public long getVersion() {
            return 1;
        }

       @Override
        public boolean equals(Object o) {
            if (o == null ||
                    !(o instanceof EntryView)) {
                return false;
            }

            EntryView that = (EntryView) o;

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
        private final ConcurrentMap<Object, Lock> locks;
        private final String name;

        public IMapImpl(String name) {
            this.name = name;
            this.map = new ConcurrentHashMap<>();
            this.locks = new ConcurrentHashMap<>();
        }

        @Override
        public EntryView<K, V> getEntryView(K key) {
            V value = map.get(key);
            return new MapEntryViewImpl<>(key, value);
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
            return oldValue;
        }

        @SuppressWarnings("unchecked")
		@Override
        public boolean remove(Object key, Object value) {
            boolean wasRemoved = map.remove(key, value);
            return wasRemoved;
        }

        @Override
        public V replace(K key, V value) {
            V oldValue = map.replace(key, value);
            return oldValue;
        }

        @Override
        public boolean replace(K key, V oldValue, V newValue) {
            boolean wasReplaced = map.replace(key, oldValue, newValue);
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
               // EntryView me = getEntryView(entry.getKey());
                if (predicate.apply(entry)) {
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
            return removed;
        }

        @Override
        public LocalMapStats getLocalMapStats() {
            throw new UnsupportedOperationException("getLocalMapStats");
        }

        @Override
        public Set<K> keySet(Predicate predicate) {
            Set<K> keySet = new LinkedHashSet<>();
            for (Map.Entry<K, V> entry : map.entrySet()) {
               // EntryView me = getEntryView(entry.getKey());
                if (predicate.apply(entry)) {
                    keySet.add(entry.getKey());
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
        public Collection<V> values(Predicate predicate) {
            Set<V> values = new LinkedHashSet<>();
            for (Map.Entry<K, V> entry : map.entrySet()) {
                EntryView me = getEntryView(entry.getKey());
                if (predicate.apply(entry)) {
                    values.add(entry.getValue());
                }
            }

            return values;
        }

        @SuppressWarnings("unchecked")
        @Override
        public V remove(Object key) {
            V value = map.remove(key);
            boolean removed = (value != null);
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
        public void flush() {
            throw new UnsupportedOperationException("flush");
        }

        @Override
        public Map<K, V> getAll(Set<K> arg0) {
            throw new UnsupportedOperationException("getAll");
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
        public boolean tryRemove(K arg0, long arg1, TimeUnit arg2) {
            throw new UnsupportedOperationException("tryRemove");
        }

        @Override
        public String getPartitionKey() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getServiceName() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void delete(Object paramObject) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void loadAll(boolean paramBoolean) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void loadAll(Set<K> paramSet, boolean paramBoolean) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Future<V> putAsync(K paramK, V paramV, long paramLong, TimeUnit paramTimeUnit) {
            throw new UnsupportedOperationException();
       }

        @Override
        public void set(K paramK, V paramV) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void set(K paramK, V paramV, long paramLong, TimeUnit paramTimeUnit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void lock(K paramK, long paramLong, TimeUnit paramTimeUnit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isLocked(K paramK) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean tryLock(K paramK,
                               long paramLong1,
                               TimeUnit paramTimeUnit1,
                               long paramLong2,
                               TimeUnit paramTimeUnit2) throws InterruptedException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void forceUnlock(K paramK) {
            throw new UnsupportedOperationException();
            
        }

        @Override
        public String addLocalEntryListener(MapListener paramMapListener) {
            throw new UnsupportedOperationException();
         }

        @Override
        public String addLocalEntryListener(EntryListener paramEntryListener) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String addLocalEntryListener(MapListener paramMapListener,
                                            Predicate<K, V> paramPredicate,
                                            boolean paramBoolean) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String addLocalEntryListener(EntryListener paramEntryListener,
                                            Predicate<K, V> paramPredicate,
                                            boolean paramBoolean) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String addLocalEntryListener(MapListener paramMapListener,
                                            Predicate<K, V> paramPredicate,
                                            K paramK,
                                            boolean paramBoolean) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String addLocalEntryListener(EntryListener paramEntryListener,
                                            Predicate<K, V> paramPredicate,
                                            K paramK,
                                            boolean paramBoolean) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String addInterceptor(MapInterceptor paramMapInterceptor) {
            throw new UnsupportedOperationException();
       }

        @Override
        public void removeInterceptor(String paramString) {
            throw new UnsupportedOperationException();
       }

        @Override
        public String addEntryListener(EntryListener paramEntryListener, boolean paramBoolean) {
            throw new UnsupportedOperationException();
         }

        @Override
        public String addPartitionLostListener(MapPartitionLostListener paramMapPartitionLostListener) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removePartitionLostListener(String paramString) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void evictAll() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object executeOnKey(K paramK, EntryProcessor paramEntryProcessor) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Map<K, Object> executeOnKeys(Set<K> paramSet, EntryProcessor paramEntryProcessor) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void submitToKey(K paramK, EntryProcessor paramEntryProcessor, ExecutionCallback paramExecutionCallback) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Future submitToKey(K paramK, EntryProcessor paramEntryProcessor) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Map<K, Object> executeOnEntries(EntryProcessor paramEntryProcessor) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Map<K, Object> executeOnEntries(EntryProcessor paramEntryProcessor, Predicate paramPredicate) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <SuppliedValue, Result> Result aggregate(Supplier<K, V, SuppliedValue> paramSupplier,
                                                        Aggregation<K, SuppliedValue, Result> paramAggregation) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <SuppliedValue, Result> Result aggregate(Supplier<K, V, SuppliedValue> paramSupplier,
                                                        Aggregation<K, SuppliedValue, Result> paramAggregation,
                                                        JobTracker paramJobTracker) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String addEntryListener(MapListener arg0, boolean arg1) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String addEntryListener(MapListener arg0, K arg1, boolean arg2) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String addEntryListener(EntryListener arg0, K arg1, boolean arg2) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String addEntryListener(MapListener arg0, Predicate<K, V> arg1, boolean arg2) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String addEntryListener(EntryListener arg0, Predicate<K, V> arg1, boolean arg2) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String addEntryListener(MapListener arg0, Predicate<K, V> arg1, K arg2, boolean arg3) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String addEntryListener(EntryListener arg0, Predicate<K, V> arg1, K arg2, boolean arg3) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeEntryListener(String arg0) {
            throw new UnsupportedOperationException();
        }
    }

}
