/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.socket;

import java.net.InetSocketAddress;

import org.apache.mina.core.service.DefaultTransportMetadata;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.IoSessionRecycler;
import org.apache.mina.transport.socket.DatagramAcceptor;
import org.jboss.netty.channel.socket.DatagramChannelFactory;

import com.kaazing.mina.netty.ChannelIoAcceptor;
import com.kaazing.mina.netty.DefaultIoAcceptorChannelHandlerFactory;
import com.kaazing.mina.netty.IoAcceptorChannelHandlerFactory;
import com.kaazing.mina.netty.bootstrap.ServerBootstrapFactory;

public class DatagramChannelIoAcceptor
    extends ChannelIoAcceptor<DatagramChannelIoSessionConfig, DatagramChannelFactory, InetSocketAddress>
    implements DatagramAcceptor {

    private static final TransportMetadata CONNECTIONLESS_TRANSPORT_METADATA = new DefaultTransportMetadata(
            "Kaazing", "DatagramChannel", true, true, InetSocketAddress.class,
            DatagramChannelIoSessionConfig.class, Object.class);

    private IoSessionRecycler sessionRecycler;  // TODO

    public DatagramChannelIoAcceptor(DatagramChannelIoSessionConfig sessionConfig,
            DatagramChannelFactory channelFactory) {
        this(sessionConfig, channelFactory, new DefaultIoAcceptorChannelHandlerFactory());
    }

    protected DatagramChannelIoAcceptor(DatagramChannelIoSessionConfig sessionConfig,
            DatagramChannelFactory channelFactory, IoAcceptorChannelHandlerFactory handlerFactory) {
        super(sessionConfig, channelFactory, handlerFactory, ServerBootstrapFactory.CONNECTIONLESS);
    }

    @Override
    public IoSessionRecycler getSessionRecycler() {
        return sessionRecycler;
    }

    @Override
    public void setSessionRecycler(IoSessionRecycler sessionRecycler) {
        this.sessionRecycler = sessionRecycler;
    }

    @Override
    public void setDefaultLocalAddress(InetSocketAddress localAddress) {
        super.setDefaultLocalAddress(localAddress);
    }

    @Override
    public TransportMetadata getTransportMetadata() {
        return CONNECTIONLESS_TRANSPORT_METADATA;
    }
}
