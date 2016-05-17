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


import org.kaazing.gateway.resource.address.Protocol;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.transport.dispatch.ProtocolDispatcher;

import java.net.Proxy;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public abstract class Transport {

    public abstract BridgeAcceptor getAcceptor();
    public abstract BridgeConnector getConnector();

    public abstract BridgeAcceptor getAcceptor(ResourceAddress address);
    public abstract BridgeConnector getConnector(ResourceAddress address);

    /**
     * Gets a list of transport extensions into which resources should be injected
     * @return  Available transport extensions or empty set if there are none
     */
    public Collection<?> getExtensions() {
        return Collections.EMPTY_SET;
    }

    /**
     * Return a map of proxy handlers for this transport
     *
     * @return map of proxy handlers, otherwise empty map
     */
    public Map<Proxy.Type, ProxyHandler> getProxyHandlers() {
        return Collections.emptyMap();
    }

    /**
     * Return a map (scheme -> Protocol) for this transport
     *
     * @return map of protocols, otherwise empty map
     */
    public Map<String, Protocol> getProtocols() {
        return Collections.emptyMap();
    }

    /**
     * Return a map (protocol name -> ProtocolDispatcher) for this transport
     *
     * @return map of protocol dispatchers, otherwise empty map
     */
    public Map<String, ProtocolDispatcher> getProtocolDispatchers() {
        return Collections.emptyMap();
    }

}
