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
package org.kaazing.gateway.resource.address;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static org.kaazing.gateway.resource.address.ResourceAddress.NEXT_PROTOCOL;
import static org.kaazing.gateway.resource.address.ResourceAddress.TRANSPORT;
import static org.kaazing.gateway.resource.address.uri.URIUtils.getScheme;
import static org.kaazing.gateway.resource.address.uri.URIUtils.uriToString;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;

public class ResourceAddressFactory {

    // scheme name -> factory, for e.g { "ws" -> ws factory, "http" -> http factory, ... }
    private final Map<String, ResourceAddressFactorySpi<?>> addressFactories;

    // scheme name -> { alternate scheme name1 -> factory1, alternate scheme name2 -> factory2,...}
    // "ws" -> { "wse" -> wse factory...}
    private final Map<String, Map<String, ResourceAddressFactorySpi<?>>> alternateAddressFactories;

    public static ResourceAddressFactory newResourceAddressFactory() {
        return newResourceAddressFactory(currentThread().getContextClassLoader());
    }

    @SuppressWarnings("rawtypes")
    public static ResourceAddressFactory newResourceAddressFactory(
            ClassLoader classLoader) {
        ServiceLoader<ResourceAddressFactorySpi> loader = loadResourceAddressFactorySpi(classLoader);

        // scheme name -> factory, for e.g { "ws" -> ws factory, "http" -> http factory, ... }
        Map<String, ResourceAddressFactorySpi<?>> addressFactories = new HashMap<>();

        // scheme name -> { alternate scheme name1 -> factory1, alternate scheme name2 -> factory2,...}
        // "ws" -> { "wse" -> wse factory,  ...}
        Map<String, Map<String, ResourceAddressFactorySpi<?>>> alternateAddressFactories =
                new HashMap<>();

        for (ResourceAddressFactorySpi<?> resourceAddressFactorySpi : loader) {
            String schemeName = resourceAddressFactorySpi.getSchemeName();
            ResourceAddressFactorySpi<?> oldResourceAddressFactorySpi = addressFactories.put(schemeName, resourceAddressFactorySpi);
            if (oldResourceAddressFactorySpi != null) {
                throw new RuntimeException(format("Duplicate scheme resource address factory: %s", schemeName));
            }

            String alternateToScheme = resourceAddressFactorySpi.getRootSchemeName();
            if (alternateToScheme != null) {
                Map<String, ResourceAddressFactorySpi<?>> alternates = alternateAddressFactories.get(alternateToScheme);
                if (alternates == null) {
                    alternates = new HashMap<>();
                    alternateAddressFactories.put(alternateToScheme, alternates);
                }
                alternates.put(schemeName, resourceAddressFactorySpi);
            }
        }

        // If there are no alternate address factories for a scheme, just populate an empty map
        for (Map.Entry<String, Map<String, ResourceAddressFactorySpi<?>>>  e: alternateAddressFactories.entrySet()) {
            String schemeName = e.getKey();
            Map<String, ResourceAddressFactorySpi<?>> alternates = e.getValue();
            alternates = (alternates == null)
                    ? Collections.emptyMap()
                    : Collections.unmodifiableMap(alternates);
            alternateAddressFactories.put(schemeName, alternates);
        }

        // Create ResourceAddressFactory instance
        addressFactories = Collections.unmodifiableMap(addressFactories);
        alternateAddressFactories = Collections.unmodifiableMap(alternateAddressFactories);
        ResourceAddressFactory resourceAddressFactory = new ResourceAddressFactory(addressFactories, alternateAddressFactories);

        for (ResourceAddressFactorySpi<?> resourceAddressFactorySpi : addressFactories.values()) {
            // inject ResourceAddressFactory into ResourceAddressFactorySpi instances
            resourceAddressFactorySpi.setResourceAddressFactory(resourceAddressFactory);
        }

        return resourceAddressFactory;
    }

    @SuppressWarnings("rawtypes")
    private static ServiceLoader<ResourceAddressFactorySpi> loadResourceAddressFactorySpi(ClassLoader classLoader) {
        Class<ResourceAddressFactorySpi> service = ResourceAddressFactorySpi.class;
        return (classLoader != null) ? ServiceLoader.load(service, classLoader) : ServiceLoader.load(service);
    }


    private ResourceAddressFactory(Map<String, ResourceAddressFactorySpi<?>> addressFactories,
                                   Map<String, Map<String, ResourceAddressFactorySpi<?>>> alternateAddressFactories) {
        this.addressFactories = addressFactories;
        this.alternateAddressFactories = alternateAddressFactories;
    }

    public ResourceAddress newResourceAddress(String location) {
        Map<String, Object> EMPTY_OPTIONS = Collections.emptyMap();
        return newResourceAddress(location, EMPTY_OPTIONS);
    }

    // convenience method only, consider removing from API
    public ResourceAddress newResourceAddress(String location, String nextProtocol) {
        if (nextProtocol != null) {
            ResourceOptions options = ResourceOptions.FACTORY.newResourceOptions();
            options.setOption(NEXT_PROTOCOL, nextProtocol);
            return newResourceAddress(location, options);
        }
        else {
            return newResourceAddress(location);
        }
    }

    /**
     * Creates a new resource address for the given location and options
     *
     * @param options cannot be null, otherwise NullPointerException is thrown
     * @return resource address
     */
    public ResourceAddress newResourceAddress(String location, ResourceOptions options) {
        return newResourceAddress(location, options, null /* qualifier */);
    }

    /**
     * Creates a new resource address for the given location and options
     *
     * @param options cannot be null, otherwise NullPointerException is thrown
     * @return resource address
     */
    public ResourceAddress newResourceAddress(String location,
                                              ResourceOptions options,
                                              Object qualifier) {
        Objects.requireNonNull(options, "options cannot be null");
        ResourceAddressFactorySpi<?> resourceAddressFactory = findResourceAddressFactory(getScheme(location));
        return resourceAddressFactory.newResourceAddress(location, options, qualifier);
    }


    public ResourceAddress newResourceAddress(String location, Map<String, Object> options) {
        ResourceAddressFactorySpi<?> resourceAddressFactory = findResourceAddressFactory(getScheme(location));
        return resourceAddressFactory.newResourceAddress(location, options, ResourceOptions.FACTORY);
    }

    public ResourceAddress newResourceAddress(final String location,
                                              final Map<String, Object> options,
                                              final String nextProtocol) {
        ResourceAddressFactorySpi<?> resourceAddressFactory = findResourceAddressFactory(getScheme(location));
        if (nextProtocol != null) {
            return resourceAddressFactory.newResourceAddress(location, options, new ResourceOptions.Factory() {

                @Override
                public ResourceOptions newResourceOptions(ResourceOptions defaults) {
                    ResourceOptions newOptions = ResourceOptions.FACTORY.newResourceOptions(defaults);
                    newOptions.setOption(NEXT_PROTOCOL, nextProtocol);
                    return newOptions;
                }

                @Override
                public ResourceOptions newResourceOptions() {
                    ResourceOptions newOptions = ResourceOptions.FACTORY.newResourceOptions();
                    newOptions.setOption(NEXT_PROTOCOL, nextProtocol);
                    return newOptions;
                }
            });
        }
        else {
            return resourceAddressFactory.newResourceAddress(location, options, ResourceOptions.FACTORY);
        }
    }

    public ResourceAddress newResourceAddress(ResourceAddress uriAddress,
                                              ResourceAddress transportAddress) {
        ResourceOptions options = ResourceOptions.FACTORY.newResourceOptions();
        options.setOption(TRANSPORT, transportAddress);
        options.setOption(NEXT_PROTOCOL, uriAddress.getOption(NEXT_PROTOCOL));
        return newResourceAddress(uriToString(uriAddress.getResource()), options);
    }

    private ResourceAddressFactorySpi<?> findResourceAddressFactory(String schemeName) throws IllegalArgumentException {

        if (schemeName == null) {
            throw new NullPointerException("schemeName");
        }

        ResourceAddressFactorySpi<?> resourceAddressFactory = addressFactories.get(schemeName);
        if (resourceAddressFactory == null) {
            throw new IllegalArgumentException(format("Unable to load scheme '%s': No appropriate resource address factory found", schemeName));
        }

        return resourceAddressFactory;
    }

    public Map<String, ResourceAddressFactorySpi<?>> getAlternateAddressFactories(String schemeName) {
        Map<String, ResourceAddressFactorySpi<?>> alternates = alternateAddressFactories.get(schemeName);
        return alternates == null ? Collections.emptyMap() : alternates;
    }

}
