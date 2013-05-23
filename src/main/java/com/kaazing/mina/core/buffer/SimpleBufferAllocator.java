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
package com.kaazing.mina.core.buffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.apache.mina.core.buffer.IoBufferAllocator;

/**
 * A simplistic {@link IoBufferAllocator} which simply allocates a new
 * buffer every time.
 */
public final class SimpleBufferAllocator extends AbstractIoBufferAllocatorEx<SimpleBufferAllocator.SimpleBuffer> {

    public static final SimpleBuffer EMPTY_UNSHARED_BUFFER = new SimpleUnsharedBuffer(ByteBuffer.allocate(0));

    public static final SimpleBufferAllocator BUFFER_ALLOCATOR = new SimpleBufferAllocator();

    private SimpleBufferAllocator() {
    }

    @Override
    public ByteBuffer allocateNioBuffer(int capacity, int flags) {
        return allocateNioBuffer0(capacity, flags);
    }

    @Override
    public SimpleBuffer wrap(ByteBuffer nioBuffer, int flags) {
        boolean shared = (flags & IoBufferEx.FLAG_SHARED) != IoBufferEx.FLAG_NONE;
        return shared ? new SimpleSharedBuffer(nioBuffer) : new SimpleUnsharedBuffer(nioBuffer);
    }

    public abstract static class SimpleBuffer extends AbstractIoBufferEx {
        protected SimpleBuffer(int capacity) {
            super(capacity);
        }

        protected SimpleBuffer(SimpleBuffer parent) {
            super(parent);
        }
    }

    static final class SimpleUnsharedBuffer extends SimpleBuffer {
        private ByteBuffer buf;

        protected SimpleUnsharedBuffer(ByteBuffer buf) {
            super(buf.capacity());
            this.buf = buf;
            buf.order(ByteOrder.BIG_ENDIAN);
        }

        protected SimpleUnsharedBuffer(SimpleBuffer parent, ByteBuffer buf) {
            super(parent);
            this.buf = buf;
        }

        @Override
        public int flags() {
            return IoBufferEx.FLAG_NONE;
        }

        @Override
        public SimpleSharedBuffer asSharedBuffer() {
            return new SimpleSharedBuffer(buf);
        }

        @Override
        public SimpleUnsharedBuffer asUnsharedBuffer() {
            return this;
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
        protected SimpleUnsharedBuffer duplicate0() {
            return new SimpleUnsharedBuffer(this, this.buf.duplicate());
        }

        @Override
        protected SimpleUnsharedBuffer slice0() {
            return new SimpleUnsharedBuffer(this, this.buf.slice());
        }

        @Override
        protected SimpleUnsharedBuffer asReadOnlyBuffer0() {
            return new SimpleUnsharedBuffer(this, this.buf.asReadOnlyBuffer());
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
            // do nothing
        }
    }

    static final class SimpleSharedBuffer extends SimpleBuffer {
        private final ThreadLocal<ByteBuffer> bufRef;

        protected SimpleSharedBuffer(final ByteBuffer buf) {
            super(buf.capacity());
            this.bufRef = new ThreadLocal<ByteBuffer>() {
                @Override
                protected ByteBuffer initialValue() {
                    return buf.duplicate();
                }
            };
            buf.order(ByteOrder.BIG_ENDIAN);
        }

        protected SimpleSharedBuffer(SimpleBuffer parent, final ByteBuffer buf) {
            super(parent);
            this.bufRef = new ThreadLocal<ByteBuffer>() {
                @Override
                protected ByteBuffer initialValue() {
                    return buf.duplicate();
                }
            };
        }

        private SimpleSharedBuffer(SimpleSharedBuffer parent, ThreadLocal<ByteBuffer> bufRef) {
            super(parent);
            this.bufRef = bufRef;
        }

        @Override
        public int flags() {
            return IoBufferEx.FLAG_SHARED;
        }

        @Override
        public SimpleSharedBuffer asSharedBuffer() {
            return this;
        }

        @Override
        public SimpleUnsharedBuffer asUnsharedBuffer() {
            return new SimpleUnsharedBuffer(buf().duplicate());
        }

        @Override
        public ByteBuffer buf() {
            return bufRef.get();
        }

        @Override
        protected void buf(ByteBuffer buf) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected SimpleSharedBuffer duplicate0() {
            return new SimpleSharedBuffer(this, buf().duplicate());
        }

        @Override
        protected SimpleSharedBuffer slice0() {
            return new SimpleSharedBuffer(this, buf().slice());
        }

        @Override
        protected SimpleSharedBuffer asReadOnlyBuffer0() {
            return new SimpleSharedBuffer(this, buf().asReadOnlyBuffer());
        }

        @Override
        public byte[] array() {
            return buf().array();
        }

        @Override
        public int arrayOffset() {
            return buf().arrayOffset();
        }

        @Override
        public boolean hasArray() {
            return buf().hasArray();
        }

        @Override
        public void free() {
            // do nothing
        }

    }
}
