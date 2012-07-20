/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;


public interface IoAcceptorChannelHandlerFactory {

	public IoAcceptorChannelHandler createHandler(ChannelIoAcceptor<?, ?, ?> acceptor);
}
