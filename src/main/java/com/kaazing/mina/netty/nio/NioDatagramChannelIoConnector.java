/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.nio;

import java.net.InetSocketAddress;

import org.apache.mina.core.service.DefaultTransportMetadata;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.transport.socket.SocketSessionConfig;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.socket.nio.NioDatagramChannel;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;

import com.kaazing.mina.netty.ChannelIoSession;
import com.kaazing.mina.netty.datagram.DatagramChannelIoConnector;
import com.kaazing.mina.netty.datagram.DatagramChannelIoSessionConfig;

public class NioDatagramChannelIoConnector extends DatagramChannelIoConnector {

    private static final TransportMetadata TRANSPORT_METADATA = new DefaultTransportMetadata(
            "Kaazing", "NioSocketChannel", false, true, InetSocketAddress.class,
            SocketSessionConfig.class, Object.class);

    public NioDatagramChannelIoConnector(DatagramChannelIoSessionConfig sessionConfig,
            NioDatagramChannelFactory channelFactory) {
        super(sessionConfig, channelFactory);
    }

    @Override
    public TransportMetadata getTransportMetadata() {
        return TRANSPORT_METADATA;
    }

    @Override
    public ChannelIoSession createSession(ChannelHandlerContext context) {
        return new NioDatagramChannelIoSession(this, (NioDatagramChannel) context.getChannel());
    }

}
