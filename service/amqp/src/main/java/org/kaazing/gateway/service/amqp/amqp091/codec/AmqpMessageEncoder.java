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
package org.kaazing.gateway.service.amqp.amqp091.codec;

import static org.kaazing.gateway.service.amqp.amqp091.message.AmqpConnectionMessage.AMQP_AUTHENTICATION_MECHANISM;
import static org.kaazing.gateway.transport.bridge.CachingMessageEncoder.IO_MESSAGE_ENCODER;
import static org.kaazing.mina.core.buffer.IoBufferEx.FLAG_DIRECT;
import static org.kaazing.mina.core.buffer.IoBufferEx.FLAG_SHARED;
import static org.kaazing.mina.core.buffer.IoBufferEx.FLAG_ZERO_COPY;

import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecException;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.kaazing.gateway.service.amqp.amqp091.AmqpFrame;
import org.kaazing.gateway.service.amqp.amqp091.AmqpProperty;
import org.kaazing.gateway.service.amqp.amqp091.AmqpTable;
import org.kaazing.gateway.service.amqp.amqp091.AmqpTable.AmqpTableEntry;
import org.kaazing.gateway.service.amqp.amqp091.AmqpType;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpClassMessage;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpCloseMessage;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpCloseOkMessage;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpConnectionMessage;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpMessage;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpMessage.MessageKind;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpOpenMessage;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpOpenOkMessage;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpProtocolHeaderMessage;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpSecureMessage;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpSecureOkMessage;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpStartMessage;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpStartOkMessage;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpTuneMessage;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpTuneOkMessage;
import org.kaazing.gateway.transport.bridge.CachingMessageEncoder;
import org.kaazing.gateway.transport.bridge.MessageEncoder;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AmqpMessageEncoder extends ProtocolEncoderAdapter {
    private static final String CLASS_NAME = AmqpMessageEncoder.class.getName();
    private static final String SERVICE_AMQP_PROXY_LOGGER = "service.amqp.proxy";

    private static final Map<Character, AmqpType> typeIdentifierMap = 
                                             new HashMap<>();
    private static final Map<AmqpType, String> typeMap = 
                                             new HashMap<>();

    static {            
        typeIdentifierMap.put('F', AmqpType.FIELDTABLE);
        typeIdentifierMap.put('I', AmqpType.INT);
        typeIdentifierMap.put('L', AmqpType.LONGLONG);
        typeIdentifierMap.put('S', AmqpType.LONGSTRING);
        typeIdentifierMap.put('s', AmqpType.SHORTSTRING);
        typeIdentifierMap.put('T', AmqpType.TIMESTAMP);
        typeIdentifierMap.put('U', AmqpType.SHORT);
        typeIdentifierMap.put('V', AmqpType.VOID);

        typeMap.put(AmqpType.FIELDTABLE, "F");
        typeMap.put(AmqpType.INT, "I");
        typeMap.put(AmqpType.LONGLONG, "L");
        typeMap.put(AmqpType.LONGSTRING, "S");
        typeMap.put(AmqpType.SHORTSTRING, "s");
        typeMap.put(AmqpType.TIMESTAMP, "T");
        typeMap.put(AmqpType.SHORT, "U");
        typeMap.put(AmqpType.VOID, "V");
    }

    // private byte    bitCount = 0;

    private final IoBufferAllocatorEx<?>      allocator;
    private final CachingMessageEncoder       cachingEncoder;
    private final MessageEncoder<AmqpMessage> encoder;
    
    public AmqpMessageEncoder(IoBufferAllocatorEx<?> allocator) {
        this(IO_MESSAGE_ENCODER, allocator);
    }
    
    public AmqpMessageEncoder(CachingMessageEncoder  cachingEncoder,
                              IoBufferAllocatorEx<?> allocator) {
        this.cachingEncoder = cachingEncoder;
        this.allocator = allocator;
        this.encoder = new AmqpMessageEncoderImpl();
    }
    
    @Override
    public void encode(IoSession             session, 
                       Object                message,
                       ProtocolEncoderOutput out) throws Exception {
        assert (message != null) : "AMQP message is null";
        
        AmqpMessage amqpMessage = (AmqpMessage)message;
        IoBufferEx  buffer;
        
        if (amqpMessage.hasCache()) {
            buffer = cachingEncoder.encode(encoder, amqpMessage, allocator, FLAG_SHARED | FLAG_ZERO_COPY | FLAG_DIRECT);
        }
        else {
            buffer = encodeInternal(session, allocator, FLAG_ZERO_COPY, amqpMessage);
        }

        out.write(buffer);
    }

    // --------------------- Public Static Methods ---------------------------
    public static String encodeAuthAmqPlain(String username, char[] password) {
        Logger logger = LoggerFactory.getLogger(SERVICE_AMQP_PROXY_LOGGER);
        if (logger.isDebugEnabled()) {
            logger.debug(CLASS_NAME + ".encodeAuthAmqPlain(): Username - " + username);
        }

        int size =  1  +                   // LOGIN.length
                    5  +                   // LOGIN
                    1  +                   // type-id -- 'S' for LongString
                    4  +                   // username's length
                    username.length() +    // username
                    1  +                   // PASSWORD.length
                    8  +                   // PASSWORD
                    1  +                   // type-id -- 'S' for LongString
                    4  +                   // password's length
                    password.length;       // password
                    
        ByteBuffer buffer = ByteBuffer.allocate(size);

        putShortString(buffer, "LOGIN");
        putType(buffer, AmqpType.LONGSTRING);
        putLongString(buffer, username);

        putShortString(buffer, "PASSWORD");
        putType(buffer, AmqpType.LONGSTRING);
        putLongString(buffer, new String(password)); // ### TODO: Use char[] directly
        buffer.flip();
        
        // Nulling out the password for security. 
        Arrays.fill(password, '\0');

        int len = buffer.remaining();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < len; i++)
        {
            builder.append((char)(buffer.get() & 0xff)); 

        }
        String s = builder.toString();
        return s;
    }

    // -------------------- Private Encoding Methods -------------------------
    
    private static String encodeAuthPlain(String username, char[] password) {
        // PLAIN mechanism encoding - NUL + username + NUL + password
        int size = username.length() + password.length + 2;
        ByteBuffer buffer = ByteBuffer.allocate(size);
        buffer.put((byte)0);
        buffer.put(username.getBytes());
        buffer.put((byte)0);
        buffer.put(new String(password).getBytes());
        
        buffer.flip();
        
        // Nulling out the password for security. 
        Arrays.fill(password, '\0');

        int len = buffer.remaining();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < len; i++)
        {
            builder.append((char)(buffer.get() & 0xff)); 

        }
        String s = builder.toString();
        return s;

    }
    
    private static IoBufferEx encodeInternal(IoSession session,
                                             IoBufferAllocatorEx<?>  allocator,
                                             int                     flags, AmqpMessage             message) {
        MessageKind messageKind = message.getMessageKind();
        IoBufferEx  buffer;
        
        Logger logger = LoggerFactory.getLogger(SERVICE_AMQP_PROXY_LOGGER);
        if (logger.isDebugEnabled()) {
            logger.debug(CLASS_NAME + ".encode(): Message - " + message);
        }
        
        switch (messageKind) {
            case PROTOCOL_HEADER:
                buffer = encodeProtocolHeader(allocator,
                                              FLAG_ZERO_COPY,
                                              (AmqpProtocolHeaderMessage)message);
                break;

            case CLASS:
                buffer = encodeClass(session, 
                                     allocator, 
                                     FLAG_ZERO_COPY, (AmqpClassMessage)message);
                break;
                
            default:
                String s = "Unexpected AMQP message: " + messageKind;
                throw new IllegalStateException(s);
        }

        return buffer;
    }
    
    private static IoBufferEx encodeProtocolHeader(IoBufferAllocatorEx<?>    allocator,
                                                   int                       flags,
                                                   AmqpProtocolHeaderMessage message) {
        // Allocate 8 bytes for the protocol header which contains the following
        // bytes -- 'A', 'M', 'Q', 'P', '0', '0', '9', '1'.
        ByteBuffer allocated = allocator.allocate(8, flags);
        int        offset = allocated.position();
        
        allocated.put(message.getProtocolHeader());
        allocated.position(offset);
        
        Logger logger = LoggerFactory.getLogger(SERVICE_AMQP_PROXY_LOGGER);
        if (logger.isDebugEnabled()) {
            logger.debug(CLASS_NAME + ".encodeProtocolHeader(): " + getHexDump(allocated));
        }
        
        return allocator.wrap(allocated, flags);
    }
     
    private static IoBufferEx encodeClass(IoSession session,
                                          IoBufferAllocatorEx<?> allocator,
                                          int                    flags, AmqpClassMessage       message)
            throws IllegalStateException {        
        switch (message.getClassKind()) {
            case CONNECTION:
                return encodeConnection(session, 
                                        allocator,
                                        flags, (AmqpConnectionMessage) message);
                
            default:
                String s = "Unexpected AMQP class: " + message.getClassKind();
                throw new IllegalStateException(s);
        }        
    }

    private static IoBufferEx encodeConnection(IoSession session,
                                               IoBufferAllocatorEx<?> allocator,
                                               int                    flags, AmqpConnectionMessage  message) 
            throws IllegalStateException {
        switch (message.getMethodKind()) {
            case CLOSE:
                return encodeClose(allocator, flags, (AmqpCloseMessage)message);
                
            case CLOSE_OK:
                return encodeCloseOk(allocator, flags, (AmqpCloseOkMessage)message);

            case OPEN:
                return encodeOpen(allocator, flags, (AmqpOpenMessage)message);
                
            case OPEN_OK:
                return encodeOpenOk(allocator, flags, (AmqpOpenOkMessage)message);
                
            case SECURE:
                return encodeSecure(allocator, flags, (AmqpSecureMessage)message);

            case SECURE_OK:
                return encodeSecureOk(session, allocator, flags, (AmqpSecureOkMessage)message);

            case START:
                return encodeStart(allocator, flags, (AmqpStartMessage)message);
                
            case START_OK:
                return encodeStartOk(session, allocator, flags, (AmqpStartOkMessage)message);

            case TUNE:
                return encodeTune(allocator, flags, (AmqpTuneMessage)message);
                
            case TUNE_OK:
                return encodeTuneOk(allocator, flags, (AmqpTuneOkMessage)message);

            default:
                String s = "Unexpected AMQP Method Kind: " + message.getMethodKind();
                throw new IllegalStateException(s);
        }        
    }

    private static IoBufferEx encodeClose(IoBufferAllocatorEx<?> allocator,
                                          int                    flags,
                                          AmqpCloseMessage       message) 
            throws IllegalStateException {
        String     replyText = message.getReplyText();
        int        size = 1 +                    // frame-type
                          2 +                    // channel-id
                          4 +                    // payload-size
                          2 +                    // class-id
                          2 +                    // method-id
                          2 +                    // reply-code
                          1 +                    // size of reply-text
                          replyText.length() +   // len(<reply-text>)
                          2 +                    // close-class-id
                          2 +                    // close-method-id
                          1;                     // end-of-frame
        ByteBuffer allocated = allocator.allocate(size, flags);
        int        offset = allocated.position();
        int        payloadSize = size - 8;

        // Encode the frame-type and the channel-id.
        putUnsigned(allocated, AmqpFrame.METHOD.type());
        putUnsignedShort(allocated, (short) message.getChannelId());

        // Encode the payload size.
        putUnsignedInt(allocated, payloadSize);

        // Encode the class-id and the method-id.
        putUnsignedShort(allocated, message.getClassKind().classId());
        putUnsignedShort(allocated, message.getMethodKind().methodId());

        // Encode the parameters.
        putUnsignedShort(allocated, message.getReplyCode());
        putShortString(allocated, replyText);
        putUnsignedShort(allocated, message.getReasonClassId());
        putUnsignedShort(allocated, message.getReasonMethodId());

        // Encode the frame terminator.
        putUnsigned(allocated, AmqpClassMessage.FRAME_END);
        
        allocated.position(offset);
        
        Logger logger = LoggerFactory.getLogger(SERVICE_AMQP_PROXY_LOGGER);
        if (logger.isDebugEnabled()) {
            String s = ".encodeClose(): Raw Bytes - " + getHexDump(allocated);
            logger.debug(CLASS_NAME + s);
        }

        return allocator.wrap(allocated, flags);
    }
    
    private static IoBufferEx encodeCloseOk(IoBufferAllocatorEx<?> allocator,
                                            int                    flags,
                                            AmqpCloseOkMessage     message) 
            throws IllegalStateException {
        int        size = 1 +                    // frame-type
                          2 +                    // channel-id
                          4 +                    // payload-size
                          2 +                    // class-id
                          2 +                    // method-id
                          1;                     // end-of-frame

        ByteBuffer allocated = allocator.allocate(size, flags);
        int        offset = allocated.position();
        int        payloadSize = size - 8;

        // Encode the frame-type and the channel-id.
        putUnsigned(allocated, AmqpFrame.METHOD.type());
        putUnsignedShort(allocated, message.getChannelId());

        // Encode the payload size.
        putUnsignedInt(allocated, payloadSize);

        // Encode the class-id and the method-id.
        putUnsignedShort(allocated, message.getClassKind().classId());
        putUnsignedShort(allocated, message.getMethodKind().methodId());

        // No parameters to encode.

        // Encode the frame terminator.
        putUnsigned(allocated, AmqpClassMessage.FRAME_END);
        
        allocated.position(offset);
        
        Logger logger = LoggerFactory.getLogger(SERVICE_AMQP_PROXY_LOGGER);
        if (logger.isDebugEnabled()) {
            String s = ".encodeCloseOk(): Raw Bytes - " + getHexDump(allocated);
            logger.debug(CLASS_NAME + s);
        }

        return allocator.wrap(allocated, flags);
    }
    
    private static IoBufferEx encodeOpen(IoBufferAllocatorEx<?> allocator,
                                         int                    flags,
                                         AmqpOpenMessage        message) 
            throws IllegalStateException {
        String     virtualHost = message.getVirtualHost();
        String     reserved1 = message.getReserved1();
        int        size =  1 +                      // frame-type
                           2 +                      // channel-id
                           4 +                      // payload-size
                           2 +                      // class-id
                           2 +                      // method-id
                           1 +                      // size of virtual-host
                           virtualHost.length() +   // len([virtual-host]);
                           1 +                      // size of reserved1
                           reserved1.length() +     // len([reserved1]);
                           1 +                      // reserved2
                           1;                       // end-of-frame
        ByteBuffer allocated = allocator.allocate(size, flags);
        int        offset = allocated.position();
        int        payloadSize = size - 8;

        // Encode the frame-type and the channel-id.
        putUnsigned(allocated, AmqpFrame.METHOD.type());
        putUnsignedShort(allocated, message.getChannelId());

        // Encode the payload size.
        putUnsignedInt(allocated, payloadSize);

        // Encode the class-id and the method-id.
        putUnsignedShort(allocated, message.getClassKind().classId());
        putUnsignedShort(allocated, message.getMethodKind().methodId());

        // Encode the parameters.
        putShortString(allocated, virtualHost);
        putShortString(allocated, reserved1);
        putBit(allocated, message.getReserved2());

        // Encode the frame terminator.
        putUnsigned(allocated, AmqpClassMessage.FRAME_END);
        
        allocated.position(offset);
        
        Logger logger = LoggerFactory.getLogger(SERVICE_AMQP_PROXY_LOGGER);
        if (logger.isDebugEnabled()) {
            String s = ".encodeOpen(): Raw Bytes - " + getHexDump(allocated);
            logger.debug(CLASS_NAME + s);
        }

        return allocator.wrap(allocated, flags);
    }
 
    private static IoBufferEx encodeOpenOk(IoBufferAllocatorEx<?> allocator,
                                           int                    flags,
                                           AmqpOpenOkMessage      message) 
            throws IllegalStateException {
        String     reserved1 = message.getReserved1();
        int        size =  1 +                      // frame-type
                           2 +                      // channel-id
                           4 +                      // payload-size
                           2 +                      // class-id
                           2 +                      // method-id
                           1 +                      // size of reserved1
                           reserved1.length() +     // len([reserved1])
                           1;                       // end-of-frame
        ByteBuffer allocated = allocator.allocate(size, flags);
        int        offset = allocated.position();
        int        payloadSize = size - 8;

        // Encode the frame-type and the channel-id.
        putUnsigned(allocated, AmqpFrame.METHOD.type());
        putUnsignedShort(allocated, message.getChannelId());

        // Encode the payload size.
        putUnsignedInt(allocated, payloadSize);

        // Encode the class-id and the method-id.
        putUnsignedShort(allocated, message.getClassKind().classId());
        putUnsignedShort(allocated, message.getMethodKind().methodId());

        // Encode the parameters.
        putShortString(allocated, reserved1);

        // Encode the frame terminator.
        putUnsigned(allocated, AmqpClassMessage.FRAME_END);
        
        allocated.position(offset);
        
        Logger logger = LoggerFactory.getLogger(SERVICE_AMQP_PROXY_LOGGER);
        if (logger.isDebugEnabled()) {
            String s = ".encodeOpenOk(): Raw Bytes - " + getHexDump(allocated);
            logger.debug(CLASS_NAME + s);
        }

        return allocator.wrap(allocated, flags);
    }
    
    private static IoBufferEx encodeSecure(IoBufferAllocatorEx<?> allocator,
                                           int                    flags,
                                           AmqpSecureMessage      message) 
            throws IllegalStateException {
        String     challenge = message.getChallenge();
        int        size = 1 +                      // frame-type
                          2 +                      // channel-id
                          4 +                      // payload-size
                          2 +                      // class-id
                          2 +                      // method-id
                          4 +                      // size of challenge
                          challenge.length() +     // len([challenge])
                          1;                       // end-of-frame
        ByteBuffer allocated = allocator.allocate(size, flags);
        int        offset = allocated.position();
        int        payloadSize = size - 8;

        // Encode the frame-type and the channel-id.
        putUnsigned(allocated, AmqpFrame.METHOD.type());
        putUnsignedShort(allocated, message.getChannelId());

        // Encode the payload size.
        putUnsignedInt(allocated, payloadSize);

        // Encode the class-id and the method-id.
        putUnsignedShort(allocated, message.getClassKind().classId());
        putUnsignedShort(allocated, message.getMethodKind().methodId());

        // Encode the parameters.
        putLongString(allocated, challenge);

        // Encode the frame terminator.
        putUnsigned(allocated, AmqpClassMessage.FRAME_END);

        allocated.position(offset);
        
        Logger logger = LoggerFactory.getLogger(SERVICE_AMQP_PROXY_LOGGER);
        if (logger.isDebugEnabled()) {
            String s = ".encodeSecure(): Raw Bytes - " + getHexDump(allocated);
            logger.debug(CLASS_NAME + s);
        }

        return allocator.wrap(allocated, flags);
    }

    private static IoBufferEx encodeSecureOk(IoSession session,
                                             IoBufferAllocatorEx<?> allocator,
                                             int                    flags, AmqpSecureOkMessage    message)
            throws IllegalStateException {
        String     username = message.getUsername();
        char[]     password = message.getPassword();
        
        // The authentication mechanism corresponding to this security mechanism response is injected as a 
        // session attribute while decoding start-ok
        Object mechanismObj = session.getAttribute(AMQP_AUTHENTICATION_MECHANISM);
        if (mechanismObj == null) {
            throw new IllegalStateException("Missing session attribute - " + AMQP_AUTHENTICATION_MECHANISM);
        }
        String authMechanism = mechanismObj.toString();
        
        String response;
        
        if ("AMQPLAIN".equals(authMechanism)) {
            response = encodeAuthAmqPlain(username, password);
        }
        else if ("PLAIN".equalsIgnoreCase(authMechanism)) {
            response = encodeAuthPlain(username, password);
        }
        else {
            throw new IllegalStateException("Unsupported SASL authentication mechanism: " + authMechanism);
        }
        
        int        size = 1 +                      // frame-type
                          2 +                      // channel-id
                          4 +                      // payload-size
                          2 +                      // class-id
                          2 +                      // method-id
                          4 +                      // size of response
                          response.length() +      // len([response])
                          1;                       // end-of-frame

        ByteBuffer allocated = allocator.allocate(size, flags);
        int        offset = allocated.position();
        int        payloadSize = size - 8;

        // Encode the frame-type and the channel-id.
        putUnsigned(allocated, AmqpFrame.METHOD.type());
        putUnsignedShort(allocated, message.getChannelId());

        // Encode the payload size.
        putUnsignedInt(allocated, payloadSize);

        // Encode the class-id and the method-id.
        putUnsignedShort(allocated, message.getClassKind().classId());
        putUnsignedShort(allocated, message.getMethodKind().methodId());

        // Encode the parameters.
        putLongString(allocated, response);

        // Encode the frame terminator.
        putUnsigned(allocated, AmqpClassMessage.FRAME_END);
        
        allocated.position(offset);
        
        /*
        // Since the raw bytes will contain the password, we are not logging
        // the buffer. It can be used during debugging though.
        Logger logger = LoggerFactory.getLogger(SERVICE_AMQP_PROXY_LOGGER);
        if (logger.isDebugEnabled()) {
            String s = ".encodeSecureOk(): Raw Bytes - " + getHexDump(allocated);
            logger.debug(CLASS_NAME + s);
        }
        */

        return allocator.wrap(allocated, flags);
    }

    private static IoBufferEx encodeStart(IoBufferAllocatorEx<?> allocator,
                                          int                    flags,
                                          AmqpStartMessage       message) 
            throws IllegalStateException {
        AmqpTable  serverProps = message.getServerProperties();
        String     mechanisms = message.getSecurityMechanisms();
        String     locales = message.getLocales();
        int        size =  1 +                       // frame-type
                           2 +                       // channel-id
                           4 +                       // payload-size
                           2 +                       // class-id
                           2 +                       // method-id
                           1 +                       // version-major
                           1 +                       // version-minor
                           serverProps.getLength() + // len([server-properties])
                           4 +                       // size of mechanisms
                           mechanisms.length() +     // len[(mechanisms])
                           4 +                       // size of locales
                           locales.length() +        // len([locales])
                           1;                        // end-of-frame
        ByteBuffer allocated = allocator.allocate(size, flags);
        int        offset = allocated.position();
        int        payloadSize = size - 8;

        // Encode the frame-type and the channel-id.
        putUnsigned(allocated, AmqpFrame.METHOD.type());
        putUnsignedShort(allocated, message.getChannelId());

        // Encode the payload size.
        putUnsignedInt(allocated, payloadSize);

        // Encode the class-id and the method-id.
        putUnsignedShort(allocated, message.getClassKind().classId());
        putUnsignedShort(allocated, message.getMethodKind().methodId());

        // Encode the parameters.
        putUnsigned(allocated, message.getVersionMajor());
        putUnsigned(allocated, message.getVersionMinor());
        putTable(allocated, serverProps);
        putLongString(allocated, mechanisms);
        putLongString(allocated, locales);

        // Encode the frame terminator.
        putUnsigned(allocated, AmqpClassMessage.FRAME_END);

        allocated.position(offset);

        Logger logger = LoggerFactory.getLogger(SERVICE_AMQP_PROXY_LOGGER);
        if (logger.isDebugEnabled()) {
            String s = ".encodeStart(): Raw Bytes - " + getHexDump(allocated);
            logger.debug(CLASS_NAME + s);
        }

        return allocator.wrap(allocated, flags);
    }

    private static IoBufferEx encodeStartOk(IoSession session,
                                            IoBufferAllocatorEx<?> allocator,
                                            int                    flags, AmqpStartOkMessage     message) 
            throws IllegalStateException {
        AmqpTable  clientProps = message.getClientProperties();
        String     mechanism = message.getSecurityMechanism();
        String     username = message.getUsername();
        char[]     password = message.getPassword();
        
        String response;
        
        if ("AMQPLAIN".equals(mechanism)) {
            response = encodeAuthAmqPlain(username, password);
        }
        else if ("PLAIN".equalsIgnoreCase(mechanism)) {
            response = encodeAuthPlain(username, password);
        }
        else {
            throw new IllegalStateException("Unsupported SASL authentication mechanism: " + mechanism);
        }
        
        // Inject the SASL authentication mechanism as a session attribute
        // The mechanism selected by client is needed later for secure-ok
        session.setAttribute(AMQP_AUTHENTICATION_MECHANISM, mechanism);
        
        String     locale = message.getLocale();
        int        size = 1 +                        // frame-type
                          2 +                        // channel-id
                          4 +                        // payload-size
                          2 +                        // class-id
                          2 +                        // method-id
                          clientProps.getLength() +  // len([client-properties])
                          1 +                        // size of mechanism
                          mechanism.length() +       // len[(mechanism])
                          4 +                        // size of response
                          response.length() +        // len([response])
                          1 +                        // size of locale
                          locale.length() +          // len([locale])
                          1;                         // end-of-frame
        ByteBuffer allocated = allocator.allocate(size, flags);
        int        offset = allocated.position();
        int        payloadSize = size - 8;

        // Encode the frame-type and the channel-id.
        putUnsigned(allocated, AmqpFrame.METHOD.type());
        putUnsignedShort(allocated, message.getChannelId());

        // Encode the payload size.
        putUnsignedInt(allocated, payloadSize);

        // Encode the class-id and the method-id.
        putUnsignedShort(allocated, message.getClassKind().classId());
        putUnsignedShort(allocated, message.getMethodKind().methodId());

        // Encode the parameters.
        putTable(allocated, clientProps);
        putShortString(allocated, mechanism);
        putLongString(allocated, response);
        putShortString(allocated, locale);
        
        // Encode the frame terminator.
        putUnsigned(allocated, AmqpClassMessage.FRAME_END);

        allocated.position(offset);
        
        /*
        // Since the raw bytes will contain the password, we are not logging
        // the buffer. It can be used during debugging though.
        Logger logger = LoggerFactory.getLogger(SERVICE_AMQP_PROXY_LOGGER);
        if (logger.isDebugEnabled()) {
            String s = ".encodeStartOk(): Raw Bytes - " + getHexDump(allocated);
            logger.debug(CLASS_NAME + s);
        }
        */

        return allocator.wrap(allocated, flags);
    }

    private static IoBufferEx encodeTune(IoBufferAllocatorEx<?> allocator,
                                         int                    flags,
                                         AmqpTuneMessage        message) 
            throws IllegalStateException {
        int        size = 1 +        // frame-type
                          2 +        // channel-id
                          4 +        // payload-size
                          2 +        // class-id
                          2 +        // method-id
                          2 +        // max-channels
                          4 +        // max-frame-size
                          2 +        // heartbeat-delay
                          1;         // end-of-frame
        ByteBuffer allocated = allocator.allocate(size, flags);
        int        offset = allocated.position();
        int        payloadSize = size - 8;

        // Encode the frame-type and the channel-id.
        putUnsigned(allocated, AmqpFrame.METHOD.type());
        putUnsignedShort(allocated, message.getChannelId());

        // Encode the payload size.
        putUnsignedInt(allocated, payloadSize);
        
        // Encode the class-id and the method-id.
        putUnsignedShort(allocated, message.getClassKind().classId());
        putUnsignedShort(allocated, message.getMethodKind().methodId());

        // Encode the parameters.
        putUnsignedShort(allocated, (short)message.getMaxChannels());
        allocated.putInt(message.getMaxFrameSize());
        putUnsignedShort(allocated, (short)message.getHeartbeatDelay());

        // Encode the frame terminator.
        putUnsigned(allocated, AmqpClassMessage.FRAME_END);

        allocated.position(offset);
        
        Logger logger = LoggerFactory.getLogger(SERVICE_AMQP_PROXY_LOGGER);
        if (logger.isDebugEnabled()) {
            String s = ".encodeTune(): Raw Bytes - " + getHexDump(allocated);
            logger.debug(CLASS_NAME + s);
        }

        return allocator.wrap(allocated, flags);
    }    
    
    private static IoBufferEx encodeTuneOk(IoBufferAllocatorEx<?> allocator,
                                           int                    flags,
                                           AmqpTuneOkMessage      message) 
            throws IllegalStateException {
        int        size = 1 +       // frame-type
                          2 +       // channel-id
                          4 +       // payload-size
                          2 +       // class-id
                          2 +       // method-id
                          2 +       // max-channels
                          4 +       // max-frame-size
                          2 +       // heartbeat
                          1;        // end-of-frame
        
        ByteBuffer allocated = allocator.allocate(size, flags);
        int        offset = allocated.position();
        int        payloadSize = size - 8;

        // Encode the frame-type and the channel-id.
        putUnsigned(allocated, AmqpFrame.METHOD.type());
        putUnsignedShort(allocated, message.getChannelId());

        // Encode the payload size.
        putUnsignedInt(allocated, payloadSize);

        // Encode the class-id and the method-id.
        putUnsignedShort(allocated, message.getClassKind().classId());
        putUnsignedShort(allocated, message.getMethodKind().methodId());

        // Encode the parameters.
        putUnsignedShort(allocated, (short)message.getMaxChannels());
        allocated.putInt(message.getMaxFrameSize());
        putUnsignedShort(allocated, (short)message.getHeartbeatDelay());

        // Encode the frame terminator.
        putUnsigned(allocated, AmqpClassMessage.FRAME_END);

        allocated.position(offset);
        
        Logger logger = LoggerFactory.getLogger(SERVICE_AMQP_PROXY_LOGGER);
        if (logger.isDebugEnabled()) {
            String s = ".encodeTuneOk(): Raw Bytes - " + getHexDump(allocated);
            logger.debug(CLASS_NAME + s);
        }

        return allocator.wrap(allocated, flags);
    }
    
    private static String getHexDump(ByteBuffer buffer) {
        if (buffer.position() == buffer.limit()) {
            return "empty";
        }
        
        StringBuilder hexDump = new StringBuilder();
        for (int i = buffer.position() + buffer.arrayOffset(); i < buffer.limit(); i++) {
            hexDump.append(Integer.toHexString(buffer.get(i)&0xFF)).append(' ');
        }
                
        return hexDump.toString();
    }


    private static ByteBuffer putBit(ByteBuffer buffer, Boolean v) {     
        // Packs one (of possibly several) boolean values as bits into a single
        // 8-bit field.
        // 
        // If the last value written was a bit, the buffer's bit flag is false.
        // If the buffer's bit flag is set, putBit will try to pack the current bit
        // value into the same byte.
        byte value = 0;
        if (v) {
            value = 1;
        }

        putUnsigned(buffer, value);

        /*
        if (this.bitCount > 0) {
            byte lastByte = buffer.get(buffer.position() - 1);
            lastByte = (byte)((value << this.bitCount) | lastByte);
            buffer.put(buffer.position() - 1, lastByte);
        }
        else {
            putUnsigned(buffer, value);
        }
        */
        return buffer;
    }
    
    private static ByteBuffer putMilliseconds(ByteBuffer buffer, long millis) {
        return buffer.putLong(millis);
    }

    private static void putFieldValue(ByteBuffer buffer, Object value) 
            throws IllegalStateException {
        if (value == null) {
            return;
        }
        
        if (!(value instanceof Boolean)) {
            String s = "Encoding error: Cannot encode field value";
            throw new IllegalStateException(s);
        }

        byte b = (byte) (((Boolean) value) ? 1 : 0);
        
        putUnsigned(buffer, 't');
        putUnsigned(buffer, b);
    }

    private static void putFieldTable(ByteBuffer         buffer, 
                                      Map<String,Object> fieldTable) 
            throws IllegalStateException {
        if ((fieldTable == null) || fieldTable.isEmpty()) {
            putUnsignedInt(buffer, 0);
            return;
        }
        
        int len = 0;        
        for (Map.Entry<String, Object> pair : fieldTable.entrySet()) {
            String key = pair.getKey();
            
            len += 1;                // size of the key
            len += key.length();     // key itself
            
            len += 1;                // field type
            len += 1;                // field value -- 1 for true, 0 for false
        }
        
        putUnsignedInt(buffer, len);

        for (Map.Entry<String, Object> pair : fieldTable.entrySet()) {
            String key = pair.getKey();
            Object value = pair.getValue(); // Expecting this to be Boolean
            
            putShortString(buffer, key);
            putFieldValue(buffer, value);
        }
    }

    private static void putTable(ByteBuffer buffer, AmqpTable table) 
            throws IllegalStateException {       
        if (table == null) {            
            putUnsignedInt(buffer, 0);               
        }
        else {            
            List<AmqpTableEntry> entries = table.getEntries();
            AmqpTableEntry       entry;
            ByteBuffer           bytes = ByteBuffer.allocate(table.getLength());

            for (AmqpTableEntry entry1 : entries) {
                entry = entry1;

                putShortString(bytes, entry.getKey());
                putType(bytes, entry.getType());
                putObjectOfType(bytes, entry.getType(), entry.getValue());
            }

            bytes.flip();
            putUnsignedInt(buffer, bytes.remaining());
            buffer.put(bytes);
        }
    }

    private static void putLongString(ByteBuffer buffer, String s) {
        putUnsignedInt(buffer, s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            byte charCode = (byte)c;

            putUnsigned(buffer, charCode);
        }
    }

    private static void putShortString(ByteBuffer buffer, String s) {
        putUnsigned(buffer, (byte)s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            byte charCode = (byte)c;
            putUnsigned(buffer, charCode);
        }
    }

    private static void putType(ByteBuffer buffer, AmqpType type) {
        String ti = typeMap.get(type);
        char c = ti.charAt(0);
        byte charCode = (byte)c;
        putUnsigned(buffer, charCode);
    }

    private static ByteBuffer putUnsigned(ByteBuffer buffer, int v) {
        byte b = (byte)(v & 0xFF);
        return buffer.put(b);
    }
    
    private static ByteBuffer putUnsignedShort(ByteBuffer buffer, int v) {
        buffer.putShort((short)(v & 0xFFFF));
        return buffer;
    }

    private static ByteBuffer putUnsignedShortAt(ByteBuffer buffer, int atPosition, int v) {
        buffer.putShort(atPosition, (short)(v & 0xFFFF));
        return buffer;
    }
    
    public static ByteBuffer putUnsignedInt(ByteBuffer buffer, long value) {
        buffer.putInt((int)value & 0xFFFFFFFF);
        return buffer;
    }

    // Puts an unsigned long.
    private static ByteBuffer putUnsignedLong(ByteBuffer buffer, long v) {
        buffer.putInt(0);
        return putUnsignedInt(buffer, (int)v);
    }

    private static void putObjectOfType(ByteBuffer buffer, 
                                        AmqpType   type, 
                                        Object     value) 
            throws IllegalStateException {
        switch (type) {
            case BIT:
                putBit(buffer, (Boolean)value);
                break;
            case SHORTSTRING:
                putShortString(buffer, (String)value);
                break;
            case LONGSTRING:
                putLongString(buffer, (String)value);
                break;
            case FIELDTABLE:
                putFieldTable(buffer, (Map<String, Object>)value);
                break;
            case TABLE:
                putTable(buffer, (AmqpTable)value);
                break;
            case INT:
                buffer.putInt((Integer)value);
                break;
            case UNSIGNEDINT:
                putUnsignedInt(buffer, (Integer)value);
                break;
            case UNSIGNEDSHORT:
                putUnsignedShort(buffer, (Short)value);
                break;
            case UNSIGNED:
                putUnsigned(buffer, (Byte)value);
                break;
            case SHORT:
                int channelmax = (Integer)value;
                putUnsignedShort(buffer, (short)channelmax);
                break;
            case LONG:
                putUnsignedInt(buffer, (Integer)value);
                break;
            case LONGLONG:
                int val = (Integer)value;
                putUnsignedLong(buffer, (long)val);
                break;
            case TIMESTAMP:
                long millis = ((Timestamp)value).getTime();
                putMilliseconds(buffer, millis);
                break;
            case VOID:
                // We do not encode the null value on the wire. The type 'V'
                // indicates a null value.
                break;
            default:
                String s = "Invalid type: '" + type + "' not found in _typeCodeMap";
                throw new IllegalStateException(s);
        }           
    }
    
    @SuppressWarnings("unused")
    private static ByteBuffer putContentProperties(ByteBuffer buffer,
                                                   SortedMap<AmqpProperty, Object> contentProperties) 
            throws ProtocolCodecException {

        if (contentProperties.size() == 0) {
            putUnsignedShort(buffer, (short) 0);
            return buffer;
        }
        
        int packedPropertyFlagsAt = buffer.position();
        
        // fill in flags after iterating properties below. Skipped 2 bytes will
        // be filled in later.
        buffer.position(packedPropertyFlagsAt + 2);  

        short packedPropertyFlags = 0x00;
        for (Map.Entry<AmqpProperty, Object> entry : contentProperties.entrySet()) {
            AmqpProperty property = entry.getKey();
            Object propertyValue = entry.getValue();
            packedPropertyFlags |= (1 << property.ordinal());

            AmqpType   propertyType = property.domain().type();
            putObjectOfType(buffer, propertyType, propertyValue);
        }

        packedPropertyFlags <<= 2; // ???
        putUnsignedShortAt(buffer, packedPropertyFlagsAt, packedPropertyFlags);

        return buffer;
    }
    
    private static final class AmqpMessageEncoderImpl implements MessageEncoder<AmqpMessage> {
        @Override
        public IoBufferEx encode(IoBufferAllocatorEx<?> allocator, AmqpMessage message, int flags) {
            return encodeInternal(null, allocator, flags, message);
        }
    }
}
