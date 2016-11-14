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
package org.kaazing.gateway.resource.address.tcp;

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
import static org.kaazing.gateway.resource.address.tcp.TcpResourceAddress.BIND_ADDRESS;
import static org.kaazing.gateway.resource.address.tcp.TcpResourceAddress.LOGIN_CONTEXT_FACTORY;
import static org.kaazing.gateway.resource.address.tcp.TcpResourceAddress.MAXIMUM_OUTBOUND_RATE;
import static org.kaazing.gateway.resource.address.tcp.TcpResourceAddress.REALM;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.kaazing.gateway.resource.address.NameResolver;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.security.LoginContextFactory;
import org.kaazing.gateway.security.TypedCallbackHandlerMap;
import org.kaazing.test.util.ResolutionTestUtils;

@RunWith(Parameterized.class)
public class TcpResourceAddressFactorySpiTest {

    private static String networkInterface = ResolutionTestUtils.getLoopbackInterface();

    private TcpResourceAddressFactorySpi factory;
    private Map<String,Object> options;
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

    @Rule
    public ExpectedException thrown= ExpectedException.none();

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {"tcp://localhost:2020"}, {"tcp://[@" + networkInterface + "]:2020"}
           });
    }

    @Parameter
    public String addressURI;

    @Before
    public void before() {
        factory = new TcpResourceAddressFactorySpi();
        options = new HashMap<>();
        options.put("tcp.nextProtocol", "custom");
        options.put("tcp.maximumOutboundRate", 534L);
        options.put("tcp.qualifier", "random");
        options.put("tcp.bind", new InetSocketAddress(2222));
        options.put(REALM.name(), "demo");
        options.put(LOGIN_CONTEXT_FACTORY.name(), loginContextFactory);
    }

    @Test
    public void shouldHaveTcpSchemeName() throws Exception {
        TcpResourceAddressFactorySpi factory = new TcpResourceAddressFactorySpi();
        assertEquals("tcp", factory.getSchemeName());
    }

    @Test (expected = IllegalArgumentException.class)
    public void shouldRequireTcpSchemeName() throws Exception {
        factory.newResourceAddress("test://opaque");
    }

    @Test (expected = IllegalArgumentException.class)
    public void shouldRequireExplicitPort() throws Exception {
        factory.newResourceAddress("tcp://127.0.0.1");
    }

    @Test
    public void shouldThrowSpecificNICExceptionNoBrackets() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("The interface name @ethh is not recognized");
        factory.newResourceAddress("tcp://@ethh:8080");
    }

    @Test
    public void shouldThrowSpecificNICExceptionBrackets() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("The interface name [@ethh] is not recognized");
        factory.newResourceAddress("tcp://[@ethh]:8080");
    }

    @Test
    public void shouldThrowGenericResolutionException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Unable to resolve DNS name: ethh");
        factory.newResourceAddress("tcp://ethh:8080");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRequireExplicitPortOnIPv6() throws Exception {
        factory.newResourceAddress("tcp://[::1]");
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void shouldRequireExplicitPortOnIPv6FromString() throws Exception {
        Map<String, Object> options = new HashMap<>();
        options.put(BIND_ADDRESS.name(), "[::1]");
        factory.newResourceAddress("tcp://[::1]:2020", options);
    }

    @Test
    public void shouldCreateAddressWithResolvedHost() throws Exception {
        ResourceAddress address = factory.newResourceAddress(addressURI);
        assertEquals(URI.create("tcp://127.0.0.1:2020"), address.getResource());
    }

    @Test
    public void shouldCreateAddressWithResolvedIPv6HostFromString() throws Exception {
        Map<String, Object> options = new HashMap<>();
        options.put(BIND_ADDRESS.name(), "[::1]:2020");
        ResourceAddress address = factory.newResourceAddress("tcp://[::1]:2020", options);
        assertEquals(URI.create("tcp://[0:0:0:0:0:0:0:1]:2020"), address.getResource());
    }
    
    @Test
    public void shouldCreateAddressWithResolvedIPv6PortOnlyFromString() throws Exception {
        Map<String, Object> options = new HashMap<>();
        options.put(BIND_ADDRESS.name(), "2020");
        ResourceAddress address = factory.newResourceAddress("tcp://[::1]:2020", options);
        assertEquals("tcp://[::1]:2020", address.getExternalURI());
    }

    @Test
    public void shouldCreateAddressWithDefaultOptions() throws Exception {
        ResourceAddress address = factory.newResourceAddress(addressURI);
        assertNull(address.getOption(NEXT_PROTOCOL));
        assertNull(address.getOption(QUALIFIER));
        assertNull(address.getOption(BIND_ADDRESS));
        assertEquals(0xFFFFFFFFL, address.getOption(MAXIMUM_OUTBOUND_RATE).longValue());
        assertNull(address.getOption(REALM));
        assertNull(address.getOption(LOGIN_CONTEXT_FACTORY));
    }

    @Test
    public void shouldCreateAddressWithOptions() {
        ResourceAddress address = factory.newResourceAddress(addressURI, options);
        assertEquals("custom", address.getOption(NEXT_PROTOCOL));
        assertEquals("random", address.getOption(QUALIFIER));
        assertEquals(new InetSocketAddress(2222), address.getOption(BIND_ADDRESS));
        assertEquals(534L, address.getOption(MAXIMUM_OUTBOUND_RATE).longValue());
        assertEquals("demo", address.getOption(REALM));
        assertEquals(loginContextFactory, address.getOption(LOGIN_CONTEXT_FACTORY));
    }

    @Test
    public void shouldResolveIPv6Address() {
        Map<String, Object> options = new HashMap<>();
        options.put("resolver", new NameResolver() {

            @Override
            public Collection<InetAddress> getAllByName(String host) throws UnknownHostException {
                return singleton(getByAddress("::1", new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01 }));
            }
        });
        ResourceAddress address = factory.newResourceAddress(addressURI, options);
        assertNotNull(address);
        assertEquals(URI.create("tcp://[0:0:0:0:0:0:0:1]:2020"), address.getResource());
    }

    @Test
    public void shouldCreateIPv6Address() throws Exception {
        Set<URI> expectedURI = new HashSet<>();
        InetAddress[] addresses = InetAddress.getAllByName("::1");
        for(InetAddress address : addresses) {
            expectedURI.add( URI.create(format("tcp://[%s]:2020", address.getHostAddress())));
        }
        // Skip the test if IPV6 is not set up or mapped to IPv4
        assumeTrue(!expectedURI.isEmpty());

        String addressURI = "tcp://[::1]:2020";
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
                return asList(
                        getByAddress("127.0.0.1", new byte[] { 0x7f, 0x00, 0x00, 0x01 }),
                        getByAddress("127.0.0.2", new byte[] { 0x7f, 0x00, 0x00, 0x02 }),
                        getByAddress("127.0.0.3", new byte[] { 0x7f, 0x00, 0x00, 0x03 }));
            }
        });
        options.put("transport", "pipe://internal");
        ResourceAddress address = factory.newResourceAddress(addressURI, options);
        assertNotNull(address);
        assertEquals(URI.create("tcp://127.0.0.1:2020"), address.getResource());
        assertEquals("pipe://internal", address.getOption(TRANSPORT_URI));
        ResourceAddress alternate = address.getOption(ALTERNATE);
        assertNotNull(alternate);
        assertEquals(URI.create("tcp://127.0.0.2:2020"), alternate.getResource());
        assertEquals("pipe://internal", alternate.getOption(TRANSPORT_URI));
        alternate = alternate.getOption(ALTERNATE);
        assertNotNull(alternate);
        assertEquals(URI.create("tcp://127.0.0.3:2020"), alternate.getResource());
        assertEquals("pipe://internal", alternate.getOption(TRANSPORT_URI));
        alternate = alternate.getOption(ALTERNATE);
        assertNull(alternate);
    }

}
