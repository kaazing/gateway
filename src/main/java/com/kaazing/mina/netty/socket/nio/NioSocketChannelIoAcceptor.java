/**
 * Copyright (c) 2007-2013, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.socket.nio;

import java.net.InetSocketAddress;

import org.apache.mina.core.service.DefaultTransportMetadata;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.transport.socket.SocketSessionConfig;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelConfig;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioSocketChannel;

import com.kaazing.mina.core.service.IoProcessorEx;
import com.kaazing.mina.netty.ChannelIoSession;
import com.kaazing.mina.netty.socket.SocketChannelIoAcceptor;

public class NioSocketChannelIoAcceptor extends SocketChannelIoAcceptor {

    private static final TransportMetadata NIO_SOCKET_TRANSPORT_METADATA = new DefaultTransportMetadata(
            "Kaazing", "NioSocketChannel", false, true, InetSocketAddress.class,
            SocketSessionConfig.class, Object.class);

    public NioSocketChannelIoAcceptor(NioSocketChannelIoSessionConfig sessionConfig) {
        this(sessionConfig, new NioServerSocketChannelFactory());
    }

    public NioSocketChannelIoAcceptor(NioSocketChannelIoSessionConfig sessionConfig,
            NioServerSocketChannelFactory channelFactory) {
        this(sessionConfig, channelFactory, new SimpleChannelHandler());
    }

    public NioSocketChannelIoAcceptor(NioSocketChannelIoSessionConfig sessionConfig,
            final NioServerSocketChannelFactory channelFactory, ChannelHandler bindHandler) {
        super(sessionConfig, channelFactory, bindHandler);
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
