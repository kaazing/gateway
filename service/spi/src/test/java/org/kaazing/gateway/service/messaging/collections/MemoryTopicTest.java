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
package org.kaazing.gateway.service.messaging.collections;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.kaazing.gateway.service.collections.MemoryCollectionsFactory;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;
import org.kaazing.test.util.ITUtil;

import com.hazelcast.core.ITopic;
import com.hazelcast.core.MessageListener;


// TODO move this test to org.kaazing.gateway.server.context.resolve (standalone/default)
//
// Tests not covered:
//    - topic publish will happen-before calling add/remove inside a message listener
//    - add/remove inside a message listener guarantees topicStats having correct values
//
public class MemoryTopicTest {

    private MemoryCollectionsFactory factory;

    @Rule
    public RuleChain chain = ITUtil.createRuleChain(10, TimeUnit.SECONDS);

    @Before
    public void setUp() throws Exception {
        factory = new MemoryCollectionsFactory(new SchedulerProvider());
        Thread.sleep(100);
    }

    @Test
    public void shouldCallMessageListenersOnTwoThreads() throws Exception {
        ITopic<String> topic = factory.getTopic("topic");
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
    }

    @Test
    public void shouldNotifyListenersIfOneThrowsException() throws InterruptedException {
        ITopic<String> topic = factory.getTopic("topic");
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
    }

    @Test
    public void shouldCallMultipleTimesMessageListener() throws InterruptedException {
        ITopic<String> topic = factory.getTopic("topic");
        CountDownLatch listenersCalled = new CountDownLatch(2);
        MessageListener m = message -> listenersCalled.countDown();
        topic.addMessageListener(m);
        topic.addMessageListener(m);
        topic.publish("msg1");
        listenersCalled.await();
        assertEquals(1, topic.getLocalTopicStats().getPublishOperationCount());
        assertEquals(2, topic.getLocalTopicStats().getReceiveOperationCount());
    }

    @Test
    public void shouldAddAndRemoveMessageListener() throws InterruptedException {
        ITopic<String> topic = factory.getTopic("topic");
        CountDownLatch listenersCalled = new CountDownLatch(1);
        MessageListener m = message -> listenersCalled.countDown();
        String name = topic.addMessageListener(m);
        topic.publish("msg1");
        listenersCalled.await();
        topic.removeMessageListener(name);
        topic.publish("msg2");
        assertEquals(2, topic.getLocalTopicStats().getPublishOperationCount());
        assertEquals(1, topic.getLocalTopicStats().getReceiveOperationCount());
    }

    @Test
    public void shouldAddAndRemoveMessageListenerWithoutDeadlock() throws InterruptedException {
        ITopic<String> topic = factory.getTopic("topic");
        CountDownLatch listenersCalled = new CountDownLatch(1);
        MessageListener m1 = message -> {};
        String name = topic.addMessageListener(m1);
        MessageListener m2 = message -> {
            topic.removeMessageListener(name);
            listenersCalled.countDown();
        };
        topic.addMessageListener(m2);
        topic.publish("msg1");
        listenersCalled.await();
    }

    @Test
    public void shouldPubSubFromMessageListeners() {
        CountDownLatch listenersCalled = new CountDownLatch(1);
        ITopic<String> topic1 = factory.getTopic("topic1");
        ITopic<String> topic2 = factory.getTopic("topic2");
        MessageListener m1 = message -> topic2.publish("Message sent to topic2");
        MessageListener m2 = message -> listenersCalled.countDown();
        topic1.addMessageListener(m1);
        topic2.addMessageListener(m2);
        topic1.publish("trigger m2");
    }


    @Test
    public void shouldNotOverflow() throws InterruptedException {
        ITopic<String> topic = factory.getTopic("topic");
        CountDownLatch latch = new CountDownLatch(10000);
        topic.addMessageListener(message -> {
            latch.countDown();
            if (latch.getCount() > 0) {
                topic.publish("Resend: " + message.getMessageObject());
            }
        });
        topic.publish("KickOff");
        latch.await();
    }
}
