/**
 * Copyright (c) 2007-2013, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.channel;
import java.net.SocketAddress;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.util.internal.StringUtil;

public class DownstreamMessageEventEx implements MessageEvent {

    private Channel channel;
    private ChannelFuture future;
    private Object message;
    private SocketAddress remoteAddress;

    public boolean isResetable() {
        return future == null || future.isDone();
    }

    /**
     * Creates a new instance.
     */
    public void reset(
            Channel channel, ChannelFuture future,
            Object message, SocketAddress remoteAddress) {

        if (this.future != null && !this.future.isDone()) {
            throw new IllegalStateException("Cannot reset message event before future has completed");
        }

        if (channel == null) {
            throw new NullPointerException("channel");
        }
        if (future == null) {
            throw new NullPointerException("future");
        }
        if (message == null) {
            throw new NullPointerException("message");
        }
        this.channel = channel;
        this.future = future;
        this.message = message;
        if (remoteAddress != null) {
            this.remoteAddress = remoteAddress;
        } else {
            this.remoteAddress = channel.getRemoteAddress();
        }
    }

    public Channel getChannel() {
        return channel;
    }

    public ChannelFuture getFuture() {
        return future;
    }

    public Object getMessage() {
        return message;
    }

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
