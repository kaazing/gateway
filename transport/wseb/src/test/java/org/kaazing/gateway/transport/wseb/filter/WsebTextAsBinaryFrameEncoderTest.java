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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.kaazing.mina.core.buffer.IoBufferEx.FLAG_SHARED;
import static org.kaazing.mina.core.buffer.IoBufferEx.FLAG_ZERO_COPY;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.junit.Test;
import org.kaazing.gateway.transport.BridgeCodecSession;
import org.kaazing.gateway.transport.bridge.CachingMessageEncoder;
import org.kaazing.gateway.transport.ws.Command;
import org.kaazing.gateway.transport.ws.WsBinaryMessage;
import org.kaazing.gateway.transport.ws.WsCommandMessage;
import org.kaazing.gateway.transport.ws.WsMessage;
import org.kaazing.gateway.transport.ws.WsTextMessage;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.buffer.SimpleBufferAllocator;
import org.kaazing.mina.filter.codec.ProtocolCodecSessionEx;


public class WsebTextAsBinaryFrameEncoderTest {

	private static final Charset UTF_8 = Charset.forName("UTF-8");
	
	@Test
	public void testEncodeBinaryFrame() throws Exception {
		ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
		IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
		ProtocolEncoder encoder = new WsebTextAsBinaryFrameEncoder(allocator);

		IoBufferEx buf = allocator.wrap(allocator.allocate(200)).fill((byte)0x97, 200).flip();
        WsMessage in = new WsBinaryMessage(buf);
		
		encoder.encode(session, in, session.getEncoderOutput());
		
		IoBufferEx out = (IoBufferEx) session.getEncoderOutputQueue().poll();
		assertEquals(allocator.wrap(allocator.allocate(203))
                              .put((byte)0x80)
                              .put((byte)-0x7f)
                              .put((byte)0x48)
                              .fill((byte)0x97, 200)
                              .flip(), 
				     out);
        assertNotSame(buf.array(), out.array());
	}

    @Test
    public void testEncodeBinaryFrameZeroCopy() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder encoder = new WsebTextAsBinaryFrameEncoder(allocator);

        WsebBufferAllocator wsAllocator = new WsebBufferAllocator(allocator);
        IoBufferEx buf = wsAllocator.wrap(wsAllocator.allocate(200, FLAG_ZERO_COPY)).fillAndReset((byte)0x97, 200);
        WsMessage in = new WsBinaryMessage(buf);

        encoder.encode(session, in, session.getEncoderOutput());

        IoBufferEx out = (IoBufferEx) session.getEncoderOutputQueue().poll();
        assertEquals(allocator.wrap(allocator.allocate(203))
                              .put((byte)0x80)
                              .put((byte)-0x7f)
                              .put((byte)0x48)
                              .fill((byte)0x97, 200)
                              .flip(), 
                     out);
        assertSame(buf.array(), out.array());
    }
    
    @Test
    public void testEncodeCloseCommandMessage() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder encoder = new WsebTextAsBinaryFrameEncoder(allocator);

        WsCommandMessage in = new WsCommandMessage(Command.close());
        
        encoder.encode(session, in, session.getEncoderOutput());
        
        IoBufferEx out = (IoBufferEx) session.getEncoderOutputQueue().poll();
        assertEquals(allocator.wrap(allocator.allocate(4))
                              .put((byte) 0x01)
                              .put((byte) 0x30)
                              .put((byte) 0x32)
                              .put((byte) 0xFF)
                              .flip(), 
                     out);
    }
    
	@Test
	public void testEncodeTextFrame() throws Exception {
		ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
		ProtocolEncoder encoder = new WsebTextAsBinaryFrameEncoder(allocator);

		IoBufferEx buf = allocator.wrap(ByteBuffer.wrap("Hello, world".getBytes(UTF_8)));
        WsMessage in = new WsTextMessage(buf);
		
		encoder.encode(session, in, session.getEncoderOutput());
		
		IoBufferEx out = (IoBufferEx) session.getEncoderOutputQueue().poll();
		assertEquals(allocator.wrap(allocator.allocate(14))
                              .put((byte)0x80)
                              .put((byte) 12)
                              .putString("Hello, world", UTF_8.newEncoder())
                              .flip(), 
				     out);
        assertNotSame(buf.array(), out.array());
	}

    @Test
    public void testEncodeTextFrameZeroCopy() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        WsebBufferAllocator wsebAllocator = new WsebBufferAllocator(allocator);
        ProtocolEncoder encoder = new WsebTextAsBinaryFrameEncoder(wsebAllocator);

        byte[] textBytes = "Hello, world".getBytes(UTF_8);
        IoBufferEx buf = wsebAllocator.wrap(wsebAllocator.allocate(textBytes.length, FLAG_ZERO_COPY));
        int pos = buf.position();
        buf.put(textBytes);
        buf.position(pos);
        WsMessage in = new WsTextMessage(buf);

        encoder.encode(session, in, session.getEncoderOutput());

        IoBufferEx out = (IoBufferEx) session.getEncoderOutputQueue().poll();
        assertEquals(allocator.wrap(allocator.allocate(14))
                              .put((byte)0x80)
                              .put((byte)12)
                              .putString("Hello, world", UTF_8.newEncoder())
                              .flip(), 
                     out);
        assertSame(buf.array(), out.array());
    }

    @Test
    public void testEncodeTextFrameSharedZeroCopy() throws Exception {
        IoBufferAllocatorEx<?> allocator = SimpleBufferAllocator.BUFFER_ALLOCATOR;
        WsebBufferAllocator wsebAllocator = new WsebBufferAllocator(allocator);

        BridgeCodecSession primary = new BridgeCodecSession("primary");
        ProtocolEncoderOutput primaryOutput = primary.getEncoderOutput();
        CachingMessageEncoder primaryMessageEncoder = primary.getMessageEncoder();

        BridgeCodecSession secondary = new BridgeCodecSession("secondary");
        ProtocolEncoderOutput secondaryOutput = secondary.getEncoderOutput();
        CachingMessageEncoder secondaryMessageEncoder = secondary.getMessageEncoder();

        ProtocolEncoder primaryEncoder = new WsebTextAsBinaryFrameEncoder(primaryMessageEncoder, allocator);
        ProtocolEncoder primaryEncoder2 = new WsebTextAsBinaryFrameEncoder(primaryMessageEncoder, allocator);

        ProtocolEncoder secondaryEncoder = new WsebTextAsBinaryFrameEncoder(secondaryMessageEncoder, allocator);
        ProtocolEncoder secondaryEncoder2 = new WsebTextAsBinaryFrameEncoder(secondaryMessageEncoder, allocator);

        byte[] textBytes = "Hello, world".getBytes(UTF_8);
        IoBufferEx buf = wsebAllocator.wrap(wsebAllocator.allocate(textBytes.length, FLAG_ZERO_COPY), FLAG_SHARED);
        int pos = buf.position();
        buf.put(textBytes);
        buf.position(pos);
        WsMessage in = new WsTextMessage(buf);
        in.initCache();

        primaryEncoder.encode(primary, in, primaryOutput);
        primaryEncoder2.encode(primary, in, primaryOutput);
        secondaryEncoder.encode(secondary, in, secondaryOutput);
        secondaryEncoder2.encode(secondary, in, secondaryOutput);

        IoBufferEx primaryOut = (IoBufferEx) primary.getEncoderOutputQueue().poll();
        IoBufferEx primaryOut2 = (IoBufferEx) primary.getEncoderOutputQueue().poll();
        IoBufferEx secondaryOut = (IoBufferEx) secondary.getEncoderOutputQueue().poll();
        IoBufferEx secondaryOut2 = (IoBufferEx) secondary.getEncoderOutputQueue().poll();
        
        IoBufferEx expected = allocator.wrap(allocator.allocate(14), FLAG_SHARED)
                                       .put((byte)0x80)
                                       .put((byte)12)
                                       .putString("Hello, world", UTF_8.newEncoder())
                                       .flip();

        assertEquals(expected, primaryOut);
        assertSame(buf.array(), primaryOut.array());
        
        assertEquals(expected, primaryOut2);
        assertSame(buf.array(), primaryOut2.array());
        
        assertEquals(expected, secondaryOut);
        assertNotSame(buf.array(), secondaryOut.array());

        assertEquals(expected, secondaryOut2);
        assertNotSame(buf.array(), secondaryOut2.array());

        assertSame(primaryOut.array(), primaryOut2.array());
        assertSame(secondaryOut.array(), secondaryOut2.array());
    }

    @Test
    public void testEncodeBinaryFrameSharedZeroCopy() throws Exception {
        IoBufferAllocatorEx<?> allocator = SimpleBufferAllocator.BUFFER_ALLOCATOR;
        WsebBufferAllocator wsebAllocator = new WsebBufferAllocator(allocator);

        BridgeCodecSession primary = new BridgeCodecSession("primary");
        ProtocolEncoderOutput primaryOutput = primary.getEncoderOutput();
        CachingMessageEncoder primaryMessageEncoder = primary.getMessageEncoder();

        BridgeCodecSession secondary = new BridgeCodecSession("secondary");
        ProtocolEncoderOutput secondaryOutput = secondary.getEncoderOutput();
        CachingMessageEncoder secondaryMessageEncoder = secondary.getMessageEncoder();

        ProtocolEncoder primaryEncoder = new WsebTextAsBinaryFrameEncoder(primaryMessageEncoder, allocator);
        ProtocolEncoder primaryEncoder2 = new WsebTextAsBinaryFrameEncoder(primaryMessageEncoder, allocator);

        ProtocolEncoder secondaryEncoder = new WsebTextAsBinaryFrameEncoder(secondaryMessageEncoder, allocator);
        ProtocolEncoder secondaryEncoder2 = new WsebTextAsBinaryFrameEncoder(secondaryMessageEncoder, allocator);

        IoBufferEx buf = wsebAllocator.wrap(wsebAllocator.allocate(200, FLAG_ZERO_COPY), FLAG_SHARED).fillAndReset((byte)0x97, 200);
        WsMessage in = new WsBinaryMessage(buf);
        in.initCache();
        
        primaryEncoder.encode(primary, in, primaryOutput);
        primaryEncoder2.encode(primary, in, primaryOutput);
        secondaryEncoder.encode(secondary, in, secondaryOutput);
        secondaryEncoder2.encode(secondary, in, secondaryOutput);

        IoBuffer primaryOut = (IoBuffer)primary.getEncoderOutputQueue().poll();
        IoBuffer primaryOut2 = (IoBuffer)primary.getEncoderOutputQueue().poll();
        IoBuffer secondaryOut = (IoBuffer)secondary.getEncoderOutputQueue().poll();
        IoBuffer secondaryOut2 = (IoBuffer)secondary.getEncoderOutputQueue().poll();
        
        IoBufferEx expected = allocator.wrap(allocator.allocate(203), FLAG_SHARED)
                                       .put((byte)0x80)
                                       .put((byte)-0x7f)
                                       .put((byte)0x48)
                                       .fill((byte)0x97, 200)
                                       .flip();

        assertEquals(expected, primaryOut);
        assertSame(buf.array(), primaryOut.array());
        
        assertEquals(expected, primaryOut2);
        assertSame(buf.array(), primaryOut2.array());
        
        assertEquals(expected, secondaryOut);
        assertNotSame(buf.array(), secondaryOut.array());

        assertEquals(expected, secondaryOut2);
        assertNotSame(buf.array(), secondaryOut2.array());

        assertSame(primaryOut.array(), primaryOut2.array());
        assertSame(secondaryOut.array(), secondaryOut2.array());
    }

}
