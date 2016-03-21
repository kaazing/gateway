package org.kaazing.gateway.resource.address;

import static org.junit.Assert.assertEquals;

import java.net.InetSocketAddress;

import org.junit.Test;

public class ResolutionUtilsTest {

    @Test
    public void shouldParsePortOnly() {
        assertEquals(new InetSocketAddress(2020), ResolutionUtils.parseBindAddress("2020"));
    }
    
    @Test
    public void shouldParseIpV4WithPort() {
        assertEquals(new InetSocketAddress("127.0.0.1", 2020), ResolutionUtils.parseBindAddress("127.0.0.1:2020"));
    }
    
    @Test
    public void shouldParseIpV6WithPort() {
        assertEquals( new InetSocketAddress("[::1]", 2020), ResolutionUtils.parseBindAddress("[::1]:2020"));
    }
    
    @Test
    public void shouldParseNetwokInterfaceWithPort() {
        assertEquals(new InetSocketAddress("@eth0", 2020), ResolutionUtils.parseBindAddress("@eth0:2020"));
    }
    
    @Test
    public void shouldParseComplexNetwokInterfaceWithPort() {
        assertEquals(new InetSocketAddress("[@Local Area Connection]", 2020), ResolutionUtils.parseBindAddress("[@Local Area Connection]:2020"));
    }
    
    @Test
    public void shouldParseSubNetwokInterfaceWithPort() {
        assertEquals( new InetSocketAddress("[@eth0:1]", 2020), ResolutionUtils.parseBindAddress("[@eth0:1]:2020"));
    }
    
    @Test (expected = IllegalArgumentException.class)
    public void shouldRequireExplicitPort() {
        ResolutionUtils.parseBindAddress("localhost");
    }
    
    @Test (expected = IllegalArgumentException.class)
    public void shouldRequireExplicitPortOnIpv4() {
        ResolutionUtils.parseBindAddress("127.0.0.1");
    }
    
    @Test (expected = IllegalArgumentException.class)
    public void shouldRequireExplicitPortOnIpv6() {
        ResolutionUtils.parseBindAddress("[::1]");
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void shouldRequireExplicitPortOnNetworkInterface() {
        ResolutionUtils.parseBindAddress("[@eth0:1]");
    }

}
