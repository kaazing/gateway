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

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.KeyManagerFactorySpi;
import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.X509KeyManager;

public class SslKeyManagerFactorySpi
    extends KeyManagerFactorySpi {

    private KeyManager[] sslKeyManagers;

    @Override
    protected void engineInit(KeyStore keyStore, char[] password)
        throws NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException {

        String algorithm = KeyManagerFactory.getDefaultAlgorithm();
        KeyManagerFactory defaultKmf = KeyManagerFactory.getInstance(algorithm);
        defaultKmf.init(keyStore, password);

        KeyManager[] keyManagers = defaultKmf.getKeyManagers();
        sslKeyManagers = new KeyManager[keyManagers.length];

        for (int i = 0; i < keyManagers.length; i++) {
            sslKeyManagers[i] = new SslKeyManager((X509KeyManager) keyManagers[i]);
        }
    }

    @Override
    protected void engineInit(ManagerFactoryParameters params)
        throws InvalidAlgorithmParameterException {
	throw new InvalidAlgorithmParameterException("Not implemented");
    }

    @Override
    protected KeyManager[] engineGetKeyManagers() {
        return sslKeyManagers;
    }
}
