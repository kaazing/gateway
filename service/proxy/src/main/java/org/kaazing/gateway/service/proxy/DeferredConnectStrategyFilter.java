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

import java.util.concurrent.atomic.AtomicReference;

import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;

public final class DeferredConnectStrategyFilter extends IoFilterAdapter {

    private enum State { READY, OBSERVED, FLUSHED }

    private final AtomicReference<State> sessionOpened = new AtomicReference<>(State.READY);

    @Override
    public void sessionOpened(
        NextFilter nextFilter,
        IoSession session) throws Exception
    {
        sessionOpened.compareAndSet(State.READY, State.OBSERVED);
    }

    @Override
    public void messageReceived(
        NextFilter nextFilter,
        IoSession session,
        Object message) throws Exception
    {
        flushSessionOpenedIfNecessary(nextFilter, session);
        super.messageReceived(nextFilter, session, message);
    }

    @Override
    public void messageSent(
        NextFilter nextFilter,
        IoSession session,
        WriteRequest writeRequest) throws Exception
    {
        flushSessionOpenedIfNecessary(nextFilter, session);
        super.messageSent(nextFilter, session, writeRequest);
    }

    @Override
    public void sessionIdle(
        NextFilter nextFilter,
        IoSession session,
        IdleStatus status) throws Exception
    {
        flushSessionOpenedIfNecessary(nextFilter, session);
        super.sessionIdle(nextFilter, session, status);
    }

    @Override
    public void sessionClosed(
        NextFilter nextFilter,
        IoSession session) throws Exception
    {
        flushSessionOpenedIfNecessary(nextFilter, session);
        super.sessionClosed(nextFilter, session);
    }

    @Override
    public void exceptionCaught(
        NextFilter nextFilter,
        IoSession session,
        Throwable cause) throws Exception
    {
        flushSessionOpenedIfNecessary(nextFilter, session);
        super.exceptionCaught(nextFilter, session, cause);
    }

    private void flushSessionOpenedIfNecessary(
        NextFilter nextFilter,
        IoSession session) throws Exception
    {
        if (sessionOpened.compareAndSet(State.OBSERVED, State.FLUSHED)) {
            super.sessionOpened(nextFilter, session);
            session.getFilterChain().remove(this);
        }
    }
}
