/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.socket;

import java.net.DatagramSocket;
import java.net.SocketException;

import org.jboss.netty.channel.socket.DefaultDatagramChannelConfig;

public class DefaultDatagramChannelIoSessionConfig extends DatagramChannelIoSessionConfig {

    public DefaultDatagramChannelIoSessionConfig() {
        super(new DefaultDatagramChannelConfig(newDatagramSocket()));
    }

    private static DatagramSocket newDatagramSocket() {
        DatagramSocket result;
        try {
            result = new DatagramSocket();
        }
        catch (SocketException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

}
