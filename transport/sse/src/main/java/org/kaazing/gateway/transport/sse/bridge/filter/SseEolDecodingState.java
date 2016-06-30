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

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.statemachine.DecodingState;

public abstract class SseEolDecodingState implements DecodingState {

	private static final byte CR = 13;
    private static final byte LF = 10;

    private boolean hasCR;

    /**
     * {@inheritDoc}
     */
    @Override
    public DecodingState decode(IoBuffer in, ProtocolDecoderOutput out)
            throws Exception {
        while (in.hasRemaining()) {
            byte b = in.get();
            
            if (b == LF) {
            	// CRLF or LF
            	SseEolStyle eolStyle = hasCR ? SseEolStyle.CRLF : SseEolStyle.LF;
                hasCR = false;
                return finishDecode(eolStyle, out);
            }
            else if (hasCR) {
            	// CR followed by !LF
                hasCR = false;
                in.skip(-1);
                return finishDecode(SseEolStyle.CR, out);
            }
            else if (b == CR) {
            	// potential CR or CRLF
            	hasCR = true;
            }
            else {
                in.skip(-1);
                return finishDecode(SseEolStyle.NONE, out);
            }
        }

        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DecodingState finishDecode(ProtocolDecoderOutput out)
            throws Exception {
        return finishDecode(SseEolStyle.NONE, out);
    }

    protected abstract DecodingState finishDecode(SseEolStyle eolStyle,
            ProtocolDecoderOutput out) throws Exception;
}
