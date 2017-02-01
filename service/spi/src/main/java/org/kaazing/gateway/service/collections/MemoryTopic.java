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


import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.Resource;

import org.kaazing.gateway.util.scheduler.SchedulerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import com.hazelcast.monitor.LocalTopicStats;
import com.hazelcast.monitor.impl.LocalTopicStatsImpl;

public class MemoryTopic<E> extends MemoryDistributedObject implements ITopic<E> {

    private ScheduledExecutorService scheduler;

    private Map<String, MessageListener> messageListeners = new ConcurrentHashMap<>();

    private final LocalTopicStatsImpl localTopicStats;

    private static final Logger LOGGER = LoggerFactory.getLogger(MemoryTopic.class);

    public MemoryTopic(String name, SchedulerProvider provider) {
        super(name);
        this.scheduler = provider.getScheduler("memory_topics_publisher", false);
        this.localTopicStats = new LocalTopicStatsImpl();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Created topic: " + this.getName());
        }
    }

    @Override
    public void publish(Object o) {
        Message m = new Message(this.getName(), o, System.nanoTime(), null);
        this.scheduler.submit(() -> {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Publishing message on topic: " + MemoryTopic.this.getName() + ", notifying " + messageListeners.size() + " listener(s).");
            }
            for (Map.Entry<String, MessageListener> entry : messageListeners.entrySet()) {
                try {
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Publishing message on topic: " + MemoryTopic.this.getName() + ", notifying listener: " + entry.getKey() + ".");
                    }
                    localTopicStats.incrementReceives();
                    entry.getValue().onMessage(m);
                } catch (Exception e) {
                    LOGGER.debug("Message listener: " + entry.getKey() + " failed.", e);
                }
            }
        });
        localTopicStats.incrementPublishes();
    }

    @Override
    public String addMessageListener(MessageListener messageListener) {
        if (messageListener == null) {
            throw new MemoryCollectionsException("messageListener must be not null");
        }
        String key = UUID.randomUUID().toString();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Adding message listener: " + key + " on topic: " + MemoryTopic.this.getName());
        }
        messageListeners.put(key, messageListener);
        return key;
    }

    @Override
    public boolean removeMessageListener(String s) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Removing message listener: " + s + " on topic: " + MemoryTopic.this.getName());
        }
        messageListeners.remove(s);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Removed message listener: " + s + " on topic: " + MemoryTopic.this.getName());
        }
        return true;
    }

    @Override
    public LocalTopicStats getLocalTopicStats() {
        return localTopicStats;
    }
}
