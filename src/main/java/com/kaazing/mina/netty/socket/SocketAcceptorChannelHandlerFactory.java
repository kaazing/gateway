/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.socket;

import org.jboss.netty.channel.group.ChannelGroup;

import com.kaazing.mina.netty.IoAcceptorChannelHandlerFactory;

public interface SocketAcceptorChannelHandlerFactory
        extends IoAcceptorChannelHandlerFactory<SocketChannelIoAcceptor> {
    @Override
    DefaultSocketAcceptorChannelHandler createHandler(SocketChannelIoAcceptor acceptor, ChannelGroup channelGroup);
}