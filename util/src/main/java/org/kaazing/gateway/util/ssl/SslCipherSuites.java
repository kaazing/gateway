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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class SslCipherSuites {

    private enum CipherStrength {
        HIGH("HIGH"),
        MEDIUM("MEDIUM"),
        LOW("LOW"),
        EXPORT40("EXPORT40"),

        // OpenSSL provides this token, but it is an empty list
        EXPORT56("EXPORT56"),

        NONE("NONE");

        private final String tokenName;

        CipherStrength(String tokenName) {
            this.tokenName = tokenName;
        }
    }

    private enum CipherKeyExchange {
        RSA("kRSA"),
        DH("kDH"),
        DHE("kEDH"),
        ECDH_ECDSA("kECDHe"),
        ECDH_RSA("kECDHr"),
        ECDHE("kEECDHe"),
        KRB5("kKRB5");

        private final String tokenName;

        CipherKeyExchange(String tokenName) {
            this.tokenName = tokenName;
        }
    }

    private enum CipherAuthentication {
        RSA("aRSA"),
        DSS("aDSS"),
        ECDH("aECDH"),
        ECDSA("aECDSA"),
        KRB5("aKRB5"),
        NULL("aNULL");

        private final String tokenName;

        CipherAuthentication(String tokenName) {
            this.tokenName = tokenName;
        }
    }

    private enum CipherEncryption {
        RC4("RC4"),
        AES128("AES128"),
        AES256("AES256"),
        AESGCM128("AESGCM128"),
        AESGCM256("AESGCM256"),
        TripleDES("3DES"),
        DES("DES"),
        NULL("eNULL");

        private final String tokenName;

        CipherEncryption(String tokenName) {
            this.tokenName = tokenName;
        }
    }

    private enum CipherMAC {
        MD5("MD5"),
        SHA1("SHA1"),
        SHA256("SHA256"),
        SHA384("SHA384"),
        AEAD("AEAD");

        private final String tokenName;

        CipherMAC(String tokenName) {
            this.tokenName = tokenName;
        }
    }

    private enum CipherProtocolVersion {
        SSLV3("SSLv3"),
        TLSV1("TLSv1"),
        TLSV1_1("TLSv1.1"),
        TLSV1_2("TLSv1.2");

        private final String tokenName;

        CipherProtocolVersion(String tokenName) {
            this.tokenName = tokenName;
        }
    }

    private static class SslCipher implements Comparable<SslCipher> {
        final String name;
        private final String nickname;
        private final Integer strengthBits;
        private final CipherStrength strength;
        private final CipherKeyExchange keyExchange;
        private final CipherAuthentication authentication;
        private final CipherEncryption encryption;
        private final CipherMAC mac;
        private final CipherProtocolVersion protocolVersion;
        private final boolean fips;

        SslCipher(String name,
                  String nickname,
                  int strengthBits,
                  CipherStrength strength,
                  CipherKeyExchange keyExchange,
                  CipherAuthentication authentication,
                  CipherEncryption encryption,
                  CipherMAC mac,
                  CipherProtocolVersion protocolVersion,
                  boolean fips) {
            this.name = name;
            this.nickname = nickname;
            this.strengthBits = strengthBits;
            this.strength = strength;
            this.keyExchange = keyExchange;
            this.authentication = authentication;
            this.encryption = encryption;
            this.mac = mac;
            this.protocolVersion = protocolVersion;
            this.fips = fips;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o instanceof SslCipher) {
                SslCipher c = (SslCipher) o;
                return name.equals(c.name);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public int compareTo(SslCipher o) {
            // To sort the ciphers in DESCENDING order of bits, we need to
            // invert the normal comparison
            return strengthBits.compareTo(o.strengthBits) * -1;
        }

        @Override
        public String toString() {
            return String.format("%s:[%s/%s/%s/%s/%s/%s(%d)%s]", name,
                keyExchange, authentication, encryption,
                mac, protocolVersion, strength,
                strengthBits, fips ? "/FIPS" : "");
        }
    }

    // Used as a placeholder for non-SunJSSEProvider ciphers; we override
    // compareTo() so that sorting by strength keeps these ciphers in their
    // configured place in the list.
    private static class OtherSslCipher
        extends SslCipher {

        OtherSslCipher(String name) {
            super(name, null, 0, null, null, null, null, null, null, false);
        }

        @Override
        public int compareTo(SslCipher o) {
            return 0;
        }
    }

    private enum CipherOp {
        ADD,        // default
        KILL,        // '!' prefix
        RIGHT_SHIFT,    // '+' prefix
        REMOVE        // '-' prefix
    }

    // These mappings are based on the groupings used by OpenSSL; see
    //   openssl/ssl/ssl.h
    //   openssl/ssl/tls1.h
    //   openssl/ssl/s3_lib.c.
    //   openssl/ssl/ssl_locl.h

    // Map of the ciphersuite name (as appearing in IETF specs) to
    // various details about that ciphersuite
    private static final Map<String, SslCipher> CIPHERS;

    // Map of the ciphersuite name (as defined as a nickname in OpenSSL)
    private static final Map<String, SslCipher> CIPHER_NICKNAMES;
    static {
        Map<String, SslCipher> ciphers = new HashMap<>();
        Map<String, SslCipher> cipherNicknames = new HashMap<>();

        initJava8SunDefaultEnabledCipherSuites(ciphers, cipherNicknames);
        initJava8SunDefaultDisabledCipherSuites(ciphers, cipherNicknames);

        CIPHERS = Collections.unmodifiableMap(ciphers);
        CIPHER_NICKNAMES = Collections.unmodifiableMap(cipherNicknames);
    }

    private static final Map<String, List<SslCipher>> CIPHER_STRENGTHS;
    static {
        Map<String, List<SslCipher>> cipherStrengths = CIPHERS.values()
                .stream()
                .collect(Collectors.groupingBy(s -> s.strength.tokenName));

        Stream.of(CipherStrength.values())
                .forEach(s -> cipherStrengths.putIfAbsent(s.tokenName, Collections.emptyList()));

        CIPHER_STRENGTHS = Collections.unmodifiableMap(cipherStrengths);
    }

    private static final Map<String, List<SslCipher>> CIPHER_KEY_EXCHANGES;
    static {
        Map<String, List<SslCipher>> cipherKeyExchanges = CIPHERS.values()
                .stream()
                .collect(Collectors.groupingBy(s -> s.keyExchange.tokenName));

        Stream.of(CipherKeyExchange.values())
                .forEach(s -> cipherKeyExchanges.putIfAbsent(s.tokenName, Collections.emptyList()));

        CIPHER_KEY_EXCHANGES = Collections.unmodifiableMap(cipherKeyExchanges);
    }

    private static final Map<String, List<SslCipher>> CIPHER_AUTHNS;
    static {
        Map<String, List<SslCipher>> cipherAuthns = CIPHERS.values()
                .stream()
                .collect(Collectors.groupingBy(s -> s.authentication.tokenName));

        Stream.of(CipherAuthentication.values())
                .forEach(s -> cipherAuthns.putIfAbsent(s.tokenName, Collections.emptyList()));

        CIPHER_AUTHNS = Collections.unmodifiableMap(cipherAuthns);
    }

    private static final Map<String, List<SslCipher>> CIPHER_ENCRYPTS;
    static {
        Map<String, List<SslCipher>> cipherEncrypts = CIPHERS.values()
                .stream()
                .collect(Collectors.groupingBy(s -> s.encryption.tokenName));

        Stream.of(CipherEncryption.values())
                .forEach(s -> cipherEncrypts.putIfAbsent(s.tokenName, Collections.emptyList()));

        CIPHER_ENCRYPTS = Collections.unmodifiableMap(cipherEncrypts);
    }

    private static final Map<String, List<SslCipher>> CIPHER_MACS;
    static {
        Map<String, List<SslCipher>> cipherMacs = CIPHERS.values()
                .stream()
                .collect(Collectors.groupingBy(s -> s.mac.tokenName));

        Stream.of(CipherMAC.values())
                .forEach(s -> cipherMacs.putIfAbsent(s.tokenName, Collections.emptyList()));

        CIPHER_MACS = Collections.unmodifiableMap(cipherMacs);
    }

    private static final Map<String, List<SslCipher>> CIPHER_PROTOCOLS;
    static {
        Map<String, List<SslCipher>> cipherProtocols = CIPHERS.values()
                .stream()
                .collect(Collectors.groupingBy(s -> s.protocolVersion.tokenName));

        Stream.of(CipherProtocolVersion.values())
                .forEach(s -> cipherProtocols.putIfAbsent(s.tokenName, Collections.emptyList()));

        CIPHER_PROTOCOLS = Collections.unmodifiableMap(cipherProtocols);
    }

    private static final List<SslCipher> CIPHER_FIPS;
    static {
        List<SslCipher> fips = CIPHERS.values()
                .stream()
                .filter(s -> s.fips)
                .collect(Collectors.toList());

        CIPHER_FIPS = Collections.unmodifiableList(fips);
    }

    // Java 8 Sun provider's default enabled cipher suites
    // https://docs.oracle.com/javase/8/docs/technotes/guides/security/SunProviders.html
    private static void initJava8SunDefaultEnabledCipherSuites(Map<String, SslCipher> ciphers,
                                                 Map<String, SslCipher> ciphersByNickname) {
        SslCipher cipher;

        // TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384
        cipher = new SslCipher("TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
                "ECDHE-ECDSA-AES256-SHA384", 256,
                CipherStrength.HIGH,
                CipherKeyExchange.ECDHE,
                CipherAuthentication.ECDSA,
                CipherEncryption.AES256,
                CipherMAC.SHA384,
                CipherProtocolVersion.TLSV1_2,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384
        cipher = new SslCipher("TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384",
                "ECDHE-RSA-AES256-SHA384", 256,
                CipherStrength.HIGH,
                CipherKeyExchange.ECDHE,
                CipherAuthentication.RSA,
                CipherEncryption.AES256,
                CipherMAC.SHA384,
                CipherProtocolVersion.TLSV1_2,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_RSA_WITH_AES_256_CBC_SHA256
        cipher = new SslCipher("TLS_RSA_WITH_AES_256_CBC_SHA256",
                "AES256-SHA256", 256,
                CipherStrength.HIGH,
                CipherKeyExchange.RSA,
                CipherAuthentication.RSA,
                CipherEncryption.AES256,
                CipherMAC.SHA256,
                CipherProtocolVersion.TLSV1_2,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA384
        cipher = new SslCipher("TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA384",
                "ECDH-ECDSA-AES256-SHA384", 256,
                CipherStrength.HIGH,
                CipherKeyExchange.ECDH_ECDSA,
                CipherAuthentication.ECDH,
                CipherEncryption.AES256,
                CipherMAC.SHA384,
                CipherProtocolVersion.TLSV1_2,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_ECDH_RSA_WITH_AES_256_CBC_SHA384
        cipher = new SslCipher("TLS_ECDH_RSA_WITH_AES_256_CBC_SHA384",
                "ECDH-RSA-AES256-SHA384", 256,
                CipherStrength.HIGH,
                CipherKeyExchange.ECDH_RSA,
                CipherAuthentication.ECDH,
                CipherEncryption.AES256,
                CipherMAC.SHA384,
                CipherProtocolVersion.TLSV1_2,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_DHE_RSA_WITH_AES_256_CBC_SHA256
        cipher = new SslCipher("TLS_DHE_RSA_WITH_AES_256_CBC_SHA256",
                "DHE-RSA-AES256-SHA256", 256,
                CipherStrength.HIGH,
                CipherKeyExchange.DHE,
                CipherAuthentication.RSA,
                CipherEncryption.AES256,
                CipherMAC.SHA256,
                CipherProtocolVersion.TLSV1_2,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_DHE_DSS_WITH_AES_256_CBC_SHA256
        cipher = new SslCipher("TLS_DHE_DSS_WITH_AES_256_CBC_SHA256",
                "DHE-DSS-AES256-SHA256", 256,
                CipherStrength.HIGH,
                CipherKeyExchange.DHE,
                CipherAuthentication.DSS,
                CipherEncryption.AES256,
                CipherMAC.SHA256,
                CipherProtocolVersion.TLSV1_2,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA
        cipher = new SslCipher("TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
                "ECDHE-ECDSA-AES256-SHA", 256,
                CipherStrength.HIGH,
                CipherKeyExchange.ECDHE,
                CipherAuthentication.ECDSA,
                CipherEncryption.AES256,
                CipherMAC.SHA1,
                CipherProtocolVersion.TLSV1,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA
        cipher = new SslCipher("TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
                "ECDHE-RSA-AES256-SHA", 256,
                CipherStrength.HIGH,
                CipherKeyExchange.ECDHE,
                CipherAuthentication.RSA,
                CipherEncryption.AES256,
                CipherMAC.SHA1,
                CipherProtocolVersion.TLSV1,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_RSA_WITH_AES_256_CBC_SHA
        cipher = new SslCipher("TLS_RSA_WITH_AES_256_CBC_SHA",
                "AES256-SHA", 256,
                CipherStrength.HIGH,
                CipherKeyExchange.RSA,
                CipherAuthentication.RSA,
                CipherEncryption.AES256,
                CipherMAC.SHA1,
                CipherProtocolVersion.TLSV1,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA
        cipher = new SslCipher("TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA",
                "ECDH-ECDSA-AES256-SHA", 256,
                CipherStrength.HIGH,
                CipherKeyExchange.ECDH_ECDSA,
                CipherAuthentication.ECDH,
                CipherEncryption.AES256,
                CipherMAC.SHA1,
                CipherProtocolVersion.TLSV1,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_ECDH_RSA_WITH_AES_256_CBC_SHA
        cipher = new SslCipher("TLS_ECDH_RSA_WITH_AES_256_CBC_SHA",
                "ECDH-RSA-AES256-SHA", 256,
                CipherStrength.HIGH,
                CipherKeyExchange.ECDH_RSA,
                CipherAuthentication.ECDH,
                CipherEncryption.AES256,
                CipherMAC.SHA1,
                CipherProtocolVersion.TLSV1,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_DHE_RSA_WITH_AES_256_CBC_SHA
        cipher = new SslCipher("TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
                "DHE-RSA-AES256-SHA", 256,
                CipherStrength.HIGH,
                CipherKeyExchange.DHE,
                CipherAuthentication.RSA,
                CipherEncryption.AES256,
                CipherMAC.SHA1,
                CipherProtocolVersion.TLSV1,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_DHE_DSS_WITH_AES_256_CBC_SHA
        cipher = new SslCipher("TLS_DHE_DSS_WITH_AES_256_CBC_SHA",
                "DHE-DSS-AES256-SHA", 256,
                CipherStrength.HIGH,
                CipherKeyExchange.DHE,
                CipherAuthentication.DSS,
                CipherEncryption.AES256,
                CipherMAC.SHA1,
                CipherProtocolVersion.TLSV1,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256
        cipher = new SslCipher("TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
                "ECDHE-ECDSA-AES128-SHA256", 128,
                CipherStrength.HIGH,
                CipherKeyExchange.ECDHE,
                CipherAuthentication.ECDSA,
                CipherEncryption.AES128,
                CipherMAC.SHA256,
                CipherProtocolVersion.TLSV1_2,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256
        cipher = new SslCipher("TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
                "ECDHE-RSA-AES128-SHA256", 128,
                CipherStrength.HIGH,
                CipherKeyExchange.ECDHE,
                CipherAuthentication.RSA,
                CipherEncryption.AES128,
                CipherMAC.SHA256,
                CipherProtocolVersion.TLSV1_2,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_RSA_WITH_AES_128_CBC_SHA256
        cipher = new SslCipher("TLS_RSA_WITH_AES_128_CBC_SHA256",
                "AES128-SHA256", 128,
                CipherStrength.HIGH,
                CipherKeyExchange.RSA,
                CipherAuthentication.RSA,
                CipherEncryption.AES128,
                CipherMAC.SHA256,
                CipherProtocolVersion.TLSV1_2,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256
        cipher = new SslCipher("TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256",
                "ECDH-ECDSA-AES128-SHA256", 128,
                CipherStrength.HIGH,
                CipherKeyExchange.ECDH_ECDSA,
                CipherAuthentication.ECDH,
                CipherEncryption.AES128,
                CipherMAC.SHA256,
                CipherProtocolVersion.TLSV1_2,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256
        cipher = new SslCipher("TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256",
                "ECDH-RSA-AES128-SHA256", 128,
                CipherStrength.HIGH,
                CipherKeyExchange.ECDH_RSA,
                CipherAuthentication.ECDH,
                CipherEncryption.AES128,
                CipherMAC.SHA256,
                CipherProtocolVersion.TLSV1_2,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_DHE_RSA_WITH_AES_128_CBC_SHA256
        cipher = new SslCipher("TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
                "DHE-RSA-AES128-SHA256", 128,
                CipherStrength.HIGH,
                CipherKeyExchange.DHE,
                CipherAuthentication.RSA,
                CipherEncryption.AES128,
                CipherMAC.SHA256,
                CipherProtocolVersion.TLSV1_2,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_DHE_DSS_WITH_AES_128_CBC_SHA256
        cipher = new SslCipher("TLS_DHE_DSS_WITH_AES_128_CBC_SHA256",
                "DHE-DSS-AES128-SHA256", 128,
                CipherStrength.HIGH,
                CipherKeyExchange.DHE,
                CipherAuthentication.DSS,
                CipherEncryption.AES128,
                CipherMAC.SHA256,
                CipherProtocolVersion.TLSV1_2,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA
        cipher = new SslCipher("TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
                "ECDHE-ECDSA-AES128-SHA", 128,
                CipherStrength.HIGH,
                CipherKeyExchange.ECDHE,
                CipherAuthentication.ECDSA,
                CipherEncryption.AES128,
                CipherMAC.SHA1,
                CipherProtocolVersion.TLSV1,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA
        cipher = new SslCipher("TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
                "ECDHE-RSA-AES128-SHA", 128,
                CipherStrength.HIGH,
                CipherKeyExchange.ECDHE,
                CipherAuthentication.RSA,
                CipherEncryption.AES128,
                CipherMAC.SHA1,
                CipherProtocolVersion.TLSV1,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);


        // TLS_RSA_WITH_AES_128_CBC_SHA
        cipher = new SslCipher("TLS_RSA_WITH_AES_128_CBC_SHA",
                "AES128-SHA", 128,
                CipherStrength.HIGH,
                CipherKeyExchange.RSA,
                CipherAuthentication.RSA,
                CipherEncryption.AES128,
                CipherMAC.SHA1,
                CipherProtocolVersion.TLSV1,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);


        // TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA
        cipher = new SslCipher("TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA",
                "ECDH-ECDSA-AES128-SHA", 128,
                CipherStrength.HIGH,
                CipherKeyExchange.ECDH_ECDSA,
                CipherAuthentication.ECDH,
                CipherEncryption.AES128,
                CipherMAC.SHA1,
                CipherProtocolVersion.TLSV1,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_ECDH_RSA_WITH_AES_128_CBC_SHA
        cipher = new SslCipher("TLS_ECDH_RSA_WITH_AES_128_CBC_SHA",
                "ECDH-RSA-AES128-SHA", 128,
                CipherStrength.HIGH,
                CipherKeyExchange.ECDH_RSA,
                CipherAuthentication.ECDH,
                CipherEncryption.AES128,
                CipherMAC.SHA1,
                CipherProtocolVersion.TLSV1,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_DHE_RSA_WITH_AES_128_CBC_SHA
        cipher = new SslCipher("TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
                "DHE-RSA-AES128-SHA", 128,
                CipherStrength.HIGH,
                CipherKeyExchange.DHE,
                CipherAuthentication.RSA,
                CipherEncryption.AES128,
                CipherMAC.SHA1,
                CipherProtocolVersion.TLSV1,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_DHE_DSS_WITH_AES_128_CBC_SHA
        cipher = new SslCipher("TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
                "DHE-DSS-AES128-SHA", 128,
                CipherStrength.HIGH,
                CipherKeyExchange.DHE,
                CipherAuthentication.DSS,
                CipherEncryption.AES128,
                CipherMAC.SHA1,
                CipherProtocolVersion.TLSV1,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_ECDHE_ECDSA_WITH_RC4_128_SHA
        cipher = new SslCipher("TLS_ECDHE_ECDSA_WITH_RC4_128_SHA",
                "ECDHE-ECDSA-RC4-SHA", 128,
                CipherStrength.MEDIUM,
                CipherKeyExchange.ECDHE,
                CipherAuthentication.ECDSA,
                CipherEncryption.RC4,
                CipherMAC.SHA1,
                CipherProtocolVersion.TLSV1,
                false);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_ECDHE_RSA_WITH_RC4_128_SHA
        cipher = new SslCipher("TLS_ECDHE_RSA_WITH_RC4_128_SHA",
                "ECDHE-RSA-RC4-SHA", 128,
                CipherStrength.MEDIUM,
                CipherKeyExchange.ECDHE,
                CipherAuthentication.RSA,
                CipherEncryption.RC4,
                CipherMAC.SHA1,
                CipherProtocolVersion.TLSV1,
                false);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // SSL_RSA_WITH_RC4_128_SHA
        cipher = new SslCipher("SSL_RSA_WITH_RC4_128_SHA",
                "RC4-SHA", 128,
                CipherStrength.MEDIUM,
                CipherKeyExchange.RSA,
                CipherAuthentication.RSA,
                CipherEncryption.RC4,
                CipherMAC.SHA1,
                CipherProtocolVersion.SSLV3,
                false);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_ECDH_ECDSA_WITH_RC4_128_SHA
        cipher = new SslCipher("TLS_ECDH_ECDSA_WITH_RC4_128_SHA",
                "ECDH-ECDSA-RC4-SHA", 128,
                CipherStrength.MEDIUM,
                CipherKeyExchange.ECDH_ECDSA,
                CipherAuthentication.ECDH,
                CipherEncryption.RC4,
                CipherMAC.SHA1,
                CipherProtocolVersion.TLSV1,
                false);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_ECDH_RSA_WITH_RC4_128_SHA
        cipher = new SslCipher("TLS_ECDH_RSA_WITH_RC4_128_SHA",
                "ECDH-RSA-RC4-SHA", 128,
                CipherStrength.MEDIUM,
                CipherKeyExchange.ECDH_RSA,
                CipherAuthentication.ECDH,
                CipherEncryption.RC4,
                CipherMAC.SHA1,
                CipherProtocolVersion.TLSV1,
                false);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384
        cipher = new SslCipher("TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                "ECDHE-ECDSA-AES256-GCM-SHA384", 256,
                CipherStrength.HIGH,
                CipherKeyExchange.ECDHE,
                CipherAuthentication.ECDSA,
                CipherEncryption.AESGCM256,
                CipherMAC.AEAD,
                CipherProtocolVersion.TLSV1_2,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256
        cipher = new SslCipher("TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                "ECDHE-ECDSA-AES128-GCM-SHA256", 128,
                CipherStrength.HIGH,
                CipherKeyExchange.ECDHE,
                CipherAuthentication.ECDSA,
                CipherEncryption.AESGCM128,
                CipherMAC.SHA256,
                CipherProtocolVersion.TLSV1_2,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384
        cipher = new SslCipher("TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                "ECDHE-RSA-AES256-GCM-SHA384", 256,
                CipherStrength.HIGH,
                CipherKeyExchange.ECDHE,
                CipherAuthentication.RSA,
                CipherEncryption.AESGCM256,
                CipherMAC.AEAD,
                CipherProtocolVersion.TLSV1_2,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_RSA_WITH_AES_256_GCM_SHA384
        cipher = new SslCipher("TLS_RSA_WITH_AES_256_GCM_SHA384",
                "AES256-GCM-SHA384", 256,
                CipherStrength.HIGH,
                CipherKeyExchange.RSA,
                CipherAuthentication.RSA,
                CipherEncryption.AESGCM256,
                CipherMAC.AEAD,
                CipherProtocolVersion.TLSV1_2,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_ECDH_ECDSA_WITH_AES_256_GCM_SHA384
        // openssl doesn't have it

        // TLS_ECDH_RSA_WITH_AES_256_GCM_SHA384
        // openssl doesn't have it

        // TLS_DHE_RSA_WITH_AES_256_GCM_SHA384
        cipher = new SslCipher("TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
                "DHE-RSA-AES256-GCM-SHA384", 256,
                CipherStrength.HIGH,
                CipherKeyExchange.DHE,
                CipherAuthentication.RSA,
                CipherEncryption.AESGCM256,
                CipherMAC.AEAD,
                CipherProtocolVersion.TLSV1_2,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_DHE_DSS_WITH_AES_256_GCM_SHA384
        cipher = new SslCipher("TLS_DHE_DSS_WITH_AES_256_GCM_SHA384",
                "DHE-DSS-AES256-GCM-SHA384", 256,
                CipherStrength.HIGH,
                CipherKeyExchange.DHE,
                CipherAuthentication.DSS,
                CipherEncryption.AESGCM256,
                CipherMAC.AEAD,
                CipherProtocolVersion.TLSV1_2,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256
        cipher = new SslCipher("TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                "ECDHE-RSA-AES128-GCM-SHA256", 128,
                CipherStrength.HIGH,
                CipherKeyExchange.ECDHE,
                CipherAuthentication.RSA,
                CipherEncryption.AESGCM128,
                CipherMAC.AEAD,
                CipherProtocolVersion.TLSV1_2,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_RSA_WITH_AES_128_GCM_SHA256
        cipher = new SslCipher("TLS_RSA_WITH_AES_128_GCM_SHA256",
                "AES128-GCM-SHA256", 128,
                CipherStrength.HIGH,
                CipherKeyExchange.RSA,
                CipherAuthentication.RSA,
                CipherEncryption.AESGCM128,
                CipherMAC.AEAD,
                CipherProtocolVersion.TLSV1_2,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_ECDH_ECDSA_WITH_AES_128_GCM_SHA256
        // openssl doesn't have it

        // TLS_ECDH_RSA_WITH_AES_128_GCM_SHA256
        // openssl doesn't have it

        // TLS_DHE_RSA_WITH_AES_128_GCM_SHA256
        cipher = new SslCipher("TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
                "DHE-RSA-AES128-GCM-SHA256", 128,
                CipherStrength.HIGH,
                CipherKeyExchange.DHE,
                CipherAuthentication.RSA,
                CipherEncryption.AESGCM128,
                CipherMAC.AEAD,
                CipherProtocolVersion.TLSV1_2,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_DHE_DSS_WITH_AES_128_GCM_SHA256
        cipher = new SslCipher("TLS_DHE_DSS_WITH_AES_128_GCM_SHA256",
                "DHE-DSS-AES128-GCM-SHA256", 128,
                CipherStrength.HIGH,
                CipherKeyExchange.DHE,
                CipherAuthentication.DSS,
                CipherEncryption.AESGCM128,
                CipherMAC.AEAD,
                CipherProtocolVersion.TLSV1_2,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA
        cipher = new SslCipher("TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA",
                "ECDHE-ECDSA-DES-CBC3-SHA", 168,
                CipherStrength.HIGH,
                CipherKeyExchange.ECDHE,
                CipherAuthentication.ECDSA,
                CipherEncryption.TripleDES,
                CipherMAC.SHA1,
                CipherProtocolVersion.TLSV1,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA
        cipher = new SslCipher("TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA",
                "ECDHE-RSA-DES-CBC3-SHA", 168,
                CipherStrength.HIGH,
                CipherKeyExchange.ECDHE,
                CipherAuthentication.RSA,
                CipherEncryption.TripleDES,
                CipherMAC.SHA1,
                CipherProtocolVersion.TLSV1,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // SSL_RSA_WITH_3DES_EDE_CBC_SHA
        cipher = new SslCipher("SSL_RSA_WITH_3DES_EDE_CBC_SHA",
                "DES-CBC3-SHA", 168,
                CipherStrength.HIGH,
                CipherKeyExchange.RSA,
                CipherAuthentication.RSA,
                CipherEncryption.TripleDES,
                CipherMAC.SHA1,
                CipherProtocolVersion.SSLV3,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA
        cipher = new SslCipher("TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA",
                "ECDH-ECDSA-DES-CBC3-SHA", 168,
                CipherStrength.HIGH,
                CipherKeyExchange.ECDH_ECDSA,
                CipherAuthentication.ECDH,
                CipherEncryption.TripleDES,
                CipherMAC.SHA1,
                CipherProtocolVersion.TLSV1,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA
        cipher = new SslCipher("TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA",
                "ECDH-RSA-DES-CBC3-SHA", 168,
                CipherStrength.HIGH,
                CipherKeyExchange.ECDH_RSA,
                CipherAuthentication.ECDH,
                CipherEncryption.TripleDES,
                CipherMAC.SHA1,
                CipherProtocolVersion.TLSV1,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA
        cipher = new SslCipher("SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA",
                "EDH-RSA-DES-CBC3-SHA", 168,
                CipherStrength.HIGH,
                CipherKeyExchange.DHE,
                CipherAuthentication.RSA,
                CipherEncryption.TripleDES,
                CipherMAC.SHA1,
                CipherProtocolVersion.SSLV3,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA
        cipher = new SslCipher("SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA",
                "EDH-DSS-DES-CBC3-SHA", 168,
                CipherStrength.HIGH,
                CipherKeyExchange.DHE,
                CipherAuthentication.DSS,
                CipherEncryption.TripleDES,
                CipherMAC.SHA1,
                CipherProtocolVersion.SSLV3,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // SSL_RSA_WITH_RC4_128_MD5
        cipher = new SslCipher("SSL_RSA_WITH_RC4_128_MD5",
                "RC4-MD5", 128,
                CipherStrength.MEDIUM,
                CipherKeyExchange.RSA,
                CipherAuthentication.RSA,
                CipherEncryption.RC4,
                CipherMAC.MD5,
                CipherProtocolVersion.SSLV3,
                false);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_EMPTY_RENEGOTIATION_INFO_SCSV
        // openssl doesn't have it
    }

    // Java 8 Sun provider's default disabled cipher suites
    // https://docs.oracle.com/javase/8/docs/technotes/guides/security/SunProviders.html
    private static void initJava8SunDefaultDisabledCipherSuites(Map<String, SslCipher> ciphers,
                                                               Map<String, SslCipher> ciphersByNickname) {
        SslCipher cipher;

        // TLS_DH_anon_WITH_AES_256_GCM_SHA384
        // TODO by default it is disabled so not adding it for now

        // TLS_DH_anon_WITH_AES_128_GCM_SHA256
        // TODO by default it is disabled so not adding it for now

        // TLS_DH_anon_WITH_AES_256_CBC_SHA256
        // TODO by default it is disabled so not adding it for now

        // TLS_ECDH_anon_WITH_AES_256_CBC_SHA
        cipher = new SslCipher("TLS_ECDH_anon_WITH_AES_256_CBC_SHA",
                "AECDH-AES256-SHA", 256,
                CipherStrength.HIGH,
                CipherKeyExchange.ECDHE,
                CipherAuthentication.NULL,
                CipherEncryption.AES256,
                CipherMAC.SHA1,
                CipherProtocolVersion.TLSV1,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_DH_anon_WITH_AES_256_CBC_SHA
        cipher = new SslCipher("TLS_DH_anon_WITH_AES_256_CBC_SHA",
                "ADH-AES256-SHA", 256,
                CipherStrength.HIGH,
                CipherKeyExchange.DHE,
                CipherAuthentication.NULL,
                CipherEncryption.AES256,
                CipherMAC.SHA1,
                CipherProtocolVersion.TLSV1,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_DH_anon_WITH_AES_128_CBC_SHA256
        // TODO by default it is disabled so not adding it for now

        // TLS_ECDH_anon_WITH_AES_128_CBC_SHA
        cipher = new SslCipher("TLS_ECDH_anon_WITH_AES_128_CBC_SHA",
                "AECDH-AES128-SHA", 128,
                CipherStrength.HIGH,
                CipherKeyExchange.ECDHE,
                CipherAuthentication.NULL,
                CipherEncryption.AES128,
                CipherMAC.SHA1,
                CipherProtocolVersion.TLSV1,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_DH_anon_WITH_AES_128_CBC_SHA
        cipher = new SslCipher("TLS_DH_anon_WITH_AES_128_CBC_SHA",
                "ADH-AES128-SHA", 128,
                CipherStrength.HIGH,
                CipherKeyExchange.DHE,
                CipherAuthentication.NULL,
                CipherEncryption.AES128,
                CipherMAC.SHA1,
                CipherProtocolVersion.TLSV1,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_ECDH_anon_WITH_RC4_128_SHA
        cipher = new SslCipher("TLS_ECDH_anon_WITH_RC4_128_SHA",
                "AECDH-RC4-SHA", 128,
                CipherStrength.MEDIUM,
                CipherKeyExchange.ECDHE,
                CipherAuthentication.NULL,
                CipherEncryption.RC4,
                CipherMAC.SHA1,
                CipherProtocolVersion.TLSV1,
                false);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // SSL_DH_anon_WITH_RC4_128_MD5
        cipher = new SslCipher("SSL_DH_anon_WITH_RC4_128_MD5",
                "ADH-RC4-MD5", 128,
                CipherStrength.MEDIUM,
                CipherKeyExchange.DHE,
                CipherAuthentication.NULL,
                CipherEncryption.RC4,
                CipherMAC.MD5,
                CipherProtocolVersion.SSLV3,
                false);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_ECDH_anon_WITH_3DES_EDE_CBC_SHA
        cipher = new SslCipher("TLS_ECDH_anon_WITH_3DES_EDE_CBC_SHA",
                "AECDH-DES-CBC3-SHA", 168,
                CipherStrength.HIGH,
                CipherKeyExchange.ECDHE,
                CipherAuthentication.NULL,
                CipherEncryption.TripleDES,
                CipherMAC.SHA1,
                CipherProtocolVersion.TLSV1,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // SSL_DH_anon_WITH_3DES_EDE_CBC_SHA
        cipher = new SslCipher("SSL_DH_anon_WITH_3DES_EDE_CBC_SHA",
                "ADH-DES-CBC3-SHA", 168,
                CipherStrength.HIGH,
                CipherKeyExchange.DHE,
                CipherAuthentication.NULL,
                CipherEncryption.TripleDES,
                CipherMAC.SHA1,
                CipherProtocolVersion.SSLV3,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_RSA_WITH_NULL_SHA256
        cipher = new SslCipher("TLS_RSA_WITH_NULL_SHA256",
                "NULL-SHA256", 0,
                CipherStrength.NONE,
                CipherKeyExchange.RSA,
                CipherAuthentication.RSA,
                CipherEncryption.NULL,
                CipherMAC.SHA256,
                CipherProtocolVersion.TLSV1_2,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_ECDHE_ECDSA_WITH_NULL_SHA
        //
        // Note: Surprising, this NULL ciphersuite IS FIPS-approved
        cipher = new SslCipher("TLS_ECDHE_ECDSA_WITH_NULL_SHA",
                "ECDHE-ECDSA-NULL-SHA", 0,
                CipherStrength.NONE,
                CipherKeyExchange.ECDHE,
                CipherAuthentication.ECDH,
                CipherEncryption.NULL,
                CipherMAC.SHA1,
                CipherProtocolVersion.TLSV1,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_ECDHE_RSA_WITH_NULL_SHA
        //
        // Note: Surprising, this NULL ciphersuite IS FIPS-approved
        cipher = new SslCipher("TLS_ECDHE_RSA_WITH_NULL_SHA",
                "ECDHE-RSA-NULL-SHA", 0,
                CipherStrength.NONE,
                CipherKeyExchange.ECDHE,
                CipherAuthentication.ECDH,
                CipherEncryption.NULL,
                CipherMAC.SHA1,
                CipherProtocolVersion.TLSV1,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // SSL_RSA_WITH_NULL_SHA
        cipher = new SslCipher("SSL_RSA_WITH_NULL_SHA",
                "NULL-SHA", 0,
                CipherStrength.NONE,
                CipherKeyExchange.RSA,
                CipherAuthentication.RSA,
                CipherEncryption.NULL,
                CipherMAC.SHA1,
                CipherProtocolVersion.SSLV3,
                false);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_ECDH_ECDSA_WITH_NULL_SHA
        //
        // Note: Surprising, this NULL ciphersuite IS FIPS-approved
        cipher = new SslCipher("TLS_ECDH_ECDSA_WITH_NULL_SHA",
                "ECDH-ECDSA-NULL-SHA", 0,
                CipherStrength.NONE,
                CipherKeyExchange.ECDH_ECDSA,
                CipherAuthentication.ECDH,
                CipherEncryption.NULL,
                CipherMAC.SHA1,
                CipherProtocolVersion.TLSV1,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_ECDH_RSA_WITH_NULL_SHA
        //
        // Note: Surprising, this NULL ciphersuite IS FIPS-approved
        cipher = new SslCipher("TLS_ECDH_RSA_WITH_NULL_SHA",
                "ECDH-RSA-NULL-SHA", 0,
                CipherStrength.NONE,
                CipherKeyExchange.ECDH_RSA,
                CipherAuthentication.ECDH,
                CipherEncryption.NULL,
                CipherMAC.SHA1,
                CipherProtocolVersion.TLSV1,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_ECDH_anon_WITH_NULL_SHA
        //
        // Note: Surprising, this NULL ciphersuite IS FIPS-approved
        cipher = new SslCipher("TLS_ECDH_anon_WITH_NULL_SHA",
                "AECDH-NULL-SHA", 0,
                CipherStrength.NONE,
                CipherKeyExchange.ECDHE,
                CipherAuthentication.NULL,
                CipherEncryption.NULL,
                CipherMAC.SHA1,
                CipherProtocolVersion.TLSV1,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // SSL_RSA_WITH_NULL_MD5
        cipher = new SslCipher("SSL_RSA_WITH_NULL_MD5",
                "NULL-MD5", 0,
                CipherStrength.NONE,
                CipherKeyExchange.RSA,
                CipherAuthentication.RSA,
                CipherEncryption.NULL,
                CipherMAC.MD5,
                CipherProtocolVersion.SSLV3,
                false);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // SSL_RSA_WITH_DES_CBC_SHA
        cipher = new SslCipher("SSL_RSA_WITH_DES_CBC_SHA",
                "DES-CBC-SHA", 56,
                CipherStrength.LOW,
                CipherKeyExchange.RSA,
                CipherAuthentication.RSA,
                CipherEncryption.DES,
                CipherMAC.SHA1,
                CipherProtocolVersion.SSLV3,
                false);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // SSL_DHE_RSA_WITH_DES_CBC_SHA
        cipher = new SslCipher("SSL_DHE_RSA_WITH_DES_CBC_SHA",
                "EDH-RSA-DES-CBC-SHA", 56,
                CipherStrength.LOW,
                CipherKeyExchange.DHE,
                CipherAuthentication.RSA,
                CipherEncryption.DES,
                CipherMAC.SHA1,
                CipherProtocolVersion.SSLV3,
                false);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // SSL_DHE_DSS_WITH_DES_CBC_SHA
        cipher = new SslCipher("SSL_DHE_DSS_WITH_DES_CBC_SHA",
                "EDH-DSS-DES-CBC-SHA", 56,
                CipherStrength.LOW,
                CipherKeyExchange.DHE,
                CipherAuthentication.DSS,
                CipherEncryption.DES,
                CipherMAC.SHA1,
                CipherProtocolVersion.SSLV3,
                false);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);


        // SSL_DH_anon_WITH_DES_CBC_SHA
        cipher = new SslCipher("SSL_DH_anon_WITH_DES_CBC_SHA",
                "ADH-DES-CBC-SHA", 56,
                CipherStrength.LOW,
                CipherKeyExchange.DHE,
                CipherAuthentication.NULL,
                CipherEncryption.DES,
                CipherMAC.SHA1,
                CipherProtocolVersion.SSLV3,
                false);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // SSL_RSA_EXPORT_WITH_RC4_40_MD5
        cipher = new SslCipher("SSL_RSA_EXPORT_WITH_RC4_40_MD5",
                "EXP-RC4-MD5", 40,
                CipherStrength.EXPORT40,
                CipherKeyExchange.RSA,
                CipherAuthentication.RSA,
                CipherEncryption.RC4,
                CipherMAC.MD5,
                CipherProtocolVersion.SSLV3,
                false);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // SSL_DH_anon_EXPORT_WITH_RC4_40_MD5
        cipher = new SslCipher("SSL_DH_anon_EXPORT_WITH_RC4_40_MD5",
                "EXP-ADH-RC4-MD5", 40,
                CipherStrength.EXPORT40,
                CipherKeyExchange.DHE,
                CipherAuthentication.NULL,
                CipherEncryption.RC4,
                CipherMAC.MD5,
                CipherProtocolVersion.SSLV3,
                false);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // SSL_RSA_EXPORT_WITH_DES40_CBC_SHA
        cipher = new SslCipher("SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
                "EXP-DES-CBC-SHA", 40,
                CipherStrength.EXPORT40,
                CipherKeyExchange.RSA,
                CipherAuthentication.RSA,
                CipherEncryption.DES,
                CipherMAC.SHA1,
                CipherProtocolVersion.SSLV3,
                false);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA
        cipher = new SslCipher("SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA",
                "EXP-EDH-RSA-DES-CBC-SHA", 40,
                CipherStrength.EXPORT40,
                CipherKeyExchange.DHE,
                CipherAuthentication.RSA,
                CipherEncryption.DES,
                CipherMAC.SHA1,
                CipherProtocolVersion.SSLV3,
                false);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA
        cipher = new SslCipher("SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA",
                "EXP-EDH-DSS-DES-CBC-SHA", 40,
                CipherStrength.EXPORT40,
                CipherKeyExchange.DHE,
                CipherAuthentication.DSS,
                CipherEncryption.DES,
                CipherMAC.SHA1,
                CipherProtocolVersion.SSLV3,
                false);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // SSL_DH_anon_EXPORT_WITH_DES40_CBC_SHA
        cipher = new SslCipher("SSL_DH_anon_EXPORT_WITH_DES40_CBC_SHA",
                "EXP-ADH-DES-CBC-SHA", 40,
                CipherStrength.EXPORT40,
                CipherKeyExchange.DHE,
                CipherAuthentication.NULL,
                CipherEncryption.DES,
                CipherMAC.SHA1,
                CipherProtocolVersion.SSLV3,
                false);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_KRB5_WITH_RC4_128_SHA
        cipher = new SslCipher("TLS_KRB5_WITH_RC4_128_SHA",
                "KRB5-RC4-SHA", 128,
                CipherStrength.MEDIUM,
                CipherKeyExchange.KRB5,
                CipherAuthentication.KRB5,
                CipherEncryption.RC4,
                CipherMAC.SHA1,
                CipherProtocolVersion.TLSV1,
                false);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_KRB5_WITH_RC4_128_MD5
        cipher = new SslCipher("TLS_KRB5_WITH_RC4_128_MD5",
                "KRB5-RC4-MD5", 128,
                CipherStrength.MEDIUM,
                CipherKeyExchange.KRB5,
                CipherAuthentication.KRB5,
                CipherEncryption.RC4,
                CipherMAC.MD5,
                CipherProtocolVersion.TLSV1,
                false);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_KRB5_WITH_3DES_EDE_CBC_SHA
        cipher = new SslCipher("TLS_KRB5_WITH_3DES_EDE_CBC_SHA",
                "KRB5-DES-CBC3-SHA", 168,
                CipherStrength.HIGH,
                CipherKeyExchange.KRB5,
                CipherAuthentication.KRB5,
                CipherEncryption.TripleDES,
                CipherMAC.SHA1,
                CipherProtocolVersion.TLSV1,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_KRB5_WITH_3DES_EDE_CBC_MD5
        cipher = new SslCipher("TLS_KRB5_WITH_3DES_EDE_CBC_MD5",
                "KRB5-DES-CBC3-MD5", 168,
                CipherStrength.HIGH,
                CipherKeyExchange.KRB5,
                CipherAuthentication.KRB5,
                CipherEncryption.TripleDES,
                CipherMAC.MD5,
                CipherProtocolVersion.TLSV1,
                true);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_KRB5_WITH_DES_CBC_SHA
        cipher = new SslCipher("TLS_KRB5_WITH_DES_CBC_SHA",
                "KRB5-DES-CBC-SHA", 56,
                CipherStrength.LOW,
                CipherKeyExchange.KRB5,
                CipherAuthentication.KRB5,
                CipherEncryption.DES,
                CipherMAC.SHA1,
                CipherProtocolVersion.TLSV1,
                false);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_KRB5_WITH_DES_CBC_MD5
        cipher = new SslCipher("TLS_KRB5_WITH_DES_CBC_MD5",
                "KRB5-DES-CBC-MD5", 56,
                CipherStrength.LOW,
                CipherKeyExchange.KRB5,
                CipherAuthentication.KRB5,
                CipherEncryption.DES,
                CipherMAC.MD5,
                CipherProtocolVersion.TLSV1,
                false);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_KRB5_EXPORT_WITH_RC4_40_SHA
        cipher = new SslCipher("TLS_KRB5_EXPORT_WITH_RC4_40_SHA",
                "EXP-KRB5-RC4-SHA", 40,
                CipherStrength.EXPORT40,
                CipherKeyExchange.KRB5,
                CipherAuthentication.KRB5,
                CipherEncryption.RC4,
                CipherMAC.SHA1,
                CipherProtocolVersion.TLSV1,
                false);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_KRB5_EXPORT_WITH_RC4_40_MD5
        cipher = new SslCipher("TLS_KRB5_EXPORT_WITH_RC4_40_MD5",
                "EXP-KRB5-RC4-MD5", 40,
                CipherStrength.EXPORT40,
                CipherKeyExchange.KRB5,
                CipherAuthentication.KRB5,
                CipherEncryption.RC4,
                CipherMAC.MD5,
                CipherProtocolVersion.TLSV1,
                false);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_KRB5_EXPORT_WITH_DES_CBC_40_SHA
        cipher = new SslCipher("TLS_KRB5_EXPORT_WITH_DES_CBC_40_SHA",
                "EXP-KRB5-DES-CBC-SHA", 40,
                CipherStrength.EXPORT40,
                CipherKeyExchange.KRB5,
                CipherAuthentication.KRB5,
                CipherEncryption.DES,
                CipherMAC.SHA1,
                CipherProtocolVersion.TLSV1,
                false);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);

        // TLS_KRB5_EXPORT_WITH_DES_CBC_40_MD5
        cipher = new SslCipher("TLS_KRB5_EXPORT_WITH_DES_CBC_40_MD5",
                "EXP-KRB5-DES-CBC-MD5", 40,
                CipherStrength.EXPORT40,
                CipherKeyExchange.KRB5,
                CipherAuthentication.KRB5,
                CipherEncryption.DES,
                CipherMAC.MD5,
                CipherProtocolVersion.TLSV1,
                false);
        ciphers.putIfAbsent(cipher.name, cipher);
        ciphersByNickname.putIfAbsent(cipher.nickname, cipher);
    }

    // The logic for building up the list of ciphersuite strings is based
    // on OpenSSL's ssl_create_cipher_list() function, found in:
    //   openssl/ssl/ssl_ciph.c

    private static List<SslCipher> getCiphers(String token) {
        List<SslCipher> ciphers;

        SslCipher cipher = CIPHERS.get(token);
        if (cipher != null) {
            ciphers = new ArrayList<>(1);
            ciphers.add(cipher);
            return ciphers;
        }

        cipher = CIPHER_NICKNAMES.get(token);
        if (cipher != null) {
            ciphers = new ArrayList<>(1);
            ciphers.add(cipher);
            return ciphers;
        }

        ciphers = CIPHER_STRENGTHS.get(token);
        if (ciphers != null) {
            return ciphers;
        }

        ciphers = CIPHER_KEY_EXCHANGES.get(token);
        if (ciphers != null) {
            return ciphers;
        }

        ciphers = CIPHER_AUTHNS.get(token);
        if (ciphers != null) {
            return ciphers;
        }

        ciphers = CIPHER_ENCRYPTS.get(token);
        if (ciphers != null) {
            return ciphers;
        }

        ciphers = CIPHER_MACS.get(token);
        if (ciphers != null) {
            return ciphers;
        }

        ciphers = CIPHER_PROTOCOLS.get(token);
        if (ciphers != null) {
            return ciphers;
        }

        if (token.equals("FIPS")) {
            return CIPHER_FIPS;
        }

        // If we reach this point, then we don't how to handle this
        // token.  Could be a custom ciphersuite name for some non-standard
        // JSSE provider, so we leave it alone.

        cipher = new OtherSslCipher(token);
        ciphers = new ArrayList<>(1);
        ciphers.add(cipher);
        return ciphers;
    }

    private static List<SslCipher> resolveCiphers(List<String> tokens) {
        List<SslCipher> resolvedCiphers = new LinkedList<>();
        List<SslCipher> killedCiphers = new LinkedList<>();

        for (String token : tokens) {
            CipherOp op = CipherOp.ADD;
            List<SslCipher> ciphers = new LinkedList<>();

            // Handle prefaces: '!', '-', and '+'.
            if (token.startsWith("!")) {
                if (resolvedCiphers.isEmpty()) {
                    throw new IllegalArgumentException("No matching ciphersuites found");
                }

                token = token.substring(1);
                op = CipherOp.KILL;

            } else if (token.startsWith("-")) {
                if (resolvedCiphers.isEmpty()) {
                    throw new IllegalArgumentException("No matching ciphersuites found");
                }

                token = token.substring(1);
                op = CipherOp.REMOVE;

            } else if (token.startsWith("+")) {
                if (resolvedCiphers.isEmpty()) {
                    throw new IllegalArgumentException("No matching ciphersuites found");
                }

                token = token.substring(1);
                op = CipherOp.RIGHT_SHIFT;

            } else if (token.startsWith("@")) {
                if (resolvedCiphers.isEmpty()) {
                    throw new IllegalArgumentException("No matching ciphersuites found");
                }

                // Handle "control" ciphers (i.e. '@STRENGTH')
                token = token.substring(1);

                if (token.equals("STRENGTH")) {
                    Collections.sort(resolvedCiphers);
                    continue;
                }

                throw new IllegalArgumentException(String.format("Unknown control cipher string '%s'", token));
            }

            // Does this token have the '+' symbol, indicating logical AND
            // of the delimited tokens?
            String[] subtokens = token.split("\\+");
            if (subtokens.length > 1) {

                // Get the first set as the "base" set...
                List<String> resolvedTokens = resolveAlias(subtokens[0]);
                List<SslCipher> baseCiphers = resolveCiphers(resolvedTokens);

                //...and retain only the rest, giving us the intersection
                for (int i = 1; i < subtokens.length; i++) {
                    resolvedTokens = resolveAlias(subtokens[i]);
                    baseCiphers.retainAll(resolveCiphers(resolvedTokens));
                }

                ciphers = baseCiphers;

            } else {
                List<String> resolvedTokens = resolveAlias(token);
                for (String resolvedToken : resolvedTokens) {
                    ciphers.addAll(getCiphers(resolvedToken));
                }
            }

            // Now, what do we do with the cipher list we've obtained?

            switch (op) {
                case ADD:
                    for (SslCipher c : ciphers) {
                        resolvedCiphers.add(c);
                    }
                    break;

                case KILL:
                    killedCiphers.addAll(ciphers);
                    break;

                case REMOVE:
                    resolvedCiphers.removeAll(ciphers);
                    break;

                case RIGHT_SHIFT:
                    List<SslCipher> removedCiphers = new LinkedList<>();
                    ListIterator<SslCipher> iter = resolvedCiphers.listIterator();
                    while (iter.hasNext()) {
                        SslCipher c = iter.next();
                        if (ciphers.contains(c)) {
                            removedCiphers.add(c);
                            iter.remove();
                        }
                    }

                    int idx = resolvedCiphers.size() - 1;
                    if (idx < 0) {
                        idx = 0;
                    }

                    resolvedCiphers.addAll(idx, removedCiphers);
                    break;
            }
        }

        resolvedCiphers.removeAll(killedCiphers);

        if (!resolvedCiphers.isEmpty()) {

            // For better security out of the box, automatically sort the
            // list of ciphers all of the time, strongest ciphers first.  Also
            // leads to more predictable configurations.
            Collections.sort(resolvedCiphers);
        }

        return resolvedCiphers;
    }

    private static List<String> resolveAlias(List<String> aliases) {
        boolean firstElement = true;

        // See openssl/ssl/ssl_ciph.c's cipher_aliases[] array
        ListIterator<String> iter = aliases.listIterator();
        while (iter.hasNext()) {
            String token = iter.next();

            switch (token) {
                case "DH":
                    iter.remove();
                    iter.add(CipherKeyExchange.DH.tokenName);

                    break;
                case "ADH":
                    iter.remove();
                    iter.add(CipherKeyExchange.DHE.tokenName);
                    iter.add(CipherKeyExchange.ECDHE.tokenName);
                    iter.add(CipherAuthentication.NULL.tokenName);

                    break;
                case "EDH":
                    iter.remove();
                    iter.add(CipherKeyExchange.DHE.tokenName);
                    iter.add("!" + CipherKeyExchange.ECDHE.tokenName);
                    iter.add("!" + CipherAuthentication.NULL.tokenName);

                    break;
                case "kECDH":
                    iter.remove();
                    iter.add(CipherKeyExchange.ECDH_RSA.tokenName);
                    iter.add(CipherKeyExchange.ECDH_ECDSA.tokenName);
                    iter.add(CipherKeyExchange.ECDHE.tokenName);

                    break;
                case "ECDH":
                    iter.remove();
                    iter.add(CipherKeyExchange.ECDH_RSA.tokenName);
                    iter.add(CipherKeyExchange.ECDH_ECDSA.tokenName);
                    iter.add(CipherKeyExchange.ECDHE.tokenName);

                    break;
                case "DSS":
                    iter.remove();
                    iter.add(CipherAuthentication.DSS.tokenName);

                    break;
                case "ECDSA":
                    iter.remove();
                    iter.add(CipherAuthentication.ECDSA.tokenName);

                    break;
                case "NULL":
                    iter.remove();
                    iter.add(CipherEncryption.NULL.tokenName);

                    break;
                case "KRB5":
                    iter.remove();
                    iter.add(CipherKeyExchange.KRB5.tokenName);
                    iter.add(CipherAuthentication.KRB5.tokenName);

                    break;
                case "RSA":
                    iter.remove();
                    iter.add(CipherKeyExchange.RSA.tokenName);
                    iter.add(CipherAuthentication.RSA.tokenName);

                    break;
                case "AES":
                    iter.remove();
                    iter.add(CipherEncryption.AES128.tokenName);
                    iter.add(CipherEncryption.AES256.tokenName);
                    iter.add(CipherEncryption.AESGCM128.tokenName);
                    iter.add(CipherEncryption.AESGCM256.tokenName);

                    break;
                case "AES128":
                    iter.remove();
                    iter.add(CipherEncryption.AES128.tokenName);
                    iter.add(CipherEncryption.AESGCM128.tokenName);

                    break;
                case "AES256":
                    iter.remove();
                    iter.add(CipherEncryption.AES256.tokenName);
                    iter.add(CipherEncryption.AESGCM256.tokenName);

                    break;
                case "AESGCM":
                    iter.remove();
                    iter.add(CipherEncryption.AESGCM128.tokenName);
                    iter.add(CipherEncryption.AESGCM256.tokenName);

                    break;
                case "EXP":
                case "EXPORT":
                    iter.remove();
                    iter.add("EXPORT40");
                    iter.add("EXPORT56");

                    break;
                case "SHA":
                    iter.remove();
                    iter.add(CipherMAC.SHA1.tokenName);

                    break;
                case "AECDH":
                    iter.remove();
                    iter.add(CipherKeyExchange.ECDHE.tokenName + "+" +
                            CipherAuthentication.NULL.tokenName);

                    break;
                case "EECDH":
                    iter.remove();
                    iter.add(CipherKeyExchange.ECDH_ECDSA.tokenName);

                    break;
                case "DEFAULT":
                case "DEFAULTS":

                    if (!firstElement) {
                        throw new IllegalArgumentException("DEFAULT must be the first cipher string specified");
                    }

                    iter.remove();
                    iter.add("HIGH");
                    iter.add("MEDIUM");

                    // Anonymous Diffie-Hellman ciphersuites do NOT provide
                    // authentication, and thus they are not allowed by default.
                    // See KG-6189 for more details.
                    iter.add("!ADH");

                    iter.add("!KRB5");

                    break;
                case "COMPLEMENTOFDEFAULT":
                    iter.remove();
                    iter.add(CipherKeyExchange.DHE.tokenName);
                    iter.add("!" + CipherEncryption.NULL.tokenName);

                    break;
                case "COMPLEMENTOFALL":
                    iter.remove();
                    iter.add(CipherEncryption.NULL.tokenName);

                    break;
                case "ALL":
                    iter.remove();

                    // To get all of ciphers, include all of the "strength"
                    // values.
                    iter.add("HIGH");
                    iter.add("MEDIUM");
                    iter.add("LOW");
                    iter.add("EXPORT56");
                    iter.add("EXPORT40");
                    iter.add("NONE");
                    iter.add("!eNULL");
                    break;
            }

            firstElement = false;
        }

        return aliases;
    }

    private static List<String> resolveAlias(String alias) {
        List<String> aliases = new ArrayList<>(1);
        aliases.add(alias);
        return resolveAlias(aliases);
    }

    public static List<String> resolve(List<String> tokens) {
        if (tokens == null ||
            tokens.isEmpty()) {

            List<String> defaults = new ArrayList<>(2);
            defaults.add("HIGH");
            defaults.add("MEDIUM");

            // Anonymous Diffie-Hellman ciphersuites do NOT provide
            // authentication, and thus they are not allowed by default.
            // See KG-6189 for more details.
            defaults.add("!ADH");

            defaults.add("!KRB5");

            return resolve(defaults);
        }

        List<String> resolvedTokens = resolveAlias(tokens);

        List<SslCipher> resolvedCiphers = resolveCiphers(resolvedTokens);
        if (resolvedCiphers.isEmpty()) {
            throw new IllegalArgumentException("No ciphersuites matching configured <ssl.ciphers> found");
        }

        List<String> resolvedNames = new ArrayList<>(resolvedCiphers.size());
        for (SslCipher c : resolvedCiphers) {
            resolvedNames.add(c.name);
        }

        return resolvedNames;
    }

    // We return a String array here, rather than a list, because the
    // javax.net.ssl.SSLEngine.setEnabledCipherSuites() method wants a
    // String array.

    public static String[] resolveCSV(String csv) {
        List<String> resolved;

        if (csv != null &&
                ! csv.equals("")) {
            String[] elts = csv.split(",");
            List<String> tokens = new ArrayList<>(elts.length);
            for (String elt : elts) {
                tokens.add(elt.trim());
            }

            resolved = resolve(tokens);

        } else {
            // No 'ssl.ciphers' configured?  Use the defaults, then.
            resolved = resolve(null);
        }

        return resolved.toArray(new String[resolved.size()]);
    }
}
