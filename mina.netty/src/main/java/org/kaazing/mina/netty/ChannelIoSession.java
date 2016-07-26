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

import java.net.SocketAddress;
import java.util.concurrent.Executor;

import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.TransportMetadata;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelConfig;

import org.jboss.netty.channel.socket.nio.AbstractNioWorker;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.service.IoProcessorEx;
import org.kaazing.mina.core.session.AbstractIoSessionEx;
import org.kaazing.mina.netty.ChannelIoBufferAllocator.ChannelIoBuffer;

public class ChannelIoSession<C extends ChannelConfig> extends AbstractIoSessionEx {

    private static final IoBufferAllocatorEx<ChannelIoBuffer> BUFFER_ALLOCATOR = new ChannelIoBufferAllocator();

    private final ChannelIoService service;
    private final Channel channel;
    private final ChannelIoSessionConfig<C> config;
    private final IoHandler handler;
    private final IoProcessorEx<ChannelIoSession<? extends ChannelConfig>> processor;
    private final TransportMetadata transportMetadata;
    private volatile boolean closedReceived;

    public ChannelIoSession(ChannelIoService service, IoProcessorEx<ChannelIoSession<? extends ChannelConfig>> processor,
            Channel channel, ChannelIoSessionConfig<C> config, Thread ioThread, Executor ioExecutor) {
        super(0, ioThread, ioExecutor, service.getThreadLocalWriteRequest(0));
        this.service = service;
        this.channel = channel;
        this.config = config;
        this.config.setAll(service.getSessionConfig());
        this.handler = service.getHandler();
        this.processor = processor;
        this.transportMetadata = service.getTransportMetadata();
    }

    @Override
    public IoBufferAllocatorEx<ChannelIoBuffer> getBufferAllocator() {
        return BUFFER_ALLOCATOR;
    }

    @Override
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
    public ChannelIoSessionConfig<C> getConfig() {
        return config;
    }

    @Override
    public IoProcessorEx<ChannelIoSession<? extends ChannelConfig>> getProcessor() {
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

    protected boolean isClosedReceived() {
        return closedReceived;
    }

    void setClosedReceived() {
        closedReceived = true;
    }

    public static final class WorkerExecutor implements Executor {
        public final AbstractNioWorker worker;

        public WorkerExecutor(AbstractNioWorker worker) {
            this.worker = worker;
        }

        @Override
        public void execute(Runnable command) {
            worker.executeInIoThread(command, /* alwaysAsync */ true);
        }
    }

}
