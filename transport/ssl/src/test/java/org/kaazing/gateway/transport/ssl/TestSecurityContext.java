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

import org.kaazing.gateway.security.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

public class TestSecurityContext implements SecurityContext {
    private final KeyStore keyStore;
    private final String keyStoreFile;
    private final char[] keyStorePassword;
    private final KeyStore trustStore;
    private final char[] trustStorePassword;
    private final String keyStoreFilePath;
    private final String trustStoreFile;
    private final String trustStoreFilePath;
    private final String keyStorePasswordFile;

    private static final Logger logger = LoggerFactory.getLogger(TestSecurityContext.class);

    public TestSecurityContext(KeyStore keyStore,
                                  String keyStoreFile,
                                  String keyStoreFilePath,
                                  char[] keyStorePassword,
                                  String keyStorePasswordFile,
                                  KeyStore trustStore,
                                  String trustStoreFile,
                                  String trustStoreFilePath,
                                  char[] trustStorePassword)
            throws KeyStoreException, NoSuchAlgorithmException {
        this.keyStore = keyStore;
        this.keyStoreFile = keyStoreFile;
        this.keyStorePassword = keyStorePassword;
        this.trustStore = trustStore;
        this.trustStorePassword = trustStorePassword;
        this.keyStoreFilePath = keyStoreFilePath;
        this.trustStoreFile = trustStoreFile;
        this.trustStoreFilePath = trustStoreFilePath;
        this.keyStorePasswordFile = keyStorePasswordFile;

        if (keyStore == null) {
            // subsequent certificate lookup will fail and gateway will not
            // start if a secure accept is encountered
            logger.warn("Keystore is not specified");
        }
    }

    @Override
    public String getKeyStoreFile() {
        return keyStoreFile;
    }

    @Override
    public KeyStore getKeyStore() {
        return keyStore;
    }

    @Override
    public KeyStore getTrustStore() {
        return trustStore;
    }

    @Override
    public char[] getKeyStorePassword() {
        return keyStorePassword;
    }

    @Override
    public char[] getTrustStorePassword() {
        return trustStorePassword;
    }

    public String getKeyStoreFilePath() {
        return keyStoreFilePath;
    }

    public String getTrustStoreFile() {
        return trustStoreFile;
    }

    public String getTrustStoreFilePath() {
        return trustStoreFilePath;
    }

    public String getKeyStorePasswordFile() {
        return keyStorePasswordFile;
    }
}

