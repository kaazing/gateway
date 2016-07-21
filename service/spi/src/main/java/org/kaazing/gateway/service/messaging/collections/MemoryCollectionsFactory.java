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

import java.awt.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.kaazing.gateway.util.AtomicCounter;
import org.kaazing.gateway.util.GL;

import com.hazelcast.config.Config;
import com.hazelcast.config.InterfacesConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.EntryView;
import com.hazelcast.core.ExecutionCallback;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IAtomicLong;
import com.hazelcast.core.IList;
import com.hazelcast.core.ILock;
import com.hazelcast.core.IMap;
import com.hazelcast.core.IQueue;
import com.hazelcast.core.ITopic;
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
    private static HazelcastInstance hazelcastInstance ;

    static {
        Config config = new Config("StandAlone");
        config.setProperty("hazelcast.phone.home.enabled", "false");
        config.setProperty("hazelcast.io.thread.count", "1");
        config.setProperty("hazelcast.socket.bind.any", "false");
        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger("com.hazelcast");
        if (GL.isTraceEnabled(GL.CLUSTER_LOGGER_NAME)) {
            logger.setLevel(Level.FINEST);
        }else {
           // logger.setLevel(Level.OFF);
        }
        hazelcastInstance = Hazelcast.newHazelcastInstance(config);

    }
    
   
    public MemoryCollectionsFactory() {
    }
    
    public static HazelcastInstance getHazelcastInstance() {
        return hazelcastInstance;
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

     class imapImpl implements IMap{

        @Override
        public boolean isEmpty() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public int size() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public void destroy() {
            // TODO Auto-generated method stub
            
        }

        @Override
        public String getName() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getPartitionKey() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getServiceName() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void putAll(Map paramMap) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public boolean containsKey(Object paramObject) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean containsValue(Object paramObject) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public Object get(Object paramObject) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Object put(Object paramK, Object paramV) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Object remove(Object paramObject) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public boolean remove(Object paramObject1, Object paramObject2) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public void delete(Object paramObject) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void flush() {
            // TODO Auto-generated method stub
            
        }

        @Override
        public Map getAll(Set paramSet) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void loadAll(boolean paramBoolean) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void loadAll(Set paramSet, boolean paramBoolean) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void clear() {
            // TODO Auto-generated method stub
            
        }

        @Override
        public Future getAsync(Object paramK) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Future putAsync(Object paramK, Object paramV) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Future putAsync(Object paramK, Object paramV, long paramLong, TimeUnit paramTimeUnit) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Future removeAsync(Object paramK) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public boolean tryRemove(Object paramK, long paramLong, TimeUnit paramTimeUnit) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean tryPut(Object paramK, Object paramV, long paramLong, TimeUnit paramTimeUnit) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public Object put(Object paramK, Object paramV, long paramLong, TimeUnit paramTimeUnit) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void putTransient(Object paramK, Object paramV, long paramLong, TimeUnit paramTimeUnit) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public Object putIfAbsent(Object paramK, Object paramV) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Object putIfAbsent(Object paramK, Object paramV, long paramLong, TimeUnit paramTimeUnit) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public boolean replace(Object paramK, Object paramV1, Object paramV2) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public Object replace(Object paramK, Object paramV) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void set(Object paramK, Object paramV) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void set(Object paramK, Object paramV, long paramLong, TimeUnit paramTimeUnit) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void lock(Object paramK) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void lock(Object paramK, long paramLong, TimeUnit paramTimeUnit) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public boolean isLocked(Object paramK) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean tryLock(Object paramK) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean tryLock(Object paramK, long paramLong, TimeUnit paramTimeUnit) throws InterruptedException {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean tryLock(Object paramK,
                               long paramLong1,
                               TimeUnit paramTimeUnit1,
                               long paramLong2,
                               TimeUnit paramTimeUnit2) throws InterruptedException {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public void unlock(Object paramK) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void forceUnlock(Object paramK) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public String addLocalEntryListener(MapListener paramMapListener) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String addLocalEntryListener(EntryListener paramEntryListener) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String addLocalEntryListener(MapListener paramMapListener, Predicate paramPredicate, boolean paramBoolean) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String addLocalEntryListener(EntryListener paramEntryListener,
                                            Predicate paramPredicate,
                                            boolean paramBoolean) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String addLocalEntryListener(MapListener paramMapListener,
                                            Predicate paramPredicate,
                                            Object paramK,
                                            boolean paramBoolean) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String addLocalEntryListener(EntryListener paramEntryListener,
                                            Predicate paramPredicate,
                                            Object paramK,
                                            boolean paramBoolean) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String addInterceptor(MapInterceptor paramMapInterceptor) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void removeInterceptor(String paramString) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public String addEntryListener(MapListener paramMapListener, boolean paramBoolean) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String addEntryListener(EntryListener paramEntryListener, boolean paramBoolean) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public boolean removeEntryListener(String paramString) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public String addPartitionLostListener(MapPartitionLostListener paramMapPartitionLostListener) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public boolean removePartitionLostListener(String paramString) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public String addEntryListener(MapListener paramMapListener, Object paramK, boolean paramBoolean) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String addEntryListener(EntryListener paramEntryListener, Object paramK, boolean paramBoolean) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String addEntryListener(MapListener paramMapListener, Predicate paramPredicate, boolean paramBoolean) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String addEntryListener(EntryListener paramEntryListener, Predicate paramPredicate, boolean paramBoolean) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String addEntryListener(MapListener paramMapListener,
                                       Predicate paramPredicate,
                                       Object paramK,
                                       boolean paramBoolean) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String addEntryListener(EntryListener paramEntryListener,
                                       Predicate paramPredicate,
                                       Object paramK,
                                       boolean paramBoolean) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public EntryView getEntryView(Object paramK) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public boolean evict(Object paramK) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public void evictAll() {
            // TODO Auto-generated method stub
            
        }

        @Override
        public Set keySet() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Collection values() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Set entrySet() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Set keySet(Predicate paramPredicate) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Set entrySet(Predicate paramPredicate) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Collection values(Predicate paramPredicate) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Set localKeySet() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Set localKeySet(Predicate paramPredicate) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void addIndex(String paramString, boolean paramBoolean) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public LocalMapStats getLocalMapStats() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Object executeOnKey(Object paramK, EntryProcessor paramEntryProcessor) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Map executeOnKeys(Set paramSet, EntryProcessor paramEntryProcessor) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void submitToKey(Object paramK,
                                EntryProcessor paramEntryProcessor,
                                ExecutionCallback paramExecutionCallback) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public Future submitToKey(Object paramK, EntryProcessor paramEntryProcessor) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Map executeOnEntries(EntryProcessor paramEntryProcessor) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Map executeOnEntries(EntryProcessor paramEntryProcessor, Predicate paramPredicate) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Object aggregate(Supplier paramSupplier, Aggregation paramAggregation) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Object aggregate(Supplier paramSupplier, Aggregation paramAggregation, JobTracker paramJobTracker) {
            // TODO Auto-generated method stub
            return null;
        }
         
     }
}
