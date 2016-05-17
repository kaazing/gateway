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
package org.kaazing.gateway.transport.http;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.kaazing.gateway.resource.address.http.HttpResourceAddress;
import org.kaazing.gateway.transport.TypedAttributeKey;
import org.kaazing.gateway.transport.http.bridge.filter.HttpFilterAdapter;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.mina.netty.util.threadlocal.VicariousThreadLocal;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/*
 * A pool for reusable persistent transport connections. HttpConnector
 * may pick one of the transport connections instead of creating a new
 * one while connecting to the origin server.
 */
class PersistentConnectionPool {

    private static final String IDLE_FILTER = HttpProtocol.NAME + "#idle";
    private static final TypedAttributeKey<HttpResourceAddress> SERVER_ADDRESS =
            new TypedAttributeKey<>(PersistentConnectionPool.class, "address");

    // server address -> set of persistent connections (per thread).
    // Using ThreadLocal for the following reasons:
    // - we need to return thread-aligned IoSession
    // - it reduces contention compared to synchronized ServerConnections
    private final ThreadLocal<ServerConnections> connections;

    private final Logger logger;
    private final HttpConnectIdleFilter idleFilter;
    private final CloseListener closeListener;

    PersistentConnectionPool(Logger logger) {
        this.connections = new VicariousThreadLocal<ServerConnections>() {
            @Override
            protected  ServerConnections initialValue() {
                return new ServerConnections();
            }
        };
        this.logger = logger;
        this.idleFilter = new HttpConnectIdleFilter(logger);
        this.closeListener = new CloseListener(this);
    }

    /*
     * Cache existing transport session so that it can be used for future
     * connect requests to server
     *
     * @return true if the idle connection is cached for reuse
     *         false otherwise
     */
    boolean recycle(DefaultHttpSession httpSession) {
        if (!add(httpSession)) {
            return false;
        }

        HttpResourceAddress serverAddress = (HttpResourceAddress)httpSession.getRemoteAddress();
        IoSession transportSession = httpSession.getParent();

        SERVER_ADDRESS.set(transportSession, serverAddress);

        // Connection needs to be removed from pool upon session's close. Adding
        // a close future listener for that
        CloseFuture closeFuture = transportSession.getCloseFuture();
        closeFuture.addListener(closeListener);

        // Track transport session idle so that we don't waste resources.
        // Upstream server can take new fresh connections if gateway closes idle connections
        transportSession.getFilterChain().addLast(IDLE_FILTER, idleFilter);
        int keepAliveTimeout = serverAddress.getOption(HttpResourceAddress.KEEP_ALIVE_TIMEOUT);
        transportSession.getConfig().setBothIdleTime(keepAliveTimeout);

        return true;
    }

    /*
     * Returns an existing transport session for the resource address that can be reused
     *
     * @return a reusable IoSession for the address
     *         otherwise null
     */
    IoSession take(HttpResourceAddress serverAddress) {
        IoSession transportSession = removeThreadAligned(serverAddress);
        if (transportSession != null) {
            // Got a cached persistent connection

            // Remove session idle tracking for this session
            transportSession.getConfig().setBothIdleTime(0);
            IoFilterChain filterChain = transportSession.getFilterChain();
            if (filterChain.contains(IDLE_FILTER)) {
                filterChain.remove(IDLE_FILTER);
            }

            // Remove our CloseFuture listener as it is out of pool
            CloseFuture closeFuture = transportSession.getCloseFuture();
            closeFuture.removeListener(closeListener);

            SERVER_ADDRESS.remove(transportSession);
        }

        return transportSession;
    }

    private boolean add(DefaultHttpSession httpSession) {
        HttpResourceAddress serverAddress = (HttpResourceAddress)httpSession.getRemoteAddress();
        IoSession transportSession = httpSession.getParent();


        ServerConnections serverConnections = connections.get();
        boolean cached = serverConnections.add(serverAddress, transportSession);
        if (cached) {
            if (logger.isDebugEnabled()) {
                int cachedConnections = serverConnections.cachedConnections(serverAddress);
                logger.debug(String.format("Caching persistent connection: server = %s session = %s pool = %d",
                        serverAddress.getResource(), transportSession, cachedConnections));
            }
            return true;
        } else {
            if (logger.isDebugEnabled()) {
                int cachedConnections = serverConnections.cachedConnections(serverAddress);
                logger.debug(String.format("NOT caching persistent connection: server = %s session = %s pool = %d",
                        serverAddress.getResource(), transportSession, cachedConnections));
            }
        }
        return false;
    }

    private void remove(HttpResourceAddress serverAddress, IoSession session) {
        ServerConnections serverConnections = connections.get();
        boolean removed = serverConnections.remove(serverAddress, session);
        if (removed) {
            if (logger.isDebugEnabled()) {
                int cachedConnections = serverConnections.cachedConnections(serverAddress);
                logger.debug(String.format("Removing cached persistent connection: server = %s session = %s pool = %d",
                        serverAddress.getResource(), session, cachedConnections));
            }
        }
    }

    private IoSession removeThreadAligned(HttpResourceAddress serverAddress) {
        ServerConnections serverConnections = connections.get();
        IoSession session = serverConnections.removeAny(serverAddress);
        if (session != null) {
            // Connection was in the pool
            if (logger.isDebugEnabled()) {
                int count = serverConnections.cachedConnections(serverAddress);
                logger.debug(String.format("Reusing cached persistent connection: server = %s  session = %s pool = %d",
                        serverAddress.getResource(), session, count));
            }
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Cache miss - NO cached persistent connection: server = %s",
                        serverAddress.getResource()));
            }
        }
        return session;
    }

    /*
     * If a session is closed, it will be removed from this pool using this
     * CloseFuture listener
     */
    private static class CloseListener implements IoFutureListener<CloseFuture> {

        private final PersistentConnectionPool store;

        CloseListener(PersistentConnectionPool store) {
            this.store = store;
        }

        @Override
        public void operationComplete(CloseFuture future) {
            IoSessionEx session = (IoSessionEx) future.getSession();
            HttpResourceAddress serverAddress = SERVER_ADDRESS.get(session);
            store.remove(serverAddress, session);
        }
    }

    /*
     * Filter to detect if a persistent connection is idle
     */
    private static class HttpConnectIdleFilter extends HttpFilterAdapter<IoSessionEx> {
        private final Logger logger;

        HttpConnectIdleFilter(Logger logger) {
            this.logger = logger;
        }

        @Override
        public void sessionIdle(NextFilter nextFilter, IoSession session, IdleStatus status) throws Exception {
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Idle cached persistent connection: session=%s", session));
            }

            // Transport connection will be removed from pool in an listener of CloseFuture
            session.close(false);
        }
    }


    /**
     * Keeps track of presistent connections to a server. Implemenation is not thread-safe
     * and the caller will take care of thread safety.
     */
    private static class ServerConnections {
        // IoSession[] limits the number of connections as per config
        // null means empty spot, non null means already cached connection
        private final Map<HttpResourceAddress, IoSession[]> addressToConnections;

        private ServerConnections() {
            this.addressToConnections = new HashMap<>();
        }

        /*
         * Caches the given persistent connection for the given server
         * @return true if it is cached
         *         false otherwise
         */
        private boolean add(HttpResourceAddress serverAddress, IoSession session) {
            int maxConnections = serverAddress.getOption(HttpResourceAddress.KEEP_ALIVE_CONNECTIONS);
            IoSession[] connections = addressToConnections.get(serverAddress);
            if (connections == null) {
                connections = new IoSession[maxConnections];
                addressToConnections.put(serverAddress, connections);
            }

            assert maxConnections == connections.length;
            for(int i=0; i < connections.length; i++) {
                if (connections[i] == null) {
                    connections[i] = session;
                    return true;
                }
            }
            return false;
        }

        /*
         * Removes given persistent connection for the given server
         * @return true if the session is removed from the pool
         *         false otherwise
         */
        private boolean remove(HttpResourceAddress serverAddress, IoSession session) {
            IoSession[] connections = addressToConnections.get(serverAddress);
            if (connections != null) {
                for(int i=0; i < connections.length; i++) {
                    if (connections[i] == session) {
                        connections[i] = null;
                        return true;
                    }
                }
            }
            return false;
        }

        /*
         * Returns a cached persistent connection for the server
         *
         * @return any IoSession for the server if available
         *         null otherwise
         */
        private IoSession removeAny(HttpResourceAddress serverAddress) {
            IoSession[] connections = addressToConnections.get(serverAddress);
            if (connections != null) {
                for(int i=0; i < connections.length; i++) {
                    if (connections[i] != null) {
                        IoSession session = connections[i];
                        connections[i] = null;
                        return session;
                    }
                }
            }
            return null;
        }

        /*
         * Returns the current number of cached persistent connection for the server
         *
         * @return the current number of connections for the server
         *         0 if there no caching for the server
         */
        private int cachedConnections(HttpResourceAddress serverAddress) {
            IoSession[] connections = addressToConnections.get(serverAddress);
            if (connections != null) {
                return (int) Arrays.stream(connections).filter(Objects::nonNull).count();
            }

            return 0;
        }
    }

}
