/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelUpstreamHandler;

public interface IoAcceptorChannelHandler<T extends ChannelIoAcceptor<?, ?, ?>>
        extends ChannelUpstreamHandler {

    public void setPipelineFactory(ChannelPipelineFactory pipelineFactory);
}
