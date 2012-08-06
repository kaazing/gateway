/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.socket.nio;

import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioEventLoop;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import org.apache.mina.transport.socket.DefaultSocketSessionConfig;
import org.apache.mina.transport.socket.SocketSessionConfig;

import com.kaazing.mina.netty.socket.SocketChannelIoAcceptor;

public class NioSocketChannelIoAcceptor extends SocketChannelIoAcceptor<NioEventLoop> {

	public NioSocketChannelIoAcceptor() {
		this(new NioEventLoop());
	}
	
	public NioSocketChannelIoAcceptor(NioEventLoop childEventLoop) {
		this(new DefaultSocketSessionConfig(), new NioEventLoop(), childEventLoop);
	}

	public NioSocketChannelIoAcceptor(SocketSessionConfig sessionConfig,
			NioEventLoop parentEventLoop, NioEventLoop childEventLoop) {
		super(sessionConfig, parentEventLoop, childEventLoop);
	}

	@Override
	protected ServerSocketChannel newServerChannel(NioEventLoop parentEventLoop, NioEventLoop childEventLoop) {
		return new NioServerSocketChannel();
	}

}
