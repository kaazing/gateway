package org.kaazing.gateway.service.turn.proxy.stun.attributes;

import java.nio.ByteBuffer;

public class XorPeerAddress extends Address {
    private static final byte[] MAGIC_COOKIE = new byte[]{(byte) 0x21, (byte) 0x12, (byte) 0xA4, (byte) 0x42};

    public XorPeerAddress(byte[] variable) {
        super(variable);
        setPort(xorWithMagicCookie((short) getPort()));
        setAddress(xorWithMagicCookie(getAddress()));
    }

    @Override
    public short getType() {
        return AttributeType.XOR_PEER_ADDRESS.getType();
    }

    @Override
    public byte[] getVariable() {
        ByteBuffer byteBuffer = ByteBuffer.allocate((getFamily() == Family.IPV4) ? 4 + 32/8 : 4 + 128/8);
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
        byte[] temp = bytes.clone();
        for (int i = 0; i < temp.length; i++) {
            temp[i] = (byte) (temp[i] ^ MAGIC_COOKIE[i % 4]);
        }
        return temp;
    }
}
