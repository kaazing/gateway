/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

public class DefaultIoAcceptorChannelHandlerFactory implements
        IoAcceptorChannelHandlerFactory {

    @Override
    public IoAcceptorChannelHandler createHandler(ChannelIoService acceptor) {
        return new IoAcceptorChannelHandler(acceptor);
    }

}
