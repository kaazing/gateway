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

import java.util.Set;

public class CrossOriginConstraintConfiguration implements
        Configuration<SuppressibleCrossOriginConstraintConfiguration> {

    private final SuppressibleCrossOriginConstraintConfiguration _configuration;

    private Suppressible<String> _allowOrigin;
    private Suppressible<String> _allowMethods;
    private Suppressible<String> _allowHeaders;

    public CrossOriginConstraintConfiguration() {
        _configuration = new SuppressibleCrossOriginConstraintConfigurationImpl();
        _configuration.setSuppression(Suppressibles.getDefaultSuppressions());
    }

    @Override
    public void accept(ConfigurationVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public SuppressibleCrossOriginConstraintConfiguration getSuppressibleConfiguration() {
        return _configuration;
    }

    public String getAllowOrigin() {
        if (_allowOrigin == null) {
            return null;
        }
        return _allowOrigin.value();
    }

    public void setAllowOrigin(String allowOrigin) {
        this._allowOrigin = new Suppressible<>(allowOrigin);
    }

    public String getAllowMethods() {
        if (_allowMethods == null) {
            return null;
        }
        return _allowMethods.value();
    }

    public void setAllowMethods(String allowMethods) {
        this._allowOrigin = new Suppressible<>(allowMethods);
    }

    public String getAllowHeaders() {
        if (_allowHeaders == null) {
            return null;
        }
        return _allowHeaders.value();
    }

    public void setAllowHeaders(String allowHeaders) {
        this._allowOrigin = new Suppressible<>(allowHeaders);
    }

    private class SuppressibleCrossOriginConstraintConfigurationImpl extends
            SuppressibleCrossOriginConstraintConfiguration {
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
        public Suppressible<String> getAllowOrigin() {
            return _allowOrigin;
        }

        @Override
        public void setAllowOrigin(Suppressible<String> allowOrigin) {
            _allowOrigin = allowOrigin;
        }

        @Override
        public Suppressible<String> getAllowMethods() {
            return _allowMethods;
        }

        @Override
        public void setAllowMethods(Suppressible<String> allowMethods) {
            _allowMethods = allowMethods;
        }

        @Override
        public Suppressible<String> getAllowHeaders() {
            return _allowHeaders;
        }

        @Override
        public void setAllowHeaders(Suppressible<String> allowHeaders) {
            _allowHeaders = allowHeaders;
        }
    }
}
