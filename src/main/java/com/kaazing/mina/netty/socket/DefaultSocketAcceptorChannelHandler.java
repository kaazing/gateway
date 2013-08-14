/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.socket;

import org.jboss.netty.channel.group.ChannelGroup;

import com.kaazing.mina.netty.DefaultIoAcceptorChannelHandler;

public class DefaultSocketAcceptorChannelHandler extends DefaultIoAcceptorChannelHandler<SocketChannelIoAcceptor>
        implements SocketAcceptorChannelHandler {

    public DefaultSocketAcceptorChannelHandler(SocketChannelIoAcceptor acceptor, ChannelGroup channelGroup) {
        super(acceptor, channelGroup);
    }

    @Override
    public int getBacklog() {
        return getAcceptor().getBacklog();
    }

    @Override
    public boolean isReuseAddress() {
        return getAcceptor().isReuseAddress();
    }
}
