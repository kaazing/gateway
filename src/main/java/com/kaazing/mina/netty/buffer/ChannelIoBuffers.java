/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.buffer;

import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;

import org.apache.mina.core.buffer.IoBuffer;

public class ChannelIoBuffers {

	private ChannelIoBuffers() {
		// no instances
	}
	
	public static IoBuffer wrap(ByteBuf byteBuf) {
        if (byteBuf.hasNioBuffer()) {
            ByteBuffer buffer = byteBuf.nioBuffer(byteBuf.readerIndex(), byteBuf.readableBytes());
            return IoBuffer.wrap(buffer);
        }
        else if (byteBuf.hasArray()) {
            byte[] byteArray = byteBuf.array();
            int offset = byteBuf.arrayOffset();
            int length = byteBuf.readableBytes();
            return IoBuffer.wrap(byteArray, offset, length);
        }
        else {
            throw new IllegalStateException("Unable to convert ByteBuf to IoBuffer");
        }
//        return new ChannelReadableIoBuffer(buf);
	}
}
