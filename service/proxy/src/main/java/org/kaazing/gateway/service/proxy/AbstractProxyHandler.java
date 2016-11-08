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

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.kaazing.gateway.service.ServiceContext;
import org.kaazing.gateway.service.proxy.ProxyConnectStrategy.Strategy;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.filter.util.WriteRequestFilterEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractProxyHandler extends IoHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractProxyHandler.class);

    private static final AttributeKey ATTACHED_SESSION_KEY = new AttributeKey(AbstractProxyHandler.class,
            "attachedSession");
    private static final AttributeKey QUEUED_MESSAGES_KEY = new AttributeKey(AbstractProxyHandler.class,
            "queuedMessages");

    private ServiceContext serviceContext;
    private int maximumPendingBytes;
    private int maximumTransferredBytes = -1; // default to unlimited
    private int thresholdPendingBytes;
    private int maximumRecoveryInterval = 0;
    private ProxyConnectStrategy connectStrategy;

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(format("Closing session %s due to exception:", session), cause);
        }
        else {
            LOGGER.info(format("Closing session %s due to exception: %s", session, cause));
        }

        // KG-2335: avoid recursion due to write exceptions during flush if the connection is dead
        // TODO: consider adding a new SessionClosingFilter on the network end of the filter chain to fulfill the
        // session close future if an IOEXception is reported in exceptionCaught. Then session.isClosing() would
        // suffice here and anywhere else we may need this logic.
        boolean connectionClosing = session.isClosing() || (cause instanceof IOException);
        session.close(connectionClosing);
    }

    @Override
    public void sessionCreated(IoSession session) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[" + session.getId() + "] session created (" + session + ")");
        }
        Queue<Object> messageQueue = new ConcurrentLinkedQueue<>();
        session.setAttribute(QUEUED_MESSAGES_KEY, messageQueue);
    }

    @Override
    public void sessionClosed(IoSession session) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[" + session.getId() + "] session closed");
        }

        IoSession detachedSession = AbstractProxyHandler.detachSessions(session);
        if (detachedSession != null) {
            detachedSession.close(false);
        }
    }

    public void setServiceContext(ServiceContext serviceContext) {
        this.serviceContext = serviceContext;
    }

    @Override
    public void messageReceived(IoSession session, Object message) {

        // Note: IoBuffer message already duplicated in filter

        Queue<Object> messageQueue = getMessageQueue(session);
        AttachedSessionManager attachedSessionManager = getAttachedSessionManager(session);
        if (attachedSessionManager != null) {

            // efficient check to minimize additional processing per message
            if (messageQueue != null) {
                // flush deferred writes
                flushQueuedMessages(messageQueue, session, attachedSessionManager);

                // detach message queue
                session.removeAttribute(QUEUED_MESSAGES_KEY);
            }

            // write current message
            attachedSessionManager.writeMessage(session, message);
        } else if (messageQueue != null && !session.isClosing()) {
            // queue message unless closing
            messageQueue.add(message);
        }
    }

    public void setMaximumPendingBytes(int maximumPendingBytes) {
        this.maximumPendingBytes = maximumPendingBytes;
        thresholdPendingBytes = maximumPendingBytes / 2;
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Proxy handler %s: maximum.pending.bytes=%d, using resume threshold %d", this,
                    maximumPendingBytes, thresholdPendingBytes));
        }
    }

    void setMaximumTransferredBytes(int maximumTransferredBytes) {
        this.maximumTransferredBytes = maximumTransferredBytes;
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Proxy handler " + this + ": maximum.transferred.bytes=" + maximumTransferredBytes + ".");
        }
    }

    public void setMaximumRecoveryInterval(int maximumRecoveryInterval) {
        this.maximumRecoveryInterval = maximumRecoveryInterval;
        if ( LOGGER.isDebugEnabled() ) {
            LOGGER.debug("Proxy handler " + this + ": maximum.recovery.interval=" + maximumRecoveryInterval + ".");
        }
    }

    public int getMaximumRecoveryInterval() {
        return maximumRecoveryInterval;
    }

    public void setPreparedConnectionCount(int preparedConnectionCount) {
        setConnectStrategy(Strategy.PREPARED, preparedConnectionCount, preparedConnectionCount);
    }

    public void setPreparedConnectionCount(String connectStrategy, int preparedConnectionCount, int maxConnectionCount) {
        switch (connectStrategy) {
        case "prepared":
        case "immediate":
        case "deferred":
            break;
        default:
            throw new IllegalArgumentException(String.format("Unexpected value for connect strategy: %s", connectStrategy));
        }

        setConnectStrategy(Strategy.valueOf(connectStrategy.toUpperCase()), preparedConnectionCount, maxConnectionCount);
    }

    protected void setConnectStrategy(
        Strategy connectStrategy,
        int preparedConnectionCount,
        int maxConnectionCount)
    {
        this.connectStrategy = ProxyConnectStrategy.newInstance(connectStrategy, preparedConnectionCount, maxConnectionCount);
        if ( LOGGER.isDebugEnabled() ) {
            LOGGER.debug("Proxy handler " + this + ": connect.strategy=" + connectStrategy + ".");
        }
    }

    public int getPreparedConnectionCount() {
        return connectStrategy.getConnectionCount();
    }

    protected boolean isDeferredConnectStrategy() {
        return connectStrategy.getStrategy() == Strategy.DEFERRED;
    }

    // called by connect listener in proxy service handler
    protected void flushQueuedMessages(IoSession session, AttachedSessionManager attachedSessionManager) {
        Queue<Object> messageQueue = getMessageQueue(session);
        if (messageQueue != null) {
            flushQueuedMessages(messageQueue, session, attachedSessionManager);
            // Note: leave messageQueue intact on session to avoid race condition
            // will be cleared by next received message anyway
        }
    }

    protected ServiceContext getServiceContext() {
        return serviceContext;
    }

    private void flushQueuedMessages(Queue<Object> messageQueue,
                                     IoSession session,
                                     AttachedSessionManager attachedSessionManager) {
        while (messageQueue != null && !messageQueue.isEmpty()) {
            Object queuedMessage = messageQueue.poll();
            attachedSessionManager.writeMessage(session, queuedMessage);
        }
    }

    @SuppressWarnings("unchecked")
    private Queue<Object> getMessageQueue(IoSession session) {
        return (Queue<Object>) session.getAttribute(QUEUED_MESSAGES_KEY);
    }

    protected static AttachedSessionManager getAttachedSessionManager(IoSession session) {
        return (AttachedSessionManager) session.getAttribute(ATTACHED_SESSION_KEY);
    }

    protected AttachedSessionManager attachSessions(IoSession session, IoSession attachedSession) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[" + session.getId() + "->" + attachedSession.getId() + "] attaching sessions");
        }
        AttachedSessionManager attachedSessionManager = new AttachedSessionManager(attachedSession);
        session.setAttribute(ATTACHED_SESSION_KEY, attachedSessionManager);
        attachedSession.setAttribute(ATTACHED_SESSION_KEY, new AttachedSessionManager(session));
        return attachedSessionManager;
    }

    static IoSession detachSessions(IoSession session) {
        AttachedSessionManager detached = (AttachedSessionManager) session.removeAttribute(ATTACHED_SESSION_KEY);
        IoSession detachedSession = null;

        if (detached != null) {
            detachedSession = detached.getAttachedSession();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("[" + session.getId() + "->" + detachedSession.getId() + "] detaching sessions");
            }
            detachedSession.removeAttribute(ATTACHED_SESSION_KEY);
        }
        return detachedSession;
    }

    // This class manages an attached session, that is, the session to which messages received on a source session
    // will be written. It maintains a total of the number of bytes of messages scheduled for write which
    // have not yet been written, and suspends reads on the source session when the number of bytes
    // scheduled for write exceeds the configured maximum.pending.bytes for the service.
    protected class AttachedSessionManager {
        private final IoSession attachedSession;
        private final AtomicInteger scheduledWriteBytes = new AtomicInteger(0);
        private final AtomicBoolean readSuspended = new AtomicBoolean(false);
        private final AtomicInteger totalTransferredBytes = new AtomicInteger(0);

        // private throughput limit for this session
        private int sessionMaximumTransferredBytes = AbstractProxyHandler.this.maximumTransferredBytes;

        AttachedSessionManager(IoSession attachedSession) {
            this.attachedSession = attachedSession;
        }

        public IoSession getAttachedSession() {
            return attachedSession;
        }

        void writeMessage(final IoSession sourceSession, Object message) {

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("[" + sourceSession.getId() + "->" + attachedSession.getId() + "] proxying message: "
                        + message);
            }
            final int bytesWritten;
            if (message instanceof IoBuffer) {
                IoBuffer b = (IoBuffer) message;
                bytesWritten = b.remaining();
            } else {
                bytesWritten = 0;
            }

            // check if we are draining only after maximum transferred bytes
            switch (sessionMaximumTransferredBytes) {
            case -1:
                // no constraint on number of transferred bytes
                break;
            case 0:
                // if maximum transferred bytes is zero, then we don't write anything
                return;
            default:
                int newTotalTransferredBytes = totalTransferredBytes.addAndGet(bytesWritten);
                // simplify the check to write or not, maximizing the opportunity to drain
                if (newTotalTransferredBytes > sessionMaximumTransferredBytes) {

                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("[" + sourceSession.getId() + "->" + attachedSession.getId() + ", "
                                + Thread.currentThread().getName()
                                + "] writeMessage (maximum bytes transferred, draining only)");
                    }

                    // simplify the check for next iteration
                    sessionMaximumTransferredBytes = 0;
                }
                break;
            }

            int newScheduledWriteBytes = scheduledWriteBytes.addAndGet(bytesWritten);
            if (newScheduledWriteBytes > maximumPendingBytes) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("[" + sourceSession.getId() + "->" + attachedSession.getId() + ", "
                            + Thread.currentThread().getName() + "] scheduledWriteBytes " + newScheduledWriteBytes
                            + " exceeds " + maximumPendingBytes + ", suspending reads on " + sourceSession);
                }
                // KG-2665: handle the fact that AbstractIoSession.suspendRead and resumeRead are not
                // thread-safe by guarding with an AtomicBoolean and spinning to make sure the outcome is correct
                // when there is a race with resumeRead in the write request future listener. This also requires
                // a fix in Mina (KG-2820) to make AbstractIoSession.readSuspended volatile.
                while (readSuspended.compareAndSet(false, true)) {
                    sourceSession.suspendRead();
                }
            }
            // Add the FutureListener after suspending to ensure the FutureListener sees it is suspended
            WriteFuture future = attachedSession.write(message);
            future.addListener(new IoFutureListener<WriteFuture>() {
                @Override
                public void operationComplete(WriteFuture future) {
                    int newScheduledWriteBytes = scheduledWriteBytes.addAndGet(-bytesWritten);
                    // Use <= to ensure we resume read in case where both values are 0
                    if (readSuspended.get() && newScheduledWriteBytes <= thresholdPendingBytes) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("[" + sourceSession.getId() + "->" + attachedSession.getId() + ", "
                                    + Thread.currentThread().getName() + "] scheduledWriteBytes "
                                    + newScheduledWriteBytes + " <= " + thresholdPendingBytes + ", resuming reads on "
                                    + sourceSession);
                        }
                        // KG-2665: handle race with suspendRead, see above.
                        while (readSuspended.compareAndSet(true, false)) {
                            sourceSession.resumeRead();
                        }
                    }
                }
            });
        }

    }

    protected static class DuplicateBufferFilter extends WriteRequestFilterEx {

        private final IoBufferAllocatorEx<?> allocator;

        public DuplicateBufferFilter(IoBufferAllocatorEx<?> allocator) {
            this.allocator = allocator;
        }

        @Override
        protected Object doFilterWrite(NextFilter nextFilter,
                                       IoSession session,
                                       WriteRequest writeRequest,
                                       Object message) throws Exception {

            if (message instanceof IoBufferEx) {
                IoBufferEx buf = (IoBufferEx) message;
                message = allocator.wrap(buf.buf(), buf.flags());
            }

            return message;
        }
    }

}
