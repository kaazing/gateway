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
package org.kaazing.gateway.service.proxy;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;
import org.kaazing.gateway.service.ServiceContext;
import org.kaazing.gateway.service.proxy.ServiceConnectManager.HeartbeatFilter;

class ConnectionPool {
    private static final AttributeKey CONNECT_FUTURE_KEY = new AttributeKey(ServiceConnectManager.class, "connectFutureKey");
    
    private final ServiceContext serviceContext;
    private final AbstractProxyHandler connectHandler;
    private final String connectURI;
    private final HeartbeatFilter heartbeatFilter;
    private final IoFutureListener<ConnectFuture> connectListener;
    private final int preparedConnectionCount;

    private final AtomicInteger currentPreparedConnectionCount;
    private final PreConnectFilter preConnectFilter;
    private final AtomicBoolean preConnectFlag;
    private final ConnectFutures connectFutures;
    private boolean active = false;

    /**
     * hearbeatFilter the only parameter that can be null
     */
    ConnectionPool(ServiceContext serviceContext, AbstractProxyHandler connectHandler, String connectURI, HeartbeatFilter heartbeatFilter,
            IoFutureListener<ConnectFuture> connectListener, int preparedConnectionCount, boolean isThreadAligned) {
        this.serviceContext = serviceContext;
        this.connectHandler = connectHandler;
        this.connectURI = connectURI;
        this.heartbeatFilter = heartbeatFilter;
        this.connectListener = connectListener;
        this.preparedConnectionCount = preparedConnectionCount;
        preConnectFlag = new AtomicBoolean(false);
        currentPreparedConnectionCount = new AtomicInteger(0);
        preConnectFilter = new PreConnectFilter(this);
        connectFutures = ConnectFutures.createConnectFutures(preparedConnectionCount, isThreadAligned);
    }

    void start() {
        resume();
        if (preparedConnectionCount > 0) {
            fillPreConnects();
        }
    }

    ConnectFuture getNextConnectFuture(final IoSessionInitializer<ConnectFuture> connectInitializer) {
        ConnectFuture future = connectFutures.pollFirstEntry();
        if (future == null) {
            future = doConnect(false, connectInitializer);
        } else {
            currentPreparedConnectionCount.decrementAndGet();

            // No longer a pre-connect, remove the pre-connect filter which is responsible for cleaning up zombie pre-connects
            IoSession connectSession = future.getSession();
            IoFilterChain filterChain = connectSession.getFilterChain();
            if (filterChain.contains("PreConnectFilter")) {
                filterChain.remove("PreConnectFilter");
                connectSession.removeAttribute(CONNECT_FUTURE_KEY);
            }

            if (connectInitializer != null) {
                connectInitializer.initializeSession(connectSession, future);
            }
        }
        fillPreConnects();
        return future;
    }

    private void fillPreConnects() {
        if (preConnectFlag.compareAndSet(false, true)) {
            if (currentPreparedConnectionCount.get() < preparedConnectionCount) {
                do {
                    doConnect(true, null);
                } while (isActive() && (currentPreparedConnectionCount.incrementAndGet() < preparedConnectionCount));
            }
            preConnectFlag.compareAndSet(true, false);
        }
    }

    private boolean isActive() {
        return active;
    }

    private void quiesce() {
        active = false;
    }

    private void resume() {
        active = true;
    }

    void remove(Object key) {
        if (key != null) {
            ConnectFuture future = connectFutures.remove(key);
            if (future != null) {
                // only decrement the connection count if a future was actually removed from the list
                currentPreparedConnectionCount.decrementAndGet();
            }
        }
    }

    private void decrementConnectionCount() {
        currentPreparedConnectionCount.decrementAndGet();
    }

    // Set the connect future key on the session so that if the session is closed we can remove
    // the associated ConnectFuture from the map of preconnect futures.
    private void addConnectFuture(ConnectFuture future) {
        Object key = connectFutures.add(future);
        future.getSession().setAttributeIfAbsent(CONNECT_FUTURE_KEY, key);
    }

    private ConnectFuture doConnect(final boolean preconnected, final IoSessionInitializer<ConnectFuture> connectInitializer) {
        ConnectFuture future = serviceContext.connect(connectURI, connectHandler, new IoSessionInitializer<ConnectFuture>() {
            @Override
            public void initializeSession(IoSession connectSession, ConnectFuture future) {
                if (heartbeatFilter != null) {
                    connectSession.getFilterChain().addLast("ServiceHeartbeat", heartbeatFilter);
                }
                connectSession.getFilterChain().addLast("PreConnectFilter", preConnectFilter);

                if (connectInitializer != null) {
                    connectInitializer.initializeSession(connectSession, future);
                }
            }
        });
        future.addListener(new IoFutureListener<ConnectFuture>() {
            @Override
            public void operationComplete(ConnectFuture future) {
                if ( future.isConnected() ) {
                    if (preconnected) {
                        // Add the future as an attribute so if the connection goes down it can be removed from the map.
                        addConnectFuture(future);
                    }
                }
                else {
                    if (preconnected) {
                        // The connection failed, quiesce the connect manager and decrement the connection count
                        quiesce();
                        decrementConnectionCount();
                    }
                }
                connectListener.operationComplete(future);
            }
        });
        return future;
    }

    static class PreConnectFilter extends IoFilterAdapter {
        private final ConnectionPool connectManager;

        private PreConnectFilter(ConnectionPool connectManager) {
            this.connectManager = connectManager;
        }

        @Override
        public void sessionClosed(NextFilter nextFilter, IoSession session) throws Exception {
            // if this is a pre-connected session, remove it from the ConnectManager's list
            connectManager.remove(session.getAttribute(CONNECT_FUTURE_KEY));

            super.sessionClosed(nextFilter, session);
        }
    }

    private static abstract class ConnectFutures {
        
        /**
         * @param future
         * @return  Key which must be used to remove the future using {@link remove}
         */
        abstract Object add(ConnectFuture future);

        abstract ConnectFuture pollFirstEntry();

        abstract ConnectFuture remove(Object key);

        private static ConnectFutures createConnectFutures(int preparedConnectionCount, boolean isThreadAligned) {
            return preparedConnectionCount == 0 
                    ? EMPTY_CONNECT_FUTURES
                    : !isThreadAligned ? new ThreadSafeConnectFutures(preparedConnectionCount) 
                                       : new ArrayWrapConnectFutures(preparedConnectionCount);
        }

        private static final ConnectFutures EMPTY_CONNECT_FUTURES = new ConnectFutures() {
            @Override
            Object add(ConnectFuture future) {
                return null;
            }
            @Override
            ConnectFuture pollFirstEntry() {
                return null;
            }
            @Override
            ConnectFuture remove(Object key) {
                return null;
            }
        };
    }

    private static class ThreadSafeConnectFutures extends ConnectFutures {
        private final ConcurrentSkipListMap<Long, ConnectFuture> connectFutures;
        AtomicLong nextFutureId = new AtomicLong(0);

        private ThreadSafeConnectFutures(int preparedConnectionCount) {
            connectFutures = new ConcurrentSkipListMap<>();
        }

        @Override
        ConnectFuture pollFirstEntry() {
            Entry<Long, ConnectFuture> entry = connectFutures.pollFirstEntry();
            return entry == null ? null : entry.getValue();
        }

        @Override
        ConnectFuture remove(Object key) {
            return connectFutures.remove(key);
        }

        @Override
        Object add(ConnectFuture future) {
            Long key = nextFutureId.getAndIncrement();
            connectFutures.put(key,  future);
            return key;
        }
    }

    /**
     * This implementation is not thread safe but is more efficient
     */
    private static class ArrayWrapConnectFutures extends ConnectFutures {
        private final ConnectFuture[] futures;
        private int firstIndex = 0;
        private int insertIndex = -1;

        private ArrayWrapConnectFutures(int maxConnections) {
            this.futures = new ConnectFuture[maxConnections];
        }

        @Override
        Integer add(ConnectFuture future) {
            insertIndex = ++insertIndex < futures.length ? insertIndex : 0;
            assert futures[insertIndex] == null : "preparing too many connections";
            futures[insertIndex] = future;
            if (futures[firstIndex] == null) {
                // All entries were removed, then we just added one
                firstIndex = insertIndex;
            }
            return insertIndex;
        }

        @Override
        ConnectFuture pollFirstEntry() {
            ConnectFuture result = futures[firstIndex];
            futures[firstIndex] = null;
            adjustFirstIndex();
            return result;
        }

        @Override
        ConnectFuture remove(Object key) {
            int index = (Integer)key;
            ConnectFuture removed = futures[index];
            futures[index] = null;
            adjustFirstIndex();
            return removed;
        }

        /**
         * Advance firstIndex to the oldest entry still present, if any
         */
        private void adjustFirstIndex() {
            for (int i=0; futures[firstIndex] == null && i<futures.length; i++) {
                firstIndex = ++firstIndex < futures.length ? firstIndex : 0;
            }
        }
    }
}