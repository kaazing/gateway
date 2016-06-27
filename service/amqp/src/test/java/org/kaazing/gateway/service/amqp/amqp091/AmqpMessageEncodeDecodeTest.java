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

import java.net.SocketAddress;
import java.util.List;
import java.util.Locale;
import java.util.Queue;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.DefaultTransportMetadata;
import org.apache.mina.core.session.IoSessionConfig;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.junit.Test;
import org.kaazing.gateway.service.amqp.amqp091.AmqpTable.AmqpTableEntry;
import org.kaazing.gateway.service.amqp.amqp091.codec.AmqpMessageDecoder;
import org.kaazing.gateway.service.amqp.amqp091.codec.AmqpMessageEncoder;
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

public class AmqpMessageEncodeDecodeTest {
    @Test
    public void testClose() throws Exception {
        IoBuffer               buffer;
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder        encoder = new AmqpMessageEncoder(allocator);
        ProtocolEncoderOutput  encoderOut = session.getEncoderOutput();
        Queue<Object> encoderOutputQueue = session.getEncoderOutputQueue();

        AmqpCloseMessage         message = new AmqpCloseMessage();
        String                   replyText = "Broker shutdown";
        int                      replyCode = 9090;

        message.setReplyText(replyText);
        message.setReplyCode(replyCode);

        try {
            encoder.encode(session, message, encoderOut);
            buffer = (IoBuffer)encoderOutputQueue.poll();
        } 
        catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
                
        try {
            ProtocolCodecSessionEx decSession = new ProtocolCodecSessionEx();
            IoBufferAllocatorEx<?> decAllocator = session.getBufferAllocator();

            decSession.setTransportMetadata(new DefaultTransportMetadata(
                    "mina", "dummy", false, true,
                    SocketAddress.class, IoSessionConfig.class, Object.class));

            ProtocolDecoderOutput output = decSession.getDecoderOutput();
            Queue<Object> outputQueue = decSession.getDecoderOutputQueue();

            AmqpMessageDecoder decoder = new AmqpMessageDecoder(decAllocator, true);
            decoder.decode(decSession, buffer, output);
            AmqpCloseMessage actual = (AmqpCloseMessage) outputQueue.poll();

            System.out.println("Reply Text: " + actual.getReplyText());
            System.out.println("Reply Code: " + actual.getReplyCode());
            assertEquals(replyText, actual.getReplyText());
            assertEquals(replyCode, actual.getReplyCode());

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    public void testCloseOk() throws Exception {
        IoBuffer               buffer;
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder        encoder = new AmqpMessageEncoder(allocator);
        ProtocolEncoderOutput  encoderOut = session.getEncoderOutput();
        Queue<Object> encoderOutputQueue = session.getEncoderOutputQueue();

        AmqpCloseOkMessage       message = new AmqpCloseOkMessage();

        try {
            encoder.encode(session, message, encoderOut);
            buffer = (IoBuffer)encoderOutputQueue.poll();
        } 
        catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        
        try {
            ProtocolCodecSessionEx decSession = new ProtocolCodecSessionEx();
            IoBufferAllocatorEx<?> decAllocator = session.getBufferAllocator();

            session.setTransportMetadata(new DefaultTransportMetadata(
                    "mina", "dummy", false, true,
                    SocketAddress.class, IoSessionConfig.class, Object.class));

            ProtocolDecoderOutput output = decSession.getDecoderOutput();
            Queue<Object> outputQueue = decSession.getDecoderOutputQueue();

            AmqpMessageDecoder decoder = new AmqpMessageDecoder(decAllocator, true);
            decoder.decode(decSession, buffer, output);
            AmqpCloseOkMessage actual = (AmqpCloseOkMessage) outputQueue.poll();

            System.out.println("Method Kind: " + actual.getMethodKind());
            assertEquals(ConnectionMethodKind.CLOSE_OK, actual.getMethodKind());

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    public void testOpen() throws Exception {
        IoBuffer               buffer;
        IoBuffer               protocolHeaderBuffer;
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder        encoder = new AmqpMessageEncoder(allocator);
        ProtocolEncoderOutput  encoderOut = session.getEncoderOutput();
        Queue<Object> encoderOutputQueue = session.getEncoderOutputQueue();

        AmqpOpenMessage          message = new AmqpOpenMessage();
        String                   virtualHost = "/kaazing";

        message.setVirtualHost(virtualHost);

        try {
            AmqpProtocolHeaderMessage phm = new AmqpProtocolHeaderMessage();
            encoder.encode(session, phm, encoderOut);
            protocolHeaderBuffer = (IoBuffer)encoderOutputQueue.poll();

            encoder.encode(session, message, encoderOut);
            buffer = (IoBuffer)encoderOutputQueue.poll();
        } 
        catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        
        try {
            ProtocolCodecSessionEx decSession = new ProtocolCodecSessionEx();
            IoBufferAllocatorEx<?> decAllocator = session.getBufferAllocator();

            session.setTransportMetadata(new DefaultTransportMetadata(
                    "mina", "dummy", false, true,
                    SocketAddress.class, IoSessionConfig.class, Object.class));

            ProtocolDecoderOutput output = decSession.getDecoderOutput();
            Queue<Object> outputQueue = decSession.getDecoderOutputQueue();

            AmqpMessageDecoder decoder = new AmqpMessageDecoder(decAllocator, false);

            decoder.decode(decSession, protocolHeaderBuffer, output);
            outputQueue.poll();

            decoder.decode(decSession, buffer, output);
            AmqpOpenMessage actual = (AmqpOpenMessage) outputQueue.poll();

            System.out.println("Virtual Host: " + actual.getVirtualHost());
            assertEquals(virtualHost, actual.getVirtualHost());

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    public void testOpenOk() throws Exception {
        IoBuffer               buffer;
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder        encoder = new AmqpMessageEncoder(allocator);
        ProtocolEncoderOutput  encoderOut = session.getEncoderOutput();
        Queue<Object> encoderOutputQueue = session.getEncoderOutputQueue();

        AmqpOpenOkMessage        message = new AmqpOpenOkMessage();
        String                   reserved1 = "/amqp";

        message.setReserved1(reserved1);

        try {
            encoder.encode(session, message, encoderOut);
            buffer = (IoBuffer)encoderOutputQueue.poll();
        } 
        catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
                
        try {
            ProtocolCodecSessionEx decSession = new ProtocolCodecSessionEx();
            IoBufferAllocatorEx<?> decAllocator = session.getBufferAllocator();

            session.setTransportMetadata(new DefaultTransportMetadata(
                    "mina", "dummy", false, true,
                    SocketAddress.class, IoSessionConfig.class, Object.class));

            ProtocolDecoderOutput output = decSession.getDecoderOutput();
            Queue<Object> outputQueue = decSession.getDecoderOutputQueue();

            AmqpMessageDecoder decoder = new AmqpMessageDecoder(decAllocator, true);
            decoder.decode(decSession, buffer, output);
            AmqpOpenOkMessage actual = (AmqpOpenOkMessage) outputQueue.poll();

            System.out.println("Reserved1: " + actual.getReserved1());
            assertEquals(reserved1, actual.getReserved1());

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    public void testTune() throws Exception {
        IoBuffer               buffer;
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder        encoder = new AmqpMessageEncoder(allocator);
        ProtocolEncoderOutput  encoderOut = session.getEncoderOutput();
        Queue<Object> encoderOutputQueue = session.getEncoderOutputQueue();

        AmqpTuneMessage          message = new AmqpTuneMessage();
        int                      maxChannels = 512;
        int                      maxFrameSize = Integer.MAX_VALUE - 1;
        int                      heartbeatDelay = 18;

        message.setMaxChannels(maxChannels);
        message.setMaxFrameSize(maxFrameSize);
        message.setHeartbeatDelay(heartbeatDelay);
        
        try {
            encoder.encode(session, message, encoderOut);
            buffer = (IoBuffer)encoderOutputQueue.poll();
        } 
        catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
                
        try {
            ProtocolCodecSessionEx decSession = new ProtocolCodecSessionEx();
            IoBufferAllocatorEx<?> decAllocator = session.getBufferAllocator();
            
            session.setTransportMetadata(new DefaultTransportMetadata(
                    "mina", "dummy", false, true,
                    SocketAddress.class, IoSessionConfig.class, Object.class));

            ProtocolDecoderOutput output = decSession.getDecoderOutput();
            Queue<Object> outputQueue = decSession.getDecoderOutputQueue();

            AmqpMessageDecoder decoder = new AmqpMessageDecoder(decAllocator, true);
            decoder.decode(decSession, buffer, output);
            AmqpTuneMessage actual = (AmqpTuneMessage) outputQueue.poll();

            System.out.println("Max Channels: " + actual.getMaxChannels());
            System.out.println("Max Frame Size: " + actual.getMaxFrameSize());
            System.out.println("Heartbeat Delay: " + actual.getHeartbeatDelay());
            
            assertEquals(maxChannels, actual.getMaxChannels());
            assertEquals(maxFrameSize, actual.getMaxFrameSize());
            assertEquals(heartbeatDelay, actual.getHeartbeatDelay());
            
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    public void testTuneOk() throws Exception {
        IoBuffer               buffer;
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder        encoder = new AmqpMessageEncoder(allocator);
        ProtocolEncoderOutput  encoderOut = session.getEncoderOutput();
        Queue<Object> encoderOutputQueue = session.getEncoderOutputQueue();

        AmqpTuneOkMessage        message = new AmqpTuneOkMessage();
        int                      maxChannels = 256;
        int                      maxFrameSize = 0xFFFF;
        int                      heartbeatDelay = 0;

        message.setMaxChannels(maxChannels);
        message.setMaxFrameSize(maxFrameSize);
        message.setHeartbeatDelay(heartbeatDelay);

        try {
            encoder.encode(session, message, encoderOut);
            buffer = (IoBuffer)encoderOutputQueue.poll();
        } 
        catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        
        try {
            ProtocolCodecSessionEx decSession = new ProtocolCodecSessionEx();
            IoBufferAllocatorEx<?> decAllocator = session.getBufferAllocator();

            session.setTransportMetadata(new DefaultTransportMetadata(
                    "mina", "dummy", false, true,
                    SocketAddress.class, IoSessionConfig.class, Object.class));

            ProtocolDecoderOutput output = decSession.getDecoderOutput();
            Queue<Object> outputQueue = decSession.getDecoderOutputQueue();

            AmqpMessageDecoder decoder = new AmqpMessageDecoder(decAllocator, true);
            decoder.decode(decSession, buffer, output);
            AmqpTuneOkMessage actual = (AmqpTuneOkMessage) outputQueue.poll();

            System.out.println("Max Channels: " + actual.getMaxChannels());
            System.out.println("Max Frame Size: " + actual.getMaxFrameSize());
            System.out.println("Heartbeat Delay: " + actual.getHeartbeatDelay());

            assertEquals(maxChannels, actual.getMaxChannels());
            assertEquals(maxFrameSize, actual.getMaxFrameSize());
            assertEquals(heartbeatDelay, actual.getHeartbeatDelay());
            
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    public void testStart() throws Exception {
        IoBuffer               buffer;
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder        encoder = new AmqpMessageEncoder(allocator);
        ProtocolEncoderOutput  encoderOut = session.getEncoderOutput();
        Queue<Object> encoderOutputQueue = session.getEncoderOutputQueue();

        AmqpStartMessage         message = new AmqpStartMessage();
        String                   mechanisms = "AMQPLAIN PLAIN CRAM-MD5";
        String                   locales = new Locale("en", "US").toString();
        AmqpTable                props = new AmqpTable();
        byte                     majorVersion = 0;
        byte                     minorVersion = 9;
        
        props.addLongString("prop1", "value1");

        message.setVersionMajor(majorVersion);
        message.setVersionMinor(minorVersion);
        message.setServerProperties(props);
        message.setSecurityMechanisms(mechanisms);
        message.setLocales(locales);
        
        try {
            encoder.encode(session, message, encoderOut);
            buffer = (IoBuffer)encoderOutputQueue.poll();
        } 
        catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        
        try {
            ProtocolCodecSessionEx decSession = new ProtocolCodecSessionEx();
            IoBufferAllocatorEx<?> decAllocator = session.getBufferAllocator();

            session.setTransportMetadata(new DefaultTransportMetadata(
                    "mina", "dummy", false, true,
                    SocketAddress.class, IoSessionConfig.class, Object.class));

            ProtocolDecoderOutput output = decSession.getDecoderOutput();
            Queue<Object> outputQueue = decSession.getDecoderOutputQueue();

            AmqpMessageDecoder decoder = new AmqpMessageDecoder(decAllocator, true);
            decoder.decode(decSession, buffer, output);
            AmqpStartMessage actual = (AmqpStartMessage) outputQueue.poll();
            AmqpTable serverProps = actual.getServerProperties();
            
            List<AmqpTableEntry> entries = serverProps.getEntries("prop1");

            System.out.println("Version Major: " + actual.getVersionMajor());
            System.out.println("Version Minor: " + actual.getVersionMinor());
            System.out.println("Server Properties: " + serverProps);
            System.out.println("Mechanisms: " + actual.getSecurityMechanisms());
            System.out.println("Locales: " + actual.getLocales());

            assertEquals(majorVersion, actual.getVersionMajor());
            assertEquals(minorVersion, actual.getVersionMinor());
            assertEquals(mechanisms, actual.getSecurityMechanisms());
            assertEquals(locales, actual.getLocales());
            assertEquals("value1", entries.get(0).getValue());
            
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    public void testStartOk() throws Exception {
        IoBuffer               buffer;
        ProtocolCodecSessionEx session = new ProtocolCodecSessionEx();
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        ProtocolEncoder        encoder = new AmqpMessageEncoder(allocator);
        ProtocolEncoderOutput  encoderOut = session.getEncoderOutput();
        Queue<Object> encoderOutputQueue = session.getEncoderOutputQueue();

        AmqpStartOkMessage       message = new AmqpStartOkMessage();
        AmqpTable                props = new AmqpTable();
        String                   mechanism = "AMQPLAIN";
        String                   locale = new Locale("en", "US").toString();
        String                   username = "guest";
        
        props.addLongString("library", "KaazingAmqpClient");
        props.addLongString("library_version", "3.3.0");
        props.addLongString("library_platform", "Javascript");

        message.setClientProperties(props);
        message.setSecurityMechanism(mechanism);
        message.setUsername(username);
        message.setPassword("guest".toCharArray());
        message.setLocale(locale);
        
        try {
            encoder.encode(session, message, encoderOut);
            buffer = (IoBuffer)encoderOutputQueue.poll();
        } 
        catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        
        try {
            ProtocolCodecSessionEx decSession = new ProtocolCodecSessionEx();
            IoBufferAllocatorEx<?> decAllocator = session.getBufferAllocator();

            decSession.setTransportMetadata(new DefaultTransportMetadata(
                    "mina", "dummy", false, true,
                    SocketAddress.class, IoSessionConfig.class, Object.class));

            ProtocolDecoderOutput output = decSession.getDecoderOutput();
            Queue<Object> outputQueue = decSession.getDecoderOutputQueue();

            AmqpMessageDecoder decoder = new AmqpMessageDecoder(decAllocator, true);
            decoder.decode(decSession, buffer, output);
            AmqpStartOkMessage actual = (AmqpStartOkMessage) outputQueue.poll();

            System.out.println("Client Properties: " + actual.getClientProperties());
            System.out.println("Mechanism: " + actual.getSecurityMechanism());
            System.out.println("Locale: " + actual.getLocale());
            System.out.println("Username: " + actual.getUsername());
            System.out.println("Password: " + actual.getPassword().toString());
            
            assertEquals(mechanism, actual.getSecurityMechanism());
            assertEquals(locale, actual.getLocale());
            assertEquals(username, actual.getUsername());
            assertArrayEquals("guest".toCharArray(), actual.getPassword());
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }

        return;
    }

}
