package com.kaazing.mina.transport.netty.datagram;

import java.net.InetSocketAddress;

import org.apache.mina.core.service.DefaultTransportMetadata;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.transport.socket.DatagramConnector;
import org.apache.mina.transport.socket.DatagramSessionConfig;
import org.jboss.netty.channel.socket.DatagramChannelFactory;

import com.kaazing.mina.transport.netty.AbstractChannelIoConnector;

public class DatagramChannelIoConnector extends AbstractChannelIoConnector<DatagramSessionConfig, DatagramChannelFactory, InetSocketAddress> implements DatagramConnector {

	private static final TransportMetadata TRANSPORT_METADATA = new DefaultTransportMetadata(
			"Kaazing", "DatagramChannel", false, true, InetSocketAddress.class,
			DatagramSessionConfig.class, Object.class);
	
	public DatagramChannelIoConnector(DatagramSessionConfig sessionConfig,
			DatagramChannelFactory channelFactory) {
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
