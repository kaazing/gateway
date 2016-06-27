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
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.kaazing.gateway.resource.address.ResourceAddress.ALTERNATE;
import static org.kaazing.gateway.resource.address.ResourceAddress.BIND_ALTERNATE;
import static org.kaazing.gateway.resource.address.ResourceAddress.IDENTITY_RESOLVER;
import static org.kaazing.gateway.resource.address.ResourceAddress.NEXT_PROTOCOL;
import static org.kaazing.gateway.resource.address.ResourceAddress.QUALIFIER;
import static org.kaazing.gateway.resource.address.ResourceAddress.RESOLVER;
import static org.kaazing.gateway.resource.address.ResourceAddress.TRANSPORT;
import static org.kaazing.gateway.resource.address.ResourceAddress.TRANSPORTED_URI;
import static org.kaazing.gateway.resource.address.ResourceAddress.TRANSPORT_URI;
import static org.kaazing.gateway.resource.address.uri.URIUtils.getPort;
import static org.kaazing.gateway.resource.address.uri.URIUtils.getScheme;
import static org.kaazing.gateway.resource.address.uri.URIUtils.modifyURIPort;
import static org.kaazing.gateway.resource.address.uri.URIUtils.modifyURIScheme;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class ResourceAddressFactorySpi<T extends ResourceAddress> {

    private static final String NO_ADDRESSES_AVAILABLE_FOR_BINDING_FORMATTER =
            " No addresses available for binding for URI: %s.";
    private static final Map<String, Object> EMPTY_OPTIONS = emptyMap();

    private ResourceAddressFactory addressFactory;

    public void setResourceAddressFactory(ResourceAddressFactory addressFactory) {
        this.addressFactory = addressFactory;
    }

    protected ResourceAddressFactory getResourceAddressFactory() {
        return addressFactory;
    }

    /**
     * Returns the name of the scheme provided by factories using this
     * service provider.
     */
    public abstract String getSchemeName();

    public final T newResourceAddress(String location) {
        return newResourceAddress(location, new HashMap<>(EMPTY_OPTIONS));
    }
    
    public final T newResourceAddress(String location, Map<String, Object> optionsByName) {
        return newResourceAddress(location, optionsByName, ResourceOptions.FACTORY);
    }

    /**
     * Returns a {@link ResourceAddress} instance for the named scheme.
     *
     */
    public final T newResourceAddress(String location,
                                      Map<String, Object> optionsByName,
                                      ResourceOptions.Factory optionsFactory) {
        if (location == null) {
            throw new NullPointerException("location");
        }

        if (optionsByName == null) {
            throw new NullPointerException("optionsByName");
        }

        String schemeName = getSchemeName();
        if (schemeName == null) {
            throw new NullPointerException("schemeName");
        }

        String transportName = getTransportName();
        if (transportName == null) {
            throw new NullPointerException("transportName");
        }

        if (!schemeName.equals(getScheme(location))) {
            throw new IllegalArgumentException(format("Expected scheme \"%s\" for URI: %s", schemeName, location));
        }

        ResourceOptions options = optionsFactory.newResourceOptions();

        setAlternateOption(location, options, optionsByName);

        stripOptionPrefixes(optionsByName);

        String external = location;

        // make the external port implicit
        if (getPort(external) == getSchemePort()) {
            location = modifyURIPort(location, -1);
        }
        
        // make the internal port explicit
        if (getPort(location) == -1) {
            location = modifyURIPort(location, getSchemePort());
        }

        // convert the scheme to transport
        if (!transportName.equals(getSchemeName())) {
            location = modifyURIScheme(location, transportName);
        }

        parseNamedOptions(location, options, optionsByName);

        // create a list of alternate addresses for this location
        List<T> addresses = newResourceAddresses0(external, location, options);

        // fill in the alternate addresses by walking the list in reverse order
        T alternate = null;
        for (int i=addresses.size() - 1; i >= 0; i--) {
            T address = addresses.get(i);

            if (alternate != null) {
                ResourceOptions newOptions = ResourceOptions.FACTORY.newResourceOptions(options);
                newOptions.setOption(ALTERNATE, alternate);
                setOptions(address, location, newOptions, null);
            }
            else {
                setOptions(address, location, options, null);
            }

            // validate the resource transport name
            URI resource = address.getResource();
            if (!transportName.equals(resource.getScheme())) {
                throw new IllegalArgumentException(format("Expected scheme \"%s\" for resource address URI: %s", transportName, resource));
            }

            alternate = address;
        }

        if (addresses.isEmpty()) {
            throwNoAddressesToBindError(location);
        }

        return addresses.get(0);
    }

    protected void setAlternateOption(String location,
                                      ResourceOptions options,
                                      Map<String, Object> optionsByName) {
        // by default resource addresses do not have alternative addresses.
        // over-ride this to share the incoming optionsByName map values across any alternative addresses.
    }

    protected void stripOptionPrefixes(Map<String, Object> optionsByName) {
        String transportName = getTransportName();
        String protocolName = getProtocolName();
        String transportFormat = format("%s.", transportName);

        List<String> prefixes;
        if (protocolName != null) {
            String transportAsProtocolFormat = format("%s[%s].", transportName, protocolName);
            prefixes = asList(transportFormat, transportAsProtocolFormat);
        } else {
            prefixes = Collections.singletonList(transportFormat);
        }

        // TODO: scheme-prefixed option names?
        // strip off current transport prefix for option names
        for (String prefix : prefixes) {
            Collection<String> prefixedOptionNames = new HashSet<>();
            final Set<Map.Entry<String,Object>> entrySet = optionsByName.entrySet();
            for (Map.Entry<String, Object> entry : entrySet) {
                String optionName = entry.getKey();
                if (optionName.startsWith(prefix)) {
                    prefixedOptionNames.add(optionName);
                }
            }
            int prefixLength = prefix.length();
            for (String prefixedOptionName : prefixedOptionNames) {
                Object optionValue = optionsByName.remove(prefixedOptionName);
                String newOptionName = prefixedOptionName.substring(prefixLength);
                optionsByName.put(newOptionName, optionValue);
            }
        }
    }

    public final T newResourceAddress(String location, ResourceOptions options, Object qualifier) {
        
        String external = location;
        
        // make the port explicit
        if (getPort(location) == -1) {
            location = modifyURIPort(location, getSchemePort());
        }
        
        // convert the scheme to transport
        String transportName = getTransportName();
        if (!transportName.equals(getSchemeName())) {
            location = modifyURIScheme(location, transportName);
        }

        // create a list of alternate addresses for this location
        List<T> addresses = newResourceAddresses0(external, location, options);

        // fill in the alternate addresses by walking the list in reverse order
        T alternate = null;
        for (int i=addresses.size() - 1; i >= 0; i--) {
            T address = addresses.get(i);
            if (alternate != null) {
                ResourceOptions newOptions = ResourceOptions.FACTORY.newResourceOptions(options);
                newOptions.setOption(ALTERNATE, alternate);
                setOptions(address, location, newOptions, qualifier);
            }
            else {
                setOptions(address, location, options, qualifier);
            }

            // validate the resource transport name
            URI resource = address.getResource();
            if (!transportName.equals(resource.getScheme())) {
                throw new IllegalArgumentException(format("Expected scheme \"%s\" for resource address URI: %s", transportName, resource));
            }

            alternate = address;
        }

        if (addresses.isEmpty()) {
            throwNoAddressesToBindError(location);
        }

        return addresses.get(0);
    }

    protected List<T> newResourceAddresses0(String original, String location, ResourceOptions options) {
        return singletonList(newResourceAddress0(original, location, options));
    }
    
    // note: extra hook for tcp.bind / udp.bind which changes location
    protected T newResourceAddress0(String original, String location,  ResourceOptions options) {

        final T address = newResourceAddress0(original, location);
        setOptions(address, location, options, options.getOption(QUALIFIER));
        return address;
    }

    protected abstract T newResourceAddress0(String original, String location);

    private void setOptions(T address, String location, ResourceOptions options, Object qualifier) {
        // default the transport
        ResourceAddress transport = options.getOption(TRANSPORT);
        String transportURI = options.getOption(TRANSPORT_URI);

        if (transport == null && addressFactory != null) {
            ResourceOptions newOptions = ResourceOptions.FACTORY.newResourceOptions(options);
            if (transportURI == null) {
                ResourceFactory factory = getTransportFactory();
                if (factory != null) {
                    transportURI = factory.createURI(location);
                    newOptions.setOption(TRANSPORT_URI, transportURI);
                }
            }
            if (transportURI != null) {
                // TODO: make ResourceOptions hierarchical to provide options here?
                ResourceOptions transportOptions = ResourceOptions.FACTORY.newResourceOptions();
                transportOptions.setOption(NEXT_PROTOCOL, getProtocolName());
                URI locationURI = URI.create(location);
                transportOptions.setOption(TRANSPORTED_URI, locationURI);
                transport = addressFactory.newResourceAddress(transportURI, transportOptions);
            }

            newOptions.setOption(TRANSPORT, transport);
            options = newOptions;
        }

        setOptions(address, options, qualifier);
    }

    protected void setOptions(T address, ResourceOptions options, Object qualifier) {

        Object newQualifier = options.getOption(QUALIFIER);
        if (newQualifier == null) {
            newQualifier = qualifier;
        }

        if (options.hasOption(NEXT_PROTOCOL)) {
            address.setOption0(NEXT_PROTOCOL, options.getOption(NEXT_PROTOCOL));
        }

        if (options.hasOption(TRANSPORT_URI)) {
            address.setOption0(TRANSPORT_URI, options.getOption(TRANSPORT_URI));
        }

        if (options.hasOption(TRANSPORT)) {
            address.setOption0(TRANSPORT, options.getOption(TRANSPORT));
        }

        if (options.hasOption(ALTERNATE)) {
            address.setOption0(ALTERNATE, options.getOption(ALTERNATE));
        }

        // JRF: still relevant for address after name resolution?
        if (options.hasOption(RESOLVER)) {
            address.setOption0(RESOLVER, options.getOption(RESOLVER));
        }

        if (options.hasOption(BIND_ALTERNATE)) {
            address.setOption0(BIND_ALTERNATE, options.getOption(BIND_ALTERNATE));
        }

        if (options.hasOption(TRANSPORTED_URI)) {
            address.setOption0(TRANSPORTED_URI, options.getOption(TRANSPORTED_URI));
        }

        if (options.hasOption(IDENTITY_RESOLVER)) {
            address.setOption0(IDENTITY_RESOLVER, options.getOption(IDENTITY_RESOLVER));
        }

        if (qualifier != null || options.hasOption(QUALIFIER)) {
            address.setOption0(QUALIFIER, newQualifier);
        }
    }

    /**
     * Returns the default port for the scheme provided by factories using
     * this service provider.
     * 
     * @return the default scheme port
     */
    protected int getSchemePort() {
        return -1;
    }

    /**
     * Returns the name of the transport for created ResourceAddresses.
     */
    protected abstract String getTransportName();
    
    /*
     * Create a resource address with options.
     */
    protected final void parseNamedOptions(String location,
                                           ResourceOptions options,
                                           Map<String, Object> optionsByName) {

        String nextProtocol = (String) optionsByName.remove(NEXT_PROTOCOL.name());
        if (nextProtocol != null) {
            options.setOption(NEXT_PROTOCOL, nextProtocol);
        }
        
        Object qualifier = optionsByName.remove(QUALIFIER.name());
        if (qualifier != null) {
            options.setOption(QUALIFIER, qualifier);
        }

        String transportURI = (String) optionsByName.remove(TRANSPORT.name());
        if (transportURI == null) {
            ResourceFactory factory = getTransportFactory();
            if (factory != null) {
                transportURI = factory.createURI(location);
            }
        }
        if (transportURI != null) {
            options.setOption(TRANSPORT_URI, transportURI);
        }

        ResourceAddress alternate = (ResourceAddress) optionsByName.remove(ALTERNATE.name());
        if (alternate != null) {
            options.setOption(ALTERNATE, alternate);
        }

        NameResolver resolver = (NameResolver) optionsByName.remove(RESOLVER.name());
        if (resolver != null) {
            options.setOption(RESOLVER, resolver);
        }

        Boolean bindAlternate = (Boolean) optionsByName.remove(BIND_ALTERNATE.name());
        if (bindAlternate != null) {
            options.setOption(BIND_ALTERNATE, bindAlternate);
        }

        URI transportedURI = (URI) optionsByName.remove(TRANSPORTED_URI.name());
        if (transportedURI != null) {
            options.setOption(TRANSPORTED_URI, transportedURI);
        }

        IdentityResolver identityResolver = (IdentityResolver) optionsByName.remove(IDENTITY_RESOLVER.name());
        if (identityResolver != null) {
            options.setOption(IDENTITY_RESOLVER, identityResolver);
        }

        // scheme-specific options
        parseNamedOptions0(location, options, optionsByName);

        // all address options consumed, now create transport address with options by name
        ResourceAddress transport = null;
        if (transportURI != null && addressFactory != null) {
            String protocolName = getProtocolName();
            if (optionsByName == Collections.<String,Object>emptyMap()) {
                optionsByName = new HashMap<>();
            }
            URI locationURI = URI.create(location);
            optionsByName.put(TRANSPORTED_URI.name(), locationURI);
            transport = addressFactory.newResourceAddress(transportURI, optionsByName, protocolName);
        }
        if (transport != null) {
            options.setOption(TRANSPORT, transport);
        }
    }

    protected void parseNamedOptions0(String location, ResourceOptions options,
                                      Map<String, Object> optionsByName) {
    }

    @SuppressWarnings("unchecked")
    protected T newResourceAddressWithAlternate(String location,
                                                Map<String, Object> optionsByName,
                                                ResourceAddress alternateAddress) {

        optionsByName.put(ALTERNATE.name(), alternateAddress);
        return (T) getResourceAddressFactory().newResourceAddress(location, optionsByName);
    }

    /**
     * These options are removed and set in {@link #parseNamedOptions(java.net.URI, ResourceOptions, java.util.Map }
     * above, so we need to include them in the new options by name map used for alternate construction.
     */

    @SuppressWarnings("JavadocReference")
    protected Map<String, Object> getNewOptionsByName(ResourceOptions options, Map<String, Object> optionsByName) {
        Map<String,Object> clonedOptionsByName = new HashMap<>(optionsByName);
        clonedOptionsByName.put(NEXT_PROTOCOL.name(), options.getOption(NEXT_PROTOCOL));
        clonedOptionsByName.put(QUALIFIER.name(), options.getOption(QUALIFIER));
        clonedOptionsByName.put(TRANSPORT_URI.name(), options.getOption(TRANSPORT_URI));
        clonedOptionsByName.put(BIND_ALTERNATE.name(), options.getOption(BIND_ALTERNATE));
        clonedOptionsByName.put(RESOLVER.name(), options.getOption(RESOLVER));

        return clonedOptionsByName;
    }
    
    protected abstract ResourceFactory getTransportFactory();
    
    protected abstract String getProtocolName();

    /*
     * Returns the scheme name of the root or "alternate to" resource address.
     * For e.g if this is wse resource resource factory, it would return "ws"
     * as wse is alternate to ws
     *
     * @return null if this is not an alternate to any other resource address
     *     Otherwise, the scheme name of the that resource address.
     */
    protected String getRootSchemeName() {
        return null;
    }

    /**
     * Throws general exception when no addresses to bind are found.
     * @param location
     */
    private void throwNoAddressesToBindError(String location) {
        throw new IllegalArgumentException(format(NO_ADDRESSES_AVAILABLE_FOR_BINDING_FORMATTER, location));
    }

}
