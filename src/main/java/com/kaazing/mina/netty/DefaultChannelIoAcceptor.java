/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import java.net.SocketAddress;

import org.apache.mina.core.service.DefaultTransportMetadata;
import org.apache.mina.core.service.TransportMetadata;
import org.jboss.netty.channel.ServerChannelFactory;

import com.kaazing.mina.core.session.IoSessionConfigEx;
import com.kaazing.mina.netty.bootstrap.ServerBootstrapFactory;

public class DefaultChannelIoAcceptor
    extends ChannelIoAcceptor<IoSessionConfigEx, ServerChannelFactory, SocketAddress> {

    private static final TransportMetadata CONNECTED_TRANSPORT_METADATA = new DefaultTransportMetadata(
            "Kaazing", "Channel", false, true, SocketAddress.class,
            IoSessionConfigEx.class, Object.class);

    public DefaultChannelIoAcceptor(ServerChannelFactory channelFactory) {
        this(new DefaultChannelIoSessionConfig(), channelFactory, new DefaultIoAcceptorChannelHandlerFactory());
    }

    public DefaultChannelIoAcceptor(IoSessionConfigEx sessionConfig,
            ServerChannelFactory channelFactory, IoAcceptorChannelHandlerFactory handlerFactory) {
        super(sessionConfig, channelFactory, handlerFactory, ServerBootstrapFactory.CONNECTED);
    }

    @Override
    public TransportMetadata getTransportMetadata() {
        return CONNECTED_TRANSPORT_METADATA;
    }

}
