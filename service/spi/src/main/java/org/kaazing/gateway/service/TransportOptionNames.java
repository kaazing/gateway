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

package org.kaazing.gateway.service;

/**
 * When building a ResourceAddress with a Map<String,Object>, what are the valid string keys?
 */
@Deprecated
public interface TransportOptionNames {

    /**
     * What protocol is spoken using my session?
     */
    public static final String NEXT_PROTOCOL = "nextProtocol"; // set at each level pointing back up.

    /**
     * What WebSocket protocols does the server globally support?
     */
    public static final String SUPPORTED_PROTOCOLS = "supportedProtocols";

    /**
     * What WebSocket inactivity timeout is defined, if any (in millis)?
     */
    public static final String INACTIVITY_TIMEOUT = "inactivityTimeout";

    /**
     * For WebSocket connections from the gateway, what version of the WebSocket protocol should be used?
     */
    public static final String WS_PROTOCOL_VERSION = "protocolVersion";
    public static final String WS_LIGHTWEIGHT_SESSION = "wsn.isLightweightWsnSession";

    /**
     * To indicated persistent HTTP connections
     */
    public static final String HTTP_KEEP_ALIVE = "keepAlive";

    /**
     * How long should we keep a HTTP session alive?  Useful for directory services.
     */
    public static final String HTTP_KEEP_ALIVE_TIMEOUT_KEY = "keepAliveTimeout";

    //------

    public static final String SSL_CIPHERS = "ssl.ciphers";
    public static final String SSL_PROTOCOLS = "ssl.protocols";
    public static final String SSL_ENCRYPTION_ENABLED = "ssl.encryptionEnabled";
    public static final String SSL_WANT_CLIENT_AUTH = "ssl.wantClientAuth";
    public static final String SSL_NEED_CLIENT_AUTH = "ssl.needClientAuth";
    public static final String SSL_KEY_SELECTOR = "ssl.keySelector";

    public static final String TCP_MAXIMUM_OUTBOUND_RATE = "tcp.maximumOutboundRate";

    public static final String TCP_TRANSPORT = "tcp.transport";
    public static final String SSL_TRANSPORT = "ssl.transport";
    public static final String HTTP_TRANSPORT = "http.transport";

    public static final String PIPE_TRANSPORT = "pipe.transport";

}
