package com.kaazing.mina.transport.netty.socket;

import java.net.InetSocketAddress;

import org.apache.mina.core.service.DefaultTransportMetadata;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.transport.socket.SocketConnector;
import org.apache.mina.transport.socket.SocketSessionConfig;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;

import com.kaazing.mina.transport.netty.AbstractChannelIoConnector;

public class SocketChannelIoConnector extends AbstractChannelIoConnector<SocketSessionConfig, ClientSocketChannelFactory, InetSocketAddress> implements SocketConnector {

	private static final TransportMetadata TRANSPORT_METADATA = new DefaultTransportMetadata(
			"Kaazing", "SocketChannel", false, true, InetSocketAddress.class,
			SocketSessionConfig.class, Object.class);
	
	public SocketChannelIoConnector(SocketSessionConfig sessionConfig,
			ClientSocketChannelFactory channelFactory) {
		super(sessionConfig, channelFactory);
	}

	@Override
	public void setDefaultRemoteAddress(InetSocketAddress remoteAddress) {
		super.setDefaultRemoteAddress(remoteAddress);
	}

	@Override
	public TransportMetadata getTransportMetadata() {
		return TRANSPORT_METADATA;
	}
}
