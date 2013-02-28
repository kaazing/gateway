/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import java.net.SocketAddress;

import org.apache.mina.core.service.DefaultTransportMetadata;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.IoSessionConfig;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ServerChannelFactory;

public class DefaultChannelIoAcceptor extends ChannelIoAcceptor<IoSessionConfig, ServerChannelFactory, SocketAddress> {

	private static final TransportMetadata TRANSPORT_METADATA = new DefaultTransportMetadata(
			"Kaazing", "Channel", false, true, SocketAddress.class,
			IoSessionConfig.class, Object.class);
	
    public DefaultChannelIoAcceptor(ServerChannelFactory channelFactory) {
		this(new DefaultChannelIoSessionConfig(), channelFactory, new DefaultIoAcceptorChannelHandlerFactory());
	}

	public DefaultChannelIoAcceptor(IoSessionConfig sessionConfig,
			ServerChannelFactory channelFactory, IoAcceptorChannelHandlerFactory handlerFactory) {
		super(sessionConfig, channelFactory, handlerFactory);
	}

	@Override
	public TransportMetadata getTransportMetadata() {
		return TRANSPORT_METADATA;
	}

    @Override
    public ChannelIoSession createSession(ChannelHandlerContext context) {
        return new DefaultChannelIoSession(this, context.getChannel());
    }
    
}
