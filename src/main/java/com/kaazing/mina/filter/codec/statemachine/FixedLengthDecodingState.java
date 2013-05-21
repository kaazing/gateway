/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package com.kaazing.mina.filter.codec.statemachine;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.statemachine.DecodingState;

import com.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import com.kaazing.mina.core.buffer.IoBufferEx;

/**
 * {@link DecodingState} which consumes all received bytes until a configured
 * number of read bytes has been reached. Please note that this state can
 * produce a buffer with less data than the configured length if the associated
 * session has been closed unexpectedly.
 */
public abstract class FixedLengthDecodingState implements DecodingState {

    protected final IoBufferAllocatorEx<?> allocator;
    private final int length;

    private IoBufferEx buffer;

    /**
     * Constructs a new instance using the specified decode length.
     *
     * @param length the number of bytes to read.
     */
    public FixedLengthDecodingState(IoBufferAllocatorEx<?> allocator, int length) {
        this.allocator = allocator;
        this.length = length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DecodingState decode(IoBuffer in, ProtocolDecoderOutput out)
            throws Exception {
        IoBufferEx inEx = (IoBufferEx) in;
        if (buffer == null) {
            if (in.remaining() >= length) {
                int limit = in.limit();
                in.limit(in.position() + length);
                IoBufferEx product = inEx.slice();
                in.position(in.position() + length);
                in.limit(limit);
                return finishDecode((IoBuffer) product, out);
            }

            buffer = allocator.allocate(length, /* shared */ false);
            buffer.mark();
            buffer.put(inEx);
            return this;
        }

        if (in.remaining() >= length - buffer.position()) {
            int limit = in.limit();
            in.limit(in.position() + length - buffer.position());
            buffer.put(inEx);
            in.limit(limit);
            int allocatedPos = buffer.markValue();
            buffer.flip();
            buffer.position(allocatedPos);
            IoBufferEx product = buffer;
            buffer = null;
            return finishDecode((IoBuffer) product, out);
        }

        buffer.put(inEx);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DecodingState finishDecode(ProtocolDecoderOutput out)
            throws Exception {
        IoBufferEx readData;
        if (buffer == null) {
            readData = allocator.allocate(0, /* shared */ false);
        } else {
            int allocatedPos = buffer.markValue();
            buffer.flip();
            buffer.position(allocatedPos);
            readData = buffer;
            buffer = null;
        }
        return finishDecode((IoBuffer) readData, out);
    }

    /**
     * Invoked when this state has consumed the configured number of bytes.
     *
     * @param ioBufferEx the data.
     * @param out the current {@link ProtocolDecoderOutput} used to write
     *        decoded messages.
     * @return the next state if a state transition was triggered (use
     *         <code>this</code> for loop transitions) or <code>null</code> if
     *         the state machine has reached its end.
     * @throws Exception if the read data violated protocol specification.
     */
    protected abstract DecodingState finishDecode(IoBuffer ioBufferEx,
            ProtocolDecoderOutput out) throws Exception;
}
