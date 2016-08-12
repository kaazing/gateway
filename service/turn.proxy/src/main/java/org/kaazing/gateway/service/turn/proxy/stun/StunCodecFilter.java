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

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.mina.filter.codec.ProtocolCodecFilter;

import java.security.cert.Certificate;
import java.util.concurrent.ConcurrentMap;

import static org.kaazing.gateway.service.turn.proxy.stun.StunAttributeFactory.CredentialType.SHORT_TERM;

public class StunCodecFilter extends ProtocolCodecFilter {

    public StunCodecFilter(ConcurrentMap<String, String> currentTransactions, Certificate sharedSecret){
        super(new TurnCodecFactory(currentTransactions, sharedSecret));
    }
    
    private static class TurnCodecFactory implements ProtocolCodecFactory {

        private final ConcurrentMap<String, String> currentTransactions;
        private final Certificate sharedSecret;
        private StunAttributeFactory stunAttributeFactory = new StunAttributeFactory(SHORT_TERM);

        public TurnCodecFactory(ConcurrentMap<String, String> currentTransactions, Certificate sharedSecret) {
            this.currentTransactions = currentTransactions;
            this.sharedSecret = sharedSecret;
        }

        @Override
        public ProtocolEncoder getEncoder(IoSession session) throws Exception {
            IoSessionEx sessionEx = (IoSessionEx) session;
            return new StunFrameEncoder(sessionEx.getBufferAllocator(), currentTransactions, sharedSecret);
        }

        @Override
        public ProtocolDecoder getDecoder(IoSession session) throws Exception {
            IoSessionEx sessionEx = (IoSessionEx) session;
            return new StunFrameDecoder(sessionEx.getBufferAllocator(), stunAttributeFactory);
        }
    }
}
