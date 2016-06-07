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
package org.kaazing.gateway.service.http.proxy;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;

public class TlsTestUtil {
    static char[] password = "ab987c".toCharArray();

    static char[] password() {
        return password;
    }

    static KeyStore keyStore() {
        try {
            // Initialize KeyStore of gateway
            KeyStore keyStore = KeyStore.getInstance("JCEKS");
            FileInputStream kis = new FileInputStream("target/truststore/keystore.db");
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
            FileInputStream tis = new FileInputStream("target/truststore/truststore.db");
            trustStore.load(tis, null);
            tis.close();
            return trustStore;
        } catch (Exception e) {
            throw new RuntimeException("Cannot create truststore", e);
        }
    }

    static SSLSocketFactory clientSocketFactory() {
        try {
            KeyStore trustStore = trustStore();

            // Configure client socket factory to trust the gateway's certificate
            SSLContext sslContext = SSLContext.getInstance("TLS");
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);
            sslContext.init(null, tmf.getTrustManagers(), null);
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new RuntimeException("Cannot create client ssl socket factory", e);
        }
    }

    static SSLServerSocketFactory serverSocketFactory() {
        try {
            KeyStore keyStore = keyStore();
            SSLContext sslContext = SSLContext.getInstance("TLS");
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, password);
            sslContext.init(kmf.getKeyManagers(), null, null);
            return sslContext.getServerSocketFactory();
        } catch (Exception e) {
            throw new RuntimeException("Cannot create client ssl socket factory", e);
        }
    }
}
