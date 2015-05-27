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

import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.kaazing.gateway.resource.address.http.HttpResourceAddress;
import org.kaazing.gateway.transport.TypedAttributeKey;
import org.kaazing.gateway.transport.http.bridge.filter.HttpFilterAdapter;
import org.kaazing.mina.core.session.IoSessionEx;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/*
 * A pool for reusable persistent transport connections. HttpConnector
 * may pick one of the transport connections instead of creating a new
 * one while connecting to the origin server.
 */
public class PersistentConnectionPool {

    private static final String IDLE_FILTER = HttpProtocol.NAME + "#idle";
    private static final TypedAttributeKey<HttpResourceAddress> SERVER_ADDRESS =
            new TypedAttributeKey<>(PersistentConnectionPool.class, "address");

    // transport address -> set of persistent connections
    private final ConcurrentMap<HttpResourceAddress, Set<IoSession>> connections;
    private final Logger logger;
    private final HttpConnectIdleFilter idleFilter;
    private final CloseListener closeListener;

    PersistentConnectionPool(Logger logger) {
        this.connections = new ConcurrentHashMap<>();
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
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Caching persistent connection: http address=%s, transport session=%s",
                    httpSession.getRemoteAddress().getResource(), httpSession.getParent()));
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
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Reusing cached persistent connection: http address=%s, transport session=%s",
                        serverAddress.getResource(), transportSession));
            }
            transportSession.getConfig().setBothIdleTime(0);
            transportSession.getFilterChain().remove(IDLE_FILTER);
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
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Removed cached persistent connection: http address=%s, transport session=%s",
                        serverAddress.getResource(), session));
            }
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
            session.getConfig().setBothIdleTime(0);
            session.getFilterChain().remove(IDLE_FILTER);

            // Transport connection will be removed from pool in an listener of CloseFuture
            session.close(false);
        }
    }

    private Set<IoSession> getTransportSessions(HttpResourceAddress serverAddress) {
        Set<IoSession> transportSessions = connections.get(serverAddress);
        if (transportSessions == null) {
            HashSet<IoSession> newTransportSessions = new HashSet<>();
            transportSessions = connections.putIfAbsent(serverAddress, newTransportSessions);
            if (transportSessions == null) {
                transportSessions = newTransportSessions;
            }
        }

        return transportSessions;
    }

    private boolean add(DefaultHttpSession httpSession) {
        HttpResourceAddress serverAddress = (HttpResourceAddress)httpSession.getRemoteAddress();
        int max = serverAddress.getOption(HttpResourceAddress.KEEP_ALIVE_MAX_CONNECTIONS);
        IoSession transportSession = httpSession.getParent();

        Set<IoSession> transportSessions = getTransportSessions(serverAddress);

        synchronized (transportSessions) {
            if (transportSessions.size() < max) {
                transportSessions.add(transportSession);
                return true;
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Not caching persistent connection: http address=%s, transport session=%s",
                    serverAddress.getResource(), transportSession));
        }
        return false;
    }

    private void remove(HttpResourceAddress serverAddress, IoSession session) {
        Set<IoSession> transportSessions = getTransportSessions(serverAddress);
        synchronized (transportSessions) {
            transportSessions.remove(session);
        }
    }

    private IoSession removeThreadAligned(HttpResourceAddress serverAddress) {
        Set<IoSession> transportSessions = getTransportSessions(serverAddress);

        synchronized (transportSessions) {
            Iterator<IoSession> it = transportSessions.iterator();
            while(it.hasNext()) {
                IoSessionEx session = (IoSessionEx)it.next();
                if (session.getIoThread() == Thread.currentThread()) {
                    it.remove();
                    return session;
                }
            }
        }
        return null;
    }

}
