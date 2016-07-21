package org.kaazing.gateway.service.turn.proxy.stun;

import org.kaazing.k3po.lang.el.Function;

public abstract class AddressAttribute extends StunMessageAttribute {

    private int type;
    private String addresses;
    private int port;

    enum Family {
        ipv4((byte) 0x01), ipv6((byte) 0x02);

        private final byte encoding;

        Family(byte encoding) {
            this.encoding = encoding;
        }

        byte getEncoding() {
            return encoding;
        }
    }

    public AddressAttribute(int type, byte[] variable) {
        this.type = type;
        this.port = port;
        this.addresses = unMask(variable);
                
    }

    private String unMask(byte[] variable) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public short getType() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public short getLength() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Function
    public static byte[] portXOR(int port) {
        byte[] x = new byte[2];
        byte one = (byte) (port >> 8);
        byte two = (byte) (port);
        x[0] = (byte) (one ^ 0x21); // magic cookie byte 1
        x[1] = (byte) (two ^ 0x12); // magic cookie byte 2
        return x;
    }

    @Function
    public static byte[] ipXOR(String ip) {
        byte[] x = new byte[4];
        int[] temp = new int[4];
        byte[] temp2 = new byte[4];

        String[] string = ip.split("\\.");
        for (int i = 0; i < temp.length; i++) {
            temp2[i] = (byte) Integer.parseInt(string[i]);
        }

        byte magic_cookie_0 = (byte) 0x21;
        byte magic_cookie_1 = (byte) 0x12;
        byte magic_cookie_2 = (byte) 0xA4;
        byte magic_cookie_3 = (byte) 0x42;

        x[0] = (byte) (magic_cookie_0 ^ temp2[0]);
        x[1] = (byte) (magic_cookie_1 ^ temp2[1]);
        x[2] = (byte) (magic_cookie_2 ^ temp2[2]);
        x[3] = (byte) (magic_cookie_3 ^ temp2[3]);

        return x;
    }

}
