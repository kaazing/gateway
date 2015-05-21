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

package org.kaazing.gateway.transport.ws.extension;

import static org.kaazing.gateway.transport.ws.extension.ExtensionHeader.EndpointKind.SERVER;

import java.util.ArrayList;
import java.util.List;

import org.apache.mina.core.session.IoSession;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.transport.http.HttpAcceptSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated // This will be replaced by WebSocketExtensionFactory
public class WsExtensionUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(WsExtensionUtils.class);
    
    public static void addWsExtensionFilters(IoSession session) {
        
    }
    
    public static List<byte[]> getEscapeSequences(List<ExtensionHeader> extensions) {
        List<byte[]> result = new ArrayList<>();
        if ( extensions != null ) {
            for ( ExtensionHeader extension: extensions ) {
                result.add(extension.getControlBytes());
            }
        }
        return result;
    }

    public static WsExtensionNegotiationResult negotiateWebSocketExtensions(ResourceAddress address,
                                                                            HttpAcceptSession session,
                                                                            String headerName,
                                                                            List<String> clientRequestedExtensions,
                                                                            List<String> serverWsExtensions) {
        if (clientRequestedExtensions != null) {
            List<ExtensionHeader> wsSupportedExtensions = toWsExtensions(serverWsExtensions);
            List<ExtensionHeader> wsRequestedExtensions = toWsExtensions(clientRequestedExtensions);
    
            // Since the client may have provided parameters in their
            // extensions, we retain the common requested extensions, not
            // the common supported extensions as was previously done.
            wsRequestedExtensions.retainAll(wsSupportedExtensions);
    
            if (wsRequestedExtensions.isEmpty()) {
                // It is possible that this service requires that at least ONE
                // shared extension be found; this is indicated by NOT having
                // null in the serverWsExtensions list.  (How's that for a
                // subtle semantic?)
// TODO: remove this "subtle semantic" in CE 5.0 since it is in fact never used in the 4.0 code base. It was originally added as part of the APNS work.
                //
                // If this happens, then we need to tell the caller to close
                // the session.
                if (!serverWsExtensions.contains(null)) {
                    return new WsExtensionNegotiationResult(WsExtensionNegotiationResult.Status.FAILURE, "No shared but required WebSocket Extensions found");
                }
    
            } else {
                List<ExtensionHeader> wsAcceptedExtensions = new ArrayList<>(wsRequestedExtensions.size());
    
                for (ExtensionHeader candidate : wsRequestedExtensions) {
                    ExtensionHeader accepted = ExtensionHeaderBuilder.create(address, candidate);
                    WsExtensionValidation validation = accepted.checkValidity();
                    if (validation.isValid()) {
                        wsAcceptedExtensions.add(accepted);
    
                    } else {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug(String.format("Unable to accept %s WS extension: %s", candidate.getExtensionToken(), validation.getFailureReason()));
                        }
                    }
                }
    
                //TODO: This is where we can add server-initiated coordinated extension parameters.
    
                for (ExtensionHeader ext : wsAcceptedExtensions) {
                    session.addWriteHeader(headerName, ext.toString());
                }
    
                return new WsExtensionNegotiationResult(wsAcceptedExtensions, SERVER);
            }
        }
    
        return WsExtensionNegotiationResult.OK_EMPTY;
    }


    public static List<ExtensionHeader> toWsExtensions(List<String> extensionTokens) {
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

}

