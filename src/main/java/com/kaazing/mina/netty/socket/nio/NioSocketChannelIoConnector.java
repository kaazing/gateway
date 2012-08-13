/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.socket.nio;

import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import org.apache.mina.transport.socket.DefaultSocketSessionConfig;
import org.apache.mina.transport.socket.SocketSessionConfig;

import com.kaazing.mina.netty.socket.SocketChannelIoConnector;

public class NioSocketChannelIoConnector extends SocketChannelIoConnector<NioEventLoopGroup> {

	public NioSocketChannelIoConnector() {
		this(new NioEventLoopGroup());
	}
	
	public NioSocketChannelIoConnector(NioEventLoopGroup group) {
		this(new DefaultSocketSessionConfig(), group);
	}

	public NioSocketChannelIoConnector(SocketSessionConfig sessionConfig,
			NioEventLoopGroup group) {
		super(sessionConfig, group);
	}

	@Override
	protected SocketChannel newChannel(NioEventLoopGroup group) {
		return new NioSocketChannel();
	}

}
