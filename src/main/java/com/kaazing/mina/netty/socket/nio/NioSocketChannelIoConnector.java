/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.socket.nio;

import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioEventLoop;
import io.netty.channel.socket.nio.NioSocketChannel;

import org.apache.mina.transport.socket.SocketSessionConfig;

import com.kaazing.mina.netty.socket.SocketChannelIoConnector;

public class NioSocketChannelIoConnector extends SocketChannelIoConnector<NioEventLoop> {

	public NioSocketChannelIoConnector(SocketSessionConfig sessionConfig,
			NioEventLoop eventLoop) {
		super(sessionConfig, eventLoop);
	}

	@Override
	protected SocketChannel newChannel(NioEventLoop eventLoop) {
		return new NioSocketChannel();
	}

}
