/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import org.jboss.netty.channel.group.ChannelGroup;

public class DefaultIoAcceptorChannelHandlerFactory
       implements IoAcceptorChannelHandlerFactory {

    @Override
    public IoAcceptorChannelHandler createHandler(ChannelIoAcceptor<?, ?, ?> acceptor, ChannelGroup channelGroup) {
        return new IoAcceptorChannelHandler(acceptor, channelGroup);
    }

}
