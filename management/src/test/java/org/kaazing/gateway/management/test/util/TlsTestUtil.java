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
package org.kaazing.gateway.management.test.util;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;

public class TlsTestUtil {
    static final String keyStoreFileLocation = "target/truststore/keystore.db";

    public static char[] password() {
        return "ab987c".toCharArray();
    }

    public static KeyStore keyStore() {
        try {
            // Initialize KeyStore of gateway
            KeyStore keyStore = KeyStore.getInstance("JCEKS");
            FileInputStream kis = new FileInputStream(keyStoreFileLocation);
            keyStore.load(kis, password());
            kis.close();
            return keyStore;
        } catch (Exception e) {
            File file = new File(keyStoreFileLocation);
            throw new RuntimeException("Cannot create keystore: " + file.getAbsolutePath(), e);
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
