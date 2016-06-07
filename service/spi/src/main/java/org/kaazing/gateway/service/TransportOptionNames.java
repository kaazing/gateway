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
package org.kaazing.gateway.service;

/**
 * When building a ResourceAddress with a Map<String,Object>, what are the valid string keys?
 */
@Deprecated
public interface TransportOptionNames {

    /**
     * What protocol is spoken using my session?
     */
    String NEXT_PROTOCOL = "nextProtocol"; // set at each level pointing back up.

    /**
     * What WebSocket protocols does the server globally support?
     */
    String SUPPORTED_PROTOCOLS = "supportedProtocols";

    /**
     * What WebSocket inactivity timeout is defined, if any (in millis)?
     */
    String INACTIVITY_TIMEOUT = "inactivityTimeout";

    /**
     * For WebSocket connections from the gateway, what version of the WebSocket protocol should be used?
     */
    String WS_PROTOCOL_VERSION = "protocolVersion";
    String WS_LIGHTWEIGHT_SESSION = "wsn.isLightweightWsnSession";

    /**
     * To indicated persistent HTTP connections
     */
    String HTTP_KEEP_ALIVE = "keepAlive";

    /**
     * How long should we keep a HTTP session alive?  Useful for directory services.
     */
    String HTTP_KEEP_ALIVE_TIMEOUT_KEY = "keepAliveTimeout";

    //------

    String SSL_CIPHERS = "ssl.ciphers";
    String SSL_PROTOCOLS = "ssl.protocols";
    String SSL_ENCRYPTION_ENABLED = "ssl.encryptionEnabled";
    String SSL_WANT_CLIENT_AUTH = "ssl.wantClientAuth";
    String SSL_NEED_CLIENT_AUTH = "ssl.needClientAuth";
    String SSL_KEY_SELECTOR = "ssl.keySelector";

    String HTTP_SERVER_HEADER_ENABLED = "http.serverHeaderEnabled";


    String TCP_MAXIMUM_OUTBOUND_RATE = "tcp.maximumOutboundRate";

    String TCP_TRANSPORT = "tcp.transport";
    String SSL_TRANSPORT = "ssl.transport";
    String HTTP_TRANSPORT = "http.transport";

    String PIPE_TRANSPORT = "pipe.transport";

}
