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
package org.kaazing.gateway.server.context.resolve;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.kaazing.gateway.service.cluster.BalancerMapListener;
import org.kaazing.gateway.service.cluster.ClusterConnectOptionsContext;
import org.kaazing.gateway.service.cluster.ClusterContext;
import org.kaazing.gateway.service.cluster.InstanceKeyListener;
import org.kaazing.gateway.service.cluster.MemberId;
import org.kaazing.gateway.service.cluster.MembershipEventListener;
import org.kaazing.gateway.service.messaging.collections.CollectionsFactory;
import org.kaazing.gateway.service.messaging.collections.MemoryCollectionsFactory;
import org.kaazing.gateway.util.Utils;

import com.hazelcast.core.IdGenerator;

/**
 * This class is the standalone case where the current node is itself the master this does not have any high availability
 * functionality. The only supported method for now is the Map.
 */
public class StandaloneClusterContext implements ClusterContext {

    private final CollectionsFactory collectionsFactory;
    private final ConcurrentMap<Object, Lock> locks;
    private final ConcurrentMap<String, IdGeneratorImpl> idGenerators;
    private final String localInstanceKey = Utils.randomHexString(16);

    public StandaloneClusterContext() {
        this.collectionsFactory = new MemoryCollectionsFactory();
        this.locks = new ConcurrentHashMap<>();
        this.idGenerators = new ConcurrentHashMap<>();
    }

    @Override
    public void addMembershipEventListener(MembershipEventListener eventListener) {
        // this is a no-op
    }

    @Override
    public void removeMembershipEventListener(MembershipEventListener eventListener) {
        // this is a no-op
    }

    @Override
    public void addInstanceKeyListener(InstanceKeyListener instanceKeyListener) {
        // this is a no-op
    }

    @Override
    public void removeInstanceKeyListener(InstanceKeyListener instanceKeyListener) {
        // this is a no-op
    }

    @Override
    public void addBalancerMapListener(BalancerMapListener balancerMapListener) {
        // this is a no-op
    }

    @Override
    public void removeBalancerMapListener(BalancerMapListener balancerMapListener) {
        // this is a no-op
    }

    @Override
    public String getClusterName() {
        return getLocalMember().toString();
    }

    @Override
    public List<MemberId> getMemberIds() {
        List<MemberId> list = new ArrayList<>();
        list.add(getLocalMember());
        return list;
    }

    @Override
    public List<MemberId> getAccepts() {
        return null;
    }

    @Override
    public List<MemberId> getConnects() {
        return null;
    }

    @Override
    public ClusterConnectOptionsContext getConnectOptions() {
        return null;
    }

    @Override
    public IdGenerator getIdGenerator(String name) {
        IdGeneratorImpl impl = idGenerators.get(name);
        if (impl == null) {
            IdGeneratorImpl newImpl = new IdGeneratorImpl(name);
            impl = idGenerators.putIfAbsent(name, newImpl);
            if (impl == null) {
                impl = newImpl;
            }
        }

        return impl;
    }

    @Override
    public MemberId getLocalMember() {
        return new MemberId("tcp", "standalone", 0);
    }

    @Override
    public String getInstanceKey(MemberId memberId) {
        // There's only 1 member for a standalone cluster, so return the local ID
        return localInstanceKey;
    }

    @Override
    public Lock getLock(Object obj) {
        Lock lock = locks.get(obj);
        if (lock == null) {
            lock = new ReentrantLock();
            Lock oldLock = locks.putIfAbsent(obj, lock);
            if (oldLock != null) {
                lock = oldLock;
            }
        }
        return lock;
    }

    @Override
    public void start() {
    }

    @Override
    public void dispose() {
    }

    @Override
    public CollectionsFactory getCollectionsFactory() {
        return collectionsFactory;
    }

    @Override
    public void logClusterState() {
        // no cluster state to log for standalone
    }

    @Override
    public void logClusterStateAtInfoLevel() {
        // no cluster state to log for standalone
    }

    private class IdGeneratorImpl implements IdGenerator {
        private final AtomicLong currentId;
        private final String name;

        public IdGeneratorImpl(String name) {
            this.name = name;

            // We start the current ID at the smallest possible long value,
            // so that we have the entire range of values to use.
            this.currentId = new AtomicLong(Long.MIN_VALUE);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public long newId() {
            return currentId.incrementAndGet();
        }

        @Override
        public void destroy() {
            currentId.set(Long.MIN_VALUE);
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
        public boolean init(long paramLong) {
            // TODO Auto-generated method stub
            return false;
        }
    }
}
