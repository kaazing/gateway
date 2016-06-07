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
package org.kaazing.gateway.transport.wseb.filter;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.kaazing.gateway.transport.BridgeSession;
import org.kaazing.gateway.util.codec.PassThroughDecoder;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.mina.filter.codec.ProtocolCodecFilter;

public class WsebEncodingCodecFilter extends ProtocolCodecFilter {

    public enum EscapeTypes { NO_ESCAPE, ESCAPE_ZERO_AND_NEWLINES, ESCAPE_ZERO }

    public WsebEncodingCodecFilter() {
        super(new WsCodecFactory(EscapeTypes.NO_ESCAPE));
    }

    public WsebEncodingCodecFilter(EscapeTypes escape) {
        super(new WsCodecFactory(escape));
    }

    public WsebEncodingCodecFilter(ProtocolCodecFactory wsCodecFactory) {
        super(wsCodecFactory);
    }

    private static class WsCodecFactory implements ProtocolCodecFactory {

        EscapeTypes escapeType = EscapeTypes.NO_ESCAPE;

        public WsCodecFactory(EscapeTypes escape) {
            this.escapeType = escape;
        }

        @Override
        public ProtocolEncoder getEncoder(IoSession session) {
            IoSessionEx sessionEx = (IoSessionEx) session;
            IoBufferAllocatorEx<?> allocator = sessionEx.getBufferAllocator();

            if (session instanceof BridgeSession) {
                BridgeSession bridgeSession = (BridgeSession)session;
                switch (escapeType) {
                case ESCAPE_ZERO_AND_NEWLINES:
                    return new WsebFrameEscapeZeroAndNewLineEncoder(bridgeSession.getMessageEncoder(), allocator);
                case ESCAPE_ZERO:
                    return new WsebFrameEscapeZeroAndNewLineEncoder(bridgeSession.getMessageEncoder(), allocator); // TODO: change to escape zero only later
                default:
                    return new WsebFrameEncoder(bridgeSession.getMessageEncoder(), allocator);
                }
            }

            switch (escapeType) {
            case ESCAPE_ZERO_AND_NEWLINES:
                return new WsebFrameEscapeZeroAndNewLineEncoder(allocator);
            case ESCAPE_ZERO:
                return new WsebFrameEscapeZeroAndNewLineEncoder(allocator);  // TODO: change to escape zero only later
            default:
                return new WsebFrameEncoder(allocator);
            }
        }

        @Override
        public ProtocolDecoder getDecoder(IoSession session) {
            return PassThroughDecoder.PASS_THROUGH_DECODER;
        }
    }


}
