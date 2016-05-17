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

import org.apache.mina.core.session.IdleStatus;

import org.apache.mina.core.session.IoSession;

public class DirectProxyHandler extends ProxyHandler {

    public static final ProxyHandler DIRECT_PROXY_HANDLER = new DirectProxyHandler();

    private DirectProxyHandler() {
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        IoSession proxySession = getProxySession(session);
        proxySession.getFilterChain().fireExceptionCaught(cause);
    }

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        IoSession proxySession = getProxySession(session);
        proxySession.getFilterChain().fireMessageReceived(message);
    }

    @Override
    public void sessionClosed(IoSession session) throws Exception {
        IoSession proxySession = getProxySession(session);
        if (!proxySession.isClosing()) {
            proxySession.close(false);
        }
    }

    @Override
    public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
        IoSession proxySession = getProxySession(session);
        proxySession.getFilterChain().fireSessionIdle(status);
    }

    private IoSession getProxySession(IoSession session) {
        return (IoSession) session.getAttribute(PROXY_SESSION_KEY);
    }
}
