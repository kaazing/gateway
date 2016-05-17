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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.kaazing.gateway.util.Utils.asByteBuffer;
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
import org.kaazing.gateway.transport.ws.WsBinaryMessage;
import org.kaazing.gateway.transport.ws.WsCloseMessage;
import org.kaazing.gateway.transport.ws.WsContinuationMessage;
import org.kaazing.gateway.transport.ws.WsMessage;
import org.kaazing.gateway.transport.ws.WsPingMessage;
import org.kaazing.gateway.transport.ws.WsPongMessage;
import org.kaazing.gateway.transport.ws.WsTextMessage;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.buffer.SimpleBufferAllocator;
import org.kaazing.mina.filter.codec.ProtocolCodecSessionEx;


public class WsFrameEncoderTest {

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    @Test
    public void shouldEncodeNormalClose() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder encoder = new WsFrameEncoder(allocator, false);

        WsMessage in = WsCloseMessage.NORMAL_CLOSE;
        System.out.println("###### " + in.getBytes());

        encoder.encode(session, in, session.getEncoderOutput());

        IoBufferEx out = (IoBufferEx) session.getEncoderOutputQueue().poll();
        assertEquals(allocator.wrap(allocator.allocate(4))
                              .put((byte)0x88)
                              .put((byte)0x02)
                              .put((byte)0x03) // 1000 is 0x03E8
                              .put((byte)0xE8)
                              .flip(),
                     out);
    }

    @Test
    public void shouldEncodeUnexpectedConditionClose()
        throws Exception {

        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder encoder = new WsFrameEncoder(allocator, false);

        WsMessage in = WsCloseMessage.UNEXPECTED_CONDITION;

        encoder.encode(session, in, session.getEncoderOutput());

        IoBufferEx out = (IoBufferEx) session.getEncoderOutputQueue().poll();
        assertEquals(allocator.wrap(allocator.allocate(4))
                              .put((byte)0x88)
                              .put((byte)0x02)
                              .put((byte)0x03) // 1011 is 0x03F3
                              .put((byte)0xF3)
                              .flip(),
                     out);
    }

    @Test
    public void shouldEncodeCloseWithReason()
        throws Exception {

        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder encoder = new WsFrameEncoder(allocator, false);

        ByteBuffer reason = asByteBuffer("highly abnormal condition");
        WsCloseMessage in = new WsCloseMessage(1011, reason.duplicate());
        assertTrue(in.isFin());

        encoder.encode(session, in, session.getEncoderOutput());

        IoBufferEx out = (IoBufferEx) session.getEncoderOutputQueue().poll();
        assertEquals(allocator.wrap(allocator.allocate(4 + reason.remaining()))
                              .put((byte)0x88)
                              .put((byte)(2 + reason.remaining())) //payload length = status + reason
                              .put((byte)0x03) // 1011 is 0x03F3
                              .put((byte)0xF3)
                              .put(reason.duplicate())
                              .flip(),
                     out);
    }

    @Test
    public void testEncodeZeroLengthPingFrame() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder encoder = new WsFrameEncoder(allocator, false);

        IoBufferEx buf = allocator.wrap(allocator.allocate(0)).flip();
        WsMessage in = new WsPingMessage(buf);

        encoder.encode(session, in, session.getEncoderOutput());

        IoBufferEx out = (IoBufferEx) session.getEncoderOutputQueue().poll();
        assertEquals(allocator.wrap(allocator.allocate(2))
                              .put((byte)0x89)
                              .put((byte)0x00)
                              .flip(),
                     out);
        assertNotSame(buf.array(), out.array());
    }

    @Test
    public void testEncodeZeroLengthMaskedPongFrame() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder encoder = new WsFrameEncoder(allocator, true);

        IoBufferEx buf = allocator.wrap(allocator.allocate(0)).flip();
        WsMessage in = new WsPongMessage(buf);

        encoder.encode(session, in, session.getEncoderOutput());

        IoBuffer out = (IoBuffer)session.getEncoderOutputQueue().poll();
        assertEquals(out.remaining(), 6);
        assertEquals((byte)0x8A, out.get(0));
        assertEquals((byte)0x80, out.get(1));
        assertNotSame(buf.array(), out.array());
    }

    @Test
    public void testEncodeZeroLengthBinaryFrame() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder encoder = new WsFrameEncoder(allocator, false);

        IoBufferEx buf = allocator.wrap(allocator.allocate(0)).flip();
        WsMessage in = new WsBinaryMessage(buf);

        encoder.encode(session, in, session.getEncoderOutput());

        IoBufferEx out = (IoBufferEx) session.getEncoderOutputQueue().poll();
        assertEquals(allocator.wrap(allocator.allocate(2))
                              .put((byte)0x82)
                              .put((byte) 0x00)
                              .flip(),
                     out);
        assertNotSame(buf.array(), out.array());
    }

    @Test
    public void testEncodeBinaryFrame() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder encoder = new WsFrameEncoder(allocator, false);

        IoBufferEx buf = allocator.wrap(allocator.allocate(200)).fill((byte)0x97, 200).flip();
        WsMessage in = new WsBinaryMessage(buf);

        encoder.encode(session, in, session.getEncoderOutput());

        IoBufferEx out = (IoBufferEx) session.getEncoderOutputQueue().poll();
        assertEquals(allocator.wrap(allocator.allocate(204))
                              .put((byte) 0x82)
                              .put((byte) 126)
                              .put((byte) 0x00)
                              .put((byte) 0xC8)
                              .fill((byte) 0x97, 200)
                              .flip(), 
                     out);
        assertNotSame(buf.array(), out.array());
    }

    @Test
    public void testEncodeBinaryFrameZeroCopy() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder encoder = new WsFrameEncoder(allocator, false);

        WsBufferAllocator wsAllocator = new WsBufferAllocator(allocator);
        IoBufferEx buf = wsAllocator.wrap(wsAllocator.allocate(200, FLAG_ZERO_COPY)).fillAndReset((byte)0x97, 200);
        WsMessage in = new WsBinaryMessage(buf);

        encoder.encode(session, in, session.getEncoderOutput());

        IoBufferEx out = (IoBufferEx) session.getEncoderOutputQueue().poll();
        assertEquals(allocator.wrap(allocator.allocate(204))
                              .put((byte) 0x82)
                              .put((byte) 126)
                              .put((byte) 0x00)
                              .put((byte) 0xC8)
                              .fill((byte) 0x97, 200)
                              .flip(), 
                     out);
        assertSame(buf.array(), out.array());
    }

    @Test
    public void testEncodeZeroLengthTextFrame() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder encoder = new WsFrameEncoder(allocator, false);

        IoBufferEx buf = allocator.wrap(allocator.allocate(0)).flip();
        WsMessage in = new WsTextMessage(buf);

        encoder.encode(session, in, session.getEncoderOutput());

        IoBufferEx out = (IoBufferEx) session.getEncoderOutputQueue().poll();
        assertEquals(allocator.wrap(allocator.allocate(2))
                              .put((byte) 0x81)
                              .put((byte)0x00)
                              .flip(),
                     out);
        assertNotSame(buf.array(), out.array());
    }

    @Test
    public void testEncodeTextFrame() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder encoder = new WsFrameEncoder(allocator, false);

        IoBufferEx buf = allocator.wrap(ByteBuffer.wrap("Hello, world".getBytes(UTF_8)));
        WsMessage in = new WsTextMessage(buf);
        
        encoder.encode(session, in, session.getEncoderOutput());
        
        IoBufferEx out = (IoBufferEx) session.getEncoderOutputQueue().poll();
        assertEquals(allocator.wrap(allocator.allocate(14))
                              .put((byte) 0x81)
                              .put((byte) 12)
                              .putString("Hello, world", UTF_8.newEncoder())
                              .flip(), 
                     out);
        assertNotSame(buf.array(), out.array());
    }

    @Test
    public void testEncodeTextContinuationFrame() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder encoder = new WsFrameEncoder(allocator, false);

        IoBufferEx buf1 = allocator.wrap(ByteBuffer.wrap("a".getBytes(UTF_8)));
        WsMessage in = new WsTextMessage(buf1, false);
        encoder.encode(session, in, session.getEncoderOutput());

        IoBufferEx buf2 = allocator.wrap(ByteBuffer.wrap("b".getBytes(UTF_8)));
        WsMessage cont = new WsContinuationMessage(buf2, true);
        encoder.encode(session, cont, session.getEncoderOutput());

        IoBufferEx buf3 = allocator.wrap(ByteBuffer.wrap("c".getBytes(UTF_8)));
        WsMessage binary = new WsBinaryMessage(buf3, true);
        encoder.encode(session, binary, session.getEncoderOutput());

        IoBufferEx out1 = (IoBufferEx) session.getEncoderOutputQueue().poll();
        assertEquals(allocator.wrap(allocator.allocate(3))
                .put((byte) 0x01).put((byte) 0x01).put((byte)'a')
                .flip(),
                out1);
        assertNotSame(buf1.array(), out1.array());

        IoBufferEx out2 = (IoBufferEx) session.getEncoderOutputQueue().poll();
        assertEquals(allocator.wrap(allocator.allocate(3))
                .put((byte) 0x80).put((byte) 0x01).put((byte)'b')
                .flip(),
                out2);
        assertNotSame(buf2.array(), out2.array());

        IoBufferEx out3 = (IoBufferEx) session.getEncoderOutputQueue().poll();
        assertEquals(allocator.wrap(allocator.allocate(3))
                .put((byte) 0x82).put((byte) 0x01).put((byte)'c')
                .flip(),
                out3);
        assertNotSame(buf3.array(), out3.array());
    }

    @Test
    public void testEncodeTextFrameZeroCopy() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder encoder = new WsFrameEncoder(allocator, false);

        WsBufferAllocator wsAllocator = new WsBufferAllocator(allocator);
        byte[] textBytes = "Hello, world".getBytes(UTF_8);
        IoBufferEx buf = wsAllocator.wrap(wsAllocator.allocate(textBytes.length, FLAG_ZERO_COPY));
        int pos = buf.position();
        buf.put(textBytes);
        buf.position(pos);
        WsMessage in = new WsTextMessage(buf);
        
        encoder.encode(session, in, session.getEncoderOutput());
        
        IoBufferEx out = (IoBufferEx) session.getEncoderOutputQueue().poll();
        assertEquals(allocator.wrap(allocator.allocate(14))
                              .put((byte)0x81)
                              .put((byte) 12)
                              .putString("Hello, world", UTF_8.newEncoder())
                              .flip(), 
                     out);
        assertSame(buf.array(), out.array());
    }

    @Test
    public void testEncodeTextFrameSharedZeroCopy() throws Exception {
        IoBufferAllocatorEx<?> allocator = SimpleBufferAllocator.BUFFER_ALLOCATOR;
        WsBufferAllocator wsAllocator = new WsBufferAllocator(allocator);

        BridgeCodecSession primary = new BridgeCodecSession("primary");
        ProtocolEncoderOutput primaryOutput = primary.getEncoderOutput();
        CachingMessageEncoder primaryMessageEncoder = primary.getMessageEncoder();

        BridgeCodecSession secondary = new BridgeCodecSession("secondary");
        ProtocolEncoderOutput secondaryOutput = secondary.getEncoderOutput();
        CachingMessageEncoder secondaryMessageEncoder = secondary.getMessageEncoder();

        ProtocolEncoder primaryEncoder = new WsFrameEncoder(primaryMessageEncoder, allocator, false);
        ProtocolEncoder primaryEncoder2 = new WsFrameEncoder(primaryMessageEncoder, allocator, false);

        ProtocolEncoder secondaryEncoder = new WsFrameEncoder(secondaryMessageEncoder, allocator, false);
        ProtocolEncoder secondaryEncoder2 = new WsFrameEncoder(secondaryMessageEncoder, allocator, false);

        byte[] textBytes = "Hello, world".getBytes(UTF_8);
        IoBufferEx buf = wsAllocator.wrap(wsAllocator.allocate(textBytes.length, FLAG_ZERO_COPY), FLAG_SHARED);
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
                                       .put((byte)0x81)
                                       .put((byte) 12)
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
        WsBufferAllocator wsAllocator = new WsBufferAllocator(allocator);

        BridgeCodecSession primary = new BridgeCodecSession("primary");
        ProtocolEncoderOutput primaryOutput = primary.getEncoderOutput();
        CachingMessageEncoder primaryMessageEncoder = primary.getMessageEncoder();

        BridgeCodecSession secondary = new BridgeCodecSession("secondary");
        ProtocolEncoderOutput secondaryOutput = secondary.getEncoderOutput();
        CachingMessageEncoder secondaryMessageEncoder = secondary.getMessageEncoder();

        ProtocolEncoder primaryEncoder = new WsFrameEncoder(primaryMessageEncoder, allocator, false);
        ProtocolEncoder primaryEncoder2 = new WsFrameEncoder(primaryMessageEncoder, allocator, false);

        ProtocolEncoder secondaryEncoder = new WsFrameEncoder(secondaryMessageEncoder, allocator, false);
        ProtocolEncoder secondaryEncoder2 = new WsFrameEncoder(secondaryMessageEncoder, allocator, false);

        IoBufferEx buf = wsAllocator.wrap(wsAllocator.allocate(200, FLAG_ZERO_COPY), FLAG_SHARED).fillAndReset((byte)0x97, 200);
        WsMessage in = new WsBinaryMessage(buf);
        in.initCache();
        
        primaryEncoder.encode(primary, in, primaryOutput);
        primaryEncoder2.encode(primary, in, primaryOutput);
        secondaryEncoder.encode(secondary, in, secondaryOutput);
        secondaryEncoder2.encode(secondary, in, secondaryOutput);

        IoBufferEx primaryOut = (IoBufferEx) primary.getEncoderOutputQueue().poll();
        IoBufferEx primaryOut2 = (IoBufferEx) primary.getEncoderOutputQueue().poll();
        IoBufferEx secondaryOut = (IoBufferEx) secondary.getEncoderOutputQueue().poll();
        IoBufferEx secondaryOut2 = (IoBufferEx) secondary.getEncoderOutputQueue().poll();
        
        IoBufferEx expected = allocator.wrap(allocator.allocate(204), FLAG_SHARED)
                                       .put((byte)0x82)
                                       .put((byte) 126)
                                       .put((byte)0x00)
                                       .put((byte) 200)
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
