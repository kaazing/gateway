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
package org.kaazing.gateway.service;

import static java.lang.String.format;
import static java.util.Collections.unmodifiableMap;
import static java.util.ServiceLoader.load;

import java.util.Collection;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ServiceFactory {

    private final Map<String, ServiceFactorySpi> serviceFactoriesByType;

    private ServiceFactory(Map<String, ServiceFactorySpi> serviceFactoriesByType) {
        this.serviceFactoriesByType = unmodifiableMap(serviceFactoriesByType);
    }

    public static ServiceFactory newServiceFactory() {
        return newServiceFactory(load(ServiceFactorySpi.class));
    }

    public static ServiceFactory newServiceFactory(ClassLoader loader) {
        return newServiceFactory(load(ServiceFactorySpi.class, loader));
    }

    public Service newService(String serviceType) {
        ServiceFactorySpi serviceFactory = serviceFactoriesByType.get(serviceType);
        if (serviceFactory == null) {
            throw new IllegalArgumentException("Unrecognized service type: " + serviceType);
        }
        return serviceFactory.newService(serviceType);
    }

    private static ServiceFactory newServiceFactory(ServiceLoader<ServiceFactorySpi> serviceFactories) {
        ConcurrentMap<String, ServiceFactorySpi> serviceFactoriesByType = new ConcurrentHashMap<>();
        for (ServiceFactorySpi serviceFactory : serviceFactories) {
            Collection<String> serviceTypes = serviceFactory.getServiceTypes();
            for (String serviceType : serviceTypes) {
                ServiceFactorySpi oldServiceFactory = serviceFactoriesByType.putIfAbsent(serviceType, serviceFactory);
                if (oldServiceFactory != null) {
                    // TODO: ignore, log warning?
                    throw new RuntimeException(format("Duplicate type service factory: %s", serviceType));
                }
            }
        }
        return new ServiceFactory(serviceFactoriesByType);
    }
}
