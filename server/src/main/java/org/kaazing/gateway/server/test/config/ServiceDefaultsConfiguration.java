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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ServiceDefaultsConfiguration implements Configuration<SuppressibleServiceDefaultsConfiguration> {
    private final SuppressibleServiceDefaultsConfiguration _configuration;

    private final Map<String, Suppressible<String>> acceptOptions = new HashMap<>();
    private final Map<String, String> unsuppressibleAcceptOptions = Suppressibles.unsuppressibleMap(acceptOptions);
    private final Map<String, Suppressible<String>> connectOptions = new HashMap<>();
    private final Map<String, String> unsuppressibleConnectOptions = Suppressibles.unsuppressibleMap(connectOptions);
    private final Map<String, Suppressible<String>> mimeMappings = new HashMap<>();
    private final Map<String, String> unsuppressibleMimeMappings = Suppressibles.unsuppressibleMap(mimeMappings);

    public ServiceDefaultsConfiguration() {
        _configuration = new SuppressibleServiceDefaultsConfigurationImpl();
        _configuration.setSuppression(Suppressibles.getDefaultSuppressions());
    }

    @Override
    public SuppressibleServiceDefaultsConfiguration getSuppressibleConfiguration() {
        return _configuration;
    }

    @Override
    public void accept(ConfigurationVisitor visitor) {
        visitor.visit(this);
    }

    public Map<String, String> getAcceptOptions() {
        return unsuppressibleAcceptOptions;
    }

    public Map<String, String> getConnectOptions() {
        return unsuppressibleConnectOptions;
    }

    public void addAcceptOption(String key, String value) {
        unsuppressibleAcceptOptions.put(key, value);
    }

    public void addConnectOption(String key, String value) {
        unsuppressibleConnectOptions.put(key, value);
    }

    public Map<String, String> getMimeMappings() {
        return unsuppressibleMimeMappings;
    }

    public void addMimeMapping(String key, String value) {
        unsuppressibleMimeMappings.put(key, value);
    }

    private class SuppressibleServiceDefaultsConfigurationImpl extends SuppressibleServiceDefaultsConfiguration {
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

        @Override
        public Map<String, Suppressible<String>> getAcceptOptions() {
            return acceptOptions;
        }

        @Override
        public Map<String, Suppressible<String>> getConnectOptions() {
            return connectOptions;
        }

        @Override
        public void addAcceptOption(String key, Suppressible<String> value) {
            acceptOptions.put(key, value);
        }

        @Override
        public void addConnectOption(String key, Suppressible<String> value) {
            connectOptions.put(key, value);
        }

        @Override
        public Map<String, Suppressible<String>> getMimeMappings() {
            return mimeMappings;
        }

        @Override
        public void addMimeMapping(String key, Suppressible<String> value) {
            mimeMappings.put(key, value);
        }
    }
}
