/**
 * Copyright 2007-2016, Kaazing Corporation. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kaazing.mina.netty;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.WriteFuture;
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
//            filterChain.fireMessageSent(request);
            setFutureWritten(filterChain, request.getFuture());
        }
        else {
            Throwable cause = future.getCause();
            request.getFuture().setException(cause);
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
