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
package org.kaazing.gateway.service.turn.proxy;

import org.apache.mina.core.session.IoSession;
import org.kaazing.gateway.service.proxy.AbstractProxyHandler;
import org.kaazing.gateway.service.turn.proxy.stun.StunMessageClass;
import org.kaazing.gateway.service.turn.proxy.stun.StunMessageMethod;
import org.kaazing.gateway.service.turn.proxy.stun.StunProxyMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class TurnProxyConnectHandler extends AbstractProxyHandler {

    static final Logger LOGGER = LoggerFactory.getLogger(TurnProxyConnectHandler.class);

    private final TurnProxyAcceptHandler acceptHandler;

    /**
     * @param turnProxyAcceptHandler
     */
    TurnProxyConnectHandler(TurnProxyAcceptHandler turnProxyAcceptHandler) {
        acceptHandler = turnProxyAcceptHandler;
    }

    @Override
    public void messageReceived(IoSession session, Object message) {
        if (session.getAttribute(TurnProxyAcceptHandler.TURN_STATE_KEY) != TurnSessionState.ALLOCATED && message instanceof StunProxyMessage) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("Received message [%s] from [%s] ", message, session));
            }
            StunProxyMessage stunMessage = (StunProxyMessage) message;
            if (stunMessage.getMethod() == StunMessageMethod.ALLOCATE
                    && stunMessage.getMessageClass() == StunMessageClass.RESPONSE) {
                session.setAttribute(TurnProxyAcceptHandler.TURN_STATE_KEY, TurnSessionState.ALLOCATED);
            }
        }
        super.messageReceived(session, message);
    }
}