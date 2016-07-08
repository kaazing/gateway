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

import static org.jboss.netty.channel.Channels.fireChannelBound;
import static org.jboss.netty.channel.Channels.fireChannelClosed;
import static org.jboss.netty.channel.Channels.fireChannelConnected;
import static org.jboss.netty.channel.Channels.fireChannelDisconnected;
import static org.jboss.netty.channel.Channels.fireChannelOpen;
import static org.jboss.netty.channel.Channels.fireChannelUnbound;
import static org.jboss.netty.channel.Channels.fireMessageReceived;
import static org.jboss.netty.channel.Channels.pipeline;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.netty.channel.AbstractChannel;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelConfig;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelSink;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ChildChannelStateEvent;
import org.jboss.netty.channel.DefaultChildChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.ServerChannelFactory;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.nio.NioDatagramChannel;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;
import org.kaazing.mina.netty.IoAcceptorChannelHandler;

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
        private final Map<SocketAddress, Channel> childChannels;

        ConnectionlessParentChannelHandler() {
            childChannels = new ConcurrentHashMap<>();
        }

        @Override
        public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
            super.channelConnected(ctx, e);
        }

        public void childChannelOpen(ChannelHandlerContext ctx, ChildChannelStateEvent e) throws Exception {
            System.out.println("JITU ********* childChannelOpen " + e.getChildChannel().getRemoteAddress() + " thread = " + Thread.currentThread());
            ((IoAcceptorChannelHandler) parentHandler).childChannelOpen(ctx, e);
            fireChannelConnected(e.getChildChannel(), e.getChildChannel().getRemoteAddress());

//            try {
//                System.out.println("JITU ********* firing channelConnected (before) on child channel childChannelOpen" + e.getChildChannel());
//                // TODO make sure it has remote address
//                fireChannelConnected(e.getChildChannel(), e.getChannel().getRemoteAddress());
//                System.out.println("JITU ********* firing channelConnected (after) on child channel childChannelOpen" + e.getChildChannel());
//            } catch(Exception ex) {
//                ex.printStackTrace();
//            }
        }

        @Override
        public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {

            System.out.println("JITU *** ConnectionlessServerBootstrap received message = " + e);

            // lookup child channel based on local and remote addresses
            Channel channel = e.getChannel();
            Channel childChannel = getChildChannel(channel, e.getRemoteAddress());

            // deliver message received to child channel pipeline
            fireMessageReceived(childChannel, e.getMessage());
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
            ctx.sendUpstream(e);
        }

        private Channel getChildChannel(Channel channel, SocketAddress remoteAddress) throws Exception {
            return childChannels.computeIfAbsent(remoteAddress, x -> {
                ChannelPipelineFactory childPipelineFactory = getPipelineFactory();
                ChannelPipeline childPipeline;
                try {
                    childPipeline = childPipelineFactory.getPipeline();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                System.out.println("JITU server pipeline = " + channel.getPipeline());
                System.out.println("JITU child pipeline = " + childPipeline);

                ChannelFactory channelFactory = channel.getFactory();
                NioDatagramChannel childChannel = (NioDatagramChannel) ((NioDatagramChannelFactory)channelFactory).newChildChannel(channel, childPipeline);
                childChannel.setLocalAddress((InetSocketAddress) channel.getLocalAddress());
                childChannel.setRemoteAddress((InetSocketAddress) remoteAddress);
                fireChannelOpen(childChannel);
                //fireChannelConnected(childChannel, remoteAddress);

                return childChannel;
            });
        }
    }
}
