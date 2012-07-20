/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.datagram;

import java.net.InetSocketAddress;

import org.apache.mina.core.service.DefaultTransportMetadata;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.transport.socket.DatagramConnector;
import org.apache.mina.transport.socket.DatagramSessionConfig;
import org.jboss.netty.channel.socket.DatagramChannelFactory;

import com.kaazing.mina.netty.ChannelIoConnector;

public class DatagramChannelIoConnector extends ChannelIoConnector<DatagramSessionConfig, DatagramChannelFactory, InetSocketAddress> implements DatagramConnector {

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
