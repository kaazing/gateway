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
package org.kaazing.gateway.management.update.check;

import java.util.Collection;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import static java.util.Collections.unmodifiableMap;
import static java.util.ServiceLoader.load;

public final class ManagementUpdateCheckFactory {

    private final Map<String, ManagementUpdateCheckFactorySpi> factoriesByProtocol;

    private ManagementUpdateCheckFactory(Map<String, ManagementUpdateCheckFactorySpi> serviceFactoriesByType) {
        this.factoriesByProtocol = unmodifiableMap(serviceFactoriesByType);
    }

    public static ManagementUpdateCheckFactory newManagementUpdateCheckFactory() {
        return newManagementUpdateCheckFactory(load(ManagementUpdateCheckFactorySpi.class));
    }

    public static ManagementUpdateCheckFactory newManagementUpdateCheckFactory(ClassLoader loader) {
        return newManagementUpdateCheckFactory(load(ManagementUpdateCheckFactorySpi.class, loader));
    }

    public ManagementUpdateCheck newUpdateCheck(String protocol) {
        ManagementUpdateCheckFactorySpi managementUpdateCheckFactory = factoriesByProtocol.get(protocol);
        if (managementUpdateCheckFactory == null) {
            throw new IllegalArgumentException("Could not find factory for protocol: " + protocol);
        }
        return managementUpdateCheckFactory.newManagementUpdateCheck(protocol);
    }

    private static ManagementUpdateCheckFactory newManagementUpdateCheckFactory(ServiceLoader<ManagementUpdateCheckFactorySpi>
                                                                                        serviceFactories) {
        ConcurrentMap<String, ManagementUpdateCheckFactorySpi> serviceFactoriesByType =
                new ConcurrentHashMap<>();
        for (ManagementUpdateCheckFactorySpi managementUpdateCheckFactory : serviceFactories) {
            Collection<String> serviceTypes = managementUpdateCheckFactory.getSupportedProtocols();
            for (String protocol : serviceTypes) {
                ManagementUpdateCheckFactorySpi oldManagementUpdateCheckFactory = serviceFactoriesByType.putIfAbsent(
                        protocol, managementUpdateCheckFactory);
                if (oldManagementUpdateCheckFactory != null) {
                    // NOOP, Just take the first one
                }
            }
        }
        return new ManagementUpdateCheckFactory(serviceFactoriesByType);
    }
}
