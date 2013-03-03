/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSessionInitializer;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

import com.kaazing.mina.core.session.AbstractIoSessionEx;

public class IoSessionChannelHandler extends SimpleChannelHandler {

    private static ThreadLocal<ReadIdleTracker> READ_IDLE_TRACKER = new ThreadLocal<ReadIdleTracker>() {
		@Override
		protected ReadIdleTracker initialValue() {
			return new ReadIdleTracker();
		}
    };

    private final ChannelIoSession session;
    private final IoFilterChain filterChain;
	private final IoFuture future;
	private final IoSessionInitializer<?> initializer;
	private final ReadIdleTracker idleTracker;
	
	public IoSessionChannelHandler(ChannelIoSession session, IoFuture future, IoSessionInitializer<?> initializer) {
		this.session = session;
		this.filterChain = session.getFilterChain();
		this.future = future;
		this.initializer = initializer;
		this.idleTracker = READ_IDLE_TRACKER.get();
	}

	@Override
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e)
	throws Exception {
		session.getService().initializeSession(session, future, initializer);
		idleTracker.addSession(session);
		session.getProcessor().add(session);
	}
	
	@Override
	public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e)
			throws Exception {
		session.close(true);
		idleTracker.removeSession(session);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
			throws Exception {
		filterChain.fireExceptionCaught(e.getCause());
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
			throws Exception {
		Object message = e.getMessage();
		if (message instanceof ChannelBuffer) {
			ChannelBuffer buf = (ChannelBuffer)message;
			message = IoBuffer.wrap(buf.toByteBuffer());
			buf.skipBytes(buf.readableBytes());
		}
		filterChain.fireMessageReceived(message);
	}

	private static class ReadIdleTracker {
	    private static final long IDLE_TIMEOUT_PRECISION_MILLIS = 100L;
	    private static long QUASI_IMMEDIATE_MILLIS = 10;

	    // auto-reaping scheduler Thread when no more live references to Timer
	    private final Timer timer;
	    private final NotifyReadIdleSessionsTask timerTask;

	    private ReadIdleTracker() {
	        timer = new Timer(String.format("%s - idleTimeout", Thread.currentThread().getName(),
	                          true)); // use daemon thread so it doesn't prevent application shutdown
	        timerTask = new NotifyReadIdleSessionsTask();
	        timer.scheduleAtFixedRate(timerTask, IDLE_TIMEOUT_PRECISION_MILLIS, IDLE_TIMEOUT_PRECISION_MILLIS);
	    }
	    
	    private void addSession(final AbstractIoSessionEx session) {
	        timer.schedule(new TimerTask() {
	            @Override
	            public void run() {
	                timerTask.addSession0(session);
	            }
	        }, QUASI_IMMEDIATE_MILLIS);
	    }
	    
	    private void removeSession(final AbstractIoSessionEx session) {
	        timer.schedule(new TimerTask() {
	            @Override
	            public void run() {
	                timerTask.removeSession0(session);
	            }
	        }, QUASI_IMMEDIATE_MILLIS);
	    }

	    private static class NotifyReadIdleSessionsTask extends TimerTask {
	    	
	        // Only accessed from timer thread
	        private final Collection<AbstractIoSessionEx> sessions;
	    	
	        private NotifyReadIdleSessionsTask() {
	    		this.sessions = new LinkedList<AbstractIoSessionEx>();
	    	}
	    	
		    @Override
		    public void run() {
		        if (sessions.size() == 0) {
		            return; // no sessions left, stop running
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
	
}
