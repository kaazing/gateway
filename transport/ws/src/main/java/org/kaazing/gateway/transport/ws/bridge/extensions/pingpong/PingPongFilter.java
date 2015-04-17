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

package org.kaazing.gateway.transport.ws.bridge.extensions.pingpong;

import static org.kaazing.gateway.transport.ws.AbstractWsControlMessage.Style.CLIENT;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.kaazing.gateway.transport.ws.WsFilterAdapter;
import org.kaazing.gateway.transport.ws.WsPingMessage;
import org.kaazing.gateway.transport.ws.WsPongMessage;
import org.kaazing.gateway.transport.ws.bridge.extensions.WsExtensions;
import org.kaazing.gateway.transport.ws.extension.ExtensionBuilder;

/**
 * This filter is used when the x-kaazing-ping-pong extension is active, to set the extension instance on outgoing PING or
 * PONG messages of style client, to cause them to be encoded as extension messages visible to the Kaazing client libraries.  
 */
class PingPongFilter extends WsFilterAdapter {
    private static final PingPongExtension EXTENSION = new PingPongExtension(new ExtensionBuilder(WsExtensions.PING_PONG));
    
    private static final PingPongFilter INSTANCE = new PingPongFilter();
    
    public static PingPongFilter getInstance() {
        return INSTANCE;
    }
    
    @Override
    protected Object doFilterWriteWsPing(NextFilter nextFilter, IoSession session, WriteRequest writeRequest, WsPingMessage wsPing)
            throws Exception {
        if (wsPing.getStyle() == CLIENT) {
            wsPing.setExtension(EXTENSION);
        }
        return wsPing;
    }
    
    @Override
    protected Object doFilterWriteWsPong(NextFilter nextFilter, IoSession session, WriteRequest writeRequest, WsPongMessage wsPong)
            throws Exception {
        if (wsPong.getStyle() == CLIENT) {
            wsPong.setExtension(EXTENSION);
        }
        return wsPong;
    }
    
}
