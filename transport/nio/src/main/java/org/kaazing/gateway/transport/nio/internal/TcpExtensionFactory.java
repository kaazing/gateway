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

import static java.util.ServiceLoader.load;

import java.util.Collection;
import java.util.List;
import java.util.ServiceLoader;

import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.transport.nio.TcpExtension;
import org.kaazing.gateway.transport.nio.TcpExtensionFactorySpi;

public interface TcpExtensionFactory {

    List<TcpExtension> bind(ResourceAddress address);

    Collection<TcpExtensionFactorySpi> availableExtensions();

    /**
     * Creates a new instance of WebSocketExtensionFactory. It uses the specified {@link ClassLoader} to load
     * {@link WebSocketExtensionFactorySpi} objects that are registered using META-INF/services.
     *
     * @return WebSocketExtensionFactory
     */
    static TcpExtensionFactory newInstance(ClassLoader cl) {
        ServiceLoader<TcpExtensionFactorySpi> services = load(TcpExtensionFactorySpi.class, cl);
        return TcpExtensionFactoryImpl.newInstance(services);
    }

    /**
     * Creates a new instance of WebSocketExtensionFactory. It uses the default {@link ClassLoader} to load
     * {@link WebSocketExtensionFactorySpi} objects that are registered using META-INF/services.
     *
     * @return WebSocketExtensionFactory
     */
    static TcpExtensionFactory newInstance() {
        ServiceLoader<TcpExtensionFactorySpi> services = load(TcpExtensionFactorySpi.class);
        return TcpExtensionFactoryImpl.newInstance(services);
    }

}