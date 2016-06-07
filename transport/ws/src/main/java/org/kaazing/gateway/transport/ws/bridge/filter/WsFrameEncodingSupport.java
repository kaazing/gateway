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

import static org.kaazing.mina.core.buffer.IoBufferEx.FLAG_ZERO_COPY;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentMap;

import org.kaazing.gateway.transport.bridge.Message;
import org.kaazing.gateway.transport.ws.WsCloseMessage;
import org.kaazing.gateway.transport.ws.WsMessage;
import org.kaazing.gateway.transport.ws.WsMessage.Kind;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;

public class WsFrameEncodingSupport {

    /**
	 * Encode WebSocket message as a single frame, with the provided masking value applied.
	 */
    public static IoBufferEx doEncode(IoBufferAllocatorEx<?> allocator, int flags, WsMessage message, int maskValue) {
        final boolean mask = true;

        IoBufferEx ioBuf = getBytes(allocator, flags, message);
        ByteBuffer buf = ioBuf.buf();

    	boolean fin = message.isFin();

    	int remaining = buf.remaining();

    	int offset = 2 + (mask ? 4 : 0) + calculateLengthSize(remaining);
    		ByteBuffer b = allocator.allocate(offset + remaining, flags);

		int start = b.position();

		byte b1 = (byte) (fin ? 0x80 : 0x00);
		byte b2 = (byte) (mask ? 0x80 : 0x00);

		b1 = doEncodeOpcode(b1, message);
		b2 |= lenBits(remaining);

		b.put(b1).put(b2);

		doEncodeLength(b, remaining);

		if (mask) {
			b.putInt(maskValue);
		}

		if ( mask ) {
            WsFrameUtils.xor(buf, b, maskValue);
        }
            // reset buffer position after write in case of reuse
            // (KG-8125) if shared, duplicate to ensure we don't affect other threads
            if (ioBuf.isShared()) {
                b.put(buf.duplicate());
            }
            else {
                int bufPos = buf.position();
                b.put(buf);
                buf.position(bufPos);
            }
            b.limit(b.position());
            b.position(start);
    		return allocator.wrap(b, flags);
	}

    /**
	 * Encode WebSocket message as a single frame
	 */
    public static IoBufferEx doEncode(IoBufferAllocatorEx<?> allocator, int flags, WsMessage message) {

        IoBufferEx ioBuf = getBytes(allocator, flags, message);
    	ByteBuffer buf = ioBuf.buf();

    	boolean mask = false; // FIXME enable masking for WsnConnector
    	boolean fin = message.isFin();
    	int maskValue = 0;	  // TODO random mask

    	int remaining = buf.remaining();
    	int position = buf.position();

    	int offset = 2 + (mask ? 4 : 0) + calculateLengthSize(remaining);
        if (((flags & FLAG_ZERO_COPY) != 0) && (position >= offset)) {
            if (!isCacheEmpty(message)) {
                throw new IllegalStateException("Cache must be empty: flags = " + flags);
            }

            // TODO: duplicate implicit for non-aligned (master -> thread-local)
            //       duplicate explicit for aligned to prevent exposure of
            //         position / limit changes to higher level transports

            ByteBuffer b = buf.duplicate();
            b.position(position - offset);
            b.mark();
    		byte b1 = (byte) (fin ? 0x80 : 0x00);
    		byte b2 = (byte) (mask ? 0x80 : 0x00);

    		b1 = doEncodeOpcode(b1, message);
    		b2 |= lenBits(remaining);

    		b.put(b1).put(b2);

    		doEncodeLength(b, remaining);

            if (mask) {
            	b.putInt(maskValue);
            }
            b.position(b.position() + remaining);
            b.limit(b.position());
            b.reset();
            return allocator.wrap(b, flags);
    	} else {
    		ByteBuffer b = allocator.allocate(offset + remaining, flags);

    		int start = b.position();

    		byte b1 = (byte) (fin ? 0x80 : 0x00);
    		byte b2 = (byte) (mask ? 0x80 : 0x00);

    		b1 = doEncodeOpcode(b1, message);
    		b2 |= lenBits(remaining);

    		b.put(b1).put(b2);

    		doEncodeLength(b, remaining);

    		if (mask) {
    			b.putInt(maskValue);
    		}

            // reset buffer position after write in case of reuse
            // (KG-8125) if shared, duplicate to ensure we don't affect other threads
            if (ioBuf.isShared()) {
                b.put(buf.duplicate());
            }
            else {
                int bufPos = buf.position();
                b.put(buf);
                buf.position(bufPos);
            }

    		b.limit(b.position());
    		b.position(start);
            return allocator.wrap(b, flags);
    	}
	}


    protected enum Opcode {
    	CONTINUATION(0),
    	TEXT(1),
    	BINARY(2),
    	RESERVED3(3), RESERVED4(4), RESERVED5(5), RESERVED6(6), RESERVED7(7),
    	CLOSE(8),
    	PING(9),
    	PONG(10);

    	private int code;

    	public int getCode() {
    		return this.code;
    	}
    	
    	public static Opcode valueOf(int code) {
    	    switch (code) {
    	    case 0:
    	        return CONTINUATION;
    	    case 1:
    	        return TEXT;
    	    case 2:
    	        return BINARY;
            case 3:
                return RESERVED3;
            case 4:
                return RESERVED4;
            case 5:
                return RESERVED5;
            case 6:
                return RESERVED6;
            case 7:
                return RESERVED7;
            case 8:
                return CLOSE;
            case 9:
                return PING;
            case 10:
                return PONG;
            default:
                throw new IllegalArgumentException("Unrecognized WebSocket frame opcode: " + code);
    	    }
    	}

    	Opcode(int code) {
    		this.code = code;
    	}
    }

    private static int calculateLengthSize(int length) {
        if (length < 126) {
        	return 0;
        } else if (length < 65535) {
        	return 2;
        } else {
        	return 8;
        }
	}


    /**
     * Encode a WebSocket opcode onto a byte that might have some high bits set.
     *
     * @param b
     * @param message
     * @return
     */
    private static byte doEncodeOpcode(byte b, WsMessage message) {
        Kind kind = message.getKind();

        switch (kind) {
        case CONTINUATION:
            b |= Opcode.CONTINUATION.getCode();
            break;
        case TEXT: {
        	b |= Opcode.TEXT.getCode();
        	break;
        }
        case BINARY: {
        	b |= Opcode.BINARY.getCode();
        	break;
        }
        case PING: {
        	b |= Opcode.PING.getCode();
        	break;
        }
        case PONG: {
        	b |= Opcode.PONG.getCode();
        	break;
        }
        case CLOSE: {
        	b |= Opcode.CLOSE.getCode();
        	break;
        }
        default:
            throw new IllegalStateException("Unrecognized frame type: " + message.getKind());
        }
        return b;
    }

    private static byte lenBits(int length) {
        if (length < 126) {
        	return (byte) length;
        } else if (length <= 0xFFFF) {
        	return (byte) 126;
        } else {
        	return (byte) 127;
        }
    }

    private static void doEncodeLength(ByteBuffer buf, int length) {
        if (length < 126) {
        	return;
        } else if (length <= 0xFFFF) {
        	buf.putShort((short) length);
        } else {
        	// Unsigned long (should never have a message that large! really!)
        	buf.putLong((long) length);
        }
    }

    private static IoBufferEx getBytes(IoBufferAllocatorEx<?> allocator, int flags, WsMessage message) {   
        switch(message.getKind()) {
        case CLOSE:
            int length = 0;
            int status = ((WsCloseMessage)message).getStatus();
            ByteBuffer reason = ((WsCloseMessage)message).getReason();
            if (status != 1005) {
                length += 2;
            }
            if (reason != null) {
                length += reason.remaining();
            }
            ByteBuffer close = allocator.allocate(length, flags);
            int offset = close.position();
            if (status != 1005) {
                close.putShort((short)status);
            }
            if (reason != null) {
                close.put(reason);
                reason.flip();
            }
            close.flip();
            close.position(offset);
            return allocator.wrap(close, flags);
        default:
            return message.getBytes();
        }
    }

    private  static boolean isCacheEmpty(Message message) {
        boolean emptyCache = true;
        
        if (message.hasCache()) {
            ConcurrentMap<String, IoBufferEx> cache = message.getCache();
            emptyCache = cache.isEmpty();
        }

        return emptyCache;
    }

}
