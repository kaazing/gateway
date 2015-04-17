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
import static org.kaazing.gateway.transport.ws.WsMessage.Kind.TEXT;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.filterchain.IoFilterChain.Entry;
import org.kaazing.gateway.transport.ws.WsMessage;
import org.kaazing.gateway.transport.ws.WsMessage.Kind;
import org.kaazing.gateway.transport.ws.WsPingMessage;
import org.kaazing.gateway.transport.ws.WsPongMessage;
import org.kaazing.gateway.transport.ws.extension.Extension;
import org.kaazing.gateway.transport.ws.extension.ExtensionBuilder;
import org.kaazing.gateway.util.Utils;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.filter.codec.ProtocolCodecFilter;

public final class PingPongExtension extends ExtensionBuilder {
    // We want to use valid but infrequently used UTF-8 characteers. ASCII control characters fit the bill!
    private static final byte[] CONTROL_BYTES = { (byte)0x01, (byte)0x01, (byte)0x01, (byte)0x02 };
    
    private static final byte[] EMPTY_PING_BYTES = { (byte)0x09, (byte)0x00 };
    private static final byte[] EMPTY_PONG_BYTES = { (byte)0x0a, (byte)0x00 };
    private static final WsPingMessage emptyPing = new WsPingMessage();
    private static final WsPongMessage emptyPong = new WsPongMessage();
    static {
        emptyPing.setStyle(CLIENT);
        emptyPong.setStyle(CLIENT);
    }
    
    public PingPongExtension(Extension extension) {
        super(extension);
        appendParameter(Utils.toHex(CONTROL_BYTES));
    }
    
    @Override
    public boolean canDecode(EndpointKind endpointKind, Kind messageKind) {
        return messageKind == TEXT;
    }
    
    @Override
    public boolean canEncode(EndpointKind endpointKind, Kind messageKind) {
        return messageKind == TEXT;
    }
    
    @Override
    // We  need to add out filter immediately after the codec filter, so we should come last
    // to make sure no-one else gets between us and the codec filter.
    public String getOrdering() {
        return "zzz";
    }
    
    @Override
    public WsMessage decode(IoBufferEx payload) {
        // No payload support for now
        byte first = payload.get();
        switch (first) {
        case (byte)0x09:
            return emptyPing;
        case (byte)0x0a:
            return emptyPong;
        default:
            throw new IllegalArgumentException(String.format("Unrecognized ping pong extension opcode 0x%x", first));
        }
    }
    
    @Override
    public byte[] encode(WsMessage message) {
        // No payload support for now
        switch (message.getKind()) {
        case PING:
            assert ((WsPingMessage)message).getStyle() == CLIENT;
            return EMPTY_PING_BYTES;
        case PONG:
            assert ((WsPongMessage)message).getStyle() == CLIENT;
            return EMPTY_PONG_BYTES;
        default:
            throw new IllegalArgumentException(message.toString());
        }
    }
    
    @Override
    public byte[] getControlBytes() {
        return CONTROL_BYTES;
    }
    
    @Override
    public void removeBridgeFilters(IoFilterChain filterChain) {
        try {
            filterChain.remove(PingPongFilter.class);
        }
        catch (IllegalArgumentException e) {
            // filter was not found, ignore
        }
    }
    
    @Override
    public void updateBridgeFilters(IoFilterChain filterChain) {
        for (Entry entry : filterChain.getAll()) {
            if (ProtocolCodecFilter.class.isAssignableFrom(entry.getFilter().getClass())) {
                filterChain.addAfter(entry.getName(), getExtensionToken(), PingPongFilter.getInstance());
                break;
            }
        }
    }
    
}
