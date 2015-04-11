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

import static org.kaazing.gateway.transport.ws.WsMessage.Kind.TEXT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.ws.WsResourceAddress;
import org.kaazing.gateway.transport.ws.WsMessage;
import org.kaazing.gateway.transport.ws.WsMessage.Kind;
import org.kaazing.gateway.transport.ws.bridge.extensions.WsExtensions;
import org.kaazing.gateway.transport.ws.bridge.extensions.idletimeout.IdleTimeoutExtension;
import org.kaazing.gateway.transport.ws.bridge.extensions.pingpong.PingPongExtension;
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
public class WsExtensionBuilder implements WsExtension {
    // TODO: Allocate control bytes for each active extension? (add setControlBytes method to WsExtension)
    //private static int nextControlBytes = 0;
    
    private String extensionToken;
    private Map<String, WsExtensionParameter> parametersByName = new LinkedHashMap<>();

    public WsExtensionBuilder(String extensionToken) {
        if (extensionToken == null) {
            throw new NullPointerException("extensionToken");
        }

        // Look for any parameters in the given token
        int idx = extensionToken.indexOf(';');
        if (idx == -1) {
            this.extensionToken = extensionToken;

        } else {
            String[] elts = extensionToken.split(";");
            this.extensionToken = elts[0].trim();

            for (int i = 1; i < elts.length; i++) {
                String key = null;
                String value = null;

                idx = elts[i].indexOf('=');
                if (idx == -1) {
                    key = elts[i].trim();

                } else {
                    key = elts[i].substring(0, idx).trim();
                    value = elts[i].substring(idx+1).trim();
                }
                
                appendParameter(key, value);
            }
        }
    }

    public WsExtensionBuilder(WsExtension extension) {
        this.extensionToken = extension.getExtensionToken();
        List<WsExtensionParameter> parameters = extension.getParameters();
        for ( WsExtensionParameter p: parameters) {
            this.parametersByName.put(p.getName(), p);
        }
    }

    public String getExtensionToken() {
        return extensionToken;
    }

    public List<WsExtensionParameter> getParameters() {
        return Collections.unmodifiableList(new ArrayList<>(parametersByName.values()));
    }

    @Override
    public boolean hasParameters() {
        return !parametersByName.isEmpty();
    }

    @Override
    public WsExtensionValidation checkValidity() {
        // By default, all extensions are valid
        return new WsExtensionValidation();
    }

    public WsExtension toWsExtension() {
        return this;
    }

    public WsExtensionBuilder setExtensionToken(String token) {
        this.extensionToken = token;
        return this;
    }

    public WsExtensionBuilder append(WsExtensionParameter parameter) {
        if ( !parametersByName.containsKey(parameter.getName()) ) {
            parametersByName.put(parameter.getName(), parameter);
        }
        return this;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder(extensionToken);
        for (WsExtensionParameter wsExtensionParameter: parametersByName.values()) {
            b.append(';').append(' ').append(wsExtensionParameter);
        }
        return b.toString();
    }

    // Default equality is by extension token, ignoring parameters.

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof WsExtensionBuilder)) return false;

        WsExtensionBuilder that = (WsExtensionBuilder) o;

        return !(extensionToken != null ? !extensionToken.equals(that.extensionToken) : that.extensionToken != null);

    }

    @Override
    public int hashCode() {
        return extensionToken != null ? extensionToken.hashCode() : 0;
    }

    public void appendParameter(String parameterContents) {
       append(new WsExtensionParameterBuilder(parameterContents));
    }

    public void appendParameter(String parameterName, String parameterValue) {
        append(new WsExtensionParameterBuilder(parameterName, parameterValue));
    }

    public static WsExtension create(ResourceAddress address, WsExtension extension) {
        WsExtension ext;
    
        if (extension.getExtensionToken().equals(WsExtensions.IDLE_TIMEOUT)) {
            IdleTimeoutExtension idleTimeoutExt = new IdleTimeoutExtension(extension,
                    address.getOption(WsResourceAddress.INACTIVITY_TIMEOUT));
            ext = idleTimeoutExt;
    
        } else if (extension.getExtensionToken().equals(WsExtensions.PING_PONG)) {
            PingPongExtension typedExtension = new PingPongExtension(extension);
            ext = typedExtension;
        
        } else {
            ext = extension;
        }
    
        return ext;
    }

    @Override
    public boolean canDecode(EndpointKind endpointKind, Kind messageKind) {
        return false;
    }

    @Override
    public boolean canEncode(EndpointKind endpointKind, Kind messageKind) {
        return false;
    }

    @Override
    public WsMessage decode(IoBufferEx payload) {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] encode(WsMessage message) {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] getControlBytes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Kind getEncodedKind(WsMessage message) {
        return TEXT;
    }
    
    @Override
    public String getOrdering() {
        return getExtensionToken();
    }

    @Override
    public void handleMessage(IoSessionEx session, WsMessage message) {
    }

    @Override
    public void removeBridgeFilters(IoFilterChain filterChain) {
    }

    @Override
    public void updateBridgeFilters(IoFilterChain filterChain) {
    }
    
}
