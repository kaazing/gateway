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

    @Test (expected = IllegalArgumentException.class)
    public void shouldRequireExplicitPortOnNetworkInterface() {
        ResolutionUtils.parseBindAddress("[@eth0:1]");
    }

    @Test (expected = IllegalArgumentException.class)
    public void shouldRequireIpV4WithoutSquareBrackets() {
        ResolutionUtils.parseBindAddress("[192.168.0.1]:2020");
    }

    @Test (expected = IllegalArgumentException.class)
    public void shouldRequireCorrectPort() {
        ResolutionUtils.parseBindAddress("host:90000");
    }
    
    @Test (expected = IllegalArgumentException.class)
    public void shouldRequireComplexInterfaceBetweenBrackets() {
        ResolutionUtils.parseBindAddress("@Local Area Connection:2020");
    }

}
