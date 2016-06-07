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

import static org.kaazing.gateway.transport.bridge.CachingMessageEncoder.IO_MESSAGE_ENCODER;

import java.security.SecureRandom;

import org.kaazing.gateway.transport.bridge.CachingMessageEncoder;
import org.kaazing.gateway.transport.ws.WsCloseMessage;
import org.kaazing.gateway.transport.ws.WsMessage;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;

public class WsFrameEncoder extends AbstractWsFrameEncoder {
    
    private final boolean maskSends;
    private static SecureRandom prng;
    
    static {
        // KG-3433: although native ("NativePRNG") is faster than "SHA1PRNG" (which is typically the default)
        // we now always use the platform default, to avoid getting exception on platforms like Windows 7 
        // which do not have NativePRNG and to give us a leeway to make it configurable in a later
        // patch release (see KG-3403). 
        prng = new SecureRandom();
    }
    
    public WsFrameEncoder(IoBufferAllocatorEx<?> allocator, boolean maskSends) {
        this(IO_MESSAGE_ENCODER, allocator, maskSends);
    }
    
    public WsFrameEncoder(CachingMessageEncoder cachingEncoder, IoBufferAllocatorEx<?> allocator, boolean maskSends) {
        super(cachingEncoder, allocator);
        this.maskSends = maskSends; 
    }

    @Override
    protected IoBufferEx doTextEncode(IoBufferAllocatorEx<?> allocator, int flags, WsMessage message) {
        return doMessageEncode(allocator, flags, message);
    }

    @Override
    protected IoBufferEx doContinuationEncode(IoBufferAllocatorEx<?> allocator, int flags, WsMessage message) {
        return doMessageEncode(allocator, flags, message);
    }

    @Override
    protected IoBufferEx doBinaryEncode(IoBufferAllocatorEx<?> allocator, int flags, WsMessage message) {
        return doMessageEncode(allocator, flags, message);
    }

    @Override
    protected IoBufferEx doCloseEncode(IoBufferAllocatorEx<?> allocator, int flags, WsCloseMessage message) {
        return doMessageEncode(allocator, flags, message);
    }
    
    private IoBufferEx doMessageEncode(IoBufferAllocatorEx<?> allocator, int flags, WsMessage message) {
        if (maskSends) {
            return WsFrameEncodingSupport.doEncode(allocator, flags, message, prng.nextInt());
        } else {
        return WsFrameEncodingSupport.doEncode(allocator, flags, message);
        }
    }
}
