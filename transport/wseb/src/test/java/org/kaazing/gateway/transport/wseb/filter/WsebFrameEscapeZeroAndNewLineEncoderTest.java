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

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.mina.filter.codec.ProtocolEncoder;
import org.junit.Test;
import org.kaazing.gateway.transport.ws.WsBinaryMessage;
import org.kaazing.gateway.transport.ws.WsMessage;
import org.kaazing.gateway.transport.ws.WsPingMessage;
import org.kaazing.gateway.transport.ws.WsPongMessage;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.filter.codec.ProtocolCodecSessionEx;


public class WsebFrameEscapeZeroAndNewLineEncoderTest {
    
    @Test
    public void testEncodeNoEscapeChars() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder encoder = new WsebFrameEscapeZeroAndNewLineEncoder(allocator);

        IoBufferEx buf = allocator.wrap(allocator.allocate(200)).fill((byte)0x01, 200).flip();
        WsMessage in = new WsBinaryMessage(buf);
        
        encoder.encode(session, in, session.getEncoderOutput());
        
        IoBufferEx out = (IoBufferEx) session.getEncoderOutputQueue().poll();
        byte[] expect = new byte[203];
        Arrays.fill(expect, (byte)1);
        expect[0] = (byte) 0x80;
        expect[1] = (byte) 0x81;
        expect[2] = 0x48;
        assertEquals(allocator.wrap(ByteBuffer.wrap(expect)), out);
  }
    
    @Test
    public void testEncodeWithEscapeChars() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder encoder = new WsebFrameEscapeZeroAndNewLineEncoder(allocator);

        IoBufferEx buf = allocator.wrap(allocator.allocate(7));
        buf.put((byte) 0x01);
        buf.put((byte) 0x00);
        buf.put((byte) 0x01);
        buf.put((byte) 0x7f);
        buf.put((byte) 0x01);
        buf.put((byte) 0x0a);
        buf.put((byte) 0x0d);
        buf.flip();
        WsMessage in = new WsBinaryMessage(buf);
        
        encoder.encode(session, in, session.getEncoderOutput());
        
        IoBufferEx out = (IoBufferEx) session.getEncoderOutputQueue().poll();
        byte[] expected = new byte[] {(byte) 0x80, 0x07, 0x01, 0x7f, 0x30, 0x01, 0x7f, 0x7f, 0x01, 0x7f, 0x6e, 0x7f, 0x72};
        assertEquals(allocator.wrap(ByteBuffer.wrap(expected)), out);
    }

    @Test
    public void testEncodeBinaryRange() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder encoder = new WsebFrameEscapeZeroAndNewLineEncoder(allocator);

        byte[] bytes = new byte[256];
        for (int i = 0; i < 256; i++) {
            bytes[i] = (byte) i;
        }
        IoBufferEx buf = allocator.wrap(ByteBuffer.wrap(bytes));
        
        WsMessage in = new WsBinaryMessage(buf);
        
        encoder.encode(session, in, session.getEncoderOutput());
        String s = "";
        IoBufferEx out = (IoBufferEx) session.getEncoderOutputQueue().poll();
        for (int i = 0 ; i < out.buf().array().length; i++) {
            s += String.format("(byte)0x%02x, ", out.buf().array()[i]);
        }
        byte[] expected = new byte[] {(byte)0x80, (byte)0x82, (byte)0x7f, (byte)0x30, 
                (byte)0x7f, (byte)0x30, (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04, (byte)0x05, (byte)0x06, (byte)0x07, (byte)0x08, (byte)0x09, (byte)0x7f, (byte)0x6e, (byte)0x0b, (byte)0x0c, (byte)0x7f, (byte)0x72, (byte)0x0e, (byte)0x0f,
                (byte)0x10, (byte)0x11, (byte)0x12, (byte)0x13, (byte)0x14, (byte)0x15, (byte)0x16, (byte)0x17, (byte)0x18, (byte)0x19, (byte)0x1a, (byte)0x1b, (byte)0x1c, (byte)0x1d, (byte)0x1e, (byte)0x1f,
                (byte)0x20, (byte)0x21, (byte)0x22, (byte)0x23, (byte)0x24, (byte)0x25, (byte)0x26, (byte)0x27, (byte)0x28, (byte)0x29, (byte)0x2a, (byte)0x2b, (byte)0x2c, (byte)0x2d, (byte)0x2e, (byte)0x2f,
                (byte)0x30, (byte)0x31, (byte)0x32, (byte)0x33, (byte)0x34, (byte)0x35, (byte)0x36, (byte)0x37, (byte)0x38, (byte)0x39, (byte)0x3a, (byte)0x3b, (byte)0x3c, (byte)0x3d, (byte)0x3e, (byte)0x3f,
                (byte)0x40, (byte)0x41, (byte)0x42, (byte)0x43, (byte)0x44, (byte)0x45, (byte)0x46, (byte)0x47, (byte)0x48, (byte)0x49, (byte)0x4a, (byte)0x4b, (byte)0x4c, (byte)0x4d, (byte)0x4e, (byte)0x4f,
                (byte)0x50, (byte)0x51, (byte)0x52, (byte)0x53, (byte)0x54, (byte)0x55, (byte)0x56, (byte)0x57, (byte)0x58, (byte)0x59, (byte)0x5a, (byte)0x5b, (byte)0x5c, (byte)0x5d, (byte)0x5e, (byte)0x5f,
                (byte)0x60, (byte)0x61, (byte)0x62, (byte)0x63, (byte)0x64, (byte)0x65, (byte)0x66, (byte)0x67, (byte)0x68, (byte)0x69, (byte)0x6a, (byte)0x6b, (byte)0x6c, (byte)0x6d, (byte)0x6e, (byte)0x6f,
                (byte)0x70, (byte)0x71, (byte)0x72, (byte)0x73, (byte)0x74, (byte)0x75, (byte)0x76, (byte)0x77, (byte)0x78, (byte)0x79, (byte)0x7a, (byte)0x7b, (byte)0x7c, (byte)0x7d, (byte)0x7e, (byte)0x7f,    (byte)0x7f,
                (byte)0x80, (byte)0x81, (byte)0x82, (byte)0x83, (byte)0x84, (byte)0x85, (byte)0x86, (byte)0x87, (byte)0x88, (byte)0x89, (byte)0x8a, (byte)0x8b, (byte)0x8c, (byte)0x8d, (byte)0x8e, (byte)0x8f,
                (byte)0x90, (byte)0x91, (byte)0x92, (byte)0x93, (byte)0x94, (byte)0x95, (byte)0x96, (byte)0x97, (byte)0x98, (byte)0x99, (byte)0x9a, (byte)0x9b, (byte)0x9c, (byte)0x9d, (byte)0x9e, (byte)0x9f,
                (byte)0xa0, (byte)0xa1, (byte)0xa2, (byte)0xa3, (byte)0xa4, (byte)0xa5, (byte)0xa6, (byte)0xa7, (byte)0xa8, (byte)0xa9, (byte)0xaa, (byte)0xab, (byte)0xac, (byte)0xad, (byte)0xae, (byte)0xaf,
                (byte)0xb0, (byte)0xb1, (byte)0xb2, (byte)0xb3, (byte)0xb4, (byte)0xb5, (byte)0xb6, (byte)0xb7, (byte)0xb8, (byte)0xb9, (byte)0xba, (byte)0xbb, (byte)0xbc, (byte)0xbd, (byte)0xbe, (byte)0xbf,
                (byte)0xc0, (byte)0xc1, (byte)0xc2, (byte)0xc3, (byte)0xc4, (byte)0xc5, (byte)0xc6, (byte)0xc7, (byte)0xc8, (byte)0xc9, (byte)0xca, (byte)0xcb, (byte)0xcc, (byte)0xcd, (byte)0xce, (byte)0xcf,
                (byte)0xd0, (byte)0xd1, (byte)0xd2, (byte)0xd3, (byte)0xd4, (byte)0xd5, (byte)0xd6, (byte)0xd7, (byte)0xd8, (byte)0xd9, (byte)0xda, (byte)0xdb, (byte)0xdc, (byte)0xdd, (byte)0xde, (byte)0xdf,
                (byte)0xe0, (byte)0xe1, (byte)0xe2, (byte)0xe3, (byte)0xe4, (byte)0xe5, (byte)0xe6, (byte)0xe7, (byte)0xe8, (byte)0xe9, (byte)0xea, (byte)0xeb, (byte)0xec, (byte)0xed, (byte)0xee, (byte)0xef,
                (byte)0xf0, (byte)0xf1, (byte)0xf2, (byte)0xf3, (byte)0xf4, (byte)0xf5, (byte)0xf6, (byte)0xf7, (byte)0xf8, (byte)0xf9, (byte)0xfa, (byte)0xfb, (byte)0xfc, (byte)0xfd, (byte)0xfe, (byte)0xff};
        assertEquals(allocator.wrap(ByteBuffer.wrap(expected)), out);
    }
    
    @Test
    public void testEncodeWithEscapeCharsLargeSize() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder encoder = new WsebFrameEscapeZeroAndNewLineEncoder(allocator);
 
        byte[] bytes = new byte[4000];
        Arrays.fill(bytes, (byte)0x01);
        bytes[2990] = (byte) 0x00;
        bytes[2991] = (byte) 0x7f;
        bytes[2992] = (byte) 0x0a;
        bytes[2993] = (byte) 0x0d;
        IoBufferEx buf = allocator.wrap(ByteBuffer.wrap(bytes));
        
        
        WsMessage in = new WsBinaryMessage(buf);
        
        encoder.encode(session, in, session.getEncoderOutput());
        
        IoBufferEx out = (IoBufferEx) session.getEncoderOutputQueue().poll();
        byte[] expected = new byte[4007];
        Arrays.fill(expected, (byte)0x01);
        //ws frame header
        expected[0] = (byte) 0x80;
        expected[1] = (byte) 0x9f;
        expected[2] = (byte) 0x20;
        expected[2993] = (byte) 0x7f;
        expected[2994] = (byte) 0x30;
        expected[2995] = (byte) 0x7f;
        expected[2996] = (byte) 0x7f;
        expected[2997] = (byte) 0x7f;
        expected[2998] = (byte) 0x6e;
        expected[2999] = (byte) 0x7f;
        expected[3000] = (byte) 0x72;
        assertEquals(allocator.wrap(ByteBuffer.wrap(expected)), out);
    }
    
    @Test
    public void testEncodeNoEscapeCharsDirect() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder encoder = new WsebFrameEscapeZeroAndNewLineEncoder(allocator);

        byte[] bytes = new byte[200];
        Arrays.fill(bytes, (byte)0x01);
        
        ByteBuffer direct = ByteBuffer.allocateDirect(200);
        direct.put(bytes);
        direct.flip();
        IoBufferEx buf = allocator.wrap(direct);
        WsMessage in = new WsBinaryMessage(buf);
        
        encoder.encode(session, in, session.getEncoderOutput());
        
        IoBufferEx out = (IoBufferEx) session.getEncoderOutputQueue().poll();
        byte[] expect = new byte[203];
        Arrays.fill(expect, (byte)1);
        expect[0] = (byte) 0x80;
        expect[1] = (byte) 0x81;
        expect[2] = 0x48;
        assertEquals(allocator.wrap(ByteBuffer.wrap(expect)), out);
    }
    
    @Test
    public void testEncodeWithEscapeCharsDirect() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder encoder = new WsebFrameEscapeZeroAndNewLineEncoder(allocator);
 
        byte[] bytes = new byte[] { (byte) 0x01,(byte) 0x00, (byte) 0x01, (byte) 0x7f, (byte) 0x01, (byte) 0x0a, (byte) 0x0d};
        ByteBuffer direct = ByteBuffer.allocateDirect(200);
        direct.put(bytes);
        direct.flip();
        IoBufferEx buf = allocator.wrap(direct);
        
        
        WsMessage in = new WsBinaryMessage(buf);
        
        encoder.encode(session, in, session.getEncoderOutput());
        
        IoBufferEx out = (IoBufferEx) session.getEncoderOutputQueue().poll();
        byte[] expected = new byte[] {(byte) 0x80, 0x07, 0x01, 0x7f, 0x30, 0x01, 0x7f, 0x7f, 0x01, 0x7f, 0x6e, 0x7f, 0x72};
        assertEquals(allocator.wrap(ByteBuffer.wrap(expected)), out);
    }
    
    @Test
    public void testEncodeWithEscapeCharsLargeSizeDirect() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder encoder = new WsebFrameEscapeZeroAndNewLineEncoder(allocator);
 
        byte[] bytes = new byte[4000];
        Arrays.fill(bytes, (byte)0x01);
        bytes[2990] = (byte) 0x00;
        bytes[2991] = (byte) 0x7f;
        bytes[2992] = (byte) 0x0a;
        bytes[2993] = (byte) 0x0d;
        ByteBuffer direct = ByteBuffer.allocateDirect(4000);
        direct.put(bytes);
        direct.flip();
        IoBufferEx buf = allocator.wrap(direct);
        
        
        WsMessage in = new WsBinaryMessage(buf);
        
        encoder.encode(session, in, session.getEncoderOutput());
        
        IoBufferEx out = (IoBufferEx) session.getEncoderOutputQueue().poll();
        byte[] expected = new byte[4007];
        Arrays.fill(expected, (byte)0x01);
        //ws frame header
        expected[0] = (byte) 0x80;
        expected[1] = (byte) 0x9f;
        expected[2] = (byte) 0x20;
        expected[2993] = (byte) 0x7f;
        expected[2994] = (byte) 0x30;
        expected[2995] = (byte) 0x7f;
        expected[2996] = (byte) 0x7f;
        expected[2997] = (byte) 0x7f;
        expected[2998] = (byte) 0x6e;
        expected[2999] = (byte) 0x7f;
        expected[3000] = (byte) 0x72;
        assertEquals(allocator.wrap(ByteBuffer.wrap(expected)), out);
    }
    
    @Test
    public void testEncodeBinaryRangeDirect() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder encoder = new WsebFrameEscapeZeroAndNewLineEncoder(allocator);

        byte[] bytes = new byte[256];
        for (int i = 0; i < 256; i++) {
            bytes[i] = (byte) i;
        }
        ByteBuffer direct = ByteBuffer.allocateDirect(256);
        direct.put(bytes);
        direct.flip();
        IoBufferEx buf = allocator.wrap(direct);
        
        WsMessage in = new WsBinaryMessage(buf);
        
        encoder.encode(session, in, session.getEncoderOutput());
        
        IoBufferEx out = (IoBufferEx) session.getEncoderOutputQueue().poll();
        byte[] expected = new byte[] {(byte)0x80, (byte)0x82, (byte)0x7f, (byte)0x30, 
                (byte)0x7f, (byte)0x30, (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04, (byte)0x05, (byte)0x06, (byte)0x07, (byte)0x08, (byte)0x09, (byte)0x7f, (byte)0x6e, (byte)0x0b, (byte)0x0c, (byte)0x7f, (byte)0x72, (byte)0x0e, (byte)0x0f,
                (byte)0x10, (byte)0x11, (byte)0x12, (byte)0x13, (byte)0x14, (byte)0x15, (byte)0x16, (byte)0x17, (byte)0x18, (byte)0x19, (byte)0x1a, (byte)0x1b, (byte)0x1c, (byte)0x1d, (byte)0x1e, (byte)0x1f,
                (byte)0x20, (byte)0x21, (byte)0x22, (byte)0x23, (byte)0x24, (byte)0x25, (byte)0x26, (byte)0x27, (byte)0x28, (byte)0x29, (byte)0x2a, (byte)0x2b, (byte)0x2c, (byte)0x2d, (byte)0x2e, (byte)0x2f,
                (byte)0x30, (byte)0x31, (byte)0x32, (byte)0x33, (byte)0x34, (byte)0x35, (byte)0x36, (byte)0x37, (byte)0x38, (byte)0x39, (byte)0x3a, (byte)0x3b, (byte)0x3c, (byte)0x3d, (byte)0x3e, (byte)0x3f,
                (byte)0x40, (byte)0x41, (byte)0x42, (byte)0x43, (byte)0x44, (byte)0x45, (byte)0x46, (byte)0x47, (byte)0x48, (byte)0x49, (byte)0x4a, (byte)0x4b, (byte)0x4c, (byte)0x4d, (byte)0x4e, (byte)0x4f,
                (byte)0x50, (byte)0x51, (byte)0x52, (byte)0x53, (byte)0x54, (byte)0x55, (byte)0x56, (byte)0x57, (byte)0x58, (byte)0x59, (byte)0x5a, (byte)0x5b, (byte)0x5c, (byte)0x5d, (byte)0x5e, (byte)0x5f,
                (byte)0x60, (byte)0x61, (byte)0x62, (byte)0x63, (byte)0x64, (byte)0x65, (byte)0x66, (byte)0x67, (byte)0x68, (byte)0x69, (byte)0x6a, (byte)0x6b, (byte)0x6c, (byte)0x6d, (byte)0x6e, (byte)0x6f,
                (byte)0x70, (byte)0x71, (byte)0x72, (byte)0x73, (byte)0x74, (byte)0x75, (byte)0x76, (byte)0x77, (byte)0x78, (byte)0x79, (byte)0x7a, (byte)0x7b, (byte)0x7c, (byte)0x7d, (byte)0x7e, (byte)0x7f,    (byte)0x7f,
                (byte)0x80, (byte)0x81, (byte)0x82, (byte)0x83, (byte)0x84, (byte)0x85, (byte)0x86, (byte)0x87, (byte)0x88, (byte)0x89, (byte)0x8a, (byte)0x8b, (byte)0x8c, (byte)0x8d, (byte)0x8e, (byte)0x8f,
                (byte)0x90, (byte)0x91, (byte)0x92, (byte)0x93, (byte)0x94, (byte)0x95, (byte)0x96, (byte)0x97, (byte)0x98, (byte)0x99, (byte)0x9a, (byte)0x9b, (byte)0x9c, (byte)0x9d, (byte)0x9e, (byte)0x9f,
                (byte)0xa0, (byte)0xa1, (byte)0xa2, (byte)0xa3, (byte)0xa4, (byte)0xa5, (byte)0xa6, (byte)0xa7, (byte)0xa8, (byte)0xa9, (byte)0xaa, (byte)0xab, (byte)0xac, (byte)0xad, (byte)0xae, (byte)0xaf,
                (byte)0xb0, (byte)0xb1, (byte)0xb2, (byte)0xb3, (byte)0xb4, (byte)0xb5, (byte)0xb6, (byte)0xb7, (byte)0xb8, (byte)0xb9, (byte)0xba, (byte)0xbb, (byte)0xbc, (byte)0xbd, (byte)0xbe, (byte)0xbf,
                (byte)0xc0, (byte)0xc1, (byte)0xc2, (byte)0xc3, (byte)0xc4, (byte)0xc5, (byte)0xc6, (byte)0xc7, (byte)0xc8, (byte)0xc9, (byte)0xca, (byte)0xcb, (byte)0xcc, (byte)0xcd, (byte)0xce, (byte)0xcf,
                (byte)0xd0, (byte)0xd1, (byte)0xd2, (byte)0xd3, (byte)0xd4, (byte)0xd5, (byte)0xd6, (byte)0xd7, (byte)0xd8, (byte)0xd9, (byte)0xda, (byte)0xdb, (byte)0xdc, (byte)0xdd, (byte)0xde, (byte)0xdf,
                (byte)0xe0, (byte)0xe1, (byte)0xe2, (byte)0xe3, (byte)0xe4, (byte)0xe5, (byte)0xe6, (byte)0xe7, (byte)0xe8, (byte)0xe9, (byte)0xea, (byte)0xeb, (byte)0xec, (byte)0xed, (byte)0xee, (byte)0xef,
                (byte)0xf0, (byte)0xf1, (byte)0xf2, (byte)0xf3, (byte)0xf4, (byte)0xf5, (byte)0xf6, (byte)0xf7, (byte)0xf8, (byte)0xf9, (byte)0xfa, (byte)0xfb, (byte)0xfc, (byte)0xfd, (byte)0xfe, (byte)0xff};
        assertEquals(allocator.wrap(ByteBuffer.wrap(expected)), out);
    }
    
    @Test
    public void testEncodePingMessage() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder encoder = new WsebFrameEscapeZeroAndNewLineEncoder(allocator);

        WsPingMessage in = new WsPingMessage(allocator.wrap(allocator.allocate(0)));

        encoder.encode(session, in, session.getEncoderOutput());

        IoBufferEx out = (IoBufferEx) session.getEncoderOutputQueue().poll();
        assertEquals(allocator.wrap(allocator.allocate(3))
                              .put((byte) 0x89)
                              .put((byte) 0x7f)
                              .put((byte) 0x30)
                              .flip(),
                     out);
    }

    @Test
    public void testEncodePongMessage() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder encoder = new WsebFrameEscapeZeroAndNewLineEncoder(allocator);

        WsPongMessage in = new WsPongMessage(allocator.wrap(allocator.allocate(0)));

        encoder.encode(session, in, session.getEncoderOutput());

        IoBufferEx out = (IoBufferEx) session.getEncoderOutputQueue().poll();
        assertEquals(allocator.wrap(allocator.allocate(3))
                              .put((byte) 0x8a)
                              .put((byte) 0x7f)
                              .put((byte) 0x30)
                              .flip(),
                     out);
    }
}
