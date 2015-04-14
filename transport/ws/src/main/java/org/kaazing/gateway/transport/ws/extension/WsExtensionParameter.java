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
public interface WsExtensionParameter {

    String getName();
    String getValue();

}
