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

import java.net.InetSocketAddress;

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

class TurnProxyConnectHandler extends AbstractProxyHandler {

    static final Logger LOGGER = LoggerFactory.getLogger(TurnProxyConnectHandler.class);
    private String fixedMappedAddress;

    public void setFixedMappedAddress(String fixedMappedAddress) {
        this.fixedMappedAddress = fixedMappedAddress;
    }

    @Override
    public void messageReceived(IoSession session, Object message) {
        if (message instanceof StunProxyMessage) {
            LOGGER.debug(String.format("Received message [%s] from [%s] ", message, session));
            StunProxyMessage stunMessage = (StunProxyMessage) message;
            if (stunMessage.getMethod() == StunMessageMethod.ALLOCATE &&
                stunMessage.getMessageClass() == StunMessageClass.RESPONSE) {
                // TODO here we should override the relay address
                // relay address -> the proxy's address and port ?
                InetSocketAddress acceptAddress = getMappedAddress(session);
                if (acceptAddress != null) {
                    LOGGER.debug(String.format("Will override mapped-address or xor-mapped-address with %s: ", acceptAddress.toString()));
                    overrideMappedAddress(stunMessage, acceptAddress);
                }
            }
        }
        super.messageReceived(session, message);
    }


    private void overrideMappedAddress(StunProxyMessage stunMessage, InetSocketAddress acceptAddress) {
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


    private InetSocketAddress getMappedAddress(IoSession session) {
        InetSocketAddress acceptAddress = null;

        if (fixedMappedAddress != null) {
            int i = fixedMappedAddress.lastIndexOf(':');
            acceptAddress = new InetSocketAddress(
                fixedMappedAddress.substring(0, i),
                Integer.parseInt(fixedMappedAddress.substring(i + 1, fixedMappedAddress.length()))
            );
        } else {
            AttachedSessionManager attachedSessionManager = getAttachedSessionManager(session);
            IoSession acceptSession = attachedSessionManager.getAttachedSession();
            LOGGER.debug(
                String.format("Extracting remote address and port from accept session: %s",
                acceptSession.getRemoteAddress().toString())
            );
            if (acceptSession.getRemoteAddress() instanceof InetSocketAddress) {
                acceptAddress = (InetSocketAddress) acceptSession.getRemoteAddress();
            }
        }
        return acceptAddress;
    }
}