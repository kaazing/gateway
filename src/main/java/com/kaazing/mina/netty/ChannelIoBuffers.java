/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import io.netty.buffer.ByteBuf;

import org.apache.mina.core.buffer.IoBuffer;

public class ChannelIoBuffers {

	private ChannelIoBuffers() {
		// no instances
	}
	
	public static ChannelIoBuffer wrap(ByteBuf byteBuf) {
        if (byteBuf.hasNioBuffer()) {
            ByteBuf sliceByteBuf = byteBuf.slice();
            IoBuffer sliceBuf = IoBuffer.wrap(sliceByteBuf.nioBuffer());
            return new ChannelIoBufferImpl(sliceBuf, sliceByteBuf);
        }
        else if (byteBuf.hasArray()) {
            ByteBuf sliceByteBuf = byteBuf.slice();
            byte[] byteArray = sliceByteBuf.array();
            int offset = sliceByteBuf.arrayOffset();
            int index = sliceByteBuf.readerIndex();
            int length = sliceByteBuf.readableBytes();
            return new ChannelIoBufferImpl(IoBuffer.wrap(byteArray, offset + index, length), sliceByteBuf);
        }
        else {
            throw new IllegalStateException("Unable to convert ByteBuf to IoBuffer");
        }
	}
}
