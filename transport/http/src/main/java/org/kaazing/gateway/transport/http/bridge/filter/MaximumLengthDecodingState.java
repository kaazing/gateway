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
package org.kaazing.gateway.transport.http.bridge.filter;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.filter.codec.ProtocolDecoderException;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.statemachine.DecodingState;
import org.kaazing.gateway.transport.http.bridge.HttpContentMessage;
import org.kaazing.mina.core.buffer.IoBufferEx;

final class MaximumLengthDecodingState implements DecodingState {
    private long remaining;

    MaximumLengthDecodingState(long maximumLength) {
        this.remaining = maximumLength;
    }

    @Override
    public DecodingState decode(IoBuffer in, ProtocolDecoderOutput out) throws Exception {
        IoBufferEx inEx = (IoBufferEx) in;
        int length = inEx.remaining();
        if (remaining > length) {
            // more data will come in next IP packet
            remaining -= length;
            IoBufferEx slice = inEx.getSlice(length);
            HttpContentMessage httpContent = new HttpContentMessage(slice, false);
            out.write(httpContent);

            return this;
        } else if (remaining > 0L) {
            // remaining <= in.remaining() - data is completed
            int remainingAsInt = (int) remaining;
            IoBufferEx slice = inEx.getSlice(remainingAsInt);
            remaining = 0L;

            HttpContentMessage httpContent = new HttpContentMessage(slice, true);
            out.write(httpContent);

            return finishDecode(out);
        } else {
            throw new ProtocolDecoderException("Content length exceeded: " + in.getHexDump());
        }
    }

    @Override
    public DecodingState finishDecode(ProtocolDecoderOutput out) throws Exception {
        return null;
    }
}
