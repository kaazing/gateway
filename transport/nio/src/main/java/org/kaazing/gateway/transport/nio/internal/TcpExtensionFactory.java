/*
 * Copyright 2014, Kaazing Corporation. All rights reserved.
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
import static java.util.ServiceLoader.load;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.transport.nio.TcpExtension;
import org.kaazing.gateway.transport.nio.TcpExtensionFactorySpi;

public class TcpExtensionFactory {
    private final List<TcpExtensionFactorySpi> factories;

    /**
     * Creates a new instance of WebSocketExtensionFactory. It uses the default {@link ClassLoader} to load
     * {@link WebSocketExtensionFactorySpi} objects that are registered using META-INF/services.
     *
     * @return WebSocketExtensionFactory
     */
    public static TcpExtensionFactory newInstance() {
        ServiceLoader<TcpExtensionFactorySpi> services = load(TcpExtensionFactorySpi.class);
        return newInstance(services);
    }

    /**
     * Creates a new instance of WebSocketExtensionFactory. It uses the specified {@link ClassLoader} to load
     * {@link WebSocketExtensionFactorySpi} objects that are registered using META-INF/services.
     *
     * @return WebSocketExtensionFactory
     */
    public static TcpExtensionFactory newInstance(ClassLoader cl) {
        ServiceLoader<TcpExtensionFactorySpi> services = load(TcpExtensionFactorySpi.class, cl);
        return newInstance(services);
    }
    
    public List<TcpExtension> bind(ResourceAddress address) {
        List<TcpExtension> extensions = new ArrayList<TcpExtension>(factories.size());
        for (TcpExtensionFactorySpi factory : factories) {
            TcpExtension extension = factory.bind(address);
            if (extension != null) {
               extensions.add(extension); 
            }
        }
        return extensions;
    }
    
    private static TcpExtensionFactory newInstance(ServiceLoader<TcpExtensionFactorySpi> services) {
        List<TcpExtensionFactorySpi> factories = new ArrayList<TcpExtensionFactorySpi>();
        for (TcpExtensionFactorySpi service : services) {
            factories.add(service);
        }
        return new TcpExtensionFactory(unmodifiableList(factories));
    }
    
    private TcpExtensionFactory(List<TcpExtensionFactorySpi> factories) {
        this.factories = factories;
    }

}
