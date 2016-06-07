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
package org.kaazing.mina.netty.channel;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.util.internal.StringUtil;

import org.kaazing.mina.netty.buffer.ByteBufferWrappingChannelBuffer;

public class DownstreamMessageEventEx implements MessageEvent {

    private final DefaultChannelFutureEx future;
    private final ByteBufferWrappingChannelBuffer channelBuf;
    private Channel channel;
    private Object message;
    private SocketAddress remoteAddress;

    public DownstreamMessageEventEx() {
        future = new DefaultChannelFutureEx();
        channelBuf = new ByteBufferWrappingChannelBuffer();
    }

    public boolean isResetable() {
        return future.isResetable();
    }

    /**
     * Initializes the instance.
     */
    public void reset(Channel channel, ByteBuffer message, SocketAddress remoteAddress, boolean cancellable) {

        if (!future.isResetable()) {
            throw new IllegalStateException("Cannot reset message event before future has completed");
        }

        if (channel == null) {
            throw new NullPointerException("channel");
        }
        if (message == null) {
            throw new NullPointerException("message");
        }
        this.channel = channel;
        this.future.reset(channel, cancellable);
        // note: NETTY duplicates original ByteBuffer when converting ChannelBuffer to ByteBuffer
        //       instead, wrap with a non-duplicating ChannelBuffer
        this.message = channelBuf.wrap(message);
        if (remoteAddress != null) {
            this.remoteAddress = remoteAddress;
        } else {
            this.remoteAddress = channel.getRemoteAddress();
        }
    }

    @Override
    public Channel getChannel() {
        return channel;
    }

    @Override
    public ChannelFuture getFuture() {
        return future;
    }

    @Override
    public Object getMessage() {
        return message;
    }

    @Override
    public SocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    @Override
    public String toString() {
        if (getRemoteAddress() == getChannel().getRemoteAddress()) {
            return getChannel().toString() + " WRITE: " +
                   StringUtil.stripControlCharacters(getMessage());
        } else {
            return getChannel().toString() + " WRITE: " +
                   StringUtil.stripControlCharacters(getMessage()) + " to " +
                   getRemoteAddress();
        }
    }
}
