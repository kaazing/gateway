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

import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeMap;

import org.kaazing.gateway.resource.address.ws.WsResourceAddress;
import org.kaazing.gateway.transport.http.HttpAcceptSession;
import org.kaazing.gateway.transport.ws.extension.WebSocketExtensionFactorySpi.ExtensionOrderCategory;

public final class WebSocketExtensionFactory {

    private final Map<String, WebSocketExtensionFactorySpi> factoriesRO;
    private final List<String> extensionNames;
    private final Map<Integer, Set<ExtensionHeader>> extensionHeadersByCategory;

    private WebSocketExtensionFactory(Map<String, WebSocketExtensionFactorySpi> factoriesRO) {
        this.factoriesRO = factoriesRO;
        this.extensionNames = new ArrayList<>();
        Map<Integer, Set<ExtensionHeader>>extensionHeadersByCategory = new TreeMap<>();
        for(WebSocketExtensionFactorySpi extension: factoriesRO.values()){
            // Add to extension names
            this.extensionNames.add(extension.getExtensionName());
            // Order the values
            List<ExtensionOrderCategory> extensionOrderValues =
                    Arrays.asList(WebSocketExtensionFactorySpi.ExtensionOrderCategory.values());
            Integer key = extensionOrderValues.indexOf(extension.getOrderCategory());
            Set<ExtensionHeader> value = extensionHeadersByCategory.get(key);
            if(value == null){
                value = new HashSet<>();
                extensionHeadersByCategory.put(key, value);
            }
            String extensionToken = extension.getExtensionName();
            value.add(new ExtensionHeaderBuilder(extensionToken).toExtensionHeader());
        }
        this.extensionHeadersByCategory = unmodifiableMap(extensionHeadersByCategory);
    }

    /**
     * Returns the names of all the supported/discovered extensions
     *
     * @return Collection of extension names
     */
    public List<String> getExtensionNames() {
        return extensionNames;
    }

    /**
     * This method is called for each extension requested by the client during the WebSocket handshake. 
     * @param requestedExtension  Extension token and parameters from the WebSocket handshake HTTP request
     *                            corresponding to one of the extensions in a WebSocket extensions
     *                            HTTP header (which is a comma-separated list of extensions)
     * @param address    WebSocket resource address on which the handshake is taking place
     * @return           WebSocketExtensionSpi instance representing the active, negotiated extension
     *                   or null if the extension is not available
     * @throws ProtocolException  If the extension cannot be negotiated. Throwing this exception will result
     *                      in failing the WebSocket connection.
     */
    public WebSocketExtension negotiate(ExtensionHeader extension, WsResourceAddress address) throws ProtocolException {
        String extensionName = extension.getExtensionToken();

        WebSocketExtensionFactorySpi factory = factoriesRO.get(extensionName);
        if (factory == null) {
            return null;
        }

        return factory.negotiate(extension, address);
    }

    /**
     * 
     * @param address  WsResourceAddress for the WebSocket connection for which extensions are being negotiated
     * @param session  HttpSession upon which the WebSocket upgrade handshake is occurring
     * @param headerName Name of the HTTP header conveying extensions (e.g. "sec-websocket-extensions")
     * @param clientRequestedExtensions List of extension header values (one per requested extension, parsing of 
     *                                  any comma-separated list is already done by the HTTP transport layer)
     * @return object representing the list of negotiated  WebSocketExtensionSpi instances in the order they should appear
     *                negotiated in (farthest from network to closest)
     * @throws ProtocolException
     */
    public ActiveExtensions negotiateWebSocketExtensions(WsResourceAddress address, HttpAcceptSession session,
        String headerName, List<String> clientRequestedExtensions) throws ProtocolException {

        ActiveExtensions result = ActiveExtensions.EMPTY;
        if (clientRequestedExtensions != null) {
            List<ExtensionHeader> requestedExtensions = toWsExtensions(clientRequestedExtensions);

            // get the acceptedExtensions
            LinkedList<WebSocketExtension> acceptedExtensions = new LinkedList<>();

            // Orders the extensions based on SPI preferences, and then order that they came in
            for(Set<ExtensionHeader> extensionHeaders: extensionHeadersByCategory.values()){
                for (ExtensionHeader candidate : requestedExtensions) {
                    if(extensionHeaders.contains(candidate)){
                        WebSocketExtensionFactorySpi extension = factoriesRO.get(candidate.getExtensionToken());
                        WebSocketExtension acceptedExtension = extension.negotiate(candidate, address);
                        // negotiated can be null if the extension doesn't want to be active
                        if (acceptedExtension != null) {
                            acceptedExtensions.add(acceptedExtension);
                        }
                    }
                }
            }
            result = new ActiveExtensions(acceptedExtensions);
        }
        return result;
    }

    private static List<ExtensionHeader> toWsExtensions(Collection<String> extensionTokens) {
        if (extensionTokens == null) {
            throw new NullPointerException("extensionTokens");
        }

        if (extensionTokens.size() == 0) {
            return new ArrayList<>();
        }

        List<ExtensionHeader> exts = new ArrayList<>(extensionTokens.size());
        for (String extensionToken : extensionTokens) {
            if (extensionToken != null) {
                exts.add(new ExtensionHeaderBuilder(extensionToken).toExtensionHeader());
            }
        }

        return exts;
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
