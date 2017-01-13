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
package org.kaazing.gateway.transport;

import static java.lang.String.format;
import static java.util.Collections.unmodifiableMap;
import static java.util.ServiceLoader.load;

import java.lang.reflect.Method;
import java.net.Proxy;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ServiceLoader;
import java.util.Set;

import javax.annotation.Resource;

import org.kaazing.gateway.resource.address.Protocol;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.transport.dispatch.ProtocolDispatcher;
import org.kaazing.mina.util.ExceptionMonitor;


public class TransportFactory {

    private final Map<String, Transport> transportsByName;
    private final Map<String, Transport> transportsBySchemeName;
    private final Map<Proxy.Type, ProxyHandler> proxyHandlersByType;
    private final Map<String, Protocol> protocolsBySchemeName;
    private final Map<String, ProtocolDispatcher> dispatchersByProtocolName;

    static{
        ExceptionMonitor.setInstance(new TransportExceptionMonitor());
    }

    private TransportFactory(Map<String, Transport> transportsByName,
                             Map<String, Transport> transportsBySchemeName,
                             Map<Proxy.Type, ProxyHandler> proxyHandlersByType,
                             Map<String, Protocol> protocolsBySchemeName,
                             Map<String, ProtocolDispatcher> dispatchersByProtocolName) {
        this.transportsByName = unmodifiableMap(transportsByName);
        this.transportsBySchemeName = unmodifiableMap(transportsBySchemeName);
        this.proxyHandlersByType = unmodifiableMap(proxyHandlersByType);
        this.protocolsBySchemeName = unmodifiableMap(protocolsBySchemeName);
        this.dispatchersByProtocolName = unmodifiableMap(dispatchersByProtocolName);
    }

    /**
     * Creates a new transport factory. The factory is configured using given configuration.
     *
     * @param configuration for the newly created transport factory
     * @return a transport factory
     */
    public static TransportFactory newTransportFactory(Map<String, ?> configuration) {
        return newTransportFactory(load(TransportFactorySpi.class), configuration);
    }

    public static TransportFactory newTransportFactory(ClassLoader loader, Map<String, ?> configuration) {
        return newTransportFactory(load(TransportFactorySpi.class, loader), configuration);
    }

    public Transport getTransport(String transportName) {
        Transport transport = transportsByName.get(transportName);
        if (transport == null) {
            throw new IllegalArgumentException("Unrecognized transport: " + transportName);
        }
        return transport;
    }

    public Transport getTransportForScheme(String schemeName) {
        if (schemeName.contains("tls")) {
            schemeName = schemeName.replace("tls", "ssl");
        }
        Transport transport = transportsBySchemeName.get(schemeName);
        if (transport == null) {
            throw new IllegalArgumentException("Unrecognized scheme:" + schemeName);
        }
        return transport;
    }

    public Set<String> getTransportNames() {
        return transportsByName.keySet();
    }

    private static TransportFactory newTransportFactory(ServiceLoader<TransportFactorySpi> transportFactories, Map<String, ?> configuration) {
        Map<String, Transport> transportsByName = new HashMap<>();
        Map<String, Transport> transportsBySchemeName = new HashMap<>();
        Map<Proxy.Type, ProxyHandler> proxyHandlersByType = new HashMap<>();
        Map<String, Protocol> protocolsBySchemeName = new HashMap<>();
        Map<String, ProtocolDispatcher> dispatchersByProtocolName = new HashMap<>();

        for (TransportFactorySpi transportFactory : transportFactories) {

            if (!transportFactory.isEnabled(configuration)) {
                continue;
            }

            String transportName = transportFactory.getTransportName();
            if (transportsByName.containsKey(transportName)) {
                throw new RuntimeException(format("Duplicate transport name transport factory: %s", transportName));
            }

            Collection<String> schemeNames = transportFactory.getSchemeNames();
            for(String schemeName : schemeNames) {
                if (transportsBySchemeName.containsKey(schemeName)) {
                    throw new RuntimeException(format("Duplicate scheme name transport factory: %s", transportName));
                }

            }
            Transport transport = transportFactory.newTransport(configuration);
            transportsByName.put(transportName, transport);
            for(String schemeName : schemeNames) {
                transportsBySchemeName.put(schemeName, transport);
            }

            proxyHandlersByType.putAll(transport.getProxyHandlers());

            protocolsBySchemeName.putAll(transport.getProtocols());

            // Collect all protocol dispatchers for a transport
            dispatchersByProtocolName.putAll(transport.getProtocolDispatchers());
        }

        return new TransportFactory(transportsByName, transportsBySchemeName, proxyHandlersByType,
                protocolsBySchemeName, dispatchersByProtocolName);
    }

    public BridgeAcceptor getAcceptor(ResourceAddress address) {
        return transportsBySchemeName.get(address.getResource().getScheme()).getAcceptor(address);
    }

    public BridgeConnector getConnector(ResourceAddress address) {
        return transportsBySchemeName.get(address.getResource().getScheme()).getConnector(address);
    }

    public Map<Proxy.Type, ProxyHandler> getProxyHandlers() {
        return proxyHandlersByType;
    }

    /**
     * Returns {@link Protocol} for an URI
     *
     * @param uri resource address URI
     * @return protocol for the given URI
     */
    public Protocol getProtocol(URI uri) {
        return getProtocol(uri.getScheme());
    }

    /**
     * Returns {@link Protocol} for an URI scheme
     *
     * @param scheme scheme of resource address URI
     * @return protocol for the given URI scheme
     */
    public Protocol getProtocol(String scheme) {
        return protocolsBySchemeName.get(scheme);
    }


    /**
     * Returns a map of protocol dispatchers (protocol name -> protocol dispatcher).
     *
     * @return map of (protocol name -> protocol dispatcher). If none found, returns an empty map.
     */
    public Map<String, ProtocolDispatcher> getProtocolDispatchers() {
        return dispatchersByProtocolName;
    }

    public ProtocolDispatcher getProtocolDispatcher(String protocolName) {
        return dispatchersByProtocolName.get(protocolName);
    }

    /**
     * Inject the given resources plus all available transport acceptors and connectors into every available acceptor
     * and connector.
     * @param resources Resources that should be injected (in addition to available transport acceptors and connectors)
     * @return A map containing the given input resources plus entries for the acceptor and connector of each available
     * transport. This can be used to inject resources into other objects which require acceptors or connectors to be injected.
     */
    public Map<String, Object> injectResources(Map<String, Object> resources) {
        Map<String, Object> allResources = new HashMap<>(resources);
        for (Entry<String, Transport> entry : transportsByName.entrySet()) {
            allResources.put(entry.getKey() + ".acceptor", entry.getValue().getAcceptor());
            allResources.put(entry.getKey() + ".connector", entry.getValue().getConnector());
        }
        for (Transport transport : transportsByName.values()) {
            BridgeAcceptor acceptor = transport.getAcceptor();
            if (acceptor != null) {
                injectResources(acceptor, allResources);
            }
            BridgeConnector connector = transport.getConnector();
            if (connector != null) {
                injectResources(transport.getConnector(), allResources);
            }
            for (Object extension: transport.getExtensions()) {
                injectResources(extension, allResources);
            }
        }
        return allResources;
    }

    private void injectResources(Object target, Map<String, Object> resources) {
        Class<?> clazz = target.getClass();

        for (Method method : clazz.getMethods()) {
            Resource resource = method.getAnnotation(Resource.class);
            if (resource != null) {
                String name = resource.name();
                Object val = resources.get(name);
                if (val != null) {
                    try {
                        method.invoke(target, val);
                    } catch (Exception e) {
                        throw new RuntimeException("Error while injecting named " + name + " resource", e);
                    }
                }
            }
        }
    }

}
