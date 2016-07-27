package org.kaazing.gateway.service.turn.proxy.stun.attributes;

public class ReservationToken extends Attribute {

    byte[] token;

    public ReservationToken(byte[] value) {
        this.token = value;
    }

    @Override
    public short getType() {
        return AttributeType.RESERVATION_TOKEN.getType();
    }

    @Override
    public short getLength() {
        return 8;
    }

    @Override
    public byte[] getVariable() {
        return token;
    }

}
