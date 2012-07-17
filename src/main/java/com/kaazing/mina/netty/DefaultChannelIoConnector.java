package com.kaazing.mina.netty;

import java.net.SocketAddress;

import org.apache.mina.core.service.DefaultTransportMetadata;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.IoSessionConfig;
import org.jboss.netty.channel.ChannelFactory;

public class DefaultChannelIoConnector extends ChannelIoConnector<IoSessionConfig, ChannelFactory, SocketAddress> {

	private static final TransportMetadata TRANSPORT_METADATA = new DefaultTransportMetadata(
			"Kaazing", "Channel", false, true, SocketAddress.class,
			IoSessionConfig.class, Object.class);
	
	public DefaultChannelIoConnector(ChannelFactory channelFactory) {
		super(new DefaultChannelIoSessionConfig(), channelFactory);
	}

	public DefaultChannelIoConnector(IoSessionConfig sessionConfig,
			ChannelFactory channelFactory) {
		super(sessionConfig, channelFactory);
	}

	@Override
	public TransportMetadata getTransportMetadata() {
		return TRANSPORT_METADATA;
	}

}
