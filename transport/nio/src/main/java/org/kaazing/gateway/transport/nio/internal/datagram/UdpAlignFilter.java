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
package org.kaazing.gateway.transport.nio.internal.datagram;

import java.util.Arrays;

import org.apache.mina.core.session.IoSession;
import org.kaazing.gateway.transport.IoFilterAdapter;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.slf4j.Logger;

/**
 * Adds padding with 0x00 to any message in order to have its length multiple of the configured 'udp.align'
 *     - accept.option
 *     - connect.option
 * The padding is a 'right pad', meaning bytes are inserted after the message's content.
 */
public class UdpAlignFilter extends IoFilterAdapter<IoSessionEx> {

    private final Logger logger;
    private final Integer align;

    public UdpAlignFilter(Logger logger, Integer align, IoSession session) {
        this.logger = logger;
        this.align = align;
    }

    @Override
    protected void doMessageReceived(NextFilter nextFilter, IoSessionEx session, Object message) throws Exception {
        if (message instanceof IoBufferEx) {
            IoBufferEx buffer = (IoBufferEx) message;
            if (buffer.remaining() % this.align > 0) {
                if (logger.isTraceEnabled()) {
                    logger.trace(String.format("Received message with length: %d, aligning on %d bytes", buffer.remaining(), align));
                }
                final int padLength = this.align - (buffer.remaining() % this.align);
                final byte[] padding = new byte[padLength];
                Arrays.fill(padding, (byte) 0x00);
                final IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
                IoBufferEx alignedBuffers = allocator.wrap(allocator.allocate(buffer.remaining() + padLength));
                alignedBuffers.put(buffer);
                alignedBuffers.put(padding);
                message = alignedBuffers.flip();
            }
        }
        super.doMessageReceived(nextFilter, session, message);
    }
}
