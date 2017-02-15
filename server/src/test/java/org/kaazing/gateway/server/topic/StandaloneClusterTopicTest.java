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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.kaazing.gateway.server.context.resolve.StandaloneClusterContext;
import org.kaazing.gateway.service.collections.CollectionsFactory;
import org.kaazing.test.util.ITUtil;

import com.hazelcast.core.ITopic;
import com.hazelcast.core.MessageListener;

// TODO Add a parent abstract class that defines test cases for both cluster and single node.
public class StandaloneClusterTopicTest extends AbstractClusterTopicTest {

    private static final StandaloneClusterContext STANDALONE_CLUESTER_CONTEXT = new StandaloneClusterContext();

    private CollectionsFactory factory;

    @Rule
    public RuleChain chain = ITUtil.createRuleChain(10, TimeUnit.SECONDS);

    @Before
    public void setUp() throws Exception {
        factory = STANDALONE_CLUESTER_CONTEXT.getCollectionsFactory();
    }

    @Test
    public void shouldRejectAddAndRemoveFromMessageListenerSameThread() throws InterruptedException {
        ITopic<String> topic = factory.getTopic("topic_reject_add_remove_from_listener_same_thread");
        CountDownLatch listenersCalled = new CountDownLatch(1);
        MessageListener<String> m1 = message -> listenersCalled.countDown();
        String name = topic.addMessageListener(m1);
        MessageListener<String> m2 = message -> {
            try {
                topic.removeMessageListener(name); // will throw UnsupportedOperationException
            } catch (UnsupportedOperationException e) {
                topic.addMessageListener(m -> {}); // will also throw UnsupportedOperationException
            }
        };
        topic.addMessageListener(m2);
        topic.publish("msg1");
        listenersCalled.await();
        assertEquals(1, topic.getLocalTopicStats().getPublishOperationCount());
        assertEquals(2, topic.getLocalTopicStats().getReceiveOperationCount());
        assertTrue(topic.removeMessageListener(name));
        topic.destroy();
    }

    @Test
    public void shouldNotAllowNestedPublish() throws InterruptedException {
        ITopic<String> topic = factory.getTopic("topic_nested_publish_same_thread");
        CountDownLatch listenerCalled = new CountDownLatch(1);
        topic.addMessageListener(message -> {
            try {
                topic.publish("Resend: " + message.getMessageObject());
            } catch (UnsupportedOperationException e) {
                listenerCalled.countDown();
            }
        });
        topic.publish("KickOff");
        listenerCalled.await();
        assertEquals(1, topic.getLocalTopicStats().getPublishOperationCount());
        assertEquals(1, topic.getLocalTopicStats().getReceiveOperationCount());
        topic.destroy();
    }

    @Override
    protected ITopic<String> getTopicForShouldCallMessageListenersOnTwoThreads() {
        return factory.getTopic("topic_two_threads");
    }

    @Override
    protected ITopic<String> getTopicMember1ForShouldNotifyListenersIfOneThrowsException() {
        return factory.getTopic("topic_message_listener_null_pointer");
    }

    @Override
    protected ITopic<String> getTopicMember2ForShouldNotifyListenersIfOneThrowsException() {
        return factory.getTopic("topic_message_listener_null_pointer");
    }

    @Override
    protected ITopic<String> getTopicMember1ForShouldCallMultipleTimesMessageListener() {
        return factory.getTopic("topic_multiple_times_same_listener");
    }

    @Override
    protected ITopic<String> getTopicMember2ForShouldCallMultipleTimesMessageListener() {
        return factory.getTopic("topic_multiple_times_same_listener");
    }

    @Override
    protected ITopic<String> getTopicMember1ForShouldAddAndRemoveMessageListener() {
        return factory.getTopic("topic_add_remove_listener");
    }

    @Override
    protected ITopic<String> getTopicMember2ForShouldAddAndRemoveMessageListener() {
        return factory.getTopic("topic_add_remove_listener");
    }

    @Override
    protected ITopic<String> getTopicMember1ForShouldAllowAddAndRemoveFromMessageListenerDifferentThread() {
        return factory.getTopic("topic_allow_add_remove_from_listener_different_thread");
    }

    @Override
    protected ITopic<String> getTopicMember2ForShouldAllowAddAndRemoveFromMessageListenerDifferentThread() {
        return factory.getTopic("topic_allow_add_remove_from_listener_different_thread");
    }

    @Override
    protected ITopic<String> getTopicMember1ForShouldPubSubFromMessageListeners() {
        return factory.getTopic("topic_pub_sub_msg_listeners_1");
    }

    @Override
    protected ITopic<String> getTopicMember2ForShouldPubSubFromMessageListeners() {
        return factory.getTopic("topic_pub_sub_msg_listeners_2");
    }

    @Override
    protected ITopic<String> getTopicMember1ForShouldNotDeadlockNestedPublishOnDifferentThread() {
        return factory.getTopic("topic_nested_publish_different_thread_one_member");
    }

    @Override
    protected ITopic<String> getTopicMember1ForShouldNotAddNullMessageListener() {
        return factory.getTopic("topic_null_message_listener");
    }

    @Override
    protected ITopic<String> getTopicMember1ForShouldDetectClassIncompatibility() {
        return factory.getTopic("topic_class_cast");
    }

    @Override
    protected ITopic<Integer> getTopicMember2ForShouldDetectClassIncompatibility() {
        return factory.getTopic("topic_class_cast");
    }

}
