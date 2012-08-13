/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.socket.aio;

import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.aio.AioEventLoopGroup;
import io.netty.channel.socket.aio.AioSocketChannel;

import org.apache.mina.transport.socket.SocketSessionConfig;

import com.kaazing.mina.netty.socket.SocketChannelIoConnector;

public class AioSocketChannelIoConnector extends SocketChannelIoConnector<AioEventLoopGroup> {

	public AioSocketChannelIoConnector(SocketSessionConfig sessionConfig,
			AioEventLoopGroup eventLoop) {
		super(sessionConfig, eventLoop);
	}

	@Override
	protected SocketChannel newChannel(AioEventLoopGroup group) {
		return new AioSocketChannel(group);
	}

}
