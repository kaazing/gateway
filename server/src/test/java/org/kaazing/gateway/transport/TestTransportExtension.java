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

import javax.annotation.Resource;

import org.kaazing.gateway.resource.address.ResourceAddressFactory;
import org.kaazing.gateway.transport.TransportFactory;

public class TestTransportExtension {

    private TransportFactory transportFactory;
    private ResourceAddressFactory resourceAddressFactory;

    public ResourceAddressFactory getResourceAddressFactory() {
        return resourceAddressFactory;
    }

    @Resource(name = "resourceAddressFactory")
    public void setResourceAddressFactory(ResourceAddressFactory resourceAddressFactory) {
        this.resourceAddressFactory = resourceAddressFactory;
    }

    public TransportFactory getTransportFactory() {
        return transportFactory;
    }

    @Resource(name = "transportFactory")
    public void setTransportFactory(TransportFactory bridgeServiceFactory) {
        this.transportFactory = bridgeServiceFactory;
    }

}