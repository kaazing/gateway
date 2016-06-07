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

import static org.jboss.netty.channel.Channels.pipeline;

import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.ServerChannelFactory;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

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

        private final Map<SocketAddress, Map<SocketAddress, Channel>> childChannelsByLocalAddress;

        public ConnectionlessParentChannelHandler() {
            childChannelsByLocalAddress = new ConcurrentHashMap<>();
        }

        @Override
        public void channelConnected(ChannelHandlerContext ctx,
                ChannelStateEvent e) throws Exception {
            // TODO Auto-generated method stub
            super.channelConnected(ctx, e);
        }

        @Override
        public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
                throws Exception {

//            // lookup child channel based on local and remote addresses
//            Channel channel = e.getChannel();
//            SocketAddress remoteAddress = e.getRemoteAddress();
//            Channel childChannel = getChildChannel(channel, remoteAddress, true);
//
//            // deliver message received to child channel pipeline
//            fireMessageReceived(childChannel, e);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
                throws Exception {
            ctx.sendUpstream(e);
        }

        private Channel getChildChannel(Channel channel, SocketAddress remoteAddress,
                                        boolean createIfNull) throws Exception {

            SocketAddress localAddress = channel.getLocalAddress();
            Map<SocketAddress, Channel> childChannelsByRemoteAddress = childChannelsByLocalAddress.get(localAddress);
            if (childChannelsByRemoteAddress == null && createIfNull) {
                Map<SocketAddress, Channel> newChildChannels = new ConcurrentHashMap<>();
                childChannelsByRemoteAddress = childChannelsByLocalAddress.put(localAddress, newChildChannels);
                if (childChannelsByRemoteAddress == null) {
                    childChannelsByRemoteAddress = newChildChannels;
                }
            }
            if (childChannelsByRemoteAddress != null) {
                Channel childChannel = childChannelsByRemoteAddress.get(remoteAddress);
                if (childChannel == null && createIfNull) {
                    ChannelPipelineFactory childPipelineFactory = getPipelineFactory();
                    ChannelPipeline newChildPipeline = childPipelineFactory.getPipeline();
                    ChannelFactory channelFactory = channel.getFactory();
                    Channel newChildChannel = channelFactory.newChannel(newChildPipeline);
                    childChannel = childChannelsByRemoteAddress.put(remoteAddress, newChildChannel);
                    if (childChannel == null) {
//                        newChildChannel.bind(localAddress);
                        newChildChannel.connect(remoteAddress);
                        childChannel = newChildChannel;
                    }
                }
                if (childChannel != null) {
                    final Map<SocketAddress, Channel> childChannelsByRemoteAddress0 = childChannelsByRemoteAddress;
                    final SocketAddress remoteAddress0 = remoteAddress;
                    ChannelFuture closeFuture = childChannel.getCloseFuture();
                    closeFuture.addListener(new ChannelFutureListener() {

                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            childChannelsByRemoteAddress0.remove(remoteAddress0);
                        }

                    });
                }
                return childChannel;
            }
            return null;
        }
    }
}
