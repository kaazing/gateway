/**
 * Copyright (c) 2007-2013, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import static java.lang.System.currentTimeMillis;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.write.WriteRequest;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;

import com.kaazing.mina.core.session.AbstractIoSession;

final class ChannelWriteFutureListener implements ChannelFutureListener {
    private final IoFilterChain filterChain;
    private final WriteRequest request;
    private final int bytesWritten;

    public ChannelWriteFutureListener(IoFilterChain filterChain, WriteRequest request, int bytesWritten) {
        this.filterChain = filterChain;
        this.request = request;
        this.bytesWritten = bytesWritten;
    }

    @Override
    public void operationComplete(ChannelFuture future) throws Exception {
        operationComplete(future, filterChain, request, bytesWritten);
    }

    public static void operationComplete(ChannelFuture future, IoFilterChain filterChain, WriteRequest request,
                                         int bytesWritten) {
        if (future.isSuccess()) {
//            filterChain.fireMessageSent(request);
            ((AbstractIoSession) filterChain.getSession()).increaseWrittenBytes(bytesWritten, currentTimeMillis());
            setFutureWritten(filterChain, request.getFuture());
        }
        else {
            filterChain.fireExceptionCaught(future.getCause());
        }
    }

    private static void setFutureWritten(IoFilterChain filterChain, WriteFuture future) {
        try {
            future.setWritten();
        } catch (Throwable t) {
            filterChain.fireExceptionCaught(t);
        }
    }
}
