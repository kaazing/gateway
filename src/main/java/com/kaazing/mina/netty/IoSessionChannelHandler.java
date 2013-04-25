/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;


import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.session.IoSessionInitializer;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelConfig;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;


public class IoSessionChannelHandler extends SimpleChannelHandler {

    private final ChannelIoSession<? extends ChannelConfig> session;
    private final IoFilterChain filterChain;
    private final IoFuture future;
    private final IoSessionInitializer<?> initializer;
    private final IoSessionIdleTracker idleTracker;

    public IoSessionChannelHandler(ChannelIoSession<? extends ChannelConfig> session, IoFuture future,
            IoSessionInitializer<?> initializer, IoSessionIdleTracker idleTracker) {
        this.session = session;
        this.filterChain = session.getFilterChain();
        this.future = future;
        this.initializer = initializer;
        this.idleTracker = idleTracker;
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e)
    throws Exception {
        session.getService().initializeSession(session, future, initializer);
        idleTracker.addSession(session);
        session.getProcessor().add(session);
    }

    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e)
            throws Exception {
        // Processor remove takes care of firing sessionClosed on the filter chain.
        session.getProcessor().remove(session);
        idleTracker.removeSession(session);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
            throws Exception {
        filterChain.fireExceptionCaught(e.getCause());
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
            throws Exception {
        Object message = e.getMessage();
        if (message instanceof ChannelBuffer) {
            ChannelBuffer buf = (ChannelBuffer) message;
            message = IoBuffer.wrap(buf.toByteBuffer());
            buf.skipBytes(buf.readableBytes());
        }
        filterChain.fireMessageReceived(message);
    }

}
