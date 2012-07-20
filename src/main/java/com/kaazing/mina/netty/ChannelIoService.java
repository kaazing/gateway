/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.service.IoService;
import org.apache.mina.core.session.IoSessionInitializer;

public interface ChannelIoService extends IoService {

	public void initializeSession(ChannelIoSession session, IoFuture future, IoSessionInitializer<?> sessionInitializer);
	
}
