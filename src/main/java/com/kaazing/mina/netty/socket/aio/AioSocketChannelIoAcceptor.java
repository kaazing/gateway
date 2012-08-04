/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.socket.aio;

import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.aio.AioEventLoop;
import io.netty.channel.socket.aio.AioServerSocketChannel;

import org.apache.mina.transport.socket.SocketSessionConfig;

import com.kaazing.mina.netty.socket.SocketChannelIoAcceptor;

public class AioSocketChannelIoAcceptor extends SocketChannelIoAcceptor<AioEventLoop> {

	public AioSocketChannelIoAcceptor(SocketSessionConfig sessionConfig,
			AioEventLoop parentEventLoop, AioEventLoop childEventLoop) {
		super(sessionConfig, parentEventLoop, childEventLoop);
	}

	@Override
	protected ServerSocketChannel newServerChannel(AioEventLoop parentEventLoop) {
		return new AioServerSocketChannel(parentEventLoop);
	}

}
