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

import org.kaazing.gateway.transport.wsr.bridge.filter.WsrBuffer.WsrSharedBuffer;
import org.kaazing.gateway.transport.wsr.bridge.filter.WsrBuffer.WsrUnsharedBuffer;
import org.kaazing.mina.core.buffer.AbstractIoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;

public class WsrBufferAllocator extends AbstractIoBufferAllocatorEx<WsrBuffer> {

	private IoBufferAllocatorEx<?> parent;

    public WsrBufferAllocator(IoBufferAllocatorEx<?> parent) {
        this.parent = parent;
    }

	@Override
	public WsrBuffer wrap(ByteBuffer nioBuffer, int flags) {
        boolean shared = (flags & IoBufferEx.FLAG_SHARED) != IoBufferEx.FLAG_NONE;
		return shared ? new WsrSharedBuffer(nioBuffer) : new WsrUnsharedBuffer(nioBuffer);
	}
	
	@Override
	public ByteBuffer allocate(int capacity, int flags) {
        boolean offset = (flags & IoBufferEx.FLAG_ZERO_COPY) != IoBufferEx.FLAG_NONE;
        if (offset) {
            // chunk prefix:
            // 1 (chunk stream id)
            // 3 timestamp
            // 3 stream message length
            // 1 stream message kind
            // 4 (message stream id)
            // stream message prefix:
            // 1 + 2 + 1 ( AMF0 string type marker + unsigned short length + utf8('d') )
            // 1 (AMF0->AMF3 type marker + AMF3 ByteArray type marker 
            // 1 (0x0 mystery byte)
            // 4 maximum bytes for AMF3 U29B (length with low flag)
            int frameOffset = 1 + 3 + 3 + 1 + 4 + 1 + 1 + 4; 
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

}
