/*
 * Copyright 2014, Kaazing Corporation. All rights reserved.
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

package org.kaazing.gateway.transport.ws.extension;

import static java.util.Collections.unmodifiableMap;
import static java.util.ServiceLoader.load;

import java.io.IOException;
import java.net.ProtocolException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ws.WsResourceAddress;
import org.kaazing.gateway.transport.http.HttpAcceptSession;


public final class WebSocketExtensionFactory {

    private final Map<String, WebSocketExtensionFactorySpi> factoriesRO;

    private WebSocketExtensionFactory(Map<String, WebSocketExtensionFactorySpi> factoriesRO) {
        this.factoriesRO = factoriesRO;
    }

    /**
     * Returns the names of all the supported/discovered extensions.
     *
     * @return Collection of extension names
     */
    public Collection<String> getExtensionNames() {
        return factoriesRO.keySet();
    }

    /**
     * This method is called for each extension requested by the client during the WebSocket handshake. 
     * @param requestedExtension  Extension token and parameters from the WebSocket handshake HTTP request
     *                            corresponding to one of the extensions in a WebSocket extensions
     *                            HTTP header (which is a comma-separated list of extensions)
     * @param address    WebSocket resource address on which the handshake is taking place
     * @return           WebSocketExtensionSpi instance representing the active, negotiated extension
     *                   or null if the extension is not available
     * @throws IOException  If the extension cannot be negotiated. Throwing this exception will result
     *                      in failing the WebSocket connection.
     */
    public WebSocketExtensionSpi negotiate(ExtensionHeader extension, WsResourceAddress address)
            throws IOException {
        String extensionName = extension.getExtensionToken();

        WebSocketExtensionFactorySpi factory = factoriesRO.get(extensionName);
        if (factory == null) {
            return null;
        }

        return factory.negotiate(extension, address);
    }
    
    public ActiveExtensions negotiateWebSocketExtensions(ResourceAddress address,
                                                         HttpAcceptSession session,
                                                         String headerName,
                                                         List<String> clientRequestedExtensions,
                                                         List<String> serverWsExtensions) throws ProtocolException {
        // TODO: implement this method, based loosely on WsExtensionUtils.negotiateWebSocketExtensions. Note that 
        // as commented in that method we do not need to have any notion of mandatory extensions the client must negotiate.
        throw new UnsupportedOperationException ();
        //return ActiveExtensions.EMPTY;
    }

    /**
     * Creates a new instance of WebSocketExtensionFactory. It uses the default {@link ClassLoader} to load
     * {@link WebSocketExtensionFactorySpi} objects that are registered using META-INF/services.
     *
     * @return WebSocketExtensionFactory
     */
    public static WebSocketExtensionFactory newInstance() {
        ServiceLoader<WebSocketExtensionFactorySpi> services = load(WebSocketExtensionFactorySpi.class);
        return newInstance(services);
    }

    /**
     * Creates a new instance of WebSocketExtensionFactory. It uses the specified {@link ClassLoader} to load
     * {@link WebSocketExtensionFactorySpi} objects that are registered using META-INF/services.
     *
     * @return WebSocketExtensionFactory
     */
    public static WebSocketExtensionFactory newInstance(ClassLoader cl) {
        ServiceLoader<WebSocketExtensionFactorySpi> services = load(WebSocketExtensionFactorySpi.class, cl);
        return newInstance(services);
    }


    private static WebSocketExtensionFactory newInstance(ServiceLoader<WebSocketExtensionFactorySpi> services) {
        Map<String, WebSocketExtensionFactorySpi> factories = new HashMap<String, WebSocketExtensionFactorySpi>();
        for (WebSocketExtensionFactorySpi service : services) {
            String extensionName = service.getExtensionName();
            factories.put(extensionName, service);
        }
        return new WebSocketExtensionFactory(unmodifiableMap(factories));
    }
}
