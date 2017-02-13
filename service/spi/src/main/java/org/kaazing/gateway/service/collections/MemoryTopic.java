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
package org.kaazing.gateway.service.collections;


import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import com.hazelcast.monitor.LocalTopicStats;
import com.hazelcast.monitor.impl.LocalTopicStatsImpl;


/**
 * Implementation of Hazelcast's ITopic interface providing a multi-threaded observer.
 *
 * It imposes restrictions on calling add/remove/publish methods from within a message listener's oMessage method on the
 * same thread. The main reason is to avoid StackOverflow by nested method calls.
 *
 * // TODO fix following forward reference
 * For implementation differences see tests in: org.kaazing.gateway.server.topic.AbstractClusterTopicTest
 *
 * @param <E> The type of the message exchanged.
 */
public class MemoryTopic<E> implements ITopic<E> {

    private final String name;

    private final class MessageListenerHolder {
        private final String key;
        private final MessageListener<E> listener;

        public MessageListenerHolder(String key, MessageListener<E> listener) {
            this.key = key;
            this.listener = listener;
        }
    }

    private List<MessageListenerHolder> messageListenerHolders = new CopyOnWriteArrayList<>();

    private final ThreadLocal<Boolean> isPublishing = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };

    private LocalTopicStatsImpl localTopicStats;

    private static final Logger LOGGER = LoggerFactory.getLogger(MemoryTopic.class);

    public MemoryTopic(String name) {
        this.name = name;
        ;
        this.localTopicStats = new LocalTopicStatsImpl();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Created topic: " + this.getName());
        }
    }

    @Override
    public String getPartitionKey() {
        return null;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getServiceName() {
        return null;
    }

    @Override
    public void publish(E o) {
        if (isPublishing.get()) {
            throw new UnsupportedOperationException("Cannot nest publishing operations");
        }
        isPublishing.set(true);
        Message<E> m = new Message<>(this.getName(), o, System.nanoTime(), null);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Publishing message on topic: " + MemoryTopic.this.getName() + ", notifying " + messageListenerHolders.size() + " listener(s).");
        }
        for (MessageListenerHolder holder : messageListenerHolders) {
            try {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Publishing message on topic: " + MemoryTopic.this.getName() + ", notifying listener: " + holder.key + ".");
                }
                localTopicStats.incrementReceives();
                holder.listener.onMessage(m);
            } catch (Exception e) {
                LOGGER.debug("Message listener: " + holder.key + " failed.", e);
            }
        }
        localTopicStats.incrementPublishes();
        isPublishing.set(false);
    }

    @Override
    public String addMessageListener(MessageListener<E> messageListener) {
        if (isPublishing.get()) {
            throw new UnsupportedOperationException("Cannot alter message listeners from publish operation");
        }
        if (messageListener == null) {
            throw new NullPointerException("messageListener must be not null");
        }
        String key = UUID.randomUUID().toString();
        MessageListenerHolder holder = new MessageListenerHolder(key, messageListener);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Adding message listener: " + key + " on topic: " + MemoryTopic.this.getName());
        }
        messageListenerHolders.add(holder);
        return key;
    }

    @Override
    public boolean removeMessageListener(String s) {
        if (isPublishing.get()) {
            throw new UnsupportedOperationException("Cannot alter message listeners from publish operation");
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Removing message listener: " + s + " on topic: " + MemoryTopic.this.getName());
        }

        boolean result = false;
        for (MessageListenerHolder holder : messageListenerHolders) {
            if (holder.key.equals(s)) {
                result = messageListenerHolders.remove(holder);
                break;
            }
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Removed message listener: " + s + " on topic: " + MemoryTopic.this.getName());
        }
        return result;
    }

    @Override
    public LocalTopicStats getLocalTopicStats() {
        return localTopicStats;
    }

    @Override
    public void destroy() {
        messageListenerHolders.clear();
        this.localTopicStats = new LocalTopicStatsImpl();
    }
}
