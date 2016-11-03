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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.kaazing.gateway.transport.ws.WsBinaryMessage;
import org.kaazing.gateway.transport.ws.WsContinuationMessage;
import org.kaazing.gateway.transport.ws.WsMessage;
import org.kaazing.gateway.transport.ws.WsPingMessage;
import org.kaazing.gateway.transport.ws.WsPongMessage;
import org.kaazing.gateway.transport.ws.WsTextMessage;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.filter.codec.ProtocolCodecSessionEx;


public class WsFrameDecoderTest {

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void decodeZeroLengthPingFrame() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolDecoder decoder = new WsFrameDecoder(allocator, 0, false);

        IoBufferEx in = allocator.wrap(allocator.allocate(2))
                                 .put((byte)0x89)
                                 .put((byte)0x00)
                                 .flip();

        decoder.decode(session, (IoBuffer) in, session.getDecoderOutput());

        WsMessage out = (WsMessage)session.getDecoderOutputQueue().poll();
        assertEquals(new WsPingMessage(allocator.wrap(allocator.allocate(0))), out);

        assertTrue(session.getDecoderOutputQueue().isEmpty());
        decoder.finishDecode(session, session.getDecoderOutput());

        assertTrue(session.getDecoderOutputQueue().isEmpty());
        assertFalse(in.hasRemaining());
    }

    @Test
    public void decodeTextContinuationFrame() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolDecoder decoder = new WsFrameDecoder(allocator, 200, false);

        int firstPayload = 125;
        String first = createString('a', firstPayload);

        int secondPayload = 2;
        String second = createString('b', secondPayload);

        int thirdPayload = 4;
        String thrid = createString('c', thirdPayload);

        IoBufferEx in = allocator.wrap(allocator.allocate(firstPayload + secondPayload + thirdPayload + 6))
                // text frame
                .put((byte) 0x01)
                .put((byte) firstPayload)
                .putString(first, UTF_8.newEncoder())
                // continuation frame with FIN
                .put((byte) 0x80)
                .put((byte) secondPayload)
                .putString(second, UTF_8.newEncoder())
                // text frame with FIN
                .put((byte) 0x81)
                .put((byte) thirdPayload)
                .putString(thrid, UTF_8.newEncoder())
                .flip();

        decoder.decode(session, (IoBuffer) in, session.getDecoderOutput());

        WsMessage out1 = (WsMessage)session.getDecoderOutputQueue().poll();
        assertEquals(new WsTextMessage(allocator.wrap(ByteBuffer.wrap(first.getBytes(UTF_8))), false), out1);

        WsMessage out2 = (WsMessage)session.getDecoderOutputQueue().poll();
        assertEquals(new WsContinuationMessage(allocator.wrap(ByteBuffer.wrap(second.getBytes(UTF_8)))), out2);

        WsMessage out3 = (WsMessage)session.getDecoderOutputQueue().poll();
        assertEquals(new WsTextMessage(allocator.wrap(ByteBuffer.wrap(thrid.getBytes(UTF_8)))), out3);

        assertTrue(session.getDecoderOutputQueue().isEmpty());
        decoder.finishDecode(session, session.getDecoderOutput());

        assertTrue(session.getDecoderOutputQueue().isEmpty());
        assertFalse(in.hasRemaining());
    }

    @Test
    public void decodeBinaryContinuationFrame() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolDecoder decoder = new WsFrameDecoder(allocator, 200, false);

        int firstPayload = 125;
        byte[] first = createString('a', firstPayload).getBytes("UTF-8");

        int secondPayload = 2;
        byte[] second = createString('b', secondPayload).getBytes("UTF-8");

        int thirdPayload = 4;
        byte[] thrid = createString('c', thirdPayload).getBytes("UTF-8");

        IoBufferEx in = allocator.wrap(allocator.allocate(firstPayload + secondPayload + thirdPayload + 6))
                // binary frame
                .put((byte) 0x02)
                .put((byte) firstPayload)
                .put(first)
                // continuation frame with FIN
                .put((byte) 0x80)
                .put((byte) secondPayload)
                .put(second)
                // binary frame with FIN
                .put((byte) 0x82)
                .put((byte) thirdPayload)
                .put(thrid)
                .flip();

        decoder.decode(session, (IoBuffer) in, session.getDecoderOutput());

        WsMessage out1 = (WsMessage)session.getDecoderOutputQueue().poll();
        assertEquals(new WsBinaryMessage(allocator.wrap(ByteBuffer.wrap(first)), false), out1);

        WsMessage out2 = (WsMessage)session.getDecoderOutputQueue().poll();
        assertEquals(new WsContinuationMessage(allocator.wrap(ByteBuffer.wrap(second))), out2);

        WsMessage out3 = (WsMessage)session.getDecoderOutputQueue().poll();
        assertEquals(new WsBinaryMessage(allocator.wrap(ByteBuffer.wrap(thrid))), out3);

        assertTrue(session.getDecoderOutputQueue().isEmpty());
        decoder.finishDecode(session, session.getDecoderOutput());

        assertTrue(session.getDecoderOutputQueue().isEmpty());
        assertFalse(in.hasRemaining());
    }

    @Test
    public void decodeTextContinuationFrameWithFragmentedPayload() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolDecoder decoder = new WsFrameDecoder(allocator, 250, false);

        String textFramePayload = createString('a', 100);
        String continuationFramePayloadFirstFragment = createString('b', 50);
        String continuationFramePayloadSecondFragment = createString('b', 50);

        IoBufferEx in = allocator.wrap(allocator.allocate(102))
                // text frame
                .put((byte) 0x01)
                .put((byte) 100)
                .putString(textFramePayload, UTF_8.newEncoder())
                .flip();

        IoBufferEx[] array = new IoBufferEx[] { allocator.wrap(allocator.allocate(2))
                // continuation frame fragment (opcode and payload length)
                .put((byte) 0x80)
                .put((byte) 0x64)
                .flip(),
                allocator.wrap(allocator.allocate(50))
                        // continuation frame payload first fragment
                        .putString(continuationFramePayloadFirstFragment, UTF_8.newEncoder())
                        .flip(),
                allocator.wrap(allocator.allocate(50))
                        // continuation frame payload second fragment
                        .putString(continuationFramePayloadSecondFragment, UTF_8.newEncoder())
                        .flip()
        };
        decoder.decode(session, (IoBuffer) in, session.getDecoderOutput());

        for (IoBufferEx buffer : array) {
            decoder.decode(session, (IoBuffer) buffer, session.getDecoderOutput());
        }

        WsMessage out1 = (WsMessage)session.getDecoderOutputQueue().poll();
        assertEquals(new WsTextMessage(allocator.wrap(ByteBuffer.wrap(textFramePayload.getBytes(UTF_8))), false), out1);

        WsMessage out2 = (WsMessage)session.getDecoderOutputQueue().poll();
        assertEquals(new WsContinuationMessage(allocator.wrap(ByteBuffer.wrap(createString('b', 100).getBytes(UTF_8)))), out2);
    }

    @Test
    public void decodeBinaryContinuationFrameWithFragmentedPayload() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolDecoder decoder = new WsFrameDecoder(allocator, 250, false);

        byte[] binaryFramePayload = createString('a', 100).getBytes();
        byte[] continuationFramePayload = createString('b', 100).getBytes();

        IoBufferEx in = allocator.wrap(allocator.allocate(102))
                // binary frame
                .put((byte) 0x02)
                .put((byte) 100)
                .put(binaryFramePayload)
                .flip();

        IoBufferEx[] array = new IoBufferEx[] { allocator.wrap(allocator.allocate(2))
                // continuation frame fragment (opcode and payload length)
                .put((byte) 0x80)
                .put((byte) 0x64)
                .flip(),
                allocator.wrap(allocator.allocate(50))
                        // continuation frame first fragment
                        .put(continuationFramePayload,0, 50)
                        .flip(),
                allocator.wrap(allocator.allocate(50))
                        // continuation frame second fragment
                        .put(continuationFramePayload, 50, 50)
                        .flip()
        };
        decoder.decode(session, (IoBuffer) in, session.getDecoderOutput());

        for (IoBufferEx buffer : array) {
            decoder.decode(session, (IoBuffer) buffer, session.getDecoderOutput());
        }

        WsMessage out1 = (WsMessage)session.getDecoderOutputQueue().poll();
        assertEquals(new WsBinaryMessage(allocator.wrap(ByteBuffer.wrap(binaryFramePayload)), false), out1);

        WsMessage out2 = (WsMessage)session.getDecoderOutputQueue().poll();
        assertEquals(new WsContinuationMessage(allocator.wrap(ByteBuffer.wrap(continuationFramePayload))), out2);
    }

    @Test(expected = ProtocolDecoderException.class)
    public void decodeFragmentedContinuationFrameExceedingMaxMessageSize() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolDecoder decoder = new WsFrameDecoder(allocator, 150, false);

        String textFramePayload = createString('a', 100);

        IoBufferEx textFrameBuffer = allocator.wrap(allocator.allocate(102))
                // text frame
                .put((byte) 0x01)
                .put((byte) 100)
                .putString(textFramePayload, UTF_8.newEncoder())
                .flip();

        // the decoder should fail fast when message size exceeds the max message size without waiting for
        // payload
        IoBufferEx continuationFrameBuffer = allocator.wrap(allocator.allocate(2))
                // continuation frame fragment (opcode and payload length)
                .put((byte) 0x80)
                .put((byte) 0x64)
                .flip();

        decoder.decode(session, (IoBuffer) textFrameBuffer, session.getDecoderOutput());

        // since the maximum message size is 150, the following statement will
        // cause the message size(200) to exceed maximum message size
        // The decoder should fail fast once the payload length is decoded regardless of the
        // availability of payload
        decoder.decode(session, (IoBuffer) continuationFrameBuffer, session.getDecoderOutput());
    }

    @Test
    public void pingInTextContinuationSequence() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolDecoder decoder = new WsFrameDecoder(allocator, 500, false);

        int firstPayload = 125;
        String first = createString('a', firstPayload);

        int secondPayload = 125;
        String second = createString('b', secondPayload);

        int thirdPayload = 4;
        String third = createString('c', thirdPayload);

        int fourthPayload = 6;
        byte[] fourth = createString('d', fourthPayload).getBytes("UTF-8");

        IoBufferEx in = allocator.wrap(allocator.allocate(firstPayload+secondPayload+2+thirdPayload+fourthPayload+8))
                // text frame
                .put((byte) 0x01)
                .put((byte) firstPayload)
                .putString(first, UTF_8.newEncoder())
                // continuation frame
                .put((byte) 0x00)
                .put((byte) secondPayload)
                .putString(second, UTF_8.newEncoder())
                // ping frame
                .put((byte) 0x89)
                .put((byte) 0x00)
                // continuation frame with FIN
                .put((byte) 0x80)
                .put((byte) thirdPayload)
                .putString(third, UTF_8.newEncoder())
                // binary frame
                .put((byte) 0x82)
                .put((byte) fourthPayload)
                .put(fourth)
                .flip();

        decoder.decode(session, (IoBuffer) in, session.getDecoderOutput());

        WsMessage out1 = (WsMessage)session.getDecoderOutputQueue().poll();
        assertEquals(new WsTextMessage(allocator.wrap(ByteBuffer.wrap(first.getBytes(UTF_8))), false), out1);

        WsMessage out2 = (WsMessage)session.getDecoderOutputQueue().poll();
        assertEquals(new WsContinuationMessage(allocator.wrap(ByteBuffer.wrap(second.getBytes(UTF_8))), false), out2);

        WsMessage out = (WsMessage)session.getDecoderOutputQueue().poll();
        assertEquals(new WsPingMessage(allocator.wrap(allocator.allocate(0))), out);

        WsMessage out3 = (WsMessage)session.getDecoderOutputQueue().poll();
        assertEquals(new WsContinuationMessage(allocator.wrap(ByteBuffer.wrap(third.getBytes(UTF_8)))), out3);

        WsMessage out4 = (WsMessage)session.getDecoderOutputQueue().poll();
        assertEquals(new WsBinaryMessage(allocator.wrap(ByteBuffer.wrap(fourth))), out4);

        assertTrue(session.getDecoderOutputQueue().isEmpty());
        decoder.finishDecode(session, session.getDecoderOutput());

        assertTrue(session.getDecoderOutputQueue().isEmpty());
        assertFalse(in.hasRemaining());
    }

    private IoBufferEx getMaxMessageSizeBuffer(IoBufferAllocatorEx<?> allocator) throws Exception {
        int firstPayload = 125;
        String first = createString('a', firstPayload);

        int secondPayload = 125;
        String second = createString('b', secondPayload);

        int thirdPayload = 4;
        String third = createString('c', thirdPayload);

        int fourthPayload = 6;
        byte[] fourth = createString('d', fourthPayload).getBytes("UTF-8");

        return allocator.wrap(allocator.allocate(firstPayload+secondPayload+2+thirdPayload+fourthPayload+8))
                // text frame
                .put((byte) 0x01).put((byte) firstPayload).putString(first, UTF_8.newEncoder())
                // continuation frame
                .put((byte) 0x00).put((byte) secondPayload).putString(second, UTF_8.newEncoder())
                // ping frame
                .put((byte) 0x89).put((byte) 0x00)
                // continuation frame with FIN
                .put((byte) 0x80).put((byte) thirdPayload).putString(third, UTF_8.newEncoder())
                // binary frame
                .put((byte) 0x82).put((byte) fourthPayload).put(fourth)
                .flip();

    }

    @Test
    public void testMaxMessageSize() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();

        try {
            IoBuffer in = (IoBuffer) getMaxMessageSizeBuffer(allocator);
            ProtocolDecoder decoder = new WsFrameDecoder(allocator, 253, false);
            decoder.decode(session, in, session.getDecoderOutput());
            fail("Expected throw exception as the message size > 253");
        } catch (ProtocolDecoderException e) {
            // expected exception as message size exceeds 255
        }

        IoBuffer in = (IoBuffer) getMaxMessageSizeBuffer(allocator);
        ProtocolDecoder decoder = new WsFrameDecoder(allocator, 254, false);
        decoder.decode(session, in, session.getDecoderOutput());

        in = (IoBuffer) getMaxMessageSizeBuffer(allocator);
        decoder = new WsFrameDecoder(allocator, 255, false);
        decoder.decode(session, in, session.getDecoderOutput());
    }

    @Test
    public void pingInBinaryContinuationSequence() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolDecoder decoder = new WsFrameDecoder(allocator, 400, false);

        int firstPayload = 125;
        byte[] first = createString('a', firstPayload).getBytes("UTF-8");

        int secondPayload = 125;
        byte[] second = createString('b', secondPayload).getBytes("UTF-8");

        int thirdPayload = 4;
        byte[] third = createString('c', thirdPayload).getBytes("UTF-8");

        int fourthPayload = 6;
        String fourth = createString('d', fourthPayload);

        IoBufferEx in = allocator.wrap(allocator.allocate(firstPayload+secondPayload+2+thirdPayload+fourthPayload+8))
                // text frame
                .put((byte) 0x01)
                .put((byte) firstPayload)
                .put(first)
                // continuation frame
                .put((byte) 0x00)
                .put((byte) secondPayload)
                .put(second)
                // ping frame
                .put((byte) 0x89)
                .put((byte) 0x00)
                // continuation frame with FIN
                .put((byte) 0x80)
                .put((byte) thirdPayload)
                .put(third)
                // binary frame
                .put((byte) 0x82)
                .put((byte) fourthPayload)
                .putString(fourth, UTF_8.newEncoder())
                .flip();

        decoder.decode(session, (IoBuffer) in, session.getDecoderOutput());

        WsMessage out1 = (WsMessage)session.getDecoderOutputQueue().poll();
        assertEquals(new WsTextMessage(allocator.wrap(ByteBuffer.wrap(first)), false), out1);

        WsMessage out2 = (WsMessage)session.getDecoderOutputQueue().poll();
        assertEquals(new WsContinuationMessage(allocator.wrap(ByteBuffer.wrap(second)), false), out2);

        WsMessage out = (WsMessage)session.getDecoderOutputQueue().poll();
        assertEquals(new WsPingMessage(allocator.wrap(allocator.allocate(0))), out);

        WsMessage out3 = (WsMessage)session.getDecoderOutputQueue().poll();
        assertEquals(new WsContinuationMessage(allocator.wrap(ByteBuffer.wrap(third))), out3);

        WsMessage out4 = (WsMessage)session.getDecoderOutputQueue().poll();
        assertEquals(new WsBinaryMessage(allocator.wrap(ByteBuffer.wrap(fourth.getBytes(UTF_8)))), out4);

        assertTrue(session.getDecoderOutputQueue().isEmpty());
        decoder.finishDecode(session, session.getDecoderOutput());

        assertTrue(session.getDecoderOutputQueue().isEmpty());
        assertFalse(in.hasRemaining());
    }


    @Test
    public void decodeZeroLengthMaskedPongFrame() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolDecoder decoder = new WsFrameDecoder(allocator, 0, true);

        IoBufferEx in = allocator.wrap(allocator.allocate(6))
                                 .put((byte)0x8A)
                                 .put((byte)0x80)
                                 .fill(4) // mask
                                 .flip();

        decoder.decode(session, (IoBuffer) in, session.getDecoderOutput());

        WsMessage out = (WsMessage)session.getDecoderOutputQueue().poll();
        assertEquals(new WsPongMessage(allocator.wrap(allocator.allocate(0)).flip()), out);

        assertTrue(session.getDecoderOutputQueue().isEmpty());
        decoder.finishDecode(session, session.getDecoderOutput());

        assertTrue(session.getDecoderOutputQueue().isEmpty());
        assertFalse(in.hasRemaining());
    }

    @Test
    public void decodeZeroLengthBinaryFrame() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolDecoder decoder = new WsFrameDecoder(allocator, 0, false);

        IoBufferEx in = allocator.wrap(allocator.allocate(2))
                                 .put((byte)0x82)
                                 .put((byte) 0x00)
                                 .flip();

        decoder.decode(session, (IoBuffer) in, session.getDecoderOutput());

        WsMessage out = (WsMessage)session.getDecoderOutputQueue().poll();
        assertEquals(new WsBinaryMessage(allocator.wrap(allocator.allocate(0))), out);

        assertTrue(session.getDecoderOutputQueue().isEmpty());
        decoder.finishDecode(session, session.getDecoderOutput());

        assertTrue(session.getDecoderOutputQueue().isEmpty());
        assertFalse(in.hasRemaining());
    }

    @Test
    public void decodeBinaryFrame() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolDecoder decoder = new WsFrameDecoder(allocator, 0, false);

        IoBufferEx in = allocator.wrap(allocator.allocate(204))
                                 .put((byte) 0x82)
                                 .put((byte) 126)
                                 .put((byte) 0x00)
                                 .put((byte) 0xC8)
                                 .fill(200)
                                 .flip();

        decoder.decode(session, (IoBuffer) in, session.getDecoderOutput());

        WsMessage out = (WsMessage)session.getDecoderOutputQueue().poll();
        assertEquals(new WsBinaryMessage(allocator.wrap(allocator.allocate(200)).fill(200).flip()), out);

        assertTrue(session.getDecoderOutputQueue().isEmpty());
        decoder.finishDecode(session, session.getDecoderOutput());

        assertTrue(session.getDecoderOutputQueue().isEmpty());
        assertFalse(in.hasRemaining());
    }

    @Test
    public void decodeFragmentedBinaryFrame() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolDecoder decoder = new WsFrameDecoder(allocator, 0, false);

        IoBufferEx[] array = new IoBufferEx[] { allocator.wrap(allocator.allocate(104))
                                                         .put((byte) 0x82)
                                                         .put((byte) 126)
                                                         .put((byte) 0x00)
                                                         .put((byte) 0xC8)
                                                         .fill(100)
                                                         .flip(),
                                                allocator.wrap(allocator.allocate(102))
                                                         .fill(100)
                                                         .put((byte) 0x82)
                                                         .put((byte) 0x00)
                                                         .flip(),
        };

        
        for (IoBufferEx in : array) {
            decoder.decode(session, (IoBuffer) in, session.getDecoderOutput());
        }

        WsMessage fragmented = (WsMessage) session.getDecoderOutputQueue().poll();
        assertEquals(new WsBinaryMessage(allocator.wrap(allocator.allocate(200)).fill(200).flip()), fragmented);
        
        WsMessage empty = (WsMessage)session.getDecoderOutputQueue().poll();
        assertEquals(new WsBinaryMessage(allocator.wrap(allocator.allocate(0))), empty);
        
        assertTrue(session.getDecoderOutputQueue().isEmpty());
        decoder.finishDecode(session, session.getDecoderOutput());

        assertTrue(session.getDecoderOutputQueue().isEmpty());

        for (IoBufferEx in : array) {
            assertFalse(in.hasRemaining());
        }
    }

    @Test
    public void decodeZeroLengthTextFrame() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolDecoder decoder = new WsFrameDecoder(allocator, 0, false);

        IoBufferEx in = allocator.wrap(allocator.allocate(2))
                                 .put((byte) 0x81)
                                 .put((byte)0x00)
                                 .flip();

        decoder.decode(session, (IoBuffer) in, session.getDecoderOutput());

        WsMessage out = (WsMessage) session.getDecoderOutputQueue().poll();
        assertEquals(new WsTextMessage(allocator.wrap(allocator.allocate(0)).flip()), out);

        assertTrue(session.getDecoderOutputQueue().isEmpty());
        decoder.finishDecode(session, session.getDecoderOutput());

        assertTrue(session.getDecoderOutputQueue().isEmpty());
        assertFalse(in.hasRemaining());
    }

    @Test
    public void decodeTextFrame() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolDecoder decoder = new WsFrameDecoder(allocator, 0, false);

        IoBufferEx in = allocator.wrap(allocator.allocate(14))
                                 .put((byte) 0x81)
                                 .put((byte) 0x0C)
                                 .putString("Hello, world", UTF_8.newEncoder())
                                 .flip();

        decoder.decode(session, (IoBuffer) in, session.getDecoderOutput());

        WsMessage out = (WsMessage)session.getDecoderOutputQueue().poll();
        assertEquals(new WsTextMessage(allocator.wrap(ByteBuffer.wrap("Hello, world".getBytes(UTF_8)))), out);

        assertTrue(session.getDecoderOutputQueue().isEmpty());
        decoder.finishDecode(session, session.getDecoderOutput());

        assertTrue(session.getDecoderOutputQueue().isEmpty());
        assertFalse(in.hasRemaining());
    }

    @Test
    public void decodeFragmentedTextFrame() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolDecoder decoder = new WsFrameDecoder(allocator, 0, false);

        IoBufferEx[] array = new IoBufferEx[] { allocator.wrap(allocator.allocate(103))
                                                         .put((byte) 0x81)
                                                         .put((byte) 0x0C)
                                                         .putString("Hello", UTF_8.newEncoder())
                                                         .flip(),
                                                allocator.wrap(allocator.allocate(102))
                                                         .putString(", world", UTF_8.newEncoder())
                                                         .put((byte) 0x81)
                                                         .put((byte) 0x00)
                                                         .flip(),
        };

        for (IoBufferEx in : array) {
            decoder.decode(session, (IoBuffer) in, session.getDecoderOutput());
        }

        WsMessage fragmented = (WsMessage)session.getDecoderOutputQueue().poll();
        assertEquals(new WsTextMessage(allocator.wrap(ByteBuffer.wrap("Hello, world".getBytes(UTF_8)))), fragmented);

        WsMessage empty = (WsMessage)session.getDecoderOutputQueue().poll();
        assertEquals(new WsTextMessage(allocator.wrap(allocator.allocate(0))), empty);
        
        assertTrue(session.getDecoderOutputQueue().isEmpty());
        decoder.finishDecode(session, session.getDecoderOutput());

        assertTrue(session.getDecoderOutputQueue().isEmpty());

        for (IoBufferEx in : array) {
            assertFalse(in.hasRemaining());
        }
    }

    @Test
    public void sizeLimitEqualledDecodeBinaryFrame() throws Exception {
        sizeLimitDecodeBinaryFrame(200);
    }

    @Test(expected=ProtocolDecoderException.class)
    public void sizeLimitExceededDecodeBinaryFrame() throws Exception {
        sizeLimitDecodeBinaryFrame(199);
    }
    
    @Test // negative should be interpreted as unset (no limit)
    public void sizeLimitNegativeDecodeBinaryFrame() throws Exception {
        sizeLimitDecodeBinaryFrame(-1);
    }
    
    
    // Decode a 200-byte binary frame
    private void sizeLimitDecodeBinaryFrame(int maxSize) throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolDecoder decoder = new WsFrameDecoder(allocator, maxSize, false);

        IoBufferEx in = allocator.wrap(allocator.allocate(204))
                                 .put((byte)0x82)
                                 .put((byte)126)
                                 .put((byte)0x00)
                                 .put((byte)0xC8)
                                 .fill(200)
                                 .flip();

        decoder.decode(session, (IoBuffer) in, session.getDecoderOutput());

        WsMessage out = (WsMessage) session.getDecoderOutputQueue().poll();
        assertEquals(new WsBinaryMessage(allocator.wrap(allocator.allocate(200)).fill(200).flip()), out);

        assertTrue(session.getDecoderOutputQueue().isEmpty());
        decoder.finishDecode(session, session.getDecoderOutput());

        assertTrue(session.getDecoderOutputQueue().isEmpty());
        assertFalse(in.hasRemaining());
    }

    @Test
    public void sizeLimitEqualledDecodeFragmentedBinaryFrame() throws Exception {
        sizeLimitDecodeFragmentedBinaryFrame(200);
    }
    
    @Test(expected=ProtocolDecoderException.class)
    public void sizeLimitExceededDecodeFragmentedBinaryFrame() throws Exception {
        sizeLimitDecodeFragmentedBinaryFrame(199);
    }
 
    private void sizeLimitDecodeFragmentedBinaryFrame(int maxSize) throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolDecoder decoder = new WsFrameDecoder(allocator, maxSize, false);

        IoBufferEx[] array = new IoBufferEx[] { allocator.wrap(allocator.allocate(104))
                                                         .put((byte)0x82)
                                                         .put((byte)126)
                                                         .put((byte)0x00)
                                                         .put((byte)0xC8)
                                                         .fill(100)
                                                         .flip(),
                                                allocator.wrap(allocator.allocate(102))
                                                         .fill(100)
                                                         .put((byte)0x82)
                                                         .put((byte)0x00)
                                                         .flip(),
        };

        
        for (IoBufferEx in : array) {
            decoder.decode(session, (IoBuffer) in, session.getDecoderOutput());
        }
        
        WsMessage fragmented = (WsMessage) session.getDecoderOutputQueue().poll();
        assertEquals(new WsBinaryMessage(allocator.wrap(allocator.allocate(200)).fill(200).flip()), fragmented);
        
        WsMessage empty = (WsMessage)session.getDecoderOutputQueue().poll();
        assertEquals(new WsBinaryMessage(allocator.wrap(allocator.allocate(0))), empty);
        
        assertTrue(session.getDecoderOutputQueue().isEmpty());
        decoder.finishDecode(session, session.getDecoderOutput());

        assertTrue(session.getDecoderOutputQueue().isEmpty());

        for (IoBufferEx in : array) {
            assertFalse(in.hasRemaining());
        }
    }

    @Test(expected=ProtocolDecoderException.class)
    public void sizeLimitExceededDecodeTextFrame() throws Exception {
        sizeLimitDecodeTextFrame(11);
    }
    
    @Test
    public void sizeLimitEqualledDecodeTextFrame() throws Exception {
        sizeLimitDecodeTextFrame(12);
    }
    
    // Decode a 12-byte text frame
    private void sizeLimitDecodeTextFrame(int maxSize) throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolDecoder decoder = new WsFrameDecoder(allocator, maxSize, false);

        IoBufferEx in = allocator.wrap(allocator.allocate(14))
                                 .put((byte)0x81)
                                 .put((byte)12)
                                 .putString("Hello, world", UTF_8.newEncoder())
                                 .flip();
        
        decoder.decode(session, (IoBuffer) in, session.getDecoderOutput());
        
        WsMessage out = (WsMessage) session.getDecoderOutputQueue().poll();
        assertEquals(new WsTextMessage(allocator.wrap(ByteBuffer.wrap("Hello, world".getBytes(UTF_8)))), out);
        
        assertTrue(session.getDecoderOutputQueue().isEmpty());
        decoder.finishDecode(session, session.getDecoderOutput());

        assertTrue(session.getDecoderOutputQueue().isEmpty());
        assertFalse(in.hasRemaining());
    }

    @Test
    public void sizeLimitEqualledDecodeFragmentedTextFrame() throws Exception {
        sizeLimitDecodeFragmentedTextFrame(12);
    }
    
    @Test(expected=ProtocolDecoderException.class)
    public void sizeLimitExceededDecodeFragmentedTextFrame() throws Exception {
        sizeLimitDecodeFragmentedTextFrame(11);
    }

    // Decode a 12-byte text frame sent in 2 chunks
    private void sizeLimitDecodeFragmentedTextFrame(int maxSize) throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolDecoder decoder = new WsFrameDecoder(allocator, maxSize, false);

        IoBufferEx[] array = new IoBufferEx[] { allocator.wrap(allocator.allocate(103))
                                                         .put((byte)0x81)
                                                         .put((byte)0x0C)
                                                         .putString("Hello", UTF_8.newEncoder())
                                                         .flip(),
                                                allocator.wrap(allocator.allocate(102))
                                                         .putString(", world", UTF_8.newEncoder())
                                                         .put((byte)0x81)
                                                         .put((byte)0x00)
                                                         .flip(),
        };

        for (IoBufferEx in : array) {
            decoder.decode(session, (IoBuffer) in, session.getDecoderOutput());
        }

        WsMessage fragmented = (WsMessage) session.getDecoderOutputQueue().poll();
        assertEquals(new WsTextMessage(allocator.wrap(ByteBuffer.wrap("Hello, world".getBytes(UTF_8)))), fragmented);
        
        WsMessage empty = (WsMessage)session.getDecoderOutputQueue().poll();
        assertEquals(new WsTextMessage(allocator.wrap(allocator.allocate(0)).flip()), empty);
        
        assertTrue(session.getDecoderOutputQueue().isEmpty());
        decoder.finishDecode(session, session.getDecoderOutput());

        assertTrue(session.getDecoderOutputQueue().isEmpty());

        for (IoBufferEx in : array) {
            assertFalse(in.hasRemaining());
        }
    }
    
    @Test
    public void sizeLimitEqualledDecodeDoubleTextFrame() throws Exception {
        sizeLimitDecodeDoubleTextFrame(15);
    }
    
    @Test(expected=ProtocolDecoderException.class)
    public void sizeLimitExceededDecodeDoubleTextFrame() throws Exception {
        sizeLimitDecodeDoubleTextFrame(14);
    }
    
    // Decode a 12-byte text frame and a 15 byte text frame written in one go (one network packet)
    private void sizeLimitDecodeDoubleTextFrame(int maxSize) throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolDecoder decoder = new WsFrameDecoder(allocator, maxSize, false);

        IoBufferEx in = allocator.wrap(allocator.allocate(12+2+15+2))
                                 .put((byte)0x81)
                                 .put((byte)12)
                                 .putString("123456789012", UTF_8.newEncoder())
                                 .put((byte)0x81)
                                 .put((byte)15)
                                 .putString("123456789012345", UTF_8.newEncoder())
                                 .flip();

        decoder.decode(session, (IoBuffer) in, session.getDecoderOutput());

        WsMessage out = (WsMessage)session.getDecoderOutputQueue().poll();
        assertEquals(new WsTextMessage(allocator.wrap(ByteBuffer.wrap("123456789012".getBytes(UTF_8)))), out);

        WsMessage out2 = (WsMessage)session.getDecoderOutputQueue().poll();
        assertEquals(new WsTextMessage(allocator.wrap(ByteBuffer.wrap("123456789012345".getBytes(UTF_8)))), out2);

        assertTrue(session.getDecoderOutputQueue().isEmpty());
        decoder.finishDecode(session, session.getDecoderOutput());

        assertTrue(session.getDecoderOutputQueue().isEmpty());
        assertFalse(in.hasRemaining());
    }
    
    // Make sure we fail early for case of large text messages: we should fail as soon as we process
    // a network packet that exceeds the limit (first packet in this case)
    @Test(expected=ProtocolDecoderException.class)
    public void sizeLimitDecodeTextFrameFailEarly1() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolDecoder decoder = new WsFrameDecoder(allocator, 20, false);
        
        int dataSize = 30;
        StringBuilder data = new StringBuilder(dataSize);
        for ( int i=0; i<(dataSize); i++ ) {
            data.append((i%10));
        }

        IoBufferEx in = allocator.wrap(allocator.allocate(dataSize+2))
                                 .put((byte)0x81)
                                 .put((byte)30)
                                 .putString(data.toString(), UTF_8.newEncoder())
                                 .flip();
        // As soon as we sent part of a message that exceeds the limit it should throw the exception
        decoder.decode(session, (IoBuffer) in, session.getDecoderOutput());
    }
    
    // Make sure we fail early for case of large text messages: we should fail as soon as we process
    // a network packet that exceeds the limit (2nd packet in this case)
    @Test(expected=ProtocolDecoderException.class)
    public void sizeLimitDecodeTextFrameFailEarly2() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolDecoder decoder = new WsFrameDecoder(allocator, 20, false);
        
        int dataSize = 30;
        StringBuilder data = new StringBuilder(dataSize);
        for ( int i=0; i<(dataSize); i++ ) {
            data.append((i%10));
        }
        IoBufferEx in = allocator.wrap(allocator.allocate(dataSize+2))
                                 .put((byte)0x81)
                                 .put((byte)30)
                                 .putString(data.toString(), UTF_8.newEncoder())
                                 .flip();
        decoder.decode(session, (IoBuffer) in.getSlice(10), session.getDecoderOutput());
        // Now if we send the next 12 bytes that should exceed the limit (first byte is control byte, doesn't count)
        decoder.decode(session, (IoBuffer) in.getSlice(12), session.getDecoderOutput());
    }

    @Test
    public void testDecodingMaskedPingFrameWhenMaskingNotExpectedThrowsProtocolDecoderException() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolDecoder decoder = new WsFrameDecoder(allocator, 200, false);

        IoBufferEx in = allocator.wrap(allocator.allocate(6))
                                 .put((byte) 0x89)
                                 .put((byte) 0x80)
                                 .fill(4)
                                 .flip();

        thrown.expect(ProtocolDecoderException.class);
        decoder.decode(session, (IoBuffer) in, session.getDecoderOutput());
    }

    @Test
    public void testDecodingPingUnmaskedFrameWhenMaskingExpectedThrowsProtocolDecoderException() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolDecoder decoder = new WsFrameDecoder(allocator, 200, true);

        IoBufferEx in = allocator.wrap(allocator.allocate(6))
                                 .put((byte) 0x89)
                                 .put((byte) 0x00)
                                 .fill(4)
                                 .flip();

        thrown.expect(ProtocolDecoderException.class);
        decoder.decode(session, (IoBuffer) in, session.getDecoderOutput());
    }
/*
 * Use the below utility to decode a ws frame.
    public static String decodeABinaryFrame(byte[] hexBytes) throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolDecoder decoder = new WsFrameDecoder(allocator, 0);

        IoBufferEx in = allocator.wrap(allocator.allocate(hexBytes.length))
                .put(hexBytes)
                .flip();

        decoder.decode(session, (IoBuffer) in, session.getDecoderOutput());

        WsMessage out = (WsMessage)session.getDecoderOutputQueue().poll();
        return new String(out.getBytes().array());
    }
    public static void main(String[] strings) throws Exception
    {
        byte b[] = {(byte)0x82, (byte)0xCB, (byte)0xF5, (byte)0xC7, (byte)0x2C, (byte)0xDE, (byte)0xB2, (byte)0x82, (byte)0x78, (byte)0xFE, (byte)0xDA, (byte)0xA2, (byte)0x4F, (byte)0xB6, (byte)0x9A, (byte)0xE7
                , (byte)0x64, (byte)0x8A, (byte)0xA1, (byte)0x97, (byte)0x03, (byte)0xEF, (byte)0xDB, (byte)0xF6, (byte)0x21, (byte)0xD4, (byte)0xA6, (byte)0xA2, (byte)0x4F, (byte)0xF3, (byte)0xA2, (byte)0xA2
                , (byte)0x4E, (byte)0x8D, (byte)0x9A, (byte)0xA4, (byte)0x47, (byte)0xBB, (byte)0x81, (byte)0xEA, (byte)0x69, (byte)0xA6, (byte)0x81, (byte)0xA2, (byte)0x42, (byte)0xAD, (byte)0x9C, (byte)0xA8
                , (byte)0x42, (byte)0xAD, (byte)0xCF, (byte)0xE7, (byte)0x54, (byte)0xF3, (byte)0x9E, (byte)0xA6, (byte)0x4D, (byte)0xA4, (byte)0x9C, (byte)0xA9, (byte)0x4B, (byte)0xF3, (byte)0x9D, (byte)0xB3
                , (byte)0x58, (byte)0xAE, (byte)0xD8, (byte)0xB5, (byte)0x49, (byte)0xA8, (byte)0x94, (byte)0xAB, (byte)0x45, (byte)0xBA, (byte)0x94, (byte)0xB3, (byte)0x49, (byte)0xD3, (byte)0xFF, (byte)0xCA
                , (byte)0x26};
        String s = decodeABinaryFrame(b);
        
        System.out.print(s);
    }
*/
    private static String createString(char ch, int size) {
        char[] arr = new char[size];
        for(int i=0; i < size; i++) {
            arr[i] = ch;
        }
        return new String(arr);
    }

}
