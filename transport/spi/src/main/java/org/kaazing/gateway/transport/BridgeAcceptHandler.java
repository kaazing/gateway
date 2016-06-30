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

import static org.kaazing.gateway.transport.BridgeSession.LOCAL_ADDRESS;

import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IoSession;

import org.kaazing.gateway.resource.address.ResourceAddress;

public class BridgeAcceptHandler extends AbstractBridgeHandler {

    public static final TypedAttributeKey<IoHandler> DELEGATE_KEY = new TypedAttributeKey<>(BridgeAcceptHandler.class, "delegate");

    private final BridgeAcceptor acceptor;

    public BridgeAcceptHandler(BridgeAcceptor acceptor) {
        this.acceptor = acceptor;
    }

    @Override
    protected IoHandler getHandler(IoSession session, boolean throwIfNull) throws Exception {
        IoHandler handler = DELEGATE_KEY.get(session);
        if (handler == null) {
            ResourceAddress localAddress = LOCAL_ADDRESS.get(session);
            // If there is an exception in sessionCreated(), it may not set up LOCAL_ADDRESS
            // and hence localAddress could be null. So return null in this case when
            // exceptionCaught()/sessionClosed() looks up the handler.
            handler = localAddress == null ? null : acceptor.getHandler(localAddress);
            if (handler != null) {
                DELEGATE_KEY.set(session, handler);
            }
            else if (throwIfNull) {
                throw new Exception("No handler found for: " + localAddress);
            }
        }
        return handler;
    }
}
