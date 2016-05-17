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

import org.kaazing.gateway.transport.http.bridge.filter.HttpBuffer.HttpSharedBuffer;
import org.kaazing.gateway.transport.http.bridge.filter.HttpBuffer.HttpUnsharedBuffer;
import org.kaazing.mina.core.buffer.AbstractIoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;

public final class HttpBufferAllocator extends AbstractIoBufferAllocatorEx<HttpBuffer> {

    private final IoBufferAllocatorEx<?> parent;
    
    public HttpBufferAllocator(IoBufferAllocatorEx<?> parent) {
        this.parent = parent;
    }
    
    @Override
    public ByteBuffer allocate(int capacity, int flags) {
        boolean offset = (flags & IoBufferEx.FLAG_ZERO_COPY) != IoBufferEx.FLAG_NONE;
        if (offset) {
            // HEX(remaining) CRLF data CRLF
            // 0 CRLF CRLF
            int frameOffset = 10 + HttpGzipEncoder.GZIP_PREFIX_SIZE; // maximum 8 hex digits for 31-bit positive integer + 2 (CRLF) + 5 bytes for potential gzip frame prefix 
            int framePadding = 2 + 5; // CRLF, 0 CRLF CRLF
            ByteBuffer buf = parent.allocate(frameOffset + capacity + framePadding, flags);
            buf.position(buf.position() + frameOffset);
            buf.limit(buf.position() + capacity);
            assert (buf.remaining() == capacity);
            return buf;
        }
        else {
            // no offset
            return parent.allocate(capacity, flags);
        }
    }

    @Override
    public HttpBuffer wrap(ByteBuffer nioBuffer, int flags) {
        boolean shared = (flags & IoBufferEx.FLAG_SHARED) != IoBufferEx.FLAG_NONE;
        return shared ? new HttpSharedBuffer(nioBuffer) : new HttpUnsharedBuffer(nioBuffer);
    }

}