/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import org.jboss.netty.channel.ChannelConfig;
import org.jboss.netty.channel.DefaultChannelConfig;

public class DefaultChannelIoSessionConfig extends ChannelIoSessionConfig<ChannelConfig> {

	public DefaultChannelIoSessionConfig() {
		this(new DefaultChannelConfig());
	}
	
	public DefaultChannelIoSessionConfig(ChannelConfig channelConfig) {
		super(channelConfig);
	}
}
