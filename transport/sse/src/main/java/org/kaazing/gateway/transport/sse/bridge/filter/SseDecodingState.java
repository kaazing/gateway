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

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.statemachine.DecodingState;
import org.apache.mina.filter.codec.statemachine.LinearWhitespaceSkippingState;
import org.kaazing.gateway.transport.DecodingStateMachine;
import org.kaazing.gateway.transport.sse.bridge.SseMessage;
import org.kaazing.gateway.util.ConsumeToTerminatorDecodingState;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;

//TODO: use CompositeIoBuffer when available
public class SseDecodingState extends DecodingStateMachine {

    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final CharsetDecoder UTF_8_DECODER = UTF_8.newDecoder();
    private static final byte LINEFEED_BYTE = "\n".getBytes(UTF_8)[0];

    private enum Type {
        UNKNOWN, EVENT, DATA, ID, RETRY, COMMENT
    }

    private static Map<String, Type> types = new HashMap<>();
    static {
        types.put("event", Type.EVENT);
        types.put("data", Type.DATA);
        types.put("id", Type.ID);
        types.put("retry", Type.RETRY);
    }

    private SseMessage message;
    private Type type;

    private static Type getType(String name) {
        Type type = types.get(name);
        if (type == null) {
            type = Type.UNKNOWN;
        }
        return type;
    }

    private final DecodingState FIND_EMPTY_LINE = new SseEolDecodingState() {
        @Override
        protected DecodingState finishDecode(SseEolStyle eolStyle, ProtocolDecoderOutput out) throws Exception {
            if (eolStyle != SseEolStyle.NONE) {
                if (message != null) {
                    out.write(message);
                    message = null;
                }
                return null;
            }
            else {
                if (message == null) {
                    message = new SseMessage();                    
                }
                return READ_FIELD_NAME;
            }
        }
    };
    
    private final DecodingState READ_FIELD_NAME = new ConsumeToTerminatorDecodingState(allocator, (byte) ':') {
        @Override
        protected DecodingState finishDecode(IoBuffer buffer, ProtocolDecoderOutput out) throws Exception {
            if (!buffer.hasRemaining()) {
                type = Type.COMMENT;
            }
            else {
                String name = buffer.getString(UTF_8_DECODER);
                type = getType(name);
            }
            return AFTER_READ_FIELD_NAME;
        }
    };

    private final DecodingState AFTER_READ_FIELD_NAME = new DecodingState() {
        @Override
		public DecodingState decode(IoBuffer in, ProtocolDecoderOutput out)
				throws Exception {
        	if (!in.hasRemaining()) {
        		return this;
        	}

        	// optionally skip space
        	if (in.get(in.position()) == 32) {
        		in.skip(1);
        	}
        	
			return finishDecode(out);
		}

		@Override
		public DecodingState finishDecode(ProtocolDecoderOutput out)
				throws Exception {
			return READ_FIELD_VALUE;
		}
    };
    
    private final DecodingState AFTER_READ_FIELD_VALUE = new LinearWhitespaceSkippingState() {
        @Override
        protected DecodingState finishDecode(int skippedBytes) throws Exception {
            return FIND_EMPTY_LINE;
        }
    };

    private final DecodingState READ_FIELD_VALUE = new SseConsumeToEolDecodingState(allocator) {
        @Override
        protected DecodingState finishDecode(IoBuffer buffer, ProtocolDecoderOutput out) throws Exception {

            // no data, we are done with the stream, return out
            if (buffer == END_OF_STREAM_BUFFER) {
                return this;
            }

            String value;
            
            switch (type) {
            case DATA:
                // get current message data
                IoBufferEx data = message.getData();

                // if data does not exit this is the first one so just set it directly
                // this will be a read only buffer and this is optimized for single data delivery
                if (data == null) {
                    IoBufferEx newData = (IoBufferEx) buffer.slice();
                    // derived buffers cannot be made auto-expand
                    // nor can an attempt be made to set auto-expand to false either
                    if (newData.isAutoExpand()) {
                    	newData.setAutoExpander(null);
                    }
					message.setData(newData);
                }
                // if this is not the first data field then we need to add it to what we have already
                else {
                    // if the data is read only then this is the second data field we have received and we
                    // need to allocate a new, auto expanding, buffer and add the previous data to it
                    if (!data.isAutoExpand()) {
                        // create new buffer that can fit the current data, the new data, and the linefeed between them
                    	int capacity = data.remaining() + buffer.remaining() + 1;
                        ByteBuffer nioBuf = allocator.allocate(capacity);
                        IoBufferEx newData = allocator.wrap(nioBuf).setAutoExpander(allocator);
                        // put the current data into the new buffer and rewind to the start as if it has been there all along
                        newData.put(data);
                        newData.flip();
                        // swap in the new buffer
                        data = newData;
                        message.setData(newData);
                    }

                    // append the new data to the current buffer with the appropriate line feed
                    data.mark();
                    data.skip(data.remaining());
                    data.put(LINEFEED_BYTE);
                    data.put((IoBufferEx) buffer);

                    // reset the buffer so it is ready to be read
                    data.reset();
                }
                break;
            case EVENT:
                value = buffer.getString(UTF_8_DECODER);
                message.setType(value);
                break;
            case ID:
                value = buffer.getString(UTF_8_DECODER);
                message.setId(value);
                break;
            case RETRY:
                value = buffer.getString(UTF_8_DECODER);
                message.setRetry(Integer.parseInt(value));
                break;
            case COMMENT:
                value = buffer.getString(UTF_8_DECODER);
                message.setComment(value);
                break;
            }
            return AFTER_READ_FIELD_VALUE;
        }
    };

    SseDecodingState(IoBufferAllocatorEx<?> allocator) {
        super(allocator);
    }

    @Override
    protected DecodingState init() throws Exception {
        return FIND_EMPTY_LINE;
    }

    @Override
    protected DecodingState finishDecode(List<Object> childProducts, ProtocolDecoderOutput out) throws Exception {
        for (Object childProduct : childProducts) {
            out.write(childProduct);
        }
        return null;
    }

    @Override
    protected void destroy() throws Exception {
    }
}
