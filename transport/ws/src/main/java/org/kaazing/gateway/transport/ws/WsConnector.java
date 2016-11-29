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
package org.kaazing.gateway.transport.ws;

import static java.lang.String.format;
import static org.kaazing.gateway.resource.address.ResourceAddress.NEXT_PROTOCOL;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IoSessionInitializer;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ws.WsResourceAddress;
import org.kaazing.gateway.transport.BridgeConnector;
import org.kaazing.gateway.transport.ws.extension.WebSocketExtensionFactory;

public class WsConnector implements BridgeConnector {

    private static final String WSE_PROTOCOL_NAME = "wse/1.0";
    private static final String WS_DRAFT_PROTOCOL_NAME = "ws/draft-7x";
    private static final String WS_NATIVE_PROTOCOL_NAME = "ws/rfc6455";

    private final WebSocketExtensionFactory extensionFactory;
    private BridgeConnector wsnConnector;
    private BridgeConnector wsebConnector;

    private Map<String, BridgeConnector> wsBridgeConnectorMap;


    @Resource(name = "wsn.connector")
    public void setWsnConnector(BridgeConnector wsnConnector) {
        this.wsnConnector = wsnConnector;
    }

    @Resource(name = "wseb.connector")
    public void setWsebConnector(BridgeConnector wsebConnector) {
        this.wsebConnector = wsebConnector;
    }


    public WsConnector(WebSocketExtensionFactory extensionFactory) {
        this.extensionFactory = extensionFactory;
    }

    public WebSocketExtensionFactory getWebSocketExtensionFactory() {
        return extensionFactory;
    }

    @Override
    public ConnectFuture connect(ResourceAddress connectAddress,
                                 IoHandler handler,
                                 IoSessionInitializer<? extends ConnectFuture> initializer) {


        BridgeConnector connector = selectWsConnector((WsResourceAddress) connectAddress);

        return connector.connect(connectAddress, handler, initializer);
    }

    @Override
    public void connectInit(ResourceAddress address) {
        // no-op by default
    }


    @Override
    public void connectDestroy(ResourceAddress address) {
        // no-op by default
    }


    private BridgeConnector selectWsConnector(WsResourceAddress bindAddress) {
        if (wsBridgeConnectorMap == null) {
            initwsBridgeConnectorMap();
        }

        if ( bindAddress.getTransport() == null ) {
            throw new RuntimeException(format("Cannot select a WebSocket acceptor for address '%s'.", bindAddress));
        }

        String nextProtocol = bindAddress.getTransport().getOption(NEXT_PROTOCOL);
        if (nextProtocol == null || nextProtocol.isEmpty()) {
            throw new RuntimeException(format("Cannot find a transport nextProtocol for address '%s'.", bindAddress));
        }

        BridgeConnector connector = wsBridgeConnectorMap.get(nextProtocol);
        if (connector == null) {
            throw new RuntimeException(format("Cannot find a %s transport for address '%s'.", nextProtocol, bindAddress));
        }
        return connector;
    }

    private void initwsBridgeConnectorMap() {
        wsBridgeConnectorMap = new HashMap<>();
        wsBridgeConnectorMap.put(WSE_PROTOCOL_NAME, wsebConnector);
        wsBridgeConnectorMap.put(WS_DRAFT_PROTOCOL_NAME, wsnConnector);
        wsBridgeConnectorMap.put(WS_NATIVE_PROTOCOL_NAME, wsnConnector);
    }

    @Override
    public void dispose() {
    }
}
