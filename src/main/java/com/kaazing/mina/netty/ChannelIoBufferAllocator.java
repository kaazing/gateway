/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package com.kaazing.mina.netty;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.buffer.IoBufferAllocator;

import com.kaazing.mina.core.buffer.AbstractIoBufferEx;
import com.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import com.kaazing.mina.netty.ChannelIoBufferAllocator.ChannelIoBuffer;

/**
 * A simplistic {@link IoBufferAllocator} which simply allocates a new
 * buffer every time.
 */
public final class ChannelIoBufferAllocator implements IoBufferAllocatorEx<ChannelIoBuffer>, IoBufferAllocator {

    @Override
    public IoBufferAllocator asBufferAllocator() {
        return this;
    }

    @Override
    public IoBuffer wrap(ByteBuffer nioBuffer) {
        return wrap(nioBuffer, false);
    }

    @Override
    public ByteBuffer allocateNioBuffer(int capacity) {
        return allocateNioBuffer(capacity, /* direct */ false);
    }

    @Override
    public ChannelIoBuffer allocate(int capacity, boolean shared) {
        return wrap(allocateNioBuffer(capacity), shared);
    }

    public ChannelIoBuffer allocate(int capacity, boolean direct, boolean shared) {
        return wrap(allocateNioBuffer(capacity, direct), shared);
    }

    public ByteBuffer allocateNioBuffer(int capacity, boolean direct) {
        ByteBuffer nioBuffer;
        if (direct) {
            nioBuffer = ByteBuffer.allocateDirect(capacity);
        } else {
            nioBuffer = ByteBuffer.allocate(capacity);
        }
        return nioBuffer;
    }

    public ChannelIoBuffer wrap(ByteBuffer nioBuffer, boolean shared) {
        return shared ? new ChannelIoSharedBuffer(nioBuffer) : new ChannelIoUnsharedBuffer(nioBuffer);
    }

    public void dispose() {
        // Do nothing
    }

    abstract static class ChannelIoBuffer extends AbstractIoBufferEx {
        private ByteBuffer buf;

        protected ChannelIoBuffer(ByteBuffer buf) {
            super(buf.capacity());
            this.buf = buf;
            buf.order(ByteOrder.BIG_ENDIAN);
        }

        protected ChannelIoBuffer(ChannelIoBuffer parent, ByteBuffer buf) {
            super(parent);
            this.buf = buf;
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
            // Do nothing
        }
    }

    // note: thread-aligned so no need for thread local ByteBuffer storage
    static final class ChannelIoSharedBuffer extends ChannelIoBuffer {
        private ChannelIoSharedBuffer(ByteBuffer buf) {
            super(buf);
        }

        private ChannelIoSharedBuffer(ChannelIoBuffer parent, ByteBuffer buf) {
            super(parent, buf);
        }

        @Override
        public boolean isShared() {
            return true;
        }

        @Override
        public ChannelIoSharedBuffer asSharedBuffer() {
            return this;
        }

        @Override
        public ChannelIoBuffer asUnsharedBuffer() {
            return new ChannelIoUnsharedBuffer(buf());
        }

        @Override
        protected ChannelIoUnsharedBuffer duplicate0() {
            return new ChannelIoUnsharedBuffer(this, buf().duplicate());
        }

        @Override
        protected ChannelIoUnsharedBuffer slice0() {
            return new ChannelIoUnsharedBuffer(this, buf().slice());
        }

        @Override
        protected ChannelIoUnsharedBuffer asReadOnlyBuffer0() {
            return new ChannelIoUnsharedBuffer(this, buf().asReadOnlyBuffer());
        }

    }

    static final class ChannelIoUnsharedBuffer extends ChannelIoBuffer {
        private ChannelIoUnsharedBuffer(ByteBuffer buf) {
            super(buf);
        }

        private ChannelIoUnsharedBuffer(ChannelIoBuffer parent, ByteBuffer buf) {
            super(parent, buf);
        }

        @Override
        public boolean isShared() {
            return false;
        }

        @Override
        public ChannelIoSharedBuffer asSharedBuffer() {
            return new ChannelIoSharedBuffer(buf());
        }

        @Override
        public ChannelIoBuffer asUnsharedBuffer() {
            return this;
        }

        @Override
        protected ChannelIoUnsharedBuffer duplicate0() {
            return new ChannelIoUnsharedBuffer(this, buf().duplicate());
        }

        @Override
        protected ChannelIoUnsharedBuffer slice0() {
            return new ChannelIoUnsharedBuffer(this, buf().slice());
        }

        @Override
        protected ChannelIoUnsharedBuffer asReadOnlyBuffer0() {
            return new ChannelIoUnsharedBuffer(this, buf().asReadOnlyBuffer());
        }

    }
}
