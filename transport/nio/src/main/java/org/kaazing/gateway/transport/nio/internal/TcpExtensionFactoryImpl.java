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
package org.kaazing.gateway.transport.nio.internal;

import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ServiceLoader;

import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.transport.nio.TcpExtension;
import org.kaazing.gateway.transport.nio.TcpExtensionFactorySpi;

public class TcpExtensionFactoryImpl implements TcpExtensionFactory {
    private final List<TcpExtensionFactorySpi> factories;

    /* (non-Javadoc)
     * @see org.kaazing.gateway.transport.nio.internal.TcpExtensionFactory#bind(org.kaazing.gateway.resource.address.ResourceAddress)
     */
    @Override
    public List<TcpExtension> bind(ResourceAddress address) {
        List<TcpExtension> extensions = new ArrayList<>(factories.size());
        for (TcpExtensionFactorySpi factory : factories) {
            TcpExtension extension = factory.bind(address);
            if (extension != null) {
               extensions.add(extension); 
            }
        }
        return extensions;
    }
    
    /* (non-Javadoc)
     * @see org.kaazing.gateway.transport.nio.internal.TcpExtensionFactory#availableExtensions()
     */
    @Override
    public Collection<TcpExtensionFactorySpi> availableExtensions() {
       return factories;
    }
    
    static TcpExtensionFactory newInstance(ServiceLoader<TcpExtensionFactorySpi> services) {
        List<TcpExtensionFactorySpi> factories = new ArrayList<>();
        for (TcpExtensionFactorySpi service : services) {
            factories.add(service);
        }
        return new TcpExtensionFactoryImpl(unmodifiableList(factories));
    }
    
    private TcpExtensionFactoryImpl(List<TcpExtensionFactorySpi> factories) {
        this.factories = factories;
    }

}
