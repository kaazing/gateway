/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.nio;

import java.util.concurrent.Executor;

import org.jboss.netty.channel.socket.Worker;
import org.jboss.netty.channel.socket.nio.NioSocketChannel;

import com.kaazing.mina.netty.ChannelIoService;
import com.kaazing.mina.netty.ChannelIoSession;

/**
 * This session is always used in conjunction with an NioSocketChannel, which necessarily has an associated worker.
 * It forces all operations of the session to be done in the worker thread (using worker.executeIntoThread if a call 
 * is made in another thread).
 */
public class NioSocketChannelIoSession extends ChannelIoSession {
	
	public NioSocketChannelIoSession(ChannelIoService service, NioSocketChannel channel) {
	    super(service, channel, Thread.currentThread(), new WorkerExecutor(channel));
        assert isIoThread(channel);
	}
	
	private static class WorkerExecutor implements Executor {
	    private Worker worker; 
	    
	    WorkerExecutor(NioSocketChannel channel) {
	        this.worker = channel.getWorker();
	    }

        @Override
        public void execute(Runnable command) {
            worker.executeInIoThread(command);
        }
	}
	
	private boolean isIoThread(NioSocketChannel channel) {
	    Worker worker = channel.getWorker();
	    final Thread[] ioThread = new Thread[]{null};
	    worker.executeInIoThread(new Runnable() {
            @Override
            public void run() {
                ioThread[0] = Thread.currentThread();                
            }	        
	    });
	    boolean matches = ioThread[0] == Thread.currentThread();
	    assert matches : String.format("Current thread %s does not match I/O thread %s", Thread.currentThread(), ioThread[0]);
	    return matches;
	}

}
