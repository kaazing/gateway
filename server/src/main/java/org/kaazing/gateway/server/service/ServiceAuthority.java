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

import static org.kaazing.gateway.resource.address.uri.URIUtils.getPath;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.kaazing.gateway.service.ServiceContext;
import org.kaazing.gateway.service.ServiceRegistration;

public class ServiceAuthority {

    private final List<String> index;
    private final SortedMap<String, ServiceRegistration> entries;

    ServiceAuthority() {
        index = new ArrayList<>();
        entries = new TreeMap<>();
    }

    ServiceRegistration register(String serviceURI, ServiceContext serviceContext) {
        String servicePath = getPath(serviceURI);
        ServiceRegistration oldRegistration = entries.put(servicePath, new ServiceRegistration(serviceURI, serviceContext));
        if (oldRegistration != null) {
            String oldServiceURI = oldRegistration.getURI();
            String oldServicePath = getPath(oldServiceURI);
            index.remove(oldServicePath);
        }

        index.add(servicePath);
        Collections.sort(index);

        return oldRegistration;
    }

    ServiceRegistration unregister(URI serviceURI) {
        ServiceRegistration oldRegistration = entries.remove(serviceURI.getPath());
        if (oldRegistration != null) {
            String oldServiceURI = oldRegistration.getURI();
            String oldServicePath = getPath(oldServiceURI);
            index.remove(oldServicePath);
            Collections.sort(index);
        }

        return oldRegistration;
    }

    public ServiceRegistration lookup(URI serviceURI) {
        String servicePath = serviceURI.getPath();
        return lookup0(servicePath, false);
    }

    private ServiceRegistration lookup0(String servicePath, boolean requiresExact) {
        int position = Collections.binarySearch(index, servicePath);
        if (position >= 0) {
            // found exact match
            return entries.get(index.get(position));
        } else if (!requiresExact) {
            if (servicePath.length() == 0) {
                return lookup0("/", true);
            }

            // no match, find prefix match instead
            int lastSlashAt = servicePath.lastIndexOf("/");
            if (lastSlashAt != -1) {
                return lookup0(servicePath.substring(0, lastSlashAt), false);
            }
        }

        return null;
    }

    public Collection<ServiceRegistration> values() {
        return entries.values();
    }
}
