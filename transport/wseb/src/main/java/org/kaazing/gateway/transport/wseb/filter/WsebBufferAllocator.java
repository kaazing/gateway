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
package org.kaazing.gateway.transport.wseb.filter;

import java.nio.ByteBuffer;

import org.kaazing.gateway.transport.ws.bridge.filter.WsBuffer;
import org.kaazing.gateway.transport.ws.bridge.filter.WsBuffer.WsSharedBuffer;
import org.kaazing.gateway.transport.ws.bridge.filter.WsBuffer.WsUnsharedBuffer;
import org.kaazing.mina.core.buffer.AbstractIoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;

public final class WsebBufferAllocator extends AbstractIoBufferAllocatorEx<WsBuffer> {

    private final IoBufferAllocatorEx<?> parent;

    public WsebBufferAllocator(IoBufferAllocatorEx<?> parent) {
        this.parent = parent;
    }

    @Override
    public ByteBuffer allocate(int capacity, int flags) {
        boolean offset = (flags & IoBufferEx.FLAG_ZERO_COPY) != IoBufferEx.FLAG_NONE;
        if (offset) {
            // 3.5 clients receive TEXT messages as BINARY messages with UTF-8 payload
            // 4.0 clients receive TEXT messages in BINARY message syntax, but opcode 0x81
            // 4.0 clients receive BINARY messages in BINARY message syntax, with opcode 0x80

            // [0x80 | 0x81] [length] [binary]
            // Note: 7 bits per byte to represent length
            int shiftedCapacity = capacity;
            int sizeOfLength = 0;
            do {
                sizeOfLength ++;
            } while ((shiftedCapacity >>= 7) > 0);

            int frameOffset = 1 + sizeOfLength; 
            ByteBuffer buf = parent.allocate(frameOffset + capacity, flags);
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
    public WsBuffer wrap(ByteBuffer buf, int flags) {
        boolean shared = (flags & IoBufferEx.FLAG_SHARED) != IoBufferEx.FLAG_NONE;
        return shared ? new WsSharedBuffer(buf) : new WsUnsharedBuffer(buf);
    }

}