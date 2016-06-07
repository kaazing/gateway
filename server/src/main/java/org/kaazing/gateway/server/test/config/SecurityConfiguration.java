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
package org.kaazing.gateway.server.test.config;

import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SecurityConfiguration implements Configuration<SuppressibleSecurityConfiguration> {

    private final SuppressibleSecurityConfiguration _configuration;

    private final List<RealmConfiguration> realms = new ArrayList<>();

    private Suppressible<KeyStore> _keyStore;
    private Suppressible<char[]> _keyStorePassword;
    private Suppressible<char[]> _trustStorePassword;
    private Suppressible<KeyStore> _trustStore;
    private String keyStoreFile;
    private String trustStoreFile;
    private String trustStorePasswordFile;
    private String keyStorePasswordFile;

    public SecurityConfiguration() {
        _configuration = new SuppressibleSecurityConfigurationImpl();
        _configuration.setSuppression(Suppressibles.getDefaultSuppressions());
    }

    @Override
    public void accept(ConfigurationVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public SuppressibleSecurityConfiguration getSuppressibleConfiguration() {
        return _configuration;
    }

    public List<RealmConfiguration> getRealms() {
        return realms;
    }

    public void setRealms(List<RealmConfiguration> newRealms) {
        realms.clear();
        realms.addAll(newRealms);
    }

    // keystore
    public KeyStore getKeyStore() {
        if (_keyStore == null) {
            return null;
        }
        return _keyStore.value();
    }

    public void setKeyStore(KeyStore keyStore) {
        this._keyStore = new Suppressible<>(keyStore);
    }

    // keyStore password
    public char[] getKeyStorePassword() {
        if (_keyStorePassword == null) {
            return null;
        }
        return _keyStorePassword.value();
    }

    public void setKeyStorePassword(char[] keyStorePassword) {
        this._keyStorePassword = new Suppressible<>(keyStorePassword);
    }

    // trust store
    public KeyStore getTrustStore() {
        if (_trustStore == null) {
            return null;
        }
        return _trustStore.value();
    }

    public void setTrustStore(KeyStore trustStore) {
        this._trustStore = new Suppressible<>(trustStore);
    }

    // keystore file

    /**
     * @param keyStoreFile
     * @Deprecated TODO : remove all file touch points
     */
    @Deprecated
    public void setKeyStoreFile(String keyStoreFile) {
        this.keyStoreFile = keyStoreFile;
    }

    @Deprecated
    public String getKeyStoreFile() {
        return keyStoreFile;
    }

    // truststore file
    @Deprecated
    public void setTrustStoreFile(String trustStoreFile) {
        this.trustStoreFile = trustStoreFile;
    }

    @Deprecated
    public String getTrustStoreFile() {
        return trustStoreFile;
    }

    public char[] getTrustStorePassword() {
        if (_trustStorePassword == null) {
            return null;
        }
        return _trustStorePassword.value();
    }

    public void setTrustStorePassword(char[] trustStorePassword) {

        this._trustStorePassword = new Suppressible<>(trustStorePassword);
    }

    // trust store password file
    @Deprecated
    public String getTrustStorePasswordFile() {
        return trustStorePasswordFile;
    }

    @Deprecated
    public void setTrustStorePasswordFile(String trustStorePasswordFile) {
        this.trustStorePasswordFile = trustStorePasswordFile;
    }

    // key store password file
    @Deprecated
    public String getKeyStorePasswordFile() {
        return keyStorePasswordFile;
    }

    @Deprecated
    public void setKeyStorePasswordFile(String keyStorePasswordFile) {
        this.keyStorePasswordFile = keyStorePasswordFile;
    }

    protected class SuppressibleSecurityConfigurationImpl extends SuppressibleSecurityConfiguration {
        private Set<Suppression> _suppressions;

        @Override
        public Set<org.kaazing.gateway.server.test.config.SuppressibleConfiguration.Suppression> getSuppressions() {
            return _suppressions;
        }

        @Override
        public void setSuppression(Set<org.kaazing.gateway.server.test.config.SuppressibleConfiguration.Suppression>
                                                   suppressions) {
            _suppressions = suppressions;
        }

        // keystore
        @Override
        public Suppressible<KeyStore> getKeyStore() {
            return _keyStore;
        }

        @Override
        public void setKeyStore(Suppressible<KeyStore> keyStore) {
            _keyStore = keyStore;
        }

        // keyStore password
        @Override
        public Suppressible<char[]> getKeyStorePassword() {
            return _keyStorePassword;
        }

        @Override
        public void setKeyStorePassword(Suppressible<char[]> keyStorePassword) {
            _keyStorePassword = keyStorePassword;
        }

        // trust store
        @Override
        public Suppressible<KeyStore> getTrustStore() {
            return _trustStore;
        }

        @Override
        public void setTrustStore(Suppressible<KeyStore> trustStore) {
            _trustStore = trustStore;
        }
    }

}
