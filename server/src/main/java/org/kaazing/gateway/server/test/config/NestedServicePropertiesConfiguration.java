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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NestedServicePropertiesConfiguration implements
        Configuration<SuppressibleNestedServicePropertiesConfiguration> {

    private String configElementName;
    private final SuppressibleNestedServicePropertiesConfiguration _configuration;
    private final Map<String, Suppressible<String>> suppressibleSimpleProperties = new HashMap<>();
    private final Map<String, String> simpleProperties = Suppressibles.unsuppressibleMap(suppressibleSimpleProperties);
    private final List<NestedServicePropertiesConfiguration> nestedServiceProperties = new ArrayList<>();

    public NestedServicePropertiesConfiguration(String configElementName) {
        this.configElementName = configElementName;
        _configuration = new SuppressibleNestedServicePropertiesConfigurationImpl();
        _configuration.setSuppression(Suppressibles.getDefaultSuppressions());
    }

    public Map<String, String> getSimpleProperties() {
        return simpleProperties;
    }

    public void addSimpleProperty(String key, String value) {
        simpleProperties.put(key, value);
    }

    public Collection<NestedServicePropertiesConfiguration> getNestedProperties() {
        return nestedServiceProperties;
    }

    public void addNestedProperties(NestedServicePropertiesConfiguration configuration) {
        nestedServiceProperties.add(configuration);
    }

    public String getConfigElementName() {
        return configElementName;
    }

    public void setConfigElementName(String configElementName) {
        this.configElementName = configElementName;
    }

    @Override
    public void accept(ConfigurationVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public SuppressibleNestedServicePropertiesConfiguration getSuppressibleConfiguration() {
        return _configuration;
    }

    private class SuppressibleNestedServicePropertiesConfigurationImpl extends
            SuppressibleNestedServicePropertiesConfiguration {
        private Set<Suppression> _suppressions;

        @Override
        public Set<Suppression> getSuppressions() {
            return _suppressions;
        }

        @Override
        public void setSuppression(Set<Suppression> suppressions) {
            _suppressions = suppressions;
        }

        @Override
        public Map<String, Suppressible<String>> getSimpleProperties() {
            return suppressibleSimpleProperties;
        }

        @Override
        public void addSimpleProperty(String key, Suppressible<String> value) {
            suppressibleSimpleProperties.put(key, value);
        }

        @Override
        public String getConfigElementType() {
            return configElementName;
        }
    }

}
