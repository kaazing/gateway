/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

/**
 * 
 */
package com.kaazing.mina.netty.socket;

import org.jboss.netty.channel.ChannelConfig;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.socket.ServerSocketChannel;
import org.jboss.netty.channel.socket.ServerSocketChannelFactory;

public final class IoAcceptorSocketChannelFactory implements ServerSocketChannelFactory {
	
	private final ServerSocketChannelFactory channelFactory;

	private int backlog = 50;
	private boolean reuseAddress = false;
	
	public IoAcceptorSocketChannelFactory(
			ServerSocketChannelFactory channelFactory) {
		this.channelFactory = channelFactory;
	}

	@Override
	public ServerSocketChannel newChannel(ChannelPipeline pipeline) {
		ServerSocketChannel channel = channelFactory.newChannel(pipeline);
		ChannelConfig channelConfig = channel.getConfig();
		channelConfig.setOption("reuseAddress", reuseAddress);
		channelConfig.setOption("backlog", backlog);
		return channel;
	}

	public int getBacklog() {
		return backlog;
	}

	public void setBacklog(int backlog) {
		this.backlog = backlog;
	}
	
	public boolean isReuseAddress() {
		return reuseAddress;
	}

	public void setReuseAddress(boolean reuseAddress) {
		this.reuseAddress = reuseAddress;
	}

	@Override
	public void releaseExternalResources() {
		channelFactory.releaseExternalResources();
	}
}