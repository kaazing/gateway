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

package org.kaazing.gateway.transport.ws.bridge.extensions;

import java.util.Arrays;
import java.util.List;

/**
 * A meta-data interface to capture common information about WebSocket extensions.
 */
public interface WsExtensions {

    //
    // Meta Web Socket Extension details
    //
    String NAME = "wsExtension";

    String HEADER_X_WEBSOCKET_EXTENSIONS = "X-WebSocket-Extensions";
    String HEADER_WEBSOCKET_EXTENSIONS = "WebSocket-Extensions";
    String HEADER_SEC_WEBSOCKET_EXTENSIONS = "Sec-WebSocket-Extensions";

    List<String> NATIVE_EXTENSION_HEADERS = Arrays.asList(WsExtensions.HEADER_SEC_WEBSOCKET_EXTENSIONS,
                                                          WsExtensions.HEADER_WEBSOCKET_EXTENSIONS);

    // Idle timeout Notify extension details
    String IDLE_TIMEOUT = "x-kaazing-idle-timeout";
    String IDLE_TIMEOUT_TIMEOUT_PARAM = "timeout";

    // Ping pong extension
    String PING_PONG = "x-kaazing-ping-pong";

}
