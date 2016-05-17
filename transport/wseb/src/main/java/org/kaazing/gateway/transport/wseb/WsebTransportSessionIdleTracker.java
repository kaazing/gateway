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
package org.kaazing.gateway.transport.wseb;

import static java.lang.Thread.currentThread;
import static org.kaazing.gateway.resource.address.ws.WsResourceAddress.INACTIVITY_TIMEOUT;

import org.kaazing.gateway.transport.TypedAttributeKey;
import org.kaazing.gateway.transport.ws.bridge.filter.WsCheckAliveFilter;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.mina.netty.DefaultIoSessionIdleTracker;
import org.kaazing.mina.netty.IoSessionIdleTracker;
import org.slf4j.Logger;

/**
 * The purpose of this class is to use WsCheckAliveFilter to close off WsebSessions when connectivity
 * to the client is lost. In order to make it work, we set up a dummy session (and filter chain) for each added
 * WsebSession, and use mina.netty's DefaultIoSessionIdleTracker to fire idle events on that.
 * This is a stopgap until we have a single emulated TCP session as the parent of the WsebSession. Once we have that,
 * we will just set the WsCheckAliveFilter filter on the parent's filter chain, as we do for WSN, and use
 * DefaultIoSessionIdleTracker to support idleTimeout on the TCPE session.
 */
class WsebTransportSessionIdleTracker implements IoSessionIdleTracker {
    private static final String CHECK_ALIVE_FILTER = WsebProtocol.NAME + "#checkalive";
    private static final TypedAttributeKey<Boolean> ALREADY_TRACKED = new TypedAttributeKey<>(
            WsebTransportSessionIdleTracker.class, "tracked");

    private final Logger logger;
    private final IoSessionIdleTracker idleTracker = new DefaultIoSessionIdleTracker();

    public WsebTransportSessionIdleTracker(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void addSession(final IoSessionEx wsebSession) {
        if (wsebSession.getIoThread() == currentThread()) {
            addSession0(wsebSession);
        }
        else {
            wsebSession.getIoExecutor().execute(new Runnable() {

                @Override
                public void run() {
                    addSession0(wsebSession);
                }

            });
        }
    }

    private void addSession0(IoSessionEx wsebSession) {
        if (ALREADY_TRACKED.get(wsebSession, false)) {
            // Expected for downstream reconnects
            return;
        }
        IoSessionEx transportSession = ((WsebSession)wsebSession).getTransportSession();
        ALREADY_TRACKED.set(wsebSession, true);
        idleTracker.addSession(transportSession);
        WsCheckAliveFilter.addIfFeatureEnabled(transportSession.getFilterChain(), CHECK_ALIVE_FILTER,
                ((WsebSession) wsebSession).getLocalAddress().getOption(INACTIVITY_TIMEOUT), wsebSession, logger);
    }

    @Override
    public void removeSession(IoSessionEx wsebSession) {
        if (ALREADY_TRACKED.get(wsebSession, false)) {
            // may not be set if wseb connection failed
            idleTracker.removeSession(((WsebSession)wsebSession).getTransportSession());
        }
    }

    @Override
    public void dispose() {
        idleTracker.dispose();
    }

}
