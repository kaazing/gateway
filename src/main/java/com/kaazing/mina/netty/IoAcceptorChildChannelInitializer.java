/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import io.netty.buffer.ChannelBufType;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelStateHandlerAdapter;
import io.netty.channel.group.ChannelGroup;

@Sharable
public class IoAcceptorChildChannelInitializer extends ChannelStateHandlerAdapter {

	private final ChannelIoService acceptor;
	private final ChannelBufType bufType;
	private final ChannelGroup channelGroup;
	
	public IoAcceptorChildChannelInitializer(ChannelIoService acceptor, ChannelBufType bufType, ChannelGroup channelGroup) {
		this.acceptor = acceptor;
		this.bufType = bufType;
		this.channelGroup = channelGroup;
	}

    @Override
    public final void channelRegistered(ChannelHandlerContext ctx)
            throws Exception {
        boolean removed = false;
        boolean success = false;
        try {
            initChannel(ctx);
            ctx.pipeline().remove(this);
            removed = true;
            ctx.fireChannelRegistered();
            success = true;
        } finally {
            if (!removed) {
                ctx.pipeline().remove(this);
            }
            if (!success) {
                ctx.close();
            }
        }
    }

	protected void initChannel(ChannelHandlerContext ctx) throws Exception {
		Channel childChannel = ctx.channel();
		if (channelGroup != null) {
			channelGroup.add(childChannel);
		}

		ChannelPipeline childPipeline = childChannel.pipeline();

		childPipeline.addLast("session", new IoSessionChannelHandler(acceptor, bufType));
	}

}
