/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import static io.netty.channel.ChannelOption.SO_RCVBUF;
import static io.netty.channel.ChannelOption.UDP_RECEIVE_PACKET_SIZE;
import io.netty.channel.ChannelConfig;

import org.apache.mina.core.session.AbstractIoSessionConfig;
import org.apache.mina.core.session.IoSessionConfig;

public class ChannelIoSessionConfig<T extends ChannelConfig> extends AbstractIoSessionConfig {

	protected final T channelConfig;
	
	public ChannelIoSessionConfig(T channelConfig) {
		this.channelConfig = channelConfig;
	}
	
	public void setOption(String name, Object value) {
		switch (name.charAt(0)) {
		case 'r':
			if ("readBufferSize".equals(name)) {
				Integer readBufferSize = (Integer)value;
				channelConfig.setOption(SO_RCVBUF, readBufferSize);
				channelConfig.setOption(UDP_RECEIVE_PACKET_SIZE, readBufferSize);
			}
			break;
		}
	}

	@Override
	protected void doSetAll(IoSessionConfig config) {

		int readBufferSize = config.getReadBufferSize();
		
		channelConfig.setOption(SO_RCVBUF, readBufferSize);
		channelConfig.setOption(UDP_RECEIVE_PACKET_SIZE, readBufferSize);
	}

}
