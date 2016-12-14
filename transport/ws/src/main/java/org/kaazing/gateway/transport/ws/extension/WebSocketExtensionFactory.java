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
package org.kaazing.gateway.transport.ws.extension;

import static java.util.Collections.unmodifiableMap;
import static java.util.ServiceLoader.load;

import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeMap;

import org.kaazing.gateway.resource.address.ws.WsResourceAddress;
import org.kaazing.gateway.transport.ws.extension.WebSocketExtensionFactorySpi.ExtensionOrderCategory;

public final class WebSocketExtensionFactory {

    private final Map<String, WebSocketExtensionFactorySpi> factoriesRO;
    private final Map<Integer, Set<ExtensionHeader>> extensionHeadersByCategory;

    private WebSocketExtensionFactory(Map<String, WebSocketExtensionFactorySpi> factoriesRO) {
        this.factoriesRO = factoriesRO;
        Map<Integer, Set<ExtensionHeader>>extensionHeadersByCategory = new TreeMap<>();
        for(WebSocketExtensionFactorySpi extension: factoriesRO.values()){
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
            value.add(new ExtensionHeaderBuilder(extensionToken).done());
        }
        this.extensionHeadersByCategory = unmodifiableMap(extensionHeadersByCategory);
    }

    public Collection<WebSocketExtensionFactorySpi> availableExtensions() {
        return factoriesRO.values();
    }

    /**
     *
     * @param address  WsResourceAddress for the WebSocket connection for which extensions are being negotiated
     * @param clientRequestedExtensions List of extension header values (one per requested extension, parsing of
     *                                  any comma-separated list is already done by the HTTP transport layer)
     * @param extensionHelper TODO
     * @return list of negotiated WebSocketExtensionSpi instances in the order they should appear
     *         negotiated in (farthest from network to closest)
     * @throws ProtocolException
     */
    public List<WebSocketExtension> negotiateWebSocketExtensions(WsResourceAddress address,
                                                                 List<String> clientRequestedExtensions,
                                                                 ExtensionHelper extensionHelper)
            throws ProtocolException {

        List<WebSocketExtension> result = Collections.emptyList();
        if (clientRequestedExtensions != null) {
            List<ExtensionHeader> requestedExtensions = toWsExtensions(clientRequestedExtensions);

            // get the acceptedExtensions
            LinkedList<WebSocketExtension> acceptedExtensions = new LinkedList<>();

            // Orders the extensions based on SPI preferences, and then order that they came in
            for(Set<ExtensionHeader> extensionHeaders: extensionHeadersByCategory.values()){
                for (ExtensionHeader candidate : requestedExtensions) {
                    if(extensionHeaders.contains(candidate)){
                        WebSocketExtensionFactorySpi extension = factoriesRO.get(candidate.getExtensionToken());
                        WebSocketExtension acceptedExtension = extension.negotiate(candidate, extensionHelper, address);
                        // negotiated can be null if the extension doesn't want to be active
                        if (acceptedExtension != null) {
                            acceptedExtensions.add(acceptedExtension);
                        }
                    }
                }
            }
            result = Collections.unmodifiableList(acceptedExtensions);
        }
        return result;
    }

    /**
     * Returns a list of extensions that want to be part of websocket request
     *
     * @param address WsResourceAddress to which a websocket connection is being attempted
     * @param extensionHelper extension helper
     * @return list of extensions that want to be part of the request
     */
    public List<WebSocketExtension> offerWebSocketExtensions(WsResourceAddress address,
                                                             ExtensionHelper extensionHelper) {
        List<WebSocketExtension> list = new ArrayList<>();

        for(WebSocketExtensionFactorySpi factory : factoriesRO.values()) {
            WebSocketExtension extension = factory.offer(extensionHelper, address);
            if (extension != null) {
                list.add(extension);
            }
        }
        return list;
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
                exts.add(new ExtensionHeaderBuilder(extensionToken).done());
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
        Map<String, WebSocketExtensionFactorySpi> factories = new HashMap<>();
        for (WebSocketExtensionFactorySpi service : services) {
            String extensionName = service.getExtensionName();
            factories.put(extensionName, service);
        }
        return new WebSocketExtensionFactory(unmodifiableMap(factories));
    }

}
