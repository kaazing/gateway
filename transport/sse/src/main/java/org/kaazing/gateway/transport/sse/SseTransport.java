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
package org.kaazing.gateway.transport.sse;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.kaazing.gateway.resource.address.Protocol;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.transport.BridgeAcceptor;
import org.kaazing.gateway.transport.BridgeConnector;
import org.kaazing.gateway.transport.Transport;

final class SseTransport extends Transport {

    private static final Map<String, Protocol> SSE_PROTOCOLS;
    static {
        Map<String, Protocol> map = new HashMap<>();
        map.put("sse", SseProtocol.SSE);
        map.put("sse+ssl", SseProtocol.SSE_SSL);
        SSE_PROTOCOLS = Collections.unmodifiableMap(map);
    }

    private final BridgeAcceptor acceptor;
    private final BridgeConnector connector;

    SseTransport() {
        acceptor = new SseAcceptor();
        connector = new SseConnector();
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
        return SSE_PROTOCOLS;
    }

}
