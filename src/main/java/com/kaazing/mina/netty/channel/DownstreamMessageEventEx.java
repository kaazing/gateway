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

    private final DefaultChannelFutureEx future;
    private Channel channel;
    private Object message;
    private SocketAddress remoteAddress;

    public DownstreamMessageEventEx() {
        future = new DefaultChannelFutureEx();
    }

    public boolean isResetable() {
        return future.isResetable();
    }

    /**
     * Initializes the instance.
     */
    public void reset(Channel channel, Object message, SocketAddress remoteAddress, boolean cancellable) {

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
