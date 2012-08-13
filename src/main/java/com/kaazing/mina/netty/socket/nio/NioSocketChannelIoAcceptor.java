/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.socket.nio;

import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import org.apache.mina.transport.socket.DefaultSocketSessionConfig;
import org.apache.mina.transport.socket.SocketSessionConfig;

import com.kaazing.mina.netty.socket.SocketChannelIoAcceptor;

public class NioSocketChannelIoAcceptor extends SocketChannelIoAcceptor<NioEventLoopGroup> {

	public NioSocketChannelIoAcceptor() {
		this(new NioEventLoopGroup());
	}
	
	public NioSocketChannelIoAcceptor(NioEventLoopGroup childGroup) {
		this(new DefaultSocketSessionConfig(), new NioEventLoopGroup(), childGroup);
	}

	public NioSocketChannelIoAcceptor(SocketSessionConfig sessionConfig,
			NioEventLoopGroup parentGroup, NioEventLoopGroup childGroup) {
		super(sessionConfig, parentGroup, childGroup);
	}

	@Override
	protected ServerSocketChannel newServerChannel(NioEventLoopGroup parentGroup, NioEventLoopGroup childGroup) {
		return new NioServerSocketChannel();
	}

}
