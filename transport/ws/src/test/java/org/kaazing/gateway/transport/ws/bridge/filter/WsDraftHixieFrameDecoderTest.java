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

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderException;
import org.junit.Test;
import org.kaazing.gateway.transport.ws.WsBinaryMessage;
import org.kaazing.gateway.transport.ws.WsMessage;
import org.kaazing.gateway.transport.ws.WsTextMessage;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.filter.codec.ProtocolCodecSessionEx;


public class WsDraftHixieFrameDecoderTest {

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    @Test
    public void decodeBinaryFrame() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolDecoder decoder = new WsDraftHixieFrameDecoder(allocator, 0);

        IoBufferEx in = allocator.wrap(allocator.allocate(203))
                                 .put((byte) 0x80)
                                 .put((byte) -0x7f)
                                 .put((byte) 0x48)
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
        ProtocolDecoder decoder = new WsDraftHixieFrameDecoder(allocator, 0);

        IoBufferEx[] array = new IoBufferEx[] { allocator.wrap(allocator.allocate(103))
                                                         .put((byte) 0x80)
                                                         .put((byte) -0x7f)
                                                         .put((byte) 0x48)
                                                         .fill(100)
                                                         .flip(),
                                                allocator.wrap(allocator.allocate(102))
                                                         .fill(100)
                                                         .put((byte) 0x80)
                                                         .put((byte) 0x00)
                                                         .flip(),
        };

        for (IoBufferEx in : array) {
            decoder.decode(session, (IoBuffer) in, session.getDecoderOutput());
        }

        WsMessage fragmented = (WsMessage)session.getDecoderOutputQueue().poll();
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
    public void decodeTextFrame() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolDecoder decoder = new WsDraftHixieFrameDecoder(allocator, 0);

        IoBufferEx in = allocator.wrap(allocator.allocate(14))
                                 .put((byte) 0x00)
                                 .putString("Hello, world", UTF_8.newEncoder())
                                 .put((byte) 0xff)
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
        ProtocolDecoder decoder = new WsDraftHixieFrameDecoder(allocator, 0);

        IoBufferEx[] array = new IoBufferEx[] { allocator.wrap(allocator.allocate(103))
                                                         .put((byte) 0x00)
                                                         .putString("Hello", UTF_8.newEncoder())
                                                         .flip(),
                                                allocator.wrap(allocator.allocate(102))
                                                         .putString(", world", UTF_8.newEncoder())
                                                         .put((byte) 0xff)
                                                         .put((byte) 0x00)
                                                         .put((byte) 0xff)
                                                         .flip(),
        };

        for (IoBufferEx in : array) {
            decoder.decode(session, (IoBuffer) in, session.getDecoderOutput());
        }

        WsMessage fragmented = (WsMessage) session.getDecoderOutputQueue().poll();
        assertEquals(new WsTextMessage(allocator.wrap(ByteBuffer.wrap("Hello, world".getBytes(UTF_8)))), fragmented);

        WsMessage empty = (WsMessage) session.getDecoderOutputQueue().poll();
        assertEquals(new WsTextMessage(allocator.wrap(ByteBuffer.allocate(0))), empty);

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
        ProtocolDecoder decoder = new WsDraftHixieFrameDecoder(allocator, maxSize);

        IoBufferEx in = allocator.wrap(allocator.allocate(203))
                                 .put((byte)0x80)
                                 .put((byte)-0x7f)
                                 .put((byte)0x48)
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
        ProtocolDecoder decoder = new WsDraftHixieFrameDecoder(allocator, maxSize);

        IoBufferEx[] array = new IoBufferEx[] { allocator.wrap(allocator.allocate(103))
                                                         .put((byte)0x80)
                                                         .put((byte)-0x7f)
                                                         .put((byte)0x48)
                                                         .fill(100)
                                                         .flip(),
                                                allocator.wrap(allocator.allocate(102))
                                                         .fill(100)
                                                         .put((byte)0x80)
                                                         .put((byte)0x00)
                                                         .flip(),
        };

        
        for (IoBufferEx in : array) {
            decoder.decode(session, (IoBuffer) in, session.getDecoderOutput());
        }

        WsMessage fragmented = (WsMessage)session.getDecoderOutputQueue().poll();
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
        ProtocolDecoder decoder = new WsDraftHixieFrameDecoder(allocator, maxSize);

        IoBufferEx in = allocator.wrap(allocator.allocate(14))
                                 .put((byte)0x00)
                                 .putString("Hello, world", UTF_8.newEncoder())
                                 .put((byte)0xff)
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
        ProtocolDecoder decoder = new WsDraftHixieFrameDecoder(allocator, maxSize);

        IoBufferEx[] array = new IoBufferEx[] { allocator.wrap(allocator.allocate(103))
                                                         .put((byte)0x00)
                                                         .putString("Hello", UTF_8.newEncoder())
                                                         .flip(),
                                                allocator.wrap(allocator.allocate(102))
                                                         .putString(", world", UTF_8.newEncoder())
                                                         .put((byte)0xff)
                                                         .put((byte)0x00)
                                                         .put((byte)0xff)
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
        ProtocolDecoder decoder = new WsDraftHixieFrameDecoder(allocator, maxSize);

        IoBufferEx in = allocator.wrap(allocator.allocate(12+2+15+2))
                                 .put((byte)0x00)
                                 .putString("123456789012", UTF_8.newEncoder())
                                 .put((byte)0xff)
                                 .put((byte)0x00)
                                 .putString("123456789012345", UTF_8.newEncoder())
                                 .put((byte)0xff)
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
        ProtocolDecoder decoder = new WsDraftHixieFrameDecoder(allocator, 20);
        
        int dataSize = 30;
        StringBuffer data = new StringBuffer(dataSize);
        for ( int i=0; i<(dataSize); i++ ) {
            data.append((i%10));
        }
        IoBufferEx in = allocator.wrap(allocator.allocate(dataSize+1))
                                 .put((byte)0x00)
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
        ProtocolDecoder decoder = new WsDraftHixieFrameDecoder(allocator, 20);
        
        int dataSize = 30;
        StringBuffer data = new StringBuffer(dataSize);
        for ( int i=0; i<(dataSize); i++ ) {
            data.append((i%10));
        }
        IoBufferEx in = allocator.wrap(allocator.allocate(dataSize+1))
                                 .put((byte)0x00)
                                 .putString(data.toString(), UTF_8.newEncoder())
                                 .flip();
        decoder.decode(session, (IoBuffer) in.getSlice(10), session.getDecoderOutput());
        // Now if we send the next 12 bytes that should exceed the limit (first byte is control byte, doesn't count)
        decoder.decode(session, (IoBuffer) in.getSlice(12), session.getDecoderOutput());
    }

}
