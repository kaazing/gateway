/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.group.ChannelGroup;

public class DefaultIoAcceptorChannelHandlerFactory<T extends ChannelIoAcceptor<?, ?, ?>>
        implements IoAcceptorChannelHandlerFactory<T> {

    @Override
    public IoAcceptorChannelHandler createHandler(T acceptor, ChannelGroup channelGroup, ChannelHandler bindHandler) {
        return new DefaultIoAcceptorChannelHandler(acceptor, channelGroup, bindHandler);
    }
}
