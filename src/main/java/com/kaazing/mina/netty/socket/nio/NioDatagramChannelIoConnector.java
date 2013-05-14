/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.socket.nio;

import java.net.InetSocketAddress;

import org.apache.mina.core.service.DefaultTransportMetadata;
import org.apache.mina.core.service.TransportMetadata;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelConfig;
import org.jboss.netty.channel.socket.nio.NioDatagramChannel;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;

import com.kaazing.mina.core.service.IoProcessorEx;
import com.kaazing.mina.netty.ChannelIoSession;
import com.kaazing.mina.netty.socket.DatagramChannelIoConnector;
import com.kaazing.mina.netty.socket.DatagramChannelIoSessionConfig;

public class NioDatagramChannelIoConnector extends DatagramChannelIoConnector {

    private static final TransportMetadata NIO_DATAGRAM_TRANSPORT_METADATA = new DefaultTransportMetadata(
            "Kaazing", "NioDatagramChannel", true, true, InetSocketAddress.class,
            DatagramChannelIoSessionConfig.class, Object.class);

    public NioDatagramChannelIoConnector(DatagramChannelIoSessionConfig sessionConfig) {
        super(sessionConfig, new NioDatagramChannelFactory());
    }

    @Override
    public TransportMetadata getTransportMetadata() {
        return NIO_DATAGRAM_TRANSPORT_METADATA;
    }

    @Override
    protected ChannelIoSession<? extends ChannelConfig> createSession(Channel channel,
            IoProcessorEx<ChannelIoSession<? extends ChannelConfig>> processor) {
        return new NioDatagramChannelIoSession(this, processor, (NioDatagramChannel) channel);
    }

}
