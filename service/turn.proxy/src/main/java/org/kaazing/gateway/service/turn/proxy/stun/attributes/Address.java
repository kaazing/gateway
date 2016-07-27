package org.kaazing.gateway.service.turn.proxy.stun.attributes;

import static org.kaazing.gateway.service.turn.proxy.stun.attributes.Address.Family.IPV4;
import static org.kaazing.gateway.service.turn.proxy.stun.attributes.Address.Family.IPV6;

import java.util.Arrays;

public abstract class Address extends Attribute {

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
            throw new InvalidAttributeException("No address family for: " + b);
        }
    }

    public Address(byte[] variable) {
        this.family = Family.fromValue(variable[1]);
        this.setPort(((variable[2] << 8)) + (variable[3] & 0xff));
        if (this.family == Family.IPV4) {
            this.address = Arrays.copyOfRange(variable, 4, 8);
        } else if (this.family == Family.IPV6) {
            this.address = Arrays.copyOfRange(variable, 4, 20);
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
        if (address.length == 32 / 8) {
            family = IPV4;
        } else if (address.length == 128 / 8) {
            family = IPV6;
        } else {
            throw new InvalidAttributeException("Address is neither IPv4 or IPv6");
        }
        this.address = address;
    }

    @Override
    public short getLength() {
        if (this.getFamily() == IPV4) {
            return 8;
        } else {
            return 20;
        }
    }

    public Family getFamily() {
        if (address.length == 32/8) {
            return IPV4;
        } else if (address.length == 128/8) {
            return IPV6;
        }
        throw new InvalidAttributeException("Address is not of IPv4 or IPv6 family");
    }

}
