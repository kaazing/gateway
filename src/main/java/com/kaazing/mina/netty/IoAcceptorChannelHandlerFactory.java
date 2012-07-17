package com.kaazing.mina.netty;


public interface IoAcceptorChannelHandlerFactory {

	public IoAcceptorChannelHandler createHandler(ChannelIoAcceptor<?, ?, ?> acceptor);
}
