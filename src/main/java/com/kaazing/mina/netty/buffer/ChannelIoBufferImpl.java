/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.buffer;

import static io.netty.buffer.Unpooled.unmodifiableBuffer;
import io.netty.buffer.ByteBuf;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.buffer.IoBufferWrapper;

public class ChannelIoBufferImpl extends IoBufferWrapper implements ChannelIoBuffer {

    private final ByteBuf byteBuf;
    
    public ChannelIoBufferImpl(IoBuffer buf, ByteBuf byteBuf) {
        super(buf);
        this.byteBuf = byteBuf;
    }

    @Override
    public IoBuffer duplicate() {
        // note: reference count is not otherwise decremented automatically
        ByteBuf duplicate = byteBuf.duplicate();
        duplicate.unsafe().release();
        return new ChannelIoBufferImpl(super.duplicate(), duplicate);
    }

    @Override
    public IoBuffer slice() {
        // note: reference count is not otherwise decremented automatically
        ByteBuf slice = byteBuf.slice(position(), remaining());
        slice.unsafe().release();
        return new ChannelIoBufferImpl(super.slice(), slice);
    }
    
    @Override
    public IoBuffer getSlice(int index, int length) {
        // note: reference count is not otherwise decremented automatically
        ByteBuf slice = byteBuf.slice(index, length);
        slice.unsafe().release();
        return new ChannelIoBufferImpl(super.getSlice(index, length), slice);
    }

    @Override
    public IoBuffer getSlice(int length) {
        // note: reference count is not otherwise decremented automatically
        ByteBuf slice = byteBuf.slice(position(), length);
        slice.unsafe().release();
        return new ChannelIoBufferImpl(super.getSlice(length), slice);
    }

    @Override
    public IoBuffer asReadOnlyBuffer() {
        return new ChannelIoBufferImpl(super.asReadOnlyBuffer(), unmodifiableBuffer(byteBuf));
    }

    @Override
    public ByteBuf byteBuf() {
        // note: sync readableBytes before returning byte buffer
        byteBuf.setIndex(position(), limit());
        return byteBuf;
    }

    @Override
    public IoBuffer ioBuf() {
        return this;
    }
}
