/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;

import java.net.SocketAddress;

import org.apache.mina.core.filterchain.DefaultIoFilterChain;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoProcessor;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.AbstractIoSession;
import org.apache.mina.core.session.IoSessionConfig;

public class ChannelIoSession extends AbstractIoSession {

	private final ChannelIoService service;
	private final Channel channel;
	private final ChannelIoSessionConfig<?> config;
	private final IoHandler handler;
	private final ChannelIoProcessor processor;
	private final IoFilterChain filterChain;
	private final TransportMetadata transportMetadata;
	
	public ChannelIoSession(ChannelIoService service, Channel channel) {
		this.service = service;
		this.channel = channel;
		this.config = new ChannelIoSessionConfig<ChannelConfig>(channel.config());
        this.config.setAll(service.getSessionConfig());
        this.handler = service.getHandler();
		this.processor = new ChannelIoProcessor();
		this.filterChain = new DefaultIoFilterChain(this);
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
	public IoFilterChain getFilterChain() {
		return filterChain;
	}

	@Override
	public SocketAddress getLocalAddress() {
		return channel.localAddress();
	}

	@Override
	public SocketAddress getRemoteAddress() {
		return channel.remoteAddress();
	}

	@Override
	public TransportMetadata getTransportMetadata() {
		return transportMetadata;
	}

}
