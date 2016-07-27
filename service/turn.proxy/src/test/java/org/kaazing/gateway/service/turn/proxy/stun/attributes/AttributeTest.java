package org.kaazing.gateway.service.turn.proxy.stun.attributes;

import static org.kaazing.gateway.service.turn.proxy.stun.attributes.AttributeType.MAPPED_ADDRESS;
import static org.kaazing.gateway.service.turn.proxy.stun.attributes.AttributeType.XOR_MAPPED_ADDRESS;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

public class AttributeTest {
    
    @Test
    public void mappedAddressIpv4(){
        short type = MAPPED_ADDRESS.getType();
        short length = 4 + (32 / 8);
        ByteBuffer buf = ByteBuffer.allocate(length);
        byte[] addressLocal = new byte[] { 0x7f, 0x00, 0x00, 0x01};
        buf.put(0, (byte) 0x00);
        buf.put(1, (byte) 0x01);
        buf.putShort(2, (short) 8080);
        buf.put(4, addressLocal[0]);
        buf.put(5, addressLocal[1]);
        buf.put(6, addressLocal[2]);
        buf.put(7, addressLocal[3]);
        Attribute attr = Attribute.Factory.get(type, length, buf.array());
        Assert.assertEquals(attr.getLength(), length);
        Assert.assertEquals(attr.getType(), type);
        Assert.assertTrue(Arrays.equals(attr.getVariable(), buf.array()));
        
        MappedAddress mappedAddressAttribute = (MappedAddress) attr;
        byte[] address = {-1, -1, -1, -1};
        mappedAddressAttribute.setAddress(address);
        mappedAddressAttribute.setPort(8000);
        
        byte[] newValue = new byte[] {0x00, 0x01, 0x1f, 0x40, -1, -1, -1, -1};
        Assert.assertEquals(attr.getLength(), length);
        Assert.assertEquals(attr.getType(), type);
        Assert.assertTrue(Arrays.equals(attr.getVariable(), newValue));

    }
    
    @Test
    public void mappedAddressIpv6(){
        short type = MAPPED_ADDRESS.getType();
        short length = 4 + (128 / 8);
        InetAddress ip = null;
        try{
            ip = Inet6Address.getByName("2001:0db8:85a3:0000:0000:8a2e:0370:7334");
        }catch(Exception e) {
            e.printStackTrace();
        }
        byte[] address = ip.getAddress();
        ByteBuffer buf = ByteBuffer.allocate(length);
        buf.put(0, (byte) 0x00);
        buf.put(1, (byte) 0x02);
        buf.putShort(2, (short) 8080);
        for(int i = 0; i < address.length; i++) {
            buf.put(i + 4, address[i]);
        }
        Attribute attr = Attribute.Factory.get(type, length, buf.array());
        Assert.assertEquals(attr.getLength(), length);
        Assert.assertEquals(attr.getType(), type);
        Assert.assertTrue(Arrays.equals(attr.getVariable(), buf.array()));
        
        MappedAddress mappedAddressAttribute = (MappedAddress) attr;
        try{
            ip = Inet6Address.getByName("5555:5555:5555:5555:5555:5555:5555:5555");
        }catch(Exception e) {
            e.printStackTrace();
        }
        address = ip.getAddress();
        mappedAddressAttribute.setAddress(address);
        mappedAddressAttribute.setPort(8000);
        
        ByteBuffer newValue = ByteBuffer.allocate(length);
        newValue.put(0, (byte) 0x00);
        newValue.put(1, (byte) 0x02);
        newValue.putShort(2, (short) 8000);
        for(int i = 0; i < address.length; i++) {
            newValue.put(i + 4, address[i]);
        }

        Assert.assertEquals(attr.getLength(), length);
        Assert.assertEquals(attr.getType(), type);
        Assert.assertTrue(Arrays.equals(attr.getVariable(), newValue.array()));

    }

    @Test
    public void noopAttribute(){
        // TODO, Kaazing NOOP Attribute which catchs all unimplemented
    }
    
    @Test
    public void username(){
        Assume.assumeTrue("Not implemented", true);
    }
    
    @Test
    public void messageIntegrity(){
        
    }
    
    @Test
    public void errorCode(){
        Assume.assumeTrue("Not implemented", true);
    }
    
    @Test
    public void unknownErrorCode(){
        Assume.assumeTrue("Not implemented", true);
    }
    
    @Test
    public void relam(){
        Assume.assumeTrue("Not implemented", true);
    }
    
    @Test
    public void nonce(){
        Assume.assumeTrue("Not implemented", true);
    }
    
    @Test
    public void xorMappedAddressIpv4(){
        short type = XOR_MAPPED_ADDRESS.getType();
        short length = 4 + (32 / 8);
        ByteBuffer buf = ByteBuffer.allocate(length);
        byte[] addressLocal = new byte[] { 0x7f, 0x00, 0x00, 0x01};
        buf.put(0, (byte) 0x00);
        buf.put(1, (byte) 0x01);
        buf.putShort(2, (short) 8080);
        buf.put(4, addressLocal[0]);
        buf.put(5, addressLocal[1]);
        buf.put(6, addressLocal[2]);
        buf.put(7, addressLocal[3]);
        Attribute attr = Attribute.Factory.get(type, length, buf.array());
        Assert.assertEquals(attr.getLength(), length);
        Assert.assertEquals(attr.getType(), type);
        Assert.assertTrue(Arrays.equals(attr.getVariable(), buf.array()));
        
        XorMappedAddress xorMappedAddressAttribute = (XorMappedAddress) attr;
        Assert.assertEquals(xorMappedAddressAttribute.getPort(), 0x3e82);
        Assert.assertTrue(Arrays.equals(xorMappedAddressAttribute.getAddress(), new byte[] {0x5e, 0x12, (byte) 0xa4, 0x43}));
        
        byte[] address = {-1, -1, -1, -1};
        xorMappedAddressAttribute.setPort(xorMappedAddressAttribute.xorWithMagicCookie((short) 8000));
        xorMappedAddressAttribute.setAddress(xorMappedAddressAttribute.xorWithMagicCookie(address));
        
        byte[] newValue = new byte[] {0x00, 0x01, 0x1f, 0x40, -1, -1, -1, -1};
        Assert.assertEquals(xorMappedAddressAttribute.getLength(), length);
        Assert.assertEquals(xorMappedAddressAttribute.getType(), type);
        Assert.assertTrue(Arrays.equals(xorMappedAddressAttribute.getVariable(), newValue));
    }
    
    @Test
    public void xorMappedAddressIpv6(){
        short type = XOR_MAPPED_ADDRESS.getType();
        short length = 4 + (128 / 8);
        InetAddress ip = null;
        try{
            ip = Inet6Address.getByName("2001:0db8:85a3:0000:0000:8a2e:0370:7334");
        }catch(Exception e) {
            e.printStackTrace();
        }
        byte[] address = ip.getAddress();
        ByteBuffer buf = ByteBuffer.allocate(length);
        buf.put(0, (byte) 0x00);
        buf.put(1, (byte) 0x02);
        buf.putShort(2, (short) 8080);
        for(int i = 0; i < address.length; i++) {
            buf.put(i + 4, address[i]);
        }
        Attribute attr = Attribute.Factory.get(type, length, buf.array());
        Assert.assertEquals(attr.getLength(), length);
        Assert.assertEquals(attr.getType(), type);
        Assert.assertTrue(Arrays.equals(attr.getVariable(), buf.array()));
        
        XorMappedAddress xorMappedAddressAttribute = (XorMappedAddress) attr;
        try{
            ip = Inet6Address.getByName("5555:5555:5555:5555:5555:5555:5555:5555");
        }catch(Exception e) {
            e.printStackTrace();
        }
        address = ip.getAddress();
        xorMappedAddressAttribute.setAddress(xorMappedAddressAttribute.xorWithMagicCookie(address));
        xorMappedAddressAttribute.setPort(xorMappedAddressAttribute.xorWithMagicCookie((short) 8000));
        
        ByteBuffer newValue = ByteBuffer.allocate(length);
        newValue.put(0, (byte) 0x00);
        newValue.put(1, (byte) 0x02);
        newValue.putShort(2, (short) 8000);
        for(int i = 0; i < address.length; i++) {
            newValue.put(i + 4, address[i]);
        }

        Assert.assertEquals(attr.getLength(), length);
        Assert.assertEquals(attr.getType(), type);
        Assert.assertTrue(Arrays.equals(xorMappedAddressAttribute.getVariable(), newValue.array()));
    }
    
    @Test
    public void software(){
        Assume.assumeTrue("Not implemented", true);
    }
    
    @Test
    public void alternativeServer(){
        Assume.assumeTrue("Not implemented", true);
    }
    
    @Test
    public void fingerprint(){
        Assume.assumeTrue("Not implemented", true);
    }

    @Test
    public void channelNumber(){
        Assume.assumeTrue("Not implemented", true);
    }

    @Test
    public void lifetime(){
        Assume.assumeTrue("Not implemented", true);
    }
    
    @Test
    public void xorPeerAddressIPv4(){
    }

    @Test
    public void xorPeerAddressIPv6(){
    }
    
    
    @Test
    public void data(){
        Assume.assumeTrue("Not implemented", true);
    }
    @Test
    public void xorRelayAddressIPv4(){
    }

    @Test
    public void xorRelayAddressIPv6(){
    }

    @Test
    public void evenPort(){
    }
    
    @Test
    public void requestedTransport(){
        // note to DPW want to check so we can enforce it
        
    }
    
    @Test
    public void dontFragment(){
        Assume.assumeTrue("Not implemented", true);
    }
    
    @Test
    public void reservationToken(){
        
    }
}
