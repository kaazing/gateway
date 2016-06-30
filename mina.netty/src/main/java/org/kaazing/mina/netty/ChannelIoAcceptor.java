/**
 * Copyright 2007-2016, Kaazing Corporation. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kaazing.mina.netty;

import static java.lang.String.format;

import java.io.IOException;
import java.net.BindException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.session.IoSessionInitializer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelConfig;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;

import org.kaazing.mina.core.future.BindFuture;
import org.kaazing.mina.core.future.DefaultBindFuture;
import org.kaazing.mina.core.future.DefaultUnbindFuture;
import org.kaazing.mina.core.future.UnbindFuture;
import org.kaazing.mina.core.service.AbstractIoAcceptorEx;
import org.kaazing.mina.core.service.IoProcessorEx;
import org.kaazing.mina.core.session.IoSessionConfigEx;
import org.kaazing.mina.netty.bootstrap.ServerBootstrap;
import org.kaazing.mina.netty.bootstrap.ServerBootstrapFactory;
import org.kaazing.mina.netty.util.threadlocal.VicariousThreadLocal;

public abstract class ChannelIoAcceptor<C extends IoSessionConfigEx, F extends ChannelFactory, A extends SocketAddress>
                extends AbstractIoAcceptorEx implements ChannelIoService {

    private final ServerBootstrap bootstrap;
    private final Map<SocketAddress, Channel> boundChannels;
    private IoSessionInitializer<? extends IoFuture> initializer;
    private final IoAcceptorChannelHandler parentHandler;
    private final ChannelGroup channelGroup;
    private final IoProcessorEx<ChannelIoSession<? extends ChannelConfig>> processor = new ChannelIoProcessor();
    private final List<IoSessionIdleTracker> sessionIdleTrackers
        = Collections.synchronizedList(new ArrayList<>());
    private final ThreadLocal<IoSessionIdleTracker> currentSessionIdleTracker
        = new VicariousThreadLocal<IoSessionIdleTracker>() {
        @Override
        protected IoSessionIdleTracker initialValue() {
            IoSessionIdleTracker result = new DefaultIoSessionIdleTracker();
            sessionIdleTrackers.add(result);
            return result;
        }
    };

    protected ChannelIoAcceptor(C sessionConfig, F channelFactory, final ChannelHandler bindHandler,
            ServerBootstrapFactory bootstrapFactory) {

        super(sessionConfig, new Executor() {
            @Override
            public void execute(Runnable command) {
            }
        });

        if (bindHandler == null) {
            throw new NullPointerException("bindHandler");
        }

        channelGroup = new DefaultChannelGroup();

        parentHandler = new IoAcceptorChannelHandler(this, channelGroup, bindHandler);

        bootstrap = bootstrapFactory.createBootstrap();
        bootstrap.setFactory(channelFactory);
        bootstrap.setParentHandler(parentHandler);

        boundChannels = new ConcurrentHashMap<>();
    }

    public IoSessionInitializer<? extends IoFuture> getIoSessionInitializer() {
        return initializer;
    }

    public void setIoSessionInitializer(IoSessionInitializer<? extends IoFuture> initializer) {
        this.initializer = initializer;
    }

    public void setPipelineFactory(ChannelPipelineFactory pipelineFactory) {
        parentHandler.setPipelineFactory(pipelineFactory);
    }

    @Override
    public IoSessionIdleTracker getSessionIdleTracker() {
        return currentSessionIdleTracker.get();
    }

    @Override
    public void initializeSession(ChannelIoSession<?> session, IoFuture future, IoSessionInitializer<?> initializer) {
        initSession(session, future, initializer);
    }

    @SuppressWarnings("unchecked")
    protected F getChannelFactory() {
        return (F) bootstrap.getFactory();
    }

    protected IoProcessorEx<ChannelIoSession<? extends ChannelConfig>> getProcessor() {
        return processor;
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

        Set<SocketAddress> newLocalAddresses = new HashSet<>();
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
                    Channel channel = future.getChannel();
                    boundChannels.put(localAddress, channel);
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

            ChannelFuture unbound = channel.close();

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
        ChannelFuture channelUnbound = channel.close();
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
    public final ChannelIoSession<? extends ChannelConfig> createSession(Channel channel) {
        return createSession(channel, processor);
    }

    @Override
    public ChannelIoSession<? extends ChannelConfig> newSession(SocketAddress remoteAddress,
            SocketAddress localAddress) {
        throw new UnsupportedOperationException();
    }

    protected abstract ChannelIoSession<? extends ChannelConfig> createSession(Channel channel,
            IoProcessorEx<ChannelIoSession<? extends ChannelConfig>> processor);

    @Override
    protected IoFuture dispose0() throws Exception {
        channelGroup.close().await();
        unbind();
        bootstrap.releaseExternalResources();
        for (IoSessionIdleTracker tracker : sessionIdleTrackers) {
            tracker.dispose();
        }
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
