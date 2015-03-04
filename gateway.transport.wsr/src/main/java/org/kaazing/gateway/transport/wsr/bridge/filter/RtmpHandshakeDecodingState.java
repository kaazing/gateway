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

import static org.kaazing.gateway.transport.wsr.RtmpHandshakeMessage.NONCE_LENGTH;

import java.util.List;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.statemachine.DecodingState;
import org.kaazing.gateway.transport.DecodingStateMachine;
import org.kaazing.gateway.transport.wsr.RtmpHandshakeRequestMessage;
import org.kaazing.gateway.transport.wsr.RtmpHandshakeResponseMessage;
import org.kaazing.gateway.transport.wsr.RtmpVersionMessage;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.filter.codec.statemachine.FixedLengthDecodingState;

public abstract class RtmpHandshakeDecodingState extends DecodingStateMachine {

    private final DecodingState READ_CLIENT_HANDSHAKE_START = new FixedLengthDecodingState(allocator, 1536) {

        @Override
        protected DecodingState finishDecode(IoBuffer product, ProtocolDecoderOutput out) throws Exception {
            int t1 = product.getInt();
            int t2 = product.getInt();
            IoBuffer nonce = product.getSlice(NONCE_LENGTH);

            out.write(new RtmpHandshakeRequestMessage(t1, t2, nonce.buf()));

            return READ_CLIENT_HANDSHAKE_RESPONSE;
        }
    };

    private final DecodingState READ_CLIENT_HANDSHAKE_RESPONSE = new FixedLengthDecodingState(allocator, 1536) {

        @Override
        protected DecodingState finishDecode(IoBuffer product, ProtocolDecoderOutput out) throws Exception {
            int t1 = product.getInt();
            int t2 = product.getInt();
            IoBuffer nonce = product.getSlice(NONCE_LENGTH);
            
            out.write(new RtmpHandshakeResponseMessage(t1, t2, nonce.buf()));
            return null;
        }
    };

    private final DecodingState READ_VERSION = new FixedLengthDecodingState(allocator, 1) {

        @Override
        protected DecodingState finishDecode(IoBuffer product, ProtocolDecoderOutput out) throws Exception {
            out.write(new RtmpVersionMessage());
            return READ_CLIENT_HANDSHAKE_START;
        }
    };

    public RtmpHandshakeDecodingState(IoBufferAllocatorEx<?> allocator) {
        super(allocator);
    }

    @Override
    public DecodingState decode(IoBuffer in, ProtocolDecoderOutput out) throws Exception {
        DecodingState state = super.decode(in, out);
        flush(childProducts, out);
        return state;
    }

    @Override
    protected DecodingState init() throws Exception {
        return READ_VERSION;
    }
    
    @Override
    protected void destroy() throws Exception {
        
    }
    
    @Override
    protected final DecodingState finishDecode(List<Object> childProducts, ProtocolDecoderOutput out) throws Exception {
        // flush child products to parent output before decode is complete
        flush(childProducts, out);
        return finishDecode();
    }
    
    protected abstract DecodingState finishDecode();
    
    private void flush(List<Object> childProducts, ProtocolDecoderOutput out) {
        // flush child products to parent output before decode is complete
        for (Object childProduct : childProducts) {
            out.write(childProduct);
        }
        childProducts.clear();
    }
}

