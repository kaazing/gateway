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
package org.kaazing.gateway.transport.sse.bridge.filter;

import java.nio.ByteBuffer;

import org.kaazing.gateway.transport.sse.bridge.filter.SseBuffer.SseSharedBuffer;
import org.kaazing.gateway.transport.sse.bridge.filter.SseBuffer.SseUnsharedBuffer;
import org.kaazing.mina.core.buffer.AbstractIoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;

public final class SseBufferAllocator extends AbstractIoBufferAllocatorEx<SseBuffer> {

    private final IoBufferAllocatorEx<?> parent;

    public SseBufferAllocator(IoBufferAllocatorEx<?> parent) {
        this.parent = parent;
    }
    
    @Override
    public ByteBuffer allocate(int capacity, int flags) {
        boolean offset = (flags & IoBufferEx.FLAG_ZERO_COPY) != IoBufferEx.FLAG_NONE;
        if (offset) {
            // data: [data] 0x0a
            // 0x0a
            // Note: each newline in [data] is replaced by 6 bytes of meta data
            //       so zero-copy requires no newlines in data
            int frameOffset = 5; 
            int framePadding = 2;
            ByteBuffer buf = parent.allocate(frameOffset + capacity + framePadding, flags);
            buf.position(buf.position() + frameOffset);
            buf.limit(buf.position() + capacity);
            return buf;
        }
        else {
            // no offset
            return parent.allocate(capacity, flags);
        }
    }

    @Override
    public SseBuffer wrap(ByteBuffer nioBuffer, int flags) {
        boolean shared = (flags & IoBufferEx.FLAG_SHARED) != IoBufferEx.FLAG_NONE;
        return shared ? new SseSharedBuffer(nioBuffer) : new SseUnsharedBuffer(nioBuffer);
    }

}