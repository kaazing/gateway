/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.mina.core.session.IdleStatus;

import com.kaazing.mina.core.session.AbstractIoSessionEx;
import com.kaazing.mina.core.session.IoSessionConfigEx;

// TODO: Think about cleanup - how to stop the timer thread when gateway is destroyed
public class IdleTimeoutTask extends TimerTask {
    private static final long IDLE_TIMEOUT_PRECISION_MILLIS = 100L;
    private static long QUASI_IMMEDIATE_MILLIS = 10;
    
    private static ThreadLocal<IdleTimeoutTask> CURRENT_TASK = new ThreadLocal<IdleTimeoutTask>();
    
    private final Timer timer;

    // Only accessed from timer thread
    private final List<AbstractIoSessionEx> sessions = new LinkedList<AbstractIoSessionEx>();
    
    /**
     * This method must be called from within the I/O worker thread for the session
     */
    public static void add(AbstractIoSessionEx session) {
        assert Thread.currentThread() == session.getIoThread();
        IdleTimeoutTask current = CURRENT_TASK.get();
        if (current == null) {
            // Can't use session.getIoExecutor because that does AbstractNioWorker.executeIntoThread
            // which always wakes up the selector :( (instead of letting it wait 10 millis) so we would thrash!
            current = new IdleTimeoutTask();
            CURRENT_TASK.set(current);
        }
        current.addSession(session);
    }
    
    /**
     * This method must be called from within the I/O worker thread for the session
     */
    public static void remove(AbstractIoSessionEx session) {
        assert Thread.currentThread() == session.getIoThread();
        IdleTimeoutTask current = CURRENT_TASK.get();
        if (current != null) {
            current.removeSession(session);
        }
    }

    IdleTimeoutTask() {
        timer = new Timer(String.format("%s - idleTimeout", Thread.currentThread().getName(),
                          true)); // use daemon thread so it doesn't prevent application shutdown
        timer.scheduleAtFixedRate(this, IDLE_TIMEOUT_PRECISION_MILLIS, IDLE_TIMEOUT_PRECISION_MILLIS);
    }
    
    private void addSession(final AbstractIoSessionEx session) {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                addSession0(session);
            }
        }, QUASI_IMMEDIATE_MILLIS);
    }
    
    // Must be executed from timer thread
    private void addSession0(AbstractIoSessionEx session) {
        sessions.add(session);
    }

    private void removeSession(final AbstractIoSessionEx session) {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                removeSession0(session);
            }
        }, QUASI_IMMEDIATE_MILLIS);
    }

    // Must be executed from timer thread
    private void removeSession0(AbstractIoSessionEx session) {
        sessions.remove(session);
    }

    @Override
    public void run() {
        if (sessions.size() == 0) {
            return; // no sessions left, stop running
        }
        // TailFilter in DefaultIoFilterChain(Ex) maintains last read time for us on each session
        for (AbstractIoSessionEx session : sessions) {
            long sinceLastRead = System.currentTimeMillis() - session.getLastReadTime();
            long readerIdleTimeout = ((IoSessionConfigEx)session.getConfig()).getReaderIdleTimeInMillis();
            if (sinceLastRead >  readerIdleTimeout) {
                session.getFilterChain().fireSessionIdle(IdleStatus.READER_IDLE);
            }
        }
    }

}
