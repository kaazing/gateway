package org.kaazing.gateway.service.turn.proxy;

import java.util.List;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;
import org.kaazing.gateway.service.ServiceContext;
import org.kaazing.gateway.service.proxy.AbstractProxyAcceptHandler;
import org.kaazing.gateway.service.proxy.AbstractProxyHandler;
import org.kaazing.gateway.service.turn.proxy.stun.StunCodecFilter;
import org.kaazing.gateway.service.turn.proxy.stun.StunMessage;
import org.kaazing.gateway.service.turn.proxy.stun.StunMessageAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TurnProxyHandler extends AbstractProxyAcceptHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("service.turn.proxy");

    private String connectURI;

    public TurnProxyHandler() {
        super();
    }

    public void init(ServiceContext serviceContext) {
        connectURI = serviceContext.getConnects().iterator().next();
    }

    @Override
    protected AbstractProxyHandler createConnectHandler() {
        return new ConnectHandler();
    }

    @Override
    public void sessionCreated(IoSession acceptSession) {
        acceptSession.getFilterChain().addLast("STUN_CODEC", new StunCodecFilter());
        super.sessionCreated(acceptSession);
    }

    @Override
    public void sessionOpened(IoSession acceptSession) {
        ConnectSessionInitializer sessionInitializer = new ConnectSessionInitializer(acceptSession);
        ConnectFuture connectFuture = getServiceContext().connect(connectURI, getConnectHandler(), sessionInitializer);
        connectFuture.addListener(new ConnectListener(acceptSession));
        super.sessionOpened(acceptSession);
    }

    @Override
    public void messageReceived(IoSession session, Object message) {
        if(LOGGER.isDebugEnabled()){
            LOGGER.debug("Recieved message [%s] from [%s] ", message, session);
        }
        StunMessage stunMessage = (StunMessage) message;
        List<StunMessageAttribute> attributes = stunMessage.getAttributes();
//        for(StunMessageAttribute attribute: )
        super.messageReceived(session, stunMessage);
    }

    class ConnectHandler extends AbstractProxyHandler {

        public ConnectHandler() {
        }
    }

    /*
     * Initializer for connect session. It adds the processed accept session headers on the connect session
     */
    class ConnectSessionInitializer implements IoSessionInitializer<ConnectFuture> {
        private final IoSession acceptSession;

        ConnectSessionInitializer(IoSession acceptSession) {
            this.acceptSession = acceptSession;
        }

        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            session.getFilterChain().addLast("STUN_CODEC", new StunCodecFilter());
        }

    }

    private class ConnectListener implements IoFutureListener<ConnectFuture> {

        private final IoSession acceptSession;

        ConnectListener(IoSession acceptSession) {
            this.acceptSession = acceptSession;
        }

        @Override
        public void operationComplete(ConnectFuture future) {
            if (future.isConnected()) {
                IoSession connectSession = future.getSession();

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Connected to " + connectURI + " [" + acceptSession + "->" + connectSession + "]");
                }
                if (acceptSession == null || acceptSession.isClosing()) {
                    connectSession.close(true);
                } else {
                    AttachedSessionManager attachedSessionManager = attachSessions(acceptSession, connectSession);
                    flushQueuedMessages(acceptSession, attachedSessionManager);
                }
            } else {
                LOGGER.warn("Connection to " + connectURI + " failed [" + acceptSession + "->]");
                acceptSession.close(true);
            }
        }
    }

}
