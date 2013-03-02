/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.mina.core.session.IdleStatus;

import com.kaazing.mina.core.session.AbstractIoSessionEx;
import com.kaazing.mina.core.session.IoSessionConfigEx;

public class IdleTracker {
    private static final long IDLE_TIMEOUT_PRECISION_MILLIS = 100L;
    private static long QUASI_IMMEDIATE_MILLIS = 10;

    // auto-reaping scheduler Thread when no more live references to Timer
    private final Timer timer;
    private final NotifyReadIdleSessionsTask timerTask;

    IdleTracker() {
        timer = new Timer(String.format("%s - idleTimeout", Thread.currentThread().getName(),
                          true)); // use daemon thread so it doesn't prevent application shutdown
        timerTask = new NotifyReadIdleSessionsTask();
        timer.scheduleAtFixedRate(timerTask, IDLE_TIMEOUT_PRECISION_MILLIS, IDLE_TIMEOUT_PRECISION_MILLIS);
    }
    
    public void addSession(final AbstractIoSessionEx session) {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                timerTask.addSession0(session);
            }
        }, QUASI_IMMEDIATE_MILLIS);
    }
    
    public void removeSession(final AbstractIoSessionEx session) {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                timerTask.removeSession0(session);
            }
        }, QUASI_IMMEDIATE_MILLIS);
    }

    private class NotifyReadIdleSessionsTask extends TimerTask {
    	
        // Only accessed from timer thread
        private final Collection<AbstractIoSessionEx> sessions;
    	
    	public NotifyReadIdleSessionsTask() {
    		this.sessions = new LinkedList<AbstractIoSessionEx>();
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
