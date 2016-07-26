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
package org.kaazing.mina.netty.bootstrap;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ChildChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.ServerChannelFactory;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.nio.AbstractNioWorker;
import org.jboss.netty.channel.socket.nio.NioChildDatagramChannel;
import org.jboss.netty.channel.socket.nio.NioDatagramChannel;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;
import org.kaazing.mina.netty.IoAcceptorChannelHandler;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static org.jboss.netty.channel.Channels.fireChannelConnected;
import static org.jboss.netty.channel.Channels.fireChannelOpen;
import static org.jboss.netty.channel.Channels.fireMessageReceived;
import static org.jboss.netty.channel.Channels.pipeline;

class ConnectionlessServerBootstrap extends ConnectionlessBootstrap implements ServerBootstrap {

    private ChannelHandler parentHandler;

    ConnectionlessServerBootstrap() {
    }

    @Override
    public void setFactory(ChannelFactory factory) {
        if (factory == null) {
            throw new NullPointerException("factory");
        }
        if (!(factory instanceof ServerChannelFactory)) {
            final ChannelFactory factory0 = factory;
            factory = new ChannelFactory() {

                @Override
                public Channel newChannel(ChannelPipeline pipeline) {
                    return factory0.newChannel(pipeline(new ConnectionlessParentChannelHandler()));
                }

                @Override
                public void shutdown() {
                    factory0.shutdown();
                }

                @Override
                public void releaseExternalResources() {
                    factory0.releaseExternalResources();
                }
            };
        }
        super.setFactory(factory);
    }

    @Override
    public void setParentHandler(ChannelHandler parentHandler) {
        this.parentHandler = parentHandler;
    }

    @Override
    public ChannelHandler getParentHandler() {
        return parentHandler;
    }

    private final class ConnectionlessParentChannelHandler extends SimpleChannelUpstreamHandler {

        // remote address --> child channel
        private final Map<SocketAddress, NioChildDatagramChannel> childChannels;

        ConnectionlessParentChannelHandler() {
            childChannels = new ConcurrentHashMap<>();
        }

        @Override
        public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
            super.channelConnected(ctx, e);
        }

        @Override
        public void childChannelOpen(ChannelHandlerContext ctx, ChildChannelStateEvent e) throws Exception {
            ((IoAcceptorChannelHandler) parentHandler).childChannelOpen(ctx, e);
            NioChildDatagramChannel childChannel = (NioChildDatagramChannel) e.getChildChannel();
            childChannel.getWorker().executeInIoThread(
                    () -> fireChannelConnected(childChannel, childChannel.getRemoteAddress())
            );
        }

        @Override
        public void childChannelClosed(ChannelHandlerContext ctx, ChildChannelStateEvent e) throws Exception {
            NioChildDatagramChannel childChannel = (NioChildDatagramChannel) e.getChildChannel();
            childChannels.remove(childChannel.getRemoteAddress());
            childChannel.getWorker().deregister(childChannel);
        }

        @Override
        public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
            // lookup child channel based on local and remote addresses
            Channel channel = e.getChannel();
            NioChildDatagramChannel childChannel = getChildChannel(channel, e.getRemoteAddress());

            AbstractNioWorker childWorker = childChannel.getWorker();
            childWorker.messageReceived(childChannel, e.getMessage());
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
            ctx.sendUpstream(e);
        }

        @Override
        public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
            childChannels.forEach(((socketAddress, childChannel) -> childChannel.close()));
            childChannels.clear();

            e.getFuture().setSuccess();
        }

        private NioChildDatagramChannel getChildChannel(Channel channel, SocketAddress remoteAddress) throws Exception {
            return childChannels.computeIfAbsent(remoteAddress, x -> {
                ChannelPipelineFactory childPipelineFactory = getPipelineFactory();
                ChannelPipeline childPipeline;
                try {
                    childPipeline = childPipelineFactory.getPipeline();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                ChannelFactory channelFactory = channel.getFactory();
                NioChildDatagramChannel childChannel = ((NioDatagramChannelFactory)channelFactory).newChildChannel(channel, childPipeline);
                childChannel.setLocalAddress((InetSocketAddress) channel.getLocalAddress());
                childChannel.setRemoteAddress((InetSocketAddress) remoteAddress);
                fireChannelOpen(childChannel);
                childChannel.getWorker().register(childChannel);

                return childChannel;
            });
        }
    }
}
