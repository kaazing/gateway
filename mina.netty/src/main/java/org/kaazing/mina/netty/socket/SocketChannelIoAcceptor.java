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
package org.kaazing.mina.netty.socket;

import static java.lang.String.format;

import java.net.InetSocketAddress;

import org.apache.mina.transport.socket.SocketAcceptorEx;
import org.jboss.netty.channel.ChannelConfig;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.socket.ServerSocketChannelFactory;
import org.jboss.netty.channel.socket.SocketChannelConfig;

import org.kaazing.mina.netty.ChannelIoAcceptor;
import org.kaazing.mina.netty.bootstrap.ServerBootstrapFactory;

public abstract class SocketChannelIoAcceptor extends
        ChannelIoAcceptor<SocketChannelIoSessionConfig<? extends SocketChannelConfig>, ServerSocketChannelFactory,
                                                                        InetSocketAddress> implements SocketAcceptorEx {
    private final SocketAcceptorConfig acceptorConfig;

    protected SocketChannelIoAcceptor(SocketChannelIoSessionConfig<? extends SocketChannelConfig> sessionConfig,
            ServerSocketChannelFactory channelFactory, ChannelHandler bindHandler) {
        this(sessionConfig,
             channelFactory,
             bindHandler,
             new SocketAcceptorConfig());
    }

    private SocketChannelIoAcceptor(SocketChannelIoSessionConfig<? extends SocketChannelConfig> sessionConfig,
            ServerSocketChannelFactory channelFactory, ChannelHandler bindHandler, SocketAcceptorConfig acceptorConfig) {
        super(sessionConfig,
              channelFactory,
              new SocketBindHandler(bindHandler, acceptorConfig),
              ServerBootstrapFactory.CONNECTED);

        // acceptor configuration allocated on constructor stack to share across acceptor and static bind handler
        this.acceptorConfig = acceptorConfig;
    }

    @Override
    public int getBacklog() {
        return acceptorConfig.backlog;
    }

    @Override
    public void setBacklog(int backlog) {
        acceptorConfig.backlog = backlog;
    }

    @Override
    public boolean isReuseAddress() {
        return acceptorConfig.reuseAddress;
    }

    @Override
    public void setReuseAddress(boolean reuseAddress) {
        acceptorConfig.reuseAddress = reuseAddress;
    }

    @Override
    public void setDefaultLocalAddress(InetSocketAddress localAddress) {
        super.setDefaultLocalAddress(localAddress);
    }

    private static final class SocketAcceptorConfig {

        // default 50, consistent with MINA.
        private int backlog = 50;

        // default true, consistent with MINA for socket sessions.
        private boolean reuseAddress = true;

    }

    private static final class SocketBindHandler extends SimpleChannelHandler {

        private final ChannelHandler bindHandler;
        private final SocketAcceptorConfig config;

        public SocketBindHandler(ChannelHandler bindHandler, SocketAcceptorConfig config) {
            this.bindHandler = bindHandler;
            this.config = config;
        }

        @Override
        public void bindRequested(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
            // apply bind settings before we try to bind the channel
            ChannelConfig channelConfig = ctx.getChannel().getConfig();
            channelConfig.setOption("reuseAddress", config.reuseAddress);
            channelConfig.setOption("backlog", config.backlog);

            // propagate channel open event
            super.bindRequested(ctx, e);

        }

        @Override
        public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {

            // add bind handler to pipeline
            String baseName = ctx.getName();
            String name = format("%s:socket", baseName);
            ctx.getPipeline().addAfter(baseName, name, bindHandler);

            // propagate channel open event
            super.channelOpen(ctx, e);
        }
    }
}
