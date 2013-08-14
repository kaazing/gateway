/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import org.jboss.netty.channel.group.ChannelGroup;

public class DefaultIoAcceptorChannelHandlerFactory<T extends ChannelIoAcceptor<?, ?, ?>>
        implements IoAcceptorChannelHandlerFactory<T> {

    @Override
    public IoAcceptorChannelHandler<T> createHandler(T acceptor,
                                                                              ChannelGroup channelGroup) {
        return new DefaultIoAcceptorChannelHandler<T>(acceptor, channelGroup);
    }

}
