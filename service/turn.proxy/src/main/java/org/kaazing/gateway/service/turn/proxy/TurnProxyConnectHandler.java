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
import org.kaazing.gateway.service.turn.proxy.stun.attributes.Attribute;
import org.kaazing.gateway.service.turn.proxy.stun.attributes.MappedAddress;
import org.kaazing.gateway.service.turn.proxy.stun.attributes.XorMappedAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

import static org.kaazing.gateway.service.turn.proxy.TurnProxyService.FIXED_MAPPED_ADDRESS_KEY;

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

                // TODO here we should override the mapped address and the relay address
                // mapped address -> get the acceptor session ip and port
                //                   could also used a configured value
                // TODO use an instance variable since the config will not change from one session to another
                InetSocketAddress acceptAddress = null;
                String fixedMappedAddress = (String) session.getAttribute(FIXED_MAPPED_ADDRESS_KEY);
                if (fixedMappedAddress != null) {
                    int i = fixedMappedAddress.lastIndexOf(":");
                     acceptAddress = new InetSocketAddress(fixedMappedAddress.substring(0, i), Integer.parseInt(fixedMappedAddress.substring(i+1, fixedMappedAddress.length())));
                }

                if (acceptAddress == null) {
                    AttachedSessionManager attachedSessionManager = getAttachedSessionManager(session);
                    IoSession acceptSession = attachedSessionManager.getAttachedSession();
                    LOGGER.debug(acceptSession.getRemoteAddress().toString());
                    if (acceptSession.getRemoteAddress() instanceof InetSocketAddress) {
                        acceptAddress = (InetSocketAddress) acceptSession.getRemoteAddress();
                    }
                }

                LOGGER.debug("Will override mapped-address or xor-mapped-address with: " + acceptAddress.toString());
                if (acceptAddress != null) {
                    for (int i = 0; i < stunMessage.getAttributes().size(); i++) {
                        Attribute attribute = stunMessage.getAttributes().get(i);
                        if (attribute instanceof MappedAddress) {
                            stunMessage.getAttributes().set(i, new MappedAddress(acceptAddress));
                            stunMessage.setModified(true);
                        } else if (attribute instanceof XorMappedAddress) {
                            stunMessage.getAttributes().set(i, new XorMappedAddress(acceptAddress, stunMessage.getTransactionId()));
                            stunMessage.setModified(true);
                        }
                    }
                }
                // relay address -> the proxy's address and port ???
            }
        }
        super.messageReceived(session, message);
    }
}