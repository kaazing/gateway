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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.kaazing.gateway.service.collections.MemoryCollectionsFactory;
import org.kaazing.test.util.ITUtil;

import com.hazelcast.core.ITopic;
import com.hazelcast.core.MessageListener;

public class MemoryTopicTest {

    private MemoryCollectionsFactory factory;

    @Rule
    public RuleChain chain = ITUtil.createRuleChain(10, TimeUnit.SECONDS);

    @Before
    public void setUp() throws Exception {
        factory = new MemoryCollectionsFactory();
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
        CountDownLatch listenersCalled = new CountDownLatch(2);

        topic.addMessageListener(message -> listenersCalled.countDown());
        topic.addMessageListener(message -> {throw new NullPointerException();});
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
        MessageListener m1 = message -> {
            System.out.println("Called m1");
            listenersCalled.countDown();
        };
        String name = topic.addMessageListener(m1);
        MessageListener m2 = message -> {
            System.out.println("Called m2");
            topic.removeMessageListener(name);
        };
        topic.addMessageListener(m2);
        topic.publish("msg1");
        listenersCalled.await();
    }


    @Test
    public void shouldNotDeadLock() throws InterruptedException, ExecutionException {
        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService e = Executors.newSingleThreadExecutor(runnable -> new Thread(runnable, "memory-topics-test"));
        Future f = e.submit(() -> {
            try {
                e.submit(() -> {System.out.println("got it"); latch.countDown();}).get();
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            } catch (ExecutionException e1) {
                e1.printStackTrace();
            }
        });
        // f.get();
    }


}
