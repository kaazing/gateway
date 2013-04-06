/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.session.IoSessionInitializer;
import org.jboss.netty.channel.Channel;

import com.kaazing.mina.core.service.IoServiceEx;

public interface ChannelIoService extends IoServiceEx {

    ChannelIoSession createSession(Channel channel);

    IoSessionIdleTracker getSessionIdleTracker();

    void initializeSession(ChannelIoSession session, IoFuture future, IoSessionInitializer<?> sessionInitializer);

}
