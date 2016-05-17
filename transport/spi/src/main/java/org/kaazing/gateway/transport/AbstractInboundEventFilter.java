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

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;

public abstract class AbstractInboundEventFilter extends IoFilterAdapter {

    private final TypedAttributeKey<Queue<InboundEvent>> inboundEventsKey = 
                        new TypedAttributeKey<>(getClass(), "inboundEvents");

    @Override
    public void sessionCreated(NextFilter nextFilter, IoSession session) throws Exception {
        Queue<InboundEvent> inboundEvents = inboundEventsKey.get(session);
        if (inboundEvents != null) {
            inboundEvents.add(new SessionCreatedEvent());
        }
        else {
            super.sessionCreated(nextFilter, session);
        }
    }

    @Override
    public void sessionOpened(NextFilter nextFilter, IoSession session) throws Exception {
        Queue<InboundEvent> inboundEvents = inboundEventsKey.get(session);
        if (inboundEvents != null) {
            inboundEvents.add(new SessionOpenedEvent());
        }
        else {
            super.sessionOpened(nextFilter, session);
        }
    }

    @Override
    public void sessionIdle(NextFilter nextFilter, IoSession session, IdleStatus status) throws Exception {
        // note: *always* propagate session idle
        super.sessionIdle(nextFilter, session, status);
    }

    @Override
    public void exceptionCaught(NextFilter nextFilter, IoSession session, Throwable cause) throws Exception {
        // note: *always* propagate exception caught
        super.exceptionCaught(nextFilter, session, cause);
    }

    @Override
    public void messageReceived(NextFilter nextFilter, IoSession session, Object message) throws Exception {
        Queue<InboundEvent> inboundEvents = inboundEventsKey.get(session);
        if (inboundEvents != null) {
            inboundEvents.add(new MessageReceivedEvent(message));
        }
        else {
            super.messageReceived(nextFilter, session, message);
        }
    }

    @Override
    public void messageSent(NextFilter nextFilter, IoSession session, WriteRequest writeRequest) throws Exception {
        Queue<InboundEvent> inboundEvents = inboundEventsKey.get(session);
        if (inboundEvents != null) {
            inboundEvents.add(new MessageSentEvent(writeRequest));
        }
        else {
            super.messageSent(nextFilter, session, writeRequest);
        }
    }

    @Override
    public void sessionClosed(NextFilter nextFilter, IoSession session) throws Exception {
        Queue<InboundEvent> inboundEvents = inboundEventsKey.get(session);
        if (inboundEvents != null) {
            inboundEvents.add(new SessionClosedEvent());
        }
        else {
            super.sessionClosed(nextFilter, session);
        }
    }

    protected Queue<InboundEvent> suspendInboundEvents(NextFilter nextFilter, IoSession session) throws Exception {
        Queue<InboundEvent> inboundEvents = inboundEventsKey.get(session);
        if (inboundEvents == null) {
            Queue<InboundEvent> newInboundEvents = new ConcurrentLinkedQueue<>();
            inboundEvents = inboundEventsKey.setIfAbsent(session, newInboundEvents);
            if (inboundEvents == null) {
                inboundEvents = newInboundEvents;
            }
        }
        return inboundEvents;
    }

    protected void flushInboundEvents(NextFilter nextFilter, IoSession session) throws Exception {
        Queue<InboundEvent> inboundEvents = inboundEventsKey.remove(session);
        if (inboundEvents != null) {
            for (InboundEvent inboundEvent : inboundEvents) {
                inboundEvent.flush(nextFilter, session);
            }
        }
    }

    private static abstract class InboundEvent { 
        public abstract void flush(NextFilter nextFilter, IoSession session);
    }

    private static final class SessionCreatedEvent extends InboundEvent {
        @Override
        public void flush(NextFilter nextFilter, IoSession session) {
            nextFilter.sessionCreated(session);
        }
    }

    private static final class SessionOpenedEvent extends InboundEvent {
        @Override
        public void flush(NextFilter nextFilter, IoSession session) {
            nextFilter.sessionOpened(session);
        }
    }

    private static final class MessageReceivedEvent extends InboundEvent {
        private final Object message;

        public MessageReceivedEvent(Object message) {
            this.message = message;
        }

        @Override
        public void flush(NextFilter nextFilter, IoSession session) {
            nextFilter.messageReceived(session, message);
        }
    }

    private static final class MessageSentEvent extends InboundEvent {
        private final WriteRequest writeRequest;

        public MessageSentEvent(WriteRequest writeRequest) {
            this.writeRequest = writeRequest;
        }

        @Override
        public void flush(NextFilter nextFilter, IoSession session) {
            nextFilter.messageSent(session, writeRequest);
        }
    }

    private static final class SessionClosedEvent extends InboundEvent {
        @Override
        public void flush(NextFilter nextFilter, IoSession session) {
            nextFilter.sessionClosed(session);
        }
    }

}
