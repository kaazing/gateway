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
package org.kaazing.mina.netty;

import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.concurrent.TimeUnit;

import org.apache.mina.core.session.IdleStatus;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.Timer;
import org.jboss.netty.util.TimerTask;

import org.kaazing.mina.core.session.IoSessionConfigEx;
import org.kaazing.mina.core.session.IoSessionConfigEx.ChangeListener;
import org.kaazing.mina.core.session.IoSessionEx;

public final class DefaultIoSessionIdleTracker implements IoSessionIdleTracker {

    static final long PRECISION = 100L;

    private final Timer timer;

    public DefaultIoSessionIdleTracker() {
        timer = new HashedWheelTimer(PRECISION, MILLISECONDS);
    }

    @Override
    public void addSession(final IoSessionEx session) {
        IoSessionConfigEx config = session.getConfig();
        config.setChangeListener(new NotifyIdleChangeListener(session));
    }

    @Override
    public void removeSession(final IoSessionEx session) {
        IoSessionConfigEx config = session.getConfig();
        config.setChangeListener(null);
    }

    @Override
    public void dispose() {
        timer.stop();
    }

    private final class NotifyIdleChangeListener implements ChangeListener {

        private final NotifyIdleTask notifyBothIdle;
        private final NotifyIdleTask notifyReaderIdle;
        private final NotifyIdleTask notifyWriterIdle;

        NotifyIdleChangeListener(IoSessionEx session) {
            notifyBothIdle = new NotifyBothIdleTask(session);
            notifyReaderIdle = new NotifyReaderIdleTask(session);
            notifyWriterIdle = new NotifyWriterIdleTask(session);
        }

        @Override
        public void idleTimeInMillisChanged(IdleStatus status, long idleTimeMillis) {

            if (status == IdleStatus.BOTH_IDLE) {
                notifyBothIdle.reschedule(idleTimeMillis, MILLISECONDS);
            }
            else if (status == IdleStatus.READER_IDLE) {
                notifyReaderIdle.reschedule(idleTimeMillis, MILLISECONDS);
            }
            else if (status == IdleStatus.WRITER_IDLE) {
                notifyWriterIdle.reschedule(idleTimeMillis, MILLISECONDS);
            }
            else {
                throw new IllegalArgumentException("Unrecognized idle status: " + status);
            }
        }
    }

    // KG-9954: This class will be used to assign a noop timer in place of using null
    private static final Timeout NULL_TIMEOUT = new Timeout() {
        @Override
        public Timer getTimer() {
            return null;
        }

        @Override
        public TimerTask getTask() {
            return null;
        }

        @Override
        public boolean isExpired() {
            return true;
        }

        @Override
        public boolean isCancelled() {
            return true;
        }

        @Override
        public void cancel() {
            // noop
        }
    };

    private abstract class NotifyIdleTask implements TimerTask {

        protected final IoSessionEx session;

        private volatile long idleTimeMillis;
        private volatile Timeout timeout = NULL_TIMEOUT;

        public NotifyIdleTask(IoSessionEx session) {
            this.session = session;
        }

        public final void reschedule(long idleTime, TimeUnit unit)  {
            idleTimeMillis = unit.toMillis(idleTime);
            long startPoint = Math.max(getLastIoTimeMillis(), getLastIdleTimeMillis());
            long delayMillis = startPoint + idleTimeMillis - currentTimeMillis();
            reschedule(delayMillis);
        }

        private void reschedule(long delayMillis) {
            // KG-9954 - We do not want to synchronize the reschedule method, instead use the NULL Timeout in case
            // timeout gets set to null by another thread before we call cancel.
            if (timeout != NULL_TIMEOUT) {
                timeout.cancel();
            }

            if (idleTimeMillis != 0) {
                timeout = timer.newTimeout(this, delayMillis, MILLISECONDS);
            }
            else {
                timeout = NULL_TIMEOUT;
            }
        }

        @Override
        public final void run(Timeout timeout) throws Exception {
            if (timeout.isCancelled()) {
                // reschedule has already been done, don't incur the overhead of redoing it
                return;
            }

            long startPoint = Math.max(getLastIoTimeMillis(), getLastIdleTimeMillis());
            // Doing the calculation here avoids having to call currentTimeMillis twice (here and in reschedule).
            // Given that the precision of timeout is limited, and that lastIdleTime is only updated if idle is fired, we must
            // always call currentTimeMillis(). For example, imagine session idle last fired at t0. Timeout will occur at or
            // after t0 + configured idleTime. Even if an I/O event occurred after t0, we may still need to fire sessionIdle.
            long timeUntilSessionIdle = startPoint + idleTimeMillis - currentTimeMillis();
            if (timeUntilSessionIdle <= 0 && idleTimeMillis != 0) {
                if (session.getIoThread() != IoSessionEx.NO_THREAD && !session.isClosing()) {
                    fireSessionIdle(session);
                }
                reschedule(idleTimeMillis);
            }
            else {
                // An intervening I/O means we should not fire session idle, but we must reschedule to ensure accuracy
                // of when we do fire sessionIdle.
                reschedule(timeUntilSessionIdle);
            }
        }

        // Passing session as parameter as filter chain needs to be re-accessed
        // (due to potential re-alignement and session gets a new filter chain)
        protected abstract void fireSessionIdle(IoSessionEx session);

        protected abstract long getLastIoTimeMillis();

        protected abstract long getLastIdleTimeMillis();

    }

    private final class NotifyBothIdleTask extends NotifyIdleTask {

        public NotifyBothIdleTask(IoSessionEx session) {
            super(session);
        }

        @Override
        protected void fireSessionIdle(IoSessionEx session) {
            session.getFilterChain().fireSessionIdle(IdleStatus.BOTH_IDLE);
        }

        @Override
        protected long getLastIdleTimeMillis() {
            return session.getLastIdleTime(IdleStatus.BOTH_IDLE);
        }

        @Override
        protected long getLastIoTimeMillis() {
            return session.getLastIoTime();
        }

    }

    private final class NotifyReaderIdleTask extends NotifyIdleTask {

        public NotifyReaderIdleTask(IoSessionEx session) {
            super(session);
        }

        @Override
        protected void fireSessionIdle(IoSessionEx session) {
            session.getFilterChain().fireSessionIdle(IdleStatus.READER_IDLE);
        }

        @Override
        protected long getLastIdleTimeMillis() {
            return session.getLastIdleTime(IdleStatus.READER_IDLE);
        }

        @Override
        protected long getLastIoTimeMillis() {
            return session.getLastReadTime();
        }

    }

    private final class NotifyWriterIdleTask extends NotifyIdleTask {

        public NotifyWriterIdleTask(IoSessionEx session) {
            super(session);
        }

        @Override
        protected void fireSessionIdle(IoSessionEx session) {
            session.getFilterChain().fireSessionIdle(IdleStatus.WRITER_IDLE);
        }

        @Override
        protected long getLastIdleTimeMillis() {
            return session.getLastIdleTime(IdleStatus.WRITER_IDLE);
        }

        @Override
        protected long getLastIoTimeMillis() {
            return session.getLastWriteTime();
        }

    }
}
