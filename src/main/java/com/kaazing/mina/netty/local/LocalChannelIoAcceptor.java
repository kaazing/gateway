/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.local;

import static io.netty.buffer.ChannelBufType.MESSAGE;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalEventLoopGroup;
import io.netty.channel.local.LocalServerChannel;

import org.apache.mina.core.service.DefaultTransportMetadata;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.IoSessionConfig;

import com.kaazing.mina.netty.ChannelIoAcceptor;

public class LocalChannelIoAcceptor extends ChannelIoAcceptor<LocalEventLoopGroup, IoSessionConfig, LocalServerChannel, LocalChannel, LocalAddress> {

	static final TransportMetadata TRANSPORT_METADATA = new DefaultTransportMetadata(
			"Kaazing", "LocalChannel", false, true, LocalAddress.class,
			IoSessionConfig.class, Object.class);
	
	public LocalChannelIoAcceptor() {
		this(new LocalEventLoopGroup());
	}
	
	public LocalChannelIoAcceptor(LocalEventLoopGroup childGroup) {
		this(new LocalChannelIoSessionConfig(), new LocalEventLoopGroup(), childGroup);
	}
	
	public LocalChannelIoAcceptor(IoSessionConfig sessionConfig,
			LocalEventLoopGroup parentGroup, LocalEventLoopGroup childGroup) {
		super(MESSAGE, sessionConfig, parentGroup, childGroup);
	}

	@Override
	protected LocalServerChannel newServerChannel(LocalEventLoopGroup parentGroup, LocalEventLoopGroup childGroup) {
		return new LocalServerChannel();
	}

	@Override
	public TransportMetadata getTransportMetadata() {
		return TRANSPORT_METADATA;
	}

}
