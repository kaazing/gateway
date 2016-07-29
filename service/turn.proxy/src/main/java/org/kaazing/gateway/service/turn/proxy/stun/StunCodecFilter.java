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

import static org.kaazing.gateway.service.turn.proxy.stun.StunAttributeFactory.CredentialType.SHORT_TERM;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.mina.filter.codec.ProtocolCodecFilter;

public class StunCodecFilter extends ProtocolCodecFilter {

    public StunCodecFilter(){
        super(new TurnCodecFactory());
    }
    
    private static class TurnCodecFactory implements ProtocolCodecFactory{

        /**
         * Always will be short term credentials, TODO init properly here
         */
        private StunAttributeFactory stunAttributeFactory = new StunAttributeFactory(SHORT_TERM);

        @Override
        public ProtocolEncoder getEncoder(IoSession session) throws Exception {
            IoSessionEx sessionEx = (IoSessionEx) session;
            IoBufferAllocatorEx<?> allocator = sessionEx.getBufferAllocator();

            return new StunFrameEncoder(allocator);
        }

        @Override
        public ProtocolDecoder getDecoder(IoSession session) throws Exception {
            IoSessionEx sessionEx = (IoSessionEx) session;
            IoBufferAllocatorEx<?> allocator = sessionEx.getBufferAllocator();
            return new StunFrameDecoder(stunAttributeFactory, allocator);
        }
        
    }
}
