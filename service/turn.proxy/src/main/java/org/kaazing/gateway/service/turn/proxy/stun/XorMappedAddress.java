package org.kaazing.gateway.service.turn.proxy.stun;

import java.nio.ByteBuffer;

public class XorMappedAddress extends AddressAttribute {
    private static final byte[] MAGIC_COOKIE = new byte[]{(byte) 0x21, (byte) 0x12, (byte) 0xA4, (byte) 0x42};

    public XorMappedAddress(byte[] variable) {
        super(variable);
        setPort(xorWithMagicCookie((short) getPort()));
        setAddress(xorWithMagicCookie(variable));
    }

    @Override
    public short getType() {
        return StunMessageAttributeType.XOR_MAPPED_ADDRESS.getType();
    }

    @Override
    public byte[] getVariable() {
        ByteBuffer byteBuffer = ByteBuffer.allocate((getFamily() == Family.IPV4) ? 8 : 36);
        byteBuffer.put((byte) 0x00);
        byteBuffer.put(getFamily().getEncoding());
        byteBuffer.putShort(xorWithMagicCookie((short) getPort()));
        byteBuffer.put(xorWithMagicCookie(getAddress()));
        return byteBuffer.array();
    }

    short xorWithMagicCookie(short s) {
        return (short) (s ^ 0x2112);
    }

    byte[] xorWithMagicCookie(byte[] bytes) {
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) (bytes[i] ^ MAGIC_COOKIE[i % 4]);
        }
        return bytes;
    }
}
