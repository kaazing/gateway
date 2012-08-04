/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.socket;

import static io.netty.buffer.ChannelBufType.BYTE;
import static io.netty.channel.ChannelOption.SO_BACKLOG;
import static io.netty.channel.ChannelOption.SO_REUSEADDR;
import io.netty.channel.EventLoop;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.ServerSocketChannelConfig;
import io.netty.channel.socket.SocketChannel;

import java.net.InetSocketAddress;

import org.apache.mina.core.service.DefaultTransportMetadata;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.transport.socket.SocketAcceptor;
import org.apache.mina.transport.socket.SocketSessionConfig;

import com.kaazing.mina.netty.ChannelIoAcceptor;

public abstract class SocketChannelIoAcceptor<E extends EventLoop> extends ChannelIoAcceptor<E, SocketSessionConfig, ServerSocketChannel, SocketChannel, InetSocketAddress> implements SocketAcceptor {

	static final TransportMetadata TRANSPORT_METADATA = new DefaultTransportMetadata(
			"Kaazing", "SocketChannel", false, true, InetSocketAddress.class,
			SocketSessionConfig.class, Object.class);
	
	private volatile int backlog;
	private volatile boolean reuseAddress;

	public SocketChannelIoAcceptor(SocketSessionConfig sessionConfig, E parentEventLoop, E childEventLoop) {
		super(BYTE, sessionConfig, parentEventLoop, childEventLoop);
		
		ServerSocketChannel newServerChannel = newServerChannel(parentEventLoop, childEventLoop);
		ServerSocketChannelConfig config = newServerChannel.config();
		this.backlog = config.getBacklog();
		this.reuseAddress = config.isReuseAddress();
		newServerChannel.close();
	}


	@Override
	public int getBacklog() {
		return backlog;
	}

	@Override
	public void setBacklog(int backlog) {
		this.backlog = backlog;
		
		getBootstrap().option(SO_BACKLOG, backlog);
	}
	
	@Override
	public boolean isReuseAddress() {
		return reuseAddress;
	}

	@Override
	public void setReuseAddress(boolean reuseAddress) {
		this.reuseAddress = reuseAddress;

		getBootstrap().option(SO_REUSEADDR, reuseAddress);
}

	@Override
	public void setDefaultLocalAddress(InetSocketAddress localAddress) {
		super.setDefaultLocalAddress(localAddress);
	}

	@Override
	public TransportMetadata getTransportMetadata() {
		return TRANSPORT_METADATA;
	}


	@Override
	protected abstract ServerSocketChannel newServerChannel(E parentEventLoop, E childEventLoop);
}
