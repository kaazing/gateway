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
package org.kaazing.gateway.transport.ws;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IoSession;

/**
 * Any command action on a WebSocket session can benefit from
 * the standard {@link #schedule(java.util.concurrent.ScheduledExecutorService, long, java.util.concurrent.TimeUnit)}
 * and {@link #cancel(java.util.concurrent.ScheduledFuture)} methods in this abstract class.
 * <p/>
 * Implementations are responsible for implementing methods to provide a session
 * attribute key to access the scheduled future object, and for clearing the command when the session ends.
 *
 */
public abstract class WsScheduledCommand implements Runnable {

    /**
     * Take action at the beginning of the command's lifetime.
     */
    public void startup() {
    }


    /**
     * Take action at the end of the command's lifetime.
     */
    public void shutdown() {
    }

    /**
     * Cancel the scheduled command.
     *
     * @param scheduledFuture the scheduled future used to schedule the command.
     */
    public void cancel(ScheduledFuture<?> scheduledFuture) {
        clear();
        if (scheduledFuture != null && !scheduledFuture.isDone()) {
            scheduledFuture.cancel(false);
        }
    }

    /**
     * Clear the scheduled command when the session ends or when the command is cancelled.
     */
    protected abstract void clear();

    /**
     *
     * @return an attribute key under which a session may store scheduled future for this command.
     */
    public abstract AttributeKey getScheduledFutureKey();

    /**
     * Schedule this command using the scheduler, delay and time units provided.
     * @param scheduler the scheduler for scheduling this command
     * @param delay the delay
     * @param unit the time units for the delay
     * @return a scheduled future representing the scheduled command.
     */
    public ScheduledFuture<?> schedule(ScheduledExecutorService scheduler,
                                       long delay, final TimeUnit unit) {
            return scheduler.schedule(
                    this,
                    delay, unit);
    }

    protected void addCommandFilter(IoSession session, String filterName, IoFilter filter) {
        session.getFilterChain().addLast(filterName, filter);
    }

    protected void removeCommandFilter(IoSession session, IoFilter filter) {
        final IoFilterChain filterChain = session.getFilterChain();
        if (filterChain.contains(filter)) {
            try {
                filterChain.remove(filter);
            } catch (IllegalArgumentException e) {
                // the filter was removed by another thread.  ok.
            }
        }
    }

}
