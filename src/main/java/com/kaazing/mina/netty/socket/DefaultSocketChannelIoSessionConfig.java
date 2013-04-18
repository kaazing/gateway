/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.socket;

import java.net.Socket;

import org.jboss.netty.channel.socket.DefaultSocketChannelConfig;
import org.jboss.netty.channel.socket.SocketChannelConfig;

public class DefaultSocketChannelIoSessionConfig extends SocketChannelIoSessionConfig<SocketChannelConfig> {

    public DefaultSocketChannelIoSessionConfig() {
        super(new DefaultSocketChannelConfig(new Socket()));
    }

}
