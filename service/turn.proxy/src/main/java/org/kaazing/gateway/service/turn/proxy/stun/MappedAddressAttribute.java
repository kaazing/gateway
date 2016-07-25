package org.kaazing.gateway.service.turn.proxy.stun;

public class MappedAddressAttribute extends AddressAttribute {

    public MappedAddressAttribute(byte[] variable) {
        super(variable);
    }

    @Override
    public short getType() {
        return StunMessageAttribute.Type.MAPPED_ADDRESS.getType();
    }

}
