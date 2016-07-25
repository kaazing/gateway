package org.kaazing.gateway.service.turn.proxy.stun;

public class NoopAttribute extends StunMessageAttribute {

    private final short type;
    private final short length;
    private final byte[] variable;

    public NoopAttribute(short type, short length, byte[] variable) {
        this.type = type;
        this.length = length;
        this.variable = variable;
    }

    @Override
    public short getType() {
        return type;
    }

    public short getLength() {
        return length;
    }

    public byte[] getVariable() {
        return variable;
    }

}
