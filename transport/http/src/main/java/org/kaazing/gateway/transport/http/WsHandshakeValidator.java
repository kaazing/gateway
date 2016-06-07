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
package org.kaazing.gateway.transport.http;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.kaazing.gateway.transport.http.bridge.HttpRequestMessage;
import org.kaazing.gateway.util.ws.WebSocketWireProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements http://tools.ietf.org/html/draft-ietf-hybi-thewebsocketprotocol-14#section-4.2.1
 */
public class WsHandshakeValidator {

    public static final String SEC_WEB_SOCKET_KEY = "Sec-WebSocket-Key";
    public static final String SEC_WEB_SOCKET_VERSION = "Sec-WebSocket-Version";
    public static final String SEC_WEB_SOCKET_KEY1 = "Sec-WebSocket-Key1";
    public static final String SEC_WEB_SOCKET_KEY2 = "Sec-WebSocket-Key2";

    protected static final Charset UTF_8 = Charset.forName("UTF-8");

    protected final Logger logger = LoggerFactory.getLogger("websocket.handshake.validator");

    public static Map<WebSocketWireProtocol, WsHandshakeValidator> handshakeValidatorsByWireProtocolVersion
            = new ConcurrentHashMap<>(10);

    private static boolean initialized;

    public boolean validate(HttpRequestMessage request) {
        return validate(request, true);
    }

    /**
     * Facade method to validate an HttpMessageRequest.
     *
     * @param request the request to validate
     * @return true iff the appropriate handshake for websocket protocol is contained
     *         within the provided request.
     */
    public boolean validate(HttpRequestMessage request, boolean isPostMethodAllowed) {
        WebSocketWireProtocol wireProtocolVersion = guessWireProtocolVersion(request);
        if ( wireProtocolVersion == null ) {
            return false;
        }

        final WsHandshakeValidator validator = handshakeValidatorsByWireProtocolVersion.get(wireProtocolVersion);
        return validator != null && validator.doValidate(request, isPostMethodAllowed);

    }

    public static WebSocketWireProtocol guessWireProtocolVersion(HttpRequestMessage httpRequest) {
        String httpRequestVersionHeader = httpRequest.getHeader(WsHandshakeValidator.SEC_WEB_SOCKET_VERSION);
        if ( httpRequestVersionHeader == null || httpRequestVersionHeader.length() == 0) {
            // Let's see if the request looks like Hixie 75 or 76
            if ( httpRequest.getHeader(WsHandshakeValidator.SEC_WEB_SOCKET_KEY1) != null &&
                    httpRequest.getHeader(WsHandshakeValidator.SEC_WEB_SOCKET_KEY2) != null ) {
                return WebSocketWireProtocol.HIXIE_76;
            } else {
                return WebSocketWireProtocol.HIXIE_75;
            }
        } else {
            try {
                return WebSocketWireProtocol.valueOf(Integer.parseInt(httpRequestVersionHeader));
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    /**
     * Does the provided request form a valid web socket handshake request?
     *
     * @param request the candidate web socket handshake request
     * @return true iff the provided request is a web socket handshake request.
     */
    protected boolean doValidate(HttpRequestMessage request, final boolean isPostMethodAllowed) {

        if ( !isPostMethodAllowed ) {
            if ( request.getMethod() != HttpMethod.GET) {
                return false;
            }
        } else {
            if ( request.getMethod() != HttpMethod.GET || request.getMethod() == HttpMethod.POST ) {
                return false;
            }
        }

        if ( request.getVersion() != HttpVersion.HTTP_1_1) {
            return false;
        }

        if ( request.getRequestURI() == null ) {
            return false;
        }

        boolean ok = requireHeader(request, "Connection", "Upgrade");
        if ( !ok ) { return false; }

        ok = requireHeader(request, "Upgrade", "WebSocket");
        if ( !ok ) { return false; }

        ok = requireHeader(request, "Host");

        return ok;
    }

    public boolean requireHeader(HttpRequestMessage request, String name) {
        if ( request == null || name == null ) {
            return false;
        }

        List<String> headers = request.getHeaderValues(name, false);
        if ( headers != null ) {
            boolean found = false;
            for(String header: headers) {
                if ( header != null && header.trim().length() > 0) {
                    found = true;
                }
            }
            return found;
        }
        return false;
    }


    public boolean requireHeader(HttpRequestMessage request, String name, String value) {
        if ( request == null || name == null || value == null) {
            return false;
        }

        List<String> headers = request.getHeaderValues(name, false);
        if ( headers != null ) {
            boolean found = false;
            for(String header: headers) {
                if ( header != null && header.trim().equalsIgnoreCase(value)) {
                    found = true;
                }
            }
            return found;
        }
        return false;
    }


    /**
     * Allow subclasses to register themselves as validators for specific versions of
     * the wire protocol.
     *
     * @param wireProtocolVersion the version of the protocol
     * @param validator the validator
     */
    private void register(WebSocketWireProtocol wireProtocolVersion, WsHandshakeValidator validator) {
        if ( wireProtocolVersion == null ) {
            throw new NullPointerException("wireProtocolVersion");
        }
        if ( validator == null ) {
            throw new NullPointerException("validator");
        }

        WsHandshakeValidator existingValidator =
                handshakeValidatorsByWireProtocolVersion.put(wireProtocolVersion, validator);

        logger.trace("Class "+validator.getClass().getName()+" registered to support websocket handshake for protocol "+wireProtocolVersion);
        if ( existingValidator != null ) {
            logger.trace("Multiple handshake validators have registered to support wire protocol "+wireProtocolVersion+". Using class "+this.getClass().getName()+'.');
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    public synchronized void init() {
        if ( !initialized ) {
            // Instantiate all supported protocols
            register(WebSocketWireProtocol.HIXIE_75, new WsProtocolHixie75HandshakeValidator());
            register(WebSocketWireProtocol.HIXIE_76, new WsProtocolHixie76HandshakeValidator());
            register(WebSocketWireProtocol.HYBI_8, new WsProtocol8HandshakeValidator());
            register(WebSocketWireProtocol.HYBI_13, new WsProtocol13HandshakeValidator());
            register(WebSocketWireProtocol.RFC_6455, new WsProtocol13HandshakeValidator());
            initialized = true;
        }
    }
}

