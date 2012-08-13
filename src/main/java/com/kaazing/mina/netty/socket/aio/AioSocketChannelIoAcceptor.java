/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.socket.aio;

import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.aio.AioEventLoopGroup;
import io.netty.channel.socket.aio.AioServerSocketChannel;

import org.apache.mina.transport.socket.SocketSessionConfig;

import com.kaazing.mina.netty.socket.SocketChannelIoAcceptor;

public class AioSocketChannelIoAcceptor extends SocketChannelIoAcceptor<AioEventLoopGroup> {

	public AioSocketChannelIoAcceptor(SocketSessionConfig sessionConfig,
			AioEventLoopGroup parentEventLoop, AioEventLoopGroup childEventLoop) {
		super(sessionConfig, parentEventLoop, childEventLoop);
	}

	@Override
	protected ServerSocketChannel newServerChannel(AioEventLoopGroup parentGroup, AioEventLoopGroup childGroup) {
		return new AioServerSocketChannel(childGroup) {
			@Override
			protected Runnable doRegister() throws Exception {
				// skip check during registration since we need to
				// use the child event loop for all child channels
				return null;
			}
		};
	}

}
