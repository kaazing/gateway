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
package org.kaazing.gateway.server.collections;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IAtomicLong;
import com.hazelcast.core.IList;
import com.hazelcast.core.ILock;
import com.hazelcast.core.IMap;
import com.hazelcast.core.IQueue;
import com.hazelcast.core.ITopic;

import org.kaazing.gateway.service.collections.CollectionsFactory;
import org.kaazing.gateway.util.AtomicCounter;

public class ClusterCollectionsFactory implements CollectionsFactory {


    private HazelcastInstance cluster;

    public ClusterCollectionsFactory(HazelcastInstance cluster) {
        this.cluster = cluster;
    }

    @Override
    public <E> IList<E> getList(String name) {
        return cluster.getList(name);
    }

    @Override
    public <K, V> IMap<K, V> getMap(String name) {
        return cluster.getMap(name);
    }

    @Override
    public <E> IQueue<E> getQueue(String name) {
        return cluster.getQueue(name);
    }

    @Override
    public <E> ITopic<E> getTopic(String name) {
        return cluster.getTopic(name);
    }

    @Override
    public ILock getLock(String name) {
        return cluster.getLock(name);
    }

    @Override
    public AtomicCounter getAtomicCounter(String name) {
        return new ClusterAtomicCounter(cluster.getAtomicLong(name));
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
