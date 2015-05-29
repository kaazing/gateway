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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/*
 * A pool for reusable persistent transport connections. HttpConnector
 * may pick one of the transport connections instead of creating a new
 * one while connecting to the origin server.
 */
public class PersistentConnectionPool {

    private static final String IDLE_FILTER = HttpProtocol.NAME + "#idle";
    private static final TypedAttributeKey<HttpResourceAddress> SERVER_ADDRESS =
            new TypedAttributeKey<>(PersistentConnectionPool.class, "address");

    // server address -> set of persistent connections (per thread)
    private final ThreadLocal<Map<HttpResourceAddress, List<IoSession>>> connections;

    // No of connections per server across all threads
    private final Map<HttpResourceAddress, AtomicInteger> counts;


    private final Logger logger;
    private final HttpConnectIdleFilter idleFilter;
    private final CloseListener closeListener;

    PersistentConnectionPool(Logger logger) {
        this.connections = new VicariousThreadLocal<Map<HttpResourceAddress, List<IoSession>>>() {
            @Override
            protected  Map<HttpResourceAddress, List<IoSession>> initialValue() {
                return new HashMap<>();
            }
        };
        this.counts = new ConcurrentHashMap<>();
        this.logger = logger;
        this.idleFilter = new HttpConnectIdleFilter(logger);
        this.closeListener = new CloseListener(this, logger);
    }

    /*
     * Recycle existing transport session so that it can be used as a http
     * persistent connection
     *
     * @return true if the idle connection is cached for reuse
     *         false otherwise
     */
    public boolean recycle(DefaultHttpSession httpSession) {
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
    public IoSession take(HttpResourceAddress serverAddress) {
        IoSession transportSession = removeThreadAligned(serverAddress);
        if (transportSession != null) {
            transportSession.getConfig().setBothIdleTime(0);
            IoFilterChain filterChain = transportSession.getFilterChain();
            if (filterChain.contains(IDLE_FILTER)) {
                filterChain.remove(IDLE_FILTER);
            }
            CloseFuture closeFuture = transportSession.getCloseFuture();
            closeFuture.removeListener(closeListener);

            SERVER_ADDRESS.remove(transportSession);

        }

        return transportSession;
    }



    /*
     * If a session is closed, it will be removed from this pool using this
     * CloseFuture listener
     */
    private static class CloseListener implements IoFutureListener<CloseFuture> {

        private final PersistentConnectionPool store;
        private final Logger logger;

        CloseListener(PersistentConnectionPool store, Logger logger) {
            this.store = store;
            this.logger = logger;
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
                logger.debug(String.format("Idle cached persistent connection: transport session=%s", session));
            }

            // Transport connection will be removed from pool in an listener of CloseFuture
            session.close(false);
        }
    }

    /*
    private ConnectionPool getTransportSessions(HttpResourceAddress serverAddress) {
        if (serverAddress == null) {
            throw new RuntimeException("serverAddress cannot be null");
        }
        ConnectionPool transportSessions = connections.get(serverAddress);
        if (transportSessions == null) {
            ConnectionPool newTransportSessions = new ConnectionPool(serverAddress, logger);
            transportSessions = connections.putIfAbsent(serverAddress, newTransportSessions);
            if (transportSessions == null) {
                transportSessions = newTransportSessions;
            }
        }

        return transportSessions;
    }
*/

    private boolean add(DefaultHttpSession httpSession) {
        HttpResourceAddress serverAddress = (HttpResourceAddress)httpSession.getRemoteAddress();
        IoSession transportSession = httpSession.getParent();

        AtomicInteger current = counts.get(serverAddress);
        if (current == null) {
            AtomicInteger newCurrent = new AtomicInteger(0);
            current = counts.putIfAbsent(serverAddress, newCurrent);
            if (current == null) {
                current = newCurrent;
            }
        }

        int maxConnections = serverAddress.getOption(HttpResourceAddress.KEEP_ALIVE_MAX_CONNECTIONS);
        int currentCount = current.get();
        if (currentCount < maxConnections && current.compareAndSet(currentCount, currentCount+1)) {
            List<IoSession> pool = connections.get().get(serverAddress);
            if (pool == null) {
                pool = new ArrayList<>();
                connections.get().put(serverAddress, pool);
            }
            pool.add(transportSession);
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Caching persistent connection: server = %s session = %s pool = %d",
                        serverAddress.getResource(), transportSession, currentCount+1));
            }
            return true;
        }
        return false;
    }

    private void remove(HttpResourceAddress serverAddress, IoSession session) {
        List<IoSession> pool = connections.get().get(serverAddress);
        if (pool != null) {
            boolean removed = pool.remove(session);
            if (removed) {
                int count = counts.get(serverAddress).decrementAndGet();
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("Removing cached persistent connection: server = %s session = %s pool = %d",
                            serverAddress.getResource(), session, count));
                }
            }
        }
    }

    private IoSession removeThreadAligned(HttpResourceAddress serverAddress) {
        List<IoSession> pool = connections.get().get(serverAddress);
        if (pool != null && pool.size() > 0) {
            int count = counts.get(serverAddress).decrementAndGet();
            IoSession session = pool.remove(0);
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Reusing cached persistent connection: server = %s  session = %s pool = %d",
                        serverAddress.getResource(), session, count));
            }
            return session;
        }
        return null;
    }

    /*
    private static class ConnectionPool {
        private final int maxConnections;
        private final AtomicInteger size;
        private final ThreadLocal<Set<IoSession>> connections;
        private final Logger logger;
        private final HttpResourceAddress serverAddress;

        ConnectionPool(HttpResourceAddress serverAddress, Logger logger) {
            this.logger = logger;
            this.size = new AtomicInteger(0);
            this.maxConnections = serverAddress.getOption(HttpResourceAddress.KEEP_ALIVE_MAX_CONNECTIONS);
            this.serverAddress = serverAddress;

            connections = new VicariousThreadLocal<Set<IoSession>>() {
                @Override
                protected Set<IoSession> initialValue() {
                    return new HashSet<>();
                }

            };
        }

        boolean add(IoSession session) {
            int current = size.get();
            if (current < maxConnections && size.compareAndSet(current, current+1)) {
                connections.get().add(session);
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("Caching persistent connection: server = %s session = %s pool = %d",
                            serverAddress.getResource(), session, size.get()));
                }
                return true;
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("Not caching persistent connection: server = %s session = %s pool = %d",
                            serverAddress.getResource(), session, size.get()));
                }
            }
            return false;
        }

        void remove(IoSession session) {
            if (connections.get().remove(session)) {
                size.decrementAndGet();
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("Removing cached persistent connection: server = %s session = %s pool = %d",
                            serverAddress.getResource(), session, size.get()));
                }
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("Couldn't remove cached persistent connection: server = %s session = %s pool = %d",
                            serverAddress.getResource(), session, size.get()));
                }
            }
        }

        IoSession get() {
            if (connections.get().size() > 0) {
                Iterator<IoSession> it = connections.get().iterator();
                IoSession session = it.next();
                it.remove();
                size.decrementAndGet();
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("Reusing cached persistent connection: server = %s  session = %s pool = %d",
                            serverAddress.getResource(), session, size.get()));
                }
                return session;
            }
            return null;
        }

    }
    */

}
