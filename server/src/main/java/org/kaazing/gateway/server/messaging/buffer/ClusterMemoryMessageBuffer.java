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
package org.kaazing.gateway.server.messaging.buffer;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.MessageListener;
import java.io.IOException;
import java.io.Serializable;
import org.kaazing.gateway.service.messaging.MessagingMessage;
import org.kaazing.gateway.service.messaging.buffer.MessageBuffer;
import org.kaazing.gateway.service.messaging.buffer.MessageBufferEntry;
import org.kaazing.gateway.service.messaging.buffer.MessageBufferListener;
import org.kaazing.gateway.util.GL;
import org.kaazing.gateway.util.Utils;

/**
 * TODO Add class documentation
 */
public class ClusterMemoryMessageBuffer implements MessageBuffer, Serializable {

    private static final long serialVersionUID = 1L;

    private String topicName;

    private transient MessageBuffer buffer;
    private transient ITopic<MessageBufferEntry> topic;

    private HazelcastInstance cluster;

    // create clone buffer
    protected ClusterMemoryMessageBuffer() {
    }

    // create master buffer
    public ClusterMemoryMessageBuffer(HazelcastInstance cluster, int capacity) {
        this.cluster = cluster;
        topicName = Utils.randomHexString(8);
        GL.debug("messaging", "Creating cluster message buffer {}", topicName);
        init(capacity);
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeInt(buffer.getCapacity());
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        int capacity = in.readInt();
        init(capacity);
    }

    private void init(int capacity) {
        buffer = new MemoryMessageBuffer(capacity);
        GL.debug("messaging", "Cluster message buffer listening on {}", topicName);
        topic = cluster.getTopic(topicName);
        topic.addMessageListener(new MessageListener<MessageBufferEntry>() {
            @Override
            public void onMessage(MessageBufferEntry entry) {
                if (entry == null) {
                    GL.debug("messaing", "Received null entry");
                    return;
                }
                int id = entry.getId();
                if (id > buffer.getYoungestId()) {
                    GL.debug("messaging", "Received message entry {}", entry);
                    buffer.set(id, entry.getMessage());
                }
            }
        });
    }

    // -- delegate methods --

    @Override
    public MessageBufferEntry add(MessagingMessage message) {
        GL.debug("messaging", "Received message {}", message);
        MessageBufferEntry entry = buffer.add(message);
        if (entry != null) {
            topic.publish(entry);
        } else {
            GL.debug("messaging", "Got null entry from {}", buffer);
        }
        return entry;
    }

    @Override
    public MessageBufferEntry get(int id) {
        return buffer.get(id);
    }

    @Override
    public MessageBufferEntry set(int index, MessagingMessage message) {
        return buffer.set(index, message);
    }

    @Override
    public int getYoungestId() {
        return buffer.getYoungestId();
    }

    @Override
    public int getOldestId() {
        return buffer.getOldestId();
    }

    @Override
    public int getCapacity() {
        return buffer.getCapacity();
    }

    @Override
    public void addMessageBufferListener(MessageBufferListener listener) {
        buffer.addMessageBufferListener(listener);
    }

    @Override
    public void removeMessageBufferListener(MessageBufferListener listener) {
        buffer.removeMessageBufferListener(listener);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[topicName=")
                .append(topicName)
                .append(",youngestId=")
                .append(buffer.getYoungestId())
                .append(']');
        return sb.toString();
    }

}
