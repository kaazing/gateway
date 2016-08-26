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
package org.kaazing.gateway.service.turn.proxy.stun;

import static org.kaazing.gateway.service.turn.proxy.TurnProxyAcceptHandler.TURN_SESSION_TRANSACTION_MAP;
import static org.kaazing.gateway.service.turn.proxy.stun.StunAttributeFactory.CredentialType.SHORT_TERM;

import java.security.Key;
import java.util.Map;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.mina.filter.codec.ProtocolCodecFilter;

public class StunCodecFilter extends ProtocolCodecFilter {

    public StunCodecFilter(Key sharedSecret, String keyAlgorithm) {
        super(new TurnCodecFactory(sharedSecret, keyAlgorithm));
    }

    private static class TurnCodecFactory implements ProtocolCodecFactory {

        private final Key sharedSecret;
        private final String keyAlgorithm;
        private final StunAttributeFactory stunAttributeFactory = new StunAttributeFactory(SHORT_TERM);

        public TurnCodecFactory(Key sharedSecret, String keyAlgorithm) {
            this.sharedSecret = sharedSecret;
            this.keyAlgorithm = keyAlgorithm;
        }

        @SuppressWarnings("unchecked")
        @Override
        public ProtocolEncoder getEncoder(IoSession session) throws Exception {
            IoSessionEx sessionEx = (IoSessionEx) session;
            return new StunFrameEncoder(sessionEx.getBufferAllocator(),
                    (Map<String, String>) session.getAttribute(TURN_SESSION_TRANSACTION_MAP), sharedSecret, keyAlgorithm);
        }

        @Override
        public ProtocolDecoder getDecoder(IoSession session) throws Exception {
            IoSessionEx sessionEx = (IoSessionEx) session;
            return new StunFrameDecoder(sessionEx.getBufferAllocator(), stunAttributeFactory);
        }
    }
}
