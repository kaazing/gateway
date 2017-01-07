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

package org.kaazing.gateway.server.spi.security;

import java.security.KeyStore;
import java.util.Map;

/**
 * These options are pre-populated for use in the options of the LoginModule initialize.
 * @see javax.security.auth.spi.LoginModule#initialize(javax.security.auth.Subject, javax.security.auth.callback.CallbackHandler, Map, Map)
 *
 */
public abstract class LoginModuleOptions<T> {

    public static final String CONFIG_DIRECTORY_NAME = "org.kaazing.gateway.CONFIG_DIRECTORY";
    public static final String EXPIRING_STATE_NAME = "org.kaazing.gateway.EXPIRING_STATE";
    public static final String KEYSTORE_NAME = "org.kaazing.gateway.KEYSTORE";
    public static final String TRUSTSTORE_NAME = "org.kaazing.gateway.TRUSTSTORE";

    /**
     * GATEWAY_HOME/conf directory. This is often used to store login module configuration in the conf directory.
     */
    public static final LoginModuleOptions<String> CONFIG_DIRECTORY = new LoginModuleOptions<String>() {

        @Override
        public String get(Map<String, ?> options) {
            return (String) options.get(CONFIG_DIRECTORY_NAME);
        }
    };

    /**
     * Access to the ExpiringState.
     * @see org.kaazing.gateway.server.spi.security.ExpiringState
     */
    public static final LoginModuleOptions<ExpiringState> EXPIRING_STATE = new LoginModuleOptions<ExpiringState>() {

        @Override
        public ExpiringState get(Map<String, ?> options) {
            return (ExpiringState) options.get(EXPIRING_STATE_NAME);
        }
    };

    /**
     * Provides the KeyStore used by the gateway, allowing login modules to access private keys in the KeyStore.
     */
    public static final LoginModuleOptions<KeyStore> KEYSTORE = new LoginModuleOptions<KeyStore>() {


        @Override
        public KeyStore get(Map<String, ?> options) {
            return (KeyStore) options.get(KEYSTORE_NAME);
        }
    };

    /**
     * Provides the TrustStore used by the gateway, allowing login modules to access private keys in the TrustStore.
     */
    public static final LoginModuleOptions<KeyStore> TRUSTSTORE = new LoginModuleOptions<KeyStore>() {


        @Override
        public KeyStore get(Map<String, ?> options) {
            return (KeyStore) options.get(TRUSTSTORE_NAME);
        }
    };

    /**
     * Utility method to easily get value from options map with out manually casting.
     * @param options
     * @return
     */
    public abstract T get(Map<String, ?> options);

}
