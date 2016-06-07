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

import java.nio.ByteBuffer;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.kaazing.gateway.transport.TypedAttributeKey;
import org.kaazing.gateway.util.DecodingState;
import org.kaazing.gateway.util.Encoding;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.mina.filter.util.WriteRequestFilterEx;

public class EncodingFilter extends WriteRequestFilterEx {

	private final Encoding encoding;

	public EncodingFilter(Encoding encoding) {
		this.encoding = encoding;
	}
	
	@Override
    protected Object doFilterWrite(NextFilter nextFilter, IoSession session, WriteRequest writeRequest, Object message) throws Exception {
        IoBufferEx decodedEx = (IoBufferEx) message;
        
        // encoded messages cause a subsequent call to filterWrite 
        // with original Object but empty IoBuffer as message
        // so detect empty IoBuffer to avoid unnecessary processing
        if (!decodedEx.hasRemaining()) {
            return null;
        }

        ByteBuffer decoded = decodedEx.buf();
        ByteBuffer encoded = encoding.encode(decoded);

        IoSessionEx sessionEx = (IoSessionEx) session;
        IoBufferAllocatorEx<?> allocator = sessionEx.getBufferAllocator();
        IoBufferEx encodedEx = allocator.wrap(encoded, decodedEx.flags());

        return encodedEx;
    }

    @Override
    public void messageReceived(NextFilter nextFilter, IoSession session, Object message) throws Exception {
        IoBufferEx encodedEx = (IoBufferEx) message;
        ByteBuffer encoded = encodedEx.buf();
        ByteBuffer decoded = encoding.decode(encoded, new SessionDecodingState(session));
        IoSessionEx sessionEx = (IoSessionEx) session;
        IoBufferAllocatorEx<?> allocator = sessionEx.getBufferAllocator();
        IoBufferEx decodedEx = allocator.wrap(decoded, encodedEx.flags());
        super.messageReceived(nextFilter, session, decodedEx);
    }
    
    private static class SessionDecodingState implements DecodingState {
        static TypedAttributeKey<Object> DECODING_STATE =
                new TypedAttributeKey<>(EncodingFilter.class,
                        "decodingState");
        
        private IoSession session;
        
        SessionDecodingState(IoSession session) {
            super();
            this.session = session;
        }

        @Override
        public Object get() {
            return DECODING_STATE.get(session);
        }

        @Override
        public void set(Object state) {
            DECODING_STATE.set(session, state);
        }        
    }

}
