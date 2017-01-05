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

package org.kaazing.gateway.server;

import java.security.KeyStore;
import java.util.Map;

/**
 * These options are pre-populated for use in the options of the LoginModule initialize.
 * @see javax.security.auth.spi.LoginModule#initialize(javax.security.auth.Subject, javax.security.auth.callback.CallbackHandler, Map, Map)
 *
 */
public enum LoginModuleOptions {

    /**
     * GATEWAY_HOME/conf directory. This is often used to store login module configuration in the conf directory.
     */
    CONFIG_DIRECTORY {
        private String propertyName = "org.kaazing.gateway.CONFIG_DIRECTORY";

        public String get(Map<String, ?> options) {
            return (String) options.get(propertyName());
        }

        public String propertyName() {
            return propertyName;
        }
    },

    /**
     * Access to the ExpiringState.
     * @see org.kaazing.gateway.server.ExpiringState
     */
    EXPIRING_STATE {
        private String propertyName = "org.kaazing.gateway.EXPIRING_STATE";

        public ExpiringState get(Map<String, ?> options) {
            return (ExpiringState) options.get("org.kaazing.gateway.EXPIRING_STATE");
        }

        public String propertyName() {
            return propertyName;
        }
    },

    /**
     * Provides the KeyStore used by the gateway, allowing login modules to access private keys in the KeyStore.
     */
    KEYSTORE {
        private String propertyName = "org.kaazing.gateway.KEYSTORE";

        public KeyStore get(Map<String, ?> options) {
            return (KeyStore) options.get("org.kaazing.gateway.KEYSTORE");
        }

        public String propertyName() {
            return propertyName;
        }
    },

    /**
     * Provides the TrustStore used by the gateway, allowing login modules to access private keys in the TrustStore.
     */
    TRUSTSTORE {
        private String propertyName = "org.kaazing.gateway.TRUSTSTORE";

        public KeyStore get(Map<String, ?> options) {
            return (KeyStore) options.get("org.kaazing.gateway.TRUSTSTORE");
        }

        public String propertyName() {
            return propertyName;
        }
    };

    /**
     * Utility method to easily get value from options map with out manually casting.
     * @param options
     * @return
     */
    public abstract Object get(Map<String, ?> options);

    /**
     * Name and key the option is saved as.
     * @return
     */
    public abstract String propertyName();
}
