package org.kaazing.gateway.management.jmx;

import java.io.FileInputStream;
import java.security.KeyStore;

public class TlsTestUtil {
    static char[] password = "ab987c".toCharArray();
    static String keyStoreFileLocation = "/Users/David/Desktop/kaazing-websocket-gateway-4.0.9/conf/keystore.db";

    static char[] password() {
        return password;
    }

    static KeyStore keyStore() {
        try {
            // Initialize KeyStore of gateway
            KeyStore keyStore = KeyStore.getInstance("JCEKS");
            FileInputStream kis = new FileInputStream(keyStoreFileLocation);
            keyStore.load(kis, password);
            kis.close();
            return keyStore;
        } catch (Exception e) {
            throw new RuntimeException("Cannot create keystore", e);
        }
    }

    static KeyStore trustStore() {
        try {
            // Initialize TrustStore of gateway
            KeyStore trustStore = KeyStore.getInstance("JKS");
            FileInputStream tis = new FileInputStream("/Users/David/Desktop/kaazing-websocket-gateway-4.0.9/conf/truststore.db");
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
