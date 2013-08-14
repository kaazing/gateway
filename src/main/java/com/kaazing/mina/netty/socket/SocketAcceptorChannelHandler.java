/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.socket;

import com.kaazing.mina.netty.IoAcceptorChannelHandler;

public interface SocketAcceptorChannelHandler
        extends IoAcceptorChannelHandler<SocketChannelIoAcceptor> {

    public int getBacklog();

    public boolean isReuseAddress();

}
