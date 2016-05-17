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

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.management.MBeanServer;
import org.kaazing.gateway.server.Gateway;

public class GatewayConfiguration implements Configuration<SuppressibleGatewayConfiguration> {

    private final SuppressibleGatewayConfiguration _configuration;
    private final Map<String, String> properties;
    private File webRootDirectory;
    private File tempDirectory;
    private MBeanServer jmxMBeanServer;

    private final List<ServiceConfiguration> services;
    private SecurityConfiguration security;
    private ClusterConfiguration cluster;
    private ServiceDefaultsConfiguration serviceDefaultsConfiguration;
    private NetworkConfiguration networkConfiguration;

    public GatewayConfiguration() {
        _configuration = new SuppressibleGatewayConfigurationImpl();
        _configuration.setSuppression(Suppressibles.getDefaultSuppressions());
        services = new LinkedList<>();
        properties = new HashMap<>();
    }

    @Override
    public void accept(ConfigurationVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public SuppressibleGatewayConfiguration getSuppressibleConfiguration() {
        return _configuration;
    }

    public File getWebRootDirectory() {
        return webRootDirectory;
    }

    public void setWebRootDirectory(File webRootDirectory) {
        this.webRootDirectory = webRootDirectory;
    }

    public File getTempDirectory() {
        return tempDirectory;
    }

    public void setTempDirectory(File tempDirectory) {
        this.tempDirectory = tempDirectory;
    }

    public SecurityConfiguration getSecurity() {
        return security;
    }

    public void setSecurity(SecurityConfiguration security) {
        this.security = security;
    }

    public Collection<ServiceConfiguration> getServices() {
        return services;
    }

    public void setServices(List<ServiceConfiguration> newServices) {
        services.clear();
        services.addAll(newServices);
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> newProperties) {
        properties.clear();
        properties.putAll(newProperties);
    }

    public ClusterConfiguration getCluster() {
        return cluster;
    }

    public void setCluster(ClusterConfiguration cluster) {
        this.cluster = cluster;
    }

    public ServiceDefaultsConfiguration getServiceDefaults() {
        return serviceDefaultsConfiguration;
    }

    public void setServiceDefaults(ServiceDefaultsConfiguration serviceDefaultsConfiguration) {
        this.serviceDefaultsConfiguration = serviceDefaultsConfiguration;
    }

    public MBeanServer getJmxMBeanServer() {
        return jmxMBeanServer;
    }

    public void setJmxMBeanServer(MBeanServer jmxMBeanServer) {
        this.jmxMBeanServer = jmxMBeanServer;
    }

    public NetworkConfiguration getNetwork() {
        return networkConfiguration;
    }

    public void setNetwork(NetworkConfiguration configuration) {
        this.networkConfiguration = configuration;
    }

    public String getConfigDir() {
        return properties.get(Gateway.GATEWAY_CONFIG_DIRECTORY_PROPERTY);
    }

    private class SuppressibleGatewayConfigurationImpl extends SuppressibleGatewayConfiguration {
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

    }
}
