/**
 * Copyright (c) 2007-2014, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;


import static java.lang.System.currentTimeMillis;

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
import org.jboss.netty.channel.WriteCompletionEvent;

import com.kaazing.mina.core.buffer.IoBufferAllocatorEx;


public class IoSessionChannelHandler extends SimpleChannelHandler {

    private final ChannelIoSession<? extends ChannelConfig> session;
    private final IoBufferAllocatorEx<?> allocator;
    private final IoFuture future;
    private final IoSessionInitializer<?> initializer;
    private final IoSessionIdleTracker idleTracker;

    public IoSessionChannelHandler(ChannelIoSession<? extends ChannelConfig> session, IoFuture future,
            IoSessionInitializer<?> initializer, IoSessionIdleTracker idleTracker) {
        this.session = session;
        this.allocator = session.getBufferAllocator();
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
        // filter chain can change if session is re-aligned
        IoFilterChain filterChain = session.getFilterChain();
        filterChain.fireExceptionCaught(e.getCause());
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
            throws Exception {
        Object message = e.getMessage();
        if (message instanceof ChannelBuffer) {
            ChannelBuffer buf = (ChannelBuffer) message;
            // note: read as unshared buffer
            //       can convert via IoBufferEx.asSharedBuffer() if necessary later
            message = allocator.wrap(buf.toByteBuffer());
            buf.skipBytes(buf.readableBytes());
        }

        // filter chain can change if session is re-aligned
        IoFilterChain filterChain = session.getFilterChain();
        filterChain.fireMessageReceived(message);
    }

    @Override
    public void writeComplete(ChannelHandlerContext ctx, WriteCompletionEvent e) throws Exception {
        int writtenBytes = (int) e.getWrittenAmount();
        session.increaseWrittenBytes(writtenBytes, currentTimeMillis());
    }

}
