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
package org.kaazing.gateway.service.turn.proxy.filters;

import static org.kaazing.gateway.service.turn.proxy.filters.StunMessage.MAGIC_COOKIE;
import static org.kaazing.gateway.service.turn.proxy.filters.StunMessage.attributePaddedLength;

import java.nio.BufferUnderflowException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderException;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.kaazing.gateway.service.turn.proxy.stun.attributes.Attribute;
import org.kaazing.gateway.service.turn.proxy.stun.attributes.AttributeType;
import org.kaazing.gateway.service.turn.proxy.stun.attributes.ErrorCode;
import org.kaazing.gateway.service.turn.proxy.stun.attributes.Username;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.filter.codec.CumulativeProtocolDecoderEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TurnFrameDecoder extends CumulativeProtocolDecoderEx {
    private static final Logger LOGGER = LoggerFactory.getLogger("service.turn.proxy");
    private final StunAttributeFactory stunAttributeFactory;

    public TurnFrameDecoder(IoBufferAllocatorEx<?> allocator, StunAttributeFactory stunAttributeFactory) {
        super(allocator);
        this.stunAttributeFactory = stunAttributeFactory;
    }

    @Override
    protected boolean doDecode(IoSession session, IoBufferEx in, ProtocolDecoderOutput out) throws Exception {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(session + " Decoding TURN message: " + in);
        }
        if (in.remaining() < 2) {
            return false;
        }
        in.mark();

        short leadingBits = in.getShort();

        boolean result;
        final int leadingByte = (int) (leadingBits & 0xC000);
        switch (leadingByte) {
        case 0x0000:
            // https://tools.ietf.org/html/rfc5389#section-6
            result = decodeStunMessage(in, out);
            break;
        case 0x4000:
            // https://tools.ietf.org/html/rfc5766#section-11
            result = decodeChannelDataMessage(session, in, out);
            break;
        default:
            throw new ProtocolDecoderException(String.format("Illegal leading bytes", leadingBits, leadingBits));
        }
        return result;
    }

    private boolean decodeChannelDataMessage(IoSession session, IoBufferEx in, ProtocolDecoderOutput out) {
        in.reset();
        in.mark();

        if (in.remaining() < 4) {
            return false;
        }
        short channel = in.getShort();
        int length = in.getShort() + 4;
        length = ((length + 4 - 1) / 4) * 4;

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(session + "Decoding TURN data message for channel: " + channel + ", of length: " + length);
        }

        in.position(in.position() - 4);

        if (in.remaining() < length) {
            return false;
        }
        byte[] dst = new byte[length];

        in.get(dst);
        out.write(new TurnDataMessage(dst));
        in.mark();

        return in.hasRemaining();
    }

    public static HashMap<String, Username> transactionId2Username = new HashMap<>();

    private boolean decodeStunMessage(IoBufferEx in, ProtocolDecoderOutput out) throws ProtocolDecoderException {
        in.reset();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Decoding STUN message: " + in);
        }
        if (in.remaining() < 20) {
            in.reset();
            return false;
        }
        short leadingBitsAndMessageType = in.getShort();
        StunMessageClass messageClass = StunMessageClass.valueOf(leadingBitsAndMessageType);

        StunMessageMethod method = StunMessageMethod.valueOf(leadingBitsAndMessageType);

        int messageLength = in.getShort();

        int magicCookie = in.getInt();
        validateMagicCookie(magicCookie);

        byte[] transactionId = new byte[12];
        in.get(transactionId);

        List<Attribute> attributes = new ArrayList<>();
        if (in.remaining() < messageLength) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(String.format("Message has %d bytes remaining, which is less than declared length of: %d",
                        in.remaining(), messageLength));
            }
            in.reset();
            return false;
        } else if (in.remaining() == 0) {
            /*
             * https://tools.ietf.org/html/rfc5389#section-15 After the STUN header are zero or more attributes. Each
             * attribute MUST be TLV encoded, with a 16-bit type, 16-bit length, and value. Each STUN attribute MUST end
             * on a 32-bit boundary. As mentioned above, all fields in an attribute are transmitted most significant bit
             * first.
             */
            LOGGER.trace("Message does not contain any attributes");
        } else {
            try {
                attributes = decodeAttributes(in, messageLength, transactionId);
                Username username = null;
                for (Attribute a : attributes) {
                    if (a.getType() == AttributeType.USERNAME.getType()) {

                        username = (Username) a;
                    }
                    if (username != null) {
                        LOGGER.trace("username: " + username + ", " + Base64.getEncoder().encodeToString(transactionId));
                        transactionId2Username.put(Base64.getEncoder().encodeToString(transactionId), username);
                    }
                }
            } catch (BufferUnderflowException e) {
                List<Attribute> errors = new ArrayList<>(1);
                ErrorCode errorCode = new ErrorCode();
                errorCode.setErrorCode(400);
                errorCode.setErrMsg("Bad Request");
                errors.add(errorCode);
                StunMessage stunMessage = new StunMessage(StunMessageClass.ERROR, method, transactionId, errors);
                LOGGER.warn("replying with error message: " + stunMessage);
                throw new TurnFrameDecoderException("Could not decode attributes", e, stunMessage);
            }
        }
        StunMessage stunMessage = new StunMessage(messageClass, method, transactionId, attributes);
        out.write(stunMessage);
        in.mark();

        return true;
    }

    private List<Attribute> decodeAttributes(IoBufferEx in, int remaining, byte[] transactionId) {
        List<Attribute> stunMessageAttributes = new ArrayList<>();
        // Any attribute type MAY appear more than once in a STUN message.
        // Unless specified otherwise, the order of appearance is significant:
        // only the first occurrence needs to be processed by a receiver, and
        // any duplicates MAY be ignored by a receiver.
        do {
            int type = in.getShort();
            short length = in.getShort();
            remaining -= 4;

            // get variable
            byte[] variable = new byte[length];
            in.get(variable);
            Attribute attribute = stunAttributeFactory.get(type, length, variable, transactionId);
            LOGGER.trace("Decoded STUN attribute: " + attribute);
            stunMessageAttributes.add(attribute);
            remaining -= length;

            // copy padding, it can be any value, but is not ignored for MESSAGE-INTEGRITY generation
            byte[] padding = new byte[attributePaddedLength(length) - length];
            for (int i = length; i < attributePaddedLength(length); i++) {
                padding[i - length] = in.get();
                remaining -= 1;
            }
            attribute.setPadding(padding);

        } while (remaining > 0);
        return stunMessageAttributes;
    }

    private void validateMagicCookie(int magicCookie) throws ProtocolDecoderException {
        if (magicCookie != MAGIC_COOKIE) {
            throw new ProtocolDecoderException("Illegal magic cookie value: " + magicCookie);
        }

    }
}
