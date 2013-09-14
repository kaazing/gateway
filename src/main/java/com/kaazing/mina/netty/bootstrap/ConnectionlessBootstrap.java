/**
 * Copyright (c) 2007-2013, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.bootstrap;

import java.net.SocketAddress;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelException;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineException;

// TODO: contribute this back to Netty 3.x
public class ConnectionlessBootstrap extends org.jboss.netty.bootstrap.ConnectionlessBootstrap {

    public ConnectionlessBootstrap() {
        super();
    }

    public ConnectionlessBootstrap(ChannelFactory channelFactory) {
        super(channelFactory);
    }

    /**
     * Bind a channel asynchronous to the local address
     * specified in the current {@code "localAddress"} option.  This method is
     * similar to the following code:
     *
     * <pre>
     * {@link ConnectionlessBootstrap} b = ...;
     * b.bindAsync(b.getOption("localAddress"));
     * </pre>
     *
     *
     * @return a new {@link ChannelFuture} which will be notified once the Channel is
     * bound and accepts incoming connections
     *
     * @throws IllegalStateException
     *         if {@code "localAddress"} option was not set
     * @throws ClassCastException
     *         if {@code "localAddress"} option's value is
     *         neither a {@link SocketAddress} nor {@code null}
     * @throws ChannelException
     *         if failed to create a new channel and
     *                      bind it to the local address
     */
    public ChannelFuture bindAsync() {
        SocketAddress localAddress = (SocketAddress) getOption("localAddress");
        if (localAddress == null) {
            throw new IllegalStateException("localAddress option is not set.");
        }
        return bindAsync(localAddress);
    }

    /**
     * Creates a new channel which is bound to the specified local address.
     *
     * @return a new {@link ChannelFuture} which will be notified once the Channel is
     *         bound and accepts incoming connections
     *
     * @throws ChannelException
     *         if failed to create a new channel and
     *                      bind it to the local address
     */
    public ChannelFuture bindAsync(final SocketAddress localAddress) {
        if (localAddress == null) {
            throw new NullPointerException("localAddress");
        }

        ChannelPipeline pipeline;
        try {
            pipeline = getPipelineFactory().getPipeline();
        } catch (Exception e) {
            throw new ChannelPipelineException("Failed to initialize a pipeline.", e);
        }

        Channel ch = getFactory().newChannel(pipeline);

        // Apply options.
        boolean success = false;
        try {
            ch.getConfig().setOptions(getOptions());
            success = true;
        } finally {
            if (!success) {
                ch.close();
            }
        }

        // Bind
        ChannelFuture future = ch.bind(localAddress);

        return future;
    }
}
