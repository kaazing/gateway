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
package org.kaazing.gateway.transport.io.filter;

import static org.kaazing.gateway.transport.bridge.CachingMessageEncoder.IO_MESSAGE_ENCODER;
import static org.kaazing.mina.core.buffer.IoBufferEx.FLAG_SHARED;
import static org.kaazing.mina.core.buffer.IoBufferEx.FLAG_ZERO_COPY;

import java.nio.ByteBuffer;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

import org.kaazing.gateway.transport.bridge.CachingMessageEncoder;
import org.kaazing.gateway.transport.bridge.MessageEncoder;
import org.kaazing.gateway.transport.io.IoMessage;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;

public class IoMessageEncoder extends ProtocolEncoderAdapter {

    private final CachingMessageEncoder cachingEncoder;
    private final IoBufferAllocatorEx<?> allocator;
    private final MessageEncoderImpl encoder;

    public IoMessageEncoder(IoBufferAllocatorEx<?> allocator) {
        this(IO_MESSAGE_ENCODER, allocator);
    }
    
    public IoMessageEncoder(CachingMessageEncoder cachingEncoder, IoBufferAllocatorEx<?> allocator) {
        this.cachingEncoder = cachingEncoder;
        this.allocator = allocator;
        this.encoder = new MessageEncoderImpl();
    }

    @Override
    public void encode(IoSession session, Object in, ProtocolEncoderOutput out) throws Exception {
        IoMessage message = (IoMessage)in;
        if (message.hasCache()) {
            // shared
            IoBufferEx buf = cachingEncoder.encode(encoder, message, allocator, FLAG_SHARED | FLAG_ZERO_COPY);
            out.write(buf);
        }
        else {
            // not shared
            IoBufferEx buf = message.getBuffer();
            out.write(buf);
        }
    }

    private class MessageEncoderImpl implements MessageEncoder<IoMessage> {

        @Override
        public IoBufferEx encode(IoBufferAllocatorEx<?> allocator, IoMessage message, int flags) {
            IoBufferEx buffer = message.getBuffer();
            ByteBuffer buf = buffer.buf();
            
            if ((flags & FLAG_ZERO_COPY) != 0) {
                return allocator.wrap(buffer.buf(), flags);
            }
            
            // ### TODO: Not sure if there is any offset that needs to be taken
            //           into account while calculating the capacity.
            ByteBuffer b = allocator.allocate(buffer.remaining(), flags);
            int start = b.position();
            b.put(buf.duplicate());
            b.limit(b.position());
            b.position(start);

            return allocator.wrap(b, flags);
        }
    }
}
