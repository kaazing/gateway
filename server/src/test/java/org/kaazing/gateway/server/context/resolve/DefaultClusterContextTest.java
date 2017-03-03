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

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.kaazing.gateway.service.cluster.ClusterContext;
import org.kaazing.gateway.service.cluster.MemberId;
import org.kaazing.gateway.service.cluster.MembershipEventListener;
import org.kaazing.gateway.service.collections.CollectionsFactory;
import org.kaazing.test.util.ITUtil;

public class DefaultClusterContextTest {

    public static final String BALANCER_MAP_NAME = "balancerMap";
    public static final String MEMBERID_BALANCER_MAP_NAME = "memberIdBalancerMap";

    DefaultClusterContext clusterContext1;
    DefaultClusterContext clusterContext2;

    ClusterMemberTracker memberTracker1;
    ClusterMemberTracker memberTracker2;

    @Rule
    public RuleChain chain = ITUtil.createRuleChain(60, TimeUnit.SECONDS);

    private class ClusterMemberTracker implements MembershipEventListener {

        public ClusterMemberTracker() {
            this(1, 0);
        }

        public ClusterMemberTracker(int expectedAddCount,
                                    int expectedRemoveCount) {

            membersAdded = new CountDownLatch(expectedAddCount);
            membersRemoved = new CountDownLatch(expectedRemoveCount);

        }

        CountDownLatch membersAdded;
        CountDownLatch membersRemoved;

        List<MemberId> members = new ArrayList<>();

        @Override
        public void memberAdded(MemberId newMember) {
            members.add(newMember);
            membersAdded.countDown();
        }

        @Override
        public void memberRemoved(MemberId removedMember) {
            members.remove(removedMember);
            membersRemoved.countDown();
        }

        int size() {
            return members.size();
        }

        void clear() {
            members.clear();
        }


        public void assertMemberAdded(MemberId memberId) throws InterruptedException {
            if (!membersAdded.await(3, TimeUnit.SECONDS)) {
                fail("Failed to detect " + memberId + " being added as expected.");
            }

            for (MemberId id : members) {
                if (id.equals(memberId)) {
                    return;
                }
            }

            fail("Failed to detect " + memberId +
                    " being added as expected although expected number of members being added occurred.");
        }

        public void assertMemberNotAdded(MemberId memberId) throws InterruptedException {
            if (membersAdded.await(3, TimeUnit.SECONDS)) {
                fail("Detected " + memberId + " being added when this was not expected.");
            }
        }
    }

    @Before
    public void setUp() throws Exception {
        memberTracker1 = new ClusterMemberTracker();
        memberTracker2 = new ClusterMemberTracker();
    }

    @After
    public void tearDown() throws Exception {
        memberTracker1.clear();
        memberTracker2.clear();
    }

    @Test
    public void shouldSeeLocalNodeDataAfterAClusterIsStarted() throws Exception {
        try {
            MemberId member1 = new MemberId("tcp", "127.0.0.1", 46943);
            MemberId member2 = new MemberId("tcp", "127.0.0.1", 46942);

            List<MemberId> accepts = Collections.singletonList(member1);
            List<MemberId> connects = Collections.singletonList(member2);
            final String clusterName = this.getClass().getName() + "-cluster1";
            clusterContext1 = new DefaultClusterContext(clusterName,
                    accepts,
                    connects,
                    null);

            clusterContext2 = new DefaultClusterContext(clusterName,
                    connects,
                    accepts,
                    null);

            clusterContext1.addMembershipEventListener(memberTracker1);

            clusterContext1.start();

            assertEquals("Expected correct cluster name.",
                    clusterName,
                    clusterContext1.getClusterName());

            assertEquals("Expected local member host to be same as used to construct the cluster",
                    member1.getHost(),
                    clusterContext1.getLocalMember().getHost());

            assertEquals("Expected local member port to be same as used to construct the cluster",
                    member1.getPort(),
                    clusterContext1.getLocalMember().getPort());
        } finally {
            clusterContext1.dispose();
        }
    }

    @Test
    public void shouldNotListenOnMulticastInterfaceImplicitly() throws Exception {
        try {
            MemberId acceptMember1 = new MemberId("tcp", "127.0.0.1", 46942);
            MemberId connectMember1 = new MemberId("tcp", "127.0.0.1", 1324);

            MemberId acceptMember2 = new MemberId("tcp", "127.0.0.1", 1324);
            MemberId connectMember2 = new MemberId("udp", "224.2.2.3", 54327);

            List<MemberId> acceptsMember1 = Collections.singletonList(acceptMember1);
            List<MemberId> connectsMember1 = Collections.singletonList(connectMember1);
            final String clusterName = this.getClass().getName();
            clusterContext1 = new DefaultClusterContext(clusterName,
                    acceptsMember1,
                    connectsMember1,
                    null);

            List<MemberId> acceptsMember2 = Collections.singletonList(acceptMember2);
            List<MemberId> connectsMember2 = Collections.singletonList(connectMember2);
            clusterContext2 = new DefaultClusterContext(clusterName,
                    acceptsMember2,
                    connectsMember2,
                    null);

            clusterContext1.addMembershipEventListener(memberTracker1);
            clusterContext2.addMembershipEventListener(memberTracker2);

            // If the fix is not in place the second cluster usually locks up, hence the methods here.
            startClusterContext(clusterContext1);
            startClusterContext(clusterContext2);

            memberTracker1.assertMemberNotAdded(acceptMember2);

        } catch (Exception e) {
            // This is an ugly hack, necessitated by Bamboo running in EC2
            // (which requires a very different cluster configuration) vs
            // developers running these tests on our local machines (which
            // explicitly do NOT want to configured for clustering in EC2).

            String message;

            if (e instanceof ExecutionException) {
                message = e.getCause().getMessage();

            } else {
                message = e.getMessage();
            }

            // This exception happens if this test is run while in
            // EC2.  As such, we expect this test to fail, so ignore
            // the exception.
            if (!message.contains("not supported on AWS")) {
                System.out.println("expected on Travis build" +  e.getMessage());
                Assume.assumeTrue(false);
            }

        } finally {
            try {
                clusterContext1.dispose();
            } finally {
                clusterContext2.dispose();
            }

        }
    }

    @Test
    public void shouldSeeBalancerStateAfterClusterMembersLeave() throws Exception {
        DefaultClusterContext clusterContext1 = null;
        DefaultClusterContext clusterContext2 = null;
        DefaultClusterContext clusterContext3 = null;
        DefaultClusterContext clusterContext4 = null;
        try {
            MemberId member1 = new MemberId("tcp", "127.0.0.1", 46941);
            MemberId member2 = new MemberId("tcp", "127.0.0.1", 46942);
            MemberId member3 = new MemberId("tcp", "127.0.0.1", 46943);
            MemberId member4 = new MemberId("tcp", "127.0.0.1", 46944);

            List<MemberId> accepts1 = Collections.singletonList(member1);
            List<MemberId> connects1 = Arrays.asList(member2, member3, member4);
            List<MemberId> accepts2 = Collections.singletonList(member2);
            List<MemberId> connects2 = Arrays.asList(member1, member3, member4);
            List<MemberId> accepts3 = Collections.singletonList(member3);
            List<MemberId> connects3 = Arrays.asList(member1, member2, member4);
            List<MemberId> accepts4 = Collections.singletonList(member4);
            List<MemberId> connects4 = Arrays.asList(member1, member2, member3);
            final String clusterName = this.getClass().getName() + "-cluster1";

            clusterContext1 = new DefaultClusterContext(clusterName,
                    accepts1,
                    connects1,
                    null);

            clusterContext2 = new DefaultClusterContext(clusterName,
                    accepts2,
                    connects2,
                    null);

            clusterContext3 = new DefaultClusterContext(clusterName,
                    accepts3,
                    connects3,
                    null);

            clusterContext4 = new DefaultClusterContext(clusterName,
                    accepts4,
                    connects4,
                    null);

            Map<ClusterContext, String> clusterContextToServiceUrlMap = new HashMap<>();
            initClusterContext(clusterContext1, clusterContextToServiceUrlMap, 1);

            try {
                // KG-10802:  sleep for 2 seconds to ensure cluster member 1 finishes starting up and elects itself
                //            the master node of the cluster which will allow the cluster to form as expected.
                Thread.sleep(2000);
            } catch (Exception ex) {
            }

            initClusterContext(clusterContext2, clusterContextToServiceUrlMap, 2);
            initClusterContext(clusterContext3, clusterContextToServiceUrlMap, 3);
            initClusterContext(clusterContext4, clusterContextToServiceUrlMap, 4);

            // validate cluster state
            validateClusterState(Arrays.asList(clusterContext1, clusterContext2, clusterContext3, clusterContext4), clusterContextToServiceUrlMap);

            // 2 - shut down context 1 & 2
            clusterContext1.dispose();
            clusterContext2.dispose();
            clusterContextToServiceUrlMap.remove(clusterContext1);
            clusterContextToServiceUrlMap.remove(clusterContext2);

            // 3 - validate cluster state
            validateClusterState(Arrays.asList(clusterContext3, clusterContext4), clusterContextToServiceUrlMap);

            // 4 - start context 1 & 2
            clusterContext1 = new DefaultClusterContext(clusterName,
                    accepts1,
                    connects1,
                    null);

            clusterContext2 = new DefaultClusterContext(clusterName,
                    accepts2,
                    connects2,
                    null);

            initClusterContext(clusterContext1, clusterContextToServiceUrlMap, 1);
            initClusterContext(clusterContext2, clusterContextToServiceUrlMap, 2);

            // 5 - validate cluster state
            validateClusterState(Arrays.asList(clusterContext1, clusterContext2, clusterContext3, clusterContext4), clusterContextToServiceUrlMap);

        } finally {
            if (clusterContext1 != null) {
                clusterContext1.dispose();
            }
            if (clusterContext2 != null) {
                clusterContext2.dispose();
            }
            if (clusterContext3 != null) {
                clusterContext3.dispose();
            }
            if (clusterContext4 != null) {
                clusterContext4.dispose();
            }
        }
    }

    private void initClusterContext(DefaultClusterContext clusterContext,
        Map<ClusterContext, String> clusterContextToServiceUrlMap, int nodeId) {
        startClusterContext(clusterContext);
        addToClusterState(clusterContext.getCollectionsFactory(), clusterContext.getLocalMember(), nodeId);
        clusterContextToServiceUrlMap.put(clusterContext, format("ws://node%d.example.com:8080/path", nodeId));
    }

    private void startClusterContext(final DefaultClusterContext clusterContext) {
        FutureTask<Boolean> t = new FutureTask<>(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("Calling clusterContext.start in thread " + Thread.currentThread().getName());
                    clusterContext.start();
                    System.out.println("clusterContext.start has completed in thread "
                            + Thread.currentThread().getName());
                } catch (RuntimeException r) {
                    System.out.println("clusterContext.start threw " + r + " in thread "
                            + Thread.currentThread().getName());
                    r.printStackTrace();
                    throw r;
                }
            }
        }, Boolean.TRUE);
        Executors.newSingleThreadExecutor().submit(t);
        try {
            t.get(30, TimeUnit.SECONDS); // increased from 15, because sometimes get timeout on heavily loaded machine
            // (see note in KG-6045)
        }catch(ExecutionException ex){
           if(ex.getCause().getMessage().contains("not supported on AWS")) {
               System.out.println("expected on Travis build" +  ex.getMessage());
               Assume.assumeTrue(false);
            }
           else{
               ex.printStackTrace();
               fail("Could not start cluster context : " + ex);
           }
        }catch (TimeoutException |InterruptedException e) {
            if(e.getMessage().contains("not supported on AWS")) {
                System.out.println("expected on Travis build" +  e.getMessage());
                Assume.assumeTrue(false);
             }
            else{
               e.printStackTrace();
               fail("Could not start cluster context : " + e);
            }
        }
    }

    private void addToClusterState(CollectionsFactory factory, MemberId memberId, int nodeId) {
        Map<String, Set<String>> sharedBalancerMap = factory.getMap(BALANCER_MAP_NAME);
        Map<MemberId, Map<String, List<String>>> memberIdBalancerMap = factory.getMap(MEMBERID_BALANCER_MAP_NAME);
        String balanceURI = "ws://www.example.com:8080/path";
        String targetURI = format("ws://node%d.example.com:8080/path", nodeId);
        Set<String> currentTargets = sharedBalancerMap.get(balanceURI);
        if (currentTargets == null) {
            currentTargets = new TreeSet<>();
        }
        currentTargets.add(targetURI);
        sharedBalancerMap.put(balanceURI, currentTargets);

        List<String> myTargets = new ArrayList<>();
        myTargets.add(targetURI);
        Map<String, List<String>> myBalanceTargets = new HashMap<>();
        myBalanceTargets.put(balanceURI, myTargets);
        memberIdBalancerMap.put(memberId, myBalanceTargets);
    }

    private void validateClusterState(List<ClusterContext> activeMembers, Map<ClusterContext, String> clusterContextToServiceUrlMap) {
        Set<String> shouldExistBalanceTargets = new TreeSet<>();
        Set<MemberId> shouldExistMemberIds = new HashSet<>();

        for (ClusterContext activeMember : activeMembers) {
            shouldExistBalanceTargets.add(clusterContextToServiceUrlMap.get(activeMember));
            shouldExistMemberIds.add(activeMember.getLocalMember());
        }

        int activeMembSize = activeMembers.size();
        for (int i = 0; i < activeMembSize; i++) {
            ClusterContext activeMember = activeMembers.get(i);
            CollectionsFactory collectionsFactory = activeMember.getCollectionsFactory();
            if (!validateSharedBalancer(collectionsFactory, shouldExistBalanceTargets)) {
                fail(format("Expected %s URIs as balance targets in cluster member %s's memory", activeMembSize, i + 1));
            }

            MemberId localMember = activeMember.getLocalMember();
            if (!validateMemberIdBalancerMapKeyset(collectionsFactory, localMember, shouldExistMemberIds)) {
                fail(format("Expected %s URIs as balance targets in cluster member %s's memory", shouldExistMemberIds, i + 1));
            }

            String serviceUrl = clusterContextToServiceUrlMap.get(activeMember);
            List<String> myBalanceTargets = Collections.singletonList(serviceUrl);
            if (!validateMemberIdBalancerMap(collectionsFactory, localMember, myBalanceTargets)) {
                fail(format("Expected %s as balance target provided by cluster member %s",
                        serviceUrl, i + 1));
            }
        }
    }

    private boolean validateSharedBalancer(CollectionsFactory factory, Set<String> balanceTargets) {
        Map<String, Set<String>> sharedBalancerMap = factory.getMap(BALANCER_MAP_NAME);

        Set<String> currentBalanceTargets = sharedBalancerMap.get("ws://www.example.com:8080/path");
        return (currentBalanceTargets != null) && currentBalanceTargets.equals(balanceTargets);

    }

    private boolean validateMemberIdBalancerMap(CollectionsFactory factory, MemberId memberId, List<String> balanceTargets) {
        Map<MemberId, Map<String, List<String>>> memberIdBalancerMap = factory.getMap(MEMBERID_BALANCER_MAP_NAME);
        if (memberIdBalancerMap == null) {
            return false;
        }

        Map<String, List<String>> balancerMap = memberIdBalancerMap.get(memberId);
        if (balancerMap == null) {
            return false;
        }

        String balanceURI = "ws://www.example.com:8080/path";
        List<String> memberBalanceTargets = balancerMap.get(balanceURI);
        return (memberBalanceTargets != null) && memberBalanceTargets.containsAll(balanceTargets);
    }

    private boolean validateMemberIdBalancerMapKeyset(CollectionsFactory factory, MemberId memberId, Set<MemberId> activeMembers) {
        Map<MemberId, Map<String, List<String>>> memberIdBalancerMap = factory.getMap(MEMBERID_BALANCER_MAP_NAME);
        if (memberIdBalancerMap == null) {
            return false;
        }

        return memberIdBalancerMap.keySet().equals(activeMembers);
    }
}
