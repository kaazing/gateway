/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.nio;

import java.net.InetSocketAddress;

import org.apache.mina.core.service.DefaultTransportMetadata;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.transport.socket.SocketSessionConfig;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioSocketChannel;

import com.kaazing.mina.netty.ChannelIoSession;
import com.kaazing.mina.netty.DefaultIoAcceptorChannelHandlerFactory;
import com.kaazing.mina.netty.IoAcceptorChannelHandlerFactory;
import com.kaazing.mina.netty.socket.IoAcceptorSocketChannelFactory;
import com.kaazing.mina.netty.socket.SocketChannelIoAcceptor;

public class NioSocketChannelIoAcceptor extends SocketChannelIoAcceptor {

	private static final TransportMetadata TRANSPORT_METADATA = new DefaultTransportMetadata(
			"Kaazing", "NioSocketChannel", false, true, InetSocketAddress.class,
			SocketSessionConfig.class, Object.class);
	
	public NioSocketChannelIoAcceptor(SocketSessionConfig sessionConfig,
			final NioServerSocketChannelFactory channelFactory) {
		super(sessionConfig, new IoAcceptorSocketChannelFactory(channelFactory), new DefaultIoAcceptorChannelHandlerFactory());
	}

	public NioSocketChannelIoAcceptor(SocketSessionConfig sessionConfig,
			final NioServerSocketChannelFactory channelFactory, IoAcceptorChannelHandlerFactory handlerFactory) {
		super(sessionConfig, new IoAcceptorSocketChannelFactory(channelFactory), handlerFactory);
	}

	@Override
	public TransportMetadata getTransportMetadata() {
		return TRANSPORT_METADATA;
	}
	
    @Override
    public ChannelIoSession createSession(ChannelHandlerContext context) {
        return new NioSocketChannelIoSession(this, (NioSocketChannel) context.getChannel());
    }
    
}
