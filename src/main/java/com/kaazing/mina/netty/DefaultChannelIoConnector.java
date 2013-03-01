/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import java.net.SocketAddress;

import org.apache.mina.core.service.DefaultTransportMetadata;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.IoSessionConfig;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelHandlerContext;

import com.kaazing.mina.core.session.IoSessionConfigEx;

public class DefaultChannelIoConnector extends ChannelIoConnector<IoSessionConfigEx, ChannelFactory, SocketAddress> {

	private static final TransportMetadata TRANSPORT_METADATA = new DefaultTransportMetadata(
			"Kaazing", "Channel", false, true, SocketAddress.class,
			IoSessionConfig.class, Object.class);
	
	public DefaultChannelIoConnector(ChannelFactory channelFactory) {
		super(new DefaultChannelIoSessionConfig(), channelFactory);
	}

	public DefaultChannelIoConnector(IoSessionConfigEx sessionConfig,
			ChannelFactory channelFactory) {
		super(sessionConfig, channelFactory);
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
