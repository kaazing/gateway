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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NestedServicePropertiesConfiguration implements
        Configuration<SuppressibleNestedServicePropertiesConfiguration> {

    private String configElementName;
    private final SuppressibleNestedServicePropertiesConfiguration _configuration;
    private final Map<String, Suppressible<List<String>>> suppressibleSimpleProperties = new HashMap<>();
    private final Map<String, List<String>> simpleProperties = Suppressibles.unsuppressibleMap(suppressibleSimpleProperties);
    private final List<NestedServicePropertiesConfiguration> nestedServiceProperties = new ArrayList<>();

    public NestedServicePropertiesConfiguration(String configElementName) {
        this.configElementName = configElementName;
        _configuration = new SuppressibleNestedServicePropertiesConfigurationImpl();
        _configuration.setSuppression(Suppressibles.getDefaultSuppressions());
    }

    public Map<String, List<String>> getSimpleProperties() {
        return simpleProperties;
    }

    public void addSimpleProperty(String key, String value) {
        if (simpleProperties.containsKey(key)) {
            simpleProperties.get(key).add(value);
        } else {
            List<String> newValue = new ArrayList<String>(Arrays.asList(value));
            simpleProperties.put(key, newValue);
        }
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
        public Map<String, Suppressible<List<String> >> getSimpleProperties() {
            return suppressibleSimpleProperties;
        }

        @Override
        public void addSimpleProperty(String key, Suppressible<String> value) {
            String v = value.value();
            if (suppressibleSimpleProperties.containsKey(key)) {
                suppressibleSimpleProperties.get(key).value().add(v);
            } else {
                List<String> newValue = new ArrayList<String>(Arrays.asList(v));
                Suppressible<List<String>> supValue = new Suppressible<List<String>>(newValue, Suppressibles.getDefaultSuppressions());
                suppressibleSimpleProperties.put(key, supValue);
            }
        }

        @Override
        public String getConfigElementType() {
            return configElementName;
        }
    }

}
