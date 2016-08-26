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

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import org.kaazing.gateway.service.turn.proxy.stun.StunMaskAddressFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract address based attribute class for other Address attributes to extend.
 * Support XOR encoded fields as defined here: https://tools.ietf.org/html/rfc5389#section-15.2
   Excerpt:
        X-Port is computed by taking the mapped port in host byte order,
        XOR'ing it with the most significant 16 bits of the magic cookie, and
        then the converting the result to network byte order.  If the IP
        address family is IPv4, X-Address is computed by taking the mapped IP
        address in host byte order, XOR'ing it with the magic cookie, and
        converting the result to network byte order.  If the IP address
        family is IPv6, X-Address is computed by taking the mapped IP address
        in host byte order, XOR'ing it with the concatenation of the magic
        cookie and the 96-bit transaction ID, and converting the result to
        network byte order.
 */

public abstract class AbstractAddress extends Attribute {

    static final Logger LOGGER = LoggerFactory.getLogger(StunMaskAddressFilter.class);

    private static final byte[] MAGIC_COOKIE = new byte[]{(byte) 0x21, (byte) 0x12, (byte) 0xA4, (byte) 0x42};

    protected byte[] address;
    protected int port;
    private Family family;
    private byte[] transactionId;

    public enum Family {
        IPV4((byte) 0x01, 4), IPV6((byte) 0x02, 16);

        private final byte encoding;
        private final int length;

        Family(byte encoding, int length) {
            this.encoding = encoding;
            this.length = length;
        }

        byte getEncoding() {
            return encoding;
        }

        public int getLength() {
            return length;
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

    public AbstractAddress(InetSocketAddress address, byte[] transactionId) {
        this(address);
        if (transactionId.length == IPV6.length - MAGIC_COOKIE.length) {
            this.transactionId = transactionId;
        } else {
            throw new InvalidAttributeException("Invalid length for transactionId: " + transactionId.length);
        }
    }

    public AbstractAddress(byte[] variable) {
        this.family = Family.fromValue(variable[1]);
        this.setPort((variable[2] << 8) + (variable[3] & 0xff));
        this.address = Arrays.copyOfRange(variable, 4, 4 + this.family.length);
    }

    public AbstractAddress(byte[] variable, byte[] transactionId) {
        this(variable);
        if (transactionId.length == IPV6.length - MAGIC_COOKIE.length) {
            this.transactionId = transactionId;
        } else {
            throw new InvalidAttributeException("Invalid length for transactionId: " + transactionId.length);
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
        if (address.length == IPV4.length) {
            family = IPV4;
        } else if (address.length == IPV6.length) {
            family = IPV6;
        } else {
            throw new InvalidAttributeException("Address is neither IPv4 or IPv6");
        }
        this.address = address;
    }

    @Override
    public short getLength() {
        return (short) (this.family.length + 4);
    }

    public Family getFamily() {
        if (address.length == IPV4.length) {
            return IPV4;
        } else if (address.length == IPV6.length) {
            return IPV6;
        }
        throw new InvalidAttributeException("Address is not of IPv4 or IPv6 family");
    }

    protected short xorWithMagicCookie(short s) {
        return (short) (s ^ 0x2112);
    }

    protected byte[] xorWithMagicCookie(byte[] bytes) {
        byte[] temp = bytes.clone();
        if (this.family.equals(Family.IPV4) && temp.length == IPV4.length) {
            for (int i = 0; i < temp.length; i++) {
                temp[i] = (byte) (temp[i] ^ MAGIC_COOKIE[i]);
            }
        } else if (this.family.equals(Family.IPV6) && temp.length == IPV6.length) {
            byte[] xor = new byte[IPV6.length];
            System.arraycopy(MAGIC_COOKIE, 0, xor, 0, MAGIC_COOKIE.length);
            System.arraycopy(transactionId, 0, xor, MAGIC_COOKIE.length, IPV6.length - MAGIC_COOKIE.length);
            for (int i = 0; i < temp.length; i++) {
                temp[i] = (byte) (temp[i] ^ xor[i]);
            }
        } else {
            throw new InvalidAttributeException("Cannot xor given byte array, incorrect length: " + temp.length);
        }

        return temp;
    }

    @Override
    public String toString() {
        try {
            StringBuilder sb = new StringBuilder();
            for (byte b : getVariable()) {
                sb.append(String.format("%02X ", b));
            }
            return String.format(
                    "%s - %s:%s - %s",
                    super.toString(), InetAddress.getByAddress(getAddress()), getPort(), sb.toString()
            );
        } catch (UnknownHostException e) {
            LOGGER.debug("Unable to transform address to string, using default implementation", e);
        }
        return super.toString();
    }
}
