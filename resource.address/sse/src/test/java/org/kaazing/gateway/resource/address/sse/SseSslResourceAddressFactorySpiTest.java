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
package org.kaazing.gateway.resource.address.sse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.kaazing.gateway.resource.address.ResourceAddress.NEXT_PROTOCOL;
import static org.kaazing.gateway.resource.address.ResourceAddress.QUALIFIER;
import static org.kaazing.gateway.resource.address.ResourceAddress.TRANSPORT;
import static org.kaazing.gateway.resource.address.ResourceAddress.TRANSPORT_URI;
import static org.kaazing.gateway.resource.address.ResourceAddressFactory.newResourceAddressFactory;
import static org.kaazing.gateway.resource.address.ResourceFactories.changeSchemeOnly;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.resource.address.ResourceFactory;

public class SseSslResourceAddressFactorySpiTest {

    private static final ResourceFactory HTTPS_RESOURCE_FACTORY = changeSchemeOnly("https");
    private static final ResourceFactory HTTPXE_SSL_RESOURCE_FACTORY = changeSchemeOnly("httpxe+ssl");

    private static final String ADDRESS_URI = "sse+ssl://localhost:2020/events";
    private static final String HTTPS_ADDRESS_URI = HTTPS_RESOURCE_FACTORY.createURI(ADDRESS_URI);
    private static final String HTTPXE_SSL_ADDRESS_URI = HTTPXE_SSL_RESOURCE_FACTORY.createURI(ADDRESS_URI);
    private static final String OPTIONS_ADDRESS_URI = "https://localhost:2121/events";

    private SseSslResourceAddressFactorySpi addressFactorySpi;
    private ResourceAddress httpTransportAddress;
    private ResourceAddress httpxeTransportAddress;
    private ResourceAddress optionsTransportAddress;
    private Map<String, Object> options;
    
    @Before
    public void before() {
        ResourceAddressFactory addressFactory = newResourceAddressFactory();
        addressFactorySpi = new SseSslResourceAddressFactorySpi();
        addressFactorySpi.setResourceAddressFactory(addressFactory);

        httpTransportAddress = addressFactory.newResourceAddress(HTTPS_ADDRESS_URI);
        httpxeTransportAddress = addressFactory.newResourceAddress(HTTPXE_SSL_ADDRESS_URI);
        optionsTransportAddress = addressFactory.newResourceAddress(OPTIONS_ADDRESS_URI);

        options = new HashMap<>();
        options.put("sse.nextProtocol", "custom");
        options.put("sse.qualifier", "random");
        options.put("sse.transport", URI.create("https://localhost:2121/events"));
    }

    @Test
    public void shouldHaveSseSslSchemeName() throws Exception {
        assertEquals("sse+ssl", addressFactorySpi.getSchemeName());
    }

    @Test (expected = IllegalArgumentException.class)
    public void shouldRequireSseSslSchemeName() throws Exception {
        addressFactorySpi.newResourceAddress("test://opaque");
    }

    @Test (expected = IllegalArgumentException.class)
    public void shouldRequireExplicitPath() throws Exception {
        addressFactorySpi.newResourceAddress("sse+ssl://localhost:2020");
    }

    @Test
    public void shouldNotRequireExplicitPort() throws Exception {
        ResourceAddress address = addressFactorySpi.newResourceAddress("sse+ssl://localhost/");
        URI location = address.getResource();
        assertEquals(location.getPort(), 443);
    }

    @Test
    public void shouldCreateAddressWithDefaultOptions() throws Exception {
        ResourceAddress address = addressFactorySpi.newResourceAddress(ADDRESS_URI);
        assertNull(address.getOption(NEXT_PROTOCOL));
        assertNull(address.getOption(QUALIFIER));
        assertEquals(httpTransportAddress, address.getOption(TRANSPORT));
        URI httpsAddressUri = URI.create(HTTPS_ADDRESS_URI);
        assertEquals(httpsAddressUri, address.getOption(TRANSPORT_URI));

        // Test alternate address with Httpxe transport
        address = address.getOption(ResourceAddress.ALTERNATE);
        assertNull(address.getOption(NEXT_PROTOCOL));
        assertNull(address.getOption(QUALIFIER));
        assertEquals(httpxeTransportAddress, address.getOption(TRANSPORT));
        URI httpxeSslAddressUri = URI.create(HTTPXE_SSL_ADDRESS_URI);
        assertEquals(httpxeSslAddressUri, address.getOption(TRANSPORT_URI));
    }

    @Test
    public void shouldCreateAddressWithOptions() {
        ResourceAddress address = addressFactorySpi.newResourceAddress(ADDRESS_URI, options);
        assertEquals("custom", address.getOption(NEXT_PROTOCOL));
        assertEquals("random", address.getOption(QUALIFIER));
        assertEquals(optionsTransportAddress, address.getOption(TRANSPORT));
        URI optionsAddressUri = URI.create(OPTIONS_ADDRESS_URI);
        assertEquals(optionsAddressUri, address.getOption(TRANSPORT_URI));

        // Test alternate address with Httpxe transport
        address = address.getOption(ResourceAddress.ALTERNATE);
        assertEquals("custom", address.getOption(NEXT_PROTOCOL));
        assertEquals("random", address.getOption(QUALIFIER));
        assertEquals(optionsTransportAddress, address.getOption(TRANSPORT));
        assertEquals(optionsAddressUri, address.getOption(TRANSPORT_URI));
    }

    @Test
    public void shouldCreateAddressWithDefaultTransport() throws Exception {
        ResourceAddress address = addressFactorySpi.newResourceAddress(ADDRESS_URI);
        assertNotNull(address.getOption(TRANSPORT_URI));
        URI httpsAddressUri = URI.create(HTTPS_ADDRESS_URI);
        assertEquals(httpsAddressUri, address.getOption(TRANSPORT_URI));
    }
    
    @Test
    public void shouldCreateAddressWithTransport() throws Exception {
        ResourceAddress address = addressFactorySpi.newResourceAddress(ADDRESS_URI, options);
        assertNotNull(address.getOption(TRANSPORT_URI));
        URI optionsAddressUri = URI.create(OPTIONS_ADDRESS_URI);
        assertEquals(optionsAddressUri, address.getOption(TRANSPORT_URI));
    }
    
}
