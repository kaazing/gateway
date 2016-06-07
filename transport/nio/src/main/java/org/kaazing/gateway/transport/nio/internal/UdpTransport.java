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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.kaazing.gateway.resource.address.Protocol;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.transport.BridgeAcceptor;
import org.kaazing.gateway.transport.BridgeConnector;
import org.kaazing.gateway.transport.Transport;

final class UdpTransport extends Transport {

    private static final Map<String, Protocol> UDP_PROTOCOLS;
    static {
        Map<String, Protocol> map = new HashMap<>();
        map.put("udp", NioProtocol.UDP);
        UDP_PROTOCOLS = Collections.unmodifiableMap(map);
    }

    private final BridgeAcceptor acceptor;
    private final BridgeConnector connector;

    UdpTransport(Properties configuration) {
        acceptor = new NioDatagramAcceptor(configuration);
        connector = new NioDatagramConnector(configuration);
    }
    
    @Override
    public BridgeAcceptor getAcceptor() {
        return acceptor;
    }

    @Override
    public BridgeConnector getConnector() {
        return connector;
    }

    @Override
    public BridgeAcceptor getAcceptor(ResourceAddress address) {
        return acceptor;
    }

    @Override
    public BridgeConnector getConnector(ResourceAddress address) {
        return connector;
    }

    @Override
    public Map<String, Protocol> getProtocols() {
        return UDP_PROTOCOLS;
    }
}
