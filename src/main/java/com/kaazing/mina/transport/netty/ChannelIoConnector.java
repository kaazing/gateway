package com.kaazing.mina.transport.netty;

import java.net.SocketAddress;

import org.apache.mina.core.service.DefaultTransportMetadata;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.IoSessionConfig;
import org.jboss.netty.channel.ChannelFactory;

public class ChannelIoConnector extends AbstractChannelIoConnector<IoSessionConfig, ChannelFactory, SocketAddress> {

	private static final TransportMetadata TRANSPORT_METADATA = new DefaultTransportMetadata(
			"Kaazing", "Channel", false, true, SocketAddress.class,
			IoSessionConfig.class, Object.class);
	
	public ChannelIoConnector(ChannelFactory channelFactory) {
		super(new ChannelIoSessionConfig(), channelFactory);
	}

	public ChannelIoConnector(IoSessionConfig sessionConfig,
			ChannelFactory channelFactory) {
		super(sessionConfig, channelFactory);
	}

	@Override
	public TransportMetadata getTransportMetadata() {
		return TRANSPORT_METADATA;
	}

}
