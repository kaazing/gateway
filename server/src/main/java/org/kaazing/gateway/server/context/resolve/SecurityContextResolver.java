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
package org.kaazing.gateway.server.context.resolve;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.security.KeyStore;

import org.kaazing.gateway.server.config.june2016.SecurityStoreType;
import org.kaazing.gateway.server.config.june2016.SecurityType;
import org.slf4j.Logger;

public class SecurityContextResolver implements ContextResolver<SecurityType, DefaultSecurityContext> {

    private final File configDir;
    private final Logger logger;

    public SecurityContextResolver(File configDir, Logger logger) {
        this.configDir = configDir;
        this.logger = logger;
    }

    @Override
    public DefaultSecurityContext resolve(SecurityType securityConfig) throws Exception {

        KeyStore keyStore = null;
        String keyStoreRelativePath = null;
        String keyStoreFile = null;
        char[] keyStorePassword = null;
        KeyStore trustStore = null;
        char[] trustStorePassword = null;
        String keyStorePasswordFile = null;
        String trustStoreFile = null;
        File trustStoreFilePath = null;

        if (securityConfig != null) {
            SecurityStoreType keyStoreConfig = securityConfig.getKeystore();
            if (keyStoreConfig != null) {
                keyStoreFile = keyStoreConfig.getFile();
                keyStoreRelativePath = keyStoreFile;
                SecurityStoreType.Type.Enum keyStoreType = keyStoreConfig.getType();
                keyStorePasswordFile = keyStoreConfig.getPasswordFile();

                // The configured paths may be absolute, or may be relative;
                // we need to handle both gracefully.

                if (keyStorePasswordFile != null) {
                    File passwordFilePath = new File(keyStorePasswordFile);
                    if (!passwordFilePath.isAbsolute()) {
                        passwordFilePath = new File(configDir, keyStorePasswordFile);
                    }

                    keyStorePassword = loadKeyStorePassword(passwordFilePath);
                }

                if (keyStoreFile != null) {
                    File keyStoreFilePath = new File(keyStoreFile);
                    if (!keyStoreFilePath.isAbsolute()) {
                        keyStoreFilePath = new File(configDir, keyStoreFile);
                    }

                    keyStore = loadKeyStore(keyStoreType, keyStoreFilePath, keyStorePassword);
                    keyStoreFile = keyStoreFilePath.getAbsolutePath();
                }

            }

            SecurityStoreType trustStoreConfig = securityConfig.getTruststore();
            if (trustStoreConfig != null) {
                trustStoreFile = trustStoreConfig.getFile();
                SecurityStoreType.Type.Enum trustStoreType = trustStoreConfig.getType();

                String trustStorePasswordFile = trustStoreConfig.getPasswordFile();
                if (trustStorePasswordFile != null) {
                    File trustStorePasswordFilePath = new File(trustStorePasswordFile);
                    if (!trustStorePasswordFilePath.isAbsolute()) {
                        trustStorePasswordFilePath = new File(configDir, trustStorePasswordFile);
                    }

                    trustStorePassword = loadKeyStorePassword(trustStorePasswordFilePath);
                }

                if (trustStoreFile != null) {
                    trustStoreFilePath = new File(trustStoreFile);
                    if (!trustStoreFilePath.isAbsolute()) {
                        trustStoreFilePath = new File(configDir, trustStoreFile);
                    }

                    trustStore = loadKeyStore(trustStoreType, trustStoreFilePath, trustStorePassword);
                }
            }
        }

        return new DefaultSecurityContext(keyStore, keyStoreRelativePath, keyStoreFile,
                keyStorePassword, keyStorePasswordFile, trustStore, trustStoreFile,
                trustStoreFilePath == null ? null : trustStoreFilePath.getAbsolutePath(), trustStorePassword);
    }

    private KeyStore loadKeyStore(SecurityStoreType.Type.Enum keyStoreType, File location, char[] password)
            throws Exception {
        KeyStore keyStore = KeyStore.getInstance(keyStoreType == null ? KeyStore.getDefaultType() : keyStoreType.toString());
        FileInputStream in = new FileInputStream(location);
        try {
            keyStore.load(in, password);
        } catch (IOException e) {
            // KG-2251 (more helpful error message)
            logger.error(String.format(
                    "Exception \"%s\" caught loading file \"%s\", you may need to specify \"<type>JCEKS</type>\" in the " +
                            "gateway configuration file",
                    e.getLocalizedMessage(), location));
            throw e;
        }
        in.close();

        return keyStore;
    }

    private static char[] loadKeyStorePassword(File location) throws Exception {
        BufferedReader in = new BufferedReader(new FileReader(location));
        String line = in.readLine();
        in.close();
        return line.toCharArray();
    }

}
