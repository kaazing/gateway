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
import org.kaazing.gateway.service.collections.MemoryCollectionsException;
import org.kaazing.test.util.ITUtil;

import com.hazelcast.core.ITopic;
import com.hazelcast.core.MessageListener;

public class StandaloneClusterTopicTest {

    private static final StandaloneClusterContext STANDALONE_CLUESTER_CONTEXT = new StandaloneClusterContext();

    private CollectionsFactory factory;

    @Rule
    public RuleChain chain = ITUtil.createRuleChain(10, TimeUnit.SECONDS);

    @Before
    public void setUp() throws Exception {
        factory = STANDALONE_CLUESTER_CONTEXT.getCollectionsFactory();
    }


    @Test
    public void shouldCallMessageListenersOnTwoThreads() throws Exception {
        ITopic<String> topic = factory.getTopic("topic_two_threads");
        CountDownLatch listenersCalled = new CountDownLatch(2);
        topic.addMessageListener(message -> {
            assertEquals("msg1", message.getMessageObject());
            listenersCalled.countDown();
        });
        Thread t = new Thread(() -> {
            topic.addMessageListener(message -> {
                assertEquals("msg1", message.getMessageObject());
                listenersCalled.countDown();
            });
        });
        t.start();
        t.join();
        topic.publish("msg1");
        listenersCalled.await();
        assertEquals(1, topic.getLocalTopicStats().getPublishOperationCount());
        assertEquals(2, topic.getLocalTopicStats().getReceiveOperationCount());
        topic.destroy();
    }

    @Test
    public void shouldNotifyListenersIfOneThrowsException() throws InterruptedException {
        ITopic<String> topic = factory.getTopic("topic_exception_listeners");
        CountDownLatch listenersCalled = new CountDownLatch(3);

        topic.addMessageListener(message -> listenersCalled.countDown());
        topic.addMessageListener(message -> {
            listenersCalled.countDown();
            throw new NullPointerException();
        });
        topic.addMessageListener(message -> listenersCalled.countDown());
        topic.publish("msg1");
        listenersCalled.await();
        assertEquals(1, topic.getLocalTopicStats().getPublishOperationCount());
        assertEquals(3, topic.getLocalTopicStats().getReceiveOperationCount());
        topic.destroy();
    }

    @Test
    public void shouldCallMultipleTimesMessageListener() throws InterruptedException {
        ITopic<String> topic = factory.getTopic("topic_multiple_times_same_listener");
        CountDownLatch listenersCalled = new CountDownLatch(2);
        MessageListener m = message -> listenersCalled.countDown();
        topic.addMessageListener(m);
        topic.addMessageListener(m);
        topic.publish("msg1");
        listenersCalled.await();
        assertEquals(1, topic.getLocalTopicStats().getPublishOperationCount());
        assertEquals(2, topic.getLocalTopicStats().getReceiveOperationCount());
        topic.destroy();
    }

    @Test
    public void shouldAddAndRemoveMessageListener() throws InterruptedException {
        ITopic<String> topic = factory.getTopic("topic_add_remove_listener");
        CountDownLatch listenersCalled = new CountDownLatch(1);
        MessageListener m = message -> listenersCalled.countDown();
        String name = topic.addMessageListener(m);
        topic.publish("msg1");
        listenersCalled.await();
        topic.removeMessageListener(name);
        topic.publish("msg2");
        assertEquals(2, topic.getLocalTopicStats().getPublishOperationCount());
        assertEquals(1, topic.getLocalTopicStats().getReceiveOperationCount());
        topic.destroy();
    }

    @Test
    public void shouldRejectAddAndRemoveFromMessageListenerSameThread() throws InterruptedException {
        ITopic<String> topic = factory.getTopic("topic_reject_add_remove_from_listener_same_thread");
        CountDownLatch listenersCalled = new CountDownLatch(1);
        MessageListener m1 = message -> listenersCalled.countDown();
        String name = topic.addMessageListener(m1);
        MessageListener m2 = message -> {
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
    public void shouldAllowAddAndRemoveFromMessageListenerDifferentThread() throws InterruptedException {
        ITopic<String> topic = factory.getTopic("topic_reject_add_remove_from_listener_different_thread");
        CountDownLatch listenersCalled = new CountDownLatch(2);
        MessageListener m1 = message -> listenersCalled.countDown();
        String name = topic.addMessageListener(m1);
        MessageListener m2 = message -> {
            Thread t = new Thread(() -> {
                topic.removeMessageListener(name);
                listenersCalled.countDown();
            });
            t.start();
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
        topic.addMessageListener(m2);
        topic.publish("msg1");
        listenersCalled.await();
        assertEquals(1, topic.getLocalTopicStats().getPublishOperationCount());
        assertEquals(2, topic.getLocalTopicStats().getReceiveOperationCount());
        assertFalse(topic.removeMessageListener(name)); // already removed
        topic.destroy();
    }

    @Test
    public void shouldPubSubFromMessageListeners() throws InterruptedException {
        CountDownLatch listenersCalled = new CountDownLatch(1);
        ITopic<String> topic1 = factory.getTopic("topic1");
        ITopic<String> topic2 = factory.getTopic("topic2");
        MessageListener m1 = message -> topic2.publish("Message sent to topic2");
        MessageListener m2 = message -> listenersCalled.countDown();
        topic1.addMessageListener(m1);
        topic2.addMessageListener(m2);
        topic1.publish("trigger m2");
        listenersCalled.await();
        assertEquals(1, topic1.getLocalTopicStats().getPublishOperationCount());
        assertEquals(1, topic1.getLocalTopicStats().getReceiveOperationCount());
        assertEquals(1, topic2.getLocalTopicStats().getPublishOperationCount());
        assertEquals(1, topic2.getLocalTopicStats().getReceiveOperationCount());
        topic1.destroy();
        topic2.destroy();
    }


    @Test
    public void shouldNotAllowNestedPublish() throws InterruptedException {
        ITopic<String> topic = factory.getTopic("topic_nested_publish_same_thread");
        CountDownLatch listenerCalled = new CountDownLatch(1);
        AtomicBoolean nestedPublish = new AtomicBoolean(false);
        topic.addMessageListener(message -> {
            listenerCalled.countDown();
            topic.publish("Resend: " + message.getMessageObject());
            nestedPublish.set(true);
        });
        topic.publish("KickOff");
        listenerCalled.await();
        assertFalse(nestedPublish.get());
        assertEquals(1, topic.getLocalTopicStats().getPublishOperationCount());
        assertEquals(1, topic.getLocalTopicStats().getReceiveOperationCount());
        topic.destroy();
    }

    @Test
    public void shouldNotDeadlockNestedPublishOnDifferentThread() throws InterruptedException {
        ITopic<String> topic = factory.getTopic("topic_nested_publish_different_thread");
        CountDownLatch listenerCalled = new CountDownLatch(10);
        topic.addMessageListener(message -> {
            listenerCalled.countDown();
            Thread t = new Thread(() -> {
                if (listenerCalled.getCount() > 0) {
                    topic.publish("Resend: " + message.getMessageObject());
                }
            });
            t.start();
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        topic.publish("KickOff");
        listenerCalled.await();
        assertEquals(10, topic.getLocalTopicStats().getPublishOperationCount());
        assertEquals(10, topic.getLocalTopicStats().getReceiveOperationCount());
        topic.destroy();
    }

    @Test(expected = MemoryCollectionsException.class)
    public void shouldNotAddNullMessageListener() {
        ITopic<String> topic = factory.getTopic("topic_nul_message_listener");
        topic.addMessageListener(null);
    }

}
