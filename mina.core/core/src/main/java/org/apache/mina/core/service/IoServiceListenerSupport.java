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
package org.apache.mina.core.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.util.ExceptionMonitor;

/**
 * A helper which provides addition and removal of {@link IoServiceListener}s and firing
 * events.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class IoServiceListenerSupport {
    /**
     * The {@link IoService} that this instance manages.
     */
    private final IoService service;

    /**
     * A list of {@link IoServiceListener}s.
     */
    private final List<IoServiceListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Tracks managed sessions.
     */
    private final ConcurrentMap<Long, IoSession> managedSessions = new ConcurrentHashMap<>();

    /**
     * Read only version of {@link #managedSessions}.
     */
    private final Map<Long, IoSession> readOnlyManagedSessions = Collections.unmodifiableMap(managedSessions);

    private final AtomicBoolean activated = new AtomicBoolean();
    private volatile long activationTime;
    private volatile int largestManagedSessionCount;
    private volatile long cumulativeManagedSessionCount;

    /**
     * Creates a new instance.
     */
    public IoServiceListenerSupport(IoService service) {
        if (service == null) {
            throw new NullPointerException("service");
        }
        this.service = service;
    }

    /**
     * Adds a new listener.
     */
    public void add(IoServiceListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes an existing listener.
     */
    public void remove(IoServiceListener listener) {
        listeners.remove(listener);
    }

    public long getActivationTime() {
        return activationTime;
    }

    public Map<Long, IoSession> getManagedSessions() {
        return readOnlyManagedSessions;
    }

    public int getManagedSessionCount() {
        return managedSessions.size();
    }

    public int getLargestManagedSessionCount() {
        return largestManagedSessionCount;
    }

    public long getCumulativeManagedSessionCount() {
        return cumulativeManagedSessionCount;
    }

    public boolean isActive() {
        return activated.get();
    }

    /**
     * Calls {@link IoServiceListener#serviceActivated(IoService)}
     * for all registered listeners.
     */
    public void fireServiceActivated() {
        if (!activated.compareAndSet(false, true)) {
            return;
        }

        activationTime = System.currentTimeMillis();

        for (IoServiceListener l : listeners) {
            try {
                l.serviceActivated(service);
            } catch (Throwable e) {
                ExceptionMonitor.getInstance().exceptionCaught(e);
            }
        }
    }

    /**
     * Calls {@link IoServiceListener#serviceDeactivated(IoService)}
     * for all registered listeners.
     */
    public void fireServiceDeactivated() {
        if (!activated.compareAndSet(true, false)) {
            return;
        }

        try {
            for (IoServiceListener l : listeners) {
                try {
                    l.serviceDeactivated(service);
                } catch (Throwable e) {
                    ExceptionMonitor.getInstance().exceptionCaught(e);
                }
            }
        } finally {
            disconnectSessions();
        }
    }

    /**
     * Calls {@link IoServiceListener#sessionCreated(IoSession)} for all registered listeners.
     */
    public void fireSessionCreated(IoSession session) {
        boolean firstSession = false;
        if (session.getService() instanceof IoConnector) {
            synchronized (managedSessions) {
                firstSession = managedSessions.isEmpty();
            }
        }

        // If already registered, ignore.
        if (managedSessions.putIfAbsent(session.getId(), session) != null) {
            return;
        }

        // If the first connector session, fire a virtual service activation event.
        if (firstSession) {
            fireServiceActivated();
        }

        // Fire session events.
        IoFilterChain filterChain = session.getFilterChain(); 
        filterChain.fireSessionCreated();
        filterChain.fireSessionOpened();

        int managedSessionCount = managedSessions.size();
        if (managedSessionCount > largestManagedSessionCount) {
            largestManagedSessionCount = managedSessionCount;
        }
        cumulativeManagedSessionCount ++;

        // Fire listener events.
        for (IoServiceListener l : listeners) {
            try {
                l.sessionCreated(session);
            } catch (Throwable e) {
                ExceptionMonitor.getInstance().exceptionCaught(e);
            }
        }
    }

    /**
     * Calls {@link IoServiceListener#sessionDestroyed(IoSession)} for all registered listeners.
     */
    public void fireSessionDestroyed(IoSession session) {
        // Try to remove the remaining empty session set after removal.
        if (managedSessions.remove(session.getId()) == null) {
            return;
        }

        // Fire session events.
        session.getFilterChain().fireSessionClosed();

        // Fire listener events.
        try {
            for (IoServiceListener l : listeners) {
                try {
                    l.sessionDestroyed(session);
                } catch (Throwable e) {
                    ExceptionMonitor.getInstance().exceptionCaught(e);
                }
            }
        } finally {
            // Fire a virtual service deactivation event for the last session of the connector.
            if (session.getService() instanceof IoConnector) {
                boolean lastSession;
                synchronized (managedSessions) {
                    lastSession = managedSessions.isEmpty();
                }
                if (lastSession) {
                    fireServiceDeactivated();
                }
            }
        }
    }

    private void disconnectSessions() {
        if (!(service instanceof IoAcceptor)) {
            return;
        }

        if (!((IoAcceptor) service).isCloseOnDeactivation()) {
            return;
        }

        Object lock = new Object();
        IoFutureListener<IoFuture> listener = new LockNotifyingListener(lock);

        for (IoSession s : managedSessions.values()) {
            s.close(true).addListener(listener);
        }

        try {
            synchronized (lock) {
                while (!managedSessions.isEmpty()) {
                    lock.wait(500);
                }
            }
        } catch (InterruptedException ie) {
            // Ignored
        }
    }

    private static class LockNotifyingListener implements IoFutureListener<IoFuture> {
        private final Object lock;

        public LockNotifyingListener(Object lock) {
            this.lock = lock;
        }

        public void operationComplete(IoFuture future) {
            synchronized (lock) {
                lock.notifyAll();
            }
        }
    }
}
