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

package org.kaazing.gateway.service.turn.proxy.filters;

import org.apache.mina.filter.codec.ProtocolDecoderException;

public class TurnFrameDecoderException extends ProtocolDecoderException {

    /**
     * 
     */
    private static final long serialVersionUID = -294329247047308297L;
    private final StunMessage stunMessage;

    /**
     * Constructs a new instance with the specified message and the specified
     * cause.
     * @param stunMessage 
     */
    public TurnFrameDecoderException(String message, Throwable cause, StunMessage stunMessage) {
        super(message, cause);
        this.stunMessage = stunMessage;
    }

    public StunMessage getStunMessage() {
        return stunMessage;
    }

}
