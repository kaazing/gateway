/**
 * Copyright (c) 2007-2013, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import java.util.Map;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChildChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;

public class IoAcceptorChannelHandler extends SimpleChannelUpstreamHandler {

    private final ChannelIoAcceptor<?, ?, ?> acceptor;
    private ChannelPipelineFactory pipelineFactory;
    private final ChannelGroup channelGroup;

    public IoAcceptorChannelHandler(ChannelIoAcceptor<?, ?, ?> acceptor, ChannelGroup channelGroup) {
        this.acceptor = acceptor;
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

        childPipeline.addLast("factory",
                new IoSessionFactoryChannelHandler(acceptor, null, acceptor.getIoSessionInitializer()));

        super.childChannelOpen(ctx, e);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
            throws Exception {
        // this will cause the bind channel future to fail, without noisy logging
        ctx.sendUpstream(e);
    }

}
