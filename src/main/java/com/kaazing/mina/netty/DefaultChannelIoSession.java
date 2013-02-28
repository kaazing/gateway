/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import java.util.concurrent.Executor;

import org.jboss.netty.channel.Channel;

/**
 * This session will throw runtime exceptions if any operation is attempted on the session in a thread other than the
 * thread that created the session.
 */
public class DefaultChannelIoSession extends ChannelIoSession {

	public DefaultChannelIoSession(ChannelIoService service, Channel channel) {
          super(service, channel, Thread.currentThread(), new CurrentThreadOnlyExecutor());
    }

    private static class CurrentThreadOnlyExecutor implements Executor {
        private Thread ownerThread = Thread.currentThread();
    
        @Override
        public void execute(Runnable command) {
            if (Thread.currentThread() != ownerThread) {
                throw new UnsupportedOperationException(
                        String.format("Current thread %s is different from session creator thread %s", 
                                      Thread.currentThread(), ownerThread) );
            }            
        }
    }

}
