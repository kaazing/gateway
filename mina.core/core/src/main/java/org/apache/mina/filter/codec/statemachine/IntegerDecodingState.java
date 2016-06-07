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
package org.apache.mina.filter.codec.statemachine;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.filter.codec.ProtocolDecoderException;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

/**
 * {@link DecodingState} which decodes <code>int</code> values in big-endian 
 * order (high bytes come first).
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public abstract class IntegerDecodingState implements DecodingState {
    
    private int firstByte;
    private int secondByte;
    private int thirdByte;
    private int counter;

    /**
     * {@inheritDoc}
     */
    public DecodingState decode(IoBuffer in, ProtocolDecoderOutput out)
            throws Exception {
        while (in.hasRemaining()) {
            switch (counter) {
            case 0:
                firstByte = in.getUnsigned();
                break;
            case 1:
                secondByte = in.getUnsigned();
                break;
            case 2:
                thirdByte = in.getUnsigned();
                break;
            case 3:
                counter = 0;
                return finishDecode(
                        (firstByte << 24) | (secondByte << 16) | (thirdByte << 8) | in.getUnsigned(),
                        out);
            default:
                throw new InternalError();
            }
            counter ++;
        }

        return this;
    }

    /**
     * {@inheritDoc}
     */
    public DecodingState finishDecode(ProtocolDecoderOutput out)
            throws Exception {
        throw new ProtocolDecoderException(
                "Unexpected end of session while waiting for an integer.");
    }

    /**
     * Invoked when this state has consumed a complete <code>int</code>.
     * 
     * @param value the integer.
     * @param out the current {@link ProtocolDecoderOutput} used to write 
     *        decoded messages.
     * @return the next state if a state transition was triggered (use 
     *         <code>this</code> for loop transitions) or <code>null</code> if 
     *         the state machine has reached its end.
     * @throws Exception if the read data violated protocol specification.
     */
    protected abstract DecodingState finishDecode(int value,
            ProtocolDecoderOutput out) throws Exception;
}
