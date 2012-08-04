/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.local;

import static io.netty.buffer.ChannelBufType.MESSAGE;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalEventLoop;
import io.netty.channel.local.LocalServerChannel;

import org.apache.mina.core.service.DefaultTransportMetadata;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.IoSessionConfig;

import com.kaazing.mina.netty.ChannelIoAcceptor;

public class LocalChannelIoAcceptor extends ChannelIoAcceptor<LocalEventLoop, IoSessionConfig, LocalServerChannel, LocalChannel, LocalAddress> {

	static final TransportMetadata TRANSPORT_METADATA = new DefaultTransportMetadata(
			"Kaazing", "LocalChannel", false, true, LocalAddress.class,
			IoSessionConfig.class, Object.class);
	
	public LocalChannelIoAcceptor() {
		this(new LocalEventLoop());
	}
	
	public LocalChannelIoAcceptor(LocalEventLoop childEventLoop) {
		this(new LocalChannelIoSessionConfig(), new LocalEventLoop(), childEventLoop);
	}
	
	public LocalChannelIoAcceptor(IoSessionConfig sessionConfig,
			LocalEventLoop parentEventLoop, LocalEventLoop childEventLoop) {
		super(MESSAGE, sessionConfig, parentEventLoop, childEventLoop);
	}

	@Override
	protected LocalServerChannel newServerChannel(LocalEventLoop parentEventLoop, LocalEventLoop childEventLoop) {
		return new LocalServerChannel();
	}

	@Override
	public TransportMetadata getTransportMetadata() {
		return TRANSPORT_METADATA;
	}

}
