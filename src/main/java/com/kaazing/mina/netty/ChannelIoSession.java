/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import java.net.SocketAddress;
import java.util.concurrent.Executor;

import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoProcessor;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.IoSessionConfig;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.socket.Worker;
import org.jboss.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kaazing.mina.core.session.AbstractIoSessionEx;

public class ChannelIoSession extends AbstractIoSessionEx {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChannelIoSession.class);
    
	private final ChannelIoService service;
	private final Channel channel;
	private final DefaultChannelIoSessionConfig config;
	private final IoHandler handler;
	private final ChannelIoProcessor processor;
	private final TransportMetadata transportMetadata;
	
	public ChannelIoSession(ChannelIoService service, Channel channel, Thread ioThread, Executor ioExecutor) {
	    super(ioThread, ioExecutor);
		this.service = service;
		this.channel = channel;
		this.config = new DefaultChannelIoSessionConfig(channel.getConfig());
        this.config.setAll(service.getSessionConfig());
        this.handler = service.getHandler();
		this.processor = new ChannelIoProcessor();
		this.transportMetadata = service.getTransportMetadata();
	}

	public ChannelIoService getService() {
		return service;
	}
	
	public Channel getChannel() {
		return channel;
	}
	
	@Override
	public IoHandler getHandler() {
		return handler;
	}

	@Override
	public IoSessionConfig getConfig() {
		return config;
	}

	@Override
	public IoProcessor<ChannelIoSession> getProcessor() {
		return processor;
	}

	@Override
	public SocketAddress getLocalAddress() {
		return channel.getLocalAddress();
	}

	@Override
	public SocketAddress getRemoteAddress() {
		return channel.getRemoteAddress();
	}

	@Override
	public TransportMetadata getTransportMetadata() {
		return transportMetadata;
	}
	
	private static Executor getWorkerExecutor(Channel channel) {
	    if (!(channel instanceof NioSocketChannel)) {
	        String message = String.format("Unable to get worker: channel %s is not an instance of NioSocketChannel");
	        RuntimeException e = new UnsupportedOperationException(message); 
	        LOGGER.error(message, e);
	        throw e;
	    }
        NioSocketChannel socketChannel = (NioSocketChannel)channel;
        Worker worker = socketChannel.getWorker();
        return new WorkerExecutor(worker);
	}
	
	private static class WorkerExecutor implements Executor {
	    private Worker worker; 
	    
	    WorkerExecutor(Worker worker) {
	        this.worker = worker;
	    }

        @Override
        public void execute(Runnable command) {
            worker.executeInIoThread(command);
        }
	}

}
