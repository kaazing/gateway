/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.socket;

import java.net.InetSocketAddress;

<<<<<<< .mine
=======
import org.apache.mina.transport.socket.SocketAcceptor;
>>>>>>> .r67355
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
import com.kaazing.mina.netty.bootstrap.ServerBootstrapFactory;

public abstract class SocketChannelIoAcceptor extends
        ChannelIoAcceptor<SocketChannelIoSessionConfig<? extends SocketChannelConfig>, ServerSocketChannelFactory,
                                                                        InetSocketAddress> implements SocketAcceptorEx {
    // Default 50, consistant with mina.
    private int backlog = 50;

    // Default true, consistant with mina for socket sessions.
    private boolean reuseAddress = true;

    protected SocketChannelIoAcceptor(SocketChannelIoSessionConfig<? extends SocketChannelConfig> sessionConfig,
            final ServerSocketChannelFactory channelFactory, SocketAcceptorChannelHandlerFactory handlerFactory) {
        super(
                sessionConfig,
                channelFactory,
                new SocketAcceptorChannelHandlerFactoryDecorator(handlerFactory),
                ServerBootstrapFactory.CONNECTED);
        // IoAcceptorChannelHandlerFactory<ChannelIoAcceptor<C, F, A>>
    }

    protected SocketChannelIoAcceptor(SocketChannelIoSessionConfig<? extends SocketChannelConfig> sessionConfig,
            final ServerSocketChannelFactory channelFactory, DefaultIoAcceptorChannelHandlerFactory handlerFactory) {
        super(sessionConfig, channelFactory, handlerFactory, ServerBootstrapFactory.CONNECTED);
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

<<<<<<< .mine
    // Decorate the factory so we can decorate the handler so we can add needed options on Open
    private static class SocketAcceptorChannelHandlerFactoryDecorator
            implements SocketAcceptorChannelHandlerFactory {

        private final SocketAcceptorChannelHandlerFactory delegate;

        public SocketAcceptorChannelHandlerFactoryDecorator(SocketAcceptorChannelHandlerFactory factory) {
            delegate = factory;
        }

        @Override
        public DefaultSocketAcceptorChannelHandler createHandler(SocketChannelIoAcceptor acceptor, ChannelGroup channelGroup) {
            DefaultSocketAcceptorChannelHandler handler = delegate.createHandler(acceptor, channelGroup);
            return new SocketAcceptorChannelHandlerDecorator(acceptor, channelGroup, handler);

        }

=======
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

>>>>>>> .r67355
    }

<<<<<<< .mine
    private static final class SocketAcceptorChannelHandlerDecorator extends SocketAcceptorChannelHandlerProxy {

        private final DefaultSocketAcceptorChannelHandler delegate;

        private SocketAcceptorChannelHandlerDecorator(SocketChannelIoAcceptor acceptor, ChannelGroup channelGroup,
                DefaultSocketAcceptorChannelHandler delegate) {
            super(acceptor, channelGroup);
            this.delegate = delegate;
        }

        // Send ChannelOpen up and apply settings on the way back, set reuseaddress and backlog after the channel is
        // opened, but before the bind.
        @Override
        public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
            super.channelOpen(ctx, e);
            ChannelConfig config = ctx.getChannel().getConfig();
            // Favoring setOption over type safe setters avoiding need for instanceof
            config.setOption("reuseAddress", isReuseAddress());
            config.setOption("backlog", getBacklog());
        }

        @Override
        protected DefaultSocketAcceptorChannelHandler getChannelHandler() {
            return delegate;
        }
    }

    // private static abstract class SocketSimpleChannelHandler extends
    // DefaultIoAcceptorChannelHandler<SocketChannelIoAcceptor>
    // implements SocketAcceptorChannelHandler {
    // protected SocketSimpleChannelHandler(SocketChannelIoAcceptor acceptor, ChannelGroup channelGroup) {
    // super(acceptor, channelGroup);
    // }
    // }

    private static abstract class SocketAcceptorChannelHandlerProxy extends DefaultSocketAcceptorChannelHandler {

        protected abstract DefaultSocketAcceptorChannelHandler getChannelHandler();

        protected SocketAcceptorChannelHandlerProxy(SocketChannelIoAcceptor acceptor, ChannelGroup channelGroup) {
            super(acceptor, channelGroup);
        }

        @Override
        public int getBacklog() {
            return getAcceptor().getBacklog();
        }

        @Override
        public boolean isReuseAddress() {
            return getAcceptor().isReuseAddress();
        }

        @Override
        public void setPipelineFactory(ChannelPipelineFactory pipelineFactory) {
            getChannelHandler().setPipelineFactory(pipelineFactory);
        }

        @Override
        public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
            getChannelHandler().channelOpen(ctx, e);
        }

        @Override
        public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
            getChannelHandler().handleUpstream(ctx, e);
        }

        @Override
        public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
            getChannelHandler().messageReceived(ctx, e);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
            getChannelHandler().exceptionCaught(ctx, e);
        }

        @Override
        public void channelBound(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
            getChannelHandler().channelBound(ctx, e);
        }

        @Override
        public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
            getChannelHandler().channelConnected(ctx, e);
        }

        @Override
        public void channelInterestChanged(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
            getChannelHandler().channelInterestChanged(ctx, e);
        }

        @Override
        public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
            getChannelHandler().channelDisconnected(ctx, e);
        }

        @Override
        public void channelUnbound(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
            getChannelHandler().channelUnbound(ctx, e);
        }

        @Override
        public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
            getChannelHandler().channelClosed(ctx, e);
        }

        @Override
        public void writeComplete(ChannelHandlerContext ctx, WriteCompletionEvent e) throws Exception {
            getChannelHandler().writeComplete(ctx, e);
        }

        @Override
        public void childChannelOpen(ChannelHandlerContext ctx, ChildChannelStateEvent e) throws Exception {
            getChannelHandler().childChannelOpen(ctx, e);
        }

        @Override
        public void childChannelClosed(ChannelHandlerContext ctx, ChildChannelStateEvent e) throws Exception {
            getChannelHandler().childChannelClosed(ctx, e);
        }
    }
=======
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
>>>>>>> .r67355
}
