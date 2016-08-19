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
package org.kaazing.gateway.service.turn.proxy.stun;

import static org.kaazing.gateway.service.turn.proxy.stun.StunProxyMessage.attributePaddedLength;
import static org.kaazing.gateway.util.turn.TurnUtils.HMAC_SHA_1;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.kaazing.gateway.service.turn.proxy.stun.attributes.Attribute;
import org.kaazing.gateway.service.turn.proxy.stun.attributes.Fingerprint;
import org.kaazing.gateway.service.turn.proxy.stun.attributes.MessageIntegrity;
import org.kaazing.gateway.util.turn.TurnUtils;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/*
   https://tools.ietf.org/html/rfc5389#section-15.4

   Based on the rules above, the hash used to construct MESSAGE-
   INTEGRITY includes the length field from the STUN message header.
   Prior to performing the hash, the MESSAGE-INTEGRITY attribute MUST be
   inserted into the message (with dummy content).  The length MUST then
   be set to point to the length of the message up to, and including,
   the MESSAGE-INTEGRITY attribute itself, but excluding any attributes
   after it.  Once the computation is performed, the value of the
   MESSAGE-INTEGRITY attribute can be filled in, and the value of the
   length in the STUN header can be set to its correct value -- the
   length of the entire message.  Similarly, when validating the
   MESSAGE-INTEGRITY, the length field should be adjusted to point to
   the end of the MESSAGE-INTEGRITY attribute prior to calculating the
   HMAC.  Such adjustment is necessary when attributes, such as
   FINGERPRINT, appear after MESSAGE-INTEGRITY.
 */

public class StunFrameEncoder extends ProtocolEncoderAdapter {

    private final IoBufferAllocatorEx<?> allocator;
    private final Map<String, String> currentTransactions;
    private final Key sharedSecret;
    private final String keyAlgorithm;

    private static final Logger LOGGER = LoggerFactory.getLogger("service.turn.proxy");


    public StunFrameEncoder(IoBufferAllocatorEx<?> aloc, Map<String, String> currentTrx, Key ss, String keyAlg) {
        this.allocator = aloc;
        this.currentTransactions = currentTrx;
        this.sharedSecret = ss;
        this.keyAlgorithm = keyAlg;
    }

    @Override
    public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
        if (!(message instanceof StunProxyMessage)) {
            // easiest way to avoid race condition where decoder is removed on the filter chain prior to encoder
            out.write(message);
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Encoding STUN message: " + message);
        }
        StunProxyMessage stunMessage = (StunProxyMessage) message;
        String username = null;
        if (stunMessage.getMessageClass().equals(StunMessageClass.RESPONSE) ||
            stunMessage.getMessageClass().equals(StunMessageClass.ERROR)) {
            username = currentTransactions.remove(Base64.getEncoder().encodeToString(stunMessage.getTransactionId()));
            LOGGER.trace(String.format("Removed username %s from transactions map", username));
            if (stunMessage.isModified() && (username == null || sharedSecret ==null)) {
                LOGGER.warn("STUN message is modified but MESSAGE-INTEGRITY attribute can not be recalculated because username and/or shared secret is not available");
            }
        }
        ByteBuffer buf = allocator.allocate(StunProxyMessage.HEADER_BYTES + stunMessage.getMessageLength());
        short messageMethod = stunMessage.getMethod().getValue();
        short messageClass = stunMessage.getMessageClass().getValue();
        buf.putShort((short) (messageMethod | messageClass));
        buf.putShort(stunMessage.getMessageLength());
        buf.putInt(StunProxyMessage.MAGIC_COOKIE);
        buf.put(stunMessage.getTransactionId());
        encodeAttributes(stunMessage, username, buf);
        buf.flip();
        out.write(allocator.wrap(buf));
    }

    private void encodeAttributes(StunProxyMessage stunMessage, String username, ByteBuffer buf) throws NoSuchAlgorithmException, InvalidKeyException {
        int lengthSoFar = StunProxyMessage.HEADER_BYTES;
        for (Attribute attribute : stunMessage.getAttributes()) {
            if (attribute instanceof MessageIntegrity &&
                stunMessage.isModified() && username != null && sharedSecret != null) {
                LOGGER.debug("Message is modified will override attribute MESSAGE-INTEGRITY");
                // order counts when here we can safely recreate the message integrity
                // overwrite message length and use the current buffer content, secret and password
                buf.putShort(2, (short) lengthSoFar);
                attribute = overrideMessageIntegrity(username, buf);
                buf.putShort(2, stunMessage.getMessageLength());
            } else if (attribute instanceof Fingerprint && stunMessage.isModified()) {
                LOGGER.debug("Message is modified will override atribute FINGERPRINT");
                // message length already at total value
                attribute = new Fingerprint();
                ((Fingerprint) attribute).calculate(buf.array());
            }

            lengthSoFar += 4 + attributePaddedLength(attribute.getLength());
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Encoding STUN attribute: " + attribute);
            }
            buf.putShort(attribute.getType());
            short length = attribute.getLength();
            buf.putShort(length);
            byte[] variable = attribute.getVariable();
            buf.put(variable);
            for (int i = length; i < attributePaddedLength(length); i++) {
                buf.put((byte) 0x00);
            }
        }
    }

    private Attribute overrideMessageIntegrity(String username, ByteBuffer buf) throws NoSuchAlgorithmException, InvalidKeyException {
        char[] password = TurnUtils.generatePassword(username, sharedSecret, keyAlgorithm);
        Mac hMac = Mac.getInstance(HMAC_SHA_1);
        SecretKey signingKey = new SecretKeySpec(new String(password).getBytes(), HMAC_SHA_1);
        hMac.init(signingKey);
        return new MessageIntegrity(hMac.doFinal(buf.array()), StunAttributeFactory.CredentialType.SHORT_TERM);
    }
}
