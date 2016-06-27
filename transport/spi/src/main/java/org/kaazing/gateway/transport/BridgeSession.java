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
package org.kaazing.gateway.transport;

import java.net.SocketAddress;

import org.apache.mina.core.session.IoSession;

import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.transport.bridge.CachingMessageEncoder;
import org.kaazing.mina.core.session.IoSessionEx;

public interface BridgeSession extends IoSessionEx {

    TypedAttributeKey<String> NEXT_PROTOCOL_KEY = new TypedAttributeKey<>(BridgeSession.class, "nextProtocol");
    /**
     * Used as an attribute on transports below bridge acceptors
     * that use InetSocketAddresses rather than ResourceAddress objects
     * as their local address.
     *
     * If we encounter a bridge session with a parent whose local address
     * is an inet socket address, we can ask the parent for this attribute
     * and receive the corresponding ResourceAddress object.
     *
     * In this way, we can almost pretend that all transport sessions have
     * {@code ResourceAddress} objects as local sessions.
     *
     * A code example:
     * <pre>
     * ResourceAddress localAddress = LOCAL_ADDRESS.get(nioSocketSession);
     * </pre>
     */
    TypedAttributeKey<ResourceAddress> LOCAL_ADDRESS = new TypedAttributeKey<ResourceAddress>(BridgeAcceptor.class, "localAddress") {
    
        private static final long serialVersionUID = 1L;
    
        @Override
        public ResourceAddress get(IoSession session) {
            SocketAddress localAddress = session.getLocalAddress();
            if (localAddress instanceof ResourceAddress) {
                return (ResourceAddress) localAddress;
            }
            return super.get(session);
        }
        
    };
    /**
     * Used as an attribute on transports below bridge acceptors
     * that use InetSocketAddresses rather than ResourceAddress objects
     * as their local address.
     *
     * If we encounter a bridge session with a parent whose local address
     * is an inet socket address, we can ask the parent for this attribute
     * and receive the corresponding ResourceAddress object.
     *
     * In this way, we can almost pretend that all transport sessions have
     * {@code ResourceAddress} objects as local sessions.
     *
     * A code example:
     * <pre>
     * ResourceAddress remoteAddress = REMOTE_ADDRESS.get(nioSocketSession);
     * </pre>
     */
    TypedAttributeKey<ResourceAddress> REMOTE_ADDRESS = new TypedAttributeKey<ResourceAddress>(BridgeConnector.class, "remoteAddress") {
    
        private static final long serialVersionUID = 1L;
    
        @Override
        public ResourceAddress get(IoSession session) {
            SocketAddress remoteAddress = session.getRemoteAddress();
            if (remoteAddress instanceof ResourceAddress) {
                return (ResourceAddress) remoteAddress;
            }
            return super.get(session);
        }
        
    };

    IoSessionEx getParent();

    Direction getDirection();

    CachingMessageEncoder getMessageEncoder();

    //boolean suspendFlush();

    //boolean resumeFlush();

    @Override
    ResourceAddress getLocalAddress();

    @Override
    ResourceAddress getRemoteAddress();
}
