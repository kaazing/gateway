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

import java.util.HashSet;
import java.util.Set;

public class ClusterConfiguration implements Configuration<SuppressibleClusterConfiguration> {

    private final SuppressibleClusterConfiguration _configuration;

    private final Set<Suppressible<String>> accepts;
    private final Set<String> unsurpressibleAccepts;
    private final Set<Suppressible<String>> connects;
    private final Set<String> unsurpressibleConnects;

    private Suppressible<String> _name;
    private Suppressible<String> _awsAccessKeyId;
    private Suppressible<String> _awsSecretKeyId;

    public ClusterConfiguration() {
        _configuration = new SuppressibleClusterConfigurationImpl();
        _configuration.setSuppression(Suppressibles.getDefaultSuppressions());

        accepts = new HashSet<>();
        unsurpressibleAccepts = Suppressibles.unsuppressibleSet(accepts);
        connects = new HashSet<>();
        unsurpressibleConnects = Suppressibles.unsuppressibleSet(connects);
    }

    @Override
    public SuppressibleClusterConfiguration getSuppressibleConfiguration() {
        return _configuration;
    }

    @Override
    public void accept(ConfigurationVisitor visitor) {
        visitor.visit(this);
    }

    // accepts
    public void addAccept(String acceptURI) {
        unsurpressibleAccepts.add(acceptURI);
    }

    public Set<String> getAccepts() {
        return unsurpressibleAccepts;
    }

    // connect
    public void addConnect(String connectURI) {
        unsurpressibleAccepts.add(connectURI);
    }

    public Set<String> getConnects() {
        return unsurpressibleConnects;
    }

    // AwsAccessKey

    public String getAwsAccessKeyId() {
        if (_awsAccessKeyId == null) {
            return null;
        }
        return _awsAccessKeyId.value();
    }

    public void setAwsAccessKeyId(String awsAccessKeyId) {
        this._awsAccessKeyId = new Suppressible<>(awsAccessKeyId);
    }

    // AwsSecretKey
    public String getAwsSecretKeyId() {
        if (_awsSecretKeyId == null) {
            return null;
        }
        return _awsSecretKeyId.value();
    }

    public void setAwsSecretKeyId(String awsSecretKeyId) {
        this._awsSecretKeyId = new Suppressible<>(awsSecretKeyId);
    }

    // Name
    public String getName() {
        if (_name == null) {
            return null;
        }
        return _name.value();
    }

    public void setName(String name) {
        this._name = new Suppressible<>(name);
    }

    private class SuppressibleClusterConfigurationImpl extends SuppressibleClusterConfiguration {
        private Set<Suppression> _suppressions;

        @Override
        public Set<Suppression> getSuppressions() {
            return _suppressions;
        }

        @Override
        public void setSuppression(Set<Suppression> suppressions) {
            _suppressions = suppressions;
        }

        // Secret Key
        @Override
        public Suppressible<String> getAwsSecretKeyId() {
            return _awsSecretKeyId;
        }

        @Override
        public void setAwsSecretKeyId(Suppressible<String> awsSecretKeyId) {
            _awsSecretKeyId = awsSecretKeyId;
        }

        // Access Key
        @Override
        public Suppressible<String> getAwsAccessKeyId() {
            return _awsAccessKeyId;
        }

        @Override
        public void setAwsAccessKeyId(Suppressible<String> awsAccessKeyId) {
            _awsAccessKeyId = awsAccessKeyId;
        }

        // Name
        @Override
        public Suppressible<String> getName() {
            return _name;
        }

        @Override
        public void setName(Suppressible<String> name) {
            _name = name;
        }

        // Connects
        @Override
        public Set<Suppressible<String>> getConnects() {
            return connects;
        }

        @Override
        public void addConnect(Suppressible<String> connect) {
            connects.add(connect);
        }

        // Accepts
        @Override
        public Set<Suppressible<String>> getAccepts() {
            return accepts;
        }

        @Override
        public void addAccept(Suppressible<String> accept) {
            accepts.add(accept);
        }
    }
}
