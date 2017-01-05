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

public enum LoginModuleOptions {

    CONFIG_DIRECTORY {
        private String propertyName =  "org.kaazing.gateway.CONFIG_DIRECTORY";
        public String get(Map<String,?> options) {
            return (String) options.get(propertyName());
        }
        public String propertyName() {
            return propertyName;
        }
    },
    
    EXPIRING_STATE {
        private String propertyName =  "org.kaazing.gateway.EXPIRING_STATE";
        public ExpiringState get(Map<String,?> options) {
            return (ExpiringState) options.get("org.kaazing.gateway.EXPIRING_STATE");
        }
        public String propertyName() {
            return propertyName;
        }
    },

    KEYSTORE {
        private String propertyName =  "org.kaazing.gateway.KEYSTORE";
        public KeyStore get(Map<String,?> options) {
            return (KeyStore) options.get("org.kaazing.gateway.KEYSTORE");
        }
        public String propertyName() {
            return propertyName;
        }
    },

    TRUSTSTORE {
        private String propertyName =  "org.kaazing.gateway.TRUSTSTORE";
        public KeyStore get(Map<String,?> options) {
            return (KeyStore) options.get("org.kaazing.gateway.TRUSTSTORE");
        }
        public String propertyName() {
            return propertyName;
        }
    };

    
    public abstract Object get(Map<String,?> options);
    public abstract String propertyName();
}