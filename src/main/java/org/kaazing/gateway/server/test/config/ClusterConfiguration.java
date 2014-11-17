/**
 * Copyright (c) 2007-2014 Kaazing Corporation. All rights reserved.
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.kaazing.gateway.server.test.config;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

public class ClusterConfiguration implements Configuration<SuppressibleClusterConfiguration> {

    private final SuppressibleClusterConfiguration _configuration;

    private final Set<Suppressible<URI>> accepts;
    private final Set<URI> unsurpressibleAccepts;
    private final Set<Suppressible<URI>> connects;
    private final Set<URI> unsurpressibleConnects;

    private Suppressible<String> _name;
    private Suppressible<String> _awsAccessKeyId;
    private Suppressible<String> _awsSecretKeyId;

    public ClusterConfiguration() {
        _configuration = new SuppressibleClusterConfigurationImpl();
        _configuration.setSuppression(Suppressibles.getDefaultSuppressions());

        accepts = new HashSet<Suppressible<URI>>();
        unsurpressibleAccepts = Suppressibles.unsuppressibleSet(accepts);
        connects = new HashSet<Suppressible<URI>>();
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
    public void addAccept(URI acceptURI) {
        unsurpressibleAccepts.add(acceptURI);
    }

    public Set<URI> getAccepts() {
        return unsurpressibleAccepts;
    }

    // connect
    public void addConnect(URI connectURI) {
        unsurpressibleAccepts.add(connectURI);
    }

    public Set<URI> getConnects() {
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
        this._awsAccessKeyId = new Suppressible<String>(awsAccessKeyId);
    }

    // AwsSecretKey
    public String getAwsSecretKeyId() {
        if (_awsSecretKeyId == null) {
            return null;
        }
        return _awsSecretKeyId.value();
    }

    public void setAwsSecretKeyId(String awsSecretKeyId) {
        this._awsSecretKeyId = new Suppressible<String>(awsSecretKeyId);
    }

    // Name
    public String getName() {
        if (_name == null) {
            return null;
        }
        return _name.value();
    }

    public void setName(String name) {
        this._name = new Suppressible<String>(name);
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
        public Set<Suppressible<URI>> getConnects() {
            return connects;
        }

        @Override
        public void addConnect(Suppressible<URI> connect) {
            connects.add(connect);
        }

        // Accepts
        @Override
        public Set<Suppressible<URI>> getAccepts() {
            return accepts;
        }

        @Override
        public void addAccept(Suppressible<URI> accept) {
            accepts.add(accept);
        }
    }
}