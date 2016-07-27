package org.kaazing.gateway.service.turn.proxy.stun;

import java.util.List;

import org.kaazing.gateway.service.turn.proxy.stun.attributes.Attribute;

/**
 * Stun Message as defined in https://tools.ietf.org/html/rfc5389#section-6.
 *
 */
public class StunMessage {

    private final StunMessageClass messageClass;
    private final StunMessageMethod method;
    private final byte[] transactionId;
    private final List<Attribute> attributes;
    public static final int MAGIC_COOKIE = 0x2112A442;
    private static final int PADDED_TO = 4;

    public StunMessage(StunMessageClass messageClass, StunMessageMethod method, byte[] transactionId,
            List<Attribute> attributes) {
        this.messageClass = messageClass;
        this.method = method;
        this.transactionId = transactionId;
        this.attributes = attributes;
    }

    public StunMessageClass getMessageClass() {
        return messageClass;
    }

    public StunMessageMethod getMethod() {
        return method;
    }

    public short getMessageLength() {
        short length = 0;
        for (Attribute attribute : attributes) {
            length += 4;
            length += attributePaddedLength(attribute.getLength());
        }
        return length;
    }

    public byte[] getTransactionId() {
        return transactionId;
    }

    public List<Attribute> getAttributes() {
        return attributes;
    }

    @Override
    public String toString() {
        return String.format("%s %s with length: %d", messageClass.toString(), method.toString(), getMessageLength());
    }

    public static int attributePaddedLength(int length) {
        return ((length + PADDED_TO - 1) / PADDED_TO) * PADDED_TO;
    }
}
