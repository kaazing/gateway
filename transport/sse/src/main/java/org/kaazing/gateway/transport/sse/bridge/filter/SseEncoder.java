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
package org.kaazing.gateway.transport.sse.bridge.filter;

import static org.kaazing.gateway.transport.bridge.CachingMessageEncoder.IO_MESSAGE_ENCODER;
import static org.kaazing.mina.core.buffer.IoBufferEx.FLAG_SHARED;
import static org.kaazing.mina.core.buffer.IoBufferEx.FLAG_ZERO_COPY;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.kaazing.gateway.transport.bridge.CachingMessageEncoder;
import org.kaazing.gateway.transport.bridge.MessageEncoder;
import org.kaazing.gateway.transport.http.bridge.filter.HttpGzipEncoder;
import org.kaazing.gateway.transport.sse.bridge.SseMessage;
import org.kaazing.gateway.util.Utils;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;

// TODO: need to add data fragmentation
public class SseEncoder implements ProtocolEncoder {
	private static final Charset UTF_8 = Charset.forName("UTF-8");	
	
	private static final byte[] EVENT_BYTES = "event".getBytes(UTF_8);
	private static final byte[] DATA_BYTES = "data".getBytes(UTF_8);
	private static final byte[] ID_BYTES = "id".getBytes(UTF_8);
	private static final byte[] LOCATION_BYTES = "location".getBytes(UTF_8);
	private static final byte[] RECONNECT_BYTES = "reconnect".getBytes(UTF_8);
	private static final byte[] RETRY_BYTES = "retry".getBytes(UTF_8);
	private static final byte   COLON_BYTE = ":".getBytes(UTF_8)[0];
	private static final byte   LINEFEED_BYTE = "\n".getBytes(UTF_8)[0];

	/* Block Padding is used in cases where we need to guarantee that at least 4K of data arrives
	 * to ensure timely delivery in buffering clients (e.g. Android) 
	 */
	public static final SseMessage BLOCK_PADDING_MESSAGE;
	static {
		SseMessage message = new SseMessage();
		String comment = Utils.fill(' ', 4093);
		message.setComment(comment);
		BLOCK_PADDING_MESSAGE = message;
	}

	private final CachingMessageEncoder cachingEncoder;
    private final IoBufferAllocatorEx<?> allocator;
    private final MessageEncoder<SseMessage> encoder;

    public SseEncoder(IoBufferAllocatorEx<?> allocator) {
        this(IO_MESSAGE_ENCODER, allocator);
    }
    
    public SseEncoder(CachingMessageEncoder cachingEncoder, IoBufferAllocatorEx<?> allocator) {
        this.cachingEncoder = cachingEncoder;
        this.allocator = allocator;
        this.encoder = new SseMessageEncoderImpl();
    }

	@Override
	public void dispose(IoSession session) throws Exception {		
	}

    @Override
    public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
        SseMessage sseMessage = (SseMessage)message;
        if (sseMessage.hasCache()) {
            IoBufferEx buf = cachingEncoder.encode(encoder, sseMessage, allocator, FLAG_SHARED | FLAG_ZERO_COPY);
            out.write(buf);
        }
        else {
            IoBufferEx buf = doEncode(allocator, FLAG_ZERO_COPY, sseMessage);
            out.write(buf);
        }
    }

    private static IoBufferEx doEncode(IoBufferAllocatorEx<?> allocator, int flags, SseMessage sseMessage) {
		
		// check for special block padding message
		if (sseMessage == BLOCK_PADDING_MESSAGE) {
        	// wrap using IoBuffer (not allocator) since we need an exact == match in HttpGzipEncoder later 
			return allocator.wrap(ByteBuffer.wrap(HttpGzipEncoder.SSE_4K_PADDING_BYTES), flags & ~FLAG_SHARED);
		}
		
		// calculate length
		int len = 0;

		// calculate type
		boolean hasType = false;
		String type = sseMessage.getType();
		if (type != null && type.length() > 0) {
			len += 5 + 1 + type.length() + 1; // event:<type>\n
			hasType = true;
		}

		// calculate data
		boolean hasData = false;
		IoBufferEx data = sseMessage.getData();
        if (data != null && (flags & FLAG_SHARED) != 0) {
            data = data.duplicate();
        }
		if (data != null && data.hasRemaining()) {
		    int pos = data.position();
		    int dataStartAt = pos;
		    int dataEndAt = data.indexOf(LINEFEED_BYTE);
		    while (dataEndAt != -1) {
	            len += 4 + 1 + (dataEndAt - dataStartAt) + 1; // data:<data>\n
	            data.position(dataEndAt + 1);
	            dataEndAt = data.indexOf(LINEFEED_BYTE);
		    }
		    len += 4 + 1 + data.remaining() + 1; // data:<data>\n
		    data.position(pos);
			hasData = true;
		}
		
		// calculate id
		boolean hasId = false;
		String id = sseMessage.getId();
		if (id != null && id.length() > 0) {
			len += 2 + 1 + id.length() + 1; // id:<id>\n
			hasId = true;
		}
		
		// calculate location for emulated
		boolean hasLocation = false;
		String location = sseMessage.getLocation();
		if (location != null) {
			len += 9 + 1 + location.length() + 1; // location:<location>\n
			hasLocation = true;
		}
		
		// calculate reconnect for emulated
		boolean reconnect = sseMessage.isReconnect();
		if (reconnect) {
			len += 9 + 1; // reconnect\n
		}
		
		// calculate retry
		boolean hasRetry = false;
		int retry = sseMessage.getRetry();
		String retryValue = String.valueOf(retry);
		if (retry >= 0) {
		    len += 5 + 1 + retryValue.length() + 1; // retry:<val>\n
		    hasRetry = true;
		}
		
		// calculate comment
		// Note: we must support empty comments for SSE keep-alive
		boolean hasComment = false;
		String comment = sseMessage.getComment();
		if (comment != null) {
			len += 1 + comment.length() + 1; // :<message>\n
			hasComment = true;
		}

		len += 1;

		if (hasData && !hasType && !hasId && !hasRetry && !hasComment && !hasLocation && !reconnect) {
		    // candidate for zero-copy
		    int dataOffset = 5;  // data:
		    int dataPadding = 2; // \n\n
            if (data.position() >= dataOffset && (data.capacity() - data.limit()) >= dataPadding) {
                // Note: duplicate first to represent different transport layer (no parallel encoding)
                ByteBuffer buf = data.buf();
                ByteBuffer dup = buf.duplicate();
                int remaining = dup.remaining();
                dup.position(dup.position() - dataOffset);
                dup.limit(dup.limit() + dataPadding);
                dup.mark();
                dup.put(DATA_BYTES);
                dup.put(COLON_BYTE);
                dup.position(dup.position() + remaining);
                dup.put(LINEFEED_BYTE);
                dup.put(LINEFEED_BYTE);
                dup.reset();

                // note: defer wrap until position and limit correct (needed by SimpleSharedBuffer)
                return allocator.wrap(dup);
            }
		    
		}

		// allocate exact buffer and put data
		ByteBuffer buf = allocator.allocate(len, flags & ~FLAG_SHARED);
		int offset = buf.position();
		
		// write event type if available
		if (hasType) {
			buf.put(EVENT_BYTES);
			buf.put(COLON_BYTE);
			buf.put(type.getBytes(UTF_8));
			buf.put(LINEFEED_BYTE);
		}
		
		// write data if available
		if (hasData) {
		    int dataStartAt = data.position();
            int dataEndAt = data.indexOf(LINEFEED_BYTE);
            while (data.hasRemaining() && dataEndAt != -1) {
                buf.put(DATA_BYTES);
                buf.put(COLON_BYTE);
                int limit = data.limit();
                data.limit(dataEndAt);
                buf.put(data.buf());
                data.limit(limit);
                data.position(dataEndAt + 1);
                buf.put(LINEFEED_BYTE);
            }
            buf.put(DATA_BYTES);
            buf.put(COLON_BYTE);
            buf.put(data.buf());
            buf.put(LINEFEED_BYTE);
            data.position(dataStartAt);
		}
		
		// write id if available
		if (hasId) {
			buf.put(ID_BYTES);
			buf.put(COLON_BYTE);
			buf.put(id.getBytes(UTF_8));
			buf.put(LINEFEED_BYTE);
		}
		
		// write retry if available
		if (hasRetry) {
		    buf.put(RETRY_BYTES);
		    buf.put(COLON_BYTE);
		    buf.put(retryValue.getBytes(UTF_8));
		    buf.put(LINEFEED_BYTE);
		}
		
		// write comment if available
		if (hasComment) {
			buf.put(COLON_BYTE);
			buf.put(comment.getBytes(UTF_8));
			buf.put(LINEFEED_BYTE);
		}
		
		// write location if available
		if (hasLocation) {
			buf.put(LOCATION_BYTES);
			buf.put(COLON_BYTE);
			buf.put(location.getBytes(UTF_8));
			buf.put(LINEFEED_BYTE);
		}		
		
		// write reconnect if available
		if (reconnect) {
			buf.put(RECONNECT_BYTES);
			buf.put(LINEFEED_BYTE);
		}		
		
		buf.put(LINEFEED_BYTE);
		
		// write data out
		buf.flip();
		buf.position(offset);
        return allocator.wrap(buf);
	}

    private static final class SseMessageEncoderImpl implements MessageEncoder<SseMessage> {
        @Override
        public IoBufferEx encode(IoBufferAllocatorEx<?> allocator, SseMessage message, int flags) {
            return doEncode(allocator, flags, message);
        }
    }
}
