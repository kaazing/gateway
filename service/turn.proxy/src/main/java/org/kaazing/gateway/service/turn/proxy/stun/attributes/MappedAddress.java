package org.kaazing.gateway.service.turn.proxy.stun.attributes;

import java.nio.ByteBuffer;

public class MappedAddress extends Address {

    public MappedAddress(byte[] variable) {
        super(variable);
    }

    @Override
    public short getType() {
        return AttributeType.MAPPED_ADDRESS.getType();
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
