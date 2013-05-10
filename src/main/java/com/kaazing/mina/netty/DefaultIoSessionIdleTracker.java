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
        protected final IdleStatus idleStatus;

        private volatile long idleTimeMillis;
        private volatile Timeout timeout;

        public NotifyIdleTask(IoSessionEx session, IdleStatus idleStatus) {
            this.session = session;
            this.idleStatus = idleStatus;
        }

        public final void reschedule(long idleTime, TimeUnit unit)  {

            idleTimeMillis = unit.toMillis(idleTime);

            reschedule();
        }

        protected final void reschedule() {
            if (timeout != null) {
                timeout.cancel();
            }

            if (idleTimeMillis != 0) {
                long baseline = Math.max(getLastIoTimeMillis(), getLastIdleTimeMillis());
                long delayMillis = baseline + idleTimeMillis - currentTimeMillis();
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
            else if (idleTimeMillis != 0 && currentTimeMillis() - getLastIoTimeMillis() >= idleTimeMillis) {
                IoFilterChain filterChain = session.getFilterChain();
                filterChain.fireSessionIdle(idleStatus);
            }
            else {
                // An intervening event (e.g. messageReceived) meant we should no longer fire session idle.
                // But we must reschedule to ensure accuracy of when we'll next fire sessionIdle.
                reschedule();
            }
        }

        protected abstract long getLastIoTimeMillis();

        private long getLastIdleTimeMillis() {
            return session.getLastIdleTime(idleStatus);
        }

    }

    private final class NotifyBothIdleTask extends NotifyIdleTask {

        public NotifyBothIdleTask(IoSessionEx session) {
            super(session, IdleStatus.BOTH_IDLE);
        }

        protected long getLastIoTimeMillis() {
            return session.getLastIoTime();
        }

    }

    private final class NotifyReaderIdleTask extends NotifyIdleTask {

        public NotifyReaderIdleTask(IoSessionEx session) {
            super(session, IdleStatus.READER_IDLE);
        }

        protected long getLastIoTimeMillis() {
            return session.getLastReadTime();
        }

    }

    private final class NotifyWriterIdleTask extends NotifyIdleTask {

        public NotifyWriterIdleTask(IoSessionEx session) {
            super(session, IdleStatus.WRITER_IDLE);
        }

        protected long getLastIoTimeMillis() {
            return session.getLastWriteTime();
        }

    }
}
