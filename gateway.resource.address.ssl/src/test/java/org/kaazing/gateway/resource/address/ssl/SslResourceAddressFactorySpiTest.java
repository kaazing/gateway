/**
 * Copyright (c) 2007-2014 Kaazing Corporation. All rights reserved.
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.kaazing.gateway.resource.address.ssl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.kaazing.gateway.resource.address.ResourceAddress.NEXT_PROTOCOL;
import static org.kaazing.gateway.resource.address.ResourceAddress.QUALIFIER;
import static org.kaazing.gateway.resource.address.ResourceAddress.TRANSPORT;
import static org.kaazing.gateway.resource.address.ResourceAddress.TRANSPORT_URI;
import static org.kaazing.gateway.resource.address.ssl.SslResourceAddress.CIPHERS;
import static org.kaazing.gateway.resource.address.ssl.SslResourceAddress.PROTOCOLS;
import static org.kaazing.gateway.resource.address.ssl.SslResourceAddress.ENCRYPTION_ENABLED;
import static org.kaazing.gateway.resource.address.ssl.SslResourceAddress.KEY_SELECTOR;
import static org.kaazing.gateway.resource.address.ssl.SslResourceAddress.NEED_CLIENT_AUTH;
import static org.kaazing.gateway.resource.address.ssl.SslResourceAddress.WANT_CLIENT_AUTH;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.kaazing.gateway.resource.address.ResourceAddress;

public class SslResourceAddressFactorySpiTest {

    private SslResourceAddressFactorySpi addressFactorySpi;
    private URI addressURI;
    private Map<String, Object> options;
    
    @Before
    public void before() {
        addressFactorySpi = new SslResourceAddressFactorySpi();
        addressURI = URI.create("ssl://localhost:2020");
        options = new HashMap<String, Object>();
        options.put("ssl.nextProtocol", "custom");
        options.put("ssl.qualifier", "random");
        options.put("ssl.encryptionEnabled", Boolean.FALSE);
        options.put("ssl.transport", URI.create("tcp://localhost:2121"));
        options.put("ssl.protocols", new String[] { "SSLv3" });
    }

    @Test
    public void shouldHaveSslSchemeName() throws Exception {
        assertEquals("ssl", addressFactorySpi.getSchemeName());
    }

    @Test (expected = IllegalArgumentException.class)
    public void shouldRequireSslSchemeName() throws Exception {
        addressFactorySpi.newResourceAddress(URI.create("test://opaque"));
    }

    @Test (expected = IllegalArgumentException.class)
    public void shouldRequireExplicitPort() throws Exception {
        addressFactorySpi.newResourceAddress(URI.create("ssl://localhost"));
    }

    @Test
    public void shouldCreateAddressWithDefaultOptions() throws Exception {
        ResourceAddress address = addressFactorySpi.newResourceAddress(addressURI);

        assertNotNull(address);
        assertNull(address.getOption(NEXT_PROTOCOL));
        assertNull(address.getOption(QUALIFIER));
        assertNull(address.getOption(TRANSPORT));
        assertArrayEquals(new String[] { "DEFAULT" }, address.getOption(CIPHERS));
        assertTrue(address.getOption(ENCRYPTION_ENABLED));
        assertFalse(address.getOption(WANT_CLIENT_AUTH));
        assertFalse(address.getOption(NEED_CLIENT_AUTH));
        assertNull(address.getOption(KEY_SELECTOR));
        assertNull(address.getOption(PROTOCOLS));
    }

    @Test
    public void shouldCreateAddressWithTcpTransport() throws Exception {
        ResourceAddress address = addressFactorySpi.newResourceAddress(addressURI);
        assertNotNull(address.getOption(TRANSPORT_URI));
        assertEquals(URI.create("tcp://localhost:2020"), address.getOption(TRANSPORT_URI));
    }

    @Test
    public void shouldCreateAddressWithOptions() {
        ResourceAddress address = addressFactorySpi.newResourceAddress(addressURI, options);

        assertNotNull(address);
        assertEquals("custom", address.getOption(NEXT_PROTOCOL));
        assertEquals("random", address.getOption(QUALIFIER));
        assertNotNull(address.getOption(TRANSPORT_URI));
        assertEquals(URI.create("tcp://localhost:2121"), address.getOption(TRANSPORT_URI));
        assertFalse(address.getOption(ENCRYPTION_ENABLED));
        assertArrayEquals(new String[] { "SSLv3" }, address.getOption(PROTOCOLS));
    }
}
