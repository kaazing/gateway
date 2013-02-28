/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.session.IoSessionInitializer;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

/**
 * This creates the session then immediately replaces itself with IoSessionChannelHandler, in order to allow
 * the session member variable in IoSessionChannelHandler to be final, which is more efficient.
 */
public class IoSessionFactoryChannelHandler extends SimpleChannelHandler {

    private final ChannelIoService service;
	private final IoFuture future;
	private final IoSessionInitializer<?> initializer;
	
	public IoSessionFactoryChannelHandler(ChannelIoService service) {
		this(service, null, null);
	}

	public IoSessionFactoryChannelHandler(ChannelIoService service, IoFuture future, IoSessionInitializer<?> initializer) {
		this.service = service;
		this.future = future;
		this.initializer = initializer;
	}

	@Override
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e)
	throws Exception {
	    ChannelIoSession session = service.createSession(ctx);
	    String baseName = ctx.getName();
	    String name = String.format("%s#session", baseName);
	    ChannelHandler handler = new IoSessionChannelHandler(session, future, initializer);
	    ChannelPipeline pipeline = ctx.getPipeline();
        pipeline.addAfter(baseName, name, handler);
        ctx.sendUpstream(e);
        pipeline.remove(this);
	}
	
}
