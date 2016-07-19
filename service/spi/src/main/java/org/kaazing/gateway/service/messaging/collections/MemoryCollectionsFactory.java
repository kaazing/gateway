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
import org.kaazing.gateway.util.GL;

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

public class MemoryCollectionsFactory implements CollectionsFactory {
    private static HazelcastInstance hazelcastInstance ;

    static {
        Config config = new Config("StandAlone");
        config.setProperty("hazelcast.phone.home.enabled", "false");
        config.setProperty("hazelcast.io.thread.count", "1");
        config.getNetworkConfig().setPortAutoIncrement(false);
        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
        if (!GL.isTraceEnabled(GL.CLUSTER_LOGGER_NAME)) {
            java.util.logging.Logger logger = java.util.logging.Logger.getLogger("com.hazelcast");
            logger.setLevel(Level.OFF);
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

}
