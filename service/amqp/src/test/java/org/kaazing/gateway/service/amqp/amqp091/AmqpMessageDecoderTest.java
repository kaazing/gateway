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
package org.kaazing.gateway.service.amqp.amqp091;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.kaazing.gateway.service.amqp.amqp091.message.AmqpConnectionMessage.AMQP_AUTHENTICATION_MECHANISM;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Queue;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.DefaultTransportMetadata;
import org.apache.mina.core.session.IoSessionConfig;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.junit.Test;
import org.kaazing.gateway.service.amqp.amqp091.AmqpTable.AmqpTableEntry;
import org.kaazing.gateway.service.amqp.amqp091.codec.AmqpMessageDecoder;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpClassMessage.ClassKind;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpCloseMessage;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpCloseOkMessage;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpConnectionMessage.ConnectionMethodKind;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpOpenMessage;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpOpenOkMessage;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpProtocolHeaderMessage;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpStartMessage;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpStartOkMessage;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpTuneMessage;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpTuneOkMessage;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.filter.codec.ProtocolCodecSessionEx;


public class AmqpMessageDecoderTest {
    
    @Test
    public void decodeProtocolHeader() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        byte[] bytes = AmqpProtocolHeaderMessage.PROTOCOL_0_9_1_DEFAULT_HEADER;
        
        ByteBuffer buf = allocator.allocate(bytes.length);
        buf.put(bytes);
        buf.flip();
        
        IoBuffer in = (IoBuffer) allocator.wrap(buf);

        session.setTransportMetadata(new DefaultTransportMetadata(
                "mina", "dummy", false, true,
                SocketAddress.class, IoSessionConfig.class, Object.class));

        ProtocolDecoderOutput output = session.getDecoderOutput();
        Queue<Object> outputQueue = session.getDecoderOutputQueue();

        AmqpMessageDecoder decoder = new AmqpMessageDecoder(allocator, false);
        
        decoder.decode(session, in, output);

        AmqpProtocolHeaderMessage actual = (AmqpProtocolHeaderMessage) outputQueue.poll();
        assertEquals(0, outputQueue.size());
        assertEquals(0, in.remaining());
        assertArrayEquals(bytes, actual.getProtocolHeader());

        byte[] closeBytes = {0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0b, 
                             0x00, 0x0a, 0x00, 0x32, 0x00, 0x00, 0x00, 
                             0x00, 0x00, 0x00, 0x00, (byte)0xce};

        ByteBuffer cbuf = ByteBuffer.allocate(closeBytes.length);
        cbuf.put(closeBytes);
        cbuf.flip();

        IoBuffer closeBuffer = (IoBuffer) allocator.wrap(cbuf);
        
        decoder.decode(session, closeBuffer, output);

        AmqpCloseMessage actualClose = (AmqpCloseMessage) outputQueue.poll();
        assertEquals(0, outputQueue.size());
        assertEquals(0, in.remaining());
        
        assertEquals(ClassKind.CONNECTION, actualClose.getClassKind());
        assertEquals(ConnectionMethodKind.CLOSE, actualClose.getMethodKind());
        assertEquals("", actualClose.getReplyText());
        assertEquals(0, actualClose.getReplyCode());
    }


    @Test
    public void decodeClose() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        
        session.setTransportMetadata(new DefaultTransportMetadata(
                "mina", "dummy", false, true,
                SocketAddress.class, IoSessionConfig.class, Object.class));

        ProtocolDecoderOutput output = session.getDecoderOutput();
        Queue<Object> outputQueue = session.getDecoderOutputQueue();

        AmqpMessageDecoder decoder = new AmqpMessageDecoder(allocator, true);
        byte[] bytes = {0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0b, 
                        0x00, 0x0a, 0x00, 0x32, 0x00, 0x00, 0x00, 
                        0x00, 0x00, 0x00, 0x00, (byte)0xce};


        ByteBuffer  buf = allocator.allocate(bytes.length);
        buf.put(bytes);
        buf.flip();

        IoBuffer in = (IoBuffer) allocator.wrap(buf);
        
        decoder.decode(session, in, output);

        AmqpCloseMessage actual = (AmqpCloseMessage) outputQueue.poll();
        assertEquals(0, outputQueue.size());
        assertEquals(0, in.remaining());
        
        assertEquals(ClassKind.CONNECTION, actual.getClassKind());
        assertEquals(ConnectionMethodKind.CLOSE, actual.getMethodKind());
        assertEquals("", actual.getReplyText());
        assertEquals(0, actual.getReplyCode());
    }
    
    @Test
    public void decodeCloseOk() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        
        session.setTransportMetadata(new DefaultTransportMetadata(
                "mina", "dummy", false, true,
                SocketAddress.class, IoSessionConfig.class, Object.class));

        ProtocolDecoderOutput output = session.getDecoderOutput();
        Queue<Object> outputQueue = session.getDecoderOutputQueue();

        AmqpMessageDecoder decoder = new AmqpMessageDecoder(allocator, true);
        byte[] bytes = {0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x04, 
                        0x00, 0x0a, 0x00, 0x33, (byte)0xce};

        ByteBuffer  buf = allocator.allocate(bytes.length);
        buf.put(bytes);
        buf.flip();

        IoBuffer in = (IoBuffer) allocator.wrap(buf);
        
        decoder.decode(session, in, output);

        AmqpCloseOkMessage actual = (AmqpCloseOkMessage) outputQueue.poll();
        assertEquals(0, outputQueue.size());
        assertEquals(0, in.remaining());
        
        assertEquals(ClassKind.CONNECTION, actual.getClassKind());
        assertEquals(ConnectionMethodKind.CLOSE_OK, actual.getMethodKind());
    }

    @Test
    public void decodeOpen() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        
        session.setTransportMetadata(new DefaultTransportMetadata(
                "mina", "dummy", false, true,
                SocketAddress.class, IoSessionConfig.class, Object.class));

        ProtocolDecoderOutput output = session.getDecoderOutput();
        Queue<Object> outputQueue = session.getDecoderOutputQueue();

        AmqpMessageDecoder decoder = new AmqpMessageDecoder(allocator, false);
        
        byte[] header = AmqpProtocolHeaderMessage.PROTOCOL_0_9_1_DEFAULT_HEADER;
        
        ByteBuffer buf = allocator.allocate(header.length);

        buf.put(header);
        buf.flip();

        IoBuffer inHeader = (IoBuffer) allocator.wrap(buf);
        
        decoder.decode(session, inHeader, output);
        outputQueue.poll();

        byte[] bytes = {0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 
                        0x08, 0x00, 0x0a, 0x00, 0x28, 0x01, 
                        0x2f, 0x00, 0x00, (byte)0xce};

        ByteBuffer obuf = allocator.allocate(bytes.length);
        obuf.put(bytes);
        obuf.flip();

        IoBuffer in = (IoBuffer) allocator.wrap(obuf);
        
        decoder.decode(session, in, output);

        AmqpOpenMessage actual = (AmqpOpenMessage) outputQueue.poll();
        assertEquals(0, outputQueue.size());
        assertEquals(0, in.remaining());
        
        assertEquals(ClassKind.CONNECTION, actual.getClassKind());
        assertEquals(ConnectionMethodKind.OPEN, actual.getMethodKind());

        assertEquals("/", actual.getVirtualHost());
    }

    @Test
    public void decodeOpenOk() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        
        session.setTransportMetadata(new DefaultTransportMetadata(
                "mina", "dummy", false, true,
                SocketAddress.class, IoSessionConfig.class, Object.class));

        ProtocolDecoderOutput output = session.getDecoderOutput();
        Queue<Object> outputQueue = session.getDecoderOutputQueue();

        AmqpMessageDecoder decoder = new AmqpMessageDecoder(allocator, true);
        byte[] bytes = {0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 
                        0x06, 0x00, 0x0a, 0x00, 0x29, 0x01, 
                        0x2f, (byte)0xce};

        ByteBuffer buf = allocator.allocate(bytes.length);
        buf.put(bytes);
        buf.flip();

        IoBuffer in = (IoBuffer) allocator.wrap(buf);
        
        decoder.decode(session, in, output);

        AmqpOpenOkMessage actual = (AmqpOpenOkMessage) outputQueue.poll();
        assertEquals(0, outputQueue.size());
        assertEquals(0, in.remaining());

        assertEquals(ClassKind.CONNECTION, actual.getClassKind());
        assertEquals(ConnectionMethodKind.OPEN_OK, actual.getMethodKind());

        assertEquals("/", actual.getReserved1());
    }

    @Test
    public void decodeSecure() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        session.getBufferAllocator();
        
        session.setTransportMetadata(new DefaultTransportMetadata(
                "mina", "dummy", false, true,
                SocketAddress.class, IoSessionConfig.class, Object.class));

        session.getDecoderOutput();
        session.getDecoderOutputQueue();

    }
    
    @Test
    public void decodeSecureOk() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        session.getBufferAllocator();
        
        session.setTransportMetadata(new DefaultTransportMetadata(
                "mina", "dummy", false, true,
                SocketAddress.class, IoSessionConfig.class, Object.class));

        session.getDecoderOutput();
        session.getDecoderOutputQueue();

    }

    @Test
    public void decodeStart() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        
        session.setTransportMetadata(new DefaultTransportMetadata(
                "mina", "dummy", false, true,
                SocketAddress.class, IoSessionConfig.class, Object.class));

        ProtocolDecoderOutput output = session.getDecoderOutput();
        Queue<Object> outputQueue = session.getDecoderOutputQueue();

        AmqpMessageDecoder decoder = new AmqpMessageDecoder(allocator, true);
        byte[] bytes = {0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02e, 0x00, 0x0a,
                        0x00, 0x0a, 0x00, 0x09, 0x00, 0x00, 0x00, 0x00, 0x00,
                        0x00, 0x00, 0x17, 0x41, 0x4d, 0x51, 0x50, 0x4c, 0x41,
                        0x49, 0x4e, 0x20, 0x50, 0x4c, 0x41, 0x49, 0x4e, 0x20,
                        0x43, 0x52, 0x41, 0x4d, 0x2d, 0x4d, 0x44, 0x35, 0x00,
                        0x00, 0x00, 0x05, 0x65, 0x6e, 0x5f, 0x55, 0x53, (byte) 0xce};

        ByteBuffer buf = allocator.allocate(bytes.length);
        buf.put(bytes);
        buf.flip();

        IoBuffer in = (IoBuffer) allocator.wrap(buf);
        
        decoder.decode(session, in, output);

        AmqpStartMessage actual = (AmqpStartMessage) outputQueue.poll();
        assertEquals(0, outputQueue.size());
        assertEquals(0, in.remaining());

        assertEquals(ClassKind.CONNECTION, actual.getClassKind());
        assertEquals(ConnectionMethodKind.START, actual.getMethodKind());

        assertEquals(0, actual.getVersionMajor());
        assertEquals(9, actual.getVersionMinor());
        assertEquals("en_US", actual.getLocales());
        
        String mechanisms = actual.getSecurityMechanisms();
        assertTrue(mechanisms.contains("AMQPLAIN"));
        assertTrue(mechanisms.contains("PLAIN"));
        assertTrue(mechanisms.contains("CRAM-MD5"));
    }

    @Test
    public void decodeStartOk() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        
        session.setTransportMetadata(new DefaultTransportMetadata(
                "mina", "dummy", false, true,
                SocketAddress.class, IoSessionConfig.class, Object.class));

        ProtocolDecoderOutput output = session.getDecoderOutput();
        Queue<Object> outputQueue = session.getDecoderOutputQueue();

        AmqpMessageDecoder decoder = new AmqpMessageDecoder(allocator, false);
        byte[]     header = AmqpProtocolHeaderMessage.PROTOCOL_0_9_1_DEFAULT_HEADER;
        ByteBuffer buf = allocator.allocate(header.length);
        buf.put(header);
        buf.flip();

        IoBuffer inHeader = (IoBuffer) allocator.wrap(buf);

        decoder.decode(session, inHeader, output);
        outputQueue.poll();
        
        byte[] bytes = {0x01, 0x00, 0x00, 0x00, 0x00, 0x00, (byte)0x96, 0x00, 0x0a, 0x00, 0x0b,
                0x00, 0x00, 0x00, 0x58, 0x07, 0x6c, 0x69, 0x62, 0x72, 0x61, 0x72, 
                0x79, 0x53, 0x00, 0x00, 0x00, 0x11, 0x4b, 0x61, 0x61, 0x7a, 0x69, 
                0x6e, 0x67, 0x41, 0x6d, 0x71, 0x70, 0x43, 0x6c, 0x69, 0x65, 0x6e, 
                0x74, 0x0f, 0x6c, 0x69, 0x62, 0x72, 0x61, 0x72, 0x79, 0x5f, 0x76, 
                0x65, 0x72, 0x73, 0x69, 0x6f, 0x6e, 0x53, 0x00, 0x00, 0x00, 0x05, 
                0x33, 0x2e, 0x33, 0x2e, 0x30, 0x10, 0x6c, 0x69, 0x62, 0x72, 0x61, 
                0x72, 0x79, 0x5f, 0x70, 0x6c, 0x61, 0x74, 0x66, 0x6f, 0x72, 0x6d, 
                0x53, 0x00, 0x00, 0x00, 0x0a, 0x4a, 0x61, 0x76, 0x61, 0x73, 0x63, 
                0x72, 0x69, 0x70, 0x74, 0x08, 0x41, 0x4d, 0x51, 0x50, 0x4c, 0x41, 
                0x49, 0x4e, 0x00, 0x00, 0x00, 0x23, 0x05, 0x4c, 0x4f, 0x47, 0x49, 
                0x4e, 0x53, 0x00, 0x00, 0x00, 0x05, 0x67, 0x75, 0x65, 0x73, 0x74,
                0x08, 0x50, 0x41, 0x53, 0x53, 0x57, 0x4f, 0x52, 0x44, 0x53, 0x00, 
                0x00, 0x00, 0x05, 0x67, 0x75, 0x65, 0x73, 0x74, 0x05, 0x65, 0x6e, 
                0x5f, 0x55, 0x53, (byte)0xce};

        ByteBuffer sbuf = allocator.allocate(bytes.length);
        sbuf.put(bytes);
        sbuf.flip();

        IoBuffer in = (IoBuffer) allocator.wrap(sbuf);
        
        decoder.decode(session, in, output);

        AmqpStartOkMessage actual = (AmqpStartOkMessage) outputQueue.poll();
        assertEquals(0, outputQueue.size());
        assertEquals(0, in.remaining());

        assertEquals(ClassKind.CONNECTION, actual.getClassKind());
        assertEquals(ConnectionMethodKind.START_OK, actual.getMethodKind());        
        assertEquals("guest", actual.getUsername());
        assertArrayEquals("guest".toCharArray(), actual.getPassword());
        assertEquals("en_US", actual.getLocale());
        assertEquals("AMQPLAIN", actual.getSecurityMechanism());

        AmqpTable table = actual.getClientProperties();
        
        List<AmqpTableEntry> entries = table.getEntries("library");
        assertEquals(1, entries.size());
        assertEquals("KaazingAmqpClient", entries.get(0).getValue());
        assertEquals(AmqpType.LONGSTRING, entries.get(0).getType());
        
        entries = table.getEntries("library_version");
        assertEquals(1, entries.size());
        assertEquals("3.3.0", entries.get(0).getValue());
        assertEquals(AmqpType.LONGSTRING, entries.get(0).getType());

        entries = table.getEntries("library_platform");
        assertEquals(1, entries.size());
        assertEquals("Javascript", entries.get(0).getValue());
        assertEquals(AmqpType.LONGSTRING, entries.get(0).getType());
    }
    
    @Test
    public void decodeStartOkWithPlainAuth() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        
        session.setTransportMetadata(new DefaultTransportMetadata(
                "mina", "dummy", false, true,
                SocketAddress.class, IoSessionConfig.class, Object.class));

        ProtocolDecoderOutput output = session.getDecoderOutput();
        Queue<Object> outputQueue = session.getDecoderOutputQueue();

        AmqpMessageDecoder decoder = new AmqpMessageDecoder(allocator, false);
        byte[]     header = AmqpProtocolHeaderMessage.PROTOCOL_0_9_1_DEFAULT_HEADER;
        ByteBuffer buf = allocator.allocate(header.length);
        buf.put(header);
        buf.flip();

        IoBuffer inHeader = (IoBuffer) allocator.wrap(buf);

        decoder.decode(session, inHeader, output);
        outputQueue.poll();
        
        byte[] bytes = {0x01, 
                        0x00, 0x00, 
                        0x00, 0x00, 0x00, 0x74, 
                        0x00, 0x0A, 
                        0x00, 0x0B, 
                        0x00, 0x00, 0x00, 0x50, 
                        0x07, 0x70, 0x72, 0x6f, 0x64, 0x75, 0x63, 0x74, 0x53, 
                        0x00, 0x00, 0x00, 0x0a, 0x72, 0x61, 0x62, 0x62, 0x69, 
                        0x74, 0x6d, 0x71, 0x2d, 0x63, 0x0b, 0x69, 0x6e, 0x66, 
                        0x6f, 0x72, 0x6d, 0x61, 0x74, 0x69, 0x6f, 0x6e, 0x53, 
                        0x00, 0x00, 0x00, 0x28, 0x53, 0x65, 0x65, 0x20, 0x68, 
                        0x74, 0x74, 0x70, 0x73, 0x3a, 0x2f, 0x2f, 0x67, 0x69, 
                        0x74, 0x68, 0x75, 0x62, 0x2e, 0x63, 0x6f, 0x6d, 0x2f, 
                        0x61, 0x6c, 0x61, 0x6e, 0x78, 0x7a, 0x2f, 0x72, 0x61, 
                        0x62, 0x62, 0x69, 0x74, 0x6d, 0x71, 0x2d, 0x63, 
                        0x05, 0x50, 0x4c, 0x41, 0x49, 0x4e, // Mechanism: PLAIN
                        0x00, 0x00, 0x00, 0x0c, 
                        0x00, 0x67, 0x75, 0x65, 0x73, 0x74, 0x00, 0x67, 0x75, 0x65, 0x73, 0x74, 
                        0x05, 0x65, 0x6e, 0x5f, 0x55, 0x53, 
                        (byte)0xce}; // End of Frame

        ByteBuffer sbuf = allocator.allocate(bytes.length);
        sbuf.put(bytes);
        sbuf.flip();

        IoBuffer in = (IoBuffer) allocator.wrap(sbuf);
        
        decoder.decode(session, in, output);

        AmqpStartOkMessage actual = (AmqpStartOkMessage) outputQueue.poll();
        assertEquals(0, outputQueue.size());
        assertEquals(0, in.remaining());

        assertEquals(ClassKind.CONNECTION, actual.getClassKind());
        assertEquals(ConnectionMethodKind.START_OK, actual.getMethodKind());        
        assertEquals("guest", actual.getUsername());
        assertArrayEquals("guest".toCharArray(), actual.getPassword());
        assertEquals("en_US", actual.getLocale());
        assertEquals("PLAIN", actual.getSecurityMechanism());
        
        AmqpTable clientProperties = actual.getClientProperties();
        List<AmqpTableEntry> productEntries = clientProperties.getEntries("product");
        assertTrue(productEntries.size() == 1);
        assertEquals("rabbitmq-c", productEntries.get(0).getValue());
        assertEquals(AmqpType.LONGSTRING, productEntries.get(0).getType());
        
        assertTrue(AMQP_AUTHENTICATION_MECHANISM + " should be injected as session attribute", session.containsAttribute(AMQP_AUTHENTICATION_MECHANISM));
        assertEquals(session.getAttribute(AMQP_AUTHENTICATION_MECHANISM), "PLAIN");
        
    }
    
    @Test
    public void decodeTune() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        
        session.setTransportMetadata(new DefaultTransportMetadata(
                "mina", "dummy", false, true,
                SocketAddress.class, IoSessionConfig.class, Object.class));

        ProtocolDecoderOutput output = session.getDecoderOutput();
        Queue<Object> outputQueue = session.getDecoderOutputQueue();

        AmqpMessageDecoder decoder = new AmqpMessageDecoder(allocator, true);
        byte[] bytes = {0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 
                        0x0c, 0x00, 0x0a, 0x00, 0x1e, 0x01, 
                        0x00, 0x00, 0x00, (byte)0xff, (byte)0xff, 0x00, 
                        0x00, (byte)0xce};

        ByteBuffer buf = allocator.allocate(bytes.length);
        buf.put(bytes);
        buf.flip();

        IoBuffer in = (IoBuffer) allocator.wrap(buf);
        
        decoder.decode(session, in, output);

        AmqpTuneMessage actual = (AmqpTuneMessage) outputQueue.poll();
        assertEquals(0, outputQueue.size());
        assertEquals(0, in.remaining());
        
        assertEquals(ClassKind.CONNECTION, actual.getClassKind());
        assertEquals(ConnectionMethodKind.TUNE, actual.getMethodKind());
        assertEquals(256, actual.getMaxChannels());
        assertEquals(65535, actual.getMaxFrameSize());
        assertEquals(0, actual.getHeartbeatDelay());
    }

    @Test
    public void decodeTuneOk() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        
        session.setTransportMetadata(new DefaultTransportMetadata(
                "mina", "dummy", false, true,
                SocketAddress.class, IoSessionConfig.class, Object.class));

        ProtocolDecoderOutput output = session.getDecoderOutput();
        Queue<Object> outputQueue = session.getDecoderOutputQueue();

        AmqpMessageDecoder decoder = new AmqpMessageDecoder(allocator, false);
        byte[]     header = AmqpProtocolHeaderMessage.PROTOCOL_0_9_1_DEFAULT_HEADER;        
        ByteBuffer buf = allocator.allocate(header.length);
        buf.put(header);
        buf.flip();

        IoBuffer inHeader = (IoBuffer) allocator.wrap(buf);

        decoder.decode(session, inHeader, output);
        outputQueue.poll();
        
        byte[] bytes = {0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 
                        0x0c, 0x00, 0x0a, 0x00, 0x1f, 0x01, 
                        0x00, 0x00, 0x00, (byte)0xff, (byte)0xff, 0x00, 
                        0x00, (byte)0xce};

        ByteBuffer tbuf = allocator.allocate(bytes.length);

        tbuf.put(bytes);
        tbuf.flip();

        IoBuffer in = (IoBuffer) allocator.wrap(tbuf);
        
        decoder.decode(session, in, output);

        AmqpTuneOkMessage actual = (AmqpTuneOkMessage) outputQueue.poll();
        assertEquals(0, outputQueue.size());
        assertEquals(0, in.remaining());
        
        assertEquals(ClassKind.CONNECTION, actual.getClassKind());
        assertEquals(ConnectionMethodKind.TUNE_OK, actual.getMethodKind());
        assertEquals(256, actual.getMaxChannels());
        assertEquals(65535, actual.getMaxFrameSize());
        assertEquals(0, actual.getHeartbeatDelay());
    }
}
