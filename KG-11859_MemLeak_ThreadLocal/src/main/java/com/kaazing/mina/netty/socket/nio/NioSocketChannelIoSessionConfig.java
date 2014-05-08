/**
 * Copyright (c) 2007-2013, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.socket.nio;

import org.apache.mina.transport.socket.SocketAcceptor;
import org.apache.mina.transport.socket.SocketSessionConfig;
import org.jboss.netty.channel.socket.nio.NioSocketChannelConfig;

import com.kaazing.mina.core.service.IoServiceEx;
import com.kaazing.mina.core.session.IoSessionConfigEx;
import com.kaazing.mina.netty.socket.SocketChannelIoSessionConfig;

public class NioSocketChannelIoSessionConfig extends SocketChannelIoSessionConfig<NioSocketChannelConfig>
                                          implements SocketSessionConfig {

    public NioSocketChannelIoSessionConfig(NioSocketChannelConfig channelConfig) {
        super(channelConfig);
    }

    public void init(final IoServiceEx parent) {
        // If we are a SocketAcceptor we always want to flush REUSEADDRESS into the socket.
        if (parent instanceof SocketAcceptor) {
            setReuseAddress(true);
        }
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
