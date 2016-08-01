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

package org.kaazing.gateway.transport.ws.bridge.filter;

import java.util.Properties;

import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.kaazing.gateway.resource.address.ws.WsResourceAddress;
import org.kaazing.gateway.transport.AbstractBridgeSession;
import org.kaazing.gateway.transport.IoFilterAdapter;
import org.kaazing.gateway.transport.ws.AbstractWsBridgeSession;
import org.kaazing.gateway.transport.ws.WsAcceptor;
import org.kaazing.gateway.transport.ws.WsMessage;
import org.kaazing.gateway.transport.ws.WsPingMessage;
import org.kaazing.mina.core.session.IoSessionConfigEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.slf4j.Logger;

/**
 * The purpose of this filter is to periodically check that the WebSocket connection is still alive.
 * This is necessary to implement detection of broken connections in cases where the immediate local network hop
 * is not down, so the TCP layer (NioProcessor, etc) does not realize the client has gone (story KG-6379). This is
 * done by sending a  WebSocket PING to the client if no data has been received for some time, and closing the connection if
 * no PONG is received from the client within the expected maximum round-trip time.
 */
public class WsCheckAliveFilter extends IoFilterAdapter<IoSessionEx> {
    public static final long DISABLE_INACTIVITY_TIMEOUT = WsResourceAddress.INACTIVITY_TIMEOUT_DEFAULT;

    // feature is disabled by default. If in future we want to enable by default, a suitable default would be "30sec".
    public static final long DEFAULT_WS_INACTIVITY_TIMEOUT_MILLIS = DISABLE_INACTIVITY_TIMEOUT;

    private static final String OBSOLETE_INACTIVITY_TIMEOUT_PROPERTY = "org.kaazing.gateway.transport.ws.INACTIVITY_TIMEOUT";

    private final Logger logger;

    // The following values are in milliseconds
    private final long maxExpectedRtt; // how long to wait for pong reply
    private final long pingDelay; // how long to wait before sending ping
    private final IoSession wsSession;

    private enum NextAction {
        PONG, // ping has been written, awaiting pong
        PING  // need to write ping
    }

    private NextAction nextAction = NextAction.PING;

    private long pingSentTime = 0;

    public static void validateSystemProperties(Properties configuration, Logger logger) {
        // Fail gateway startup if the obsolete system property from JMS Edition release 3.5.3 is used (KG-7125)
        if (configuration != null && configuration.containsKey(OBSOLETE_INACTIVITY_TIMEOUT_PROPERTY)) {
            String message = String.format(
               "System property %s is no longer supported, please use accept-option %s instead in the gateway configuration file",
                OBSOLETE_INACTIVITY_TIMEOUT_PROPERTY, "ws.inactivity.timeout");
            logger.error(message);
            throw new RuntimeException(message);
        }
    }

    public static void addIfFeatureEnabled(IoFilterChain filterChain, String filterName, long inactivityTimeoutIn, Logger logger) {
        addIfFeatureEnabled(filterChain, filterName, inactivityTimeoutIn, null, logger);
    }

    public static void addIfFeatureEnabled(IoFilterChain filterChain, String filterName, long inactivityTimeoutIn,
                                           IoSessionEx wsSession, Logger logger) {
        long inactivityTimeout = getInactivityTimeoutMillis(inactivityTimeoutIn, logger);
        if (inactivityTimeout > 0) {
            filterChain.addLast(filterName, new WsCheckAliveFilter(inactivityTimeout, wsSession, logger));
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Configured WebSocket inactivity timeout (ws.inactivity.timeout) is %d milliseconds", inactivityTimeout));
            }
        }
    }

    public static void moveIfFeatureEnabled(IoFilterChain fromChain, IoFilterChain toChain,
                                            String filterName, long inactivityTimeout, Logger logger) {
        if (inactivityTimeout > 0) {
            IoFilter filter = fromChain.remove(filterName);
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Moving %s filter %s to child filter chain", filterName, filter));
            }
            toChain.addLast(filterName, filter);
        }
    }

    public static void updateExtensions(IoFilterChain filterChain) {
        WsCheckAliveFilter filter = (WsCheckAliveFilter) filterChain.get(WsCheckAliveFilter.class);
        if (filter != null) {
            filter.init(filterChain);
        }
    }

    WsCheckAliveFilter(long inactivityTimeout, Logger logger) {
        this(inactivityTimeout, null, logger);
    }

    WsCheckAliveFilter(long inactivityTimeout, IoSession wsSession, Logger logger) {
        assert inactivityTimeout > 0;
        // KG-7057: Assume maximum possible round-trip time is half the configured inactivity timeout, but don't let it be 0
        this.maxExpectedRtt = Math.max(inactivityTimeout / 2, 1);
        this.pingDelay = maxExpectedRtt;
        this.logger = logger;
        this.wsSession = wsSession;
    }

    @Override
    public void onPostAdd(IoFilterChain filterChain, String name, NextFilter nextFilter) throws Exception {
        init(filterChain);
    }

    @Override
    public void onPreRemove(IoFilterChain filterChain,
                            String name,
                            NextFilter nextFilter) {
        filterChain.getSession().getConfig().setReaderIdleTime(0);
    }

    @Override
    protected void doMessageReceived(NextFilter nextFilter, IoSessionEx session, Object message) throws Exception {
        WsMessage wsMessage = (WsMessage) message;
        switch (wsMessage.getKind()) {
        case PONG:
            if (nextAction != NextAction.PONG) {
                if (logger.isTraceEnabled()) {
                    logger.trace(String.format("WsCheckAliveFilter: Unsolicited PONG received (%s), nextAction = %s",
                            wsMessage, nextAction));
                }
                // Unsolicited PONG from (rogue) client, ignore
                return;
            }

            long roundTripTime = System.currentTimeMillis() - pingSentTime;
            // Print out observed rtt to satisfy the curious
            if (logger.isTraceEnabled()) {
                logger.trace(String.format("WsCheckAliveFilter: PONG received (%s), round-trip time = %d msec, nextAction = %s",
                        wsMessage, roundTripTime, nextAction));
            }
            if (wsSession != null) {
                // wse case where session is not accessible for management
                AbstractWsBridgeSession.LAST_ROUND_TRIP_LATENCY.set(wsSession, roundTripTime);
                AbstractWsBridgeSession.LAST_ROUND_TRIP_LATENCY_TIMESTAMP.set(wsSession, pingSentTime);
            } else {
                AbstractWsBridgeSession.LAST_ROUND_TRIP_LATENCY.set(session, roundTripTime);
                AbstractWsBridgeSession.LAST_ROUND_TRIP_LATENCY_TIMESTAMP.set(session, pingSentTime);
            }
            schedulePing(session);
            return;
        default:
            break;
        }
        nextFilter.messageReceived(session, message);
    }

    @Override
    protected void doSessionIdle(NextFilter nextFilter, IoSessionEx session, IdleStatus status) throws Exception {
        if (status == IdleStatus.READER_IDLE) {
            switch (nextAction) {
            case PONG:
                logger.info("Session {} didn't receive PONG, will close corresponding connection", session);

                // Disable idle timeout so it doesn't fire while we're closing
                session.getConfig().setReaderIdleTime(0);

                // Make sure we don't attempt WS CLOSE handshake in wsn case (want to close the transport immediately)
                // Alter this once we eliminate WsCloseFilter
                IoFilterChain filterChain;
                if (session instanceof AbstractBridgeSession<?,?>
                        && ((AbstractBridgeSession<?,?>) session).getLocalAddress().getOption(WsResourceAddress.LIGHTWEIGHT)) {
                    // Extended handshake case, WsCloseFilter is on the parent session
                    filterChain = ((AbstractBridgeSession<?,?>) session).getParent().getFilterChain();
                }
                else {
                    filterChain = session.getFilterChain();
                }
                if (filterChain.contains(WsAcceptor.CLOSE_FILTER)) {
                    filterChain.remove(WsAcceptor.CLOSE_FILTER);
                }
                IoSession sessionToClose = wsSession != null ? wsSession : session;
                logger.info("Closing session {} as it didn't receive PONG", sessionToClose);
                sessionToClose.close(true);
                break;
            case PING:
                writePing(nextFilter, session);
            }
        }
        super.doSessionIdle(nextFilter, session, status);
    }

    private static long getInactivityTimeoutMillis(long inactivityTimeoutIn, Logger logger) {
        if (inactivityTimeoutIn == DISABLE_INACTIVITY_TIMEOUT) {
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("WebSocket inactivity timeout is disabled (you can use accept-option or connect-option \"%s\" to enable it)",
                        "ws.inactivity.timeout"));
            }
        }
        return inactivityTimeoutIn;
    }

    private void init(IoFilterChain filterChain) {
        IoSessionEx session = (IoSessionEx)filterChain.getSession();
        schedulePing(session);
    }

    private void schedulePing(IoSessionEx session) {
        nextAction = NextAction.PING;
        setReadIdleTimeInMillis(session, pingDelay);
    }

    private void setReadIdleTimeInMillis(IoSessionEx session, long delay) {
        IoSessionConfigEx config = session.getConfig();
        if (logger.isTraceEnabled()) {
            logger.trace("WsCheckAliveFilter.setReadIdleTimeInMillis(" + delay + ")");
        }
        if (delay == 0L) {
            config.setIdleTimeInMillis(IdleStatus.READER_IDLE, 1L); // don't pass in 0, that disables idle timeout
        }
        else {
            config.setIdleTimeInMillis(IdleStatus.READER_IDLE, delay);
        }
    }

    void pingWritten(long currentTimeMillis) {
        pingSentTime = currentTimeMillis;
        if (logger.isTraceEnabled()) {
            logger.trace("WsCheckAliveFilter.pingWritten at time " + pingSentTime);
        }
    }

    private void writePing(NextFilter nextFilter, IoSessionEx session) throws Exception {
        WsPingMessage emptyPing = new WsPingMessage();
        setReadIdleTimeInMillis(session, maxExpectedRtt);
        nextAction = NextAction.PONG;
        if (logger.isTraceEnabled()) {
            logger.trace(String.format("Writing %s at time %d", emptyPing, System.currentTimeMillis()));
        }
        pingSentTime = System.currentTimeMillis();
        session.write(emptyPing);
    }

    // For unit test only
    void flipNextAction() {
        nextAction = nextAction == NextAction.PING ? NextAction.PONG : NextAction.PING;
    }

}
