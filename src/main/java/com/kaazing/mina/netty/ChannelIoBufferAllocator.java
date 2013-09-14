/**
 * Copyright (c) 2007-2013, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.kaazing.mina.core.buffer.AbstractIoBufferAllocatorEx;
import com.kaazing.mina.core.buffer.AbstractIoBufferEx;
import com.kaazing.mina.core.buffer.IoBufferEx;
import com.kaazing.mina.netty.ChannelIoBufferAllocator.ChannelIoBuffer;

public final class ChannelIoBufferAllocator extends AbstractIoBufferAllocatorEx<ChannelIoBuffer> {

    @Override
    public ByteBuffer allocate(int capacity, int flags) {
        return allocateNioBuffer0(capacity, flags);
    }

    @Override
    public ChannelIoBuffer wrap(ByteBuffer nioBuffer, int flags) {
        boolean shared = (flags & IoBufferEx.FLAG_SHARED) != IoBufferEx.FLAG_NONE;
        return shared ? new ChannelIoSharedBuffer(nioBuffer) : new ChannelIoUnsharedBuffer(nioBuffer);
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
        public int flags() {
            return IoBufferEx.FLAG_SHARED;
        }

        @Override
        protected ChannelIoSharedBuffer asSharedBuffer0() {
            return this;
        }

        @Override
        protected ChannelIoBuffer asUnsharedBuffer0() {
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
        public int flags() {
            return IoBufferEx.FLAG_NONE;
        }

        @Override
        protected ChannelIoSharedBuffer asSharedBuffer0() {
            return new ChannelIoSharedBuffer(buf());
        }

        @Override
        protected ChannelIoBuffer asUnsharedBuffer0() {
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
