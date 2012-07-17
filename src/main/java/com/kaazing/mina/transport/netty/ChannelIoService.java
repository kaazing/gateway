package com.kaazing.mina.transport.netty;

import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.service.IoService;
import org.apache.mina.core.session.IoSessionInitializer;

public interface ChannelIoService extends IoService {

	public void initializeSession(ChannelIoSession session, IoFuture future, IoSessionInitializer<?> sessionInitializer);
	
}
