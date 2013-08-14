/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.socket;

import java.net.InetSocketAddress;

import org.apache.mina.transport.socket.SocketAcceptor;
import org.apache.mina.transport.socket.SocketAcceptorEx;
import org.jboss.netty.channel.ChannelConfig;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ChildChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.WriteCompletionEvent;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.socket.ServerSocketChannelFactory;
import org.jboss.netty.channel.socket.SocketChannelConfig;

import com.kaazing.mina.netty.ChannelIoAcceptor;
import com.kaazing.mina.netty.IoAcceptorChannelHandler;
import com.kaazing.mina.netty.IoAcceptorChannelHandlerFactory;
import com.kaazing.mina.netty.bootstrap.ServerBootstrapFactory;

public abstract class SocketChannelIoAcceptor extends
        ChannelIoAcceptor<SocketChannelIoSessionConfig<? extends SocketChannelConfig>, ServerSocketChannelFactory,
                                                                        InetSocketAddress> implements SocketAcceptorEx {
    // Default 50, consistant with mina.
    private int backlog = 50;

    // Default true, consistant with mina for socket sessions.
    private boolean reuseAddress = true;

    public SocketChannelIoAcceptor(SocketChannelIoSessionConfig<? extends SocketChannelConfig> sessionConfig,
            final ServerSocketChannelFactory channelFactory, IoAcceptorChannelHandlerFactory handlerFactory) {
        super(
                sessionConfig,
                channelFactory,
                new IoAcceptorChannelHandlerFactoryAdapter(handlerFactory),
                ServerBootstrapFactory.CONNECTED);
    }

    @Override
    public int getBacklog() {
        return backlog;
    }

    @Override
    public void setBacklog(int backlog) {
        this.backlog = backlog;
    }

    @Override
    public boolean isReuseAddress() {
        return reuseAddress;
    }

    @Override
    public void setReuseAddress(boolean reuseAddress) {
        this.reuseAddress = reuseAddress;
    }

    @Override
    public void setDefaultLocalAddress(InetSocketAddress localAddress) {
        super.setDefaultLocalAddress(localAddress);
    }

    // Decorate the factory so we can decorate the handler so we can add needed options on Open
    private static class IoAcceptorChannelHandlerFactoryAdapter implements IoAcceptorChannelHandlerFactory {

        private final IoAcceptorChannelHandlerFactory decoratedFactory;

        public IoAcceptorChannelHandlerFactoryAdapter(IoAcceptorChannelHandlerFactory factory) {
            decoratedFactory = factory;
        }

        @Override
        public IoAcceptorChannelHandler createHandler(final ChannelIoAcceptor<?, ?, ?> acceptor,
                                                      ChannelGroup channelGroup) {
            IoAcceptorChannelHandler handler = decoratedFactory.createHandler(acceptor, channelGroup);
            return new IoAcceptorChannelHandlerAdapter(acceptor, channelGroup, handler);

        }

    }

    private static final class IoAcceptorChannelHandlerAdapter extends IoAcceptorChannelHandler {

        private final IoAcceptorChannelHandler handler;

        private IoAcceptorChannelHandlerAdapter(ChannelIoAcceptor<?, ?, ?> acceptor, ChannelGroup channelGroup,
                IoAcceptorChannelHandler handler) {
            super(acceptor, channelGroup);
            this.handler = handler;
        }

        // Set reuseaddress and backlog after the channel is opened, but before the bind.
        @Override
        public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
            handler.channelOpen(ctx, e);
            ChannelConfig config = ctx.getChannel().getConfig();
            // Favoring setOption over type safe setters avoiding need for instanceof
            config.setOption("reuseAddress", ((SocketAcceptor) acceptor).isReuseAddress());
            config.setOption("backlog", ((SocketAcceptor) acceptor).getBacklog());

        }

        @Override
        public void setPipelineFactory(ChannelPipelineFactory pipelineFactory) {
            handler.setPipelineFactory(pipelineFactory);
        }

        @Override
        public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
            handler.handleUpstream(ctx, e);
        }

        @Override
        public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
            handler.messageReceived(ctx, e);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
            handler.exceptionCaught(ctx, e);
        }

        @Override
        public void channelBound(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
            handler.channelBound(ctx, e);
        }

        @Override
        public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
            handler.channelConnected(ctx, e);
        }

        @Override
        public void channelInterestChanged(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
            handler.channelInterestChanged(ctx, e);
        }

        @Override
        public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
            handler.channelDisconnected(ctx, e);
        }

        @Override
        public void channelUnbound(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
            handler.channelUnbound(ctx, e);
        }

        @Override
        public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
            handler.channelClosed(ctx, e);
        }

        @Override
        public void writeComplete(ChannelHandlerContext ctx, WriteCompletionEvent e) throws Exception {
            handler.writeComplete(ctx, e);
        }

        @Override
        public void childChannelOpen(ChannelHandlerContext ctx, ChildChannelStateEvent e) throws Exception {
            handler.childChannelOpen(ctx, e);
        }

        @Override
        public void childChannelClosed(ChannelHandlerContext ctx, ChildChannelStateEvent e) throws Exception {
            handler.childChannelClosed(ctx, e);
        }
    }
}
