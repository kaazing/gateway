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
import org.kaazing.gateway.transport.ws.extension.ActiveExtensions;
import org.kaazing.gateway.transport.ws.extension.EscapeSequencer;
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

    protected ActiveExtensions extensions;

    private final CachingMessageEncoder cachingEncoder;

    public void setExtensions(ActiveExtensions extensions) {
        this.extensions = extensions;
    }

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

        IoBufferEx buf = message.getBytes();
        byte[] escapeBytes = null;
        boolean escaping = false;
        // TODO: fix WsMessage inheritance hierarchy so WsCommandMessage doesn't have getBytes method
        if (buf != null && extensions != null ) {
            EscapeSequencer escapeSequencer = extensions.getEscapeSequencer(message.getKind());
            if (escapeSequencer != null) {
                escapeBytes = escapeSequencer.getEscapeBytes(buf);
                escaping = escapeBytes.length > 0;
            }
        }

        switch (message.getKind()) {
            case CONTINUATION:
                if (escaping) {
                    return doContinuationEscapedEncode(allocator, flags, message, escapeBytes);
                } else {
                    return doContinuationEncode(allocator, flags, message);
                }
            case BINARY: {
                if (escaping) {
                    return doBinaryEscapedEncode(allocator, flags, message, escapeBytes);
                } else {
                    return doBinaryEncode(allocator, flags, message);
                }
            }
            case TEXT: {
                if (escaping) {
                    return doTextEscapedEncode(allocator, flags, message, escapeBytes);
                } else {
                    return doTextEncode(allocator, flags, message);
                }
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

    protected abstract IoBufferEx doTextEscapedEncode(IoBufferAllocatorEx<?> allocator, int flags, WsMessage message, byte[] escapedBytes) ;

    protected abstract IoBufferEx doContinuationEscapedEncode(IoBufferAllocatorEx<?> allocator, int flags, WsMessage message, byte[] escapedBytes) ;

    protected abstract IoBufferEx doBinaryEscapedEncode(IoBufferAllocatorEx<?> allocator, int flags, WsMessage message, byte[] escapedBytes) ;

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
