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
package org.kaazing.gateway.transport;

import org.kaazing.mina.core.session.IoSessionEx;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.IoSession;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * A suspendable {@link IoFilterAdapter} that allows the filter chain to
 * suspend and resume. It buffers I/O filter events during the filter chain
 * suspension and fires those events when the filter chain processing is
 * resumed.
 */
public abstract class SuspendableIoFilterAdapter extends IoFilterAdapter {
    private final Queue<SuspendableEvent>  queue = new ArrayDeque<>();

    // Used only in I/O thread, hence not using AtomicInteger
    private int suspendCount = 0;

    @Override
    public final void sessionCreated(NextFilter nextFilter, IoSession session) throws Exception {
        if (isSuspended()) {
            queue.add(new SessionCreatedEvent(nextFilter, session));
        } else {
            doSessionCreated(nextFilter, session);
        }
    }

    protected void doSessionCreated(NextFilter nextFilter, IoSession session) throws Exception {
        nextFilter.sessionCreated(session);
    }

    @Override
    public final void sessionOpened(NextFilter nextFilter, IoSession session) throws Exception {
        if (isSuspended()) {
            queue.add(new SessionOpenedEvent(nextFilter, session));
        } else {
            doSessionOpened(nextFilter, session);
        }
    }

    protected void doSessionOpened(NextFilter nextFilter, IoSession session) throws Exception {
        nextFilter.sessionOpened(session);
    }

    @Override
    public final void sessionClosed(NextFilter nextFilter, IoSession session)
            throws Exception {
        if (isSuspended()) {
            queue.add(new SessionClosedEvent(nextFilter, session));
        } else {
            doSessionClosed(nextFilter, session);
        }
    }

    protected void doSessionClosed(NextFilter nextFilter, IoSession session) throws Exception {
        nextFilter.sessionClosed(session);
    }

    @Override
    public final void messageReceived(NextFilter nextFilter, IoSession session, Object message) throws Exception {
        if (isSuspended()) {
            queue.add(new MessageReceivedEvent(nextFilter, session, message));
        } else {
            doMessageReceived(nextFilter, session, message);
        }
    }


    /**
     * A suspendable filter usually overrides this method.
     */
    protected void doMessageReceived(NextFilter nextFilter, IoSession session, Object message) throws Exception {
        nextFilter.messageReceived(session, message);
    }

    // Suspends incoming events. Executed in I/O thread
    protected void suspendIncoming(IoSession session) {
        ++suspendCount;
        if (suspendCount == 1) {
            session.suspendRead();
        }
    }

    // Not executed in I/O thread, but it will schedule the task of firing I/O events
    // on I/O thread.
    protected void resumeIncoming(IoSession session) throws Exception {
        Runnable resumeTask = new ResumeRunnable(session);
        assert (session instanceof IoSessionEx);
        ((IoSessionEx)session).getIoExecutor().execute(resumeTask);
    }

    // Executed in I/O thread
    private class ResumeRunnable implements Runnable {
        private final IoSession session;

        ResumeRunnable(IoSession session) {
            this.session = session;
        }

        @Override
        public void run() {
            --suspendCount;
            if (suspendCount == 0) {
                while(!queue.isEmpty()) {
                    SuspendableEvent event = queue.remove();
                    try {
                        event.fire();
                    } catch (Exception e) {
                        session.getFilterChain().fireExceptionCaught(e);
                    }
                }
                session.resumeRead();
            }
        }
    }

    private boolean isSuspended() {
        return suspendCount > 0;
    }

    private interface SuspendableEvent {
        void fire() throws Exception;
    }

    private class SessionCreatedEvent implements SuspendableEvent {
        private final NextFilter nextFilter;
        private final IoSession session;

        SessionCreatedEvent(NextFilter nextFilter, IoSession session) {
            this.nextFilter = nextFilter;
            this.session = session;
        }

        @Override
        public void fire() throws Exception {
            SuspendableIoFilterAdapter.this.doSessionCreated(nextFilter, session);
        }
    }

    private class SessionOpenedEvent implements SuspendableEvent {
        private final NextFilter nextFilter;
        private final IoSession session;

        SessionOpenedEvent(NextFilter nextFilter, IoSession session) {
            this.nextFilter = nextFilter;
            this.session = session;
        }

        @Override
        public void fire() throws Exception {
            SuspendableIoFilterAdapter.this.doSessionOpened(nextFilter, session);
        }
    }

    private class SessionClosedEvent implements SuspendableEvent {
        private final NextFilter nextFilter;
        private final IoSession session;

        SessionClosedEvent(NextFilter nextFilter, IoSession session) {
            this.nextFilter = nextFilter;
            this.session = session;
        }

        @Override
        public void fire() throws Exception {
            SuspendableIoFilterAdapter.this.doSessionClosed(nextFilter, session);
        }
    }

    private class MessageReceivedEvent implements SuspendableEvent {
        private final NextFilter nextFilter;
        private final IoSession session;
        private final Object message;

        MessageReceivedEvent(NextFilter nextFilter, IoSession session, Object message) {
            this.nextFilter = nextFilter;
            this.session = session;
            this.message = message;
        }

        @Override
        public void fire() throws Exception {
            SuspendableIoFilterAdapter.this.doMessageReceived(nextFilter, session, message);
        }
    }

}