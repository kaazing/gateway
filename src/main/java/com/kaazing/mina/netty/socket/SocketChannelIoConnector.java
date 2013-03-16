/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.socket;

import java.net.InetSocketAddress;

import org.apache.mina.core.service.DefaultTransportMetadata;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.transport.socket.SocketConnector;
import org.apache.mina.transport.socket.SocketSessionConfig;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;

import com.kaazing.mina.netty.ChannelIoConnector;
import com.kaazing.mina.netty.ChannelIoSession;
import com.kaazing.mina.netty.DefaultChannelIoSession;
import com.kaazing.mina.netty.DefaultIoConnectorChannelHandlerFactory;
import com.kaazing.mina.netty.IoConnectorChannelHandlerFactory;
import com.kaazing.mina.netty.bootstrap.ClientBootstrapFactory;

public class SocketChannelIoConnector
    extends ChannelIoConnector<SocketChannelIoSessionConfig, ClientSocketChannelFactory, InetSocketAddress>
    implements SocketConnector {

    private static final TransportMetadata SOCKET_TRANSPORT_METADATA = new DefaultTransportMetadata(
            "Kaazing", "SocketChannel", false, true, InetSocketAddress.class,
            SocketSessionConfig.class, Object.class);

    public SocketChannelIoConnector(SocketChannelIoSessionConfig sessionConfig,
            ClientSocketChannelFactory channelFactory) {
        this(sessionConfig, channelFactory, new DefaultIoConnectorChannelHandlerFactory());
    }

    public SocketChannelIoConnector(SocketChannelIoSessionConfig sessionConfig,
                                    ClientSocketChannelFactory channelFactory,
                                    IoConnectorChannelHandlerFactory handlerFactory) {
        super(sessionConfig, channelFactory, handlerFactory, ClientBootstrapFactory.CONNECTED);
    }

    @Override
    public void setDefaultRemoteAddress(InetSocketAddress remoteAddress) {
        super.setDefaultRemoteAddress(remoteAddress);
    }

    @Override
    public TransportMetadata getTransportMetadata() {
        return SOCKET_TRANSPORT_METADATA;
    }

    @Override
    public ChannelIoSession createSession(Channel channel) {
        return new DefaultChannelIoSession(this, channel);
    }
}
