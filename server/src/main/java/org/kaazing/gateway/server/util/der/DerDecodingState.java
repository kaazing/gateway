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
package org.kaazing.gateway.server.util.der;

import java.util.List;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.statemachine.DecodingState;
import org.kaazing.gateway.transport.DecodingStateMachine;
import org.kaazing.gateway.util.der.DerId;
import org.kaazing.gateway.util.der.DerUtils;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.filter.codec.statemachine.FixedLengthDecodingState;

/**
 * State machine that decodes a DER-encoded message. See X.690-0207. TODO: Add support for end-of-contents octets.
 */
public abstract class DerDecodingState extends DecodingStateMachine {

    public DerDecodingState(IoBufferAllocatorEx<?> allocator) {
        super(allocator);
    }

    @Override
    protected DecodingState init() throws Exception {
        return READ_ID; // start by reading identifier octets
    }

    @Override
    protected void destroy() throws Exception {
    }

    @Override
    protected DecodingState finishDecode(List<Object> childProducts, ProtocolDecoderOutput out) throws Exception {
        if (childProducts.size() == 2) {
            return finishDecode((DerId) childProducts.get(0), (IoBuffer) childProducts.get(1), out);
        }
        return null; // TODO: Exception?
    }

    /**
     * Invoked when data is available for this state machine.
     *
     * @param id  the DER identifier
     * @param buf the contents of the DER-encoded message
     * @param out used to write decoded objects
     * @return the next state
     */
    protected abstract DecodingState finishDecode(DerId id, IoBuffer buf, ProtocolDecoderOutput out) throws Exception;

    /**
     * Decoding state to read the initial identifier octets in the DER-encoded message.
     */
    private final DecodingState READ_ID = new DecodingState() {

        @Override
        public DecodingState decode(IoBuffer in, ProtocolDecoderOutput out) throws Exception {
            DerId id = DerId.decode(in.buf());
            out.write(id);
            return READ_LENGTH;
        }

        @Override
        public DecodingState finishDecode(ProtocolDecoderOutput out) throws Exception {
            return null;
        }

    };

    /**
     * Decoding state to read the length octets in the DER-encoded message.
     */
    private final DecodingState READ_LENGTH = new DecodingState() {

        @Override
        public DecodingState decode(IoBuffer in, ProtocolDecoderOutput out) throws Exception {
            final int length = DerUtils.decodeLength(in.buf());
            return new FixedLengthDecodingState(allocator, length) {

                @Override
                protected DecodingState finishDecode(IoBuffer product, ProtocolDecoderOutput out) throws Exception {
                    out.write(product);
                    return null; // terminate the state machine
                }

            };
        }

        @Override
        public DecodingState finishDecode(ProtocolDecoderOutput out) throws Exception {
            return null;
        }

    };

}
