/**
 * Copyright 2007-2015, Kaazing Corporation. All rights reserved.
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
package org.kaazing.gateway.resource.address.udp;

import static java.lang.String.format;
import static java.net.InetAddress.getByAddress;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assume.assumeTrue;
import static org.kaazing.gateway.resource.address.ResourceAddress.ALTERNATE;
import static org.kaazing.gateway.resource.address.ResourceAddress.NEXT_PROTOCOL;
import static org.kaazing.gateway.resource.address.ResourceAddress.QUALIFIER;
import static org.kaazing.gateway.resource.address.ResourceAddress.TRANSPORT_URI;
import static org.kaazing.gateway.resource.address.udp.UdpResourceAddress.BIND_ADDRESS;
import static org.kaazing.gateway.resource.address.udp.UdpResourceAddress.MAXIMUM_OUTBOUND_RATE;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.kaazing.gateway.resource.address.NameResolver;
import org.kaazing.gateway.resource.address.ResourceAddress;

public class UdpResourceAddressFactorySpiTest {

    private UdpResourceAddressFactorySpi factory;
    private String addressURI;
    private Map<String,Object> options;

    @Before
    public void before() {
        factory = new UdpResourceAddressFactorySpi();
        addressURI = "udp://localhost:2020";
        options = new HashMap<>();
        options.put("udp.nextProtocol", "custom");
        options.put("udp.maximumOutboundRate", 534L);
        options.put("udp.qualifier", "random");
        options.put("udp.bind", new InetSocketAddress(2222));
    }

    @Test
    public void shouldHaveUdpSchemeName() throws Exception {
        UdpResourceAddressFactorySpi factory = new UdpResourceAddressFactorySpi();
        assertEquals("udp", factory.getSchemeName());
    }

    @Test (expected = IllegalArgumentException.class)
    public void shouldRequireUdpSchemeName() throws Exception {
        factory.newResourceAddress("test://opaque");
    }

    @Test (expected = IllegalArgumentException.class)
    public void shouldRequireExplicitPort() throws Exception {
        factory.newResourceAddress("udp://127.0.0.1");
    }

    @Test
    public void shouldCreateAddressWithDefaultOptions() throws Exception {
        ResourceAddress address = factory.newResourceAddress(addressURI);
        assertNull(address.getOption(NEXT_PROTOCOL));
        assertNull(address.getOption(QUALIFIER));
        assertNull(address.getOption(BIND_ADDRESS));
        assertEquals(0xFFFFFFFFL, address.getOption(MAXIMUM_OUTBOUND_RATE).longValue());
    }

    @Test
    public void shouldCreateAddressWithOptions() {
        ResourceAddress address = factory.newResourceAddress(addressURI, options);
        assertEquals("custom", address.getOption(NEXT_PROTOCOL));
        assertEquals("random", address.getOption(QUALIFIER));
        assertNull(address.getOption(BIND_ADDRESS));
        assertEquals(534L, address.getOption(MAXIMUM_OUTBOUND_RATE).longValue());
    }

    @Test
    public void shouldResolveIPv6Address() {
        Map<String, Object> options = new HashMap<>();
        options.put("resolver", new NameResolver() {

            @Override
            public Collection<InetAddress> getAllByName(String host) throws UnknownHostException {
                if ("localhost".equals(host)) {
                    return singleton(getByAddress("::1", new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01 }));
                }

                throw new UnknownHostException(host);
            }
        });
        ResourceAddress address = factory.newResourceAddress(addressURI, options);
        assertNotNull(address);
        assertEquals(URI.create("udp://[0:0:0:0:0:0:0:1]:2020"), address.getResource());
    }

    @Test
    public void shouldCreateIPv6Address() throws Exception {
        Set<URI> expectedURI = new HashSet<>();
        InetAddress[] addresses = InetAddress.getAllByName("::1");
        for(InetAddress address : addresses) {
            expectedURI.add( URI.create(format("udp://[%s]:2020", address.getHostAddress())));
        }
        // Skip the test if IPV6 is not set up or mapped to IPv4
        assumeTrue(!expectedURI.isEmpty());

        String addressURI = "udp://[::1]:2020";
        ResourceAddress resourceAddress = factory.newResourceAddress(addressURI);
        Set<URI> gotURI = new HashSet<>();
        while(resourceAddress != null) {
            gotURI.add(resourceAddress.getResource());
            resourceAddress = resourceAddress.getOption(ALTERNATE);
        }

        assertEquals(expectedURI, gotURI);
    }

    @Test
    public void shouldCreateAddressWithAlternates() {
        Map<String, Object> options = new HashMap<>();
        options.put("resolver", new NameResolver() {

            @Override
            public Collection<InetAddress> getAllByName(String host) throws UnknownHostException {
                if ("localhost".equals(host)) {
                    return asList(
                            getByAddress("127.0.0.1", new byte[] { 0x7f, 0x00, 0x00, 0x01 }),
                            getByAddress("127.0.0.2", new byte[] { 0x7f, 0x00, 0x00, 0x02 }),
                            getByAddress("127.0.0.3", new byte[] { 0x7f, 0x00, 0x00, 0x03 }));
                }

                throw new UnknownHostException(host);
            }
        });
        options.put("transport", URI.create("pipe://internal"));
        ResourceAddress address = factory.newResourceAddress(addressURI, options);
        assertNotNull(address);
        assertEquals(URI.create("udp://127.0.0.1:2020"), address.getResource());
        assertEquals(URI.create("pipe://internal"), address.getOption(TRANSPORT_URI));
        ResourceAddress alternate = address.getOption(ALTERNATE);
        assertNotNull(alternate);
        assertEquals(URI.create("udp://127.0.0.2:2020"), alternate.getResource());
        assertEquals(URI.create("pipe://internal"), alternate.getOption(TRANSPORT_URI));
        alternate = alternate.getOption(ALTERNATE);
        assertNotNull(alternate);
        assertEquals(URI.create("udp://127.0.0.3:2020"), alternate.getResource());
        assertEquals(URI.create("pipe://internal"), alternate.getOption(TRANSPORT_URI));
        alternate = alternate.getOption(ALTERNATE);
        assertNull(alternate);
    }
}
