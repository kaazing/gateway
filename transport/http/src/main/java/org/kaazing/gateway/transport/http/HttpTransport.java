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
package org.kaazing.gateway.transport.http;

import java.net.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.kaazing.gateway.resource.address.Protocol;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.transport.BridgeAcceptor;
import org.kaazing.gateway.transport.BridgeConnector;
import org.kaazing.gateway.transport.ProxyHandler;
import org.kaazing.gateway.transport.Transport;
import org.kaazing.gateway.transport.dispatch.ProtocolDispatcher;

final class HttpTransport extends Transport {

    private static final Map<Proxy.Type, ProxyHandler> HTTP_PROXY_HANDLERS;

    static {
        Map<Proxy.Type, ProxyHandler> map = new HashMap<>();
        map.put(Proxy.Type.HTTP, new HttpProxyHandler());
        HTTP_PROXY_HANDLERS = Collections.unmodifiableMap(map);
    }

    private static final Map<String, Protocol> HTTP_PROTOCOLS;

    static {
        Map<String, Protocol> map = new HashMap<>();
        map.put("http", HttpProtocol.HTTP);
        map.put("https", HttpProtocol.HTTPS);
        map.put("httpx", HttpProtocol.HTTP);
        map.put("httpx+ssl", HttpProtocol.HTTPS);

        map.put("httpx-draft", HttpProtocol.HTTP);
        map.put("httpx-draft+ssl", HttpProtocol.HTTPS);

        map.put("httpxe", HttpProtocol.HTTP);
        map.put("httpxe+ssl", HttpProtocol.HTTPS);
        HTTP_PROTOCOLS = Collections.unmodifiableMap(map);
    }

    private static final Map<String, ProtocolDispatcher> HTTP_PROTOCOL_DISPATCHERS;

    static {
        HttpProtocolDispatcher dispatcher = new HttpProtocolDispatcher();
        Map<String, ProtocolDispatcher> map = new HashMap<>();
        map.put(dispatcher.getProtocolName(), dispatcher);
        HTTP_PROTOCOL_DISPATCHERS = Collections.unmodifiableMap(map);
    }


    private final BridgeAcceptor acceptor;
    private final BridgeConnector connector;

    HttpTransport() {
        acceptor = new HttpAcceptor();
        connector = new HttpConnector();
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
    public Map<Proxy.Type, ProxyHandler> getProxyHandlers() {
        return HTTP_PROXY_HANDLERS;
    }

    @Override
    public Map<String, Protocol> getProtocols() {
        return HTTP_PROTOCOLS;
    }

    @Override
    public Map<String, ProtocolDispatcher> getProtocolDispatchers() {
        return HTTP_PROTOCOL_DISPATCHERS;
    }

}