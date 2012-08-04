/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.local;

import static com.kaazing.mina.netty.local.LocalChannelIoAcceptor.TRANSPORT_METADATA;
import static io.netty.buffer.ChannelBufType.MESSAGE;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalEventLoop;

import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.IoSessionConfig;

import com.kaazing.mina.netty.ChannelIoConnector;

public class LocalChannelIoConnector extends ChannelIoConnector<IoSessionConfig, LocalChannel, LocalAddress> {

	public LocalChannelIoConnector() {
		this(new LocalEventLoop());
	}

	public LocalChannelIoConnector(LocalEventLoop eventLoop) {
		this(new LocalChannelIoSessionConfig(), eventLoop);
	}

	public LocalChannelIoConnector(IoSessionConfig sessionConfig,
			LocalEventLoop eventLoop) {
		super(sessionConfig, eventLoop, MESSAGE);
	}

	@Override
	protected LocalChannel newChannel() {
		return new LocalChannel();
	}

	@Override
	public TransportMetadata getTransportMetadata() {
		return TRANSPORT_METADATA;
	}

}
