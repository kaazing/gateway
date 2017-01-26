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


import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import com.hazelcast.monitor.LocalTopicStats;
import com.hazelcast.monitor.impl.LocalTopicStatsImpl;

public class MemoryTopic<E> extends MemoryDistributedObject implements ITopic<E> {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> new Thread(runnable, "memory-topics-thread"));

    private Map<String, MessageListener> messageListeners = new HashMap<>();

    private final LocalTopicStatsImpl localTopicStats;

    public MemoryTopic(String name) {
        super(name);
        this.localTopicStats = new LocalTopicStatsImpl();
    }

    @Override
    public void publish(Object o) {
        EXECUTOR.submit(() -> {
            for (MessageListener messageListener : messageListeners.values()) {

                messageListener.onMessage((Message)o);
                localTopicStats.incrementReceives();
            }
        });
        localTopicStats.incrementPublishes();
    }

    @Override
    public String addMessageListener(MessageListener messageListener) {
        try {
            return EXECUTOR.submit(() -> {
                String key = "MemoryTopic_" + messageListener.hashCode();
                messageListeners.put(key, messageListener);
                return key;
            }).get();
        } catch (InterruptedException|ExecutionException e) {
            throw new RuntimeException("Unable to add message listener", e);
        }
    }

    @Override
    public boolean removeMessageListener(String s) {
        try {
            return EXECUTOR.submit(() -> {
                messageListeners.remove(s);
                return true;
            }).get();
        } catch (InterruptedException|ExecutionException e) {
            throw new RuntimeException("Unable to remove message listener", e);
        }
    }

    @Override
    public LocalTopicStats getLocalTopicStats() {
        return localTopicStats;
    }
}
