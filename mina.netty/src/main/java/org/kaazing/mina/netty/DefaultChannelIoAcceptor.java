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

import java.net.SocketAddress;

import org.apache.mina.core.service.DefaultTransportMetadata;
import org.apache.mina.core.service.TransportMetadata;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelConfig;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ServerChannelFactory;
import org.jboss.netty.channel.SimpleChannelHandler;

import org.kaazing.mina.core.service.IoProcessorEx;
import org.kaazing.mina.core.session.IoSessionConfigEx;
import org.kaazing.mina.netty.bootstrap.ServerBootstrapFactory;

public class DefaultChannelIoAcceptor
    extends ChannelIoAcceptor<IoSessionConfigEx, ServerChannelFactory, SocketAddress> {

    private static final TransportMetadata CONNECTED_TRANSPORT_METADATA = new DefaultTransportMetadata(
            "Kaazing", "Channel", false, true, SocketAddress.class,
            IoSessionConfigEx.class, Object.class);

    public DefaultChannelIoAcceptor(ServerChannelFactory channelFactory) {
        this(new DefaultChannelIoSessionConfig(), channelFactory, new SimpleChannelHandler());
    }

    public DefaultChannelIoAcceptor(IoSessionConfigEx sessionConfig,
            ServerChannelFactory channelFactory, ChannelHandler bindHandler) {
        super(sessionConfig, channelFactory, bindHandler, ServerBootstrapFactory.CONNECTED);
    }

    @Override
    protected ChannelIoSession<? extends ChannelConfig> createSession(Channel channel,
            IoProcessorEx<ChannelIoSession<? extends ChannelConfig>> processor) {
        return new DefaultChannelIoSession(this, processor, channel);
    }

    @Override
    public TransportMetadata getTransportMetadata() {
        return CONNECTED_TRANSPORT_METADATA;
    }

}
