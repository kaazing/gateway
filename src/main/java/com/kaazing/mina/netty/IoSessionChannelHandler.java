/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.session.IoSessionInitializer;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

public class IoSessionChannelHandler extends SimpleChannelHandler {

    private ChannelIoService service;
	private final IoFuture future;
	private final IoSessionInitializer<?> initializer;
	private ChannelIoSession session;
	
	public IoSessionChannelHandler(ChannelIoService service) {
		this(service, null, null);
	}

	public IoSessionChannelHandler(ChannelIoService service, IoFuture future, IoSessionInitializer<?> initializer) {
		this.service = service;
		this.future = future;
		this.initializer = initializer;
	}

	@Override
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e)
	throws Exception {
	    session = service.createSession(ctx);
		session.getService().initializeSession(session, future, initializer);
		session.getProcessor().add(session);
	}
	
	@Override
	public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e)
			throws Exception {
		session.close(true);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
			throws Exception {
		session.getFilterChain().fireExceptionCaught(e.getCause());
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
			throws Exception {
		Object message = e.getMessage();
		if (message instanceof ChannelBuffer) {
			ChannelBuffer buf = (ChannelBuffer)message;
			message = IoBuffer.wrap(buf.toByteBuffer());
			buf.skipBytes(buf.readableBytes());
		}
		session.getFilterChain().fireMessageReceived(message);
	}

}
