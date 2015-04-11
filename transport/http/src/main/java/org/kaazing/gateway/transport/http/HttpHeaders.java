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

package org.kaazing.gateway.transport.http;

public interface HttpHeaders {

    String HEADER_AUTHORIZATION = "Authorization";
    String HEADER_CONTENT_LENGTH = "Content-Length";
    String HEADER_CONTENT_TYPE = "Content-Type";
    String HEADER_DATE = "Date";

    String HEADER_HOST = "Host";
    String HEADER_MAX_AGE = "Max-Age";
    String HEADER_USER_AGENT = "User-Agent";
    String HEADER_WEBSOCKET_EXTENSIONS = "X-WebSocket-Extensions";
    String HEADER_WEBSOCKET_VERSION = "X-WebSocket-Version";
    String HEADER_X_NEXT_PROTOCOL = "X-Next-Protocol";
    String HEADER_X_ORIGIN = "X-Origin";
    String HEADER_X_CREATE_ENCODING = "X-Create-Encoding";
    String HEADER_X_ACCEPT_COMMANDS = "X-Accept-Commands";
    String HEADER_CACHE_CONTROL = "Cache-Control";


    String HEADER_UPGRADE = "Upgrade";
    String HEADER_VIA= "Via";
    String HEADER_CONNECTION = "Connection";
    String HEADER_TRANSFER_ENCODING = "Transfer-Encoding";
    String HEADER_X_SEQUENCE_NO = "X-Sequence-No";

}
