package com.kaazing.mina.netty;

import java.net.SocketAddress;

import org.apache.mina.core.service.DefaultTransportMetadata;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.IoSessionConfig;
import org.jboss.netty.channel.ServerChannelFactory;

public class ChannelIoAcceptor extends AbstractChannelIoAcceptor<IoSessionConfig, ServerChannelFactory, SocketAddress> {

	private static final TransportMetadata TRANSPORT_METADATA = new DefaultTransportMetadata(
			"Kaazing", "Channel", false, true, SocketAddress.class,
			IoSessionConfig.class, Object.class);
	
	public ChannelIoAcceptor(ServerChannelFactory channelFactory) {
		this(new ChannelIoSessionConfig(), channelFactory);
	}

	public ChannelIoAcceptor(IoSessionConfig sessionConfig,
			ServerChannelFactory channelFactory) {
		super(sessionConfig, channelFactory);
	}

	@Override
	public TransportMetadata getTransportMetadata() {
		return TRANSPORT_METADATA;
	}

}
