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
package org.kaazing.gateway.transport.bridge;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;

import org.kaazing.mina.core.buffer.AbstractIoBufferEx;

public abstract class MessageBuffer<T extends Message> extends AbstractIoBufferEx {

    private ByteBuffer buf;
    private AtomicReference<T> message;
    private volatile boolean autoCache;

    protected MessageBuffer(ByteBuffer buf) {
        super(buf.capacity());
        this.buf = buf;
        this.message = new AtomicReference<>();
    }

    protected MessageBuffer(MessageBuffer<T> parent, ByteBuffer buf) {
        super(parent);
        this.buf = buf;
        this.message = new AtomicReference<>();
    }

    public boolean setMessage(T newMessage) {
        return message.compareAndSet(null, newMessage);
    }

    public T getMessage() {
        return message.get();
    }

    @Override
    public ByteBuffer buf() {
        return buf;
    }

    @Override
    protected void buf(ByteBuffer buf) {
        this.buf = buf;
    }

    @Override
    protected MessageBuffer<T> duplicate0() {
        return create0(this, this.buf.duplicate());
    }

    @Override
    protected MessageBuffer<T> slice0() {
        return create0(this, this.buf.slice());
    }

    @Override
    protected MessageBuffer<T> asReadOnlyBuffer0() {
        return create0(this, this.buf.asReadOnlyBuffer());
    }

    protected abstract MessageBuffer<T> create0(MessageBuffer<T> parent, ByteBuffer buf);

    @Override
    public byte[] array() {
        return buf.array();
    }

    @Override
    public int arrayOffset() {
        return buf.arrayOffset();
    }

    @Override
    public boolean hasArray() {
        return buf.hasArray();
    }

    @Override
    public void free() {
        // no-op
    }

    public void setAutoCache(boolean autoCache) {
        this.autoCache = autoCache;
    }

    public boolean isAutoCache() {
        return autoCache;
    }
}
