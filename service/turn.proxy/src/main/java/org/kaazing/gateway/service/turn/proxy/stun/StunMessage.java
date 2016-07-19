package org.kaazing.gateway.service.turn.proxy.stun;

import java.util.List;

public class StunMessage {

    private final StunMessageClass messageClass;
    private final StunMessageMethod method;
    private final short messageLength;
    private final byte[] transactionId;
    private final List<StunMessageAttribute> attributes;
    public static final int MAGIC_COOKIE = 0x2112A442;

    public StunMessage(StunMessageClass messageClass, StunMessageMethod method, short messageLength, byte[] transactionId,
            List<StunMessageAttribute> attributes) {
        this.messageClass = messageClass;
        this.method = method;
        this.messageLength = messageLength;
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
        return messageLength;
    }

    public byte[] getTransactionId() {
        return transactionId;
    }

    public List<StunMessageAttribute> getAttributes() {
        return attributes;
    }

}
