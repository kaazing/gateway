/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.core.service;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.core.IoUtil;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.filterchain.IoFilterChainBuilder;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.DefaultIoFuture;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoService;
import org.apache.mina.core.service.IoServiceListener;
import org.apache.mina.core.service.IoServiceListenerSupport;
import org.apache.mina.core.service.IoServiceStatistics;
import org.apache.mina.core.session.DefaultIoSessionDataStructureFactory;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionConfig;
import org.apache.mina.core.session.IoSessionDataStructureFactory;
import org.apache.mina.core.session.IoSessionInitializationException;
import org.apache.mina.core.session.IoSessionInitializer;
import org.apache.mina.util.ExceptionMonitor;
import org.apache.mina.util.NamePreservingRunnable;

import com.kaazing.mina.core.filterchain.DefaultIoFilterChain;
import com.kaazing.mina.core.session.AbstractIoSession;
import com.kaazing.mina.core.session.IoSessionConfigEx;

/**
 * Base implementation of {@link IoService}s.
 * 
 * An instance of IoService contains an Executor which will handle the incoming
 * events.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
/* This class originates from Mina.
 * The following changes were made from the version in Mina 2.0.0-RC1g:
 * 1. Change package name
 * 2. Use our versions of AbstractIoSession and DefaultIoFilterChain 
 * 3. Do not maintain stats like last IO times, scheduled write messages and scheduled write bytes 
 *    (presumably for performance reasons)
 * 4. Capture strongly-typed IoSessionConfigEx session configuration
*/
public abstract class AbstractIoService implements IoServiceEx {
    /** 
     * The unique number identifying the Service. It's incremented
     * for each new IoService created.
     */
    private static final AtomicInteger id = new AtomicInteger();

    /** 
     * The thread name built from the IoService inherited 
     * instance class name and the IoService Id 
     **/
    private final String threadName;

    /**
     * The associated executor, responsible for handling execution of I/O events.
     */
    private final Executor executor;

    /**
     * A flag used to indicate that the local executor has been created
     * inside this instance, and not passed by a caller.
     * 
     * If the executor is locally created, then it will be an instance
     * of the ThreadPoolExecutor class.
     */
    private final boolean createdExecutor;

    /**
     * The IoHandler in charge of managing all the I/O Events. It is 
     */
    private IoHandler handler;

    /**
     * The default {@link IoSessionConfig} which will be used to configure new sessions.
     */
    private final IoSessionConfigEx sessionConfig;

    private final IoServiceListener serviceActivationListener = new IoServiceListener() {
        public void serviceActivated(IoService service) {
            // Update lastIoTime.
//            AbstractIoService s = (AbstractIoService) service;
//            IoServiceStatistics _stats = s.getStatistics();
//            _stats.setLastReadTime(s.getActivationTime());
//            _stats.setLastWriteTime(s.getActivationTime());
//            _stats.setLastThroughputCalculationTime(s.getActivationTime());

        }

        public void serviceDeactivated(IoService service) {
            // Empty handler
        }

        public void serviceIdle(IoService service, IdleStatus idleStatus) {
            // Empty handler
        }

        public void sessionCreated(IoSession session) {
            // Empty handler
        }

        public void sessionDestroyed(IoSession session) {
            // Empty handler
        }
    };

    /**
     * Current filter chain builder.
     */
    private IoFilterChainBuilder filterChainBuilder = new DefaultIoFilterChainBuilder();

    private IoSessionDataStructureFactory sessionDataStructureFactory = new DefaultIoSessionDataStructureFactory();

    /**
     * Maintains the {@link IoServiceListener}s of this service.
     */
    private final IoServiceListenerSupport listeners;

    /**
     * A lock object which must be acquired when related resources are
     * destroyed.
     */
    protected final Object disposalLock = new Object();

    private volatile boolean disposing;

    private volatile boolean disposed;

    private IoFuture disposalFuture;

    /**
     * {@inheritDoc}
     */
    private IoServiceStatistics stats = null; //new IoServiceStatistics(this);
    

    /**
     * Constructor for {@link AbstractIoService}. You need to provide a default
     * session configuration and an {@link Executor} for handling I/O events. If
     * a null {@link Executor} is provided, a default one will be created using
     * {@link Executors#newCachedThreadPool()}.
     * 
     * @param sessionConfig
     *            the default configuration for the managed {@link IoSession}
     * @param executor
     *            the {@link Executor} used for handling execution of I/O
     *            events. Can be <code>null</code>.
     */
    protected AbstractIoService(IoSessionConfigEx sessionConfig, Executor executor) {
        if (sessionConfig == null) {
            throw new NullPointerException("sessionConfig");
        }

        if (getTransportMetadata() == null) {
            throw new NullPointerException("TransportMetadata");
        }

        if (!getTransportMetadata().getSessionConfigType().isAssignableFrom(
                sessionConfig.getClass())) {
            throw new IllegalArgumentException("sessionConfig type: "
                    + sessionConfig.getClass() + " (expected: "
                    + getTransportMetadata().getSessionConfigType() + ")");
        }

        // Create the listeners, and add a first listener : a activation listener
        // for this service, which will give information on the service state.
        listeners = new IoServiceListenerSupport(this);
        listeners.add(serviceActivationListener);

        // Stores the given session configuration
        this.sessionConfig = sessionConfig;

        // Make JVM load the exception monitor before some transports
        // change the thread context class loader.
        ExceptionMonitor.getInstance();

        if (executor == null) {
            this.executor = Executors.newCachedThreadPool();
            createdExecutor = true;
        } else {
            this.executor = executor;
            createdExecutor = false;
        }

        threadName = getClass().getSimpleName() + '-' + id.incrementAndGet();
    }

    /**
     * {@inheritDoc}
     */
    public final IoFilterChainBuilder getFilterChainBuilder() {
        return filterChainBuilder;
    }

    /**
     * {@inheritDoc}
     */
    public final void setFilterChainBuilder(IoFilterChainBuilder builder) {
        if (builder == null) {
            builder = new DefaultIoFilterChainBuilder();
        }
        filterChainBuilder = builder;
    }

    /**
     * {@inheritDoc}
     */
    public final DefaultIoFilterChainBuilder getFilterChain() {
        if (filterChainBuilder instanceof DefaultIoFilterChainBuilder) {
            return (DefaultIoFilterChainBuilder) filterChainBuilder;
        }
        
        
        throw new IllegalStateException(
                    "Current filter chain builder is not a DefaultIoFilterChainBuilder.");
    }

    /**
     * {@inheritDoc}
     */
    public final void addListener(IoServiceListener listener) {
        listeners.add(listener);
    }

    /**
     * {@inheritDoc}
     */
    public final void removeListener(IoServiceListener listener) {
        listeners.remove(listener);
    }

    /**
     * {@inheritDoc}
     */
    public final boolean isActive() {
        return listeners.isActive();
    }

    /**
     * {@inheritDoc}
     */
    public final boolean isDisposing() {
        return disposing;
    }

    /**
     * {@inheritDoc}
     */
    public final boolean isDisposed() {
        return disposed;
    }

    /**
     * {@inheritDoc}
     */
    public final void dispose() {
        if (disposed) {
            return;
        }

        IoFuture disposalFuture;
        synchronized (disposalLock) {
            disposalFuture = this.disposalFuture;
            if (!disposing) {
                disposing = true;
                try {
                    this.disposalFuture = disposalFuture = dispose0();
                } catch (Exception e) {
                    ExceptionMonitor.getInstance().exceptionCaught(e);
                } finally {
                    if (disposalFuture == null) {
                        disposed = true;
                    }
                }
            }
        }
        
        if (disposalFuture != null) {
            disposalFuture.awaitUninterruptibly();
        }

        if (createdExecutor) {
            ExecutorService e = (ExecutorService) executor;
            e.shutdown();
            while (!e.isTerminated()) {
                try {
                    e.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
                } catch (InterruptedException e1) {
                    // Ignore; it should end shortly.
                }
            }
        }

        disposed = true;
    }

    /**
     * Implement this method to release any acquired resources.  This method
     * is invoked only once by {@link #dispose()}.
     */
    protected abstract IoFuture dispose0() throws Exception;

    /**
     * {@inheritDoc}
     */
    public final Map<Long, IoSession> getManagedSessions() {
        return listeners.getManagedSessions();
    }

    /**
     * {@inheritDoc}
     */
    public final int getManagedSessionCount() {
        return listeners.getManagedSessionCount();
    }

    /**
     * {@inheritDoc}
     */
    public final IoHandler getHandler() {
        return handler;
    }

    /**
     * {@inheritDoc}
     */
    public final void setHandler(org.apache.mina.core.service.IoHandler handler) {
        if (handler == null) {
            throw new NullPointerException("handler cannot be null");
        }

        if (isActive()) {
            throw new IllegalStateException(
                    "handler cannot be set while the service is active.");
        }

        this.handler = handler;
    }

    /**
     * {@inheritDoc}
     */
    public IoSessionConfigEx getSessionConfig() {
        return sessionConfig;
    }

    /**
     * {@inheritDoc}
     */
    public final IoSessionDataStructureFactory getSessionDataStructureFactory() {
        return sessionDataStructureFactory;
    }

    /**
     * {@inheritDoc}
     */
    public final void setSessionDataStructureFactory(
            IoSessionDataStructureFactory sessionDataStructureFactory) {
        if (sessionDataStructureFactory == null) {
            throw new NullPointerException("sessionDataStructureFactory");
        }

        if (isActive()) {
            throw new IllegalStateException(
                    "sessionDataStructureFactory cannot be set while the service is active.");
        }

        this.sessionDataStructureFactory = sessionDataStructureFactory;
    }

    /**
     * {@inheritDoc}
     */
    public IoServiceStatistics getStatistics() {
        return stats;
    }

    /**
     * {@inheritDoc}
     */
    public final long getActivationTime() {
        return listeners.getActivationTime();
    }

    /**
     * {@inheritDoc}
     */
    public final Set<WriteFuture> broadcast(Object message) {
        // Convert to Set.  We do not return a List here because only the
        // direct caller of MessageBroadcaster knows the order of write
        // operations.
        final List<WriteFuture> futures = IoUtil.broadcast(message,
                getManagedSessions().values());
        return new AbstractSet<WriteFuture>() {
            @Override
            public Iterator<WriteFuture> iterator() {
                return futures.iterator();
            }

            @Override
            public int size() {
                return futures.size();
            }
        };
    }

    public final IoServiceListenerSupport getListeners() {
        return listeners;
    }


    protected final void executeWorker(Runnable worker) {
        executeWorker(worker, null);
    }

    protected final void executeWorker(Runnable worker, String suffix) {
        String actualThreadName = threadName;
        if (suffix != null) {
            actualThreadName = actualThreadName + '-' + suffix;
        }
        executor.execute(new NamePreservingRunnable(worker, actualThreadName));
    }

    // TODO Figure out make it work without causing a compiler error / warning.
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected final void initSession(IoSession session,
            IoFuture future, IoSessionInitializer sessionInitializer) {
        // Update lastIoTime if needed.
//        if (stats.getLastReadTime() == 0) {
//            stats.setLastReadTime(getActivationTime());
//        }
        
//        if (stats.getLastWriteTime() == 0) {
//            stats.setLastWriteTime(getActivationTime());
//        }

        // Every property but attributeMap should be set now.
        // Now initialize the attributeMap.  The reason why we initialize
        // the attributeMap at last is to make sure all session properties
        // such as remoteAddress are provided to IoSessionDataStructureFactory.
        try {
            ((AbstractIoSession) session).setAttributeMap(session.getService()
                    .getSessionDataStructureFactory().getAttributeMap(session));
        } catch (IoSessionInitializationException e) {
            throw e;
        } catch (Exception e) {
            throw new IoSessionInitializationException(
                    "Failed to initialize an attributeMap.", e);
        }

        try {
            ((AbstractIoSession) session).setWriteRequestQueue(session
                    .getService().getSessionDataStructureFactory()
                    .getWriteRequestQueue(session));
        } catch (IoSessionInitializationException e) {
            throw e;
        } catch (Exception e) {
            throw new IoSessionInitializationException(
                    "Failed to initialize a writeRequestQueue.", e);
        }

        if ((future != null) && (future instanceof ConnectFuture)) {
            // DefaultIoFilterChain will notify the future. (We support ConnectFuture only for now).
            session.setAttribute(DefaultIoFilterChain.SESSION_CREATED_FUTURE,
                    future);
        }

        if (sessionInitializer != null) {
            sessionInitializer.initializeSession(session, future);
        }

        finishSessionInitialization0(session, future);
    }

    /**
     * Implement this method to perform additional tasks required for session
     * initialization. Do not call this method directly;
     * {@link #finishSessionInitialization(IoSession, IoFuture, IoSessionInitializer)} will call
     * this method instead.
     */
    protected void finishSessionInitialization0(IoSession session,
            IoFuture future) {
        // Do nothing. Extended class might add some specific code 
    }

    protected static class ServiceOperationFuture extends DefaultIoFuture {
        public ServiceOperationFuture() {
            super(null);
        }

        public final boolean isDone() {
            return getValue() == Boolean.TRUE;
        }

        public final void setDone() {
            setValue(Boolean.TRUE);
        }

        public final Exception getException() {
            if (getValue() instanceof Exception) {
                return (Exception) getValue();
            }
            
            return null;
        }

        public final void setException(Exception exception) {
            if (exception == null) {
                throw new NullPointerException("exception");
            }
            setValue(exception);
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getScheduledWriteBytes() {
        return 0; //stats.getScheduledWriteBytes();
    }

    /**
     * {@inheritDoc}
     */
    public int getScheduledWriteMessages() {
        return 0; //stats.getScheduledWriteMessages();
    }
    
}
