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

import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509KeyManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.kaazing.gateway.transport.ssl.bridge.filter.SslCertificateSelectionFilter;

public class SslKeyManager
    extends X509ExtendedKeyManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(SslKeyManager.class);
    
    X509KeyManager keyManager;

    private void handleException(String task, Exception e) {
        if (LOGGER.isDebugEnabled()) {
            // SSLEngine swallows exceptions (e.g. NPE that caused KG-1789) so
            // log here for diagnostic purposes
            String msg = String.format("Unexpected exception while %s", task);
            LOGGER.debug(msg, e);
        }

        if (e instanceof RuntimeException) {
            throw (RuntimeException) e;
        }
    }
    
    public SslKeyManager(X509KeyManager keyManager) {
        this.keyManager = keyManager;
    }

    @Override
    public String[] getServerAliases(String keyType, Principal[] issuers) {
        String[] serverAliases = null;

        try {
            serverAliases = keyManager.getServerAliases(keyType, issuers);

        } catch (RuntimeException re) {
            String task = String.format("getting server aliases for keyType %s, issuers %s", keyType, Arrays.toString(issuers));
            handleException(task, re);
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(String.format("provided server aliases %s for keyType %s, issuers %s", Arrays.toString(serverAliases), keyType, Arrays.toString(issuers)));
        }

        return serverAliases;
    }
    
    @Override
    public PrivateKey getPrivateKey(String alias) {
        return keyManager.getPrivateKey(alias);
    }
    
    @Override
    public String[] getClientAliases(String keyType, Principal[] issuers) {
        String[] clientAliases = null;

        try {
            clientAliases = keyManager.getClientAliases(keyType, issuers);

        } catch (Exception e) {
            String task = String.format("getting client aliases for keyType %s, issuers %s", keyType, Arrays.toString(issuers));
            handleException(task, e);
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(String.format("provided client aliases %s for keyType %s, issuers %s", Arrays.toString(clientAliases), keyType, Arrays.toString(issuers)));
        }

        return clientAliases;
    }
    
    @Override
    public X509Certificate[] getCertificateChain(String alias) {
        return keyManager.getCertificateChain(alias);
    }
    
    @Override
    public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
        return this.chooseEngineClientAlias(keyType, issuers, null);
    }

    @Override
    public String chooseEngineClientAlias(String[] keyTypes,
                                          Principal[] issuers,
                                          SSLEngine engine) {

        try {
            Collection<String> availCertAliases = SslCertificateSelectionFilter.getAvailableClientCertAliases();
            if (availCertAliases == null) {
                return keyManager.chooseClientAlias(keyTypes, issuers, null);
            }

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(String.format("available client aliases: %s", availCertAliases));
            }

            for (String keyType : keyTypes) {
                String[] aliases = this.getClientAliases(keyType, issuers);
                if (aliases != null) {
                    for (String alias : aliases) {
                        if (availCertAliases.contains(alias)) {
                            if (LOGGER.isTraceEnabled()) {
                                LOGGER.trace(String.format("chose client alias '%s' for keyTypes %s, issuers %s, engine %s", alias, Arrays.toString(keyTypes), Arrays.toString(issuers), engine));
                            }

                            return alias;
                        }
                    }
                }
            }

        } catch (RuntimeException re) {
            String task = String.format("choosing client alias for keyTypes %s, issuers %s, engine %s", Arrays.toString(keyTypes), Arrays.toString(issuers), engine);
            handleException(task, re);
        }

        return keyManager.chooseClientAlias(keyTypes, issuers, null);
    }

    @Override
    public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
        return this.chooseEngineServerAlias(keyType, issuers, null);
    }
    
    @Override
    public String chooseEngineServerAlias(String keyType,
                                          Principal[] issuers,
                                          SSLEngine engine) {
        try {
            Collection<String> availCertAliases = SslCertificateSelectionFilter.getAvailableServerCertAliases();
            if (availCertAliases == null) {
                return null;
            }

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(String.format("available server aliases: %s", availCertAliases));
            }

            String[] aliases = this.getServerAliases(keyType, issuers);
            if (aliases != null) {
                for (String alias : aliases) {
                    if (availCertAliases.contains(alias)) {
                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace(String.format("chose server alias '%s' for keyType %s, issuers %s, engine %s", alias, keyType, Arrays.toString(issuers), engine));
                        }

                        return alias;
                    }
                }
            }

        } catch (RuntimeException re) {
            String task = String.format("choosing server alias for keyType %s, issuers %s, engine %s", keyType, Arrays.toString(issuers), engine);
            handleException(task, re);
        }

        return null; 
    }
}
