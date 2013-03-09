/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.datagram;

import java.net.DatagramSocket;
import java.net.SocketException;

import org.jboss.netty.channel.socket.DefaultDatagramChannelConfig;

public class DefaultDatagramChannelIoSessionConfig extends DatagramChannelIoSessionConfig {

    public DefaultDatagramChannelIoSessionConfig() throws SocketException {
        super(new DefaultDatagramChannelConfig(new DatagramSocket()));
    }

}
