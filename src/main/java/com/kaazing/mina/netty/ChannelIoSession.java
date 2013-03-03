/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty;

import java.net.SocketAddress;
import java.util.concurrent.Executor;

import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.TransportMetadata;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelConfig;

import com.kaazing.mina.core.service.IoProcessorEx;
import com.kaazing.mina.core.session.AbstractIoSessionEx;

public class ChannelIoSession extends AbstractIoSessionEx {

    private final ChannelIoService service;
    private final Channel channel;
    private final DefaultChannelIoSessionConfig config;
    private final IoHandler handler;
    private final ChannelIoProcessor processor;
    private final TransportMetadata transportMetadata;

    public ChannelIoSession(ChannelIoService service, Channel channel, Thread ioThread, Executor ioExecutor) {
        super(ioThread, ioExecutor);
        this.service = service;
        this.channel = channel;
        this.config = new DefaultChannelIoSessionConfig(channel.getConfig());
        this.config.setAll(service.getSessionConfig());
        this.handler = service.getHandler();
        this.processor = ChannelIoProcessor.getInstance();
        this.transportMetadata = service.getTransportMetadata();
    }

    public ChannelIoService getService() {
        return service;
    }

    public Channel getChannel() {
        return channel;
    }

    @Override
    public IoHandler getHandler() {
        return handler;
    }

    @Override
    public ChannelIoSessionConfig<ChannelConfig> getConfig() {
        return config;
    }

    @Override
    public IoProcessorEx<ChannelIoSession> getProcessor() {
        return processor;
    }

    @Override
    public SocketAddress getLocalAddress() {
        return channel.getLocalAddress();
    }

    @Override
    public SocketAddress getRemoteAddress() {
        return channel.getRemoteAddress();
    }

    @Override
    public TransportMetadata getTransportMetadata() {
        return transportMetadata;
    }

    @Override
    public boolean isReadSuspended() {
        return channel.isReadable();
    }

    @Override
    public void resumeRead() {
        channel.setReadable(true);
    }

    @Override
    public void suspendRead() {
        channel.setReadable(false);
    }

}
