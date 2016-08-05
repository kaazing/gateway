package org.kaazing.gateway.service.turn.proxy.stun.attributes;

import org.kaazing.gateway.service.turn.proxy.stun.attributes.Attribute;

/**
 * When we pass Attribute through proxy without modifying or needing to understand it
 *
 */
public class ProxyNoopAttribute extends Attribute {

    private final short type;
    private final short length;
    private final byte[] value;

    public ProxyNoopAttribute(short type, short length, byte[] value) {
        this.type = type;
        this.length = length;
        this.value = value;
    }

    @Override
    public short getType() {
        return (short) type;
    }

    @Override
    public short getLength() {
        return length;
    }

    @Override
    public byte[] getVariable() {
        return value;
    }

}
