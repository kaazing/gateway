/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.mina.core.session.IdleStatus;

import com.kaazing.mina.core.session.AbstractIoSessionEx;

final class DefaultIoSessionIdleTracker implements IoSessionIdleTracker {
    private static final long IDLE_TIMEOUT_PRECISION_MILLIS = 100L;
    private static long QUASI_IMMEDIATE_MILLIS = 10;

    // auto-reaping scheduler Thread when no more live references to Timer
    private final Timer timer;
    private final DefaultIoSessionIdleTracker.NotifyReadIdleSessionsTask timerTask;

    DefaultIoSessionIdleTracker() {
        timer = new Timer(String.format("%s - idleTimeout", Thread.currentThread().getName(),
                          true)); // use daemon thread so it doesn't prevent application shutdown
        timerTask = new NotifyReadIdleSessionsTask();
        timer.scheduleAtFixedRate(timerTask, IDLE_TIMEOUT_PRECISION_MILLIS, IDLE_TIMEOUT_PRECISION_MILLIS);
    }

    @Override
    public void addSession(final AbstractIoSessionEx session) {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                timerTask.addSession0(session);
            }
        }, QUASI_IMMEDIATE_MILLIS);
    }

    @Override
    public void dispose() {
        final CountDownLatch disposed = new CountDownLatch(1);
        // Cancel the timer from a timer task to guarantee that's the last thing the timer thread does
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                timer.cancel();
                disposed.countDown();
            }
        }, QUASI_IMMEDIATE_MILLIS);
        try {
            // allow more than the expected interval in case there's heavy CPU usage
            if (!disposed.await(10000, TimeUnit.MILLISECONDS)) {
                    assert false : "DefaultIoSessionIdleTracker.dispose: timer task has not disposed";
                }
            }
        catch (InterruptedException e) {
            return;
        }
    }

    @Override
    public void removeSession(final AbstractIoSessionEx session) {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                timerTask.removeSession0(session);
            }
        }, QUASI_IMMEDIATE_MILLIS);
    }

    private static final class NotifyReadIdleSessionsTask extends TimerTask {

        // Only accessed from timer thread
        private final Collection<AbstractIoSessionEx> sessions;

        private NotifyReadIdleSessionsTask() {
            this.sessions = new LinkedList<AbstractIoSessionEx>();
        }

        @Override
        public void run() {
            if (sessions.size() == 0) {
                return; // no sessions left
            }
            // TailFilter in DefaultIoFilterChain(Ex) maintains last read time for us on each session
            long currentTime = System.currentTimeMillis();
            for (AbstractIoSessionEx session : sessions) {
                long readerIdleTimeout = session.getConfig().getReaderIdleTimeInMillis();
                if (readerIdleTimeout > 0) {
                    long sinceLastRead = currentTime - session.getLastReadTime();
                    if (sinceLastRead >  readerIdleTimeout) {
                        session.getFilterChain().fireSessionIdle(IdleStatus.READER_IDLE);
                    }
                }
            }
        }

        // Must be executed from timer thread
        private void addSession0(AbstractIoSessionEx session) {
            sessions.add(session);
        }

        // Must be executed from timer thread
        private void removeSession0(AbstractIoSessionEx session) {
            sessions.remove(session);
        }

    }
}
