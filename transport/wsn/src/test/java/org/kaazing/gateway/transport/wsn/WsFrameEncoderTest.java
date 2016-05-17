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
package org.kaazing.gateway.transport.wsn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.kaazing.mina.core.buffer.IoBufferEx.FLAG_SHARED;
import static org.kaazing.mina.core.buffer.IoBufferEx.FLAG_ZERO_COPY;

import java.nio.ByteBuffer;

import org.apache.mina.filter.codec.ProtocolEncoder;
import org.junit.Test;
import org.kaazing.gateway.transport.ws.WsBinaryMessage;
import org.kaazing.gateway.transport.ws.WsMessage;
import org.kaazing.gateway.transport.ws.WsPingMessage;
import org.kaazing.gateway.transport.ws.bridge.filter.WsBufferAllocator;
import org.kaazing.gateway.transport.ws.bridge.filter.WsFrameEncoder;
import org.kaazing.gateway.transport.ws.bridge.filter.WsFrameEncodingSupport;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.buffer.SimpleBufferAllocator;
import org.kaazing.mina.filter.codec.ProtocolCodecSessionEx;

public class WsFrameEncoderTest {

	@Test
	public void encode() throws Exception {		
		byte[] input = "abcd".getBytes();
		byte[] expected = {(byte)0x82,(byte)0x04,(byte)0x61,(byte)0x62,(byte)0x63,(byte)0x64};
		WsBufferAllocator allocator = new WsBufferAllocator(SimpleBufferAllocator.BUFFER_ALLOCATOR);
        int flags = FLAG_ZERO_COPY;
		WsBinaryMessage message = new WsBinaryMessage(allocator.wrap(ByteBuffer.wrap(input), flags));
		IoBufferEx out = WsFrameEncodingSupport.doEncode(allocator, flags, message);
		assertEquals(out, allocator.wrap(ByteBuffer.wrap(expected), flags));
	}
	
	@Test
	public void encodeZeroCopy() throws Exception {		
		byte[] input = "abcd".getBytes();
		byte[] expected = {(byte)0x82,(byte)0x04,(byte)0x61,(byte)0x62,(byte)0x63,(byte)0x64};
		WsBufferAllocator allocator = new WsBufferAllocator(SimpleBufferAllocator.BUFFER_ALLOCATOR);
		
		int flags = FLAG_SHARED | FLAG_ZERO_COPY;
		ByteBuffer in = allocator.allocate(4, flags);
		in.mark();
		in.put(ByteBuffer.wrap(input));
		in.reset();
		WsBinaryMessage message = new WsBinaryMessage(allocator.wrap(in, flags));
		
		IoBufferEx out = WsFrameEncodingSupport.doEncode(allocator, flags, message);
        assertEquals(out, allocator.wrap(ByteBuffer.wrap(expected), flags));
	}
    
    @Test
    public void encodePing() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder encoder = new WsFrameEncoder(allocator, false);

        IoBufferEx buf = allocator.wrap(allocator.allocate(200)).fill((byte)0x97, 200).flip();
        WsMessage in = new WsPingMessage(buf);
        
        encoder.encode(session, in, session.getEncoderOutput());
        
        IoBufferEx out = (IoBufferEx) session.getEncoderOutputQueue().poll();
        assertEquals(allocator.wrap(allocator.allocate(204))
                              .put((byte)0x89)
                              .put((byte)0x7E)
                              .put((byte)0x00)
                              .put((byte)200)
                              .fill((byte)0x97, 200)
                              .flip(), 
                     out);
        assertNotSame(buf.array(), out.array());
    }
    
    
	
}
