/**
 * Copyright (c) 2007-2013, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.socket;

import java.net.DatagramSocket;
import java.net.SocketException;

import org.apache.mina.transport.socket.DatagramSessionConfigEx;
import org.apache.mina.transport.socket.DefaultDatagramSessionConfigEx;
import org.jboss.netty.channel.socket.DefaultDatagramChannelConfig;

// TODO: create NIO-specific subclasses and remove this class (like DefaultSocketChannelIoSessionConfig)
public class DefaultDatagramChannelIoSessionConfig extends DatagramChannelIoSessionConfig {

    // Push Mina default config settings into the channelConfig
    private static final DatagramSessionConfigEx DEFAULT = new DefaultDatagramSessionConfigEx();

    public DefaultDatagramChannelIoSessionConfig() {
        super(new DefaultDatagramChannelConfig(newDatagramSocket()), DEFAULT);
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
