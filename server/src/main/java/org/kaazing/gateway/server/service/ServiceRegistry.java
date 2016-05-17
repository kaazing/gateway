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
package org.kaazing.gateway.server.service;

import static org.kaazing.gateway.resource.address.uri.URIUtils.getAuthority;
import static org.kaazing.gateway.resource.address.uri.URIUtils.getFragment;
import static org.kaazing.gateway.resource.address.uri.URIUtils.getQuery;

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.kaazing.gateway.service.ServiceContext;
import org.kaazing.gateway.service.ServiceRegistration;

public class ServiceRegistry {

    private final Map<String, ServiceAuthority> entries;

    public ServiceRegistry() {
        entries = new HashMap<>();
    }

    public ServiceAuthority register(String authority, ServiceAuthority value) {
        return entries.put(authority, value);
    }

    public ServiceAuthority unregister(String authority) {
        return entries.remove(authority);
    }

    public ServiceAuthority lookup(String authority) {
        return entries.get(authority);
    }

    public ServiceContext register(String serviceURI, ServiceContext serviceContext) {
        if (getQuery(serviceURI) != null || getFragment(serviceURI) != null) {
            throw new IllegalArgumentException("Service URI query and fragment must be null");
        }

        ServiceAuthority serviceAuthority = entries.get(getAuthority(serviceURI));
        if (serviceAuthority == null) {
            serviceAuthority = new ServiceAuthority();
            entries.put(getAuthority(serviceURI), serviceAuthority);
        }

        ServiceRegistration serviceRegistration = serviceAuthority.register(serviceURI, serviceContext);

        return (serviceRegistration != null) ? serviceRegistration.getServiceContext() : null;
    }

    public ServiceContext unregister(URI serviceURI) {
        if (serviceURI.getQuery() != null || serviceURI.getFragment() != null) {
            throw new IllegalArgumentException("Service URI query and fragment must be null");
        }

        ServiceAuthority serviceAuthority = entries.get(serviceURI.getAuthority());
        if (serviceAuthority == null) {
            return null;
        }

        ServiceRegistration serviceRegistration = serviceAuthority.unregister(serviceURI);

        return (serviceRegistration != null) ? serviceRegistration.getServiceContext() : null;
    }

    public ServiceRegistration lookup(URI serviceURI) {
        ServiceAuthority serviceAuthority = entries.get(serviceURI.getAuthority());
        if (serviceAuthority != null) {
            ServiceRegistration serviceRegistration = serviceAuthority.lookup(serviceURI);
            return serviceRegistration;
        }
        return null;
    }

    public Collection<ServiceAuthority> values() {
        return entries.values();
    }
}
