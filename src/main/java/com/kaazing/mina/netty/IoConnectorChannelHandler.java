/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import io.netty.buffer.ChannelBufType;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.session.IoSessionInitializer;

public class IoConnectorChannelHandler extends ChannelHandlerAdapter {

	private final ChannelIoService connector;
	private final ChannelBufType bufType;
	private final ConnectFuture connectFuture;
	private final IoSessionInitializer<?> sessionInitializer;

	public IoConnectorChannelHandler(ChannelIoService connector,
			ChannelBufType bufType,
			ConnectFuture connectFuture,
			IoSessionInitializer<?> sessionInitializer) {
		this.connector = connector;
		this.bufType = bufType;
		this.connectFuture = connectFuture;
		this.sessionInitializer = sessionInitializer;
	}

	@Override
	public void channelRegistered(ChannelHandlerContext ctx) throws Exception {

		Channel channel = ctx.channel();
		ChannelPipeline childPipeline = channel.pipeline();

		ChannelIoSession session = new ChannelIoSession(connector, ctx);
		IoSessionChannelHandler newHandler = new IoSessionChannelHandler(session,
				bufType, connectFuture, sessionInitializer);
		childPipeline.replace(this, "session", newHandler);

		newHandler.channelRegistered(ctx);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		connectFuture.setException(cause);
	}

}
