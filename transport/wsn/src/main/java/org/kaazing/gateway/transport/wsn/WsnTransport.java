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
package org.kaazing.gateway.transport.wsn;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.kaazing.gateway.resource.address.Protocol;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.transport.BridgeAcceptor;
import org.kaazing.gateway.transport.BridgeConnector;
import org.kaazing.gateway.transport.Transport;

final class WsnTransport extends Transport {

    private static final Map<String, Protocol> WSN_PROTOCOLS;
    static {
        Map<String, Protocol> map = new HashMap<>();
        map.put("wsn", WsnProtocol.WSN);
        map.put("wsn+ssl", WsnProtocol.WSN_SSL);
        map.put("wsx", WsnProtocol.WSN);
        map.put("wsx+ssl", WsnProtocol.WSN_SSL);
        map.put("ws-draft", WsDraftProtocol.WS_DRAFT);
        map.put("ws-draft+ssl", WsDraftProtocol.WS_DRAFT_SSL);
        map.put("wsx-draft", WsxDraftProtocol.WSX_DRAFT);
        map.put("wsx-draft+ssl", WsxDraftProtocol.WSX_DRAFT_SSL);
        map.put("ws-draft-75", WsDraft75Protocol.WS_DRAFT_75);
        map.put("ws-draft-75+ssl", WsDraft75Protocol.WS_DRAFT_75_SSL);
        WSN_PROTOCOLS = Collections.unmodifiableMap(map);
    }

    private final WsnAcceptor acceptor;
    private final WsnConnector connector;

    WsnTransport(Properties configuration) {
        acceptor = new WsnAcceptor();
        acceptor.setConfiguration(configuration);
        connector = new WsnConnector();
        connector.setConfiguration(configuration);
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
        return WSN_PROTOCOLS;
    }


}
