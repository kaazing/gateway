/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.local;

import io.netty.channel.ChannelConfig;
import io.netty.channel.DefaultChannelConfig;

import com.kaazing.mina.netty.ChannelIoSessionConfig;

public class LocalChannelIoSessionConfig extends ChannelIoSessionConfig<ChannelConfig> {

	public LocalChannelIoSessionConfig() {
		this(new DefaultChannelConfig());
	}
	
	public LocalChannelIoSessionConfig(ChannelConfig channelConfig) {
		super(channelConfig);
	}
	
}
