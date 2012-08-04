/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

/**
 * 
 */
package com.kaazing.mina.netty;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.write.WriteRequest;

final class ChannelWriteFutureListener implements ChannelFutureListener {
	private final IoFilterChain filterChain;
	private final WriteRequest request;

	public ChannelWriteFutureListener(IoFilterChain filterChain, WriteRequest request) {
		this.filterChain = filterChain;
		this.request = request;
	}

	@Override
	public void operationComplete(ChannelFuture future) throws Exception {
		if (future.isSuccess()) {
			filterChain.fireMessageSent(request);
		}
		else {
			filterChain.fireExceptionCaught(future.cause());
		}
	}
}