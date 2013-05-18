/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.write.WriteRequest;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;

final class ChannelWriteFutureListener implements ChannelFutureListener {
    private final IoFilterChain filterChain;
    private final WriteRequest request;

    public ChannelWriteFutureListener(IoFilterChain filterChain, WriteRequest request) {
        this.filterChain = filterChain;
        this.request = request;
    }

    @Override
    public void operationComplete(ChannelFuture future) throws Exception {
        operationComplete(future, filterChain, request);
    }

    public static void operationComplete(ChannelFuture future, IoFilterChain filterChain, WriteRequest request) {
        if (future.isSuccess()) {
            filterChain.fireMessageSent(request);
        }
        else {
            filterChain.fireExceptionCaught(future.getCause());
        }
    }
}
