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
package org.kaazing.gateway.resource.address.ws;

import static java.lang.String.format;
import static org.kaazing.gateway.resource.address.ResourceAddress.ALTERNATE;
import static org.kaazing.gateway.resource.address.ResourceAddress.BIND_ALTERNATE;
import static org.kaazing.gateway.resource.address.ResourceFactories.changeSchemeOnly;
import static org.kaazing.gateway.resource.address.ws.WsResourceAddress.CODEC_REQUIRED;
import static org.kaazing.gateway.resource.address.ws.WsResourceAddress.INACTIVITY_TIMEOUT;
import static org.kaazing.gateway.resource.address.ws.WsResourceAddress.LIGHTWEIGHT;
import static org.kaazing.gateway.resource.address.ws.WsResourceAddress.MAX_MESSAGE_SIZE;
import static org.kaazing.gateway.resource.address.ws.WsResourceAddress.REQUIRED_PROTOCOLS;
import static org.kaazing.gateway.resource.address.ws.WsResourceAddress.SUPPORTED_PROTOCOLS;
import static org.kaazing.gateway.resource.address.ws.WsResourceAddress.TRANSPORT_NAME;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.resource.address.ResourceAddressFactorySpi;
import org.kaazing.gateway.resource.address.ResourceFactory;
import org.kaazing.gateway.resource.address.ResourceOptions;

public class WsResourceAddressFactorySpi extends ResourceAddressFactorySpi<WsResourceAddress> {

    private static final String SCHEME_NAME = "ws";
    private static final int SCHEME_PORT = 80;
    private static final ResourceFactory TRANSPORT_FACTORY = changeSchemeOnly("http");
    private static final String PROTOCOL_NAME = "ws/rfc6455";

    // go backwards so we can set alternate addresses correctly
    private static final List<String> WS_ALTERNATE_SCHEMES =
            Arrays.asList("wsx-draft", "ws-draft", "wsx", "wse");

    private List<ResourceFactory> alternateResourceFactories;

    @Override
    public String getSchemeName() {
        return SCHEME_NAME;
    }

    @Override
    protected int getSchemePort() {
        return SCHEME_PORT;
    }

    @Override
    protected String getTransportName() {
        return TRANSPORT_NAME;
    }

    @Override
    protected ResourceFactory getTransportFactory() {
        return TRANSPORT_FACTORY;
    }

    @Override
    protected String getProtocolName() {
        return PROTOCOL_NAME;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void parseNamedOptions0(String location,
                                      ResourceOptions options,
                                      Map<String, Object> optionsByName) {


        Boolean codecRequired = (Boolean) optionsByName.remove(CODEC_REQUIRED.name());
        if (codecRequired != null) {
            options.setOption(CODEC_REQUIRED, codecRequired);
        }

        Boolean lightweight = (Boolean) optionsByName.remove(LIGHTWEIGHT.name());
        if (lightweight != null) {
            options.setOption(LIGHTWEIGHT, lightweight);
        }
        
        Integer maxMessageSize = (Integer) optionsByName.remove(MAX_MESSAGE_SIZE.name());
        if (maxMessageSize != null) {
            options.setOption(MAX_MESSAGE_SIZE, maxMessageSize);
        }

        Long inactivityTimeout = (Long) optionsByName.remove(INACTIVITY_TIMEOUT.name());
        if (inactivityTimeout != null) {
            options.setOption(INACTIVITY_TIMEOUT, inactivityTimeout);
        }
        
        String[] supportedProtocols = (String[]) optionsByName.remove(SUPPORTED_PROTOCOLS.name());
        if (supportedProtocols != null) {
            options.setOption(SUPPORTED_PROTOCOLS, supportedProtocols);
        }
        
        String[] requiredProtocols = (String[]) optionsByName.remove(REQUIRED_PROTOCOLS.name());
        if (requiredProtocols != null) {
            options.setOption(REQUIRED_PROTOCOLS, requiredProtocols);
        }
    }

    @Override
    public void setResourceAddressFactory(ResourceAddressFactory addressFactory) {
        super.setResourceAddressFactory(addressFactory);

        Map<String, ResourceAddressFactorySpi<?>> alternateAddressFactories =
                addressFactory.getAlternateAddressFactories(getSchemeName());
        alternateAddressFactories = new HashMap<>(alternateAddressFactories);
        List<ResourceFactory> alternateResourceFactories = new ArrayList<>();
        // Create an ordered list of resource factories for the available alternate address factories
        for(String scheme : WS_ALTERNATE_SCHEMES) {
            if (alternateAddressFactories.get(scheme) != null) {
                alternateResourceFactories.add(changeSchemeOnly(scheme));
                alternateAddressFactories.remove(scheme);
            }
        }

        // Remaining ones don't have any order
        for(String scheme : alternateAddressFactories.keySet()) {
            alternateResourceFactories.add(changeSchemeOnly(scheme));
        }
        this.alternateResourceFactories = Collections.unmodifiableList(alternateResourceFactories);
    }

    protected List<ResourceFactory> getAlternateResourceFactories() {
        return this.alternateResourceFactories;
    }

    @Override
    protected void setAlternateOption(String location,
                                      ResourceOptions options,
                                      Map<String, Object> optionsByName) {

        List<ResourceFactory> resourceFactories = getAlternateResourceFactories();

        if (resourceFactories != null &&
            !resourceFactories.isEmpty() &&
            getResourceAddressFactory() != null) {

            // create alternate addresses

            WsResourceAddress alternateAddress = null;
            for (ResourceFactory resourceFactory: resourceFactories) {
                alternateAddress = newResourceAddressWithAlternate(
                        resourceFactory.createURI(location),
                        getNewOptionsByName(options, optionsByName),
                        alternateAddress);
            }

            // save the alternate chain into this address.
            options.setOption(ALTERNATE, alternateAddress);
        }
    }

    @Override
    protected WsResourceAddress newResourceAddress0(String original, String location) {

        URI uriLocation = URI.create(location);
        String path = uriLocation.getPath();

        if (uriLocation.getHost() == null) {
            throw new IllegalArgumentException(format("Missing host in URI: %s", location));
        }

        if (uriLocation.getPort() == -1) {
            throw new IllegalArgumentException(format("Missing port in URI: %s", location));
        }

        if (path == null || path.length() == 0) {
            throw new IllegalArgumentException(format("Missing path in URI: %s", location));
        }

        return new WsResourceAddress(this, original, uriLocation);
    }

    @Override
    protected void setOptions(WsResourceAddress address, ResourceOptions options, Object qualifier) {


        // WsAcceptor will handle this
        options.setOption(BIND_ALTERNATE, Boolean.FALSE);

        super.setOptions(address, options, qualifier);

        // Mid-level websocket sessions are lightweight in the sense they
        // pass up decoded message payload and pass down encoded message payload.
        if ( "x-kaazing-handshake".equals(address.getOption(ResourceAddress.NEXT_PROTOCOL)) ) {
            options.setOption(WsResourceAddress.LIGHTWEIGHT, Boolean.TRUE);
        }

        address.setOption0(CODEC_REQUIRED, options.getOption(CODEC_REQUIRED));
        address.setOption0(LIGHTWEIGHT, options.getOption(LIGHTWEIGHT));
        address.setOption0(MAX_MESSAGE_SIZE, options.getOption(MAX_MESSAGE_SIZE));
        address.setOption0(INACTIVITY_TIMEOUT, options.getOption(INACTIVITY_TIMEOUT));
        address.setOption0(SUPPORTED_PROTOCOLS, options.getOption(SUPPORTED_PROTOCOLS));
        address.setOption0(REQUIRED_PROTOCOLS, options.getOption(REQUIRED_PROTOCOLS));
    }
}
