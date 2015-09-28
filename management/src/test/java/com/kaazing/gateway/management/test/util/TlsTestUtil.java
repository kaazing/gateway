package com.kaazing.gateway.management.test.util;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;

public class TlsTestUtil {
    static char[] password = "ab987c".toCharArray();
    static String keyStoreFileLocation = "target/truststore/keystore.db";

    public static char[] password() {
        return password;
    }

    public static KeyStore keyStore() {
        try {
            // Initialize KeyStore of gateway
            KeyStore keyStore = KeyStore.getInstance("JCEKS");
            FileInputStream kis = new FileInputStream(keyStoreFileLocation);
            keyStore.load(kis, password);
            kis.close();
            return keyStore;
        } catch (Exception e) {
            File file = new File(keyStoreFileLocation);
            throw new RuntimeException("Cannot create keystore" + file.getAbsolutePath(), e);
        }
    }

    public static KeyStore trustStore() {
        try {
            // Initialize TrustStore of gateway
            KeyStore trustStore = KeyStore.getInstance("JKS");
            FileInputStream tis = new FileInputStream("target/truststore/truststore.db");
            trustStore.load(tis, null);
            tis.close();
            return trustStore;
        } catch (Exception e) {
            throw new RuntimeException("Cannot create truststore", e);
        }
    }

    public static String getKeystoreFileLocation() {
        return keyStoreFileLocation;
    }
}
