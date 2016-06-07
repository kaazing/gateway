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
package org.kaazing.mina.netty.buffer;

import static java.lang.System.getProperty;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.jboss.netty.buffer.AbstractChannelBufferFactory;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferFactory;
import org.jboss.netty.buffer.ChannelBuffers;
import org.kaazing.mina.netty.util.threadlocal.VicariousThreadLocal;

public final class ByteBufferWrappingChannelBufferFactory extends AbstractChannelBufferFactory {

    public static final boolean OPTIMIZE_PERFORMANCE_CLIENT =
            Boolean.valueOf(getProperty("org.kaazing.mina.netty.OPTIMIZE_PERFORMANCE_CLIENT", "false"));
    public static final ChannelBufferFactory CHANNEL_BUFFER_FACTORY = new ByteBufferWrappingChannelBufferFactory();

    private final ThreadLocal<ByteBufferWrappingChannelBuffer> wrappingBufRef =
        new VicariousThreadLocal<ByteBufferWrappingChannelBuffer>() {

            @Override
            protected ByteBufferWrappingChannelBuffer initialValue() {
                return new ByteBufferWrappingChannelBuffer();
            }

        };

    @Override
    public ChannelBuffer getBuffer(ByteOrder order, int capacity) {
        ByteBufferWrappingChannelBuffer wrappingBuf = wrappingBufRef.get();
        return wrappingBuf;
    }

    @Override
    public ChannelBuffer getBuffer(ByteOrder order, byte[] array,
            int offset, int length) {
        return ChannelBuffers.wrappedBuffer(order, array, offset, length);
    }

    @Override
    public ChannelBuffer getBuffer(ByteBuffer nioBuffer) {
        ByteBufferWrappingChannelBuffer wrappingBuf = wrappingBufRef.get();
        return wrappingBuf.wrap(nioBuffer);
    }


}
