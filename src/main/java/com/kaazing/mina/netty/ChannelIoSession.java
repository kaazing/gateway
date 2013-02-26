/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import java.net.SocketAddress;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoProcessor;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.AbstractIoSession;
import org.apache.mina.core.session.IoSessionConfig;
import org.jboss.netty.channel.Channel;

import com.kaazing.mina.core.filterchain.DefaultIoFilterChainEx;
import com.kaazing.mina.core.session.AbstractIoSessionEx;

public class ChannelIoSession extends AbstractIoSessionEx {

	private final ChannelIoService service;
	private final Channel channel;
	private final DefaultChannelIoSessionConfig config;
	private final IoHandler handler;
	private final ChannelIoProcessor processor;
	private final TransportMetadata transportMetadata;
	
	public ChannelIoSession(ChannelIoService service, Channel channel) {
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

}
