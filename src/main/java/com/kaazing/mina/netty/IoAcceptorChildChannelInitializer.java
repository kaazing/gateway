/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import io.netty.buffer.ChannelBufType;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;

public class IoAcceptorChildChannelInitializer<T extends Channel> extends ChannelInitializer<T> {

	private final ChannelIoService acceptor;
	private final ChannelBufType bufType;
	
	public IoAcceptorChildChannelInitializer(ChannelIoService acceptor, ChannelBufType bufType) {
		this.acceptor = acceptor;
		this.bufType = bufType;
	}
	
	@Override
	public void initChannel(T childChannel) throws Exception {
		ChannelPipeline childPipeline = childChannel.pipeline();

		ChannelIoSession session = new ChannelIoSession(acceptor, childChannel);
		childPipeline.addLast("session", new IoSessionChannelHandler(session, bufType));
	}

}
