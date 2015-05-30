/**
 * Copyright (c) 2007-2014 Kaazing Corporation. All rights reserved.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

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

    // No of connections per server across ALL threads
    private final Map<HttpResourceAddress, AtomicInteger> counts;


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
        this.counts = new ConcurrentHashMap<>();
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

        // Take care of transport session close
        CloseFuture closeFuture = transportSession.getCloseFuture();
        closeFuture.addListener(closeListener);

        // Track transport session idle
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

        // Initialize the global server connections count
        AtomicInteger current = counts.get(serverAddress);
        if (current == null) {
            AtomicInteger newCurrent = new AtomicInteger(0);
            current = counts.putIfAbsent(serverAddress, newCurrent);
            if (current == null) {
                current = newCurrent;
            }
        }

        // If there aren't enough connections cached, let us add this connection
        int maxConnections = serverAddress.getOption(HttpResourceAddress.KEEP_ALIVE_MAX_CONNECTIONS);
        int currentCount = current.get();
        if (currentCount < maxConnections && current.compareAndSet(currentCount, currentCount+1)) {
            connections.get().add(serverAddress, transportSession);
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Caching persistent connection: server = %s session = %s pool = %d",
                        serverAddress.getResource(), transportSession, currentCount+1));
            }
            return true;
        }
        return false;
    }

    private void remove(HttpResourceAddress serverAddress, IoSession session) {
        boolean removed = connections.get().remove(serverAddress, session);
        if (removed) {
            // Connection was in the pool, so decrement the global count of
            // server connections as it is out of the pool
            int count = counts.get(serverAddress).decrementAndGet();
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Removing cached persistent connection: server = %s session = %s pool = %d",
                        serverAddress.getResource(), session, count));
            }
        }

    }

    private IoSession removeThreadAligned(HttpResourceAddress serverAddress) {
        IoSession session = connections.get().removeAny(serverAddress);
        if (session != null) {
            // Connection was in the pool, so decrement the global count of
            // server connections as it is out of the pool
            int count = counts.get(serverAddress).decrementAndGet();
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Reusing cached persistent connection: server = %s  session = %s pool = %d",
                        serverAddress.getResource(), session, count));
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
        private final Map<HttpResourceAddress, List<IoSession>> addressToConnections;

        private ServerConnections() {
            this.addressToConnections = new HashMap<>();
        }

        /*
         * Caches the given persistent connection for the given server
         */
        private void add(HttpResourceAddress serverAddress, IoSession session) {
            List<IoSession> connections = addressToConnections.get(serverAddress);
            if (connections == null) {
                connections = new ArrayList<>();
                addressToConnections.put(serverAddress, connections);
            }
            connections.add(session);
        }

        /*
         * Removes given persistent connection for the given server
         */
        private boolean remove(HttpResourceAddress serverAddress, IoSession session) {
            List<IoSession> connections = addressToConnections.get(serverAddress);
            return (connections != null) && connections.remove(session);
        }

        /*
         * Returns a cached persistent connection for the server
         *
         * @return any IoSession for the server if available
         *         null otherwise
         */
        private IoSession removeAny(HttpResourceAddress serverAddress) {
            List<IoSession> connections = addressToConnections.get(serverAddress);
            return (connections == null || connections.isEmpty()) ? null : connections.remove(0);
        }
    }

}
