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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.kaazing.gateway.resource.address.ResourceAddress.BIND_ALTERNATE;
import static org.kaazing.gateway.resource.address.ResourceAddress.NEXT_PROTOCOL;
import static org.kaazing.gateway.resource.address.ResourceAddress.TRANSPORT;
import static org.kaazing.gateway.resource.address.ResourceAddressFactory.newResourceAddressFactory;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class ResourceAddressFactoryTest {

    private String addressURI;
    private Map<String, Object> options;
    
    @Before
    public void setup() throws Exception {
        addressURI = "test://opaque";
        options = new HashMap<>();
        options.put(TRANSPORT.name(), "test://transport");
    }

    @Test (expected = IllegalArgumentException.class)
    public void shouldNotFindFactorySpiForUnregisteredScheme() {
        ResourceAddressFactory addressFactory = newResourceAddressFactory();
        String addressURI = "unregistered://opaque";
        addressFactory.newResourceAddress(addressURI);
    }

    @Test
    public void shouldCreateResourceAddress() {
        ResourceAddressFactory addressFactory = newResourceAddressFactory();
        ResourceAddress address = addressFactory.newResourceAddress(addressURI);
        assertEquals(URI.create(addressURI), address.getResource());
    }

    @Test
    public void shouldCreateResourceAddressWithNextProtocol() {
        ResourceAddressFactory addressFactory = newResourceAddressFactory();
        ResourceAddress address = addressFactory.newResourceAddress(addressURI, "nextProtocol");
        assertEquals(URI.create(addressURI), address.getResource());
        assertEquals("nextProtocol", address.getOption(NEXT_PROTOCOL));
    }

    @Test
    public void shouldCreateResourceAddressWithoutNextProtocol() {
        ResourceAddressFactory addressFactory = newResourceAddressFactory();
        ResourceAddress address = addressFactory.newResourceAddress(addressURI);
        assertEquals(URI.create(addressURI), address.getResource());
        assertNull(address.getOption(NEXT_PROTOCOL));
    }
    
    @Test
    public void shouldCreateResourceAddressWithTransport() {
        ResourceAddressFactory addressFactory = newResourceAddressFactory();
        ResourceAddress address = addressFactory.newResourceAddress(addressURI, options);
        ResourceAddress transport = address.getTransport();
        assertEquals(URI.create(addressURI), address.getResource());
        assertNotNull(transport);
        assertEquals(URI.create("test://transport"), transport.getResource());
    }
    
    @Test
    public void shouldConsumeOptionMatchingProtocolFilter() {
        Map<String, Object> options = new HashMap<>();
        options.put("test[testable].option", "testable");
        ResourceAddressFactory addressFactory = newResourceAddressFactory();
        ResourceAddress address = addressFactory.newResourceAddress(addressURI, options);
        String option = address.getOption(TestResourceAddress.OPTION);
        assertEquals("testable", option);
    }
    
    @Test
    public void shouldNotConsumeOptionNotMatchingProtocolFilter() {
        Map<String, Object> options = new HashMap<>();
        options.put("test[untestable].option", "testable");
        ResourceAddressFactory addressFactory = newResourceAddressFactory();
        ResourceAddress address = addressFactory.newResourceAddress(addressURI, options);
        String option = address.getOption(TestResourceAddress.OPTION);
        assertNull(option);
    }

    @Test
    public void shouldCreateResourceAddressWithDefaultBindAlternate() {

        ResourceAddressFactory addressFactory = newResourceAddressFactory();
        ResourceAddress address = addressFactory.newResourceAddress(addressURI);
        assertEquals(URI.create(addressURI), address.getResource());
        assertTrue(address.getOption(BIND_ALTERNATE));
    }

}
