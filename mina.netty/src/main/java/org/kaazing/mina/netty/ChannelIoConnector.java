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

import org.kaazing.mina.core.service.AbstractIoConnectorEx;
import org.kaazing.mina.core.service.IoProcessorEx;
import org.kaazing.mina.core.session.IoSessionConfigEx;
import org.kaazing.mina.netty.bootstrap.ClientBootstrap;
import org.kaazing.mina.netty.bootstrap.ClientBootstrapFactory;
import org.kaazing.mina.netty.util.threadlocal.VicariousThreadLocal;

public abstract
    class ChannelIoConnector<C extends IoSessionConfigEx, F extends ChannelFactory, A extends SocketAddress>
    extends AbstractIoConnectorEx implements ChannelIoService {

    private final F channelFactory;
    private ChannelPipelineFactory pipelineFactory;
    private final ChannelGroup channelGroup;
    private final IoProcessorEx<ChannelIoSession<? extends ChannelConfig>> processor = new ChannelIoProcessor();
    private final ClientBootstrapFactory bootstrapFactory;
    private final IoConnectorChannelHandlerFactory handlerFactory;
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
    public final ChannelIoSession<? extends ChannelConfig> createSession(Channel channel) {
        return createSession(channel, processor);
    }

    protected abstract ChannelIoSession<? extends ChannelConfig> createSession(Channel channel,
            IoProcessorEx<ChannelIoSession<? extends ChannelConfig>> processor);

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

    protected IoProcessorEx<ChannelIoSession<? extends ChannelConfig>> getProcessor() {
        return processor;
    }

    @Override
    public IoSessionIdleTracker getSessionIdleTracker() {
        return currentSessionIdleTracker.get();
    }

    @Override
    public void initializeSession(ChannelIoSession<?> session, IoFuture future, IoSessionInitializer<?> initializer) {
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
