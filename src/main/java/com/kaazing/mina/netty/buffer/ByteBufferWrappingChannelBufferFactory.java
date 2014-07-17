/**
 * Copyright (c) 2007-2014, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.buffer;

import static java.lang.System.getProperty;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.jboss.netty.buffer.AbstractChannelBufferFactory;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferFactory;
import org.jboss.netty.buffer.ChannelBuffers;
import com.kaazing.mina.netty.util.threadlocal.VicariousThreadLocal;

public final class ByteBufferWrappingChannelBufferFactory extends AbstractChannelBufferFactory {

    public static final boolean OPTIMIZE_PERFORMANCE_CLIENT =
            Boolean.valueOf(getProperty("com.kaazing.mina.netty.OPTIMIZE_PERFORMANCE_CLIENT", "false"));
    public static final ChannelBufferFactory CHANNEL_BUFFER_FACTORY = new ByteBufferWrappingChannelBufferFactory();

    private final ThreadLocal<ByteBufferWrappingChannelBuffer> wrappingBufRef =
        new VicariousThreadLocal<ByteBufferWrappingChannelBuffer>() {

            @Override
            protected ByteBufferWrappingChannelBuffer initialValue() {
                return new ByteBufferWrappingChannelBuffer();
            }

        };

    @Override
    public ChannelBuffer getBuffer(ByteOrder order, int capacity) {
        ByteBufferWrappingChannelBuffer wrappingBuf = wrappingBufRef.get();
        return wrappingBuf;
    }

    @Override
    public ChannelBuffer getBuffer(ByteOrder order, byte[] array,
            int offset, int length) {
        return ChannelBuffers.wrappedBuffer(order, array, offset, length);
    }

    @Override
    public ChannelBuffer getBuffer(ByteBuffer nioBuffer) {
        ByteBufferWrappingChannelBuffer wrappingBuf = wrappingBufRef.get();
        return wrappingBuf.wrap(nioBuffer);
    }


}
