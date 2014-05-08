/**
 * Copyright (c) 2007-2013, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.session.IoSessionInitializer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

public class IoConnectorChannelHandler extends SimpleChannelUpstreamHandler {

    private final ChannelIoConnector<?, ?, ?> connector;
    private final ConnectFuture connectFuture;
    private final IoSessionInitializer<?> sessionInitializer;

    public IoConnectorChannelHandler(ChannelIoConnector<?, ?, ?> connector, ConnectFuture connectFuture,
                                     IoSessionInitializer<?> sessionInitializer) {
        this.connector = connector;
        this.connectFuture = connectFuture;
        this.sessionInitializer = sessionInitializer;
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, final ChannelStateEvent e)
            throws Exception {

        Channel channel = e.getChannel();
        ChannelPipeline childPipeline = channel.getPipeline();

        IoSessionFactoryChannelHandler newHandler =
                new IoSessionFactoryChannelHandler(connector, connectFuture, sessionInitializer);
        childPipeline.replace(this, "factory", newHandler);

        ChannelHandlerContext childCtx = childPipeline.getContext(newHandler);
        newHandler.channelConnected(childCtx, e);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
            throws Exception {
        connectFuture.setException(e.getCause());
    }

}
