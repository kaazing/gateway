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
package org.kaazing.gateway.server.topic;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.kaazing.gateway.server.context.resolve.DefaultClusterContext;
import org.kaazing.gateway.service.cluster.MemberId;
import org.kaazing.test.util.ITUtil;

import com.hazelcast.core.ITopic;
import com.hazelcast.core.MessageListener;

public class DefaultClusterTopicTest {

    private static DefaultClusterContext clusterContext1;
    private static DefaultClusterContext clusterContext2;

    @Rule
    public RuleChain chain = ITUtil.createRuleChain(60, TimeUnit.SECONDS);


    @BeforeClass
    public static void startContexts() {
        MemberId member1 = new MemberId("tcp", "127.0.0.1", 46943);
        MemberId member2 = new MemberId("tcp", "127.0.0.1", 46942);

        List accepts = Collections.singletonList(member1);
        List connects = Collections.singletonList(member2);
        final String clusterName = DefaultClusterTopicTest.class.getName() + "-cluster1";
        clusterContext1 = new DefaultClusterContext(clusterName,
            accepts,
            connects,
            null);

        clusterContext2 = new DefaultClusterContext(clusterName,
            connects,
            accepts,
            null);

        clusterContext1.start();
        clusterContext2.start();
    }

    @AfterClass
    public static void disposeContexts() {
        clusterContext1.dispose();
        clusterContext2.dispose();
    }

    @Test
    public void shouldCallMessageListenersOnTwoThreads() throws InterruptedException {
        ITopic<String> topicMember1 = clusterContext1.getTopic("topic");
        CountDownLatch listenersCalled = new CountDownLatch(2);
        topicMember1.addMessageListener(message -> {
            assertEquals("msg1", message.getMessageObject());
            listenersCalled.countDown();
        });
        Thread t = new Thread(() -> {
            topicMember1.addMessageListener(message -> {
                assertEquals("msg1", message.getMessageObject());
                listenersCalled.countDown();
            });
        });
        t.start();
        t.join();
        topicMember1.publish("msg1");
        listenersCalled.await();
        assertEquals(1, topicMember1.getLocalTopicStats().getPublishOperationCount());
        assertEquals(2, topicMember1.getLocalTopicStats().getReceiveOperationCount());
    }

    @Test
    public void shouldSendAndReceiveMessagesBetweenMembers() throws InterruptedException {
        CountDownLatch listenerCalledDifferentMembers = new CountDownLatch(1);
        ITopic<String> topicMember1 = clusterContext1.getTopic("topic_between_members");
        topicMember1.addMessageListener(m -> {
            listenerCalledDifferentMembers.countDown();
        });

        ITopic<String> topicMember2 = clusterContext2.getTopic("topic_between_members");
        topicMember2.publish("test");
        listenerCalledDifferentMembers.await();
        assertEquals(1, topicMember2.getLocalTopicStats().getPublishOperationCount());
        assertEquals(1, topicMember1.getLocalTopicStats().getReceiveOperationCount());
    }


    @Test
    public void shouldNotifyListenersIfOneThrowsException() throws InterruptedException {
        ITopic<String> topicMessageListenerMember1 = clusterContext1.getTopic("topic_message_listener_null_pointer");
        CountDownLatch listenersCalled = new CountDownLatch(3);
        topicMessageListenerMember1.addMessageListener(message -> listenersCalled.countDown());
        topicMessageListenerMember1.addMessageListener(message -> {
            listenersCalled.countDown();
            throw new NullPointerException();
        });
        topicMessageListenerMember1.addMessageListener(message -> listenersCalled.countDown());
        ITopic<String> topicMessageListenerMember2 = clusterContext2.getTopic("topic_message_listener_null_pointer");
        topicMessageListenerMember2.publish("msg1");
        listenersCalled.await();
        assertEquals(1, topicMessageListenerMember2.getLocalTopicStats().getPublishOperationCount());
        assertEquals(3, topicMessageListenerMember1.getLocalTopicStats().getReceiveOperationCount());
    }

    @Test
    public void shouldCallMultipleTimesMessageListener() throws InterruptedException {
        ITopic<String> topicSameListenerMember1 = clusterContext1.getTopic("topic_same_listener");
        CountDownLatch listenersCalledSameListener = new CountDownLatch(2);
        MessageListener m = message -> listenersCalledSameListener.countDown();
        topicSameListenerMember1.addMessageListener(m);
        topicSameListenerMember1.addMessageListener(m);
        ITopic<String> topicSameListenerMember2 = clusterContext2.getTopic("topic_same_listener");
        topicSameListenerMember2.publish("msg1");
        listenersCalledSameListener.await();
        assertEquals(1, topicSameListenerMember2.getLocalTopicStats().getPublishOperationCount());
        assertEquals(2, topicSameListenerMember1.getLocalTopicStats().getReceiveOperationCount());
    }


    @Test
    public void shouldAddAndRemoveMessageListener() throws InterruptedException {
        ITopic<String> topicAddRemoveMember1 = clusterContext1.getTopic("topic_add_remove_listener");
        ITopic<String> topicAddRemoveMember2 = clusterContext2.getTopic("topic_add_remove_listener");
        CountDownLatch listenersCalledAddRemove = new CountDownLatch(1);
        String name = topicAddRemoveMember1.addMessageListener(message -> listenersCalledAddRemove.countDown());
        topicAddRemoveMember2.publish("msg1");
        listenersCalledAddRemove.await();
        topicAddRemoveMember1.removeMessageListener(name);
        topicAddRemoveMember2.publish("msg2");
        assertEquals(2, topicAddRemoveMember2.getLocalTopicStats().getPublishOperationCount());
        assertEquals(1, topicAddRemoveMember1.getLocalTopicStats().getReceiveOperationCount());
    }

    @Test
    public void shouldAllowSendAndReceiveMessagesOnTopics() throws Exception {
        ITopic<String> topicNoDeadlockOneMember = clusterContext1.getTopic("topic_no_deadlock_one_member");
        CountDownLatch listenersCalledNoDeadlockOneMember = new CountDownLatch(1);
        MessageListener m1 = message -> listenersCalledNoDeadlockOneMember.countDown();
        String nameListenerNoDeadlock = topicNoDeadlockOneMember.addMessageListener(m1);
        MessageListener m2 = message -> topicNoDeadlockOneMember.removeMessageListener(nameListenerNoDeadlock);
        topicNoDeadlockOneMember.addMessageListener(m2);
        topicNoDeadlockOneMember.publish("msg1");
        listenersCalledNoDeadlockOneMember.await();
    }
}
