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

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.kaazing.gateway.transport.BridgeSession;
import org.kaazing.gateway.transport.http.HttpProtocolCodecFilter;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.session.IoSessionEx;

public class HttpCodecFilter extends HttpProtocolCodecFilter {

	public HttpCodecFilter(boolean client) {
		super(client ? new HttpClientCodecFactory() : new HttpServerCodecFactory());
	}
    
    private static class HttpClientCodecFactory implements ProtocolCodecFactory {
        
        @Override
        public ProtocolEncoder getEncoder(IoSession session) {
            IoSessionEx sessionEx = (IoSessionEx) session;
            IoBufferAllocatorEx<?> allocator = sessionEx.getBufferAllocator();
            
            if (session instanceof BridgeSession) {
                BridgeSession bridgeSession = (BridgeSession)session;
                return new HttpRequestEncoder(bridgeSession.getMessageEncoder(), allocator);
            }
            
            return new HttpRequestEncoder(allocator);
        }
        
        @Override
        public ProtocolDecoder getDecoder(IoSession session) {
            return new HttpResponseDecoder();
        }
    }
    
    private static class HttpServerCodecFactory implements ProtocolCodecFactory {
        
        @Override
        public ProtocolEncoder getEncoder(IoSession session) {
            IoSessionEx sessionEx = (IoSessionEx) session;
            IoBufferAllocatorEx<?> allocator = sessionEx.getBufferAllocator();
            
            if (session instanceof BridgeSession) {
                BridgeSession bridgeSession = (BridgeSession)session;
                return new HttpResponseEncoder(bridgeSession.getMessageEncoder(), allocator);
            }
            
            return new HttpResponseEncoder(allocator);
        }
        
        @Override
        public ProtocolDecoder getDecoder(IoSession session) {
            return new HttpRequestDecoder();
        }
    }

}
