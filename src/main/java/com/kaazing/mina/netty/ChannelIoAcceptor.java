/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import static java.lang.String.format;

import java.io.IOException;
import java.net.BindException;
import java.net.SocketAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.session.IoSessionInitializer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.ChannelGroupFutureListener;
import org.jboss.netty.channel.group.DefaultChannelGroup;

import com.kaazing.mina.core.future.BindFuture;
import com.kaazing.mina.core.future.DefaultBindFuture;
import com.kaazing.mina.core.future.DefaultUnbindFuture;
import com.kaazing.mina.core.future.UnbindFuture;
import com.kaazing.mina.core.service.AbstractIoAcceptorEx;
import com.kaazing.mina.core.session.IoSessionConfigEx;
import com.kaazing.mina.netty.bootstrap.ServerBootstrap;
import com.kaazing.mina.netty.bootstrap.ServerBootstrapFactory;

public abstract class ChannelIoAcceptor<C extends IoSessionConfigEx, F extends ChannelFactory, A extends SocketAddress>
                extends AbstractIoAcceptorEx implements ChannelIoService {

    private final ServerBootstrap bootstrap;
    private final Map<SocketAddress, Channel> boundChannels;
    private final IoAcceptorChannelHandler parentHandler;
    private final ChannelGroup channelGroup;

    public ChannelIoAcceptor(C sessionConfig, F channelFactory, IoAcceptorChannelHandlerFactory handlerFactory,
                             ServerBootstrapFactory bootstrapFactory) {
        super(sessionConfig, new Executor() {
            @Override
            public void execute(Runnable command) {
            }
        });

        channelGroup = new DefaultChannelGroup();

        parentHandler = handlerFactory.createHandler(this, channelGroup);

        bootstrap = bootstrapFactory.createBootstrap();
        bootstrap.setFactory(channelFactory);
        bootstrap.setParentHandler(parentHandler);

        boundChannels = new ConcurrentHashMap<SocketAddress, Channel>();
    }

    public void setPipelineFactory(ChannelPipelineFactory pipelineFactory) {
        parentHandler.setPipelineFactory(pipelineFactory);
    }

    @Override
    public void initializeSession(ChannelIoSession session, IoFuture future, IoSessionInitializer<?> initializer) {
        initSession(session, future, initializer);
    }

    @SuppressWarnings("unchecked")
    protected F getChannelFactory() {
        return (F) bootstrap.getFactory();
    }

    @Override
    protected Set<SocketAddress> bindInternal(
            List<? extends SocketAddress> localAddresses) throws Exception {

        for (SocketAddress localAddress : localAddresses) {
            try {
                Channel channel = bootstrap.bind(localAddress);
                boundChannels.put(localAddress, channel);
            }
            catch (Exception e) {
                BindException be = new BindException(format("Unable to bind address: %s", localAddress));
                be.initCause(e);
                be.fillInStackTrace();
                throw be;
            }
        }

        Set<SocketAddress> newLocalAddresses = new HashSet<SocketAddress>();
        for (SocketAddress localAddress : localAddresses) {
            newLocalAddresses.add(localAddress);
        }

        return newLocalAddresses;
    }

    @Override
    protected BindFuture bindAsyncInternal(final SocketAddress localAddress) {
        final BindFuture bound = new DefaultBindFuture();
        ChannelFuture channelBound = bootstrap.bindAsync(localAddress);
        channelBound.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                     boundChannels.put(localAddress, future.getChannel());
                     bound.setBound();
                }
                else {
                    BindException be = new BindException(format("Unable to bind address: %s", localAddress));
                    be.initCause(future.getCause());
                    be.fillInStackTrace();
                    bound.setException(be);
                }
            }
        });
        return bound;
    }

    @Override
    protected void unbind0(List<? extends SocketAddress> localAddresses)
            throws Exception {

        for (SocketAddress localAddress : localAddresses) {
            Channel channel = boundChannels.remove(localAddress);

            if (channel == null) {
                continue;
            }

            ChannelFuture unbound = channel.unbind();

            // the signature of this method (and of the public bind method that calls it) implies it is a
            // synchronous operation, which must therefore complete or fail before we return.
            unbound.awaitUninterruptibly();
            if (!unbound.isSuccess()) {
                throw new IOException(unbound.getCause());
            }
        }

    }

    @Override
    protected UnbindFuture unbindAsyncInternal(final SocketAddress localAddress) {
        final UnbindFuture unbound = new DefaultUnbindFuture();
        Channel channel = boundChannels.remove(localAddress);
        ChannelFuture channelUnbound = channel.unbind();
        channelUnbound.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    unbound.setUnbound();
                }
                else {
                    unbound.setException(future.getCause());
                }
            }
        });
        return unbound;
    }

    @Override
    public ChannelIoSession newSession(SocketAddress remoteAddress,
            SocketAddress localAddress) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected IoFuture dispose0() throws Exception {
        channelGroup.close().addListener(new ChannelGroupFutureListener() {
            @Override
            public void operationComplete(ChannelGroupFuture future) throws Exception {
                // releaseExternalResources may throw errors if everything isn't yet unbound so wait for this
                // If the future fails presumably we want to try to clean up anyway so we don't check for success
                bootstrap.releaseExternalResources();
            }
        });
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public C getSessionConfig() {
        return (C) super.getSessionConfig();
    }

    @Override
    @SuppressWarnings("unchecked")
    public A getDefaultLocalAddress() {
        return (A) super.getDefaultLocalAddress();
    }

    @Override
    @SuppressWarnings("unchecked")
    public A getLocalAddress() {
        return (A) super.getLocalAddress();
    }
}
