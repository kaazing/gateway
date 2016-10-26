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
package org.kaazing.gateway.resource.address.http;

import static java.util.concurrent.TimeUnit.SECONDS;
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
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.DEFAULT_HTTP_KEEPALIVE_CONNECTIONS;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.KEEP_ALIVE;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.KEEP_ALIVE_CONNECTIONS;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.KEEP_ALIVE_TIMEOUT;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.MAXIMUM_REDIRECTS;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.MAX_AUTHENTICATION_ATTEMPTS;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.REALMS;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.REQUIRED_ROLES;
import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.SERVER_HEADER_ENABLED;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.resource.address.uri.URIUtils;
import org.kaazing.gateway.security.LoginContextFactory;
import org.kaazing.gateway.security.TypedCallbackHandlerMap;
import org.kaazing.test.util.ResolutionTestUtils;

public class HttpResourceAddressFactorySpiTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private static String networkInterface = ResolutionTestUtils.getLoopbackInterface();
    private HttpResourceAddressFactorySpi addressFactorySpi;
    private String addressURI;
    private Map<String, Object> options;
    private LoginContextFactory loginContextFactory = new LoginContextFactory() {
        @Override
        public LoginContext createLoginContext(TypedCallbackHandlerMap additionalCallbacks) throws LoginException {
            return null;
        }

        @Override
        public LoginContext createLoginContext(Subject subject, String username, char[] password) throws LoginException {
            return null;
        }
    };

    private final HttpRealmInfo[] realms =
            new HttpRealmInfo[]{new DefaultHttpRealmInfo(null, null, null, null, null, null, null, null)};

    @Before
    public void before() {
        addressFactorySpi = new HttpResourceAddressFactorySpi();
        addressURI = "http://localhost:2020/";
        options = new HashMap<>();
        options.put("http.nextProtocol", "custom");
        options.put("http.qualifier", "random");
        options.put("http.keepAlive", false);
        options.put("http.keepAliveTimeout", (int) SECONDS.toMillis(5));
        options.put("http.keepalive.connections", 10);
        options.put("http.realmName", "demo");
        options.put("http.requiredRoles", new String[] { "admin" });
        options.put("http.transport", "tcp://localhost:2121");

        options.put("http.realmAuthorizationMode", "authorizationMode");
        options.put("http.realmChallengeScheme", "challengeScheme");
        options.put("http.realmDescription", "realmDescription");
        options.put("http.realmAuthenticationHeaderNames",new String[] {"h1", "h2"});
        options.put("http.realmAuthenticationParameterNames",new String[] {"p1", "p2"});
        options.put("http.realmAuthenticationCookieNames",new String[] {"c1", "c2"});
        options.put("http.loginContextFactory", loginContextFactory);
        options.put("http.serverHeaderEnabled", Boolean.FALSE);
        options.put("http.max.authentication.attempts", 5);
        options.put("http.realms", realms);
    }

    @Test
    public void shouldHaveHttpSchemeName() throws Exception {
        assertEquals("http", addressFactorySpi.getSchemeName());
    }

    @Test (expected = IllegalArgumentException.class)
    public void shouldRequireHttpSchemeName() throws Exception {
        addressFactorySpi.newResourceAddress("test://opaque");
    }

    @Test (expected = IllegalArgumentException.class)
    public void shouldRequireExplicitPath() throws Exception {
        addressFactorySpi.newResourceAddress("http://localhost:80");
    }

    @Test 
    public void shouldNotRequireExplicitPort() throws Exception {
        ResourceAddress address = addressFactorySpi.newResourceAddress("http://localhost/");
        URI location = address.getResource();
        assertEquals(location.getPort(), 80);
    }

    @Test
    public void shouldCreateAddressWithDefaultOptions() throws Exception {
        ResourceAddress address = addressFactorySpi.newResourceAddress(addressURI);
        assertNull(address.getOption(NEXT_PROTOCOL));
        assertNull(address.getOption(QUALIFIER));
        assertNull(address.getOption(TRANSPORT));
        assertTrue(address.getOption(KEEP_ALIVE));
        assertEquals(address.getOption(KEEP_ALIVE_TIMEOUT).intValue(), 30);
        assertEquals(address.getOption(KEEP_ALIVE_CONNECTIONS).intValue(), DEFAULT_HTTP_KEEPALIVE_CONNECTIONS);
        assertEmpty(address.getOption(REQUIRED_ROLES));
        assertTrue(address.getOption(SERVER_HEADER_ENABLED));
        assertArrayEquals(new HttpRealmInfo[0], address.getOption(REALMS));
        assertEquals(new Integer(0), address.getOption(MAX_AUTHENTICATION_ATTEMPTS));
    }

    @Test
    public void shouldCreateAddressWithOptions() {
        ResourceAddress address = addressFactorySpi.newResourceAddress(addressURI, options);
        assertEquals("custom", address.getOption(NEXT_PROTOCOL));
        assertEquals("random", address.getOption(QUALIFIER));
        assertNull(address.getOption(TRANSPORT));
        assertEquals(5000L, address.getOption(KEEP_ALIVE_TIMEOUT).longValue());
        assertEquals(10, address.getOption(KEEP_ALIVE_CONNECTIONS).intValue());
        assertFalse(address.getOption(KEEP_ALIVE));
        assertEquals(address.getOption(MAXIMUM_REDIRECTS), new Integer(0));
        assertArrayEquals(new String[] { "admin" }, address.getOption(REQUIRED_ROLES));
        assertEquals(realms, address.getOption(REALMS));
        assertFalse(address.getOption(SERVER_HEADER_ENABLED));
        assertEquals(new Integer(5), address.getOption(MAX_AUTHENTICATION_ATTEMPTS));
    }

    @Test
    public void shouldCreateAddressWithDefaultTransport() throws Exception {
        ResourceAddress address = addressFactorySpi.newResourceAddress(addressURI);
        assertNotNull(address.getOption(TRANSPORT_URI));
        assertEquals("tcp://localhost:2020", address.getOption(TRANSPORT_URI));
    }
    
    @Test
    public void shouldCreateAddressWithTransport() throws Exception {
        ResourceAddress address = addressFactorySpi.newResourceAddress(addressURI, options);
        assertNotNull(address.getOption(TRANSPORT_URI));
        assertEquals("tcp://localhost:2121", address.getOption(TRANSPORT_URI));
    }

    @Test
    public void shouldCreateAddressWithTransportOptionsAndAllowNetworkInterfaceSyntaxBrackets() {
        ResourceAddressFactory addressFactory = ResourceAddressFactory.newResourceAddressFactory();
        Map<String, Object> options = new HashMap<>();
        options.put("http.transport", "tcp://[@" + networkInterface + "]:8080");
        ResourceAddress address = addressFactory.newResourceAddress(addressURI, options);
        verifyTransport(address, "tcp://127.0.0.1:8080");
    }

    @Test
    public void shouldCreateAddressWithTransportOptionsAndAllowNetworkInterfaceSyntaxNoBrackets() {
        ResourceAddressFactory addressFactory = ResourceAddressFactory.newResourceAddressFactory();
        Map<String, Object> options = new HashMap<>();
        options.put("http.transport", "tcp://@" + networkInterface + ":8080");
        if (networkInterface.contains(" ")) {
            thrown.expect(IllegalArgumentException.class);
            thrown.expectMessage("Network interface syntax host contains spaces but misses bracket(s)");
        }
        ResourceAddress address = addressFactory.newResourceAddress(addressURI, options);
        verifyTransport(address, "tcp://127.0.0.1:8080");
    }

    private void verifyTransport(ResourceAddress address,
            final String expectedTransportURI) {
    if (expectedTransportURI == null) {
        Assert.assertNull(address.getTransport());
    } else {
        String scheme = URIUtils.getScheme(expectedTransportURI);
        Assert.assertEquals(scheme, address.getTransport().getResource().getScheme());
        URI uriExpectedTransportURI = URI.create(expectedTransportURI);
        Assert.assertEquals(uriExpectedTransportURI, address.getTransport().getResource());
    }
}

    private void assertEmpty(String[] objects) {
        if (objects != null) {
            assertEquals(0, objects.length);
        }
    }
}
