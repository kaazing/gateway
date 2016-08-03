/**
 * Copyright 2007-2016, Kaazing Corporation. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kaazing.gateway.service.turn.proxy.stun.attributes;

import static org.kaazing.gateway.service.turn.proxy.stun.attributes.AbstractAddress.Family.IPV4;
import static org.kaazing.gateway.service.turn.proxy.stun.attributes.AbstractAddress.Family.IPV6;

import java.net.*;
import java.util.Arrays;

/**
 * Abstract address based attribute class for other Address attributes to extend.
 *
 */
public abstract class AbstractAddress extends Attribute {

    private static final byte[] MAGIC_COOKIE = new byte[]{(byte) 0x21, (byte) 0x12, (byte) 0xA4, (byte) 0x42};

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

    public AbstractAddress(InetSocketAddress address) {
        InetAddress inetAddress = address.getAddress();
        if (inetAddress instanceof Inet4Address) {
            this.family = IPV4;
        } else if (inetAddress instanceof Inet6Address) {
            this.family = IPV6;
        } else {
            throw new InvalidAttributeException("No address family for: " + inetAddress.getClass());
        }
        this.setPort(address.getPort());
        this.address = inetAddress.getAddress();
    }

    public AbstractAddress(byte[] variable) {
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
        if (address.length == 32 / 8) {
            return IPV4;
        } else if (address.length == 128 / 8) {
            return IPV6;
        }
        throw new InvalidAttributeException("Address is not of IPv4 or IPv6 family");
    }

    protected short xorWithMagicCookie(short s) {
        return (short) (s ^ 0x2112);
    }

    protected byte[] xorWithMagicCookie(byte[] bytes) {
        byte[] temp = bytes.clone();
        for (int i = 0; i < temp.length; i++) {
            temp[i] = (byte) (temp[i] ^ MAGIC_COOKIE[i % 4]);
        }
        return temp;
    }

    @Override
    public String toString() {
        try {
            return String.format("%s - %s:%s", super.toString(), InetAddress.getByAddress(getAddress()), getPort());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return super.toString();
    }
}
