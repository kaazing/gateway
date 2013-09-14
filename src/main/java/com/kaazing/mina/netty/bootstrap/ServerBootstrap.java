/**
 * Copyright (c) 2007-2013, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.bootstrap;

import java.net.SocketAddress;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandler;

public interface ServerBootstrap extends Bootstrap {

    void setFactory(ChannelFactory factory);
    ChannelFactory getFactory();

    void setParentHandler(ChannelHandler parentHandler);
    ChannelHandler getParentHandler();

    Channel bind(SocketAddress localAddress);
    ChannelFuture bindAsync(SocketAddress localAddress);
}
