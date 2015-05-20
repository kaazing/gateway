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

package org.kaazing.gateway.transport.wsr.bridge.filter;

import java.nio.ByteOrder;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.filter.codec.ProtocolDecoderException;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.statemachine.DecodingState;
import org.kaazing.gateway.transport.wsr.RtmpStreamMessage.StreamKind;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.filter.codec.statemachine.FixedLengthDecodingState;

public abstract class RtmpChunkHeaderDecodingState implements DecodingState {

    private static final int[] CHUNK_HEADER_LENGTHS = new int[] {11, 7, 3, 0};

    protected final IoBufferAllocatorEx<?> allocator;

    private int chunkStreamId;
    private int chunkType;
    private IoBufferEx buffer;
    
    RtmpChunkHeaderDecodingState(IoBufferAllocatorEx<?> allocator) {
        this.allocator = allocator;
    }

    @Override
    public DecodingState decode(IoBuffer in, ProtocolDecoderOutput out) throws Exception {

        if (!in.hasRemaining()) {
            return this;
        }

        if (buffer == null) {
            // non-destructive read before buffering
            byte packedByte = in.get();
            
            // TODO: account for multiple byte stream identifiers
            int chunkStreamId = packedByte & 0x3f;
            
            int chunkType = (packedByte & 0xc0) >> 6;
            assert ((chunkType | 0x03) == 0x03);
            
            if (in.remaining() < CHUNK_HEADER_LENGTHS[chunkType]) {
            	// remember chunk type for chunk decode
            	this.chunkStreamId = chunkStreamId;
            	this.chunkType = chunkType;
            	
                buffer = allocator.wrap(allocator.allocate(CHUNK_HEADER_LENGTHS[chunkType]));
                buffer.mark();
                buffer.setAutoExpander(allocator);

                buffer.put((IoBufferEx) in);
                return this;
            }
            
            return decodeChunkHeader(chunkStreamId, chunkType, (IoBufferEx) in, out);
        }
        else {
            assert (buffer != null);
            
            buffer.put((IoBufferEx) in);
            
            if (buffer.position() < CHUNK_HEADER_LENGTHS[chunkType]) {
                return this;
            }
            
            buffer.flip();
            IoBufferEx buf = buffer;
            buffer = null;
            return decodeChunkHeader(chunkStreamId, chunkType, buf, out);
        }
    }
    
    private DecodingState decodeChunkHeader(final int chunkStreamId, int chunkType, IoBufferEx buf, ProtocolDecoderOutput out) throws Exception {
        
        switch (chunkType) {
        case 0: {
            int timestamp = buf.getUnsignedMediumInt();
            final int messageLength = buf.getUnsignedMediumInt();
            final byte messageTypeId = buf.get();
            // messageStreamId is a little-endian field in an otherwise big-endian (network byte order) protocol
            buf.order(ByteOrder.LITTLE_ENDIAN);
            int messageStreamId = buf.getInt();
            buf.order(ByteOrder.BIG_ENDIAN);
            if (timestamp == 0xffffff) {
            	// need to parse extended timestamp field
            	final RtmpChunkHeaderDecodingState state = this;
            	return new FixedLengthDecodingState(allocator, 4) {
					
					@Override
					protected DecodingState finishDecode(IoBuffer buf,
							ProtocolDecoderOutput out) throws Exception {
						int extendedTimestamp = buf.getInt();
						return state.finishDecode(chunkStreamId, extendedTimestamp, messageLength, StreamKind.decode(messageTypeId), out);
					}
				};
            } else {
            	return finishDecode(chunkStreamId, timestamp, messageLength, StreamKind.decode(messageTypeId), messageStreamId, out);
            }
        }
        case 1: {
            int timestamp = buf.getUnsignedMediumInt();
            final int messageLength = buf.getUnsignedMediumInt();
            final byte messageTypeId = buf.get();
            if (timestamp == 0xffffff) {
            	// need to parse extended timestamp field
            	final RtmpChunkHeaderDecodingState state = this;
            	return new FixedLengthDecodingState(allocator, 4) {
					
					@Override
					protected DecodingState finishDecode(IoBuffer buf,
							ProtocolDecoderOutput out) throws Exception {
						int extendedTimestamp = buf.getInt();
						return state.finishDecode(chunkStreamId, extendedTimestamp, messageLength, StreamKind.decode(messageTypeId), out);
					}
				};
            } else {
            	return finishDecode(chunkStreamId, timestamp, messageLength, StreamKind.decode(messageTypeId), out);
            }
        }
        case 2: {
            int timestamp = buf.getUnsignedMediumInt();
            if (timestamp == 0xffffff) {
            	// need to parse extended timestamp field
            	final RtmpChunkHeaderDecodingState state = this;
            	return new FixedLengthDecodingState(allocator, 4) {
					@Override
					protected DecodingState finishDecode(IoBuffer buf,
							ProtocolDecoderOutput out) throws Exception {
						int extendedTimestamp = buf.getInt();
						return state.finishDecode(chunkStreamId, extendedTimestamp, out);
					}
				};
            } else {
            	return finishDecode(chunkStreamId, timestamp, out);
            }
        }
        case 3: {
            return finishDecode(chunkStreamId, out);
        }
        default:
            throw new ProtocolDecoderException("Unrecognized chunk type: " + chunkType);
        }
    }
    
    protected abstract DecodingState finishDecode(int chunkStreamId, int timestamp, int messageLength, StreamKind streamKind, int messageStreamId, ProtocolDecoderOutput out);

    protected abstract DecodingState finishDecode(int chunkStreamId, int timestamp, int messageLength, StreamKind streamKind, ProtocolDecoderOutput out);

    protected abstract DecodingState finishDecode(int chunkStreamId, int timestamp, ProtocolDecoderOutput out);

    protected abstract DecodingState finishDecode(int chunkStreamId, ProtocolDecoderOutput out);

    @Override
    public DecodingState finishDecode(ProtocolDecoderOutput out) throws Exception {
        return null;
    }
}
