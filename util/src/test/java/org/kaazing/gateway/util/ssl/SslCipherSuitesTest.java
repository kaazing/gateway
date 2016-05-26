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
package org.kaazing.gateway.util.ssl;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class SslCipherSuitesTest {
    private static final String[] JAVA6_CIPHERSUITES = new String[] {
        "SSL_RSA_WITH_RC4_128_MD5",
        "SSL_RSA_WITH_RC4_128_SHA",
        "TLS_RSA_WITH_AES_128_CBC_SHA",
        "TLS_RSA_WITH_AES_256_CBC_SHA",
        "TLS_ECDH_ECDSA_WITH_RC4_128_SHA",
        "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA",
        "TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA",
        "TLS_ECDH_RSA_WITH_RC4_128_SHA",
        "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA",
        "TLS_ECDH_RSA_WITH_AES_256_CBC_SHA",
        "TLS_ECDHE_ECDSA_WITH_RC4_128_SHA",
        "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
        "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
        "TLS_ECDHE_RSA_WITH_RC4_128_SHA",
        "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
        "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
        "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
        "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
        "TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
        "TLS_DHE_DSS_WITH_AES_256_CBC_SHA",
        "SSL_RSA_WITH_3DES_EDE_CBC_SHA",
        "TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA",
        "TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA",
        "TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA",
        "TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA",
        "SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA",
        "SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA",
        "SSL_RSA_WITH_DES_CBC_SHA",
        "SSL_DHE_RSA_WITH_DES_CBC_SHA",
        "SSL_DHE_DSS_WITH_DES_CBC_SHA",
        "SSL_RSA_EXPORT_WITH_RC4_40_MD5",
        "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
        "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA",
        "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA",
        "SSL_RSA_WITH_NULL_MD5",
        "SSL_RSA_WITH_NULL_SHA",
        "TLS_ECDH_ECDSA_WITH_NULL_SHA",
        "TLS_ECDH_RSA_WITH_NULL_SHA",
        "TLS_ECDHE_ECDSA_WITH_NULL_SHA",
        "TLS_ECDHE_RSA_WITH_NULL_SHA",
        "SSL_DH_anon_WITH_RC4_128_MD5",
        "TLS_DH_anon_WITH_AES_128_CBC_SHA",
        "TLS_DH_anon_WITH_AES_256_CBC_SHA",
        "SSL_DH_anon_WITH_3DES_EDE_CBC_SHA",
        "SSL_DH_anon_WITH_DES_CBC_SHA",
        "TLS_ECDH_anon_WITH_RC4_128_SHA",
        "TLS_ECDH_anon_WITH_AES_128_CBC_SHA",
        "TLS_ECDH_anon_WITH_AES_256_CBC_SHA",
        "TLS_ECDH_anon_WITH_3DES_EDE_CBC_SHA",
        "SSL_DH_anon_EXPORT_WITH_RC4_40_MD5",
        "SSL_DH_anon_EXPORT_WITH_DES40_CBC_SHA",
        "TLS_ECDH_anon_WITH_NULL_SHA",
        "TLS_KRB5_WITH_RC4_128_SHA",
        "TLS_KRB5_WITH_RC4_128_MD5",
        "TLS_KRB5_WITH_3DES_EDE_CBC_SHA",
        "TLS_KRB5_WITH_3DES_EDE_CBC_MD5",
        "TLS_KRB5_WITH_DES_CBC_SHA",
        "TLS_KRB5_WITH_DES_CBC_MD5",
        "TLS_KRB5_EXPORT_WITH_RC4_40_SHA",
        "TLS_KRB5_EXPORT_WITH_RC4_40_MD5"
    };

    private static final String[] OPENSSL_CIPHER_NICKNAMES = new String[] {
        "RC4-MD5",
        "RC4-SHA",
        "AES128-SHA",
        "AES256-SHA",
        "ECDH-ECDSA-RC4-SHA",
        "ECDH-ECDSA-AES128-SHA",
        "ECDH-ECDSA-AES256-SHA",
        "ECDH-RSA-RC4-SHA",
        "ECDH-RSA-AES128-SHA",
        "ECDH-RSA-AES256-SHA",
        "ECDHE-ECDSA-RC4-SHA",
        "ECDHE-ECDSA-AES128-SHA",
        "ECDHE-ECDSA-AES256-SHA",
        "ECDHE-RSA-RC4-SHA",
        "ECDHE-RSA-AES128-SHA",
        "ECDHE-RSA-AES256-SHA",
        "DHE-RSA-AES128-SHA",
        "DHE-RSA-AES256-SHA",
        "DHE-DSS-AES128-SHA",
        "DHE-DSS-AES256-SHA",
        "DES-CBC3-SHA",
        "ECDH-ECDSA-DES-CBC3-SHA",
        "ECDH-RSA-DES-CBC3-SHA",
        "ECDHE-ECDSA-DES-CBC3-SHA",
        "ECDHE-RSA-DES-CBC3-SHA",
        "EDH-RSA-DES-CBC3-SHA",
        "EDH-DSS-DES-CBC3-SHA",
        "DES-CBC-SHA",
        "EDH-RSA-DES-CBC-SHA",
        "EDH-DSS-DES-CBC-SHA",
        "EXP-RC4-MD5",
        "EXP-DES-CBC-SHA",
        "EXP-EDH-RSA-DES-CBC-SHA",
        "EXP-EDH-DSS-DES-CBC-SHA",
        "NULL-MD5",
        "NULL-SHA",
        "ECDH-ECDSA-NULL-SHA",
        "ECDH-RSA-NULL-SHA",
        "ECDHE-ECDSA-NULL-SHA",
        "ECDHE-RSA-NULL-SHA",
        "ADH-RC4-MD5",
        "ADH-AES128-SHA",
        "ADH-AES256-SHA",
        "ADH-DES-CBC3-SHA",
        "ADH-DES-CBC-SHA",
        "AECDH-RC4-SHA",
        "AECDH-AES128-SHA",
        "AECDH-AES256-SHA",
        "AECDH-DES-CBC3-SHA",
        "EXP-ADH-RC4-MD5",
        "EXP-ADH-DES-CBC-SHA",
        "AECDH-NULL-SHA",		// TLS1_TXT_ECDH_anon_WITH_NULL_SHA
        "KRB5-RC4-SHA",
        "KRB5-RC4-MD5",
        "KRB5-DES-CBC3-SHA",
        "KRB5-DES-CBC3-MD5",
        "KRB5-DES-CBC-SHA",
        "KRB5-DES-CBC-MD5",
        "EXP-KRB5-RC4-SHA",
        "EXP-KRB5-RC4-MD5"
    };

    @Test
    public void shouldResolveJava6CipherSuites()
        throws Exception {

        for (String ciphersuite : JAVA6_CIPHERSUITES) {
            List<String> configured = new ArrayList<>(1);
            configured.add(ciphersuite);

            List<String> resolved = SslCipherSuites.resolve(configured);
            Assert.assertTrue("Expected ciphersuite, got null", resolved != null);
            Assert.assertTrue(String.format("Expected 1 ciphersuite, got %d", resolved.size()), resolved.size() == 1);
            Assert.assertTrue(String.format("Expected ciphersuite %s, got %s", configured.get(0), resolved.get(0)), resolved.get(0).equals(configured.get(0)));
        }
    }

    // Strengths

    @Test
    public void shouldResolveHighStrength()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("HIGH");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected HIGH cipher suites, got null", resolved != null && resolved.size() != 0);
    }

    @Test
    public void shouldResolveMediumStrength()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("MEDIUM");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected MEDIUM cipher suites, got null", resolved != null && resolved.size() != 0);
    }

    @Test
    public void shouldResolveLowStrength()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("LOW");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected LOW cipher suites, got null", resolved != null && resolved.size() != 0);
    }

    @Test
    public void shouldResolveExport40Strength()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("EXPORT40");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected EXPORT40 cipher suites, got null", resolved != null && resolved.size() != 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldResolveExport56Strength()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("EXPORT56");

        List<String> resolved = SslCipherSuites.resolve(configured);
    }

    // Key exchanges

    @Test
    public void shouldResolveRSAKeyExchange()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("kRSA");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected kRSA cipher suites, got null", resolved != null && resolved.size() != 0);
    }

    // There are no DH key exchanges supported by Java currently
    @Test(expected = IllegalArgumentException.class)
    public void shouldResolveDHKeyExchange()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("kDH");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected kDH cipher suites, got null", resolved != null && resolved.size() != 0);
    }

    @Test
    public void shouldResolveECDHECDSAKeyExchange()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("kECDHe");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected kECDHe cipher suites, got null", resolved != null && resolved.size() != 0);
    }

    @Test
    public void shouldResolveECDHRSAKeyExchange()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("kECDHr");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected kECDHr cipher suites, got null", resolved != null && resolved.size() != 0);
    }

    @Test
    public void shouldResolveECDHEKeyExchange()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("kEECDHe");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected kEECDHe cipher suites, got null", resolved != null && resolved.size() != 0);
    }

    @Test
    public void shouldResolveKRB5KeyExchange()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("kKRB5");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected kKRB5 cipher suites, got null", resolved != null && resolved.size() != 0);
    }

    // Authentications

    @Test
    public void shouldResolveRSAAuthentication()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("aRSA");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected aRSA cipher suites, got null", resolved != null && resolved.size() != 0);
    }

    @Test
    public void shouldResolveDSSAuthentication()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("aDSS");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected aDSS cipher suites, got null", resolved != null && resolved.size() != 0);
    }

    @Test
    public void shouldResolveECDHAuthentication()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("aECDH");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected aECDH cipher suites, got null", resolved != null && resolved.size() != 0);
    }

    @Test
    public void shouldResolveECDSAAuthentication()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("aECDSA");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected aECDSA cipher suites, got null", resolved != null && resolved.size() != 0);
    }

    @Test
    public void shouldResolveKRB5Authentication()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("aKRB5");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected aKRB5 cipher suites, got null", resolved != null && resolved.size() != 0);
    }

    @Test
    public void shouldResolveNullAuthentication()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("aNULL");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected aNULL cipher suites, got null", resolved != null && resolved.size() != 0);
    }

    // Encryptions

    @Test
    public void shouldResolveRC4Encryption()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("RC4");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected RC4 cipher suites, got null", resolved != null && resolved.size() != 0);
    }

    @Test
    public void shouldResolveAES128Encryption()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("AES128");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected AES128 cipher suites, got null", resolved != null && resolved.size() != 0);
    }

    @Test
    public void shouldResolveAES256Encryption()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("AES256");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected AES256 cipher suites, got null", resolved != null && resolved.size() != 0);
    }

    @Test
    public void shouldResolve3DESEncryption()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("3DES");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected 3DES cipher suites, got null", resolved != null && resolved.size() != 0);
    }

    @Test
    public void shouldResolveDESEncryption()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("DES");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected DES cipher suites, got null", resolved != null && resolved.size() != 0);
    }

    @Test
    public void shouldResolveNullEncryption()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("eNULL");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected eNULL cipher suites, got null", resolved != null && resolved.size() != 0);
    }

    // MACs

    @Test
    public void shouldResolveMD5MAC()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("MD5");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected MD5 cipher suites, got null", resolved != null && resolved.size() != 0);
    }

    @Test
    public void shouldResolveSHA1MAC()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("SHA1");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected SHA1 cipher suites, got null", resolved != null && resolved.size() != 0);
    }

    @Test
    public void shouldResolveSHA256MAC()
        throws Exception {

        if (!System.getProperty("java.version").startsWith("1.7.")) {
            return;
        }

        List<String> configured = new ArrayList<>(1);
        configured.add("SHA256");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected SHA256 cipher suites, got null", resolved != null && resolved.size() != 0);
        Assert.assertTrue(String.format("Expected 11 matching ciphers, got %d", resolved.size()), resolved.size() == 11);
    }

    @Test
    public void shouldResolveSHA384MAC()
        throws Exception {

        if (!System.getProperty("java.version").startsWith("1.7.")) {
            return;
        }

        List<String> configured = new ArrayList<>(1);
        configured.add("SHA384");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected SHA384 cipher suites, got null", resolved != null && resolved.size() != 0);
        Assert.assertTrue(String.format("Expected 4 matching ciphers, got %d", resolved.size()), resolved.size() == 4);
    }

    // Protocol versions

    @Test
    public void shouldResolveSSLv3()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("SSLv3");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected SSLv3 cipher suites, got null", resolved != null && resolved.size() != 0);
        Assert.assertTrue(String.format("Expected 19 matching ciphers, got %d", resolved.size()), resolved.size() == 19);
    }

    @Test
    public void shouldResolveTLSv1()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("TLSv1");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected TLSv1 cipher suites, got null", resolved != null && resolved.size() != 0);
        Assert.assertTrue(String.format("Expected 43 matching ciphers, got %d", resolved.size()), resolved.size() == 43);
    }

    // NB: This is expected to fail, since there aren't any ciphersuites
    // specific to TLSv1.1
    @Test(expected = IllegalArgumentException.class)
    public void shouldResolveTLSv11()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("TLSv1.1");

        List<String> resolved = SslCipherSuites.resolve(configured);
    }

    @Test
    public void shouldResolveTLSv12()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("TLSv1.2");

        if (System.getProperty("java.version").startsWith("1.7.")) {
            List<String> resolved = SslCipherSuites.resolve(configured);
            Assert.assertTrue("Expected TLSv1.2 cipher suites, got null", resolved != null && resolved.size() != 0);

            int expected = 15;
            Assert.assertTrue(String.format("Expected %d matching ciphers, got %d", expected, resolved.size()), resolved.size() == expected);

        } else {
            // Java 6 does not support any of the TLSv1.2 ciphers
        }
    }

    // FIPS

    @Test
    public void shouldResolveFIPS()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("FIPS");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected FIPS cipher suites, got null", resolved != null && resolved.size() != 0);

        int expected = 59;
        Assert.assertTrue(String.format("Expected %d matching ciphers, got %d", expected, resolved.size()), resolved.size() == expected);
    }

    // Cipher nicknames

    @Test
    public void shouldResolveCipherNicknames()
        throws Exception {

        for (int i = 0; i < OPENSSL_CIPHER_NICKNAMES.length; i++) {
            String ciphersuite = OPENSSL_CIPHER_NICKNAMES[i];

            List<String> configured = new ArrayList<>(1);
            configured.add(ciphersuite);

            List<String> resolved = SslCipherSuites.resolve(configured);
            Assert.assertTrue("Expected ciphersuite, got null", resolved != null);
            Assert.assertTrue(String.format("Expected 1 ciphersuite, got %d", resolved.size()), resolved.size() == 1);

            String expected = JAVA6_CIPHERSUITES[i];
            Assert.assertTrue(String.format("Expected ciphersuite %s, got %s", expected, resolved.get(0)), resolved.get(0).equals(expected));
        }
    }

    // Group aliases

    @Test(expected = IllegalArgumentException.class)
    public void shouldResolveDHAlias()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("DH");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected DH cipher suites, got null", resolved != null && resolved.size() != 0);
    }

    @Test
    public void shouldResolvekECDHAlias()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("kECDH");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected kECDH cipher suites, got null", resolved != null && resolved.size() != 0);
    }

    @Test
    public void shouldResolveECDHAlias()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("ECDH");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected ECDH cipher suites, got null", resolved != null && resolved.size() != 0);
    }

    @Test
    public void shouldResolveDSSAlias()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("DSS");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected DSS cipher suites, got null", resolved != null && resolved.size() != 0);
    }

    @Test
    public void shouldResolveECDSAAlias()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("ECDSA");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected ECDSA cipher suites, got null", resolved != null && resolved.size() != 0);
    }

    @Test
    public void shouldResolveNullAlias()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("NULL");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected NULL cipher suites, got null", resolved != null && resolved.size() != 0);
    }

    @Test
    public void shouldResolveKRB5Alias()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("KRB5");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected KRB5 cipher suites, got null", resolved != null && resolved.size() != 0);
    }

    @Test
    public void shouldResolveRSAAlias()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("RSA");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected RSA cipher suites, got null", resolved != null && resolved.size() != 0);
    }

    @Test
    public void shouldResolveAESAlias()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("AES");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected AES cipher suites, got null", resolved != null && resolved.size() != 0);
    }

    @Test
    public void shouldResolveRC4Alias()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("RC4");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected RC4 cipher suites, got null", resolved != null && resolved.size() != 0);
    }

    @Test
    public void shouldResolveExpAlias()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("EXP");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected EXP cipher suites, got null", resolved != null && resolved.size() != 0);
    }

    @Test
    public void shouldResolveExportAlias()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("EXPORT");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected EXPORT cipher suites, got null", resolved != null && resolved.size() != 0);
    }

    @Test
    public void shouldResolveEDHAlias()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("EDH");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected EDH cipher suites, got null", resolved != null && resolved.size() != 0);

        int expected = 18;
        Assert.assertTrue(String.format("Expected %d matching ciphers, got %d", expected, resolved.size()), resolved.size() == expected);
    }

    @Test
    public void shouldResolveADHAlias()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("ADH");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected ADH cipher suites, got null", resolved != null && resolved.size() != 0);

        int expected = 60;
        Assert.assertTrue(String.format("Expected %d matching ciphers, got %d", expected, resolved.size()), resolved.size() == expected);
    }

    @Test
    public void shouldResolveAllAlias()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("ALL");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected ALL cipher suites, got null", resolved != null && resolved.size() != 0);

        int expected = 79;
        Assert.assertTrue(String.format("Expected %d matching ciphers, got %d", expected, resolved.size()), resolved.size() == expected);
    }

    @Test
    public void shouldResolveEECDHAlias()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("EECDH");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected EECDH cipher suites, got null", resolved != null && resolved.size() != 0);

        int expected = 7;
        Assert.assertTrue(String.format("Expected %d matching ciphers, got %d", expected, resolved.size()), resolved.size() == expected);
    }

    @Test
    public void shouldResolveAECDHAlias()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("AECDH");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected AECDH cipher suites, got null", resolved != null && resolved.size() != 0);
        Assert.assertTrue(String.format("Expected 5 matching ciphers, got %d", resolved.size()), resolved.size() == 5);
    }

    @Test
    public void shouldResolveSHAAlias()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("SHA");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected SHA cipher suites, got null", resolved != null && resolved.size() != 0);
        Assert.assertTrue(String.format("Expected 52 matching ciphers, got %d", resolved.size()), resolved.size() == 52);
    }

    @Test
    public void shouldResolveComplementofdefaultAlias()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("COMPLEMENTOFDEFAULT");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected COMPLEMENTOFDEFAULT cipher suites, got null", resolved != null && resolved.size() != 0);

        int expected = 25;
        Assert.assertTrue(String.format("Expected %d matching ciphers, got %d", expected, resolved.size()), resolved.size() == expected);
    }

    @Test
    public void shouldResolveComplementofallAlias()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("COMPLEMENTOFALL");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected COMPLEMENTOFALL cipher suites, got null", resolved != null && resolved.size() != 0);

        int expected = 8;
        Assert.assertTrue(String.format("Expected %d matching ciphers, got %d", expected, resolved.size()), resolved.size() == expected);
    }

    // Logical AND
    @Test
    public void shouldResolveLogicalAnd()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("SHA1+RC4");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected SHA1+RC4 cipher suites, got null", resolved != null);

        // The resolved list should include all ciphersuites that use an SHA1
        // MAC AND use RC4 encryption.
        Assert.assertTrue(String.format("Expected 8 matching ciphers, got %d", resolved.size()), resolved.size() == 8);
    }

    // Logical OR
    @Test
    public void shouldResolveLogicalOr()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("SHA1");
        configured.add("RC4");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected SHA1 and RC4 cipher suites, got null", resolved != null);

        // The resolved list should include all ciphersuites that use an SHA1
        // MAC OR use RC4 encryption.
        Assert.assertTrue(String.format("Expected 66 matching ciphers, got %d", resolved.size()), resolved.size() == 66);
    }

    @Test
    public void shouldResolveSortedCiphers()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("SHA1+RC4");
        configured.add("@STRENGTH");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected sorted SHA1+RC4 cipher suites, got null", resolved != null);

        // The resolved list should include all ciphersuites that use an SHA1
        // MAC AND use RC4 encryption.
        Assert.assertTrue(String.format("Expected 8 matching ciphers, got %d", resolved.size()), resolved.size() == 8);

        String expected = "TLS_ECDHE_ECDSA_WITH_RC4_128_SHA";
        Assert.assertTrue(String.format("Expected %s at index 0, got %s", expected, resolved.get(0)), resolved.get(0).equals(expected));

        expected = "TLS_KRB5_EXPORT_WITH_RC4_40_SHA";
        Assert.assertTrue(String.format("Expected %s at index 7, got %s", expected, resolved.get(0)), resolved.get(7).equals(expected));
    }

    @Test
    public void shouldResolveKilledCiphers()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("SHA1+RC4");
        configured.add("!ECDSA");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected SHA1+RC4 (and NOT ECDSA) cipher suites, got null", resolved != null);

        // The resolved list should include all ciphersuites that use an SHA1
        // MAC AND use RC4 encryption.

        Assert.assertTrue(String.format("Expected 7 matching ciphers, got %d", resolved.size()), resolved.size() == 7);
    }

    @Test
    public void shouldResolveKilledExportCiphers()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("RC4");
        configured.add("!EXPORT");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected RC4 (and NOT EXPORT) cipher suites, got null", resolved != null);

        // The resolved list should include all ciphersuites that use RC4
        // encryption which are NOT export-grade.
        Assert.assertTrue(String.format("Expected 10 matching ciphers, got %d", resolved.size()), resolved.size() == 10);
    }

    @Test
    public void shouldResolveRightShiftedCiphers()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("SHA1+RC4");
        configured.add("+RC4");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected SHA1+RC4 (right-shifted RC4) cipher suites, got null", resolved != null);

        Assert.assertTrue(String.format("Expected 8 matching ciphers, got %d", resolved.size()), resolved.size() == 8);
    }

    @Test
    public void shouldResolveRemovedCiphers()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("SHA1");
        configured.add("-RC4");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected SHA1 (removed RC4) cipher suites, got null", resolved != null);

        Assert.assertTrue(String.format("Expected 44 matching ciphers, got %d", resolved.size()), resolved.size() == 44);
    }

    @Test
    public void shouldResolveUnknownName()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("CUSTOM");

        List<String> resolved = SslCipherSuites.resolve(configured);

        Assert.assertTrue("Expected cipher suites, got null", resolved != null);
        Assert.assertTrue(String.format("Expected %s, got %s", configured, resolved), resolved.equals(configured));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailNoMatchingCiphers()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("!FIPS");
        List<String> resolved = SslCipherSuites.resolve(configured);
    }

    @Test
    public void shouldResolveDefaults()
        throws Exception {

        List<String> resolved = SslCipherSuites.resolve(null);
        Assert.assertTrue("Expected default cipher suites, got null", resolved != null);

        int expected = 21;
        Assert.assertTrue(String.format("Expected %d matching ciphers, got %d", expected, resolved.size()), resolved.size() == expected);
    }

    @Test
    public void shouldResolveExplicitDefault()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("DEFAULT");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected default cipher suites, got null", resolved != null);

        int expected = 21;
        Assert.assertTrue(String.format("Expected %d matching ciphers, got %d", expected, resolved.size()), resolved.size() == expected);
    }

    @Test
    public void shouldResolveExplicitDefaultAndNullAlias()
        throws Exception {

        List<String> configured = new ArrayList<>(2);
        configured.add("DEFAULT");
        configured.add("NULL");

        List<String> resolved = SslCipherSuites.resolve(configured);
        Assert.assertTrue("Expected default cipher suites, got null", resolved != null);

        int expected = 26;
        Assert.assertTrue(String.format("Expected %d matching ciphers, got %d", expected, resolved.size()), resolved.size() == expected);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotResolveNullAliasAndExplicitDefault()
        throws Exception {

        List<String> configured = new ArrayList<>(2);
        configured.add("NULL");
        configured.add("DEFAULT");

        List<String> resolved = SslCipherSuites.resolve(configured);
    }

    @Test
    public void shouldResolveSortedUnknownCipher()
        throws Exception {

        List<String> configured = new ArrayList<>(1);
        configured.add("TLS_ECDH_RSA_WITH_AES_256_CBC_SHA");
        configured.add("CUSTOM");
        configured.add("SSL_RSA_WITH_NULL_MD5");
        configured.add("@STRENGTH");

        List<String> resolved = SslCipherSuites.resolve(configured);

        Assert.assertTrue("Expected cipher suites, got null", resolved != null);
        Assert.assertTrue(String.format("Expected 3 matching ciphers, got %d", resolved.size()), resolved.size() == 3);

        String expected = "TLS_ECDH_RSA_WITH_AES_256_CBC_SHA";
        Assert.assertTrue(String.format("Expected cipher #1 to be %s, got %s", expected, resolved.get(0)), resolved.get(0).equals(expected));

        expected = "CUSTOM";
        Assert.assertTrue(String.format("Expected cipher #2 to be %s, got %s", expected, resolved.get(1)), resolved.get(1).equals(expected));

        expected = "SSL_RSA_WITH_NULL_MD5";
        Assert.assertTrue(String.format("Expected cipher #3 to be %s, got %s", expected, resolved.get(2)), resolved.get(2).equals(expected));
    }

    @Test
    public void shouldResolveCSV()
        throws Exception {

        String configured = "HIGH, MEDIUM, !ADH";
        String[] resolved = SslCipherSuites.resolveCSV(configured);

        Assert.assertTrue("Expected cipher suites, got null", resolved != null);

        int expected = 25;
        Assert.assertTrue(String.format("Expected %d matching ciphers, got %d", expected, resolved.length), resolved.length == expected);
    }
}
