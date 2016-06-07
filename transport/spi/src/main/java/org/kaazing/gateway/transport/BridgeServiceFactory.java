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

import org.kaazing.gateway.resource.address.ResourceAddress;

import java.util.Collections;

public class BridgeServiceFactory {

    private final TransportFactory transportFactory;

    public BridgeServiceFactory() {
        this(TransportFactory.newTransportFactory(Collections.emptyMap()));
    }

    public BridgeServiceFactory(TransportFactory transportFactory) {
        this.transportFactory = transportFactory;
    }

    public BridgeAcceptor newBridgeAcceptor(ResourceAddress address) {
        return transportFactory.getAcceptor(address);
    }

    public BridgeConnector newBridgeConnector(ResourceAddress address) {
        return transportFactory.getConnector(address);
    }

    public TransportFactory getTransportFactory() {
        return transportFactory;
    }

}
