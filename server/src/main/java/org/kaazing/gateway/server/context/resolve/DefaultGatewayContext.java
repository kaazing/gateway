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
package org.kaazing.gateway.server.context.resolve;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.security.RealmContext;
import org.kaazing.gateway.server.context.GatewayContext;
import org.kaazing.gateway.server.context.SchemeContext;
import org.kaazing.gateway.server.context.ServiceDefaultsContext;
import org.kaazing.gateway.server.context.TransportContext;
import org.kaazing.gateway.server.service.ServiceRegistry;
import org.kaazing.gateway.service.ServiceContext;
import org.kaazing.gateway.service.cluster.ClusterContext;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;

public class DefaultGatewayContext implements GatewayContext {

    // private static final Logger LOG = LoggerFactory.getLogger(Gateway.class);
    private final ClusterContext cluster;

    private final Map<String, DefaultSchemeContext> schemes;
    private final Map<String, DefaultTransportContext> schemeTransports;
    private final RealmsContext realms;
    private final ServiceRegistry servicesByURI;
    private final Collection<ServiceContext> services;
    private final File webDir;
    private final File tempDir;
    private final SchedulerProvider schedulerProvider;
    private final ServiceDefaultsContext serviceDefaults;

    // TODO: remove this from here when the launcher goes away and the GatewayContext, Launcher,
    // and Gateway become one class that can keep track of injectables itself
    private final Map<String, Object> injectables = new HashMap<>();

    public DefaultGatewayContext(Map<String, DefaultSchemeContext> schemes,
                                 Map<String, DefaultTransportContext> schemeTransports /* by scheme name */,
                                 RealmsContext realms,
                                 ServiceDefaultsContext serviceDefaults,
                                 Collection<ServiceContext> services,
                                 ServiceRegistry servicesByURI,
                                 File webDir,
                                 File tempDir,
                                 ClusterContext cluster,
                                 SchedulerProvider schedulerProvider) {

        this.schemes = schemes;
        this.schemeTransports = schemeTransports;
        this.realms = realms;
        this.serviceDefaults = serviceDefaults;
        this.services = services;
        this.servicesByURI = servicesByURI;
        this.webDir = webDir;
        this.tempDir = tempDir;
        this.cluster = cluster;
        this.schedulerProvider = schedulerProvider;
    }

    @Override
    public Collection<ServiceContext> getServices() {
        return services;
    }

    @Override
    public DefaultSchemeContext getScheme(String name) {
        return schemes.get(name);
    }

    @Override
    public DefaultTransportContext getTransportForScheme(String schemeName) {
        return schemeTransports.get(schemeName);
    }

    @Override
    public ServiceRegistry getServiceRegistry() {
        return servicesByURI;
    }

    @Override
    public RealmContext getRealm(String name) {
        return realms.getRealmContext(name);
    }

    @Override
    public Collection<? extends RealmContext> getRealms() {
        return realms.getRealms();
    }

    @Override
    public void dispose() {
        HashSet<TransportContext<ResourceAddress>> transports = new HashSet<>();

        for (SchemeContext scheme : schemes.values()) {
            TransportContext<ResourceAddress> transport = getTransportForScheme(scheme.getName());
            if (transport != null) {
                transports.add(transport);
            }
        }

        for (TransportContext<?> transport : transports) {
            transport.dispose();
        }

        cluster.dispose();

        schedulerProvider.shutdownNow();
    }

    @Override
    public File getTempDirectory() {
        return tempDir;
    }

    public File getWebDirectory() {
        return webDir;
    }

    @Override
    public ClusterContext getCluster() {
        return cluster;
    }

    @Override
    public ServiceDefaultsContext getServiceDefaults() {
        return serviceDefaults;
    }

    @Override
    public Map<String, Object> getInjectables() {
        return injectables;
    }

}
