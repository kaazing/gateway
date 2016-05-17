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
package org.kaazing.gateway.management.service;

import java.util.Collection;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.kaazing.gateway.management.gateway.GatewayManagementBean;
import org.kaazing.gateway.service.ServiceContext;
import static java.lang.String.format;
import static java.util.ServiceLoader.load;

public final class ServiceManagementBeanFactory {

    private ConcurrentMap<String, ServiceManagementBeanFactorySpi> serviceMgmtFactoriesByType;

    private ServiceManagementBeanFactory(
            ConcurrentMap<String, ServiceManagementBeanFactorySpi> serviceMgmtFactoriesByType) {
        this.serviceMgmtFactoriesByType = serviceMgmtFactoriesByType;
    }

    private static ServiceManagementBeanFactory newServiceManagmentBeanFactory(ServiceLoader<ServiceManagementBeanFactorySpi>
                                                                                       serviceFactories) {
        ConcurrentMap<String, ServiceManagementBeanFactorySpi> serviceMgmtFactoriesByType =
                new ConcurrentHashMap<>();
        for (ServiceManagementBeanFactorySpi ServiceManagmentBeanFactory : serviceFactories) {
            Collection<String> serviceTypes = ServiceManagmentBeanFactory.getServiceTypes();
            for (String serviceType : serviceTypes) {
                ServiceManagementBeanFactorySpi oldServiceManagmentBeanFactory = serviceMgmtFactoriesByType
                        .putIfAbsent(serviceType, ServiceManagmentBeanFactory);
                if (oldServiceManagmentBeanFactory != null) {
                    // TODO: ignore, log warning?
                    throw new RuntimeException(format("Duplicate type service managment bean factory: %s", serviceType));
                }
            }
        }
        return new ServiceManagementBeanFactory(serviceMgmtFactoriesByType);
    }

    public static ServiceManagementBeanFactory newServiceManagementBeanFactory() {
        return newServiceManagmentBeanFactory(load(ServiceManagementBeanFactorySpi.class));
    }

    public static ServiceManagementBeanFactory newServiceManagementBeanFactory(ClassLoader loader) {
        return newServiceManagmentBeanFactory(load(ServiceManagementBeanFactorySpi.class, loader));
    }

    public ServiceManagementBean newServiceManagementBean(String serviceType,
                                                          GatewayManagementBean gatewayManagementBean,
                                                          ServiceContext serviceContext) {
        ServiceManagementBeanFactorySpi serviceFactory = serviceMgmtFactoriesByType.get(serviceType);
        ServiceManagementBean result;
        if (serviceFactory == null) {
            result = new ServiceManagementBean.DefaultServiceManagementBean(gatewayManagementBean, serviceContext) {
            };
        } else {
            result = serviceFactory.newServiceManagementBean(gatewayManagementBean, serviceContext);
        }
        return result;
    }
}
