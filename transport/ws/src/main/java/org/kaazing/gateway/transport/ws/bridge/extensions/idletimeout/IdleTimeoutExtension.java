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

package org.kaazing.gateway.transport.ws.bridge.extensions.idletimeout;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.kaazing.gateway.transport.ws.bridge.extensions.WsExtensions;
import org.kaazing.gateway.transport.ws.extension.ExtensionHeader;
import org.kaazing.gateway.transport.ws.extension.ExtensionHeaderBuilder;

public final class IdleTimeoutExtension extends ExtensionHeaderBuilder {
    private final long idleTimeoutMillis;

    public IdleTimeoutExtension(ExtensionHeader extension, long idleTimeoutMillis) {
        super(extension);
        appendParameter(WsExtensions.IDLE_TIMEOUT_TIMEOUT_PARAM, Long.toString(idleTimeoutMillis));
        this.idleTimeoutMillis = idleTimeoutMillis;
    }
    
    @Override
    public void removeBridgeFilters(IoFilterChain filterChain) {
        try {
            filterChain.remove(IdleTimeoutFilter.class);
        }
        catch (IllegalArgumentException e) {
            // filter was not found, ignore
        }
    }
    
    @Override
    public void updateBridgeFilters(IoFilterChain filterChain) {
        filterChain.addLast(getExtensionToken(), new IdleTimeoutFilter(idleTimeoutMillis));
    }
    
}
