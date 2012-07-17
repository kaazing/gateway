package com.kaazing.mina.transport.netty;

import java.util.Map;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChildChannelStateEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

public class IoAcceptorChannelHandler extends SimpleChannelUpstreamHandler {

	private final ChannelIoService acceptor;
	private ChannelPipelineFactory pipelineFactory;
	
	public IoAcceptorChannelHandler(ChannelIoService acceptor) {
		this.acceptor = acceptor;
	}
	
	public void setPipelineFactory(ChannelPipelineFactory pipelineFactory) {
		this.pipelineFactory = pipelineFactory;
	}

	@Override
	public void childChannelOpen(ChannelHandlerContext ctx,
			ChildChannelStateEvent e) throws Exception {
		
		Channel childChannel = e.getChildChannel();
		ChannelPipeline childPipeline = childChannel.getPipeline();

		if (pipelineFactory != null) {
			ChannelPipeline newChildPipeline = pipelineFactory.getPipeline();
			for (Map.Entry<String, ChannelHandler> entry : newChildPipeline.toMap().entrySet()) {
				String key = entry.getKey();
				ChannelHandler handler = entry.getValue();
				childPipeline.addLast(key, handler);
			}
		}
		
		ChannelIoSession session = new ChannelIoSession(acceptor, childChannel);
		childPipeline.addLast("session", new IoSessionChannelHandler(session));
		
		super.childChannelOpen(ctx, e);
	}

}
