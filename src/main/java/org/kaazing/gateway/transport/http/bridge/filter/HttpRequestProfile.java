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

package org.kaazing.gateway.transport.http.bridge.filter;

import java.util.EnumSet;

import org.kaazing.gateway.transport.http.WsHandshakeValidator;
import org.kaazing.gateway.transport.http.bridge.HttpRequestMessage;

/**
 * Used to classify requests coming through the HTTP transport filter chain into
 * different profiles so that the correct filters can apply or not apply appropriately.
 */
@Deprecated
// Why? This class should go away when ksessionId / recycle authorization mode
// The concept of profiling requests at the HTTP layer was only introduced when
// we needed to distinguish these things for security.  It was a layer violation.
// It should only be used by KSessionId code now, and should be remove along with that.
public enum HttpRequestProfile {
    RAW_HTTP,
    EMULATED_WEB_SOCKET,
    EMULATED_WEB_SOCKET_CREATE,
    EMULATED_WEB_SOCKET_DOWNSTREAM,
    EMULATED_WEB_SOCKET_REVALIDATE,

    PREFLIGHT_COOKIES,
    WEBSOCKET_UPGRADE,
    WEBSOCKET_REVALIDATE,

    OTHER_PROTOCOL_UPGRADE;

    public static final String EMULATED_SUFFIX = "/;e";

    static EnumSet<HttpRequestProfile> NATIVE_WEBSOCKET_PROFILES =
            EnumSet.of(WEBSOCKET_UPGRADE,
                       WEBSOCKET_REVALIDATE);

    static EnumSet<HttpRequestProfile> EMULATED_PROFILES =
            EnumSet.of(EMULATED_WEB_SOCKET,
                       EMULATED_WEB_SOCKET_CREATE,
                       EMULATED_WEB_SOCKET_DOWNSTREAM,
                       EMULATED_WEB_SOCKET_REVALIDATE);

    static EnumSet<HttpRequestProfile> REVALIDATE_PROFILES =
            EnumSet.of(EMULATED_WEB_SOCKET_REVALIDATE,
                       WEBSOCKET_REVALIDATE);

    static EnumSet<HttpRequestProfile> WEBSOCKET_PROFILES =
            EnumSet.of(EMULATED_WEB_SOCKET,
                       EMULATED_WEB_SOCKET_CREATE,
                       EMULATED_WEB_SOCKET_DOWNSTREAM,
                       EMULATED_WEB_SOCKET_REVALIDATE,
                       WEBSOCKET_UPGRADE,
                       WEBSOCKET_REVALIDATE);

    static EnumSet<HttpRequestProfile> LOGIN_PROFILES =
            EnumSet.of(EMULATED_WEB_SOCKET_CREATE,
                       EMULATED_WEB_SOCKET_REVALIDATE,
                       WEBSOCKET_UPGRADE,
                       WEBSOCKET_REVALIDATE,
                       RAW_HTTP);

    static EnumSet<HttpRequestProfile> RAW_HTTP_REQUESTS =
            EnumSet.of(RAW_HTTP);

    public boolean isRevalidateProfile() {
        return REVALIDATE_PROFILES.contains(this);
    }

    public boolean isEmulatedProfile() {
        return EMULATED_PROFILES.contains(this);
    }

    public boolean isWebSocket() {
        return WEBSOCKET_PROFILES.contains(this);
    }

    public static HttpRequestProfile valueOf(HttpRequestMessage r) {
        if ( r == null ) {
            throw new NullPointerException("request");
        }

        // Assume HTTP
        HttpRequestProfile result = RAW_HTTP;

        //
        // Are we attempting to emulate a web socket?
        //
        final String path = r.getRequestURI().getPath();
        if ( path != null && path.contains(EMULATED_SUFFIX)) {
            result = EMULATED_WEB_SOCKET;
            if ( path.contains(EMULATED_SUFFIX + "/cookies")) {
                result = PREFLIGHT_COOKIES;

            } else if ( path.contains(EMULATED_SUFFIX + "/c")) {
                result = EMULATED_WEB_SOCKET_CREATE;

            } else if ( path.contains(EMULATED_SUFFIX + "/dte")) {
                // Note: We SHOULD look for "/dt" here, but for now, we
                // only really need this profile for IE browsers.  For them,
                // we only need to worry about "/dte".
                result = EMULATED_WEB_SOCKET_DOWNSTREAM;
            }
        }

        //
        // Are we attempting to re-authorize a WebSocket?
        //
        if ( path != null && path.contains(HttpProtocolCompatibilityFilter.REVALIDATE_SUFFIX))  {
            result = WEBSOCKET_REVALIDATE;
            if ((path.contains(HttpProtocolCompatibilityFilter.EMULATED_REVALIDATE_SUFFIX) ||
                 path.contains(HttpProtocolCompatibilityFilter.RTMP_REVALIDATE_SUFFIX))) {
                result = EMULATED_WEB_SOCKET_REVALIDATE;
            }
        }

        //
        // It's important that an emulated "create" request gets treated as a WEBSOCKET_UPGRADE
        // and not an EMULATED_WEB_SOCKET request, that's why this WEBSOCKET profiling comes last.
        //
        if (r.hasHeader("Upgrade")) {
            if ("websocket".equalsIgnoreCase(r.getHeader("Upgrade"))) {
                WsHandshakeValidator v = new WsHandshakeValidator();
                if ( v.validate(r) ) {
                    result = WEBSOCKET_UPGRADE;
                } else {
                    return OTHER_PROTOCOL_UPGRADE;
                }
            } else {
                result = OTHER_PROTOCOL_UPGRADE;
            }
        }
        return result;
    }
}
