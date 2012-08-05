/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.socket.aio;

import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.aio.AioEventLoop;
import io.netty.channel.socket.aio.AioSocketChannel;

import org.apache.mina.transport.socket.SocketSessionConfig;

import com.kaazing.mina.netty.socket.SocketChannelIoConnector;

public class AioSocketChannelIoConnector extends SocketChannelIoConnector<AioEventLoop> {

	public AioSocketChannelIoConnector(SocketSessionConfig sessionConfig,
			AioEventLoop eventLoop) {
		super(sessionConfig, eventLoop);
	}

	@Override
	protected SocketChannel newChannel(AioEventLoop eventLoop) {
		return new AioSocketChannel(eventLoop);
	}

}
