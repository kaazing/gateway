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
package org.kaazing.gateway.resource.address.httpx;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.kaazing.gateway.resource.address.ResourceAddress.NEXT_PROTOCOL;
import static org.kaazing.gateway.resource.address.ResourceAddress.QUALIFIER;
import static org.kaazing.gateway.resource.address.ResourceAddress.TRANSPORT;
import static org.kaazing.gateway.resource.address.ResourceAddress.TRANSPORT_URI;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.KEEP_ALIVE_TIMEOUT;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.REALMS;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.REQUIRED_ROLES;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.http.DefaultHttpRealmInfo;
import org.kaazing.gateway.resource.address.http.HttpRealmInfo;

public class HttpxResourceAddressFactorySpiTest {

    private HttpxResourceAddressFactorySpi addressFactorySpi;
    private String addressURI;
    private Map<String, Object> options;

    private final HttpRealmInfo[] realms =
            new HttpRealmInfo[]{new DefaultHttpRealmInfo(null, null, null, null, null, null, null, null)};

    @Before
    public void before() {
        addressFactorySpi = new HttpxResourceAddressFactorySpi();
        addressURI = "httpx://localhost:2020/";
        options = new HashMap<>();
        options.put("http.nextProtocol", "custom");
        options.put("http.qualifier", "random");
        options.put("http.keepAliveTimeout", (int) SECONDS.toMillis(5));
        options.put("http.realmName", "demo");
        options.put("http.requiredRoles", new String[] { "admin" });
        options.put("http.transport", "wsn://localhost:2121/");
        options.put("http.realms", realms);
    }

    @Test
    public void shouldHaveHttpxSchemeName() throws Exception {
        assertEquals("httpx", addressFactorySpi.getSchemeName());
    }

    @Test (expected = IllegalArgumentException.class)
    public void shouldRequireHttpxSchemeName() throws Exception {
        addressFactorySpi.newResourceAddress("test://opaque");
    }

    @Test (expected = IllegalArgumentException.class)
    public void shouldRequireExplicitPath() throws Exception {
        addressFactorySpi.newResourceAddress("httpx://localhost:80");
    }

    @Test 
    public void shouldNotRequireExplicitPort() throws Exception {
        ResourceAddress address = addressFactorySpi.newResourceAddress("httpx://localhost/");
        URI location = address.getResource();
        assertEquals(location.getPort(), 80);
    }

    @Test
    public void shouldCreateAddressWithDefaultOptions() throws Exception {
        ResourceAddress address = addressFactorySpi.newResourceAddress(addressURI);
        assertNull(address.getOption(NEXT_PROTOCOL));
        assertNull(address.getOption(QUALIFIER));
        assertNull(address.getOption(TRANSPORT));
        assertEquals(address.getOption(KEEP_ALIVE_TIMEOUT).intValue(), 30);
        assertArrayEquals(new HttpRealmInfo[0], address.getOption(REALMS));
        assertEmpty(address.getOption(REQUIRED_ROLES));
    }

    @Test
    public void shouldCreateAddressWithOptions() {
        ResourceAddress address = addressFactorySpi.newResourceAddress(addressURI, options);
        assertEquals("custom", address.getOption(NEXT_PROTOCOL));
        assertEquals("random", address.getOption(QUALIFIER));
        assertNull(address.getOption(TRANSPORT));
        assertEquals(5000L, address.getOption(KEEP_ALIVE_TIMEOUT).longValue());
        assertEquals(realms, address.getOption(REALMS));
        assertArrayEquals(new String[] { "admin" }, address.getOption(REQUIRED_ROLES));
    }

    @Test
    public void shouldCreateAddressWithDefaultTransport() throws Exception {
        ResourceAddress address = addressFactorySpi.newResourceAddress(addressURI);
        assertNotNull(address.getOption(TRANSPORT_URI));
        assertEquals("wsn://localhost:2020/", address.getOption(TRANSPORT_URI));
    }
    
    @Test
    public void shouldCreateAddressWithTransport() throws Exception {
        ResourceAddress address = addressFactorySpi.newResourceAddress(addressURI, options);
        assertNotNull(address.getOption(TRANSPORT_URI));
        assertEquals("wsn://localhost:2121/", address.getOption(TRANSPORT_URI));
    }

    private void assertEmpty(String[] objects) {
        if (objects != null) {
            assertEquals(0, objects.length);
        }
    }
    
}
