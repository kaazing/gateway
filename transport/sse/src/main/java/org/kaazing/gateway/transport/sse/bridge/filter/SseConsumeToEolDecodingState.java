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

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.statemachine.DecodingState;
import org.kaazing.mina.core.buffer.AbstractIoBufferEx;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.buffer.SimpleBufferAllocator;

public abstract class SseConsumeToEolDecodingState implements DecodingState {

    private static final byte CR = 13;
    private static final byte LF = 10;
    
    protected static final AbstractIoBufferEx END_OF_STREAM_BUFFER = SimpleBufferAllocator.BUFFER_ALLOCATOR.wrap(ByteBuffer.allocate(0));

    private boolean lastIsCR;
    private IoBufferEx buffer;

    @Override
    public DecodingState decode(IoBuffer in, ProtocolDecoderOutput out)
            throws Exception {
        IoBufferEx inEx = (IoBufferEx) in;
        int beginPos = in.position();
        int limit = in.limit();
        int terminatorPos = -1;
        int terminatorSize = 1;

        for (int i = beginPos; i < limit; i++) {
            byte b = in.get(i);
            if (b == CR) {
            	// potential CR or CRLF
            	lastIsCR = true;
            }
            else if (b == LF) {
                if (lastIsCR) {
                	// CRLF
                	lastIsCR = false;
                	terminatorPos = i - 1;
                	terminatorSize = 2;
                }
                else {
	            	// LF
	                terminatorPos = i;
                }
                break;
            }
        }

        if (terminatorPos >= 0) {
            IoBufferEx product;

            int endPos = terminatorPos;
            if (beginPos <= endPos) {
                in.limit(endPos);

                if (buffer == null) {
                    product = inEx.slice();
                }
                else {
                    buffer.put(inEx);
                    product = buffer.flip();
                    buffer = null;
                }

                in.limit(limit);
            } 
            else {
                if (buffer == null) {
                    product = allocator.wrap(allocator.allocate(0));
                }
                else {
                    product = buffer.flip();
                    buffer = null;
                }
            }
            in.position(terminatorPos + terminatorSize);
            return finishDecode(product.asIoBuffer(), out);
        } 
        else {
            in.position(beginPos);
            if (buffer == null) {
                buffer = allocator.wrap(allocator.allocate(in.remaining()));
                buffer.setAutoExpander(allocator);
            }

            buffer.put(inEx);
            if (lastIsCR) {
                buffer.skip(-1);
            }
            return this;
        }
    }

    
    private final IoBufferAllocatorEx<?> allocator;

    public SseConsumeToEolDecodingState(IoBufferAllocatorEx<?> allocator) {
        this.allocator = allocator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DecodingState finishDecode(ProtocolDecoderOutput out) throws Exception {
        if (buffer == null) {
            return finishDecode(END_OF_STREAM_BUFFER, out);
        }
        else {
        	IoBufferEx product = buffer.flip();
            buffer = null;
            return finishDecode(product.asIoBuffer(), out);
        }
    }

    protected abstract DecodingState finishDecode(IoBuffer product,
            ProtocolDecoderOutput out) throws Exception;
}
