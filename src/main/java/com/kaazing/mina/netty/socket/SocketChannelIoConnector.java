/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.socket;

import static com.kaazing.mina.netty.socket.SocketChannelIoAcceptor.TRANSPORT_METADATA;
import static io.netty.buffer.ChannelBufType.BYTE;
import io.netty.channel.EventLoop;
import io.netty.channel.socket.SocketChannel;

import java.net.InetSocketAddress;

import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.transport.socket.SocketConnector;
import org.apache.mina.transport.socket.SocketSessionConfig;

import com.kaazing.mina.netty.ChannelIoConnector;

public abstract class SocketChannelIoConnector extends ChannelIoConnector<SocketSessionConfig, SocketChannel, InetSocketAddress> implements SocketConnector {

	public SocketChannelIoConnector(SocketSessionConfig sessionConfig, EventLoop eventLoop) {
		super(sessionConfig, eventLoop, BYTE);
	}

	@Override
	public void setDefaultRemoteAddress(InetSocketAddress remoteAddress) {
		super.setDefaultRemoteAddress(remoteAddress);
	}

	@Override
	public TransportMetadata getTransportMetadata() {
		return TRANSPORT_METADATA;
	}

	@Override
	protected abstract SocketChannel newChannel();
}
