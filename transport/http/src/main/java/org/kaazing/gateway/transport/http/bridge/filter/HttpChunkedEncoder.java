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
package org.kaazing.gateway.transport.http.bridge.filter;

import java.nio.ByteBuffer;

import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;

public class HttpChunkedEncoder implements HttpContentWriter {

    /*
     * Chunked Transfer-Encoding
     */
    @Override
    public IoBufferEx write(IoBufferEx source, IoBufferAllocatorEx<?> allocator) {
        return writeChunked(source, allocator);
    }

    public int requiredPrefix(IoBufferEx buffer) {
        int remaining = buffer.remaining();
        String hexRemaining = Integer.toHexString(remaining);
        return hexRemaining.length() + 2;
    }

    private static final byte[] CRLF_BYTES = "\r\n".getBytes();
	public static final int CHUNKED_MAX_PREFIX_SIZE = 12;

    private static IoBufferEx writeChunked(IoBufferEx buffer, IoBufferAllocatorEx<?> allocator) {
        int remaining = buffer.remaining();

        // Non-zero buffer - chunk the buffer using pre-allocated frame prefix if available
        byte[] hexRemaining = Integer.toHexString(remaining).getBytes();
        int frameOffset = hexRemaining.length + 2;
        int framePadding = 2;
        if (buffer.position() >= frameOffset && (buffer.capacity() - buffer.limit()) >= framePadding) {
            buffer.skip(-frameOffset);
            buffer.limit(buffer.limit() + framePadding);
            int pos = buffer.position();

            if (remaining > 0) {
                buffer.put(hexRemaining);
                buffer.put(CRLF_BYTES);
                buffer.skip(remaining);
                buffer.put(CRLF_BYTES);
            }

            buffer.position(pos);
            return buffer;
        }
        else {
            // Not enough space pre-allocated - create a new buffer
            int capacity = hexRemaining.length + 2 + remaining + 2;

            ByteBuffer newBuffer = allocator.allocate(capacity);
            int position = newBuffer.position();
            if (remaining > 0) {
                newBuffer.put(hexRemaining);
                newBuffer.put(CRLF_BYTES);
                newBuffer.put(buffer.buf());
                newBuffer.put(CRLF_BYTES);
            }

            newBuffer.flip();
            newBuffer.position(position);
            return allocator.wrap(newBuffer);
        }
    }

}