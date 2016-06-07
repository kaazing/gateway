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
package org.kaazing.gateway.transport.ws.bridge.filter;

import static org.kaazing.mina.core.buffer.IoBufferEx.FLAG_ZERO_COPY;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentMap;

import org.kaazing.gateway.transport.bridge.Message;
import org.kaazing.gateway.transport.ws.WsMessage;
import org.kaazing.gateway.transport.ws.util.WsUtils;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;

/**
 * A shared helper class to expose the ability to encode messages outside the frame encoders.
 *
 */
public class WsDraftHixieFrameEncodingSupport {
     static final byte TEXT_TYPE_BYTE = (byte) 0x00;
     static final byte BINARY_TYPE_BYTE = (byte) 0x80;
     static final byte SPECIFIED_LENGTH_TEXT_TYPE_BYTE = (byte) 0x81;
     static final byte TEXT_TERMINATOR_BYTE = (byte) 0xff;
     static final byte CLOSE_TYPE_BYTE = (byte) 0xff;
     static final byte CLOSE_TERMINATOR_BYTE = (byte) 0x00;

    public static IoBufferEx doTextEncode(IoBufferAllocatorEx<?> allocator, int flags, WsMessage message) {
        IoBufferEx ioBuf = message.getBytes();
        ByteBuffer buf = ioBuf.buf();
        int textOffset = 1;
        int textPadding = 1;
        int position = buf.position();

        if (((flags & FLAG_ZERO_COPY) != 0) &&
            (position >= textOffset) &&
            ((buf.capacity() - buf.limit()) >= textPadding)) {
            if (!isCacheEmpty(message)) {
                throw new IllegalStateException("Cache must be empty: flags = " + flags);
            }

            // Note: duplicate first to represent different transport layer (no parallel encoding)
            int remaining = buf.remaining();
            ByteBuffer text = buf.duplicate();
            text.position(position - textOffset);
            text.limit(text.limit() + textPadding);
            text.mark();
            text.put(TEXT_TYPE_BYTE);
            text.position(text.position() + remaining);
            text.put(TEXT_TERMINATOR_BYTE);
            text.reset();
            return allocator.wrap(text, flags);
        }
        else {
            ByteBuffer text = allocator.allocate(buf.remaining() + 2, flags);
            int offset = text.position();
            text.put(TEXT_TYPE_BYTE);

            // (KG-8125) if shared, duplicate to ensure we don't affect other threads
            if (ioBuf.isShared()) {
                text.put(buf.duplicate());
            }
            else {
                int bufPos = buf.position();
                text.put(buf);
                buf.position(bufPos);
            }

            text.put(TEXT_TERMINATOR_BYTE);
            text.flip();
            text.position(offset);
            return allocator.wrap(text, flags);
        }
    }

    public static IoBufferEx doSpecifiedLengthTextEncode(IoBufferAllocatorEx<?> allocator, int flags, WsMessage message) {
        return doEncode(allocator, flags, message, SPECIFIED_LENGTH_TEXT_TYPE_BYTE);
    }

    public static IoBufferEx doBinaryEncode(IoBufferAllocatorEx<?> allocator, int flags, WsMessage message) {
        return doEncode(allocator, flags, message, BINARY_TYPE_BYTE);
    }

    private static IoBufferEx doEncode(IoBufferAllocatorEx<?> allocator, int flags, WsMessage message, byte opCode) {
        IoBufferEx ioBuf = message.getBytes();
        ByteBuffer buf = ioBuf.buf();
        int position = buf.position();
        int remaining = buf.remaining();
        int binaryOffset = 1 + WsUtils.calculateEncodedLengthSize(remaining);

        if (((flags & FLAG_ZERO_COPY) != 0) && (position >= binaryOffset)) {
            if (!isCacheEmpty(message)) {
                throw new IllegalStateException("Cache must be empty: flags = " + flags);
            }

            // Note: duplicate first to represent different transport layer (no parallel encoding)
            ByteBuffer binary = buf.duplicate();
            binary.position(position - binaryOffset);
            binary.mark();
            binary.put(opCode);
            WsUtils.encodeLength(binary, remaining);
            binary.position(binary.position() + remaining);
            binary.limit(binary.position());
            binary.reset();
            return allocator.wrap(binary, flags);
        }
        else {
            ByteBuffer binary = allocator.allocate(binaryOffset + remaining, flags);
            int offset = binary.position();
            binary.put(opCode);
            WsUtils.encodeLength(binary, remaining);

            // (KG-8125) if shared, duplicate to ensure we don't affect other threads
            if (ioBuf.isShared()) {
                binary.put(buf.duplicate());
            }
            else {
                int bufPos = buf.position();
                binary.put(buf);
                buf.position(bufPos);
            }

            binary.flip();
            binary.position(offset);
            return allocator.wrap(binary, flags);
        }
    }

    public static IoBufferEx doCloseEncode(IoBufferAllocatorEx<?> allocator, int flags) {
        // Draft Hixie has no notion of status or reason
        ByteBuffer close = allocator.allocate(2, flags);
        int offset = close.position();
        close.put(CLOSE_TYPE_BYTE);
        close.put(CLOSE_TERMINATOR_BYTE);
        close.flip();
        close.position(offset);
        return allocator.wrap(close, flags);
    }

    private  static boolean isCacheEmpty(Message message) {
        boolean emptyCache = true;

        if (message.hasCache()) {
            ConcurrentMap<String, IoBufferEx> cache = message.getCache();
            emptyCache = cache.isEmpty();
        }

        return emptyCache;
    }
}
