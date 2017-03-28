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
package org.kaazing.gateway.transport.nio.internal;

import java.util.concurrent.Callable;

import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.IoSessionAdapterEx;
import org.kaazing.gateway.transport.LoggingFilter;
import org.kaazing.mina.core.session.IoSessionEx;
import org.slf4j.Logger;

public class NioConnectorTcpBridgeHandler extends IoHandlerAdapter<IoSessionEx> {

    protected final Logger logger;
    protected final String transportName;

    NioConnectorTcpBridgeHandler(Logger logger, String transportName) {
        this.logger = logger;
        this.transportName = transportName;
    }

    @Override
    public void doSessionCreated(IoSessionEx session) throws Exception {
        LoggingFilter.addIfNeeded(logger, session, transportName);
        super.doSessionCreated(session);
    }

    @Override
    public void doSessionClosed(IoSessionEx session) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("AbstractNioConnector.doSessionClosed for session " + session);
        }
        IoSession tcpBridgeSession = getTcpBridgeSession(session);
        tcpBridgeSession.getFilterChain().fireSessionClosed();
    }

    @Override
    protected void doSessionOpened(final IoSessionEx session) throws Exception {
        Callable<IoSessionAdapterEx> sessionFactory = (Callable<IoSessionAdapterEx>) session
            .removeAttribute(AbstractNioConnector.CREATE_SESSION_CALLABLE_KEY);
        IoSessionAdapterEx tcpBridgeSession = sessionFactory.call();
        // already added in session creator, in case of synchronous pipe write from sessionOpened
        assert (session.getAttribute(AbstractNioConnector.TCP_SESSION_KEY) == tcpBridgeSession);

    }

    @Override
    protected void doExceptionCaught(IoSessionEx session, Throwable cause) throws Exception {
        // TODO: reset implementation should inform this one.
        IoSession tcpBridgeSession = getTcpBridgeSession(session);
        if (tcpBridgeSession != null) {
            tcpBridgeSession.getFilterChain().fireExceptionCaught(cause);
        }
    }

    @Override
    public void doSessionIdle(IoSessionEx session, IdleStatus status) throws Exception {
        IoSession tcpBridgeSession = getTcpBridgeSession(session);
        tcpBridgeSession.getFilterChain().fireSessionIdle(status);
    }

    @Override
    protected void doMessageReceived(IoSessionEx session, Object message) throws Exception {
        IoSession tcpBridgeSession = getTcpBridgeSession(session);
        tcpBridgeSession.getFilterChain().fireMessageReceived(message);
    }

    private IoSession getTcpBridgeSession(IoSession session) {
        return (IoSession) session.getAttribute(AbstractNioConnector.TCP_SESSION_KEY);
    }
}
