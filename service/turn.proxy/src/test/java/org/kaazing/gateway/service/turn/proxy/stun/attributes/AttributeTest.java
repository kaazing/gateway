package org.kaazing.gateway.service.turn.proxy.stun.attributes;

import static org.kaazing.gateway.service.turn.proxy.stun.attributes.AttributeType.MAPPED_ADDRESS;

import java.nio.ByteBuffer;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

public class AttributeTest {
    
    @Test
    public void mappedAddressIpv4(){
        short type = MAPPED_ADDRESS.getType();
        short length = 4 + (32 / 4);
        ByteBuffer buf = ByteBuffer.allocate(4 + length);
        byte[] value = new byte[]{0x12};
        Attribute attr = Attribute.Factory.get(type, length, value);
        Assert.assertEquals(attr.getLength(), length);
        Assert.assertEquals(attr.getType(), type);
        // not this byte array compare is not correct, figure some way out to  compare (via API or loop)
        Assert.assertEquals(attr.getVariable(), value);
        
        MappedAddress mappedAddressAttribute = (MappedAddress) attr;
        mappedAddressAttribute.setAddress(address);
        mappedAddressAttribute.setPort(port);
        
        Assert.assertEquals(attr.getLength(), length);
        Assert.assertEquals(attr.getType(), type);
        // not this byte array compare is not correct, figure some way out to  compare (via API or loop)
        Assert.assertEquals(attr.getVariable(), value);

    }
    
    @Test
    public void mappedAddressIpv6(){
        Assert.assertEquals(new byte[]{0x01}, new byte[]{0x01});
        
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
        
    }
    
    @Test
    public void xorMappedAddressIpv6(){
        
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
