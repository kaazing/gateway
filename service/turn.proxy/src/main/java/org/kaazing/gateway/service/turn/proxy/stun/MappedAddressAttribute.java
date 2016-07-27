package org.kaazing.gateway.service.turn.proxy.stun;

import java.nio.ByteBuffer;

public class MappedAddressAttribute extends AddressAttribute {

    public MappedAddressAttribute(byte[] variable) {
        super(variable);
    }

    @Override
    public short getType() {
        return StunMessageAttributeType.MAPPED_ADDRESS.getType();
    }

    @Override
    public byte[] getVariable() {
        ByteBuffer byteBuffer = ByteBuffer.allocate((getFamily() == Family.IPV4) ? 8 : 36);
        byteBuffer.put((byte) 0x00);
        byteBuffer.put(getFamily().getEncoding());
        byteBuffer.putShort((short) getPort());
        byteBuffer.put(getAddress());
        return byteBuffer.array();
    }

}
