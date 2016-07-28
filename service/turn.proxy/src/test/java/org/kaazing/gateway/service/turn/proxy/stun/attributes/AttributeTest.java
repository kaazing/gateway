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

import static org.kaazing.gateway.service.turn.proxy.stun.attributes.AttributeType.MAPPED_ADDRESS;
import static org.kaazing.gateway.service.turn.proxy.stun.attributes.AttributeType.XOR_MAPPED_ADDRESS;
import static org.kaazing.gateway.service.turn.proxy.stun.attributes.AttributeType.XOR_PEER_ADDRESS;
import static org.kaazing.gateway.service.turn.proxy.stun.attributes.AttributeType.XOR_RELAY_ADDRESS;
import static org.kaazing.gateway.service.turn.proxy.stun.attributes.AttributeType.EVEN_PORT;
import static org.kaazing.gateway.service.turn.proxy.stun.attributes.AttributeType.RESERVATION_TOKEN;
import static org.kaazing.gateway.service.turn.proxy.stun.attributes.AttributeType.MESSAGE_INTEGRITY;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kaazing.gateway.service.turn.proxy.stun.StunAttributeFactory;

public class AttributeTest {

    // TODO no longer static Attribute factory
    
    @BeforeClass
    public void init(){
        // Move address to private and init here
        // TODO addresses
        
    }

    @Test
    public void mappedAddressIpv4() {
        // Construct Attribute
        short type = MAPPED_ADDRESS.getType();
        short length = 4 + (32 / 8);
        ByteBuffer buf = ByteBuffer.allocate(length);
        byte[] addressLocal = new byte[]{0x7f, 0x00, 0x00, 0x01};
        buf.put(0, (byte) 0x00);
        buf.put(1, (byte) 0x01);
        buf.putShort(2, (short) 8080);
        buf.put(4, addressLocal[0]);
        buf.put(5, addressLocal[1]);
        buf.put(6, addressLocal[2]);
        buf.put(7, addressLocal[3]);
        
        // Assert constructed
        Attribute attr = StunAttributeFactory.get(type, length, buf.array());
        Assert.assertEquals(attr.getLength(), length);
        Assert.assertEquals(attr.getType(), type);
        Assert.assertTrue(Arrays.equals(attr.getVariable(), buf.array()));

        // Test Mapped Attribute setters
        MappedAddress mappedAddressAttribute = (MappedAddress) attr;
        byte[] address = {-1, -1, -1, -1};
        mappedAddressAttribute.setAddress(address);
        mappedAddressAttribute.setPort(8000);

        // Assert Mapped Attribute Characteristics ok
        // TODO assert getters specific to mapped attribute
        mappedAddressAttribute.getAddress();
        
        // Assert Attribute ok
        byte[] newValue = new byte[]{0x00, 0x01, 0x1f, 0x40, -1, -1, -1, -1};
        Assert.assertEquals(attr.getLength(), length);
        Assert.assertEquals(attr.getType(), type);
        Assert.assertTrue(Arrays.equals(attr.getVariable(), newValue));

    }

    @Test
    public void mappedAddressIpv6() throws UnknownHostException {
        short type = MAPPED_ADDRESS.getType();
        short length = 4 + (128 / 8);
        InetAddress ip = null;
        ip = Inet6Address.getByName("2001:0db8:85a3:0000:0000:8a2e:0370:7334");
        byte[] address = ip.getAddress();
        ByteBuffer buf = ByteBuffer.allocate(length);
        buf.put(0, (byte) 0x00);
        buf.put(1, (byte) 0x02);
        buf.putShort(2, (short) 8080);
        for (int i = 0; i < address.length; i++) {
            buf.put(i + 4, address[i]);
        }
        Attribute attr = StunAttributeFactory.get(type, length, buf.array());
        Assert.assertEquals(attr.getLength(), length);
        Assert.assertEquals(attr.getType(), type);
        Assert.assertTrue(Arrays.equals(attr.getVariable(), buf.array()));

        MappedAddress mappedAddressAttribute = (MappedAddress) attr;
        ip = Inet6Address.getByName("5555:5555:5555:5555:5555:5555:5555:5555");
        address = ip.getAddress();
        mappedAddressAttribute.setAddress(address);
        mappedAddressAttribute.setPort(8000);

        ByteBuffer newValue = ByteBuffer.allocate(length);
        newValue.put(0, (byte) 0x00);
        newValue.put(1, (byte) 0x02);
        newValue.putShort(2, (short) 8000);
        for (int i = 0; i < address.length; i++) {
            newValue.put(i + 4, address[i]);
        }

        Assert.assertEquals(attr.getLength(), length);
        Assert.assertEquals(attr.getType(), type);
        Assert.assertTrue(Arrays.equals(attr.getVariable(), newValue.array()));

    }

    @Test
    public void noopAttribute() {
        // TODO with any random not implemented attribute (or create your own)
    }

    @Test
    public void username() {
        Assume.assumeTrue("Not implemented", true);
    }

    @Test
    public void messageIntegrity() {
        // We will need short term and long term, probably do two class implementations.  
        // Factory will take a config 
        // example: "user:realm:pass"
        // hash: "0x8493fbc53ba582fb4c044c456bdc40eb"
        short type = MESSAGE_INTEGRITY.getType();
        short length = 16;
        byte[] key = new byte[]{(byte) 0x84, (byte) 0x93, (byte) 0xfb, (byte) 0xc5, (byte) 0x3b, (byte) 0xa5, (byte) 0x82,
                (byte) 0xfb, (byte) 0x4c, (byte) 0x04, (byte) 0x4c, (byte) 0x45, (byte) 0x6b, (byte) 0xdc, (byte) 0x40,
                (byte) 0xeb};
        Attribute attr = StunAttributeFactory.get(type, length, key);
        Assert.assertEquals(attr.getLength(), length);
        Assert.assertEquals(attr.getType(), type);
        Assert.assertTrue(Arrays.equals(attr.getVariable(), key));

        MessageIntegrity mesInteg = (MessageIntegrity) attr;
        Assert.assertEquals(mesInteg.getLength(), length);
        Assert.assertEquals(mesInteg.getType(), type);
        Assert.assertTrue(Arrays.equals(mesInteg.getVariable(), key));

        // TODO: Decoding of the key. @MessageIntegrity
        Assert.assertEquals(mesInteg.getUsername(), "user");
        Assert.assertEquals(mesInteg.getRealm(), "realm");
        Assert.assertEquals(mesInteg.getPassword(), "pass");
    }

    @Test
    public void errorCode() {
        Assume.assumeTrue("Not implemented", true);
    }

    @Test
    public void unknownErrorCode() {
        Assume.assumeTrue("Not implemented", true);
    }

    @Test
    public void relam() {
        Assume.assumeTrue("Not implemented", true);
    }

    @Test
    public void nonce() {
        Assume.assumeTrue("Not implemented", true);
    }

    @Test
    public void xorMappedAddressIpv4() {
        short type = XOR_MAPPED_ADDRESS.getType();
        short length = 4 + (32 / 8);
        ByteBuffer buf = ByteBuffer.allocate(length);
        byte[] addressLocal = new byte[]{0x7f, 0x00, 0x00, 0x01};
        buf.put(0, (byte) 0x00);
        buf.put(1, (byte) 0x01);
        buf.putShort(2, (short) 8080);
        buf.put(4, addressLocal[0]);
        buf.put(5, addressLocal[1]);
        buf.put(6, addressLocal[2]);
        buf.put(7, addressLocal[3]);
        Attribute attr = StunAttributeFactory.get(type, length, buf.array());
        Assert.assertEquals(attr.getLength(), length);
        Assert.assertEquals(attr.getType(), type);
        Assert.assertTrue(Arrays.equals(attr.getVariable(), buf.array()));

        XorMappedAddress xorMappedAddressAttribute = (XorMappedAddress) attr;
        Assert.assertEquals(xorMappedAddressAttribute.getPort(), 0x3e82);
        Assert.assertTrue(Arrays.equals(xorMappedAddressAttribute.getAddress(), new byte[]{0x5e, 0x12, (byte) 0xa4, 0x43}));

        byte[] address = {-1, -1, -1, -1};
        xorMappedAddressAttribute.setPort(xorMappedAddressAttribute.xorWithMagicCookie((short) 8000));
        xorMappedAddressAttribute.setAddress(xorMappedAddressAttribute.xorWithMagicCookie(address));

        byte[] newValue = new byte[]{0x00, 0x01, 0x1f, 0x40, -1, -1, -1, -1};
        Assert.assertEquals(xorMappedAddressAttribute.getLength(), length);
        Assert.assertEquals(xorMappedAddressAttribute.getType(), type);
        Assert.assertTrue(Arrays.equals(xorMappedAddressAttribute.getVariable(), newValue));
    }

    @Test
    public void xorMappedAddressIpv6() throws UnknownHostException {
        short type = XOR_MAPPED_ADDRESS.getType();
        short length = 4 + (128 / 8);
        byte[] address = Inet6Address.getByName("2001:0db8:85a3:0000:0000:8a2e:0370:7334").getAddress();
        ByteBuffer buf = ByteBuffer.allocate(length);
        buf.put(0, (byte) 0x00);
        buf.put(1, (byte) 0x02);
        buf.putShort(2, (short) 8080);
        for (int i = 0; i < address.length; i++) {
            buf.put(i + 4, address[i]);
        }
        Attribute attr = StunAttributeFactory.get(type, length, buf.array());
        Assert.assertEquals(attr.getLength(), length);
        Assert.assertEquals(attr.getType(), type);
        Assert.assertTrue(Arrays.equals(attr.getVariable(), buf.array()));

        XorMappedAddress xorMappedAddressAttribute = (XorMappedAddress) attr;
        address = Inet6Address.getByName("5555:5555:5555:5555:5555:5555:5555:5555").getAddress();
        xorMappedAddressAttribute.setAddress(xorMappedAddressAttribute.xorWithMagicCookie(address));
        xorMappedAddressAttribute.setPort(xorMappedAddressAttribute.xorWithMagicCookie((short) 8000));

        ByteBuffer newValue = ByteBuffer.allocate(length);
        newValue.put(0, (byte) 0x00);
        newValue.put(1, (byte) 0x02);
        newValue.putShort(2, (short) 8000);
        for (int i = 0; i < address.length; i++) {
            newValue.put(i + 4, address[i]);
        }

        Assert.assertEquals(attr.getLength(), length);
        Assert.assertEquals(attr.getType(), type);
        Assert.assertTrue(Arrays.equals(xorMappedAddressAttribute.getVariable(), newValue.array()));
    }

    @Test
    public void software() {
        Assume.assumeTrue("Not implemented", true);
    }

    @Test
    public void alternativeServer() {
        Assume.assumeTrue("Not implemented", true);
    }

    @Test
    public void fingerprint() {
        // Need to implement
    }

    @Test
    public void channelNumber() {
        Assume.assumeTrue("Not implemented", true);
    }

    @Test
    public void lifetime() {
        Assume.assumeTrue("Not implemented", true);
    }

    @Test
    public void xorPeerAddressIPv4() {
        short type = XOR_PEER_ADDRESS.getType();
        short length = 4 + (32 / 8);
        ByteBuffer buf = ByteBuffer.allocate(length);
        byte[] addressLocal = new byte[]{0x7f, 0x00, 0x00, 0x01};
        buf.put(0, (byte) 0x00);
        buf.put(1, (byte) 0x01);
        buf.putShort(2, (short) 8080);
        buf.put(4, addressLocal[0]);
        buf.put(5, addressLocal[1]);
        buf.put(6, addressLocal[2]);
        buf.put(7, addressLocal[3]);
        Attribute attr = StunAttributeFactory.get(type, length, buf.array());
        Assert.assertEquals(attr.getLength(), length);
        Assert.assertEquals(attr.getType(), type);
        Assert.assertTrue(Arrays.equals(attr.getVariable(), buf.array()));

        XorPeerAddress xorPeerAddressAttribute = (XorPeerAddress) attr;
        Assert.assertEquals(xorPeerAddressAttribute.getPort(), 0x3e82);
        Assert.assertTrue(Arrays.equals(xorPeerAddressAttribute.getAddress(), new byte[]{0x5e, 0x12, (byte) 0xa4, 0x43}));

        byte[] address = {-1, -1, -1, -1};
        xorPeerAddressAttribute.setPort(xorPeerAddressAttribute.xorWithMagicCookie((short) 8000));
        xorPeerAddressAttribute.setAddress(xorPeerAddressAttribute.xorWithMagicCookie(address));

        byte[] newValue = new byte[]{0x00, 0x01, 0x1f, 0x40, -1, -1, -1, -1};
        Assert.assertEquals(xorPeerAddressAttribute.getLength(), length);
        Assert.assertEquals(xorPeerAddressAttribute.getType(), type);
        Assert.assertTrue(Arrays.equals(xorPeerAddressAttribute.getVariable(), newValue));
    }

    @Test
    public void xorPeerAddressIPv6() throws UnknownHostException {
        short type = XOR_PEER_ADDRESS.getType();
        short length = 4 + (128 / 8);
        InetAddress ip = null;
        ip = Inet6Address.getByName("2001:0db8:85a3:0000:0000:8a2e:0370:7334");
        byte[] address = ip.getAddress();
        ByteBuffer buf = ByteBuffer.allocate(length);
        buf.put(0, (byte) 0x00);
        buf.put(1, (byte) 0x02);
        buf.putShort(2, (short) 8080);
        for (int i = 0; i < address.length; i++) {
            buf.put(i + 4, address[i]);
        }
        Attribute attr = StunAttributeFactory.get(type, length, buf.array());
        Assert.assertEquals(attr.getLength(), length);
        Assert.assertEquals(attr.getType(), type);
        Assert.assertTrue(Arrays.equals(attr.getVariable(), buf.array()));

        XorPeerAddress xorPeerAddressAttribute = (XorPeerAddress) attr;
        ip = Inet6Address.getByName("5555:5555:5555:5555:5555:5555:5555:5555");
        address = ip.getAddress();
        xorPeerAddressAttribute.setAddress(xorPeerAddressAttribute.xorWithMagicCookie(address));
        xorPeerAddressAttribute.setPort(xorPeerAddressAttribute.xorWithMagicCookie((short) 8000));

        ByteBuffer newValue = ByteBuffer.allocate(length);
        newValue.put(0, (byte) 0x00);
        newValue.put(1, (byte) 0x02);
        newValue.putShort(2, (short) 8000);
        for (int i = 0; i < address.length; i++) {
            newValue.put(i + 4, address[i]);
        }

        Assert.assertEquals(xorPeerAddressAttribute.getLength(), length);
        Assert.assertEquals(xorPeerAddressAttribute.getType(), type);
        Assert.assertTrue(Arrays.equals(xorPeerAddressAttribute.getVariable(), newValue.array()));
    }

    @Test
    public void data() {
        Assume.assumeTrue("Not implemented", true);
    }

    @Test
    public void xorRelayAddressIPv4() {
        short type = XOR_RELAY_ADDRESS.getType();
        short length = 4 + (32 / 8);
        ByteBuffer buf = ByteBuffer.allocate(length);
        byte[] addressLocal = new byte[]{0x7f, 0x00, 0x00, 0x01};
        buf.put(0, (byte) 0x00);
        buf.put(1, (byte) 0x01);
        buf.putShort(2, (short) 8080);
        buf.put(4, addressLocal[0]);
        buf.put(5, addressLocal[1]);
        buf.put(6, addressLocal[2]);
        buf.put(7, addressLocal[3]);
        Attribute attr = StunAttributeFactory.get(type, length, buf.array());
        Assert.assertEquals(attr.getLength(), length);
        Assert.assertEquals(attr.getType(), type);
        Assert.assertTrue(Arrays.equals(attr.getVariable(), buf.array()));

        XorRelayAddress xorMappedAddressAttribute = (XorRelayAddress) attr;
        Assert.assertEquals(xorMappedAddressAttribute.getPort(), 0x3e82);
        Assert.assertTrue(Arrays.equals(xorMappedAddressAttribute.getAddress(), new byte[]{0x5e, 0x12, (byte) 0xa4, 0x43}));

        byte[] address = {-1, -1, -1, -1};
        xorMappedAddressAttribute.setPort(xorMappedAddressAttribute.xorWithMagicCookie((short) 8000));
        xorMappedAddressAttribute.setAddress(xorMappedAddressAttribute.xorWithMagicCookie(address));

        byte[] newValue = new byte[]{0x00, 0x01, 0x1f, 0x40, -1, -1, -1, -1};
        Assert.assertEquals(xorMappedAddressAttribute.getLength(), length);
        Assert.assertEquals(xorMappedAddressAttribute.getType(), type);
        Assert.assertTrue(Arrays.equals(xorMappedAddressAttribute.getVariable(), newValue));
    }

    @Test
    public void xorRelayAddressIPv6() throws UnknownHostException {
        short type = XOR_RELAY_ADDRESS.getType();
        short length = 4 + (128 / 8);
        InetAddress ip = null;
        ip = Inet6Address.getByName("2001:0db8:85a3:0000:0000:8a2e:0370:7334");
        byte[] address = ip.getAddress();
        ByteBuffer buf = ByteBuffer.allocate(length);
        buf.put(0, (byte) 0x00);
        buf.put(1, (byte) 0x02);
        buf.putShort(2, (short) 8080);
        for (int i = 0; i < address.length; i++) {
            buf.put(i + 4, address[i]);
        }
        Attribute attr = StunAttributeFactory.get(type, length, buf.array());
        Assert.assertEquals(attr.getLength(), length);
        Assert.assertEquals(attr.getType(), type);
        Assert.assertTrue(Arrays.equals(attr.getVariable(), buf.array()));

        XorRelayAddress xorRelayAddressAttribute = (XorRelayAddress) attr;
        ip = Inet6Address.getByName("5555:5555:5555:5555:5555:5555:5555:5555");
        address = ip.getAddress();
        xorRelayAddressAttribute.setAddress(xorRelayAddressAttribute.xorWithMagicCookie(address));
        xorRelayAddressAttribute.setPort(xorRelayAddressAttribute.xorWithMagicCookie((short) 8000));

        ByteBuffer newValue = ByteBuffer.allocate(length);
        newValue.put(0, (byte) 0x00);
        newValue.put(1, (byte) 0x02);
        newValue.putShort(2, (short) 8000);
        for (int i = 0; i < address.length; i++) {
            newValue.put(i + 4, address[i]);
        }

        Assert.assertEquals(xorRelayAddressAttribute.getLength(), length);
        Assert.assertEquals(xorRelayAddressAttribute.getType(), type);
        Assert.assertTrue(Arrays.equals(xorRelayAddressAttribute.getVariable(), newValue.array()));
    }

    @Test
    public void evenPort() {
        short type = EVEN_PORT.getType();
        short length = 4;
        byte[] data = new byte[4];
        data[0] = (byte) 0x00;

        Attribute attr = StunAttributeFactory.get(type, length, data);
        Assert.assertEquals(attr.getLength(), length);
        Assert.assertEquals(attr.getType(), type);
        Assert.assertTrue(Arrays.equals(attr.getVariable(), data));

        EvenPort ep = (EvenPort) attr;
        Assert.assertFalse(ep.getReserveNextHigherPort());
        ep.setReserveNextHigherPort(true);
        data[0] = (byte) 0x80;
        Assert.assertEquals(ep.getLength(), length);
        Assert.assertEquals(ep.getType(), type);
        Assert.assertTrue(Arrays.equals(ep.getVariable(), data));
    }

    @Test
    public void requestedTransport() {
        // TODO: note to DPW want to check so we can enforce it
    }

    @Test
    public void dontFragment() {
        Assume.assumeTrue("Not implemented", true);
    }

    @Test
    public void reservationToken() {
        short type = RESERVATION_TOKEN.getType();
        short length = 8;
        byte[] token = new byte[]{0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55};
        Attribute attr = StunAttributeFactory.get(type, length, token);
        Assert.assertEquals(attr.getLength(), length);
        Assert.assertEquals(attr.getType(), type);
        Assert.assertTrue(Arrays.equals(attr.getVariable(), token));

        ReservationToken rt = (ReservationToken) attr;
        Assert.assertEquals(rt.getLength(), length);
        Assert.assertEquals(rt.getType(), type);
        Assert.assertTrue(Arrays.equals(rt.getVariable(), token));

        Assert.assertTrue(Arrays.equals(rt.getToken(), token));
    }
}
