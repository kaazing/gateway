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
package org.kaazing.gateway.server.test.config.builder;

import java.io.File;
import java.security.KeyStore;
import java.util.Set;
import org.kaazing.gateway.server.test.config.RealmConfiguration;
import org.kaazing.gateway.server.test.config.SecurityConfiguration;
import org.kaazing.gateway.server.test.config.Suppressible;
import org.kaazing.gateway.server.test.config.SuppressibleConfiguration.Suppression;

public abstract class AbstractSecurityConfigurationBuilder<R> extends
        AbstractConfigurationBuilder<SecurityConfiguration, R> {

    public AbstractSecurityConfigurationBuilder<R> keyStore(KeyStore keyStore) {
        configuration.getSuppressibleConfiguration().setKeyStore(
                new Suppressible<>(keyStore, getCurrentSuppressions()));
        return this;
    }

    /**
     * @param keyStoreFile
     * @return
     * @deprecated so we can get rid of all file touchpoints
     */
    @Deprecated
    public AbstractSecurityConfigurationBuilder<R> keyStoreFile(String keyStoreFile) {
        configuration.setKeyStoreFile(keyStoreFile);
        return this;
    }

    /**
     * @param keyStoreFile
     * @return
     * @deprecated so we can get rid of all file touchpoints
     */
    @Deprecated
    public AbstractSecurityConfigurationBuilder<R> trustStoreFile(String trustStoreFile) {
        configuration.setTrustStoreFile(trustStoreFile);
        return this;
    }

    public AbstractSecurityConfigurationBuilder<R> keyStorePassword(char[] keyStorePassword) {
        configuration.setKeyStorePassword(keyStorePassword);
        return this;
    }

    public AbstractSecurityConfigurationBuilder<R> trustStorePassword(char[] trustStorePassword) {
        configuration.setTrustStorePassword(trustStorePassword);
        return this;
    }

    @Deprecated
    public AbstractSecurityConfigurationBuilder<R> keyStorePasswordFile(File keyStorePasswordFile) {
        configuration.setKeyStorePasswordFile(keyStorePasswordFile.getAbsolutePath());
        return this;
    }

    @Deprecated
    public AbstractSecurityConfigurationBuilder<R> trustStorePasswordFile(File trustStorePasswordFile) {
        configuration.setTrustStorePasswordFile(trustStorePasswordFile.getAbsolutePath());
        return this;
    }

    public AbstractSecurityConfigurationBuilder<R> trustStore(KeyStore trustStore) {
        configuration.getSuppressibleConfiguration().setTrustStore(
                new Suppressible<>(trustStore, getCurrentSuppressions()));
        return this;
    }

    public abstract AbstractRealmConfigurationBuilder<? extends AbstractSecurityConfigurationBuilder<R>> realm();

    public AbstractSecurityConfigurationBuilder(SecurityConfiguration configuration, R result,
                                                Set<Suppression> suppressions) {
        super(configuration, result, suppressions);
    }

    public static class AddRealmBuilder<R extends AbstractSecurityConfigurationBuilder<?>> extends
            AbstractRealmConfigurationBuilder<R> {
        public AddRealmBuilder(R result, Set<Suppression> suppressions) {
            super(new RealmConfiguration(), result, suppressions);
        }

        @Override
        public AddLoginModuleBuilder<AddRealmBuilder<R>> loginModule() {
            return new AddLoginModuleBuilder<>(this, getCurrentSuppressions());
        }

        @Override
        public R done() {
            result.configuration.getRealms().add(configuration);
            return super.done();
        }
    }

    @Override
    public AbstractSecurityConfigurationBuilder<R> suppress(Suppression... suppressions) {
        super.addCurrentSuppressions(suppressions);
        return this;
    }

}
