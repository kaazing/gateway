/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.socket.nio;

import java.net.InetSocketAddress;

import org.apache.mina.core.service.DefaultTransportMetadata;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.transport.socket.SocketSessionConfig;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioSocketChannel;

import com.kaazing.mina.netty.ChannelIoSession;
import com.kaazing.mina.netty.socket.SocketChannelIoConnector;
import com.kaazing.mina.netty.socket.SocketChannelIoSessionConfig;

public class NioSocketChannelIoConnector extends SocketChannelIoConnector {

    private static final TransportMetadata NIO_SOCKET_TRANSPORT_METADATA = new DefaultTransportMetadata(
            "Kaazing", "NioSocketChannel", false, true, InetSocketAddress.class,
            SocketSessionConfig.class, Object.class);

    public NioSocketChannelIoConnector(SocketChannelIoSessionConfig sessionConfig) {
        this(sessionConfig, new NioClientSocketChannelFactory());
    }

    public NioSocketChannelIoConnector(SocketChannelIoSessionConfig sessionConfig,
            NioClientSocketChannelFactory channelFactory) {
        super(sessionConfig, channelFactory);
    }

    @Override
    public TransportMetadata getTransportMetadata() {
        return NIO_SOCKET_TRANSPORT_METADATA;
    }

    @Override
    public ChannelIoSession createSession(Channel channel) {
        return new NioSocketChannelIoSession(this, (NioSocketChannel) channel);
    }

}
