/**
 * Copyright (c) 2007-2013, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.socket;

import java.net.InetSocketAddress;

import org.apache.mina.core.service.DefaultTransportMetadata;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.transport.socket.DatagramConnector;
import org.apache.mina.transport.socket.DatagramSessionConfig;
import org.jboss.netty.channel.socket.DatagramChannelFactory;

import com.kaazing.mina.netty.ChannelIoConnector;
import com.kaazing.mina.netty.DefaultIoConnectorChannelHandlerFactory;
import com.kaazing.mina.netty.IoConnectorChannelHandlerFactory;
import com.kaazing.mina.netty.bootstrap.ClientBootstrapFactory;

public abstract class DatagramChannelIoConnector
    extends ChannelIoConnector<DatagramChannelIoSessionConfig, DatagramChannelFactory, InetSocketAddress>
    implements DatagramConnector {

    private static final TransportMetadata CONNECTIONLESS_TRANSPORT_METADATA = new DefaultTransportMetadata(
            "Kaazing", "DatagramChannel", true, true, InetSocketAddress.class,
            DatagramSessionConfig.class, Object.class);

    public DatagramChannelIoConnector(DatagramChannelIoSessionConfig sessionConfig,
            DatagramChannelFactory channelFactory) {
        this(sessionConfig, channelFactory, new DefaultIoConnectorChannelHandlerFactory());
    }

    protected DatagramChannelIoConnector(DatagramChannelIoSessionConfig sessionConfig,
            DatagramChannelFactory channelFactory, IoConnectorChannelHandlerFactory handlerFactory) {
        super(sessionConfig, channelFactory, handlerFactory, ClientBootstrapFactory.CONNECTIONLESS);
    }

    @Override
    public void setDefaultRemoteAddress(InetSocketAddress remoteAddress) {
        super.setDefaultRemoteAddress(remoteAddress);
    }

    @Override
    public TransportMetadata getTransportMetadata() {
        return CONNECTIONLESS_TRANSPORT_METADATA;
    }
}
