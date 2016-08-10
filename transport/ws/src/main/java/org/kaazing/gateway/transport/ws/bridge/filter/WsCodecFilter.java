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
package org.kaazing.gateway.transport.ws.bridge.filter;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.kaazing.gateway.transport.BridgeSession;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.mina.filter.codec.ProtocolCodecFilter;

public class WsCodecFilter extends ProtocolCodecFilter {


    /**
     * @param wsMaxMessageSize   Maximum permitted number of bytes in a message being decoded. If <0 there is no limit.
     */
    public WsCodecFilter(int wsMaxMessageSize, boolean maskSends) {
        super(new WsCodecFactory(wsMaxMessageSize > 0 ? wsMaxMessageSize : 0, maskSends));
    }

    private static class WsCodecFactory implements ProtocolCodecFactory {
        private int wsMaxMessageSize;
        private boolean maskSends;

        public WsCodecFactory(int wsMaxMessageSize, boolean maskSends) {
            this.wsMaxMessageSize = wsMaxMessageSize;
            this.maskSends = maskSends;
        }

        @Override
        public ProtocolEncoder getEncoder(IoSession session) {
            IoSessionEx sessionEx = (IoSessionEx) session;
            IoBufferAllocatorEx<?> allocator = sessionEx.getBufferAllocator();

            if (session instanceof BridgeSession) {
                BridgeSession bridgeSession = (BridgeSession)session;
                return new WsFrameEncoder(bridgeSession.getMessageEncoder(), allocator, maskSends);
            }

            return new WsFrameEncoder(allocator, maskSends);
        }

        @Override
        public ProtocolDecoder getDecoder(IoSession session) {
            IoSessionEx sessionEx = (IoSessionEx) session;
            IoBufferAllocatorEx<?> allocator = sessionEx.getBufferAllocator();

            return new WsFrameDecoder(allocator, wsMaxMessageSize, !maskSends);
        }
    }
}
