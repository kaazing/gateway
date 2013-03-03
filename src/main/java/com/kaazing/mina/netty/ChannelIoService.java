/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.session.IoSessionInitializer;
import org.jboss.netty.channel.ChannelHandlerContext;

import com.kaazing.mina.core.service.IoServiceEx;

public interface ChannelIoService extends IoServiceEx {

    ChannelIoSession createSession(ChannelHandlerContext context);

    void initializeSession(ChannelIoSession session, IoFuture future, IoSessionInitializer<?> sessionInitializer);

}
