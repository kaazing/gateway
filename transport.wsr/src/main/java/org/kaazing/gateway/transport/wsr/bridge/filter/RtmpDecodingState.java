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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.statemachine.DecodingState;
import org.kaazing.gateway.transport.DecodingStateMachine;
import org.kaazing.gateway.transport.wsr.RtmpSetChunkSizeMessage;
import org.kaazing.gateway.transport.wsr.RtmpStreamMessage;
import org.kaazing.gateway.transport.wsr.RtmpStreamMessage.StreamKind;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.filter.codec.statemachine.FixedLengthDecodingState;

public class RtmpDecodingState extends DecodingStateMachine {

    private static final int COMMAND_STREAM_ID = 2;

    private final WsrStreamMessageDecodingState commandStream;
    private final Map<Integer, WsrStreamMessageDecodingState> readStreams;

    private int maxChunkSize = 128;
    private int windowSize;
    private int sequenceNumber = 0;
    private int sequenceOffset = 0;
    
    public RtmpDecodingState(IoBufferAllocatorEx<?> allocator) {
        super(allocator);
        this.readStreams = new HashMap<Integer, WsrStreamMessageDecodingState>();
        this.READ_CHUNK_HEADER = new ReadChunkHeaderDecodingStateImpl(allocator);
        this.commandStream = new WsrStreamMessageDecodingState(allocator, COMMAND_STREAM_ID) {
            @Override
            protected DecodingState finishDecode(RtmpStreamMessage message, ProtocolDecoderOutput out) {
                switch (message.getStreamKind()) {
                case SET_CHUNK_SIZE:
                    RtmpSetChunkSizeMessage setChunkSize = (RtmpSetChunkSizeMessage) message;
                    setChunkSize(setChunkSize.getChunkSize());
                    break;
                case WINDOW_ACKNOWLEDGMENT_SIZE:
                	// does this message actually occur in the wild?
                	// setWindowSize();
                	break;
                default:
                    //throw new ProtocolDecoderException("Unexpected message kind on command stream " + message.getStreamKind());
                }
                return null;
            }
        };
    }
    
    private void setChunkSize(int size) {
        maxChunkSize = size;
    }
    
    private void setWindowSize(int size) {
    	windowSize = size;
    }

    private final RtmpHandshakeDecodingState READ_HANDSHAKE = new RtmpHandshakeDecodingState(allocator) {
        
        @Override
        protected DecodingState finishDecode() {
            return READ_CHUNK_HEADER;
        };
    };
    
    private final DecodingState READ_CHUNK_HEADER;
    
    @Override
    public DecodingState finishDecode(ProtocolDecoderOutput out) throws Exception {
        // TODO Auto-generated method stub
        return super.finishDecode(out);
    }

    @Override
    protected void destroy() throws Exception {

    }

    @Override
    public DecodingState decode(IoBuffer in, ProtocolDecoderOutput out) throws Exception {
        DecodingState state = super.decode(in, out);
        flush(childProducts, out);
        return state;
    }

    @Override
    protected DecodingState finishDecode(List<Object> childProducts, ProtocolDecoderOutput out) throws Exception {
        // flush child products to parent output before decode is complete
        flush(childProducts, out);
        return null;
    }

    private void flush(List<Object> childProducts, ProtocolDecoderOutput out) {
        for (Object childProduct : childProducts) {
            out.write(childProduct);
        }
        childProducts.clear();
    }

    @Override
    protected DecodingState init() throws Exception {
        return READ_HANDSHAKE;
    }

    private final class ReadChunkHeaderDecodingStateImpl extends
            RtmpChunkHeaderDecodingState {
        private ReadChunkHeaderDecodingStateImpl(
                IoBufferAllocatorEx<?> allocator) {
            super(allocator);
        }

        @Override
        protected DecodingState finishDecode(int chunkStreamId,
                ProtocolDecoderOutput out) {

            WsrStreamMessageDecodingState readStream = supplyStreamDecodingState(chunkStreamId);

            return readChunkedStream(readStream);
        }

        @Override
        protected DecodingState finishDecode(int chunkStreamId, int timestamp,
                ProtocolDecoderOutput out) {

            WsrStreamMessageDecodingState readStream = supplyStreamDecodingState(chunkStreamId);

            readStream.setTimestamp(timestamp);

            return readChunkedStream(readStream);
        }

        @Override
        protected DecodingState finishDecode(int chunkStreamId, int timestamp,
                int messageLength, StreamKind streamKind,
                ProtocolDecoderOutput out) {

            WsrStreamMessageDecodingState readStream = supplyStreamDecodingState(chunkStreamId);

            readStream.setTimestamp(timestamp);
            readStream.setMessageLength(messageLength);
            readStream.setStreamKind(streamKind);

            return readChunkedStream(readStream);
        }

        @Override
        protected DecodingState finishDecode(int chunkStreamId, int timestamp,
                int messageLength, StreamKind streamKind, int messageStreamId,
                ProtocolDecoderOutput out) {

            final WsrStreamMessageDecodingState readStream = supplyStreamDecodingState(chunkStreamId);

            readStream.setTimestamp(timestamp);
            readStream.setMessageLength(messageLength);
            readStream.setStreamKind(streamKind);
            readStream.setMessageStreamId(messageStreamId);

            return readChunkedStream(readStream);
        }

        private DecodingState readChunkedStream(
                final WsrStreamMessageDecodingState readStream) {
            int messageRemaining = readStream.getMessageRemaining();

            if (messageRemaining < maxChunkSize) {
                return new FixedLengthDecodingState(allocator, messageRemaining) {
                    @Override
                    protected DecodingState finishDecode(IoBuffer product,
                            ProtocolDecoderOutput out) throws Exception {
                        readStream.decode(product, out);
                        return READ_CHUNK_HEADER;
                    }
                };
            } else {
                return new FixedLengthDecodingState(allocator, maxChunkSize) {
                    @Override
                    protected DecodingState finishDecode(IoBuffer product,
                            ProtocolDecoderOutput out) throws Exception {
                        readStream.decode(product, out);
                        return READ_CHUNK_HEADER;
                    }
                };
            }
        }

        private WsrStreamMessageDecodingState supplyStreamDecodingState(
                int chunkStreamId) {
            // TODO: account for potential out-of-order delivery due to
            // overlapping stream use
            if (chunkStreamId == COMMAND_STREAM_ID) {
                return commandStream;
            }
            WsrStreamMessageDecodingState readStream = readStreams.get(chunkStreamId);
            if (readStream == null) {
                readStream = new WsrStreamMessageDecodingState(allocator, chunkStreamId) {
                    @Override
                    protected DecodingState finishDecode(
                            RtmpStreamMessage message, ProtocolDecoderOutput out) {
                        out.write(message);
                        return null;
                    }
                };
                readStreams.put(chunkStreamId, readStream);
            }
            return readStream;
        }
    }

}
