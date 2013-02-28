/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import java.util.Map;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChildChannelStateEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;

public class IoAcceptorChannelHandler extends SimpleChannelUpstreamHandler {

	private final ChannelIoService acceptor;
	private ChannelPipelineFactory pipelineFactory;
	private ChannelGroup channelGroup;
	
	public IoAcceptorChannelHandler(ChannelIoService acceptor) {
		this.acceptor = acceptor;
	}

	public void setChannelGroup(ChannelGroup channelGroup) {
		this.channelGroup = channelGroup;
	}
	
	public void setPipelineFactory(ChannelPipelineFactory pipelineFactory) {
		this.pipelineFactory = pipelineFactory;
	}

	@Override
	public void childChannelOpen(ChannelHandlerContext ctx,
			ChildChannelStateEvent e) throws Exception {
		
		final Channel childChannel = e.getChildChannel();
		if (channelGroup != null) {
			channelGroup.add(childChannel);
		}

		ChannelPipeline childPipeline = childChannel.getPipeline();

		if (pipelineFactory != null) {
			ChannelPipeline newChildPipeline = pipelineFactory.getPipeline();
			for (Map.Entry<String, ChannelHandler> entry : newChildPipeline.toMap().entrySet()) {
				String key = entry.getKey();
				ChannelHandler handler = entry.getValue();
				childPipeline.addLast(key, handler);
			}
		}
		
		childPipeline.addLast("factory", new IoSessionFactoryChannelHandler(acceptor));
		
		super.childChannelOpen(ctx, e);
	}

}
