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

import java.nio.ByteBuffer;

import org.kaazing.gateway.transport.ws.bridge.filter.WsBuffer.WsSharedBuffer;
import org.kaazing.gateway.transport.ws.bridge.filter.WsBuffer.WsUnsharedBuffer;
import org.kaazing.mina.core.buffer.AbstractIoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;

public class WsDraftHixieBufferAllocator extends AbstractIoBufferAllocatorEx<WsBuffer> {

    private static final int FRAME_OFFSET = 14;
    private static final int FRAME_PADDING = 1;

    private final IoBufferAllocatorEx<?> parent;

    public WsDraftHixieBufferAllocator(IoBufferAllocatorEx<?> parent) {
        this.parent = parent;
    }

    @Override
    public ByteBuffer allocate(int capacity, int flags) {
        boolean offset = (flags & IoBufferEx.FLAG_ZERO_COPY) != IoBufferEx.FLAG_NONE;
        if (offset) {
            ByteBuffer buf = parent.allocate(FRAME_OFFSET + capacity + FRAME_PADDING, flags);
            buf.position(buf.position() + FRAME_OFFSET);
            buf.limit(buf.position() + capacity);
            return buf;
        }
        else {
            // no offset
            return parent.allocate(capacity, flags);
        }
    }

    @Override
    public WsBuffer wrap(ByteBuffer nioBuffer, int flags) {
        boolean shared = (flags & IoBufferEx.FLAG_SHARED) != IoBufferEx.FLAG_NONE;
        return shared ? new WsSharedBuffer(nioBuffer) : new WsUnsharedBuffer(nioBuffer);
    }

}