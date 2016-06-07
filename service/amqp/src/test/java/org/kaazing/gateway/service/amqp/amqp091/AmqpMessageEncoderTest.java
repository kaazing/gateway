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

import java.util.Locale;
import java.util.Queue;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.junit.Test;
import org.kaazing.gateway.service.amqp.amqp091.codec.AmqpMessageEncoder;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpCloseMessage;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpCloseOkMessage;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpOpenMessage;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpOpenOkMessage;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpProtocolHeaderMessage;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpStartMessage;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpStartOkMessage;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpTuneMessage;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpTuneOkMessage;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.filter.codec.ProtocolCodecSessionEx;

public class AmqpMessageEncoderTest {

    @Test
    public void encodeProtocolHeader() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder        encoder = new AmqpMessageEncoder(allocator);
        ProtocolEncoderOutput  encoderOut = session.getEncoderOutput();
        Queue<Object> encoderOutputQueue = session.getEncoderOutputQueue();

        AmqpProtocolHeaderMessage message = new AmqpProtocolHeaderMessage();
        
        encoder.encode(session, message, encoderOut);

        IoBuffer actual = (IoBuffer)encoderOutputQueue.poll();
        assertEquals(0, encoderOutputQueue.size());
        
        byte[] bytes = new byte[actual.remaining()];
        actual.get(bytes, 
                   actual.arrayOffset() + actual.position(), 
                   actual.remaining());
        assertArrayEquals(message.getProtocolHeader(), bytes);
    }

    @Test
    public void encodeStart() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder        encoder = new AmqpMessageEncoder(allocator);
        ProtocolEncoderOutput  encoderOut = session.getEncoderOutput();
        Queue<Object> encoderOutputQueue = session.getEncoderOutputQueue();

        AmqpStartMessage message = new AmqpStartMessage();
        
        message.setVersionMajor((byte) 0);
        message.setVersionMinor((byte) 9);
        message.setSecurityMechanisms("AMQPLAIN PLAIN CRAM-MD5");
        message.setLocales(new Locale("en", "US").toString());
        
        encoder.encode(session, message, encoderOut);

        IoBuffer actual = (IoBuffer)encoderOutputQueue.poll();
        assertEquals(0, encoderOutputQueue.size());
        
        byte[] expected = {0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x2e, 0x00, 0x0a,
                           0x00, 0x0a, 0x00, 0x09, 0x00, 0x00, 0x00, 0x00, 0x00,
                           0x00, 0x00, 0x17, 0x41, 0x4d, 0x51, 0x50, 0x4c, 0x41,
                           0x49, 0x4e, 0x20, 0x50, 0x4c, 0x41, 0x49, 0x4e, 0x20,
                           0x43, 0x52, 0x41, 0x4d, 0x2d, 0x4d, 0x44, 0x35, 0x00,
                           0x00, 0x00, 0x05, 0x65, 0x6e, 0x5f, 0x55, 0x53, (byte) 0xce};

        
        byte[] bytes = new byte[actual.remaining()];
        actual.get(bytes, 
                   actual.arrayOffset() + actual.position(), 
                   actual.remaining());
        assertArrayEquals(expected, bytes);
    }
    
    @Test
    public void encodeStartOk() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder        encoder = new AmqpMessageEncoder(allocator);
        ProtocolEncoderOutput  encoderOut = session.getEncoderOutput();
        Queue<Object> encoderOutputQueue = session.getEncoderOutputQueue();

        AmqpStartOkMessage message = new AmqpStartOkMessage();
        AmqpTable          clientProps = new AmqpTable();
        
        clientProps.addLongString("library", "KaazingAmqpClient");
        clientProps.addLongString("library_version", "3.3.0");
        clientProps.addLongString("library_platform", "Javascript");
        
        message.setClientProperties(clientProps);
        message.setSecurityMechanism("AMQPLAIN");
        message.setUsername("guest");
        message.setPassword("guest".toCharArray());
        message.setLocale(new Locale("en", "US").toString());

        encoder.encode(session, message, encoderOut);

        IoBuffer actual = (IoBuffer)encoderOutputQueue.poll();
        assertEquals(0, encoderOutputQueue.size());
        
        byte[] expected = {0x01, 0x00, 0x00, 0x00, 0x00, 0x00, (byte)0x96, 0x00, 0x0a, 0x00, 0x0b,
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
        
        byte[] bytes = new byte[actual.remaining()];
        actual.get(bytes, 
                   actual.arrayOffset() + actual.position(), 
                   actual.remaining());

        assertArrayEquals(expected, bytes);
    }
    
    @Test
    public void encodeStartOkWithPlainAuth() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder        encoder = new AmqpMessageEncoder(allocator);
        ProtocolEncoderOutput  encoderOut = session.getEncoderOutput();
        Queue<Object> encoderOutputQueue = session.getEncoderOutputQueue();

        AmqpStartOkMessage message = new AmqpStartOkMessage();
        AmqpTable          clientProps = new AmqpTable();
        
        clientProps.addLongString("product", "rabbitmq-c");
        clientProps.addLongString("information", "See https://github.com/alanxz/rabbitmq-c");
        
        message.setClientProperties(clientProps);
        message.setSecurityMechanism("PLAIN");
        message.setUsername("guest");
        message.setPassword("guest".toCharArray());
        message.setLocale(new Locale("en", "US").toString());

        encoder.encode(session, message, encoderOut);

        IoBuffer actual = (IoBuffer)encoderOutputQueue.poll();
        assertEquals(0, encoderOutputQueue.size());
        
        byte[] expected = {0x01, 
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
        
        byte[] bytes = new byte[actual.remaining()];
        actual.get(bytes, 
                   actual.arrayOffset() + actual.position(), 
                   actual.remaining());

        assertArrayEquals(expected, bytes);
        assertTrue(AMQP_AUTHENTICATION_MECHANISM + " should be injected as session attribute", session.containsAttribute(AMQP_AUTHENTICATION_MECHANISM));
        assertEquals(session.getAttribute(AMQP_AUTHENTICATION_MECHANISM), "PLAIN");
    }

    @Test
    public void encodeSecure() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        new AmqpMessageEncoder(allocator);
        session.getEncoderOutput();
        session.getEncoderOutputQueue();

    }
    
    @Test
    public void encodeSecureOk() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        new AmqpMessageEncoder(allocator);
        session.getEncoderOutput();
        session.getEncoderOutputQueue();

    }

    @Test
    public void encodeTune() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder        encoder = new AmqpMessageEncoder(allocator);
        ProtocolEncoderOutput  encoderOut = session.getEncoderOutput();
        Queue<Object> encoderOutputQueue = session.getEncoderOutputQueue();

        AmqpTuneMessage message = new AmqpTuneMessage();
        
        message.setMaxChannels(256);
        message.setMaxFrameSize(0xFFFF);
        message.setHeartbeatDelay(0);

        encoder.encode(session, message, encoderOut);

        IoBuffer actual = (IoBuffer)encoderOutputQueue.poll();
        assertEquals(0, encoderOutputQueue.size());
        
        byte[] expected = {0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 
                           0x0c, 0x00, 0x0a, 0x00, 0x1e, 0x01, 
                           0x00, 0x00, 0x00, (byte)0xff, (byte)0xff, 0x00, 
                           0x00, (byte)0xce};
        
        byte[] bytes = new byte[actual.remaining()];
        actual.get(bytes, 
                   actual.arrayOffset() + actual.position(), 
                   actual.remaining());
        assertArrayEquals(expected, bytes);
    }

    @Test
    public void encodeTuneOk() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder        encoder = new AmqpMessageEncoder(allocator);
        ProtocolEncoderOutput  encoderOut = session.getEncoderOutput();
        Queue<Object> encoderOutputQueue = session.getEncoderOutputQueue();

        AmqpTuneOkMessage message = new AmqpTuneOkMessage();
        
        message.setMaxChannels(256);
        message.setMaxFrameSize(0xFFFF);
        message.setHeartbeatDelay(0);

        encoder.encode(session, message, encoderOut);

        IoBuffer actual = (IoBuffer)encoderOutputQueue.poll();
        assertEquals(0, encoderOutputQueue.size());
        
        byte[] expected = {0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 
                           0x0c, 0x00, 0x0a, 0x00, 0x1f, 0x01, 
                           0x00, 0x00, 0x00, (byte)0xff, (byte)0xff, 0x00, 
                           0x00, (byte)0xce};
        
        byte[] bytes = new byte[actual.remaining()];
        actual.get(bytes, 
                   actual.arrayOffset() + actual.position(), 
                   actual.remaining());
        assertArrayEquals(expected, bytes);
    }

    @Test
    public void encodeOpen() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder        encoder = new AmqpMessageEncoder(allocator);
        ProtocolEncoderOutput  encoderOut = session.getEncoderOutput();
        Queue<Object> encoderOutputQueue = session.getEncoderOutputQueue();

        AmqpOpenMessage message = new AmqpOpenMessage();
        message.setVirtualHost("/");

        encoder.encode(session, message, encoderOut);

        IoBuffer actual = (IoBuffer)encoderOutputQueue.poll();
        assertEquals(0, encoderOutputQueue.size());
        
        byte[] expected = {0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 
                           0x08, 0x00, 0x0a, 0x00, 0x28, 0x01, 
                           0x2f, 0x00, 0x00, (byte)0xce};
        
        byte[] bytes = new byte[actual.remaining()];
        actual.get(bytes, 
                   actual.arrayOffset() + actual.position(), 
                   actual.remaining());
        assertArrayEquals(expected, bytes);
    }

    @Test
    public void encodeOpenOk() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder        encoder = new AmqpMessageEncoder(allocator);
        ProtocolEncoderOutput  encoderOut = session.getEncoderOutput();
        Queue<Object> encoderOutputQueue = session.getEncoderOutputQueue();

        AmqpOpenOkMessage message = new AmqpOpenOkMessage();
        message.setReserved1("/");

        encoder.encode(session, message, encoderOut);

        IoBuffer actual = (IoBuffer)encoderOutputQueue.poll();
        assertEquals(0, encoderOutputQueue.size());
        
        byte[] expected = {0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 
                           0x06, 0x00, 0x0a, 0x00, 0x29, 0x01, 
                           0x2f, (byte)0xce};
        
        byte[] bytes = new byte[actual.remaining()];
        actual.get(bytes, 
                   actual.arrayOffset() + actual.position(), 
                   actual.remaining());
        assertArrayEquals(expected, bytes);
    }

    @Test
    public void encodeClose() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder        encoder = new AmqpMessageEncoder(allocator);
        ProtocolEncoderOutput  encoderOut = session.getEncoderOutput();
        Queue<Object> encoderOutputQueue = session.getEncoderOutputQueue();

        AmqpCloseMessage message = new AmqpCloseMessage();

        encoder.encode(session, message, encoderOut);

        IoBuffer actual = (IoBuffer)encoderOutputQueue.poll();
        assertEquals(0, encoderOutputQueue.size());
        
        byte[] expected = {0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0b, 
                           0x00, 0x0a, 0x00, 0x32, 0x00, 0x00, 0x00, 
                           0x00, 0x00, 0x00, 0x00, (byte)0xce};
        
        byte[] bytes = new byte[actual.remaining()];
        actual.get(bytes, 
                   actual.arrayOffset() + actual.position(), 
                   actual.remaining());
        assertArrayEquals(expected, bytes);
    }

    @Test
    public void encodeCloseOk() throws Exception {
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder        encoder = new AmqpMessageEncoder(allocator);
        ProtocolEncoderOutput  encoderOut = session.getEncoderOutput();
        Queue<Object> encoderOutputQueue = session.getEncoderOutputQueue();

        AmqpCloseOkMessage message = new AmqpCloseOkMessage();

        encoder.encode(session, message, encoderOut);

        IoBuffer actual = (IoBuffer)encoderOutputQueue.poll();
        assertEquals(0, encoderOutputQueue.size());
        
        byte[] expected = {0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x04, 
                           0x00, 0x0a, 0x00, 0x33, (byte)0xce};
        
        byte[] bytes = new byte[actual.remaining()];
        actual.get(bytes, 
                   actual.arrayOffset() + actual.position(), 
                   actual.remaining());
        assertArrayEquals(expected, bytes);
    }

    /*
     * Useful for writing tests
     * @param bytes
     *
    private void hexDump(byte[] bytes) {
        StringBuilder hexDump = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            hexDump.append(Integer.toHexString(bytes[i]&0xFF)).append(' ');
        }
                
        String s = hexDump.toString();
        System.out.println("Hex Dump: " + s);
    }
    */
}
