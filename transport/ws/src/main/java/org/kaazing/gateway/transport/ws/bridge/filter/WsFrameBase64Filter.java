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

import java.nio.ByteBuffer;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.kaazing.gateway.transport.ws.WsBinaryMessage;
import org.kaazing.gateway.transport.ws.WsFilterAdapter;
import org.kaazing.gateway.transport.ws.WsTextMessage;
import org.kaazing.gateway.util.Encoding;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.session.IoSessionEx;

public class WsFrameBase64Filter extends WsFilterAdapter {

	private final Encoding base64;
	
	public WsFrameBase64Filter() {
		this.base64 = Encoding.BASE64;
	}
	
	@Override
    protected Object doFilterWriteWsBinary(NextFilter nextFilter, IoSession session, WriteRequest writeRequest, WsBinaryMessage wsBinary) throws Exception {
	    IoBufferEx binaryEx = wsBinary.getBytes();
        ByteBuffer binary = binaryEx.buf();
        ByteBuffer encoded = base64.encode(binary);
        IoSessionEx sessionEx = (IoSessionEx) session;
        IoBufferAllocatorEx<?> allocator = sessionEx.getBufferAllocator();
        IoBufferEx encodedEx = allocator.wrap(encoded, binaryEx.flags());
        return new WsTextMessage(encodedEx);
	}

	@Override
	protected void wsTextReceived(NextFilter nextFilter, IoSession session, WsTextMessage wsText) throws Exception {
        IoBufferEx encodedEx = wsText.getBytes();
        ByteBuffer encoded = encodedEx.buf();
        ByteBuffer binary = base64.decode(encoded);
        IoSessionEx sessionEx = (IoSessionEx) session;
        IoBufferAllocatorEx<?> allocator = sessionEx.getBufferAllocator();
        IoBufferEx binaryEx = allocator.wrap(binary, encodedEx.flags());
        super.wsBinaryReceived(nextFilter, session, new WsBinaryMessage(binaryEx));
	}
	
    @Override
    protected Object doFilterWriteWsText(NextFilter nextFilter, IoSession session, WriteRequest writeRequest, WsTextMessage wsText) throws Exception {
        throw new IllegalStateException("Base64 encoding supported for binary frames only");
    }

    @Override
    protected void wsBinaryReceived(NextFilter nextFilter, IoSession session, WsBinaryMessage wsBinary) throws Exception {
        throw new IllegalStateException("Base64 decoding supported for text frames only");
    }
}
