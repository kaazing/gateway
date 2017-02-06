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

import org.kaazing.gateway.service.collections.CollectionsFactory;

import com.hazelcast.core.ITopic;

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

    // event listener
    void addMembershipEventListener(MembershipEventListener eventListener);
    void removeMembershipEventListener(MembershipEventListener eventListener);

    CollectionsFactory getCollectionsFactory();

    void logClusterState();
    void logClusterStateAtInfoLevel();

    <E> ITopic<E> getTopic(String name);
}
