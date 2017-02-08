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
import org.apache.mina.transport.socket.SocketSessionConfig;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelConfig;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioSocketChannel;

import org.kaazing.mina.core.service.IoProcessorEx;
import org.kaazing.mina.netty.ChannelIoSession;
import org.kaazing.mina.netty.socket.SocketChannelIoConnector;

public class NioSocketChannelIoConnector extends SocketChannelIoConnector {

    private static final TransportMetadata NIO_SOCKET_TRANSPORT_METADATA = new DefaultTransportMetadata(
            "Kaazing", "tcp", false, true, InetSocketAddress.class,
            SocketSessionConfig.class, Object.class);

    public NioSocketChannelIoConnector(NioSocketChannelIoSessionConfig sessionConfig) {
        this(sessionConfig, new NioClientSocketChannelFactory());
    }

    public NioSocketChannelIoConnector(NioSocketChannelIoSessionConfig sessionConfig,
            NioClientSocketChannelFactory channelFactory) {
        super(sessionConfig, channelFactory);
        sessionConfig.init(this);
    }

    @Override
    public TransportMetadata getTransportMetadata() {
        return NIO_SOCKET_TRANSPORT_METADATA;
    }

    @Override
    protected ChannelIoSession<? extends ChannelConfig> createSession(Channel channel,
            IoProcessorEx<ChannelIoSession<? extends ChannelConfig>> processor) {
        return new NioSocketChannelIoSession(this, processor, (NioSocketChannel) channel);
    }

}
