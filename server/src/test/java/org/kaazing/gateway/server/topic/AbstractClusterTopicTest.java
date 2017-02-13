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

import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import com.hazelcast.core.ITopic;
import com.hazelcast.core.MessageListener;

public abstract class AbstractClusterTopicTest {

    @Test
    public void shouldCallMessageListenersOnTwoThreads() throws InterruptedException {
        ITopic<String> topicMember1 = getTopicForShouldCallMessageListenersOnTwoThreads();
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
        topicMember1.destroy();

    }

    @Test
    public void shouldNotifyListenersIfOneThrowsException() throws InterruptedException {
        ITopic<String> topicMessageListenerMember1 = getTopicMember1ForShouldNotifyListenersIfOneThrowsException();
        CountDownLatch listenersCalled = new CountDownLatch(3);
        topicMessageListenerMember1.addMessageListener(message -> listenersCalled.countDown());
        topicMessageListenerMember1.addMessageListener(message -> {
            listenersCalled.countDown();
            throw new NullPointerException();
        });
        topicMessageListenerMember1.addMessageListener(message -> listenersCalled.countDown());
        ITopic<String> topicMessageListenerMember2 = getTopicMember2ForShouldNotifyListenersIfOneThrowsException();
        topicMessageListenerMember2.publish("msg1");
        listenersCalled.await();
        assertEquals(1, topicMessageListenerMember2.getLocalTopicStats().getPublishOperationCount());
        assertEquals(3, topicMessageListenerMember1.getLocalTopicStats().getReceiveOperationCount());
        topicMessageListenerMember1.destroy();
        topicMessageListenerMember2.destroy();
    }


    @Test
    public void shouldCallMultipleTimesMessageListener() throws InterruptedException {
        ITopic<String> topicSameListenerMember1 = getTopicMember1ForShouldCallMultipleTimesMessageListener();
        CountDownLatch listenersCalledSameListener = new CountDownLatch(2);
        MessageListener<String> m = message -> listenersCalledSameListener.countDown();
        topicSameListenerMember1.addMessageListener(m);
        topicSameListenerMember1.addMessageListener(m);
        ITopic<String> topicSameListenerMember2 = getTopicMember2ForShouldCallMultipleTimesMessageListener();
        topicSameListenerMember2.publish("msg1");
        listenersCalledSameListener.await();
        assertEquals(1, topicSameListenerMember2.getLocalTopicStats().getPublishOperationCount());
        assertEquals(2, topicSameListenerMember1.getLocalTopicStats().getReceiveOperationCount());
        topicSameListenerMember1.destroy();
        topicSameListenerMember2.destroy();
    }

    @Test
    public void shouldAddAndRemoveMessageListener() throws InterruptedException {
        ITopic<String> topicAddRemoveMember1 = getTopicMember1ForShouldAddAndRemoveMessageListener();
        ITopic<String> topicAddRemoveMember2 = getTopicMember2ForShouldAddAndRemoveMessageListener();
        CountDownLatch listenersCalledAddRemove = new CountDownLatch(1);
        String name = topicAddRemoveMember1.addMessageListener(message -> listenersCalledAddRemove.countDown());
        topicAddRemoveMember2.publish("msg1");
        listenersCalledAddRemove.await();
        topicAddRemoveMember1.removeMessageListener(name);
        topicAddRemoveMember2.publish("msg2");
        assertEquals(2, topicAddRemoveMember2.getLocalTopicStats().getPublishOperationCount());
        assertEquals(1, topicAddRemoveMember1.getLocalTopicStats().getReceiveOperationCount());
        topicAddRemoveMember1.destroy();
        topicAddRemoveMember2.destroy();
    }

    @Test
    public void shouldAllowAddAndRemoveFromMessageListenerDifferentThread() throws InterruptedException {
        ITopic<String> topicMember1 = getTopicMember1ForShouldAllowAddAndRemoveFromMessageListenerDifferentThread();
        ITopic<String> topicMember2 = getTopicMember2ForShouldAllowAddAndRemoveFromMessageListenerDifferentThread();
        CountDownLatch listenerCalled = new CountDownLatch(1);
        MessageListener<String> m1 = message -> {};
        String name = topicMember1.addMessageListener(m1);
        MessageListener<String> m2 = message -> {
            Thread t = new Thread(() -> {
                topicMember1.removeMessageListener(name);
                listenerCalled.countDown();
            });
            t.start();
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
        topicMember1.addMessageListener(m2);
        topicMember2.publish("msg1");
        listenerCalled.await();
        assertFalse(topicMember1.removeMessageListener(name)); // already removed
        topicMember1.destroy();
        topicMember2.destroy();
    }

    @Test
    public void shouldPubSubFromMessageListeners() throws InterruptedException {
        CountDownLatch listenersCalled = new CountDownLatch(1);
        ITopic<String> topic1 = getTopicMember1ForShouldPubSubFromMessageListeners();
        ITopic<String> topic2 = getTopicMember2ForShouldPubSubFromMessageListeners();
        MessageListener<String> m1 = message -> topic2.publish("Message sent to topic2");
        MessageListener<String> m2 = message -> listenersCalled.countDown();
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
    public void shouldNotDeadlockNestedPublishOnDifferentThread() throws InterruptedException {
        ITopic<String> topic = getTopicMember1ForShouldNotDeadlockNestedPublishOnDifferentThread();
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

    @Test(expected = NullPointerException.class)
    public void shouldNotAddNullMessageListener() {
        ITopic<String> topic = getTopicMember1ForShouldNotAddNullMessageListener();
        topic.addMessageListener(null);
    }

    @Test
    public void shouldDetectClassIncompatibility() throws InterruptedException {
        CountDownLatch registeredBadListenerType = new CountDownLatch(2);
        ITopic<String> topic = getTopicMember1ForShouldDetectClassIncompatibility();
        topic.addMessageListener(message -> registeredBadListenerType.countDown());
        ITopic<Integer> topic2 = getTopicMember2ForShouldDetectClassIncompatibility();
        topic2.addMessageListener(message -> {
            try {
                int i = message.getMessageObject() + 1;
            } catch (ClassCastException e) {
                registeredBadListenerType.countDown();
            }
        });
        topic.publish("xyz");
        registeredBadListenerType.await();
        assertEquals(1, topic.getLocalTopicStats().getPublishOperationCount());
        topic.destroy();
        topic2.destroy();
    }

    protected abstract ITopic<String> getTopicForShouldCallMessageListenersOnTwoThreads();

    protected abstract ITopic<String> getTopicMember1ForShouldNotifyListenersIfOneThrowsException();

    protected abstract ITopic<String> getTopicMember2ForShouldNotifyListenersIfOneThrowsException();

    protected abstract ITopic<String> getTopicMember1ForShouldCallMultipleTimesMessageListener();

    protected abstract ITopic<String> getTopicMember2ForShouldCallMultipleTimesMessageListener();

    protected abstract ITopic<String> getTopicMember1ForShouldAddAndRemoveMessageListener();

    protected abstract ITopic<String> getTopicMember2ForShouldAddAndRemoveMessageListener();

    protected abstract ITopic<String> getTopicMember1ForShouldAllowAddAndRemoveFromMessageListenerDifferentThread();

    protected abstract ITopic<String> getTopicMember2ForShouldAllowAddAndRemoveFromMessageListenerDifferentThread();

    protected abstract ITopic<String> getTopicMember1ForShouldPubSubFromMessageListeners();

    protected abstract ITopic<String> getTopicMember2ForShouldPubSubFromMessageListeners();

    protected abstract ITopic<String> getTopicMember1ForShouldNotDeadlockNestedPublishOnDifferentThread();

    protected abstract ITopic<String> getTopicMember1ForShouldNotAddNullMessageListener();

    protected abstract ITopic<String> getTopicMember1ForShouldDetectClassIncompatibility();

    protected abstract ITopic<Integer> getTopicMember2ForShouldDetectClassIncompatibility();

}
