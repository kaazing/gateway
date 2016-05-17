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
package org.kaazing.gateway.resource.address.sse;

import static org.kaazing.gateway.resource.address.ResourceFactories.changeSchemeOnly;

import java.util.HashMap;
import java.util.Map;

import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceFactory;
import org.kaazing.gateway.resource.address.ResourceOptions;

public class SseSslResourceAddressFactorySpi extends SseResourceAddressFactorySpi {

    private static final String SCHEME_NAME = "sse+ssl";
    private static final int SCHEME_PORT = 443;
    private static final ResourceFactory TRANSPORT_FACTORY = changeSchemeOnly("https");

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
    protected void setAlternateOption(String location,
                                      ResourceOptions options,
                                      Map<String, Object> optionsByName) {

        // Creating alternate SseResourceAddres with httpxe+ssl tranport
        location = SseHttpxeSslResourceAddressFactorySpi.SSE_HTTPXE_RESOURCE_FACTORY.createURI(location);
        // copying optionsByName to so that the alternate address has its own to work with
        optionsByName = new HashMap<>(optionsByName);
        ResourceAddress alternateAddress = getResourceAddressFactory().newResourceAddress(location, optionsByName);
        options.setOption(ResourceAddress.ALTERNATE, alternateAddress);
    }

}
