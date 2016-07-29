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

import static java.lang.String.format;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;
import org.jboss.netty.channel.socket.Worker;
import org.kaazing.gateway.service.ServiceContext;
import org.kaazing.gateway.transport.BridgeAcceptor;
import org.kaazing.gateway.transport.BridgeServiceFactory;
import org.kaazing.gateway.transport.Transport;
import org.kaazing.gateway.transport.TransportFactory;
import org.kaazing.gateway.transport.nio.internal.NioSocketAcceptor;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;
import org.kaazing.mina.netty.util.threadlocal.VicariousThreadLocal;
import org.slf4j.Logger;

/**
 * Object ownership and cardinalities  (1 to 1 if unspecified):
 * ServiceConnectManager (one per service, owned by the proxy service handler):
 * - connectHandler (AbstractProxyHandler)
 * - schedulerProvider
 * - ConnectManager
 *   - ConnectFutures
 *     - ConnectFuture (many)
 *       - ConnectListener (1 per ConnectFuture)
 * - HeartbeatFilter
 *   - ServiceHeartBeat
 *     - HeartbeatHandler
 *
 */
public final class ServiceConnectManager {
    private final ServiceContext serviceCtx;
    private final AbstractProxyHandler connectHandler;
    private final String connectURI;
    private final AtomicBoolean serviceConnected = new AtomicBoolean(true);
    private final SchedulerProvider schedulerProvider;

    // The heartbeat is a runnable task that is owned by the service connect manager.
    // We should only have at most one of these running at a given time.
    private ServiceHeartbeat heartbeat;

    private final Logger logger;
    private final int interval;
    private final NioSocketAcceptor tcpAcceptor;

    private HeartbeatFilter heartbeatFilter;
    private int preparedConnectionCount;

    // some statistics provided by the ServiceConnectManager
    private AtomicLong lastSuccessfulConnectTime = new AtomicLong(0);
    private AtomicLong lastFailedConnectTime = new AtomicLong(0);
    private AtomicBoolean heartbeatPingResult = new AtomicBoolean(false);
    private long heartbeatPingTimestamp = 0;
    private AtomicInteger heartbeatPingCount = new AtomicInteger(0);
    private AtomicInteger heartbeatPingSuccesses = new AtomicInteger(0);
    private AtomicInteger heartbeatPingFailures = new AtomicInteger(0);

    private final ThreadLocal<ConnectionPool> connectionPool = new VicariousThreadLocal<>();

    public ServiceConnectManager(ServiceContext service,
                                 AbstractProxyHandler connectHandler,
                                 BridgeServiceFactory bridgeServiceFactory,
                                 String connectURI,
                                 int interval,
                                 final int preparedConnectionCount) {
        this.serviceCtx = service;
        this.connectHandler = connectHandler;
        this.connectURI = connectURI;
        this.schedulerProvider = service.getSchedulerProvider();
        this.logger = service.getLogger();
        this.interval = interval;

        TransportFactory transportFactory = bridgeServiceFactory.getTransportFactory();
        Transport tcp = transportFactory.getTransport("tcp");
        assert tcp != null;
        BridgeAcceptor bridgeAcceptor = tcp.getAcceptor();
        assert bridgeAcceptor instanceof NioSocketAcceptor;
        tcpAcceptor = (NioSocketAcceptor) bridgeAcceptor;

        // Make sure prepared.connection.count is at least the number of I/O threads, so there will always
        // a thread-aligned connection available. This is needed for the reverse connectivity, because in that case
        // if we create the connection lazily we cannot guaranteed it will be on the same I/O thread (because reverse
        // connections are pre-established when the other end connects). If prepare.connection.count is 0 then we must
        // leave it as such to avoid breaking stomp.proxy service and broadcast service (which explicitly pre-establish
        // their backend connections themselves and don't need any more connections than that prepared in advance).
        int workerCount = service.getProcessorCount();
        assert workerCount > 0;
        this.preparedConnectionCount = preparedConnectionCount;
        if ( preparedConnectionCount > 0 && preparedConnectionCount < workerCount) {
            this.preparedConnectionCount = workerCount;
            if (logger.isWarnEnabled()) {
                logger.warn(String.format(
                        "Configured prepared.connection.count %d for %s service has been increased to number of IO threads %d for extra efficiency",
                        preparedConnectionCount, serviceCtx.getServiceType(), workerCount));
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("%s service with thread alignment, using prepared.connection.count=%d",
                              serviceCtx.getServiceType(), preparedConnectionCount));
        }

        if (interval > 0) {
            heartbeatFilter = new HeartbeatFilter(interval);
        } else {
            heartbeatFilter = null;
        }
    }

    // Start the connection manager so that any pre-connections are established.
    public void start() {
        IoFutureListener<ConnectFuture> tmpConnectListener;
        if (interval > 0) {
            tmpConnectListener = new IoFutureListener<ConnectFuture>() {
                @Override
                public void operationComplete(ConnectFuture future) {
                    heartbeatFilter.setServiceConnected(future.isConnected());
                    updateConnectTimes(future.isConnected());
                }
            };
        } else {
            tmpConnectListener = new IoFutureListener<ConnectFuture>() {
                @Override
                public void operationComplete(ConnectFuture future) {
                    updateConnectTimes(future.isConnected());
                }
            };
        }
        final IoFutureListener<ConnectFuture> connectListener = tmpConnectListener;

        // set a connection pool with GT 0 prepared connections in every worker thread as an optimization
        Worker[] workers = tcpAcceptor.getWorkers();
        assert preparedConnectionCount == 0 || preparedConnectionCount >= workers.length : "Prepared connection count must be 0, or >= number of IO threads";
        int minCountPerThread = preparedConnectionCount / workers.length;
        int remainder = preparedConnectionCount % workers.length;
        for (Worker worker : workers) {
            final int count = remainder-- > 0 ? minCountPerThread + 1 : minCountPerThread;
            Runnable startConnectionPoolTask = () -> {
                ConnectionPool currentPool = connectionPool.get();
                if (currentPool == null) {
                    // the first time the pool is started is needs to be created, subsequent times it should just be started
                    // without re-creating.
                    currentPool = new ConnectionPool(serviceCtx, connectHandler, connectURI, heartbeatFilter,
                            connectListener, count, true);
                    connectionPool.set(currentPool);
                }
                currentPool.start();
            };
            worker.executeInIoThread(startConnectionPoolTask);
        }
    }

    public ConnectFuture getNextConnectFuture(final IoSessionInitializer<ConnectFuture> connectInitializer) {
        ConnectionPool pool = connectionPool.get();
        if (pool == null) {
            FutureTask<ConnectFuture> delegateGetConnectFuture = new FutureTask<>(new Callable<ConnectFuture>() {

                @Override
                public ConnectFuture call() {
                    ConnectionPool delegatePool = connectionPool.get();
                    if (delegatePool != null) {
                        return delegatePool.getNextConnectFuture(connectInitializer);
                    }
                    return null;
                }
            });

            // try to reschedule on an IO Thread
            Worker[] workers = tcpAcceptor.getWorkers();
            int randomWorker = (int)(Math.random() * workers.length);
            workers[randomWorker].executeInIoThread(delegateGetConnectFuture);
            try {
                return delegateGetConnectFuture.get();
            } catch (ExecutionException executionEX) {
                if (logger.isDebugEnabled()) {
                    logger.debug(format("Failed to get connectFuture to %s from delegate connection pool due to exception", connectURI), executionEX);
                } else {
                    logger.warn(format("Failed to get ConnectFuture to %s from delegate connection pool due to exception %s", connectURI, executionEX));
                }
                return null;
            } catch (InterruptedException interruptedEx) {
                if (logger.isDebugEnabled()) {
                    logger.debug(format("Failed to get connectFuture to %s from delegate connection pool due to exception", connectURI), interruptedEx);
                } else {
                    logger.warn(format("Failed to get ConnectFuture to %s from delegate connection pool due to exception %s", connectURI, interruptedEx));
                }
                return null;
            }
        }
        return pool.getNextConnectFuture(connectInitializer);
    }

    // for unit test use
    int getPreparedConnectionCount() {
        return preparedConnectionCount;
    }

    /**
     * A single instance of this filter is set on every outgoing connection. It is in charge of making sure
     * we periodically establish a connection (and close it immediately) if there are no (permanent)
     * connections open.
     */
    class HeartbeatFilter extends IoFilterAdapter {
        private final AtomicInteger sessionCount = new AtomicInteger(0);
        private final IoFutureListener<ConnectFuture> connectListener = new IoFutureListener<ConnectFuture>() {
            @Override
            public void operationComplete(ConnectFuture future) {
                setServiceConnected(future.isConnected(), true);
            }
        };

        private HeartbeatFilter(int interval) {
            heartbeat = new ServiceHeartbeat(interval);
            heartbeat.schedule(interval);
        }

        @Override
        public void sessionClosed(NextFilter nextFilter, IoSession session) throws Exception {
            if (sessionCount.decrementAndGet() == 0) {
                // the last session is being closed, start the heartbeat again
                heartbeat.schedule();
            }
            super.sessionClosed(nextFilter, session);
        }

        @Override
        public void sessionOpened(NextFilter nextFilter, IoSession session) throws Exception {
            if (sessionCount.getAndIncrement() == 0) {
                // there is at least one session now, cancel the heartbeat
                if (logger.isTraceEnabled()) {
                    logger.trace(format("First session connected to service, cancelling heartbeat for %s", connectURI));
                }
                cancelHeartbeat();
            }

            super.sessionOpened(nextFilter, session);
        }

        /**
         * @return A Connect future listener to be used only on heartbeat connects
         */
        private IoFutureListener<ConnectFuture> getConnectListener() {
            return connectListener;
        }

        void cancelHeartbeat() {
            heartbeat.cancel();
        }

        void setServiceConnected(boolean successfullyConnected) {
            setServiceConnected(successfullyConnected, false);
        }

        /**
         * This method is called whenever we connect (preconnect, regular connect or heartbeat)
         */
        void setServiceConnected(boolean successfullyConnected, boolean isHeartbeat) {
            if (successfullyConnected) {
                heartbeatPingSuccesses.incrementAndGet();
                heartbeatPingResult.set(true);
            }
            else {
                heartbeatPingFailures.incrementAndGet();
                heartbeatPingResult.set(false);
            }
            boolean changedConnectionState = serviceConnected.compareAndSet(!successfullyConnected, successfullyConnected);

            if (changedConnectionState) {
                if (!successfullyConnected) {
                    // if the connection state was changed to disconnected, then unbind the service and start the heartbeat
                    try {
                        if (logger.isInfoEnabled()) {
                            logger.info(format("Quiescing service with connect uri '%s'.", connectURI));
                        }
                        serviceCtx.getService().quiesce();
                        if ( logger.isTraceEnabled() ) {
                            logger.trace(format("Quiesced service with connect uri '%s'.", connectURI));
                        }

                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                    heartbeat.schedule();
                } else {
                    // Are there active connections?  If so, no need to keep the heartbeat running.
                    if (sessionCount.get() > 0) {
                        heartbeat.cancel();
                    }

                    // if the connection state was changed to connected, then rebind the service
                    try {
                        if (logger.isInfoEnabled()) {
                            logger.info(format("Starting service with connect uri '%s'.", connectURI));
                        }
                        serviceCtx.getService().start();
                        if ( logger.isTraceEnabled() ) {
                            logger.trace(format("Started service with connect uri '%s'.", connectURI));
                        }
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }
            } else if (successfullyConnected && isHeartbeat) {
                ServiceConnectManager.this.start();
            }
        }
    }

    private class ServiceHeartbeat implements Runnable {
        private final int interval;
        private final AtomicReference<ScheduledFuture<?>> heartbeatTask;
        private final HeartbeatHandler handler;
        private final AtomicInteger nextDelay;
        private final ScheduledExecutorService executor;

        private ServiceHeartbeat(int interval) {
            this.interval = interval;
            handler = new HeartbeatHandler();
            heartbeatTask = new AtomicReference<>();
            nextDelay = new AtomicInteger(interval);
            executor = ServiceConnectManager.this.schedulerProvider.getScheduler("ServiceHeartbeat", false);
        }

        // Schedule the heartbeat immediately.
        private void schedule() {
            schedule(0);
        }

        // Schedule the heartbeat to start after the given delay (in seconds).  If the heartbeat is already started,
        // this is a no-op.
        private synchronized void schedule(int delay) {
            ScheduledFuture<?> future = heartbeatTask.get();

            if (future != null) {
                return;
            }

            if (delay >= interval) {
                delay = interval;
            }

            if (executor.isShutdown() || executor.isTerminated()) {
                return;
            }

            future = executor.schedule(this, delay, TimeUnit.SECONDS);

            // There is only one thread starting the heartbeat task since this method is synchronized, so
            // just set the heartbeat task to the new future.
            heartbeatTask.set(future);
        }

        private synchronized void cancel() {
            ScheduledFuture<?> currentHeartbeatTask = heartbeatTask.getAndSet(null);

            // if heartbeat is running...cancel the task
            if ( currentHeartbeatTask != null ) {
                currentHeartbeatTask.cancel(false);
            }

            // reset the delay so that the next time the heartbeat runs it'll start quickly
            // and back off to the maximum recovery interval
            if (logger.isTraceEnabled()) {
                logger.trace(format("ServiceHeartBeat.cancel (%s): set nextDelay to 0", connectURI));
            }
            nextDelay.set(0);
        }

        @Override
        public synchronized void run() {
            ScheduledFuture<?> currentHeartbeatTask = heartbeatTask.get();

            // If a client connected to the service as this method was executing, then it's possible that the heartbeat
            // was canceled and the task reference will be null, in which case we don't bother executing the heartbeat.
            if (currentHeartbeatTask != null) {
                if (logger.isTraceEnabled()) {
                    logger.trace(format("ServiceHeartBeat.run: Current Heartbeat task is "+currentHeartbeatTask+"; starting to heartbeat-connect to %s", connectURI));
                }
                ConnectFuture connectFuture = null;
                try {
                    connectFuture = serviceCtx.connect(connectURI, handler, null);
                    connectFuture.addListener(heartbeatFilter.getConnectListener()); // dummy connection
                } catch (Exception ex) {
                    if (logger.isDebugEnabled()) {
                        logger.debug(format("ServiceHeartBeat.run: exception connecting to uri %s", connectURI), ex);
                    } else {
                        logger.info(format("ServiceHeartBeat.run: exception connecting to uri %s, %s", connectURI, ex));
                    }
                }

                // the heartbeat does an exponential backoff trying to connect to the backend service,
                // so if the maximum interval hasn't been reached, reschedule the task with the new delay
                int delay = nextDelay.get();
                // this thread is the one who scheduled the heartbeat, so update the next delay accordingly
                if (delay == 0) {
                    nextDelay.compareAndSet(0, 1);
                } else if ( delay < interval ) {
                    nextDelay.compareAndSet(delay, delay * 2);
                } else {
                    // delay >= interval therefore we will cap it at interval.
                    // so do nothing - don't increase the nextDelay here.
                    // when scheduled we will use the interval to delay.
                }

                // Only reschedule the heartbeat if it hasn't been canceled.  If a client connected
                // to the service as this method was executing, then it's possible that the heartbeat
                // was canceled and the task reference will be null, in which case we don't bother scheduling.
                if (heartbeatTask.compareAndSet(currentHeartbeatTask, null)) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("ServiceHeartBeat.run adding listener to connect future to reschedule task");
                    }
                    connectFuture.addListener(new IoFutureListener<ConnectFuture>() {
                        @Override
                        public void operationComplete(ConnectFuture future) {
                            if (logger.isTraceEnabled()) {
                                logger.trace(format("ServiceHeartBeat.run.connectFuture.operationComplete (%s): nextDelay is %d", connectURI, nextDelay.get()));
                            }

                            Throwable t = future.getException();
                            if ((t != null) && (t instanceof IllegalStateException)) {
                                // do not reschedule the heartbeatTask as the connector is being shut down
                                if (logger.isTraceEnabled()) {
                                    logger.trace("ServiceHeartBeat.run.connectFuture.Completed: not rescheduling as connector is being shut down.", t);
                                }
                                return;
                            }

                            if ( nextDelay.get() != 0 ) {
                                schedule(nextDelay.get());
                            }
                        }
                    });
                }
                if (logger.isTraceEnabled()) {
                    logger.trace("ServiceHeartBeat.run finished executing heartbeat");
                }

            }
        }
    }

    private class HeartbeatHandler extends IoHandlerAdapter {
        @Override
        public void sessionOpened(IoSession session) throws Exception {
            session.close(false);

            // The service is bound on the front end, and the heartbeat detected that the backend can be successfully pinged.
            // It's possible that the heartbeat kicked in due to some spurious failure, or it's possible that the Gateway
            // is connecting to another Gateway that in turn lost connectivity to it's backend service (see KG-9924).  In
            // either case the service might not have been quiesced then started again, which prevents the pre-connects (aka
            // prepared connections) from being fulfilled.  Give the ServiceConnectManager a chance to fulfill pre-connects
            // now, and if there are no pre-connects or they are already fulfilled then this will be a no-op.
            ServiceConnectManager.this.start();
        }
    }

    // ----------------------------------------------------
    //    The data exposed by the ServiceConnectManager
    // ----------------------------------------------------

    public long getLastSuccessfulConnectTime() {
        return lastSuccessfulConnectTime.get();
    }

    public long getLastFailedConnectTime() {
        return lastFailedConnectTime.get();
    }

    public boolean getLastHeartbeatPingResult() {
        return heartbeatPingResult.get();
    }

    public long getLastHeartbeatPingTimestamp() {
        return heartbeatPingTimestamp;
    }

    public int getHeartbeatPingCount() {
        return heartbeatPingCount.get();
    }

    public int getHeartbeatPingSuccessesCount() {
        return heartbeatPingSuccesses.get();
    }

    public int getHeartbeatPingFailuresCount() {
        return heartbeatPingFailures.get();
    }

    public boolean isServiceConnected() {
        return serviceConnected.get();
    }

    public boolean isHeartbeatRunning() {
        if (heartbeat == null) {
            return false;
        }

        return heartbeat.heartbeatTask.get() != null;
    }

    private void updateConnectTimes(boolean connected) {
        if (connected) {
            lastSuccessfulConnectTime.set(System.currentTimeMillis());
        }
        else {
            lastFailedConnectTime.set(System.currentTimeMillis());
        }
    }

    // -----------------------------------------------------------
    //    End of the data exposed by the ServiceConnectManager
    // -----------------------------------------------------------

}
