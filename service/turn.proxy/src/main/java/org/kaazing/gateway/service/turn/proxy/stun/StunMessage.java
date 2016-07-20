package org.kaazing.gateway.service.turn.proxy.stun;

import java.util.List;

public class StunMessage {

    private final StunMessageClass messageClass;
    private final StunMessageMethod method;
    private final byte[] transactionId;
    private final List<StunMessageAttribute> attributes;
    public static final int MAGIC_COOKIE = 0x2112A442;
    private static final int PADDED_TO = 4;

    public StunMessage(StunMessageClass messageClass, StunMessageMethod method, byte[] transactionId,
            List<StunMessageAttribute> attributes) {
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
        for (StunMessageAttribute attribute : attributes) {
            length += 4;
            length += attributePaddedLength(attribute.getLength());
        }
        return length;
    }

    public byte[] getTransactionId() {
        return transactionId;
    }

    public List<StunMessageAttribute> getAttributes() {
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
