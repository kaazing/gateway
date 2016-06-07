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
package org.apache.mina.core.filterchain;

import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoEvent;
import org.apache.mina.core.session.IoEventType;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An I/O event or an I/O request that MINA provides for {@link IoFilter}s.
 * Most users won't need to use this class.  It is usually used by internal
 * components to store I/O events.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class IoFilterEvent extends IoEvent {
    /** A logger for this class */
    static Logger LOGGER = LoggerFactory.getLogger(IoFilterEvent.class);

    private final NextFilter nextFilter;

    public IoFilterEvent(NextFilter nextFilter, IoEventType type,
            IoSession session, Object parameter) {
        super(type, session, parameter);

        if (nextFilter == null) {
            throw new NullPointerException("nextFilter");
        }
        this.nextFilter = nextFilter;
    }

    public NextFilter getNextFilter() {
        return nextFilter;
    }

    @Override
    public void fire() {
        IoSession session = getSession();
        NextFilter nextFilter = getNextFilter();
        IoEventType type = getType();

        if ( LOGGER.isDebugEnabled()) {
            LOGGER.debug( "Firing a {} event for session {}",type, session.getId() );
        }

        switch (type) {
        case MESSAGE_RECEIVED:
            Object parameter = getParameter();
            nextFilter.messageReceived(session, parameter);
            break;
        case MESSAGE_SENT:
            WriteRequest writeRequest = (WriteRequest)getParameter();
            nextFilter.messageSent(session, writeRequest);
            break;
        case WRITE:
            writeRequest = (WriteRequest)getParameter();
            nextFilter.filterWrite(session, writeRequest);
            break;
        case CLOSE:
            nextFilter.filterClose(session);
            break;
        case EXCEPTION_CAUGHT:
            Throwable throwable = (Throwable)getParameter();
            nextFilter.exceptionCaught(session, throwable);
            break;
        case SESSION_IDLE:
            nextFilter.sessionIdle(session, (IdleStatus) getParameter());
            break;
        case SESSION_OPENED:
            nextFilter.sessionOpened(session);
            break;
        case SESSION_CREATED:
            nextFilter.sessionCreated(session);
            break;
        case SESSION_CLOSED:
            nextFilter.sessionClosed(session);
            break;
        default:
            throw new IllegalArgumentException("Unknown event type: " + type);
        }
        
        if ( LOGGER.isDebugEnabled()) {
            LOGGER.debug( "Event {} has been fired for session {}", type, session.getId() );
        }
    }
}
