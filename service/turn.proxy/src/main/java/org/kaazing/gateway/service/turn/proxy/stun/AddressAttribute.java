package org.kaazing.gateway.service.turn.proxy.stun;

import static org.kaazing.gateway.service.turn.proxy.stun.AddressAttribute.Family.IPV4;
import static org.kaazing.gateway.service.turn.proxy.stun.AddressAttribute.Family.IPV6;

import java.util.Arrays;

public abstract class AddressAttribute extends StunMessageAttribute {

    protected byte[] address;
    protected int port;
    private Family family;

    enum Family {
        IPV4((byte) 0x01), IPV6((byte) 0x02);

        private final byte encoding;

        Family(byte encoding) {
            this.encoding = encoding;
        }

        byte getEncoding() {
            return encoding;
        }

        public static Family fromValue(byte b) {
            if (b == 0x01) {
                return IPV4;
            } else if (b == 0x02) {
                return IPV6;
            }
            throw new InvalidStunAttributeException("No address family for: " + b);
        }
    }

    public AddressAttribute(byte[] variable) {
        this.family = Family.fromValue(variable[1]);
        this.setPort((variable[2] << 8) + variable[3]);
        if (this.family == Family.IPV4) {
            this.address = Arrays.copyOfRange(variable, 4, 8);
        } else if (this.family == Family.IPV6) {
            this.address = Arrays.copyOfRange(variable, 4, 16);
        }
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public byte[] getAddress() {
        return this.address;
    }

    public void setAddress(byte[] address) {
        if (address.length == 4) {
            family = IPV4;
        } else if (address.length == 8) {
            family = IPV6;
        } else {
            throw new InvalidStunAttributeException("Address is neither IPv4 or IPv6");
        }
        this.address = address;
    }
    
    @Override
    public short getLength(){
        if(this.getFamily() == IPV4){
            return 12;
        }else{
            return 36;
        }
    }

    public Family getFamily() {
        if(address.length == 4){
            return IPV4;
        } else if(address.length == 16){
            return IPV6;
        }
        throw new InvalidStunAttributeException("Address is not of IPv4 or IPv6 family");
    }

//    public static byte[] portXOR(int port) {
//        byte[] x = new byte[2];
//        byte one = (byte) (port >> 8);
//        byte two = (byte) (port);
//        x[0] = (byte) (one ^ 0x21); // magic cookie byte 1
//        x[1] = (byte) (two ^ 0x12); // magic cookie byte 2
//        return x;
//    }
//
//    public static byte[] ipXOR(String ip) {
//        byte[] x = new byte[4];
//        int[] temp = new int[4];
//        byte[] temp2 = new byte[4];
//
//        String[] string = ip.split("\\.");
//        for (int i = 0; i < temp.length; i++) {
//            temp2[i] = (byte) Integer.parseInt(string[i]);
//        }
//
//        byte magic_cookie_0 = (byte) 0x21;
//        byte magic_cookie_1 = (byte) 0x12;
//        byte magic_cookie_2 = (byte) 0xA4;
//        byte magic_cookie_3 = (byte) 0x42;
//
//        x[0] = (byte) (magic_cookie_0 ^ temp2[0]);
//        x[1] = (byte) (magic_cookie_1 ^ temp2[1]);
//        x[2] = (byte) (magic_cookie_2 ^ temp2[2]);
//        x[3] = (byte) (magic_cookie_3 ^ temp2[3]);
//
//        return x;
}
