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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import org.kaazing.gateway.resource.address.ws.WsResourceAddress;
import org.kaazing.gateway.transport.http.HttpAcceptSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WebSocketExtensionFactory {

    private final Map<String, WebSocketExtensionFactorySpi> factoriesRO;
    private final List<ExtensionHeader> supportedExtensionHeaders;
    // note: extension names are in the order they would like to be on the pipeline in.
    private final List<String> extensionNames;
    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketExtensionFactory.class);

    private WebSocketExtensionFactory(Map<String, WebSocketExtensionFactorySpi> factoriesRO) {
        this.factoriesRO = factoriesRO;
        // properly order the extension names
        LinkedList<WebSocketExtensionFactorySpi> orderedExtensions = new LinkedList<>();
        for (WebSocketExtensionFactorySpi factory : factoriesRO.values()) {
            addExtensionAtBestLocation(factory, orderedExtensions);
        }
        // WARN if extension ordering can not be made ideal by matching everyones requirements
        if (!allExtensionOrderingRequirementsAreMet(orderedExtensions) && LOGGER.isWarnEnabled()) {
            StringBuilder message =
                    new StringBuilder(
                            "Could not find an extension ordering to satisfy the requirements of all WebSocketExtensionSpi,")
                            .append(" the order is: ");
            for (WebSocketExtensionFactorySpi extension : orderedExtensions) {
                message.append(extension.getExtensionName()).append(" ");
            }
            LOGGER.warn(message.toString());

        }
        extensionNames = new ArrayList<>();
        for(WebSocketExtensionFactorySpi extension: orderedExtensions){
            extensionNames.add(extension.getExtensionName());
        }
        this.supportedExtensionHeaders = toWsExtensions(this.getExtensionNames());
    }

    static void addExtensionAtBestLocation(WebSocketExtensionFactorySpi factory,
        LinkedList<WebSocketExtensionFactorySpi> orderedExtensions) {
        int[] rankOfPotentialPositions = new int[orderedExtensions.size() + 1];
        int i = 0;
        // Brute force get the score for all the positions
        do {
            orderedExtensions.add(i, factory);
            rankOfPotentialPositions[i] = scoreList(orderedExtensions);
            orderedExtensions.remove(i);
            i++;
        } while (i <= orderedExtensions.size());
        int bestPos = 0;
        int bestScore = Integer.MIN_VALUE;
        // get the best position favoring the last entry in list in case of tie
        for (i = 0; i < rankOfPotentialPositions.length; i++) {
            if (rankOfPotentialPositions[i] >= bestScore) {
                bestPos = i;
                bestScore = rankOfPotentialPositions[i];
            }
        }
        orderedExtensions.add(bestPos, factory);
    }

    private static int scoreList(List<WebSocketExtensionFactorySpi> listOfExtensionFactories) {
        int score = 0;
        final int numOfExtensionFactories = listOfExtensionFactories.size();
        for (int pos = 0; pos < numOfExtensionFactories; pos++) {
            WebSocketExtensionFactorySpi extensionFactory = listOfExtensionFactories.get(pos);
            final String[] orderBefore = extensionFactory.orderBefore();
            if (orderBefore != null) {
                for (String ownRequirement : orderBefore) {
                    final int nextPos = pos + 1;
                    score--; // assume not found, but then add 2 if found
                    if (ownRequirement.equals("$") && nextPos == numOfExtensionFactories) {
                        score += 2;
                    }else if(ownRequirement.equals("$")){
                        final String[] nextExtensionsOrderBefore = listOfExtensionFactories.get(nextPos).orderBefore();
                        if(nextExtensionsOrderBefore != null && Arrays.asList(nextExtensionsOrderBefore).contains("$")){
                            score+=2;
                        }
                    } else {
                        for(int i = nextPos; i < numOfExtensionFactories; i++){
                            if(listOfExtensionFactories.get(i).getExtensionName().equals(ownRequirement)){
                                score+=2;
                                break;
                            }
                        }
                    }
                }
            }
        }
        return score;
    }

    private boolean allExtensionOrderingRequirementsAreMet(LinkedList<WebSocketExtensionFactorySpi> orderedExtensions) {
        int maxScore = 0;
        for (WebSocketExtensionFactorySpi extension : orderedExtensions) {
            if (extension.orderBefore() != null) {
                // note that the current scoring allows 2 extensions to have "$" and still be considered properly ordered,
                // perhaps we do want to report that, perhaps we don't
                maxScore += extension.orderBefore().length;
            }
        }
        return (scoreList(orderedExtensions) == maxScore);
    }

    /**
     * Returns the names of all the supported/discovered extensions in the order they would like to be negotiated in
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
    public WebSocketExtensionSpi negotiate(ExtensionHeader extension, WsResourceAddress address) throws ProtocolException {
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
     * @return object representing the list of negotiated  WebSocketExtensionSpi instances
     * @throws ProtocolException
     */
    public ActiveWebSocketExtensions negotiateWebSocketExtensions(WsResourceAddress address, HttpAcceptSession session,
        String headerName, List<String> clientRequestedExtensions) throws ProtocolException {

        ActiveWebSocketExtensions result = ActiveWebSocketExtensions.EMPTY;
        if (clientRequestedExtensions != null) {
            List<ExtensionHeader> requestedExtensions = toWsExtensions(clientRequestedExtensions);

            // Since the client may have provided parameters in their
            // extensions, we retain the common requested extensions, not
            // the common supported extensions as was previously done.
            requestedExtensions.retainAll(supportedExtensionHeaders);

            // get the acceptedExtensions
            LinkedList<WebSocketExtensionSpi> acceptedExtensions = new LinkedList<>();
            for (ExtensionHeader candidate : requestedExtensions) {
                WebSocketExtensionFactorySpi extension = factoriesRO.get(candidate.getExtensionToken());
                WebSocketExtensionSpi acceptedExtension = extension.negotiate(candidate, address);
                // negotiated can be null if the extension doesn't want to be active
                if (acceptedExtension != null) {
                    acceptedExtensions.add(acceptedExtension);

                }
            }

            // Orders the extensions based on SPI preferences

            result = new ActiveWebSocketExtensions(acceptedExtensions);
        }
        // if (clientRequestedExtensions != null) {
        // List<ExtensionHeader> requestedExtensions = toWsExtensions(clientRequestedExtensions);
        //
        // // Since the client may have provided parameters in their
        // // extensions, we retain the common requested extensions, not
        // // the common supported extensions as was previously done.
        // requestedExtensions.retainAll(supportedExtensionHeaders);
        //
        // // get the acceptedExtensions
        // LinkedList<WebSocketExtensionSpi> acceptedExtensions = new LinkedList<>();
        // for (ExtensionHeader candidate : requestedExtensions) {
        // WebSocketExtensionFactorySpi extension = factoriesRO.get(candidate.getExtensionToken());
        // WebSocketExtensionSpi acceptedExtension = extension.negotiate(candidate, address);
        // // negotiated can be null if the extension doesn't want to be active
        // if (acceptedExtension != null) {
        // acceptedExtensions.add(acceptedExtension);
        //
        // }
        // }
        //
        // //Orders the extensions based on SPI preferences
        //
        //
        // result = new ActiveWebSocketExtensions(acceptedExtensions);
        // }

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
