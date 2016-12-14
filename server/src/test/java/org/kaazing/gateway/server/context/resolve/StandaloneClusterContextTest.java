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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.concurrent.locks.Lock;

import org.junit.Test;
import org.kaazing.gateway.service.cluster.MemberId;

public class StandaloneClusterContextTest {

    private static final MemberId STANDALONE_CLUSTER_MEMBER = new MemberId("tcp", "standalone", 0);
    private static final StandaloneClusterContext STANDALONE_CLUESTER_CONTEXT = new StandaloneClusterContext();

    @Test
    public void shouldTestStandaloneClusterName() {
        assertEquals(STANDALONE_CLUSTER_MEMBER.toString(), STANDALONE_CLUESTER_CONTEXT.getClusterName());
    }

    @Test
    public void shouldTestStandaloneClusterMemberList() {
        assertEquals(Arrays.asList(STANDALONE_CLUSTER_MEMBER), STANDALONE_CLUESTER_CONTEXT.getMemberIds());
    }

    @Test
    public void shouldTestAcceptsIsNull() {
        assertNull(STANDALONE_CLUESTER_CONTEXT.getAccepts());
    }

    @Test
    public void shouldTestConnectsIsNull() {
        assertNull(STANDALONE_CLUESTER_CONTEXT.getConnects());
    }

    @Test
    public void shouldTestConnectOptionssIsNull() {
        assertNull(STANDALONE_CLUESTER_CONTEXT.getConnectOptions());
    }

    @Test
    public void shouldTestStandaloneLocalMember() {
        assertEquals(STANDALONE_CLUSTER_MEMBER, STANDALONE_CLUESTER_CONTEXT.getLocalMember());
    }

    @Test
    public void shouldTestGetInstanceKeyAlwaysReturnsSameKey() {
        String standaloneInstanceKey = STANDALONE_CLUESTER_CONTEXT.getInstanceKey(STANDALONE_CLUSTER_MEMBER);
        assertNotNull(standaloneInstanceKey);
        assertEquals(standaloneInstanceKey, STANDALONE_CLUESTER_CONTEXT.getInstanceKey(new MemberId("test_protocol", "test_host", 0)));
    }

    @Test
    public void shouldTestGetLockReturnsSameObjectByName() {
        Lock lock = STANDALONE_CLUESTER_CONTEXT.getLock("testLock");
        assertNotNull(lock);
        assertEquals(lock, STANDALONE_CLUESTER_CONTEXT.getLock("testLock"));
    }

    @Test
    public void shouldTestCollectionsFactoryIsNotNull() {
        assertNotNull(STANDALONE_CLUESTER_CONTEXT.getCollectionsFactory());
    }
}
