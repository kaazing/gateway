/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import java.net.SocketAddress;

import org.apache.mina.core.service.DefaultTransportMetadata;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.IoSessionConfig;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelConfig;
import org.jboss.netty.channel.ChannelFactory;

import com.kaazing.mina.core.service.IoProcessorEx;
import com.kaazing.mina.core.session.IoSessionConfigEx;
import com.kaazing.mina.netty.bootstrap.ClientBootstrapFactory;

public class DefaultChannelIoConnector extends ChannelIoConnector<IoSessionConfigEx, ChannelFactory,
                                                                        SocketAddress> {

    private static final TransportMetadata CONNECTED_TRANSPORT_METADATA = new DefaultTransportMetadata(
            "Kaazing", "Channel", false, true, SocketAddress.class,
            IoSessionConfig.class, Object.class);

    public DefaultChannelIoConnector(ChannelFactory channelFactory) {
        this(new DefaultChannelIoSessionConfig(), channelFactory, new DefaultIoConnectorChannelHandlerFactory());
    }

    public DefaultChannelIoConnector(IoSessionConfigEx sessionConfig,
            ChannelFactory channelFactory, IoConnectorChannelHandlerFactory handlerFactory) {
        super(sessionConfig, channelFactory, handlerFactory, ClientBootstrapFactory.CONNECTED);
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
