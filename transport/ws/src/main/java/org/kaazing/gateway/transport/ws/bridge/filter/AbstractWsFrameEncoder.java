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

import static org.kaazing.mina.core.buffer.IoBufferEx.FLAG_SHARED;
import static org.kaazing.mina.core.buffer.IoBufferEx.FLAG_ZERO_COPY;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.kaazing.gateway.transport.bridge.CachingMessageEncoder;
import org.kaazing.gateway.transport.bridge.MessageEncoder;
import org.kaazing.gateway.transport.ws.WsCloseMessage;
import org.kaazing.gateway.transport.ws.WsMessage;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;

/**
 * Shared capability amongst native and emulated WebSocket frame encoders.
 */
public abstract class AbstractWsFrameEncoder extends ProtocolEncoderAdapter {

    /** Required for encoding terminators for WSEB command messages */
    protected static final byte TEXT_TERMINATOR_BYTE = WsDraftHixieFrameEncodingSupport.TEXT_TERMINATOR_BYTE;

    protected final IoBufferAllocatorEx<?> allocator;
    protected final MessageEncoder<WsMessage> encoder;

    private final CachingMessageEncoder cachingEncoder;

    public AbstractWsFrameEncoder(CachingMessageEncoder cachingEncoder, IoBufferAllocatorEx<?> allocator) {
        this.allocator = allocator;
        this.cachingEncoder = cachingEncoder;
        this.encoder = new WsMessageEncoderImpl();

    }

    @Override
    public void encode(IoSession session, Object message,
                       ProtocolEncoderOutput out) throws Exception {

        WsMessage wsMessage = (WsMessage) message;
        if (wsMessage.hasCache()) {
            IoBufferEx buf = cachingEncoder.encode(encoder, wsMessage, allocator, FLAG_SHARED | FLAG_ZERO_COPY);
            out.write(buf);
        } else {
            IoBufferEx buf = doEncode(allocator, FLAG_ZERO_COPY, wsMessage);
            out.write(buf);
        }
    }

    protected IoBufferEx doEncode(IoBufferAllocatorEx<?> allocator, int flags, WsMessage message) {

        switch (message.getKind()) {
            case CONTINUATION:
                return doContinuationEncode(allocator, flags, message);
            case BINARY: {
                return doBinaryEncode(allocator, flags, message);
            }
            case TEXT: {
                return doTextEncode(allocator, flags, message);
            }
            case PING: {
                return doBinaryEncode(allocator, flags, message);
            }
            case PONG: {
                 return doBinaryEncode(allocator, flags, message);
            }
            case CLOSE: {
                return doCloseEncode(allocator, flags, (WsCloseMessage)message);
            }
            default:
                throw new IllegalStateException("Unrecognized frame type: " + message.getKind());
        }
    }

    protected abstract IoBufferEx doTextEncode(IoBufferAllocatorEx<?> allocator, int flags, WsMessage message) ;

    protected abstract IoBufferEx doContinuationEncode(IoBufferAllocatorEx<?> allocator, int flags, WsMessage message) ;

    protected abstract IoBufferEx doBinaryEncode(IoBufferAllocatorEx<?> allocator, int flags, WsMessage message) ;

    protected abstract IoBufferEx doCloseEncode(IoBufferAllocatorEx<?> allocator, int flags, WsCloseMessage message) ;

    protected final class WsMessageEncoderImpl implements MessageEncoder<WsMessage> {
        @Override
        public IoBufferEx encode(IoBufferAllocatorEx<?> allocator, WsMessage message, int flags) {
            return doEncode(allocator, flags, message);
        }
    }
}
