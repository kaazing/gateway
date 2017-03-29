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
package org.kaazing.gateway.resource.address.tcp;

import java.net.URI;
import java.util.Map;

import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.resource.address.ResourceAddressFactorySpi;
import org.kaazing.gateway.resource.address.ResourceOptions;


public class BridgeResourceAddressFactory extends ResourceAddressFactory {
    ResourceAddressFactory delegate = ResourceAddressFactory.newResourceAddressFactory();

    public BridgeResourceAddressFactory(String transportedURI) {
        super(null, null);
        ((BridgeResourceAddressFactorySpi)delegate.findResourceAddressFactory("bridge")).setTransportedURI(URI.create(transportedURI));
    }

    @Override
    public ResourceAddressFactorySpi<?> findResourceAddressFactory(String schemeName) throws IllegalArgumentException {
        return delegate.findResourceAddressFactory(schemeName);
    }

    @Override
    public ResourceAddress newResourceAddress(String location) {
        return delegate.newResourceAddress(location);
    }

    @Override
    public ResourceAddress newResourceAddress(String location, String nextProtocol) {
        if (location.startsWith("tcp")) {
            location = location.replace("tcp", "bridge");
        }
        return delegate.newResourceAddress(location, nextProtocol);
    }

    @Override
    public ResourceAddress newResourceAddress(String location, ResourceOptions options) {
        return delegate.newResourceAddress(location, options);
    }

    @Override
    public ResourceAddress newResourceAddress(String location, ResourceOptions options, Object qualifier) {
        return delegate.newResourceAddress(location, options, qualifier);
    }

    @Override
    public ResourceAddress newResourceAddress(String location, Map<String, Object> options) {
        return delegate.newResourceAddress(location, options);
    }

    @Override
    public ResourceAddress newResourceAddress(String location, Map<String, Object> options, String nextProtocol) {
        return delegate.newResourceAddress(location, options, nextProtocol);
    }

    @Override
    public ResourceAddress newResourceAddress(ResourceAddress uriAddress, ResourceAddress transportAddress) {
        return delegate.newResourceAddress(uriAddress, transportAddress);
    }

    @Override
    public Map<String, ResourceAddressFactorySpi<?>> getAlternateAddressFactories(String schemeName) {
        return delegate.getAlternateAddressFactories(schemeName);
    }
}
