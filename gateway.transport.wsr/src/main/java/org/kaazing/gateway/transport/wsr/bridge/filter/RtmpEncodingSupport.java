/**
 * Copyright (c) 2007-2014 Kaazing Corporation. All rights reserved.
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.kaazing.gateway.transport.wsr.bridge.filter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.kaazing.gateway.transport.ws.util.WsUtils;
import org.kaazing.gateway.transport.wsr.RtmpDataMessage;
import org.kaazing.gateway.transport.wsr.RtmpStreamMessage;
import org.kaazing.gateway.transport.wsr.util.Amf0Utils;
import org.kaazing.gateway.transport.wsr.util.Amf3Utils;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;

public class RtmpEncodingSupport {
    private static final byte[] DOWNSTREAM_MESSAGE_HEADER = {0x00, Amf0Utils.Type.STRING.getCode(), 0x00, 0x01, 0x64}; // "d"
    private static final int DOWNSTREAM_CHUNKSTREAM_ID = 8;
    public static final int MAXIMUM_CHUNK_SIZE = 65536;


    public static IoBufferEx doBinaryEscapedEncode(final IoBufferAllocatorEx<?> allocator, IoBufferEx buf,
                                                 RtmpDataMessage message, final int maximumChunkSize, byte[] escapeBytes) {
        int escapedRemaining = escapeBytes.length;
        int escapedChunkOffset = 1 + 3 + 3 + 1 + 4;
        int escapedStreamOffset = 5 + 1 + 1 + WsUtils.calculateEncodedLengthSize(escapedRemaining << 1);
        int escapedBinaryOffset = escapedChunkOffset + escapedStreamOffset;


        int remaining = buf.remaining();
        int chunkOffset = 1 + 3 + 3 + 1 + 4;
        int streamOffset = 5 + 1 + 1 + WsUtils.calculateEncodedLengthSize(remaining << 1);
        int binaryOffset = chunkOffset + streamOffset;

        int totalOffset = escapedBinaryOffset + escapedRemaining + binaryOffset;

        if (buf.position() > totalOffset && escapedStreamOffset + streamOffset + remaining <= maximumChunkSize) {

            // Note: duplicate first to represent different transport layer (no parallel encoding)
            IoBufferEx binary = buf.duplicate();
            binary.skip(-totalOffset);

            // remember start of binary frame
            binary.mark();

            // add the escaped chunk header
            doEncodeChunk0Header(DOWNSTREAM_CHUNKSTREAM_ID, escapedRemaining, message, binary);

            // 0x00 (mystery) AMF0 "d" (stream name)
            binary.put(DOWNSTREAM_MESSAGE_HEADER);

            // add the AMF3 ByteArray type markers and length
            binary.put(Amf0Utils.Type.AMF3.getCode());
            binary.put(Amf3Utils.Type.BYTEARRAY.getCode());
            Amf3Utils.encodeLengthWithLowFlag(binary, escapedRemaining);

            // add the escaped bytes
            binary.put(escapeBytes);

            // add the chunk header
            doEncodeChunk0Header(DOWNSTREAM_CHUNKSTREAM_ID, remaining, message, binary);

            // 0x00 (mystery) AMF0 "d" (stream name)
            binary.put(DOWNSTREAM_MESSAGE_HEADER);

            // add the AMF3 ByteArray type markers and length
            binary.put(Amf0Utils.Type.AMF3.getCode());
            binary.put(Amf3Utils.Type.BYTEARRAY.getCode());
            Amf3Utils.encodeLengthWithLowFlag(binary, remaining);

            // skip ByteArray data
            binary.skip(remaining);

            // equivalent of flip, but not to zero position
            binary.limit(binary.position());
            binary.reset();

            // write the message
            return binary;
        } else {

            ByteBuffer escapeBuf = ByteBuffer.wrap(escapeBytes);
            IoBufferEx escapeMessageBuffer = reallocatingBinaryEncode(allocator, allocator.wrap(escapeBuf), message, maximumChunkSize);
            IoBufferEx binary = reallocatingBinaryEncode(allocator, buf, message, maximumChunkSize);

            ByteBuffer finalNioBuffer = allocator.allocate(escapeMessageBuffer.remaining()+binary.remaining());
            IoBufferEx finalBuffer = allocator.wrap(finalNioBuffer);
            finalBuffer.put(escapeMessageBuffer).put(binary);
            finalBuffer.flip();
            return finalBuffer;
        }

    }


    public static IoBufferEx doBinaryEncode(final IoBufferAllocatorEx<?> allocator, IoBufferEx buf,
                                          RtmpDataMessage message, final int maximumChunkSize) {
        int remaining = buf.remaining();
        int chunkOffset = 1 + 3 + 3 + 1 + 4;
        int streamOffset = 5 + 1 + 1 + WsUtils.calculateEncodedLengthSize(remaining << 1);
        int binaryOffset = chunkOffset + streamOffset;

        if (buf.position() > binaryOffset && streamOffset + remaining <= maximumChunkSize) {

            // Note: duplicate first to represent different transport layer (no parallel encoding)
            IoBufferEx binary = buf.duplicate();
            binary.skip(-binaryOffset);

            // remember start of binary frame
            binary.mark();

            // add the chunk header
            doEncodeChunk0Header(DOWNSTREAM_CHUNKSTREAM_ID, remaining, message, binary);

            // 0x00 (mystery) AMF0 "d" (stream name)
            binary.put(DOWNSTREAM_MESSAGE_HEADER);

            // add the AMF3 ByteArray type markers and length
            binary.put(Amf0Utils.Type.AMF3.getCode());
            binary.put(Amf3Utils.Type.BYTEARRAY.getCode());
            Amf3Utils.encodeLengthWithLowFlag(binary, buf.limit() - buf.position());

            // skip ByteArray data
            binary.skip(remaining);

            // equivalent of flip, but not to zero position
            binary.limit(binary.position());
            binary.reset();

            // write the message
            return binary;
        } else {

            return reallocatingBinaryEncode(allocator, buf, message, maximumChunkSize);
        }
    }

    private static IoBufferEx reallocatingBinaryEncode(IoBufferAllocatorEx<?> allocator, IoBufferEx buf, RtmpDataMessage message, int maximumChunkSize) {
        IoBufferEx binary = allocator.wrap(allocator.allocate(1000));
        binary.setAutoExpander(allocator);

        // 0x00 (mystery) AMF0 "d" (stream name)
        binary.put(DOWNSTREAM_MESSAGE_HEADER);

        if (buf.isShared()) {
            Amf0Utils.encodeByteArray(binary, buf.duplicate());
        }
        else {
            int pos = buf.position();
            Amf0Utils.encodeByteArray(binary, buf);
            buf.position(pos);
        }
        binary.flip();

        return doEncodeChunk0Old(allocator, DOWNSTREAM_CHUNKSTREAM_ID, binary, message, maximumChunkSize);
    }

    private static IoBufferEx doEncodeChunk0Old(final IoBufferAllocatorEx<?> allocator, int chunkStreamId, IoBufferEx messageBuffer,
			RtmpStreamMessage message, int maximumChunkSize) {
		IoBufferEx buf = allocator.wrap(allocator.allocate(1000));
		buf.setAutoExpander(allocator);

		int streamMessageLength = messageBuffer.remaining();
		doEncodeChunk0Header(chunkStreamId, streamMessageLength, message, buf);

		// write message

		// chunk every "maximum-chunk-length" bytes
		while (messageBuffer.remaining() > 0) {
			if (messageBuffer.remaining() <= maximumChunkSize) {
				buf.put(messageBuffer);
			} else {
				IoBufferEx part = messageBuffer.getSlice(maximumChunkSize);
				buf.put(part);
				doEncodeChunk3Format(chunkStreamId, buf);
			}
		}

		buf.flip();
		return buf;
	}

    static void doEncodeChunk0Header(int chunkStreamId, int streamMessageLength,
                                     RtmpStreamMessage message, IoBufferEx buf) {
        // chunk stream Id
        // TODO rtmp: multiple stream ids
        buf.put((byte) chunkStreamId);

        // write chunk basic header
        // timestamp
        // TODO rtmp: use real timestamp
        buf.putMediumInt(message.getTimestamp());
        // length
        buf.putMediumInt(streamMessageLength);
        // type
        buf.put(message.getStreamKind().getCode());
        // message stream Id
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(message.getMessageStreamId());
        buf.order(ByteOrder.BIG_ENDIAN);
    }

    static void doEncodeChunk3Format(int chunkStreamId, IoBufferEx buf) {
        buf.put((byte) (0xc0 | chunkStreamId));
    }
}
