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

import java.util.List;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.kaazing.gateway.transport.ws.WsMessage;
import org.kaazing.gateway.transport.ws.WsMessage.Kind;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.session.IoSessionEx;

/**
 * <pre>
 *     Sec-WebSocket-Extensions = extension-list
 *       extension-list = 1#extension
 *        extension = extension-token *( ";" extension-param )
 *        extension-token = registered-token
 *        registered-token = token
 *        extension-param = token [ "=" (token | quoted-string) ]
 *            ;When using the quoted-string syntax variant, the value
 *            ;after quoted-string unescaping MUST conform to the
 *            ;'token' ABNF.
 * </pre>
 */
public interface WsExtension {
    enum EndpointKind { CLIENT, SERVER }

    String getExtensionToken();

    List<WsExtensionParameter> getParameters() ;

    boolean hasParameters();
    
    /**
     * Tells if this extension has frames which will need decoding
     * @param endpointKind      client or server
     * @return true if this extension has frames which can flow into the given endpoint kind
     */
    boolean canDecode(EndpointKind endpointKind, Kind messageKind);

    /**
     * Tells if this extension has frames which will need encoding
     * @param endpointKind      client or server
     * @return true if this extension has frames which can flow out from the given endpoint kind
     */
    boolean canEncode(EndpointKind endpointKind, Kind messageKind);

    WsExtensionValidation checkValidity();
    
    /**
     * @param payload  The message binary payload, excluding the extension's control bytes
     * @return
     */
    WsMessage decode(IoBufferEx payload);
    
    /**
     * Get the encoded payload of the message. Caller must not mutate the result.
     * @return  The payload for the encoded form of this message, excluding control bytes
     */
    byte[] encode(WsMessage message);
    
    /**
     * @return The control bytes used to distinguish the extension's control frames  
     *         from other WebSocket frames, or null if the extension has no control frames.
     */
    byte[] getControlBytes();
    
    /**
     * Reports which kind of WebSocket frame should be used to to send the given extension message on the wire
     * @param message   Extension message which is being encoded
     * @return
     */
    Kind getEncodedKind(WsMessage message);
    
    /**
     * Extensions are ordered alphabetically based on the results of this method.
     * @return
     */
    String getOrdering();
    
    void handleMessage(IoSessionEx session, WsMessage message);
    
    /**
     * This method should undo anything done by updateBridgeFilters (e.g. remove an extension specific filter)
     * @param filterChain
     */
    void removeBridgeFilters(IoFilterChain filterChain);
    
    /**
     * This method give the extension to manipulate the filter chain if needed (e.g. add an extension specific filter)
     * @param session  The transport session containing the WebSocket codec (parent of the WebSocket session,
     *                 or grandparent in the extended handshake case)
     */
    void updateBridgeFilters(IoFilterChain filterChain);
    
}
