/**
 * Copyright (c) 2007-2013, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.socket;

import java.net.InetSocketAddress;

import org.apache.mina.core.service.DefaultTransportMetadata;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.transport.socket.SocketAcceptorEx;
import org.apache.mina.transport.socket.SocketSessionConfig;
import org.jboss.netty.channel.socket.ServerSocketChannelFactory;
import org.jboss.netty.channel.socket.SocketChannelConfig;

import com.kaazing.mina.netty.ChannelIoAcceptor;
import com.kaazing.mina.netty.DefaultIoAcceptorChannelHandlerFactory;
import com.kaazing.mina.netty.IoAcceptorChannelHandlerFactory;
import com.kaazing.mina.netty.bootstrap.ServerBootstrapFactory;

public abstract class SocketChannelIoAcceptor
    extends ChannelIoAcceptor<SocketChannelIoSessionConfig<? extends SocketChannelConfig>,
                              IoAcceptorSocketChannelFactory, InetSocketAddress>
    implements SocketAcceptorEx {

    private static final TransportMetadata SOCKET_TRANSPORT_METADATA = new DefaultTransportMetadata(
            "Kaazing", "SocketChannel", false, true, InetSocketAddress.class,
            SocketSessionConfig.class, Object.class);

    public SocketChannelIoAcceptor(SocketChannelIoSessionConfig<? extends SocketChannelConfig> sessionConfig,
            final ServerSocketChannelFactory channelFactory) {
        this(sessionConfig, new IoAcceptorSocketChannelFactory(channelFactory),
                new DefaultIoAcceptorChannelHandlerFactory());
    }

    public SocketChannelIoAcceptor(SocketChannelIoSessionConfig<? extends SocketChannelConfig> sessionConfig,
            final ServerSocketChannelFactory channelFactory, IoAcceptorChannelHandlerFactory handlerFactory) {
        super(sessionConfig, new IoAcceptorSocketChannelFactory(channelFactory), handlerFactory,
              ServerBootstrapFactory.CONNECTED);
    }

    @Override
    public int getBacklog() {
        return getChannelFactory().getBacklog();
    }

    @Override
    public void setBacklog(int backlog) {
        getChannelFactory().setBacklog(backlog);
    }

    @Override
    public boolean isReuseAddress() {
        return getChannelFactory().isReuseAddress();
    }

    @Override
    public void setReuseAddress(boolean reuseAddress) {
        getChannelFactory().setReuseAddress(reuseAddress);
    }

    @Override
    public void setDefaultLocalAddress(InetSocketAddress localAddress) {
        super.setDefaultLocalAddress(localAddress);
    }

    @Override
    public TransportMetadata getTransportMetadata() {
        return SOCKET_TRANSPORT_METADATA;
    }

}
