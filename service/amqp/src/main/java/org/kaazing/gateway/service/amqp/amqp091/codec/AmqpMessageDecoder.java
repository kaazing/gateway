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
import static org.kaazing.gateway.service.amqp.amqp091.message.AmqpProtocolHeaderMessage.PROTOCOL_0_9_1_DEFAULT_HEADER;

import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecException;
import org.apache.mina.filter.codec.ProtocolDecoderException;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.kaazing.gateway.service.amqp.amqp091.AmqpFrame;
import org.kaazing.gateway.service.amqp.amqp091.AmqpProperty;
import org.kaazing.gateway.service.amqp.amqp091.AmqpTable;
import org.kaazing.gateway.service.amqp.amqp091.AmqpTable.AmqpTableEntry;
import org.kaazing.gateway.service.amqp.amqp091.AmqpType;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpClassMessage;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpClassMessage.ClassKind;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpCloseMessage;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpCloseOkMessage;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpConnectionMessage;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpConnectionMessage.ConnectionMethodKind;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpOpenMessage;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpOpenOkMessage;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpProtocolHeaderMessage;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpSecureMessage;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpSecureOkMessage;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpStartMessage;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpStartOkMessage;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpTuneMessage;
import org.kaazing.gateway.service.amqp.amqp091.message.AmqpTuneOkMessage;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.filter.codec.CumulativeProtocolDecoderEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AmqpMessageDecoder extends CumulativeProtocolDecoderEx {
    private static final String CLASS_NAME = AmqpMessageDecoder.class.getName();
    private static final String SERVICE_AMQP_PROXY_LOGGER = "service.amqp.proxy";
    private static final Map<Character, AmqpType> typeIdentifierMap = 
                                             new HashMap<>();
    private static final Map<AmqpType, String> typeMap = 
                                             new HashMap<>();

    enum DecoderState {
        READ_PROTOCOL_HEADER, READ_FRAME, AFTER_CONNECTION
    }

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

    private DecoderState currentState;

    public AmqpMessageDecoder(IoBufferAllocatorEx<?> allocator, boolean client) {
        this(allocator, client, null);
    }

    public AmqpMessageDecoder(IoBufferAllocatorEx<?> allocator, 
                              boolean                client, 
                              DecoderState           initial) {
        this(allocator, (initial != null) ? initial :
                                            client ? DecoderState.READ_FRAME : 
                                                     DecoderState.READ_PROTOCOL_HEADER);
    }
    
    AmqpMessageDecoder(IoBufferAllocatorEx<?> allocator, DecoderState initialState) {
        super(allocator);

        this.currentState = initialState;
        
        Logger logger = LoggerFactory.getLogger(SERVICE_AMQP_PROXY_LOGGER);
        if (logger.isDebugEnabled()) {
            String s = ".AmqpMessageDecoder(): Initial State = " + initialState;
            logger.debug(CLASS_NAME + s);
        }
    }
    
    @Override
    protected boolean doDecode(IoSession session, IoBufferEx in,
            ProtocolDecoderOutput out) throws Exception {
        Logger logger = LoggerFactory.getLogger(SERVICE_AMQP_PROXY_LOGGER);
        if (logger.isDebugEnabled()) {
            String s = ".doDecode(): Current State - " + currentState;
            logger.debug(CLASS_NAME + s);
        }

        switch (currentState) {
            case READ_PROTOCOL_HEADER:
                return decodeProtocolHeader(session, in, out);
                
            case READ_FRAME:
                return decodeFrame(session, in, out);

            case AFTER_CONNECTION:
                out.write(in.duplicate());
                in.skip(in.remaining());
                session.getFilterChain().remove(AmqpCodecFilter.NAME);
                return true;
                
            default:
                return false;
        }
    }
    
    private boolean decodeProtocolHeader(IoSession session, IoBufferEx in,
            ProtocolDecoderOutput out) throws Exception {
        Logger logger = LoggerFactory.getLogger(SERVICE_AMQP_PROXY_LOGGER);

        if (in.remaining() < 8) {
            if (logger.isDebugEnabled()) {
                String s = ".decodeProtocolHeader(): Not enough bytes to decode " +
                           "AMQP 0.9.1 protocol header - " + getHexDump(in);
                logger.debug(CLASS_NAME + s);
            }
            
            return false;
        }
        
        if (in.remaining() > 8) {
            // ### TODO: Perhaps, we should just consume 8bytes and not worry
            //           about throwing an exception. For time being, let's be
            //           conservative.
            if (logger.isDebugEnabled()) {
                String s = ".decodeProtocolHeader(): Too many bytes to decode " +
                           "AMQP 0.9.1 protocol header - " + getHexDump(in);
                logger.debug(CLASS_NAME + s);
            }
            throw new ProtocolCodecException("Invalid AMQP 0.9.1 Protocol Header");
        }
        
        byte[] bytes = new byte[in.remaining()];
        
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = in.get();
        }
        
        if (!Arrays.equals(PROTOCOL_0_9_1_DEFAULT_HEADER, bytes)) {
            String s = "Invalid AMQP 0.9.1 Protocol Header: " + getHexDump(bytes);
            throw new ProtocolCodecException(s);
        }

        AmqpProtocolHeaderMessage message = new AmqpProtocolHeaderMessage();
        message.setProtocolHeader(PROTOCOL_0_9_1_DEFAULT_HEADER);
        
        out.write(message);

        if (logger.isDebugEnabled()) {
            String s = ".decodeProtocolHeader(): AMQP 0.9.1 Protocol Header " +
                       "successfully decoded";
            logger.debug(CLASS_NAME + s);
        }
        
        currentState = DecoderState.READ_FRAME;
        
        if (logger.isDebugEnabled()) {
            String s = ".decodeFrame(): Transitioning to READ_FRAME state";
            logger.debug(CLASS_NAME + s);
        }
        
        return true;
    }
    
    private boolean decodeFrame(IoSession session, IoBufferEx in,
            ProtocolDecoderOutput out) throws Exception {
        Logger logger = LoggerFactory.getLogger(SERVICE_AMQP_PROXY_LOGGER);

        if (in.remaining() < 7) {
            if (logger.isDebugEnabled()) {
                String s = ".decodeFrame(): Cannot decode a frame without the " +
                           "first seven bytes";
                logger.debug(CLASS_NAME + s);
            }
            return false;
        }
        
        /*
        // This can be used to log the raw bytes for the entire frame for
        // all the messages. But, we don't want the logs to contain even raw 
        // bytes for the password. That's why we invoke the logger in individual
        // decode methods appropriately. For START_OK and and SECURE_OK, we will
        // make sure that error.log does not contain even the raw password 
        // bytes. This code can be used for debugging purposes.
        if (logger.isDebugEnabled()) {
            String s = ".decodeFrame(): Raw bytes - " + getHexDump(in);
            logger.debug(CLASS_NAME + s);
        }
        */

        // Mark the buffer's position. This way, we can easily reset.
        in.mark();
        
        // Read the first seven bytes to get the size of the payload.
        byte      frameType = (byte)in.getUnsigned();
        AmqpFrame frameKind = AmqpFrame.get(frameType);
        short     channelId = (short) getUnsignedShort(in);    
        long      payloadSize =  getUnsignedInt(in);
        
        // 8 bytes are used for constructing the frame around the payload --
        // frame-type    (1) + 
        // channel-id    (2) + 
        // payload-size  (4) + len([payload]) + end-of-frame (1)
        if (in.remaining() < payloadSize + 1) {
            // The frame is not complete yet. Reset the buffer to the previously
            // marked position and return false so that we called again when 
            // more data arrives.
            in.reset();
            
            if (logger.isDebugEnabled()) {
                String s = ".decodeFrame(): Fragmentation may have resulted " +
                           "in an incomplete frame - " + getHexDump(in);
                logger.debug(CLASS_NAME + s);
            }
            
            return false;
        }
        
        AmqpClassMessage message;
        short            classIndex = (short) getUnsignedShort(in);
        ClassKind        classKind = ClassKind.get(classIndex);
        
        switch (classKind) {
            case CONNECTION:
                assert(frameKind == AmqpFrame.METHOD);
                message = decodeConnection(session, in);
                message.setChannelId(channelId);
                break;
                
            default:
                String s = "Unhandled class kind - " + classKind;
                throw new ProtocolDecoderException(s);
        }
        
        byte frameEnd = (byte) in.getUnsigned();                
        if ((frameEnd & 0xFF) != AmqpClassMessage.FRAME_END)
        {
            String s = Integer.toHexString(frameEnd & 0xFF);
            throw new ProtocolCodecException("Invalid end of AMQP Frame - " + s);
        }

        out.write(message);

        if (logger.isDebugEnabled()) {
            logger.debug(CLASS_NAME + ".decodeFrame(): Message - " + message);
        }

        switch (classKind) {
            case CONNECTION:
                AmqpConnectionMessage connection = (AmqpConnectionMessage) message;
                switch (connection.getMethodKind()) {
                    case OPEN:
                    case OPEN_OK:
                        currentState = DecoderState.AFTER_CONNECTION;
                        
                        if (logger.isDebugEnabled()) {
                            String s = ".decodeFrame(): Transitioning to AFTER_CONNECTION state";
                            logger.debug(CLASS_NAME + s);
                        }
                        break;
                }
                break;
        }

        return true;
    }
 
    // ------------------ Private Methods -----------------------------------
    
    // Returns an array of length 2. The 0th element contains the userid and
    // the 1st element is the password.
    private static String[] decodeAuthAmqPlain(String response) {
        Logger logger = LoggerFactory.getLogger(SERVICE_AMQP_PROXY_LOGGER);
        
        String[] credentials = null;

        if ((response != null) && (response.trim().length() > 0)) {
            ByteBuffer buffer = ByteBuffer.wrap(response.getBytes());
            
            @SuppressWarnings("unused")
            String loginKey = getShortString(buffer);
            @SuppressWarnings("unused")
            AmqpType ltype = getType(buffer);
            String username = getLongString(buffer);
            
            @SuppressWarnings("unused")
            String passwordKey = getShortString(buffer);
            @SuppressWarnings("unused")
            AmqpType ptype = getType(buffer);
            String password = getLongString(buffer);

            if (logger.isDebugEnabled()) {
                String s = ".decodeAuthAmqPlain(): Username = " + username;
                logger.debug(CLASS_NAME + s);
            }

            credentials = new String[] { username, password };
        }
        
        return credentials;
    }
    
    private static String[] decodeAuthPlain(String response) {
        String[] credentials = null;
        
        if ((response != null) && (response.trim().length() > 0)) {
            // PLAIN mechanism response - NUL + username + NUL + password
            int firstIndexOfNULChar = response.indexOf('\0');
            int secondIndexOfNULChar = response.indexOf('\0', firstIndexOfNULChar + 1);
            String username = response.substring(firstIndexOfNULChar + 1, secondIndexOfNULChar);
            String password = response.substring(secondIndexOfNULChar + 1);
            credentials = new String[] {username, password};
        }
        
        return credentials;
    }
    
    private static AmqpConnectionMessage decodeConnection(IoSession session, IoBufferEx in) 
            throws ProtocolDecoderException {
        AmqpConnectionMessage message;
        short                 methodId = (short) getUnsignedShort(in);
        ConnectionMethodKind  methodKind = ConnectionMethodKind.get(methodId);

        switch (methodKind) {
            case CLOSE:
                message = decodeClose(in);
                break;
                
            case CLOSE_OK:
                message = decodeCloseOk(in);
                break;
                
            case OPEN:
                message = decodeOpen(in);
                break;
                
            case OPEN_OK:
                message = decodeOpenOk(in);
                break;
                
            case SECURE:
                message = decodeSecure(in);
                break;
                
            case SECURE_OK:
                message = decodeSecureOk(session, in);
                break;
                
            case START:
                message = decodeStart(in);
                break;
                
            case START_OK:
                message = decodeStartOk(session, in);
                break;
                
            case TUNE:
                message = decodeTune(in);
                break;
                
            case TUNE_OK:
                message = decodeTuneOk(in);
                break;
                
            default:
                String s = "Invalid method index: " + methodId;
                throw new ProtocolDecoderException(s);
        }

        return message;
    }
    
    private static AmqpCloseMessage decodeClose(IoBufferEx in) {
        Logger logger = LoggerFactory.getLogger(SERVICE_AMQP_PROXY_LOGGER);
        AmqpCloseMessage message = new AmqpCloseMessage();

        if (logger.isDebugEnabled()) {
            String s = ".decodeClose(): Raw bytes - " + getHexDump(in);
            logger.debug(CLASS_NAME + s);
        }

        // Decode the parameters.
        int    replyCode = getUnsignedShort(in);
        String replyText = getShortString(in);
        int    classId = getUnsignedShort(in);
        int    methodId = getUnsignedShort(in);

        message.setReplyCode(replyCode);
        message.setReplyText(replyText);

        ClassKind classKind = (classId == 0) ? null : 
                                               ClassKind.get((short)classId);
        message.setReasonClassKind(classKind);
        message.setReasonMethodId(methodId);
        
        if (logger.isDebugEnabled()) {
            logger.debug(CLASS_NAME + ".decodeClose(): " + message);
        }
        return message;
    }
    
    private static AmqpCloseOkMessage decodeCloseOk(IoBufferEx in) {
        return new AmqpCloseOkMessage();
    }

    private static AmqpOpenMessage decodeOpen(IoBufferEx in) {
        Logger logger = LoggerFactory.getLogger(SERVICE_AMQP_PROXY_LOGGER);
        AmqpOpenMessage message = new AmqpOpenMessage();

        if (logger.isDebugEnabled()) {
            String s = ".decodeOpen(): Raw bytes - " + getHexDump(in);
            logger.debug(CLASS_NAME + s);
        }

        // Decode the parameters.
        String virtualHost = getShortString(in);
        String reserved1 = getShortString(in);
        int    reserved2 = getBit(in);
        
        message.setVirtualHost(virtualHost);
        message.setReserved1(reserved1);
        message.setReserved2((reserved2 > 0) ? Boolean.TRUE : Boolean.FALSE);

        if (logger.isDebugEnabled()) {
            logger.debug(CLASS_NAME + ".decodeOpen(): " + message);
        }

        return message;
    }

    private static AmqpOpenOkMessage decodeOpenOk(IoBufferEx in) {
        Logger logger = LoggerFactory.getLogger(SERVICE_AMQP_PROXY_LOGGER);

        if (logger.isDebugEnabled()) {
            String s = ".decodeOpenOk(): Raw bytes - " + getHexDump(in);
            logger.debug(CLASS_NAME + s);
        }

        AmqpOpenOkMessage message = new AmqpOpenOkMessage();

        // Decode the parameter(s).
        String reserved1 = getShortString(in);

        message.setReserved1(reserved1);

        if (logger.isDebugEnabled()) {
            logger.debug(CLASS_NAME + ".decodeOpenOk(): " + message);
        }

        return message;
    }

    private static AmqpSecureMessage decodeSecure(IoBufferEx in) {
        Logger logger = LoggerFactory.getLogger(SERVICE_AMQP_PROXY_LOGGER);

        if (logger.isDebugEnabled()) {
            String s = ".decodeSecure(): Raw bytes - " + getHexDump(in);
            logger.debug(CLASS_NAME + s);
        }

        AmqpSecureMessage message = new AmqpSecureMessage();
        
        // Decode the parameter(s).
        String challenge = getLongString(in);

        message.setChallenge(challenge);

        if (logger.isDebugEnabled()) {
            logger.debug(CLASS_NAME + ".decodeSecure(): " + message);
        }

        return message;
    }

    private static AmqpSecureOkMessage decodeSecureOk(IoSession session, IoBufferEx in) {
        // Do not log the raw bytes as it can contain the password.

        AmqpSecureOkMessage message = new AmqpSecureOkMessage();

        // Decode the parameter(s).
        String    response = getLongString(in);
        
        // The authentication mechanism corresponding to this security mechanism response is injected as a 
        // session attribute while decoding start-ok
        Object mechanismObj = session.getAttribute(AMQP_AUTHENTICATION_MECHANISM);
        if (mechanismObj == null) {
            throw new IllegalStateException("Missing session attribute - " + AMQP_AUTHENTICATION_MECHANISM);
        }
        String authMechanism = mechanismObj.toString();
        String[] credentials = null;
        if ("AMQPLAIN".equals(authMechanism)) {
            credentials = decodeAuthAmqPlain(response);
        }
        else if ("PLAIN".equalsIgnoreCase(authMechanism)) {
            credentials = decodeAuthPlain(response);
        }

        if (credentials != null) {
            String username = credentials[0];
            char[] password = credentials[1].toCharArray();
            
            message.setUsername(username);
            message.setPassword(password);
            
            Logger logger = LoggerFactory.getLogger(SERVICE_AMQP_PROXY_LOGGER);
            if (logger.isDebugEnabled()) {
                logger.debug(CLASS_NAME + ".decodeSecureOk(): " + message);
            }
        }

        return message;
    }

    private static AmqpStartMessage decodeStart(IoBufferEx in) 
            throws ProtocolDecoderException {
        Logger logger = LoggerFactory.getLogger(SERVICE_AMQP_PROXY_LOGGER);

        if (logger.isDebugEnabled()) {
            String s = ".decodeStart(): Raw bytes - " + getHexDump(in);
            logger.debug(CLASS_NAME + s);
        }

        AmqpStartMessage message = new AmqpStartMessage();
        
        // Decode the parameters.
        short     versionMajor = in.getUnsigned();
        short     versionMinor = in.getUnsigned();
        AmqpTable serverProperties = getTable(in);
        String    mechanisms = getLongString(in);
        String    locales = getLongString(in);
        
        message.setVersionMajor((byte)versionMajor);
        message.setVersionMinor((byte)versionMinor);
        message.setServerProperties(serverProperties);
        message.setSecurityMechanisms(mechanisms);
        message.setLocales(locales);

        if (logger.isDebugEnabled()) {
            logger.debug(CLASS_NAME + ".decodeStart(): " + message);
        }

        return message;
    }

    private static AmqpStartOkMessage decodeStartOk(IoSession session, IoBufferEx in) 
            throws ProtocolDecoderException {
        // Do not log raw bytes as it can contain the password.

        AmqpStartOkMessage message = new AmqpStartOkMessage();

        // Decode the parameters.
        AmqpTable clientProperties = getTable(in);
        String    mechanism = getShortString(in);
        
        String    response = getLongString(in);
        String    locale = getShortString(in);

        message.setClientProperties(clientProperties);
        message.setSecurityMechanism(mechanism);
        String[] credentials;
        if ("AMQPLAIN".equals(mechanism)) {
            credentials = decodeAuthAmqPlain(response);
        }
        else if ("PLAIN".equalsIgnoreCase(mechanism)) {
            credentials = decodeAuthPlain(response);
        }
        else {
            throw new IllegalStateException("Unsupported SASL authentication mechanism: " + mechanism);
        }
        
        // Inject the SASL authentication mechanism as a session attribute
        // The mechanism selected by client is needed later for secure-ok
        session.setAttribute(AMQP_AUTHENTICATION_MECHANISM, mechanism);
        
        String   username;
        if (credentials != null) {
            username = credentials[0];
            char[] password = credentials[1].toCharArray();

            message.setUsername(username);
            message.setPassword(password);
        }
        
        message.setLocale(locale);
        
        Logger logger = LoggerFactory.getLogger(SERVICE_AMQP_PROXY_LOGGER);
        if (logger.isDebugEnabled()) {
            logger.debug(CLASS_NAME + ".decodeStartOk(): " + message);
        }

        return message;
    }

    private static AmqpTuneMessage decodeTune(IoBufferEx in) {
        Logger logger = LoggerFactory.getLogger(SERVICE_AMQP_PROXY_LOGGER);

        if (logger.isDebugEnabled()) {
            String s = ".decodeTune(): Raw bytes - " + getHexDump(in);
            logger.debug(CLASS_NAME + s);
        }

        AmqpTuneMessage message = new AmqpTuneMessage();

        // Decode the parameters.
        int maxChannels = getUnsignedShort(in);
        int maxFrameSize = in.getInt();
        int heartbeatDelay = getUnsignedShort(in);

        message.setMaxChannels(maxChannels);
        message.setMaxFrameSize(maxFrameSize);
        message.setHeartbeatDelay(heartbeatDelay);

        if (logger.isDebugEnabled()) {
            logger.debug(CLASS_NAME + ".decodeTune(): " + message);
        }

        return message;
    }

    private static AmqpTuneOkMessage decodeTuneOk(IoBufferEx in) {
        Logger logger = LoggerFactory.getLogger(SERVICE_AMQP_PROXY_LOGGER);

        if (logger.isDebugEnabled()) {
            String s = ".decodeTuneOk(): Raw bytes - " + getHexDump(in);
            logger.debug(CLASS_NAME + s);
        }

        AmqpTuneOkMessage message = new AmqpTuneOkMessage();

        // Decode the parameters.
        int maxChannels = getUnsignedShort(in);
        int maxFrameSize = in.getInt();
        int heartbeatDelay = getUnsignedShort(in);

        message.setMaxChannels(maxChannels);
        message.setMaxFrameSize(maxFrameSize);
        message.setHeartbeatDelay(heartbeatDelay);

        if (logger.isDebugEnabled()) {
            logger.debug(CLASS_NAME + ".decodeTuneOk(): " + message);
        }

        return message;
    }
    
    private static int getBit(IoBufferEx buffer) {
        return buffer.getUnsigned();
    }

    private static long getMilliseconds(IoBufferEx buffer) {
        return buffer.getLong();
    }
    
    public static int getUnsignedShort(IoBufferEx buffer) {
        int val = (buffer.getShort() & 0xffff);
        return val;
    }
    
    private static long getUnsignedInt(IoBufferEx buffer) {
        long val = buffer.getInt() & 0xffffffffL;
        return val;
    }

    private static long getUnsignedInt(ByteBuffer buffer) {
        long val = buffer.getInt() & 0xffffffffL;
        return val;
    }

    // Gets an AMQP unsigned long.
    private static long getUnsignedLong(IoBufferEx buffer) {
        // For unsigned longs (8 byte integers)
        // throw away the first word, then read the next
        // word as an unsigned int
        buffer.getInt();
        return getUnsignedInt(buffer);
    }

    private static Object getFieldValue(IoBufferEx buffer) {
        int typeCode = buffer.getUnsigned();
        switch(typeCode) {
        case 116:   // 't'
            boolean b = buffer.getUnsigned() != 0;
            return b;
        default:
            throw new IllegalStateException("Decoding Error in AmqpBuffer: cannot decode field value");
        }
    }

    private static Map<String,Object> getFieldTable(IoBufferEx buffer) {
        Map<String,Object> t = new HashMap<>();
        int len = (int)getUnsignedInt(buffer);
        int initial = buffer.position();
        while (len > (buffer.position()-initial)) {
            String key = getShortString(buffer);
            Object value = getFieldValue(buffer);
            t.put(key,value);
        }
        return t;
    }

    private static AmqpTable getTable(IoBufferEx buffer) throws ProtocolDecoderException {
        long len = getUnsignedInt(buffer);
        long end = buffer.position() + len;  

        ArrayList<AmqpTableEntry> entries = new ArrayList<>();
        while (buffer.position() < end) {
            String   key = getShortString(buffer);
            AmqpType type = getType(buffer);
            // String typeCodec = getMappedType(ti);
            Object   value = getObjectOfType(buffer, type);

            AmqpTable.AmqpTableEntry entry = 
                      new AmqpTable.AmqpTableEntry(key, value, type);
            entries.add(entry);
        }
        
        if (entries.isEmpty()) {
            return null;
        }
        
        AmqpTable table = new AmqpTable();
        table.setEntries(entries);
        return table;
    }

    // Returns an AMQP long-string which is a string prefixed by a unsigned 
    // 32-bit integer representing the length of the string.
    private static String getLongString(IoBufferEx buffer)
    {
        long len = getUnsignedInt(buffer);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < len; i++)
        {
            builder.append((char)(buffer.getUnsigned()));
        }
        String s = builder.toString();
        return s;
    }

    // Returns an AMQP long-string which is a string prefixed by a unsigned 
    // 32-bit integer representing the length of the string.
    private static String getLongString(ByteBuffer buffer)
    {
        long len = getUnsignedInt(buffer);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < len; i++)
        {
            builder.append((char)(buffer.get() & 0xff));
        }
        String s = builder.toString();
        return s;
    }

    // Returns an AMQP short-string which is a string prefixed by a unsigned
    // 8-bit integer representing the length of the string.
    private static String getShortString(IoBufferEx buffer)
    {
        int len = buffer.getUnsigned();       
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < len; i++)
        {
            builder.append((char)(buffer.getUnsigned()));
        }
        String s = builder.toString();
        return s;
    }

    // Returns an AMQP short-string which is a string prefixed by a unsigned
    // 8-bit integer representing the length of the string.
    private static String getShortString(ByteBuffer buffer)
    {
        int len = buffer.get() & 0xff;

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < len; i++)
        {
            builder.append((char)(buffer.get() & 0xff));
        }
        String s = builder.toString();
        return s;
    }

    private static AmqpType getType(IoBufferEx buffer) {
        char c = (char)(buffer.getUnsigned());
        return typeIdentifierMap.get(c);
    }

    private static AmqpType getType(ByteBuffer buffer) {
        char c = (char)(buffer.get() & 0xff);
        return typeIdentifierMap.get(c);
    }

    private static String getHexDump(IoBufferEx buffer) {
        if (buffer.position() == buffer.limit()) {
            return "empty";
        }
        
        StringBuilder hexDump = new StringBuilder();
        for (int i = buffer.position() + buffer.arrayOffset(); i < buffer.limit(); i++) {
            hexDump.append(Integer.toHexString(buffer.get(i)&0xFF)).append(' ');
        }
                
        return hexDump.toString();
    }

    private static String getHexDump(byte[] bytes) {
        StringBuilder hexDump = new StringBuilder();
        for (byte aByte : bytes) {
            hexDump.append(Integer.toHexString(aByte & 0xFF)).append(" ");
        }
                
        return hexDump.toString();
    }

    /*
    private FrameHeader getFrameHeader()
    {        
        int frameType = this.getUnsigned();        
        int channel = this.getUnsignedShort();        
        long size =  this.getUnsignedInt();        

        FrameHeader header = new FrameHeader();
        header.frameType = frameType;
        header.size = size;
        header.channel = channel;

        return header;
    }
    */

    private static Object getObjectOfType(IoBufferEx buffer, AmqpType type) 
           throws ProtocolDecoderException {
        Object value;
        switch (type)
        {
            case BIT:
                value = getBit(buffer);
                break;
            case SHORTSTRING:
                value = getShortString(buffer);
                break;
            case LONGSTRING:
                value = getLongString(buffer);
                break;
            case FIELDTABLE:
                value = getFieldTable(buffer);
                break;
            case TABLE:                
                value = getTable(buffer);
                break;
            case INT:
                value = buffer.getInt();
                break;
            case UNSIGNEDINT:
                value = getUnsignedInt(buffer);
                break;
            case UNSIGNEDSHORT:
                value = getUnsignedShort(buffer);
                break;
            case UNSIGNED:                
                value = buffer.getUnsigned();
                break;
            case SHORT:
                value = getUnsignedShort(buffer);
                break;
            case LONG:
                value = getUnsignedInt(buffer);
                break;
            case OCTET:                
                value = buffer.getUnsigned();
                break;
            case LONGLONG:
                value = getUnsignedLong(buffer);
                break;
            case TIMESTAMP:
                long millis = getMilliseconds(buffer);
                value = new Timestamp(millis);
                break;
            case VOID:
                value = null;
                break;
            default:
                String s = "Invalid type: '" + type;
                throw new ProtocolDecoderException(s);
        }
        return value;
    }
    
    private static final AmqpProperty[] PROPERTY_VALUES = AmqpProperty.values();
    
    @SuppressWarnings("unused")
    private static Map<AmqpProperty, Object> getContentProperties(IoBufferEx buffer) 
            throws ProtocolCodecException {
        
        int packedPropertyFlags = getUnsignedShort(buffer);
        
        Map<AmqpProperty, Object> contentProperties = new TreeMap<>();
        for (int offset = 0, bitmask = packedPropertyFlags; bitmask != 0; offset++, bitmask >>= 1) {
            if ((bitmask & 0x01) != 0x00) {
                AmqpProperty property = PROPERTY_VALUES[offset];
                contentProperties.put(property, 
                                      getObjectOfType(buffer, property.domain().type()));
            }
        }

        return contentProperties;
    }
}

/*
class FrameHeader {
    int  frameType;
    long size;
    int  channel;
}
*/

