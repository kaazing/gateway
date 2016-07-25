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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ServiceConfiguration implements Configuration<SuppressibleServiceConfiguration> {

    private final SuppressibleServiceConfigurationImpl _configuration;
    private final List<AuthorizationConstraintConfiguration> authorizationConstraints;
    private final List<CrossOriginConstraintConfiguration> crossOriginConstraints;

    private Suppressible<String> _name;
    private Suppressible<String> _type;
    private Suppressible<String> _realmName;
    private Suppressible<String> _description;

    private final Set<Suppressible<String>> balances;
    private final Set<Suppressible<String>> accepts;
    private final Map<String, Suppressible<String>> acceptOptions;
    private final Set<Suppressible<String>> connects;
    private final Map<String, Suppressible<String>> connectOptions;
    private final Map<String, Suppressible<String>> mimeMappings;
    private final Set<String> unsuppressibleAccepts;
    private final Set<String> unsuppressibleBalances;
    private final Map<String, String> unsuppressibleAcceptOptions;
    private final Set<String> unsuppressibleConnects;
    private final Map<String, String> unsuppressibleConnectOptions;
    private final Map<String, String> unsuppressibleMimeMappings;
    private final Map<String, Suppressible<List<String>>> properties;
    private final Map<String, List<String>> unsuppressibleProperties;
    private final List<NestedServicePropertiesConfiguration> nestedProperties;

    public ServiceConfiguration() {
        _configuration = new SuppressibleServiceConfigurationImpl();
        _configuration.setSuppression(Suppressibles.getDefaultSuppressions());

        balances = new HashSet<>();
        unsuppressibleBalances = Suppressibles.unsuppressibleSet(balances);

        accepts = new HashSet<>();
        unsuppressibleAccepts = Suppressibles.unsuppressibleSet(accepts);

        acceptOptions = new HashMap<>();
        unsuppressibleAcceptOptions = Suppressibles.unsuppressibleMap(acceptOptions);

        connects = new HashSet<>();
        unsuppressibleConnects = Suppressibles.unsuppressibleSet(connects);

        connectOptions = new HashMap<>();
        unsuppressibleConnectOptions = Suppressibles.unsuppressibleMap(connectOptions);

        mimeMappings = new HashMap<>();
        unsuppressibleMimeMappings = Suppressibles.unsuppressibleMap(mimeMappings);

        properties = new HashMap<>();
        unsuppressibleProperties = Suppressibles.unsuppressibleMap(properties);

        authorizationConstraints = new LinkedList<>();
        crossOriginConstraints = new LinkedList<>();
        nestedProperties = new LinkedList<>();
    }

    @Override
    public void accept(ConfigurationVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public SuppressibleServiceConfiguration getSuppressibleConfiguration() {
        return _configuration;
    }

    public List<AuthorizationConstraintConfiguration> getAuthorizationConstraints() {
        return authorizationConstraints;
    }

    public List<CrossOriginConstraintConfiguration> getCrossOriginConstraints() {
        return crossOriginConstraints;
    }

    // accept
    public void addAccept(String acceptURI) {
        unsuppressibleBalances.add(acceptURI);
    }

    public Set<String> getAccepts() {
        return unsuppressibleAccepts;
    }

    // balance
    public void addBalance(String balanceURI) {
        unsuppressibleBalances.add(balanceURI);
    }

    public Set<String> getBalances() {
        return unsuppressibleBalances;
    }

    // connect
    public void addConnect(String connectURI) {
        unsuppressibleConnects.add(connectURI);
    }

    public Set<String> getConnects() {
        return unsuppressibleConnects;
    }

    // connect options
    public void addConnectOption(String key, String value) {
        unsuppressibleMimeMappings.put(key, value);
    }

    public Map<String, String> getConnectOptions() {
        return unsuppressibleConnectOptions;
    }

    // mime mapping
    public void addMimeMapping(String key, String value) {
        unsuppressibleMimeMappings.put(key, value);
    }

    public Map<String, String> getMimeMappings() {
        return unsuppressibleMimeMappings;
    }

    // accept options
    public void addAcceptOption(String key, String value) {
        unsuppressibleAcceptOptions.put(key, value);
    }

    public Map<String, String> getAcceptOptions() {
        return unsuppressibleAcceptOptions;
    }

    // description
    public void setDescription(String description) {
        this._description = new Suppressible<>(description);
    }

    public String getDescription() {
        if (_description == null) {
            return null;
        }
        return _description.value();
    }

    // name
    public void setName(String name) {
        this._name = new Suppressible<>(name);
    }

    public String getName() {
        if (_name == null) {
            return null;
        }
        return _name.value();
    }

    // realm name
    public void setRealmName(String realmName) {
        this._realmName = new Suppressible<>(realmName);
    }

    public String getRealmName() {
        if (_realmName == null) {
            return null;
        }
        return _realmName.value();
    }

    // type
    public void setType(String type) {
        this._type = new Suppressible<>(type);
    }

    public String getType() {
        if (_type == null) {
            return null;
        }
        return _type.value();
    }

    // properties
    public List<NestedServicePropertiesConfiguration> getNestedProperties() {
        return nestedProperties;
    }

    public void addNestedProperties(NestedServicePropertiesConfiguration configuration) {
        nestedProperties.add(configuration);
    }

    // properties
    public Map<String, List<String>> getProperties() {
        return unsuppressibleProperties;
    }

    public void addProperty(String key, String value) {
        if (unsuppressibleProperties.containsKey(key)) {
            unsuppressibleProperties.get(key).add(value);
        }
        List<String> newValue = new ArrayList<String>(Arrays.asList(value));
        unsuppressibleProperties.put(key, newValue);
    }

    protected class SuppressibleServiceConfigurationImpl extends SuppressibleServiceConfiguration {
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
        public Set<Suppressible<String>> getAccepts() {
            return accepts;
        }

        @Override
        public void addAccept(Suppressible<String> acceptURI) {
            accepts.add(acceptURI);
        }

        @Override
        public Map<String, Suppressible<List<String>>> getProperties() {
            return properties;
        }

        @Override
        public void addProperty(String key, Suppressible<String> value) {
            String v = value.value();
            if (properties.containsKey(key)) {
                properties.get(key).value().add(v);
            } else {
                List<String> newValue = new ArrayList<String>(Arrays.asList(v));
                Suppressible<List<String>> supValue = new Suppressible<List<String>>(newValue, Suppressibles.getDefaultSuppressions());
                properties.put(key, supValue);
            }
        }

        @Override
        public Suppressible<String> getType() {
            return _type;
        }

        @Override
        public void setType(Suppressible<String> type) {
            _type = type;
        }

        @Override
        public Suppressible<String> getDescription() {
            return _description;
        }

        @Override
        public void setDescription(Suppressible<String> description) {
            _description = description;
        }

        @Override
        public Suppressible<String> getName() {
            return _name;
        }

        @Override
        public void setName(Suppressible<String> name) {
            _name = name;
        }

        @Override
        public Suppressible<String> getRealmName() {
            return _realmName;
        }

        @Override
        public void setRealmName(Suppressible<String> realmName) {
            _realmName = realmName;
        }

        @Override
        public Map<String, Suppressible<String>> getAcceptOptions() {
            return acceptOptions;
        }

        @Override
        public void addAcceptOption(String key, Suppressible<String> value) {
            acceptOptions.put(key, value);
        }

        @Override
        public Set<Suppressible<String>> getBalances() {
            return balances;
        }

        @Override
        public void addBalance(Suppressible<String> balanceURI) {
            balances.add(balanceURI);
        }

        @Override
        public Set<Suppressible<String>> getConnects() {
            return connects;
        }

        @Override
        public void addConnect(Suppressible<String> acceptURI) {
            connects.add(acceptURI);
        }

        @Override
        public Map<String, Suppressible<String>> getConnectOptions() {
            return connectOptions;
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
