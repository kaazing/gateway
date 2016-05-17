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

public class NetworkConfiguration implements Configuration<SuppressibleNetworkConfiguration> {

    private final SuppressibleNetworkConfiguration _configuration;
    private final Map<String, Suppressible<String[]>> mappings = new HashMap<>();
    private final Map<String, String[]> unsuppressibleMappings = Suppressibles.unsuppressibleMap(mappings);

    public NetworkConfiguration() {
        _configuration = new SuppressibleNetworkConfigurationImpl();
        _configuration.setSuppression(Suppressibles.getDefaultSuppressions());
    }

    @Override
    public void accept(ConfigurationVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public SuppressibleNetworkConfiguration getSuppressibleConfiguration() {
        return _configuration;
    }

    public Map<String, String[]> getMappings() {
        return unsuppressibleMappings;
    }

    public void addMapping(String internalAddress, String[] externalAddresses) {
        unsuppressibleMappings.put(internalAddress, externalAddresses);
    }

    private class SuppressibleNetworkConfigurationImpl extends SuppressibleNetworkConfiguration {
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
        public Map<String, Suppressible<String[]>> getMappings() {
            return mappings;
        }

        @Override
        public void addMapping(String internalAddress, Suppressible<String[]> externalAddresses) {
            mappings.put(internalAddress, externalAddresses);
        }
    }
}
