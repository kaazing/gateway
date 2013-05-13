/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.socket.nio;

import org.apache.mina.transport.socket.DefaultSocketSessionConfigEx;
import org.apache.mina.transport.socket.SocketSessionConfig;
import org.apache.mina.transport.socket.SocketSessionConfigEx;
import org.jboss.netty.channel.socket.nio.NioSocketChannelConfig;

import com.kaazing.mina.core.session.IoSessionConfigEx;
import com.kaazing.mina.netty.socket.SocketChannelIoSessionConfig;

public class NioSocketChannelIoSessionConfig extends SocketChannelIoSessionConfig<NioSocketChannelConfig>
                                          implements SocketSessionConfig {

    // Push Mina default config settings into the channelConfig
    private static final SocketSessionConfigEx DEFAULT = new DefaultSocketSessionConfigEx();

    public NioSocketChannelIoSessionConfig(NioSocketChannelConfig channelConfig) {
        super(channelConfig, DEFAULT);
    }

    @Override
    protected final void doSetAll(IoSessionConfigEx config) {

        super.doSetAll(config);

        if (config instanceof NioSocketChannelIoSessionConfig) {
            channelConfig.setReceiveBufferSizePredictorFactory(
                ((NioSocketChannelIoSessionConfig) config).channelConfig.getReceiveBufferSizePredictorFactory());
        }
    }
}
