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
package org.kaazing.mina.netty.socket.nio;

import java.net.InetSocketAddress;

import org.apache.mina.core.service.DefaultTransportMetadata;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.transport.socket.DatagramSessionConfig;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelConfig;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ChildChannelStateEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.Worker;
import org.jboss.netty.channel.socket.nio.NioDatagramChannel;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;

import org.kaazing.mina.core.service.IoProcessorEx;
import org.kaazing.mina.netty.ChannelIoSession;
import org.kaazing.mina.netty.socket.DatagramChannelIoAcceptor;
import org.kaazing.mina.netty.socket.DatagramChannelIoSessionConfig;

public class NioDatagramChannelIoAcceptor extends DatagramChannelIoAcceptor {

    private static final TransportMetadata NIO_DATAGRAM_TRANSPORT_METADATA = new DefaultTransportMetadata(
            "Kaazing", "NioDatagramChannel", true, true, InetSocketAddress.class,
            DatagramSessionConfig.class, Object.class);

    public NioDatagramChannelIoAcceptor(DatagramChannelIoSessionConfig sessionConfig) {
        super(sessionConfig, new NioDatagramChannelFactory(), new SimpleChannelUpstreamHandler() {
            @Override
            public void childChannelOpen(ChannelHandlerContext ctx, ChildChannelStateEvent e) throws Exception {
                super.childChannelOpen(ctx, e);

                NioDatagramChannel childChannel = (NioDatagramChannel) e.getChildChannel();
                Worker childWorker = childChannel.getWorker();
                ChannelPipeline childPipeline = childChannel.getPipeline();
                childPipeline.addFirst("mina-alignment", new ChildAlignmentChannelHandler(childWorker));
            }
        });
    }


    @Override
    public TransportMetadata getTransportMetadata() {
        return NIO_DATAGRAM_TRANSPORT_METADATA;
    }

    @Override
    protected ChannelIoSession<? extends ChannelConfig> createSession(Channel channel,
            IoProcessorEx<ChannelIoSession<? extends ChannelConfig>> processor) {
        return new NioDatagramChannelIoSession(this, processor, (NioDatagramChannel) channel);
    }

    private static final class ChildAlignmentChannelHandler extends SimpleChannelUpstreamHandler {
        private final Worker worker;

        private ChildAlignmentChannelHandler(Worker worker) {
            this.worker = worker;
        }

        @Override
        public void channelConnected(final ChannelHandlerContext ctx,
                final ChannelStateEvent e) throws Exception {
            worker.executeInIoThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        channelConnectedAsync(ctx, e);
                    } catch (Exception cause) {
                        Channels.fireExceptionCaught(ctx, cause);
                    }
                }
            });
        }

        private void channelConnectedAsync(ChannelHandlerContext ctx,
                ChannelStateEvent e) throws Exception {
            super.channelConnected(ctx, e);
        }
    }

}
