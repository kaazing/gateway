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

public class LoginModuleConfiguration implements Configuration<SuppressibleLoginModuleConfiguration> {

    private final SuppressibleLoginModuleConfiguration _configuration;

    private Suppressible<String> _type;
    private Suppressible<String> _success;
    private final Map<String, Suppressible<String>> options = new HashMap<>();
    private final Map<String, String> unsuppressibleOptions = Suppressibles.unsuppressibleMap(options);

    public LoginModuleConfiguration() {
        _configuration = new SuppressibleLoginModuleConfigurationImpl();
        _configuration.setSuppression(Suppressibles.getDefaultSuppressions());
    }

    @Override
    public void accept(ConfigurationVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public SuppressibleLoginModuleConfiguration getSuppressibleConfiguration() {
        return _configuration;
    }

    // Type
    public String getType() {
        if (_type == null) {
            return null;
        }
        return _type.value();
    }

    public void setType(String type) {
        this._type = new Suppressible<>(type);
    }

    // Success
    public String getSuccess() {
        if (_success == null) {
            return null;
        }
        return _success.value();
    }

    public void setSuccess(String success) {
        this._success = new Suppressible<>(success);
    }

    // Options
    public Map<String, String> getOptions() {
        return unsuppressibleOptions;
    }

    public void addOption(String optionKey, String optionValue) {
        unsuppressibleOptions.put(optionKey, optionValue);
    }

    private class SuppressibleLoginModuleConfigurationImpl extends SuppressibleLoginModuleConfiguration {
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
        public Suppressible<String> getType() {
            return _type;
        }

        @Override
        public void setType(Suppressible<String> type) {
            _type = type;
        }

        @Override
        public Suppressible<String> getSuccess() {
            return _success;
        }

        @Override
        public void setSuccess(Suppressible<String> success) {
            _success = success;
        }

        @Override
        public Map<String, Suppressible<String>> getOptions() {
            return options;
        }

        @Override
        public void addOption(String key, Suppressible<String> value) {
            options.put(key, value);
        }
    }
}
