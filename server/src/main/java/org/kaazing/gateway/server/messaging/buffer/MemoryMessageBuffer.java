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

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.kaazing.gateway.service.messaging.MessagingMessage;
import org.kaazing.gateway.service.messaging.buffer.MessageBuffer;
import org.kaazing.gateway.service.messaging.buffer.MessageBufferEntry;
import org.kaazing.gateway.service.messaging.buffer.MessageBufferListener;

// TODO: could abstract into RingArray or RingArrayList in the future to use this more general purpose
//       might need that anyway for iteration?
// TODO: currently can only store up to MAX_INT messages and then nextId will wrap over to negative so need to
//       account for that
public class MemoryMessageBuffer implements MessageBuffer {

    private final MessageBufferListenerSupport listenerSupport;

    // concurrency controls for all internal state
    private final Lock readLock;
    private final Lock writeLock;

    // these do not need concurrency because we have a global read/write lock
    private final MessageBufferEntry[] messages;
    private final int capacity;

    private int nextId;

    public MemoryMessageBuffer(int capacity) {
        listenerSupport = new MessageBufferListenerSupport();

        ReadWriteLock lock = new ReentrantReadWriteLock();
        readLock = lock.readLock();
        writeLock = lock.writeLock();

        this.capacity = capacity;
        messages = new MessageBufferEntry[capacity];
        nextId = 1;
    }

    @Override
    public MessageBufferEntry add(MessagingMessage message) {
        writeLock.lock();
        try {
            int id = nextId++;
            MessageBufferEntry m = new MessageBufferEntry(id, message);
            messages[id % capacity] = m;
            listenerSupport.messageAdded(m);
            return m;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public MessageBufferEntry set(int index, MessagingMessage message) {
        writeLock.lock();
        try {
            if (nextId <= index) {
                nextId = index + 1;
            }
            MessageBufferEntry m = new MessageBufferEntry(index, message);
            messages[index % capacity] = m;
            listenerSupport.messageAdded(m);
            return m;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public MessageBufferEntry get(int id) {
        if (id < 1) {
            return null;
        }
        readLock.lock();
        try {
            // TODO: verify this works with adjustment of wrapping nextId
            if (id >= nextId || id < (nextId - capacity)) {
                return null;
            }
            MessageBufferEntry message = messages[id % capacity];
            return message;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public int getYoungestId() {
        readLock.lock();
        try {
            return nextId - 1;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public int getOldestId() {
        readLock.lock();
        try {
            int id = nextId - capacity;
            return (id < 1) ? 1 : id;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    @Override
    public void addMessageBufferListener(MessageBufferListener listener) {
        listenerSupport.addMessageBufferListener(listener);
    }

    @Override
    public void removeMessageBufferListener(MessageBufferListener listener) {
        listenerSupport.removeMessageBufferListener(listener);
    }

}
