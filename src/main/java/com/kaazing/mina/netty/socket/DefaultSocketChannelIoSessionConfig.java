/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.socket;

import java.net.Socket;

import org.jboss.netty.channel.socket.DefaultSocketChannelConfig;

public class DefaultSocketChannelIoSessionConfig extends SocketChannelIoSessionConfig {

	public DefaultSocketChannelIoSessionConfig() {
		super(new DefaultSocketChannelConfig(new Socket()));
	}
		
}
