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
/** The copyright above pertains to portions created by Kaazing */

package org.apache.mina.core.polling;

import java.net.SocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

import org.apache.mina.core.RuntimeIoException;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.service.AbstractIoAcceptor;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoProcessor;
import org.apache.mina.core.session.AbstractIoSession;
import org.apache.mina.core.session.ExpiringSessionRecycler;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionConfig;
import org.apache.mina.core.session.IoSessionRecycler;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.core.write.WriteRequestQueue;
import org.apache.mina.util.ExceptionMonitor;

/**
 * TODO Add documentation
 * {@link IoAcceptor} for datagram transport (UDP/IP).
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * @org.apache.xbean.XBean
 */
public abstract class AbstractPollingConnectionlessIoAcceptor<T extends AbstractIoSession, H>
        extends AbstractIoAcceptor {

    private static final IoSessionRecycler DEFAULT_RECYCLER = new ExpiringSessionRecycler();

    private final Object lock = new Object();
    private final IoProcessor<T> processor = new ConnectionlessAcceptorProcessor();
    private final Queue<AcceptorOperationFuture> registerQueue =
            new ConcurrentLinkedQueue<>();
    private final Queue<AcceptorOperationFuture> cancelQueue =
            new ConcurrentLinkedQueue<>();
    private final Queue<T> flushingSessions = new ConcurrentLinkedQueue<>();
    private final Map<SocketAddress, H> boundHandles =
        Collections.synchronizedMap(new HashMap<>());

    private IoSessionRecycler sessionRecycler = DEFAULT_RECYCLER;

    private final ServiceOperationFuture disposalFuture =
        new ServiceOperationFuture();
    private volatile boolean selectable;
    
    /** The thread responsible of accepting incoming requests */ 
    private Acceptor acceptor;

    private long lastIdleCheckTime;

    /**
     * Creates a new instance.
     */
    protected AbstractPollingConnectionlessIoAcceptor(IoSessionConfig sessionConfig) {
        this(sessionConfig, null);
    }

    /**
     * Creates a new instance.
     */
    protected AbstractPollingConnectionlessIoAcceptor(IoSessionConfig sessionConfig, Executor executor) {
        super(sessionConfig, executor);

        try {
            init();
            selectable = true;
        } catch (RuntimeException e){
            throw e;
        } catch (Exception e) {
            throw new RuntimeIoException("Failed to initialize.", e);
        } finally {
            if (!selectable) {
                try {
                    destroy();
                } catch (Exception e) {
                    ExceptionMonitor.getInstance().exceptionCaught(e);
                }
            }
        }
    }

    protected abstract void init() throws Exception;
    protected abstract void destroy() throws Exception;
    protected abstract int select() throws Exception;
    protected abstract int select(int timeout) throws Exception;
    protected abstract void wakeup();
    protected abstract Iterator<H> selectedHandles();
    protected abstract H open(SocketAddress localAddress) throws Exception;
    protected abstract void close(H handle) throws Exception;
    protected abstract SocketAddress localAddress(H handle) throws Exception;
    protected abstract boolean isReadable(H handle);
    protected abstract boolean isWritable(H handle);
    protected abstract SocketAddress receive(H handle, IoBuffer buffer) throws Exception;
    protected abstract int send(T session, IoBuffer buffer, SocketAddress remoteAddress) throws Exception;
    protected abstract T newSession(IoProcessor<T> processor, H handle, SocketAddress remoteAddress) throws Exception;
    protected abstract void setInterestedInWrite(T session, boolean interested) throws Exception;

    /**
     * {@inheritDoc}
     */
    @Override
    protected IoFuture dispose0() throws Exception {
        unbind();
        if (!disposalFuture.isDone()) {
            startupAcceptor();
            wakeup();
        }
        return disposalFuture;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final Set<SocketAddress> bindInternal(
            List<? extends SocketAddress> localAddresses) throws Exception {
        // Create a bind request as a Future operation. When the selector
        // have handled the registration, it will signal this future.
        AcceptorOperationFuture request = new AcceptorOperationFuture(localAddresses);

        // adds the Registration request to the queue for the Workers
        // to handle
        registerQueue.add(request);

        // creates the Acceptor instance and has the local
        // executor kick it off.
        startupAcceptor();
        
        // As we just started the acceptor, we have to unblock the select()
        // in order to process the bind request we just have added to the 
        // registerQueue.
        wakeup();

        // Now, we wait until this request is completed.
        request.awaitUninterruptibly();

        if (request.getException() != null) {
            throw request.getException();
        }

        // Update the local addresses.
        // setLocalAddresses() shouldn't be called from the worker thread
        // because of deadlock.
        Set<SocketAddress> newLocalAddresses = new HashSet<>();

        for (H handle:boundHandles.values()) {
            newLocalAddresses.add(localAddress(handle));
        }
        
        return newLocalAddresses;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void unbind0(
            List<? extends SocketAddress> localAddresses) throws Exception {
        AcceptorOperationFuture request = new AcceptorOperationFuture(localAddresses);

        cancelQueue.add(request);
        startupAcceptor();
        wakeup();

        request.awaitUninterruptibly();

        if (request.getException() != null) {
            throw request.getException();
        }
    }

    /**
     * {@inheritDoc}
     */
    public final IoSession newSession(SocketAddress remoteAddress, SocketAddress localAddress) {
        if (isDisposing()) {
            throw new IllegalStateException("Already disposed.");
        }

        if (remoteAddress == null) {
            throw new NullPointerException("remoteAddress");
        }

        synchronized (bindLock) {
            if (!isActive()) {
                throw new IllegalStateException(
                        "Can't create a session from a unbound service.");
            }

            try {
                return newSessionWithoutLock(remoteAddress, localAddress);
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeIoException("Failed to create a session.", e);
            }
        }
    }

    private IoSession newSessionWithoutLock(
            SocketAddress remoteAddress, SocketAddress localAddress) throws Exception {
        H handle = boundHandles.get(localAddress);
        if (handle == null) {
            throw new IllegalArgumentException("Unknown local address: " + localAddress);
        }

        IoSession session;
        IoSessionRecycler sessionRecycler = getSessionRecycler();
        synchronized (sessionRecycler) {
            session = sessionRecycler.recycle(localAddress, remoteAddress);
            if (session != null) {
                return session;
            }

            // If a new session needs to be created.
            T newSession = newSession(processor, handle, remoteAddress);
            getSessionRecycler().put(newSession);
            session = newSession;
        }

        initSession(session, null, null);

        try {
            this.getFilterChainBuilder().buildFilterChain(session.getFilterChain());
            getListeners().fireSessionCreated(session);
        } catch (Throwable t) {
            ExceptionMonitor.getInstance().exceptionCaught(t);
        }

        return session;
    }

    public final IoSessionRecycler getSessionRecycler() {
        return sessionRecycler;
    }

    public final void setSessionRecycler(IoSessionRecycler sessionRecycler) {
        synchronized (bindLock) {
            if (isActive()) {
                throw new IllegalStateException(
                        "sessionRecycler can't be set while the acceptor is bound.");
            }

            if (sessionRecycler == null) {
                sessionRecycler = DEFAULT_RECYCLER;
            }
            this.sessionRecycler = sessionRecycler;
        }
    }

    private class ConnectionlessAcceptorProcessor implements IoProcessor<T> {

        public void add(T session) {
        }

        public void flush(T session) {
            if (scheduleFlush(session)) {
                wakeup();
            }
        }

        public void remove(T session) {
            getSessionRecycler().remove(session);
            getListeners().fireSessionDestroyed(session);
        }

        public void updateTrafficControl(T session) {
            throw new UnsupportedOperationException();
        }

        public void dispose() {
        }

        public boolean isDisposed() {
            return false;
        }

        public boolean isDisposing() {
            return false;
        }
    }

    /**
     * Starts the inner Acceptor thread.
     */
    private void startupAcceptor() {
        if (!selectable) {
            registerQueue.clear();
            cancelQueue.clear();
            flushingSessions.clear();
        }

        synchronized (lock) {
            if (acceptor == null) {
                acceptor = new Acceptor();
                executeWorker(acceptor);
            }
        }
    }

    private boolean scheduleFlush(T session) {
        if (session.setScheduledForFlush(true)) {
            flushingSessions.add(session);
            return true;
        } else {
            return false;
        }
    }

    /**
     * This private class is used to accept incoming connection from 
     * clients. It's an infinite loop, which can be stopped when all
     * the registered handles have been removed (unbound). 
     */
    private class Acceptor implements Runnable {
        public void run() {
            int nHandles = 0;
            lastIdleCheckTime = System.currentTimeMillis();

            while (selectable) {
                try {
                    int selected = select();

                    nHandles += registerHandles();

                    if (selected > 0) {
                        processReadySessions(selectedHandles());
                    }

                    long currentTime = System.currentTimeMillis();
                    flushSessions(currentTime);
                    nHandles -= unregisterHandles();

                    notifyIdleSessions(currentTime);

                    if (nHandles == 0) {
                        synchronized (lock) {
                            if (registerQueue.isEmpty() && cancelQueue.isEmpty()) {
                                acceptor = null;
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    ExceptionMonitor.getInstance().exceptionCaught(e);

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                    }
                }
            }

            if (selectable && isDisposing()) {
                selectable = false;
                try {
                    destroy();
                } catch (Exception e) {
                    ExceptionMonitor.getInstance().exceptionCaught(e);
                } finally {
                    disposalFuture.setValue(true);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void processReadySessions(Iterator<H> handles) {
        while (handles.hasNext()) {
            H h = handles.next();
            handles.remove();
            try {
                if (isReadable(h)) {
                    readHandle(h);
                }

                if (isWritable(h)) {
                    for (IoSession session : getManagedSessions().values()) {
                        scheduleFlush((T) session);
                    }
                }
            } catch (Throwable t) {
                ExceptionMonitor.getInstance().exceptionCaught(t);
            }
        }
    }

    private void readHandle(H handle) throws Exception {
        IoBuffer readBuf = newReadBuffer(getSessionConfig().getReadBufferSize());

        SocketAddress remoteAddress = receive(handle, readBuf);
        if (remoteAddress != null) {
            IoSession session = newSessionWithoutLock(
                    remoteAddress, localAddress(handle));

            readBuf.flip();

            IoBuffer newBuf = newReadBuffer(readBuf.limit());
            newBuf.put(readBuf);
            newBuf.flip();

            session.getFilterChain().fireMessageReceived(newBuf);
        }
    }

    protected IoBuffer newReadBuffer(int readBufferSize) {
        return IoBuffer.allocate(readBufferSize);
    }

    private void flushSessions(long currentTime) {
        for (; ;) {
            T session = flushingSessions.poll();
            if (session == null) {
                break;
            }

            session.setScheduledForFlush(false);

            try {
                boolean flushedAll = flush(session, currentTime);
                if (flushedAll && !session.getWriteRequestQueue().isEmpty(session) &&
                    !session.isScheduledForFlush()) {
                    scheduleFlush(session);
                }
            } catch (Exception e) {
                session.getFilterChain().fireExceptionCaught(e);
            }
        }
    }

    private boolean flush(T session, long currentTime) throws Exception {
        // Clear OP_WRITE
        setInterestedInWrite(session, false);

        final WriteRequestQueue writeRequestQueue = session.getWriteRequestQueue();
        final int maxWrittenBytes =
            session.getConfig().getMaxReadBufferSize() +
            (session.getConfig().getMaxReadBufferSize() >>> 1);

        int writtenBytes = 0;
        try {
            for (; ;) {
                WriteRequest req = session.getCurrentWriteRequest();
                if (req == null) {
                    req = writeRequestQueue.poll(session);
                    if (req == null) {
                        break;
                    }
                    session.setCurrentWriteRequest(req);
                }

                IoBuffer buf = (IoBuffer) req.getMessage();
                if (buf.remaining() == 0) {
                    // Clear and fire event
                    session.setCurrentWriteRequest(null);
                    buf.reset();
                    session.getFilterChain().fireMessageSent(req);
                    continue;
                }

                SocketAddress destination = req.getDestination();
                if (destination == null) {
                    destination = session.getRemoteAddress();
                }

                int localWrittenBytes = send(session, buf, destination);
                if (localWrittenBytes == 0 || writtenBytes >= maxWrittenBytes) {
                    // Kernel buffer is full or wrote too much
                    setInterestedInWrite(session, true);
                    return false;
                } else {
                    setInterestedInWrite(session, false);

                    // Clear and fire event
                    session.setCurrentWriteRequest(null);
                    writtenBytes += localWrittenBytes;
                    buf.reset();
                    session.getFilterChain().fireMessageSent(req);
                }
            }
        } finally {
            session.increaseWrittenBytes(writtenBytes, currentTime);
        }

        return true;
    }

    private int registerHandles() {
        for (;;) {
            AcceptorOperationFuture req = registerQueue.poll();
            if (req == null) {
                break;
            }

            Map<SocketAddress, H> newHandles = new HashMap<>();
            List<SocketAddress> localAddresses = req.getLocalAddresses();
            try {
                for (SocketAddress a: localAddresses) {
                    H handle = open(a);
                    newHandles.put(localAddress(handle), handle);
                }
                boundHandles.putAll(newHandles);

                getListeners().fireServiceActivated();
                req.setDone();
                return newHandles.size();
            } catch (Exception e) {
                req.setException(e);
            } finally {
                // Roll back if failed to bind all addresses.
                if (req.getException() != null) {
                    for (H handle: newHandles.values()) {
                        try {
                            close(handle);
                        } catch (Exception e) {
                            ExceptionMonitor.getInstance().exceptionCaught(e);
                        }
                    }
                    wakeup();
                }
            }
        }

        return 0;
    }

    private int unregisterHandles() {
        int nHandles = 0;
        for (;;) {
            AcceptorOperationFuture request = cancelQueue.poll();
            if (request == null) {
                break;
            }

            // close the channels
            for (SocketAddress a: request.getLocalAddresses()) {
                H handle = boundHandles.remove(a);
                if (handle == null) {
                    continue;
                }

                try {
                    close(handle);
                    wakeup(); // wake up again to trigger thread death
                } catch (Throwable e) {
                    ExceptionMonitor.getInstance().exceptionCaught(e);
                } finally {
                    nHandles ++;
                }
            }

            request.setDone();
        }

        return nHandles;
    }

    private void notifyIdleSessions(long currentTime) {
        // process idle sessions
        if (currentTime - lastIdleCheckTime >= 1000) {
            lastIdleCheckTime = currentTime;
            AbstractIoSession.notifyIdleness(
                    getListeners().getManagedSessions().values().iterator(),
                    currentTime);
        }
    }
}
