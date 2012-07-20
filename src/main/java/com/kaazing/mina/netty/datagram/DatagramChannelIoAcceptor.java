/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.datagram;

import java.net.InetSocketAddress;

import org.apache.mina.core.service.DefaultTransportMetadata;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.IoSessionRecycler;
import org.apache.mina.transport.socket.DatagramAcceptor;
import org.apache.mina.transport.socket.DatagramSessionConfig;
import org.jboss.netty.channel.socket.DatagramChannelFactory;

import com.kaazing.mina.netty.ChannelIoAcceptor;
import com.kaazing.mina.netty.DefaultIoAcceptorChannelHandlerFactory;

public class DatagramChannelIoAcceptor extends ChannelIoAcceptor<DatagramSessionConfig, DatagramChannelFactory, InetSocketAddress> implements DatagramAcceptor {

	private static final TransportMetadata TRANSPORT_METADATA = new DefaultTransportMetadata(
			"Kaazing", "DatagramChannel", false, true, InetSocketAddress.class,
			DatagramSessionConfig.class, Object.class);
	
	private IoSessionRecycler sessionRecycler;  // TODO
	
	public DatagramChannelIoAcceptor(DatagramSessionConfig sessionConfig,
			DatagramChannelFactory channelFactory) {
		super(sessionConfig, channelFactory, new DefaultIoAcceptorChannelHandlerFactory());
	}

	@Override
	public IoSessionRecycler getSessionRecycler() {
		return sessionRecycler;
	}

	@Override
	public void setSessionRecycler(IoSessionRecycler sessionRecycler) {
		this.sessionRecycler = sessionRecycler;
	}

	@Override
	public void setDefaultLocalAddress(InetSocketAddress localAddress) {
		super.setDefaultLocalAddress(localAddress);
	}

	@Override
	public TransportMetadata getTransportMetadata() {
		return TRANSPORT_METADATA;
	}
}
