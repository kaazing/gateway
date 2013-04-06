/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import static org.jboss.netty.channel.Channels.pipeline;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.DefaultConnectFuture;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.session.IoSessionInitializer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelConfig;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;

import com.kaazing.mina.core.service.AbstractIoConnectorEx;
import com.kaazing.mina.core.session.IoSessionConfigEx;
import com.kaazing.mina.netty.bootstrap.ClientBootstrap;
import com.kaazing.mina.netty.bootstrap.ClientBootstrapFactory;

public abstract
    class ChannelIoConnector<C extends IoSessionConfigEx, F extends ChannelFactory, A extends SocketAddress>
    extends AbstractIoConnectorEx implements ChannelIoService {

    private final F channelFactory;
    private ChannelPipelineFactory pipelineFactory;
    private final ChannelGroup channelGroup;
    private final ClientBootstrapFactory bootstrapFactory;
    private final IoConnectorChannelHandlerFactory handlerFactory;
    private final List<IoSessionIdleTracker> sessionIdleTrackers
        = Collections.synchronizedList(new ArrayList<IoSessionIdleTracker>());
    private final ThreadLocal<IoSessionIdleTracker> currentSessionIdleTracker
        = new ThreadLocal<IoSessionIdleTracker>() {
        @Override
        protected IoSessionIdleTracker initialValue() {
            IoSessionIdleTracker result = new DefaultIoSessionIdleTracker();
            sessionIdleTrackers.add(result);
            return result;
        }
    };


    public ChannelIoConnector(C sessionConfig, F channelFactory, IoConnectorChannelHandlerFactory handlerFactory,
                              ClientBootstrapFactory bootstrapFactory) {
        super(sessionConfig, new Executor() {
            @Override
            public void execute(Runnable command) {
            }
        });

        this.channelFactory = channelFactory;
        this.channelGroup = new DefaultChannelGroup();
        this.bootstrapFactory = bootstrapFactory;
        this.handlerFactory = handlerFactory;
    }

    public void setPipelineFactory(ChannelPipelineFactory pipelineFactory) {
        this.pipelineFactory = pipelineFactory;
    }

    @Override
    protected ConnectFuture connect0(SocketAddress remoteAddress,
            SocketAddress localAddress,
            final IoSessionInitializer<? extends ConnectFuture> sessionInitializer) {

        final ConnectFuture connectFuture = new DefaultConnectFuture();

        ChannelFactory bootstrapChannelFactory = new ChannelFactory() {

            @Override
            public Channel newChannel(ChannelPipeline pipeline) {
                Channel newChannel = channelFactory.newChannel(pipeline);
                ChannelConfig newChannelConfig = newChannel.getConfig();
                newChannelConfig.setConnectTimeoutMillis((int) getConnectTimeoutMillis());
                return newChannel;
            }

            @Override
            public void releaseExternalResources() {
                channelFactory.releaseExternalResources();
            }

            @Override
            public void shutdown() {
                channelFactory.shutdown();
            }

        };

        ClientBootstrap bootstrap = bootstrapFactory.createBootstrap();
        bootstrap.setFactory(bootstrapChannelFactory);

        // support custom channel handlers before bridge
        ChannelPipeline newPipeline;
        if (pipelineFactory != null) {
            try {
                newPipeline = pipelineFactory.getPipeline();
            } catch (Exception e) {
                connectFuture.setException(e);
                return connectFuture;
            }
        }
        else {
            newPipeline = pipeline();
        }

        newPipeline.addLast("mina-bridge", handlerFactory.createHandler(this, connectFuture, sessionInitializer));
        bootstrap.setPipeline(newPipeline);
        ChannelFuture channelFuture = bootstrap.connect(remoteAddress, localAddress);
        channelFuture.addListener(new ChannelFutureListener() {

            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (!future.isSuccess()) {
                    connectFuture.setException(future.getCause());
                } else {
                    channelGroup.add(future.getChannel());
                }
            }
        });

        return connectFuture;
    }

    @Override
    protected IoFuture dispose0() throws Exception {
        channelGroup.close().await();
        channelFactory.releaseExternalResources();
        for (IoSessionIdleTracker tracker : sessionIdleTrackers) {
            tracker.dispose();
        }
        return null;
    }

    public IoSessionIdleTracker getSessionIdleTracker() {
        return currentSessionIdleTracker.get();
    }

    @Override
    public void initializeSession(ChannelIoSession session, IoFuture future, IoSessionInitializer<?> initializer) {
        initSession(session, future, initializer);
    }

    @Override
    @SuppressWarnings("unchecked")
    public C getSessionConfig() {
        return (C) super.getSessionConfig();
    }

    @Override
    @SuppressWarnings("unchecked")
    public A getDefaultRemoteAddress() {
        return (A) super.getDefaultRemoteAddress();
    }

}
