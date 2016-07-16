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
package org.kaazing.gateway.service.cluster;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.locks.Lock;

import com.hazelcast.core.IdGenerator;
import org.kaazing.gateway.service.messaging.buffer.MessageBufferFactory;
import org.kaazing.gateway.service.messaging.collections.CollectionsFactory;

public interface ClusterContext {

    // initialization
    void start();
    
    // for gateway shutdown
    void dispose();
    
    // cluster participation
    MemberId getLocalMember();
    String getInstanceKey(MemberId memberId);
    
    String getClusterName();

    // Return a list of the memberIds of all current cluster members
    Collection<MemberId> getMemberIds();

    List<MemberId> getAccepts();
    List<MemberId> getConnects();
    ClusterConnectOptionsContext getConnectOptions();

    // cluster collections
    Lock getLock(String name);
    IdGenerator getIdGenerator(String name);

    // cluster messaging
    void addReceiveTopic(String name);
    void addReceiveQueue(String name);
    <T> T send(Object msg, MemberId member) throws Exception;
    <T> T send(Object msg, String name) throws Exception;
    void send(Object msg, final SendListener listener, MemberId member);
    void send(Object msg, final SendListener listener, String name);
    <T> void setReceiver(Class<T> type, ReceiveListener<T> receiveListener);
    <T> void removeReceiver(Class<T> type);

    // event listener
    void addMembershipEventListener(MembershipEventListener eventListener);
    void removeMembershipEventListener(MembershipEventListener eventListener);

    // instanceKey listener
    void addInstanceKeyListener(InstanceKeyListener instanceKeyListener);
    void removeInstanceKeyListener(InstanceKeyListener instanceKeyListener);

    // balancermap listener
    void addBalancerMapListener(BalancerMapListener balancerMapListener);
    void removeBalancerMapListener(BalancerMapListener balancerMapListener);

    MessageBufferFactory getMessageBufferFactory();
    CollectionsFactory getCollectionsFactory();

    void logClusterState();
    void logClusterStateAtInfoLevel();
}
