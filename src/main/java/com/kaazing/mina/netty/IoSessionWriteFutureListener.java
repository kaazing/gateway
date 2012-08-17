/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.write.WriteRequest;

final class IoSessionWriteFutureListener implements ChannelFutureListener {
	private final IoFilterChain filterChain;
	private final WriteRequest request;

	public IoSessionWriteFutureListener(IoFilterChain filterChain, WriteRequest request) {
		this.filterChain = filterChain;
		this.request = request;
	}

	@Override
	public void operationComplete(ChannelFuture future) throws Exception {
		if (future.isSuccess()) {
			Object message = request.getMessage();
			if (message != ChannelIoSession.FLUSH) {
			    IoBuffer buf = (IoBuffer)message;
			    buf.reset();
			}
			filterChain.fireMessageSent(request);
		}
		else {
			filterChain.fireExceptionCaught(future.cause());
		}
	}
}