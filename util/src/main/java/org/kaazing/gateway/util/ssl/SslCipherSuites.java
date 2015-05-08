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

package org.kaazing.gateway.util.ssl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SslCipherSuites {

    protected SslCipherSuites() {
    }

    private enum CipherStrength {
        HIGH("HIGH"),
        MEDIUM("MEDIUM"),
        LOW("LOW"),
        EXPORT40("EXPORT40"),

        // OpenSSL provides this token, but it is an empty list
        EXPORT56("EXPORT56"),

        NONE("NONE");

        private final String tokenName;

        CipherStrength(final String tokenName) {
            this.tokenName = tokenName;
        }

        public String getTokenName() {
            return tokenName;
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

        CipherKeyExchange(final String tokenName) {
            this.tokenName = tokenName;
        }

        public String getTokenName() {
            return tokenName;
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

        CipherAuthentication(final String tokenName) {
            this.tokenName = tokenName;
        }

        public String getTokenName() {
            return tokenName;
        }
    }

    private enum CipherEncryption {
        RC4("RC4"),
        AES128("AES128"),
        AES256("AES256"),
        TripleDES("3DES"),
        DES("DES"),
        NULL("eNULL");

        private final String tokenName;

        CipherEncryption(final String tokenName) {
            this.tokenName = tokenName;
        }

        public String getTokenName() {
            return tokenName;
        }
    }

    private enum CipherMAC {
        MD5("MD5"),
        SHA1("SHA1"),
        SHA256("SHA256"),
        SHA384("SHA384");

        private final String tokenName;

        CipherMAC(final String tokenName) {
            this.tokenName = tokenName;
        }

        public String getTokenName() {
            return tokenName;
        }
    }

    private enum CipherProtocolVersion {
        SSLV3("SSLv3"),
        TLSV1("TLSv1"),
        TLSV1_1("TLSv1.1"),
        TLSV1_2("TLSv1.2");

        private final String tokenName;

        CipherProtocolVersion(final String tokenName) {
            this.tokenName = tokenName;
        }

        public String getTokenName() {
            return tokenName;
        }
    }

    private static class SslCipher
        implements Comparable {
        private final String name;
        private final String nickname;
        private final Integer strengthBits;
        private final CipherStrength strength;
        private final CipherKeyExchange keyExchange;
        private final CipherAuthentication authentication;
        private final CipherEncryption encryption;
        private final CipherMAC mac;
        private final CipherProtocolVersion protocolVersion;
        private final boolean fips;

        public SslCipher(String name,
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

        public String getName() {
            return name;
        }

        public String getNickname() {
            return nickname;
        }

        public Integer getStrengthBits() {
            return strengthBits;
        }

        public CipherStrength getStrength() {
            return strength;
        }

        public CipherKeyExchange getKeyExchange() {
            return keyExchange;
        }

        public CipherAuthentication getAuthentication() {
            return authentication;
        }

        public CipherEncryption getEncryption() {
            return encryption;
        }

        public CipherMAC getMAC() {
            return mac;
        }

        public CipherProtocolVersion getProtocolVersion() {
            return protocolVersion;
        }

        public boolean isFIPS() {
            return fips;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            }

            SslCipher c = (SslCipher) o;
            return getName().equals(c.getName());
        }

        @Override
        public int hashCode() {
            int code = getName().hashCode() +
                       getNickname().hashCode() +
                       getKeyExchange().hashCode() +
                       getAuthentication().hashCode() +
                       getEncryption().hashCode() +
                       getMAC().hashCode() +
                       getProtocolVersion().hashCode() +
                       getStrength().hashCode() +
                       getStrengthBits().hashCode();
            return code;
        }

        @Override
        public int compareTo(Object o) {
            if (o == null) {
                return -1;
            }

            SslCipher c = (SslCipher) o;

            // To sort the ciphers in DESCENDING order of bits, we need to
            // invert the normal comparison
            return getStrengthBits().compareTo(c.getStrengthBits()) * -1;
        }

        @Override
        public String toString() {
            return String.format("%s:[%s/%s/%s/%s/%s/%s(%d)%s]", getName(),
                getKeyExchange(), getAuthentication(), getEncryption(),
                getMAC(), getProtocolVersion(), getStrength(),
                getStrengthBits(), isFIPS() ? "/FIPS" : "");
        }
    }

    // Used as a placeholder for non-SunJSSEProvider ciphers; we override
    // compareTo() so that sorting by strength keeps these ciphers in their
    // configured place in the list.
    private static class OtherSslCipher
        extends SslCipher {

        public OtherSslCipher(String name) {
            super(name, null, 0, null, null, null, null, null, null, false);
        }

        @Override
        public int hashCode() {
            return getName().hashCode();
        }

        @Override
        public int compareTo(Object o) {
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

    private static final ConcurrentMap<String, List<SslCipher>> CIPHER_STRENGTHS = new ConcurrentHashMap<>();
    private static boolean filledStrengths;

    private static final ConcurrentMap<String, List<SslCipher>> CIPHER_KEY_EXCHANGES = new ConcurrentHashMap<>();
    private static boolean filledKeyExchanges;

    private static final ConcurrentMap<String, List<SslCipher>> CIPHER_AUTHNS = new ConcurrentHashMap<>();
    private static boolean filledAuths;

    private static final ConcurrentMap<String, List<SslCipher>> CIPHER_ENCRYPTS = new ConcurrentHashMap<>();
    private static boolean filledEncrypts;

    private static final ConcurrentMap<String, List<SslCipher>> CIPHER_MACS = new ConcurrentHashMap<>();
    private static boolean filledMACs;

    private static final ConcurrentMap<String, List<SslCipher>> CIPHER_PROTOCOLS = new ConcurrentHashMap<>();
    private static boolean filledProtocols;

    private static final List<SslCipher> CIPHER_FIPS = new LinkedList<>();
    private static boolean filledFIPS;

    // Map of the ciphersuite name (as appearing in IETF specs) to
    // various details about that ciphersuite
    private static boolean filledCiphers;
    private static final ConcurrentMap<String, SslCipher> CIPHERS = new ConcurrentHashMap<>();

    // Map of the ciphersuite name (as defined as a nickname in OpenSSL)
    private static final ConcurrentMap<String, SslCipher> CIPHER_NICKNAMES = new ConcurrentHashMap<>();

    private static void initStrengthGroups(ConcurrentMap<String, List<SslCipher>> groups) {
        // HIGH
        List<SslCipher> highs = new LinkedList<>();

        // MEDIUM
        List<SslCipher> mediums = new LinkedList<>();

        // LOW
        List<SslCipher> lows = new LinkedList<>();

        // EXPORT40
        List<SslCipher> export40s = new LinkedList<>();

        // EXPORT56
        List<SslCipher> export56s = new LinkedList<>();

        // NONE
        List<SslCipher> nones = new LinkedList<>();

        for (SslCipher cipher : CIPHERS.values()) {
            switch (cipher.getStrength()) {
                case HIGH:
                    highs.add(cipher);
                    break;

                case MEDIUM:
                    mediums.add(cipher);
                    break;

                case LOW:
                    lows.add(cipher);
                    break;

                case EXPORT40:
                    export40s.add(cipher);
                    break;

                case EXPORT56:
                    export56s.add(cipher);
                    break;

                case NONE:
                    nones.add(cipher);
                    break;
            }
        }

        groups.put(CipherStrength.HIGH.getTokenName(), highs);
        groups.put(CipherStrength.MEDIUM.getTokenName(), mediums);
        groups.put(CipherStrength.LOW.getTokenName(), lows);
        groups.put(CipherStrength.EXPORT40.getTokenName(), export40s);
        groups.put(CipherStrength.EXPORT56.getTokenName(), export56s);
        groups.put(CipherStrength.NONE.getTokenName(), nones);
    }

    private static void initKeyExchangeGroups(ConcurrentMap<String, List<SslCipher>> groups) {
        // RSA
        List<SslCipher> rsas = new LinkedList<>();

        // DH
        List<SslCipher> dhs = new LinkedList<>();

        // DHE
        List<SslCipher> dhes = new LinkedList<>();

        // ECDH_ECDSA
        List<SslCipher> ecdhEcdsas = new LinkedList<>();

        // ECDH_RSA
        List<SslCipher> ecdhRsas = new LinkedList<>();

        // ECDHE
        List<SslCipher> ecdhes = new LinkedList<>();

        // KRB5
        List<SslCipher> krb5s = new LinkedList<>();

        for (SslCipher cipher : CIPHERS.values()) {
            switch (cipher.getKeyExchange()) {
                case RSA:
                    rsas.add(cipher);
                    break;

                case DH:
                    dhs.add(cipher);
                    break;

                case DHE:
                    dhes.add(cipher);
                    break;

                case ECDH_ECDSA:
                    ecdhEcdsas.add(cipher);
                    break;

                case ECDH_RSA:
                    ecdhRsas.add(cipher);
                    break;

                case ECDHE:
                    ecdhes.add(cipher);
                    break;

                case KRB5:
                    krb5s.add(cipher);
                    break;
            }
        }

        groups.put(CipherKeyExchange.RSA.getTokenName(), rsas);
        groups.put(CipherKeyExchange.DH.getTokenName(), dhs);
        groups.put(CipherKeyExchange.DHE.getTokenName(), dhes);
        groups.put(CipherKeyExchange.ECDH_ECDSA.getTokenName(), ecdhEcdsas);
        groups.put(CipherKeyExchange.ECDH_RSA.getTokenName(), ecdhRsas);
        groups.put(CipherKeyExchange.ECDHE.getTokenName(), ecdhes);
        groups.put(CipherKeyExchange.KRB5.getTokenName(), krb5s);
    }

    private static void initAuthenticationGroups(ConcurrentMap<String, List<SslCipher>> groups) {
        // RSA
        List<SslCipher> rsas = new LinkedList<>();

        // DSS
        List<SslCipher> dsss = new LinkedList<>();

        // ECDH
        List<SslCipher> ecdhs = new LinkedList<>();

        // ECDSA
        List<SslCipher> ecdsas = new LinkedList<>();

        // KRB5
        List<SslCipher> krb5s = new LinkedList<>();

        // NULL
        List<SslCipher> nulls = new LinkedList<>();

        for (SslCipher cipher : CIPHERS.values()) {
            switch (cipher.getAuthentication()) {
                case RSA:
                    rsas.add(cipher);
                    break;

                case DSS:
                    dsss.add(cipher);
                    break;

                case ECDH:
                    ecdhs.add(cipher);
                    break;

                case ECDSA:
                    ecdsas.add(cipher);
                    break;

                case KRB5:
                    krb5s.add(cipher);
                    break;

                case NULL:
                    nulls.add(cipher);
                    break;
            }
        }

        groups.put(CipherAuthentication.RSA.getTokenName(), rsas);
        groups.put(CipherAuthentication.DSS.getTokenName(), dsss);
        groups.put(CipherAuthentication.ECDH.getTokenName(), ecdhs);
        groups.put(CipherAuthentication.ECDSA.getTokenName(), ecdsas);
        groups.put(CipherAuthentication.KRB5.getTokenName(), krb5s);
        groups.put(CipherAuthentication.NULL.getTokenName(), nulls);
    }

    private static void initEncryptionGroups(ConcurrentMap<String, List<SslCipher>> groups) {
        // RC4
        List<SslCipher> rc4s = new LinkedList<>();

        // AES128
        List<SslCipher> aes128s = new LinkedList<>();

        // AES256
        List<SslCipher> aes256s = new LinkedList<>();

        // 3DES
        List<SslCipher> tripleDess = new LinkedList<>();

        // DES
        List<SslCipher> dess = new LinkedList<>();

        // NULL
        List<SslCipher> nulls = new LinkedList<>();

        for (SslCipher cipher : CIPHERS.values()) {
            switch (cipher.getEncryption()) {
                case RC4:
                    rc4s.add(cipher);
                    break;

                case AES128:
                    aes128s.add(cipher);
                    break;

                case AES256:
                    aes256s.add(cipher);
                    break;

                case TripleDES:
                    tripleDess.add(cipher);
                    break;

                case DES:
                    dess.add(cipher);
                    break;

                case NULL:
                    nulls.add(cipher);
                    break;
            }
        }

        groups.put(CipherEncryption.RC4.getTokenName(), rc4s);
        groups.put(CipherEncryption.AES128.getTokenName(), aes128s);
        groups.put(CipherEncryption.AES256.getTokenName(), aes256s);
        groups.put(CipherEncryption.TripleDES.getTokenName(), tripleDess);
        groups.put(CipherEncryption.DES.getTokenName(), dess);
        groups.put(CipherEncryption.NULL.getTokenName(), nulls);
    }

    private static void initMACGroups(ConcurrentMap<String, List<SslCipher>> groups) {
        // MD5
        List<SslCipher> md5s = new LinkedList<>();

        // SHA1
        List<SslCipher> sha1s = new LinkedList<>();

        // SHA256
        List<SslCipher> sha256s = new LinkedList<>();

        // SHA384
        List<SslCipher> sha384s = new LinkedList<>();

        for (SslCipher cipher : CIPHERS.values()) {
            switch (cipher.getMAC()) {
                case MD5:
                    md5s.add(cipher);
                    break;

                case SHA1:
                    sha1s.add(cipher);
                    break;

                case SHA256:
                    sha256s.add(cipher);
                    break;

                case SHA384:
                    sha384s.add(cipher);
                    break;
            }
        }

        groups.put(CipherMAC.MD5.getTokenName(), md5s);
        groups.put(CipherMAC.SHA1.getTokenName(), sha1s);
        groups.put(CipherMAC.SHA256.getTokenName(), sha256s);
        groups.put(CipherMAC.SHA384.getTokenName(), sha384s);
    }

    private static void initProtocolGroups(ConcurrentMap<String, List<SslCipher>> groups) {

        // SSLv3
        List<SslCipher> sslv3s = new LinkedList<>();

        // TLSv1
        List<SslCipher> tlsv1s = new LinkedList<>();

        // TLSv1.1
        List<SslCipher> tlsv11s = new LinkedList<>();

        // TLSv1.2
        List<SslCipher> tlsv12s = new LinkedList<>();

        for (SslCipher cipher : CIPHERS.values()) {
            switch (cipher.getProtocolVersion()) {
                case SSLV3:
                    sslv3s.add(cipher);
                    break;

                case TLSV1:
                    tlsv1s.add(cipher);
                    break;

                case TLSV1_1:
                    tlsv11s.add(cipher);
                    break;

                case TLSV1_2:
                    tlsv12s.add(cipher);
                    break;
            }
        }

        groups.put(CipherProtocolVersion.SSLV3.getTokenName(), sslv3s);
        groups.put(CipherProtocolVersion.TLSV1.getTokenName(), tlsv1s);
        groups.put(CipherProtocolVersion.TLSV1_1.getTokenName(), tlsv11s);
        groups.put(CipherProtocolVersion.TLSV1_2.getTokenName(), tlsv12s);
    }

    private static void initFIPSGroup(List<SslCipher> groups) {
        for (SslCipher cipher : CIPHERS.values()) {
            if (cipher.isFIPS()) {
                groups.add(cipher);
            }
        }
    }

    private static void initCipherSuiteGroups() {
        if (!filledStrengths) {
            initStrengthGroups(CIPHER_STRENGTHS);
            filledStrengths = true;
        }

        if (!filledKeyExchanges) {
            initKeyExchangeGroups(CIPHER_KEY_EXCHANGES);
            filledKeyExchanges = true;
        }

        if (!filledAuths) {
            initAuthenticationGroups(CIPHER_AUTHNS);
            filledAuths = true;
        }

        if (!filledEncrypts) {
            initEncryptionGroups(CIPHER_ENCRYPTS);
            filledEncrypts = true;
        }

        if (!filledMACs) {
            initMACGroups(CIPHER_MACS);
            filledMACs = true;
        }

        if (!filledProtocols) {
            initProtocolGroups(CIPHER_PROTOCOLS);
            filledProtocols = true;
        }

        if (!filledFIPS) {
            initFIPSGroup(CIPHER_FIPS);
            filledFIPS = true;
        }
    }

    // These names are those documented in:
    //   http://docs.oracle.com/javase/6/docs/technotes/guides/security/SunProviders.html#SunJSSEProvider
    private static void initJava6SunCipherSuites(ConcurrentMap<String, SslCipher> ciphers,
                                                 ConcurrentMap<String, SslCipher> ciphersByNickname) {
        SslCipher cipher = null;

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);
    }

    // These names are those documented in:
    //   http://docs.oracle.com/javase/7/docs/technotes/guides/security/SunProviders.html#SunJSSEProvider
    private static void initJava7SunCipherSuites(ConcurrentMap<String, SslCipher> ciphers,
                                                 ConcurrentMap<String, SslCipher> ciphersByNickname) {
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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);

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
        ciphers.putIfAbsent(cipher.getName(), cipher);
        ciphersByNickname.putIfAbsent(cipher.getNickname(), cipher);
    }

    public static void reset() {
        CIPHER_STRENGTHS.clear();
        filledStrengths = false;

        CIPHER_KEY_EXCHANGES.clear();
        filledKeyExchanges = false;

        CIPHER_AUTHNS.clear();
        filledAuths = false;

        CIPHER_ENCRYPTS.clear();
        filledEncrypts = false;

        CIPHER_MACS.clear();
        filledMACs = false;

        CIPHER_PROTOCOLS.clear();
        filledProtocols = false;

        CIPHER_FIPS.clear();
        filledFIPS = false;

        CIPHERS.clear();
        filledCiphers = false;

        CIPHER_NICKNAMES.clear();

        init();
    }

    public static void init() {
        if (!filledCiphers) {
            initJava6SunCipherSuites(CIPHERS, CIPHER_NICKNAMES);

            // Populate the cipher list which was ADDED in Java 7.
            initJava7SunCipherSuites(CIPHERS, CIPHER_NICKNAMES);

            filledCiphers = true;
        }

        initCipherSuiteGroups();
    }

    // The logic for building up the list of ciphersuite strings is based
    // on OpenSSL's ssl_create_cipher_list() function, found in:
    //   openssl/ssl/ssl_ciph.c

    private static List<SslCipher> getCiphers(String token) {
        List<SslCipher> ciphers = null;

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

        if (resolvedCiphers != null &&
            !resolvedCiphers.isEmpty()) {

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

            if (token.equals("DH")) {
                iter.remove();
                iter.add(CipherKeyExchange.DH.getTokenName());

            } else if (token.equals("ADH")) {
                iter.remove();
                iter.add(CipherKeyExchange.DHE.getTokenName());
                iter.add(CipherKeyExchange.ECDHE.getTokenName());
                iter.add(CipherAuthentication.NULL.getTokenName());

            } else if (token.equals("EDH")) {
                iter.remove();
                iter.add(CipherKeyExchange.DHE.getTokenName());
                iter.add("!" + CipherKeyExchange.ECDHE.getTokenName());
                iter.add("!" + CipherAuthentication.NULL.getTokenName());

            } else if (token.equals("kECDH")) {
                iter.remove();
                iter.add(CipherKeyExchange.ECDH_RSA.getTokenName());
                iter.add(CipherKeyExchange.ECDH_ECDSA.getTokenName());
                iter.add(CipherKeyExchange.ECDHE.getTokenName());

            } else if (token.equals("ECDH")) {
                iter.remove();
                iter.add(CipherKeyExchange.ECDH_RSA.getTokenName());
                iter.add(CipherKeyExchange.ECDH_ECDSA.getTokenName());
                iter.add(CipherKeyExchange.ECDHE.getTokenName());

            } else if (token.equals("DSS")) {
                iter.remove();
                iter.add(CipherAuthentication.DSS.getTokenName());

            } else if (token.equals("ECDSA")) {
                iter.remove();
                iter.add(CipherAuthentication.ECDSA.getTokenName());

            } else if (token.equals("NULL")) {
                iter.remove();
                iter.add(CipherEncryption.NULL.getTokenName());

            } else if (token.equals("KRB5")) {
                iter.remove();
                iter.add(CipherKeyExchange.KRB5.getTokenName());
                iter.add(CipherAuthentication.KRB5.getTokenName());

            } else if (token.equals("RSA")) {
                iter.remove();
                iter.add(CipherKeyExchange.RSA.getTokenName());
                iter.add(CipherAuthentication.RSA.getTokenName());

            } else if (token.equals("AES")) {
                iter.remove();
                iter.add("AES128");
                iter.add("AES256");

            } else if (token.equals("EXP") ||
                       token.equals("EXPORT")) {
                iter.remove();
                iter.add("EXPORT40");
                iter.add("EXPORT56");

            } else if (token.equals("SHA")) {
                iter.remove();
                iter.add(CipherMAC.SHA1.getTokenName());

            } else if (token.equals("AECDH")) {
                iter.remove();
                iter.add(CipherKeyExchange.ECDHE.getTokenName() + "+" +
                         CipherAuthentication.NULL.getTokenName());

            } else if (token.equals("EECDH")) {
                iter.remove();
                iter.add(CipherKeyExchange.ECDH_ECDSA.getTokenName());

            } else if (token.equals("DEFAULT") ||
                       token.equals("DEFAULTS")) {

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

            } else if (token.equals("COMPLEMENTOFDEFAULT")) {
                iter.remove();
                iter.add(CipherKeyExchange.DHE.getTokenName());
                iter.add("!" + CipherEncryption.NULL.getTokenName());

            } else if (token.equals("COMPLEMENTOFALL")) {
                iter.remove();
                iter.add(CipherEncryption.NULL.getTokenName());

            } else if (token.equals("ALL")) {
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
        // Lazily populate our group maps as needed
        init();

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
            resolvedNames.add(c.getName());
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
