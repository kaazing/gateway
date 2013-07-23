/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.concurrent.TimeUnit;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.session.IdleStatus;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.Timer;
import org.jboss.netty.util.TimerTask;

import com.kaazing.mina.core.session.AbstractIoSessionEx;
import com.kaazing.mina.core.session.IoSessionConfigEx;
import com.kaazing.mina.core.session.IoSessionConfigEx.ChangeListener;
import com.kaazing.mina.core.session.IoSessionEx;

final class DefaultIoSessionIdleTracker implements IoSessionIdleTracker {

    static final long PRECISION = 100L;

    private final Timer timer;

    DefaultIoSessionIdleTracker() {
        timer = new HashedWheelTimer(PRECISION, MILLISECONDS);
    }

    @Override
    public void addSession(final AbstractIoSessionEx session) {
        IoSessionConfigEx config = session.getConfig();
        config.setChangeListener(new NotifyIdleChangeListener(session));
    }

    @Override
    public void removeSession(final AbstractIoSessionEx session) {
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

    private abstract class NotifyIdleTask implements TimerTask {

        protected final IoSessionEx session;
        protected final IoFilterChain filterChain;

        private volatile long idleTimeMillis;
        private volatile Timeout timeout;

        public NotifyIdleTask(IoSessionEx session) {
            this.session = session;
            this.filterChain = session.getFilterChain();
        }

        public final void reschedule(long idleTime, TimeUnit unit)  {
            idleTimeMillis = unit.toMillis(idleTime);
            long startPoint = Math.max(getLastIoTimeMillis(), getLastIdleTimeMillis());
            long delayMillis = startPoint + idleTimeMillis - currentTimeMillis();
            reschedule(delayMillis);
        }

        private void reschedule(long delayMillis) {
            if (timeout != null) {
                timeout.cancel();
            }

            if (idleTimeMillis != 0) {
                timeout = timer.newTimeout(this, delayMillis, MILLISECONDS);
            }
            else {
                timeout = null;
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
                fireSessionIdle(filterChain);
                reschedule(idleTimeMillis);
            }
            else {
                // An intervening I/O means we should not fire session idle, but we must reschedule to ensure accuracy
                // of when we do fire sessionIdle.
                reschedule(timeUntilSessionIdle);
            }
        }

        protected abstract void fireSessionIdle(IoFilterChain filterChain);

        protected abstract long getLastIoTimeMillis();

        protected abstract long getLastIdleTimeMillis();

    }

    private final class NotifyBothIdleTask extends NotifyIdleTask {

        public NotifyBothIdleTask(IoSessionEx session) {
            super(session);
        }

        @Override
        protected void fireSessionIdle(IoFilterChain filterChain) {
            filterChain.fireSessionIdle(IdleStatus.BOTH_IDLE);
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
        protected void fireSessionIdle(IoFilterChain filterChain) {
            filterChain.fireSessionIdle(IdleStatus.READER_IDLE);
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
        protected void fireSessionIdle(IoFilterChain filterChain) {
            filterChain.fireSessionIdle(IdleStatus.WRITER_IDLE);
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
