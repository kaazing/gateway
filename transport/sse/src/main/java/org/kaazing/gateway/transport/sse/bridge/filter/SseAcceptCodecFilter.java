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
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderAdapter;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.kaazing.gateway.transport.BridgeSession;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.mina.filter.codec.ProtocolCodecFilter;

public class SseAcceptCodecFilter extends ProtocolCodecFilter {
	public SseAcceptCodecFilter() {
		super(new SseAcceptCodecFactory());
	}	

	private static class SseAcceptCodecFactory implements ProtocolCodecFactory {
	    
	    @Override
		public ProtocolEncoder getEncoder(IoSession session) {
            IoSessionEx sessionEx = (IoSessionEx) session;
            IoBufferAllocatorEx<?> allocator = sessionEx.getBufferAllocator();
            
	        if (session instanceof BridgeSession) {
	            BridgeSession bridgeSession = (BridgeSession)session;
	            return new SseEncoder(bridgeSession.getMessageEncoder(), allocator);
	        }
	        return new SseEncoder(allocator);
	    }
	    
	    @Override
		public ProtocolDecoder getDecoder(IoSession session) {
	        return new ProtocolDecoderAdapter() {
                @Override
				public void decode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) {
                    in.skip(in.remaining());
                }
            };
	    }
	}

}
