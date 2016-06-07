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

import static org.kaazing.gateway.resource.address.ResourceFactories.changeSchemeOnly;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.resource.address.ResourceAddressFactorySpi;
import org.kaazing.gateway.resource.address.ResourceFactory;

public class WssResourceAddressFactorySpi extends WsResourceAddressFactorySpi {

    static final String SCHEME_NAME = "wss";
    private static final int SCHEME_PORT = 443;
    private static final ResourceFactory TRANSPORT_FACTORY = changeSchemeOnly("https");

    // go backwards so we can set alternate addresses correctly
    private static final List<String> WSS_ALTERNATE_SCHEMES =
        Arrays.asList("wsx-draft+ssl", "ws-draft+ssl", "wsx+ssl", "wse+ssl");

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
    protected ResourceFactory getTransportFactory() {
        return TRANSPORT_FACTORY;
    }

    @Override
    public void setResourceAddressFactory(ResourceAddressFactory addressFactory) {
        super.setResourceAddressFactory(addressFactory);

        Map<String, ResourceAddressFactorySpi<?>> alternateAddressFactories =
                addressFactory.getAlternateAddressFactories(getSchemeName());
        alternateAddressFactories = new HashMap<>(alternateAddressFactories);
        List<ResourceFactory> alternateResourceFactories = new ArrayList<>();
        // Create an ordered list of resource factories for the available alternate address factories
        for(String scheme : WSS_ALTERNATE_SCHEMES) {
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

    @Override
    protected List<ResourceFactory> getAlternateResourceFactories() {
        return this.alternateResourceFactories;
    }

}
