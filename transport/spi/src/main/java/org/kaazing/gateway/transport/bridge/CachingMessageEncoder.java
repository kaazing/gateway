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
package org.kaazing.gateway.transport.bridge;

import static org.kaazing.mina.core.buffer.IoBufferEx.FLAG_ZERO_COPY;

import java.util.concurrent.ConcurrentMap;

import org.kaazing.gateway.transport.BridgeSession;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.session.IoSessionEx;

public abstract class CachingMessageEncoder {

    public abstract <T extends Message> IoBufferEx encode(MessageEncoder<T> encoder, T message, IoBufferAllocatorEx<?> allocator, int flags);

    protected final <T extends Message> IoBufferEx encode(String cacheKey, MessageEncoder<T> encoder, T message, IoBufferAllocatorEx<?> allocator, int flags) {
        ConcurrentMap<String, IoBufferEx> cache = message.getCache();
        
        // if cache is not initialized, throw exception (caller should guard)
        if (cache == null) {
            throw new IllegalStateException("Cache not initialized");
        }

        // lookup existing cached encoding entry
        IoBufferEx cachedBuffer = cache.get(cacheKey);
        if (cachedBuffer == null) {
            // when cachedBuffer is null, perform encode, then cache the result
            // with standard atomic race condition awareness (put-if-absent)            
            if ((flags & FLAG_ZERO_COPY) != 0) {
                if (!cache.isEmpty()) {
                    flags &= ~FLAG_ZERO_COPY;
                }
            }

            IoBufferEx newCachedBuffer = encoder.encode(allocator, message, flags);
            if (newCachedBuffer instanceof MessageBuffer<?>) {
                MessageBuffer<?> cacheableBuffer = (MessageBuffer<?>) newCachedBuffer;
                cacheableBuffer.setAutoCache(true);
            }
            cachedBuffer = cache.putIfAbsent(cacheKey, newCachedBuffer);
            if (cachedBuffer == null) {
                cachedBuffer = newCachedBuffer;
            }
        }

        return cachedBuffer;
    }

    public static CachingMessageEncoder getMessageEncoder(IoSessionEx session) {
        return (session instanceof BridgeSession) ? ((BridgeSession)session).getMessageEncoder() : IO_MESSAGE_ENCODER;
    }

    public static final CachingMessageEncoder IO_MESSAGE_ENCODER = new CachingMessageEncoder() {

        @Override
        public <T extends Message> IoBufferEx encode(MessageEncoder<T> encoder, T message, IoBufferAllocatorEx<?> allocator, int flags) {
            return encode("io", encoder, message, allocator, flags);
        }

    };
}
