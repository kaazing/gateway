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
package org.kaazing.gateway.transport.ssl;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.kaazing.gateway.resource.address.ResourceAddress.ALTERNATE;
import static org.kaazing.gateway.resource.address.ResourceAddress.NEXT_PROTOCOL;
import static org.kaazing.gateway.resource.address.ResourceAddress.TRANSPORT;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.mina.core.session.DummySession;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.ssl.bridge.filter.SslCertificateSelectionFilter;
import org.kaazing.gateway.transport.ssl.cert.CertificateBindingException;
import org.kaazing.gateway.transport.ssl.cert.VirtualHostKeySelector;

public class VirtualHostKeySelectorTest {
    private KeyStore keyStore;
    private String keyStorePassword;

    private ResourceAddressFactory addressFactory;

    private String getPassword(String file)
        throws Exception {

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        File f = new File(loader.getResource(file).toURI());
        FileReader fr = new FileReader(f);
        BufferedReader br = new BufferedReader(fr);
        String line = br.readLine();
        br.close();
        return line;
    }

    private KeyStore getKeyStore(String file)
        throws Exception {
        KeyStore ks = KeyStore.getInstance("JCEKS");

        File keyStoreFile = new File("target/truststore/" + file);

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(keyStoreFile);
            ks.load(fis, keyStorePassword.toCharArray());

        } finally {
            if (fis != null) {
                fis.close();
            }
        }

        return ks;
    }

    @Before
    public void setUp() throws Exception {

        addressFactory = ResourceAddressFactory.newResourceAddressFactory();
        SslCertificateSelectionFilter.setCurrentSession(null, false);
    }

    @Test
    public void shouldGetVirtualHostBindKey()
        throws Exception {

        keyStorePassword = getPassword("keystore.pw");
        keyStore = getKeyStore("keystore.db");

        VirtualHostKeySelector keySelector = new VirtualHostKeySelector();
        keySelector.init(keyStore, keyStorePassword);

        ResourceAddress availAliasesKey = keySelector.getAvailableCertAliasesKey(false);
        assertTrue(format("Expected null key, got '%s'", availAliasesKey), availAliasesKey == null);

        String location = "ssl://localhost:9001";
        Map<String, Object> options = new HashMap<>();
        options.put(TRANSPORT.name(), "test://transport");
        ResourceAddress address = addressFactory.newResourceAddress(location, options);

        DummySession session = new DummySession();
        session.setLocalAddress(address.getTransport());
        SslCertificateSelectionFilter.setCurrentSession(session, false);

        keySelector.bind(address);

        availAliasesKey = keySelector.getAvailableCertAliasesKey(false);
        assertTrue("Expected key, got null", availAliasesKey != null);


        Map<String, Object> testTranportOptions = new HashMap<>();
        testTranportOptions.put(NEXT_PROTOCOL.name(), "ssl");
        ResourceAddress testAddress = addressFactory.newResourceAddress("test://transport", testTranportOptions);
        ResourceAddress expectedKey = testAddress;
        assertTrue(format("Expected alias '%s', got '%s'", expectedKey, availAliasesKey), availAliasesKey.equals(expectedKey));
    }

    /*
     * KG-10369 : SSL doesn't work with Gateway in the cloud
     *
     * When ssl accept URI is ssl://foo.com (external address) and is bound using internal 0.0.0.0
     * wildcard address, the ResourceAddress would be something like the following(tcp addresses
     * have alternates)
     * ssl://foo.com
     *   tcp://x:443
     *   tcp://y:443
     *   tcp://z:443
     *
     * If an received request is on tcp://y:443 or any other tcp alternates, this test tests
     * if the certificate alias for foo.com is correctly found
     */
    @Test
    public void shouldGetVirtualHostBindAliasesForSslTcpBindWildcardAddress() throws Exception {
        keyStorePassword = getPassword("keystore.pw");
        keyStore = getKeyStore("keystore.db");

        VirtualHostKeySelector keySelector = new VirtualHostKeySelector();
        keySelector.init(keyStore, keyStorePassword);

        // ssl accept URI with tcp.bind uses wildcard address
        String location = "ssl://localhost:9001";
        Map<String, Object> options = new HashMap<>();
        options.put("ssl.tcp.bind", "0.0.0.0:9001");
        ResourceAddress address = addressFactory.newResourceAddress(location, options);
        keySelector.bind(address);

        // request is received on TCP address that is not the first transport address
        DummySession session = new DummySession();
        ResourceAddress transportAddress = address.getTransport();
        ResourceAddress alternateTcpAddress = transportAddress.getOption(ResourceAddress.ALTERNATE);
        if (alternateTcpAddress != null) {
            transportAddress = alternateTcpAddress;
        }
        session.setLocalAddress(transportAddress);
        SslCertificateSelectionFilter.setCurrentSession(session, false);

        // Verify the certificate alias for the request
        @SuppressWarnings("serial")
        Collection<String> expectedAliases = new HashSet<String>() {{ add("localhost"); }};
        Collection<String> gotAliases = keySelector.getAvailableCertAliases(false);
        assertNotNull(gotAliases);
        assertEquals(expectedAliases, gotAliases);
    }

    @Test
    public void shouldGetVirtualHostBindAliases()
        throws Exception {

        keyStorePassword = getPassword("keystore.pw");
        keyStore = getKeyStore("keystore.db");

        VirtualHostKeySelector keySelector = new VirtualHostKeySelector();
        keySelector.init(keyStore, keyStorePassword);

        Collection<String> availAliases = keySelector.getAvailableCertAliases(false);
        assertTrue(format("Expected null aliases, got '%s'", availAliases), availAliases == null);

        String location = "ssl://localhost:9001";
        Map<String, Object> options = new HashMap<>();
        options.put(TRANSPORT.name(), "test://transport");
        ResourceAddress address = addressFactory.newResourceAddress(location, options);

        DummySession session = new DummySession();
        session.setLocalAddress(address.getTransport());
        SslCertificateSelectionFilter.setCurrentSession(session, false);

        keySelector.bind(address);

        Collection<String> expectedAliases = new HashSet<>();
        expectedAliases.add("localhost");

        availAliases = keySelector.getAvailableCertAliases(false);
        assertTrue("Expected aliases, got null", availAliases != null);
        assertTrue(format("Expected aliases '%s', got '%s'", expectedAliases, availAliases), availAliases.equals(expectedAliases));
    }

    @Test //(expected = CertificateBindingException.class)
    public void shouldNotGetVirtualHostBindKeyMismatchHost()
        throws Exception {

        assumeDNSNameAccessible("www.kaazing.com");

        try {
            keyStorePassword = getPassword("keystore.pw");
            keyStore = getKeyStore("keystore.db");

            VirtualHostKeySelector keySelector = new VirtualHostKeySelector();
            keySelector.init(keyStore, keyStorePassword);

            ResourceAddress availAliasesKey = keySelector.getAvailableCertAliasesKey(false);
            assertTrue(format("Expected null key, got '%s'", availAliasesKey), availAliasesKey == null);

            String location = "ssl://www.kaazing.com:9001";
            Map<String, Object> options = new HashMap<>();
            options.put(TRANSPORT.name(), "test://transport");
            ResourceAddress address = addressFactory.newResourceAddress(location, options);

            DummySession session = new DummySession();
            session.setLocalAddress(address.getTransport());
            SslCertificateSelectionFilter.setCurrentSession(session, false);

            keySelector.bind(address);
            fail("Expected CertificateBindingException");
        } catch (CertificateBindingException e) {
            // ok
        }
    }

    @Test(expected = CertificateBindingException.class)
    public void shoulNotGetVirtualHostBindKeyWithSslBind()
        throws Exception {

        keyStorePassword = getPassword("keystore.pw");
        keyStore = getKeyStore("keystore.db");

        VirtualHostKeySelector keySelector = new VirtualHostKeySelector();
        keySelector.init(keyStore, keyStorePassword);

        ResourceAddress availAliasesKey = keySelector.getAvailableCertAliasesKey(false);
        assertTrue(format("Expected null key, got '%s'", availAliasesKey), availAliasesKey == null);

        // The keystore contains a certificate for localhost, but our <accept> hostname here is www.kaazing.com.
        // So even with our 'ssl.bind' value pointing to localhost, that a certificate for www.kaazing.com
        // should not be found.
        String uri = "ssl://www.kaazing.com:9001";
        Map<String, Object> opts = new HashMap<>();
        opts.put("ssl.tcp.bind", "localhost:9001");
        ResourceAddress address = addressFactory.newResourceAddress(uri, opts);

        DummySession session = new DummySession();
        session.setLocalAddress(address.getTransport());
        SslCertificateSelectionFilter.setCurrentSession(session, false);

        keySelector.bind(address);
    }

    @Test // KG-5706
    public void shouldGetVirtualHostBindKeyWithUnresolvableLogicalServerNameAndPortOnlySslBind()
        throws Exception {

        keyStorePassword = getPassword("keystore.pw");
        keyStore = getKeyStore("keystore.db");

        VirtualHostKeySelector keySelector = new VirtualHostKeySelector();
        keySelector.init(keyStore, keyStorePassword);

        ResourceAddress availAliasesKey = keySelector.getAvailableCertAliasesKey(false);
        assertTrue(format("Expected null key, got '%s'", availAliasesKey), availAliasesKey == null);

        // The keystore contains a certificate for localhost, but our <accept> hostname here is unknown.kaazing.com.
        // So with our 'ssl.bind' value pointing to port-only 0.0.0.0:443, a certificate for 0.0.0.0:443 should be found.
        String location = "ssl://unknown.kaazing.test:9001";
        Map<String, Object> opts = new HashMap<>();
        opts.put("ssl.tcp.bind", "0.0.0.0:443");
        ResourceAddress address = addressFactory.newResourceAddress(location, opts);

        DummySession session = new DummySession();
        session.setLocalAddress(address.getTransport());
        SslCertificateSelectionFilter.setCurrentSession(session, false);

        keySelector.bind(address);

        availAliasesKey = keySelector.getAvailableCertAliasesKey(false);
        assertTrue(format("Expected not null key, got '%s'", availAliasesKey), availAliasesKey != null);

        List<URI> tcpURIList = new ArrayList<>();
        ResourceAddress cursor = availAliasesKey;
        do {
            tcpURIList.add(cursor.getResource());
            cursor = cursor.getOption(ALTERNATE);
        } while (cursor != null);

        assertTrue("Expected IP-addressified localhost with port 443", tcpURIList.contains(URI.create("tcp://127.0.0.1:443")));
    }

    @Test // KG-5706
    public void shouldNOTGetVirtualHostBindKeyWithUnresolvableLogicalServerNameAndMISMATCHEDPortOnlyWssBind()
        throws Exception {

        keyStorePassword = getPassword("keystore.pw");
        keyStore = getKeyStore("keystore.db");

        VirtualHostKeySelector keySelector = new VirtualHostKeySelector();
        keySelector.init(keyStore, keyStorePassword);

        ResourceAddress availAliasesKey = keySelector.getAvailableCertAliasesKey(false);
        assertTrue(format("Expected null key, got '%s'", availAliasesKey), availAliasesKey == null);

        // The keystore contains a certificate for localhost, but our <accept> hostname here is unknown.kaazing.com.
        // So with our 'ssl.bind' value pointing to port-only 0.0.0.0:443, a certificate for 0.0.0.0:443 should be found.
        String uri = "ssl://unknown.kaazing.test:9001";
        Map<String, Object> opts = new HashMap<>();
        opts.put("ssl.tcp.bind", "0.0.0.0:443");
        ResourceAddress address = addressFactory.newResourceAddress(uri, opts);

        DummySession session = new DummySession();
        ResourceAddress newTransport = addressFactory.newResourceAddress("tcp://localhost:444");
        session.setLocalAddress(newTransport);
        SslCertificateSelectionFilter.setCurrentSession(session, false);

        keySelector.bind(address);

        Collection<String> availAliases = keySelector.getAvailableCertAliases(false);
        assertTrue(format("Expected no aliases, got '%s'", availAliases), availAliases == null);

    }

    @Test
    public void shouldGetVirtualHostMatchingHostAlias()
        throws Exception {

        assumeDNSNameAccessible("one.kaazing.test");

        keyStorePassword = getPassword("keystore.pw");
        keyStore = getKeyStore("keystore.db");

        VirtualHostKeySelector keySelector = new VirtualHostKeySelector();
        keySelector.init(keyStore, keyStorePassword);

        ResourceAddress availAliasesKey = keySelector.getAvailableCertAliasesKey(false);
        assertTrue(format("Expected null key, got '%s'", availAliasesKey), availAliasesKey == null);

        String location = "ssl://one.kaazing.test:443";
        Map<String, Object> options = new HashMap<>();
        options.put(TRANSPORT.name(), "tcp://10.0.67.19:443");
        ResourceAddress address = addressFactory.newResourceAddress(location, options);

        DummySession session = new DummySession();
        session.setLocalAddress(address.getTransport());
        SslCertificateSelectionFilter.setCurrentSession(session, false);

        keySelector.bind(address);

        availAliasesKey = keySelector.getAvailableCertAliasesKey(false);
        assertTrue("Expected key, got null", availAliasesKey != null);

        Map<String, Object> tcpOptions = new HashMap<>();
        tcpOptions.put(NEXT_PROTOCOL.name(), "ssl");
        ResourceAddress tcpAddress = addressFactory.newResourceAddress("tcp://10.0.67.19:443", tcpOptions);
        ResourceAddress expectedKey = tcpAddress;

        assertTrue(format("Expected alias '%s', got '%s'", expectedKey, availAliasesKey), availAliasesKey.equals(expectedKey));

        Collection<String> expectedAliases = new HashSet<>();
        expectedAliases.add("one.kaazing.test");
        expectedAliases.add(".kaazing.test");

        Collection<String> availAliases = keySelector.getAvailableCertAliases(false);
        assertTrue("Expected aliases, got null", availAliases != null);
        assertTrue(format("Expected aliases '%s', got '%s'", expectedAliases, availAliases), availAliases.equals(expectedAliases));
    }

    @Test
    public void shouldGetVirtualHostMatchingHostAlias2()
        throws Exception {

        assumeDNSNameAccessible("two.kaazing.test");

        keyStorePassword = getPassword("keystore.pw");
        keyStore = getKeyStore("keystore.db");

        VirtualHostKeySelector keySelector = new VirtualHostKeySelector();
        keySelector.init(keyStore, keyStorePassword);

        ResourceAddress availAliasesKey = keySelector.getAvailableCertAliasesKey(false);
        assertTrue(format("Expected null key, got '%s'", availAliasesKey), availAliasesKey == null);

        String location = "ssl://two.kaazing.test:443";
        Map<String, Object> options = new HashMap<>();
        options.put(TRANSPORT.name(), "tcp://10.0.66.10:443");
        ResourceAddress address = addressFactory.newResourceAddress(location, options);

        DummySession session = new DummySession();
        session.setLocalAddress(address.getTransport());
        SslCertificateSelectionFilter.setCurrentSession(session, false);

        keySelector.bind(address);

        availAliasesKey = keySelector.getAvailableCertAliasesKey(false);
        assertTrue("Expected key, got null", availAliasesKey != null);

        Map<String, Object> tcpOptions = new HashMap<>();
        tcpOptions.put(NEXT_PROTOCOL.name(), "ssl");
        ResourceAddress tcpAddress = addressFactory.newResourceAddress("tcp://10.0.66.10:443", tcpOptions);
        ResourceAddress expectedKey = tcpAddress;
        assertTrue(format("Expected alias '%s', got '%s'", expectedKey, availAliasesKey), availAliasesKey.equals(expectedKey));

        Collection<String> expectedAliases = new HashSet<>();
        expectedAliases.add("two.kaazing.test");
        expectedAliases.add(".kaazing.test");

        Collection<String> availAliases = keySelector.getAvailableCertAliases(false);
        assertTrue("Expected aliases, got null", availAliases != null);
        assertTrue(format("Expected aliases '%s', got '%s'", expectedAliases, availAliases), availAliases.equals(expectedAliases));
    }

    @Test
    public void shouldGetVirtualHostWildcardAlias()
        throws Exception {

        assumeDNSNameAccessible("three.example.test");

        keyStorePassword = getPassword("keystore.pw");
        keyStore = getKeyStore("keystore.db");

        VirtualHostKeySelector keySelector = new VirtualHostKeySelector();
        keySelector.init(keyStore, keyStorePassword);

        ResourceAddress availAliasesKey = keySelector.getAvailableCertAliasesKey(false);
        assertTrue(format("Expected null key, got '%s'", availAliasesKey), availAliasesKey == null);

        String location = "ssl://three.kaazing.test:443";
        Map<String, Object> options = new HashMap<>();
        options.put(TRANSPORT.name(), "tcp://192.168.4.8:443");
        ResourceAddress address = addressFactory.newResourceAddress(location, options);

        DummySession session = new DummySession();
        session.setLocalAddress(address.getTransport());
        SslCertificateSelectionFilter.setCurrentSession(session, false);

        keySelector.bind(address);

        availAliasesKey = keySelector.getAvailableCertAliasesKey(false);
        assertTrue("Expected key, got null", availAliasesKey != null);

        Map<String, Object> tcpOptions = new HashMap<>();
        tcpOptions.put(NEXT_PROTOCOL.name(), "ssl");
        ResourceAddress tcpAddress = addressFactory.newResourceAddress("tcp://192.168.4.8:443", tcpOptions);
        ResourceAddress expectedKey = tcpAddress;
        assertTrue(format("Expected alias '%s', got '%s'", expectedKey, availAliasesKey), availAliasesKey.equals(expectedKey));

        Collection<String> expectedAliases = new HashSet<>();
        expectedAliases.add(".kaazing.test");

        Collection<String> availAliases = keySelector.getAvailableCertAliases(false);
        assertTrue("Expected aliases, got null", availAliases != null);
        assertTrue(format("Expected aliases '%s', got '%s'", expectedAliases, availAliases), availAliases.equals(expectedAliases));
    }

    @Test //(expected = CertificateBindingException.class)
    public void shouldNotGetVirtualHostWildcardAlias()
        throws Exception {

        assumeDNSNameAccessible("three.example.test");

        try {
            keyStorePassword = getPassword("keystore.pw");
            keyStore = getKeyStore("keystore.db");

            VirtualHostKeySelector keySelector = new VirtualHostKeySelector();
            keySelector.init(keyStore, keyStorePassword);

            ResourceAddress availAliasesKey = keySelector.getAvailableCertAliasesKey(false);
            assertTrue(format("Expected null key, got '%s'", availAliasesKey), availAliasesKey == null);

            String uri = "ssl://three.example.test:443";
            ResourceAddress address = addressFactory.newResourceAddress(uri);
            InetSocketAddress inetAddress = new InetSocketAddress("three.example.test", 443);

            DummySession session = new DummySession();
            session.setLocalAddress(inetAddress);
            SslCertificateSelectionFilter.setCurrentSession(session, false);

            keySelector.bind(address);
            fail("Expected CertificateBindingException");
        } catch (CertificateBindingException e) {
            // ok
        }
    }

    static void assumeDNSNameAccessible(final String... hostnames) {
        if ( hostnames == null || hostnames.length == 0) {
            Assume.assumeTrue(false);
            return;
        }

        for (String h: hostnames) {
            try {
                InetAddress.getByName(h);
            } catch (UnknownHostException e) {
                Assume.assumeNoException(e);
            }
        }

    }
}
