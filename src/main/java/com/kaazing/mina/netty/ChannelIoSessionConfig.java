package com.kaazing.mina.netty;

import org.jboss.netty.channel.ChannelConfig;
import org.jboss.netty.channel.DefaultChannelConfig;

public class ChannelIoSessionConfig extends AbstractChannelIoSessionConfig<ChannelConfig> {

	public ChannelIoSessionConfig() {
		this(new DefaultChannelConfig());
	}
	
	public ChannelIoSessionConfig(ChannelConfig channelConfig) {
		super(channelConfig);
	}
}
