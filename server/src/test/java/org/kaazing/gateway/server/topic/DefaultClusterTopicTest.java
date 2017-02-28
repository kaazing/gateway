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

public class DefaultClusterTopicTest extends AbstractClusterTopicTest {

    private static DefaultClusterContext clusterContext1;
    private static DefaultClusterContext clusterContext2;

    @Rule
    public RuleChain chain = ITUtil.createRuleChain(60, TimeUnit.SECONDS);

    @BeforeClass
    public static void startContexts() {
        MemberId member1 = new MemberId("tcp", "127.0.0.1", 46943);
        MemberId member2 = new MemberId("tcp", "127.0.0.1", 46942);

        List<MemberId> accepts = Collections.singletonList(member1);
        List<MemberId> connects = Collections.singletonList(member2);
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
        topicMember1.destroy();
        topicMember2.destroy();
    }

    @Test
    public void shouldAllowSendAndReceiveMessagesOnTopics() throws Exception {
        ITopic<String> topicNoDeadlockOneMember = clusterContext1.getTopic("topic_no_deadlock_one_member");
        CountDownLatch listenersCalledNoDeadlockOneMember = new CountDownLatch(1);
        MessageListener<String> m1 = message -> listenersCalledNoDeadlockOneMember.countDown();
        String nameListenerNoDeadlock = topicNoDeadlockOneMember.addMessageListener(m1);
        // Will not throw UnsupportedOperationException, but StandaloneCluster implementation MemoryTopic will
        MessageListener<String> m2 = message -> topicNoDeadlockOneMember.removeMessageListener(nameListenerNoDeadlock);
        topicNoDeadlockOneMember.addMessageListener(m2);
        topicNoDeadlockOneMember.publish("msg1");
        listenersCalledNoDeadlockOneMember.await();
        topicNoDeadlockOneMember.destroy();
    }

    @Override
    protected ITopic<String> getTopicForShouldCallMessageListenersOnTwoThreads() {
        return clusterContext1.getTopic("topic_two_threads");
    }

    @Override
    protected ITopic<String> getTopicMember1ForShouldNotifyListenersIfOneThrowsException() {
        return clusterContext1.getTopic("topic_message_listener_null_pointer");
    }

    @Override
    protected ITopic<String> getTopicMember2ForShouldNotifyListenersIfOneThrowsException() {
        return clusterContext2.getTopic("topic_message_listener_null_pointer");
    }

    @Override
    protected ITopic<String> getTopicMember1ForShouldCallMultipleTimesMessageListener() {
        return clusterContext1.getTopic("topic_multiple_times_same_listener");
    }

    @Override
    protected ITopic<String> getTopicMember2ForShouldCallMultipleTimesMessageListener() {
        return clusterContext2.getTopic("topic_multiple_times_same_listener");
    }

    @Override
    protected ITopic<String> getTopicMember1ForShouldAddAndRemoveMessageListener() {
        return clusterContext1.getTopic("topic_add_remove_listener");
    }

    @Override
    protected ITopic<String> getTopicMember2ForShouldAddAndRemoveMessageListener() {
        return clusterContext2.getTopic("topic_add_remove_listener");
    }

    @Override
    protected ITopic<String> getTopicMember1ForShouldAllowAddAndRemoveFromMessageListenerDifferentThread() {
        return clusterContext1.getTopic("topic_allow_add_remove_from_listener_different_thread");
    }

    @Override
    protected ITopic<String> getTopicMember2ForShouldAllowAddAndRemoveFromMessageListenerDifferentThread() {
        return clusterContext2.getTopic("topic_allow_add_remove_from_listener_different_thread");
    }

    @Override
    protected ITopic<String> getTopicMember1ForShouldPubSubFromMessageListeners() {
        return clusterContext1.getTopic("topic_pub_sub_msg_listeners_1");
    }

    @Override
    protected ITopic<String> getTopicMember2ForShouldPubSubFromMessageListeners() {
        return clusterContext2.getTopic("topic_pub_sub_msg_listeners_2");
    }

    @Override
    protected ITopic<String> getTopicMember1ForShouldNotDeadlockNestedPublishOnDifferentThread() {
        return clusterContext1.getTopic("topic_nested_publish_different_thread_one_member");
    }

    @Override
    protected ITopic<String> getTopicMember1ForShouldNotAddNullMessageListener() {
        return clusterContext1.getTopic("topic_null_message_listener");
    }

    @Override
    protected ITopic<String> getTopicMember1ForShouldDetectClassIncompatibility() {
        return clusterContext1.getTopic("topic_class_cast");
    }

    @Override
    protected ITopic<Integer> getTopicMember2ForShouldDetectClassIncompatibility() {
        return clusterContext2.getTopic("topic_class_cast");
    }

}
