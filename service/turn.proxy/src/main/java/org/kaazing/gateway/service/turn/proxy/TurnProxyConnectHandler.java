package org.kaazing.gateway.service.turn.proxy;

import org.apache.mina.core.session.IoSession;
import org.kaazing.gateway.service.proxy.AbstractProxyHandler;
import org.kaazing.gateway.service.turn.proxy.stun.StunMessageClass;
import org.kaazing.gateway.service.turn.proxy.stun.StunMessageMethod;
import org.kaazing.gateway.service.turn.proxy.stun.StunProxyMessage;

class TurnProxyConnectHandler extends AbstractProxyHandler {

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
            if (TurnProxyAcceptHandler.LOGGER.isDebugEnabled()) {
                TurnProxyAcceptHandler.LOGGER.debug("Recieved message [%s] from [%s] ", message, session);
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