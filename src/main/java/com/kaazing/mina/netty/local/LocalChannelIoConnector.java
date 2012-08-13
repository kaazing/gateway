/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.local;

import static com.kaazing.mina.netty.local.LocalChannelIoAcceptor.TRANSPORT_METADATA;
import static io.netty.buffer.ChannelBufType.MESSAGE;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalEventLoopGroup;

import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.IoSessionConfig;

import com.kaazing.mina.netty.ChannelIoConnector;

public class LocalChannelIoConnector extends ChannelIoConnector<LocalEventLoopGroup, IoSessionConfig, LocalChannel, LocalAddress> {

	public LocalChannelIoConnector() {
		this(new LocalEventLoopGroup());
	}

	public LocalChannelIoConnector(LocalEventLoopGroup group) {
		this(new LocalChannelIoSessionConfig(), group);
	}

	public LocalChannelIoConnector(IoSessionConfig sessionConfig,
			LocalEventLoopGroup group) {
		super(sessionConfig, group, MESSAGE);
	}

	@Override
	protected LocalChannel newChannel(LocalEventLoopGroup group) {
		return new LocalChannel();
	}

	@Override
	public TransportMetadata getTransportMetadata() {
		return TRANSPORT_METADATA;
	}

}
