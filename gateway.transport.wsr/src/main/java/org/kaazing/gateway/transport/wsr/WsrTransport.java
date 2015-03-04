/**
 * Copyright (c) 2007-2014 Kaazing Corporation. All rights reserved.
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.kaazing.gateway.transport.wsr;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.kaazing.gateway.resource.address.Protocol;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.transport.BridgeAcceptor;
import org.kaazing.gateway.transport.BridgeConnector;
import org.kaazing.gateway.transport.Transport;
import org.kaazing.gateway.transport.dispatch.ProtocolDispatcher;

final class WsrTransport extends Transport {

    private static final Map<String, Protocol> WSR_PROTOCOLS;
    static {
        Map<String, Protocol> map = new HashMap<String, Protocol>();
        map.put("wsr", WsrProtocol.WSR);
        map.put("wsr+ssl", WsrProtocol.WSR_SSL);
        map.put("rtmp", RtmpProtocol.RTMP);
        map.put("rtmps", RtmpProtocol.RTMPS);
        WSR_PROTOCOLS = Collections.unmodifiableMap(map);
    }

    private static final Map<String, ProtocolDispatcher> RTMP_PROTOCOL_DISPATCHERS;

    static {
        RtmpProtocolDispatcher dispatcher = new RtmpProtocolDispatcher();
        Map<String, ProtocolDispatcher> map = new HashMap<String, ProtocolDispatcher>();
        map.put(dispatcher.getProtocolName(), dispatcher);
        RTMP_PROTOCOL_DISPATCHERS = Collections.unmodifiableMap(map);
    }

    private final BridgeAcceptor acceptor;
    private final BridgeConnector connector;

    WsrTransport() {
        acceptor = new WsrAcceptor();
        connector = new WsrConnector();
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
        return WSR_PROTOCOLS;
    }

    @Override
    public Map<String, ProtocolDispatcher> getProtocolDispatchers() {
        return RTMP_PROTOCOL_DISPATCHERS;
    }
}
